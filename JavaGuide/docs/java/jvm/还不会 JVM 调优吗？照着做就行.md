# 还不会 JVM 调优吗？照着做就行



这几天压测预生产环境，发现 TPS 各种不稳。因为是重构的系统，据说原来的系统在高并发的时候一点问题没有，结果重构的系统被几十个并发压一下就各种不稳定。虽然测试的同事没有说啥，但自己感觉被啪啪的打脸。



于是各种排查，最先想到的就是 JVM 参数，于是优化一番，希望能够出一个好的结果。尽管后来证明不稳定的原因是安装 LoadRunner 的压测服务器不稳定，不关我的系统的事，不过也是记录一下，一是做个备份，二是可以给别人做个参考。



## 写在前面的话

因为 Hotspot JDK 提供的参数默认值，在小版本之间不断变化，参数之间也会互相影响。而且，服务器配置不同，都可能影响最后的效果。所以千万不要迷信网上的某篇文章（包括这篇）里面的参数配置，一切的配置都需要自己亲身测试一番才能用。针对于 JVM 参数默认值不断变化，可以使用`-XX:+PrintFlagsFinal`打印当前环境 JVM 参数默认值，比如：`java -XX:PrintFlagsFinal -version`，也可以用`java [生产环境参数] -XX:+PrintFlagsFinal –version | grep [待查证的参数]`查看具体的参数数据。



[这里](https://www.howardliu.cn/files/java/global-flags.txt) 是一个 8G 服务器的参数，JDK 版本信息如下：



```
java version "1.8.0_73"Java(TM) SE Runtime Environment (build 1.8.0_73-b02)Java HotSpot(TM) 64-Bit Server VM (build 25.73-b02, mixed mode)
```

复制代码

## 堆设置

堆内存设置应该算是一个 Java 程序猿的基本素养，最少也得修改过 Xms、Xmx、Xmn 这三个参数了。但是一个 2G 堆大小的 JVM，可能总共占用多少内存的？



> 堆内存 ＋ 线程数 ＊ 线程栈 ＋ 永久代 ＋ 二进制代码 ＋ 堆外内存
>
> 2G + 1000 * 1M + 256M + 48/240M + (～2G) = 5.5G (3.5G)
>
> - 堆内存： 存储 Java 对象，默认为物理内存的 1/64
> - 线程栈： 存储局部变量（原子类型，引用）及其他，默认为 1M
> - 永久代： 存储类定义及常量池，注意 JDK7/8 的区别
> - 二进制代码：JDK7 与 8，打开多层编译时的默认值不一样，从 48 到 240M
> - 堆外内存： 被 Netty，堆外缓存等使用，默认最大值约为堆内存大小



也就是说，堆内存设置为 2G，那一个有 1000 个线程的 JVM 可能需要占 5.5G，在考虑系统占用、IO 占用等等各种情况，一台 8G 的服务器，也就启动一个服务了。当然，如果线程数少、并发不高、压力不大，还是可以启动多个，而且也可以把堆内存降低。



1. -Xms2g 与 -Xmx2g：堆内存大小，第一个是最小堆内存，第二个是最大堆内存，比较合适的数值是 2-4g，再大就得考虑 GC 时间
2. -Xmn1g 或 （-XX:NewSize=1g 和 -XX:MaxNewSize=1g） 或 -XX:NewRatio=1：设置新生代大小，JDK 默认新生代占堆内存大小的 1/3，也就是`-XX:NewRatio=2`。这里是设置的 1g，也就是`-XX:NewRatio=1`。可以根据自己的需要设置。
3. -XX:MetaspaceSize=128m 和 -XX:MaxMetaspaceSize=512m，JDK8 的永生代几乎可用完机器的所有内存，为了保护服务器不会因为内存占用过大无法连接，需要设置一个 128M 的初始值，512M 的最大值保护一下。
4. -XX:SurvivorRatio：新生代中每个存活区的大小，默认为 8，即 1/10 的新生代， 1/(SurvivorRatio+2)，有人喜欢设小点省点给新生代，但要避免太小使得存活区放不下临时对象而要晋升到老生代，还是从 GC Log 里看实际情况了。
5. -Xss256k：在堆之外，线程占用栈内存，默认每条线程为 1M。存放方法调用出参入参的栈、局部变量、标量替换后的局部变量等，有人喜欢设小点节约内存开更多线程。但反正内存够也就不必要设小，有人喜欢再设大点，特别是有 JSON 解析之类的递归调用时不能设太小。
6. -XX:MaxDirectMemorySize：堆外内存/直接内存的大小，默认为堆内存减去一个 Survivor 区的大小。
7. -XX:ReservedCodeCacheSize：JIT 编译后二进制代码的存放区，满了之后就不再编译。默认开多层编译 240M，可以在 JMX 里看看 CodeCache 的大小。

## GC 设置

目前比较主流的 GC 是 CMS 和 G1，有大神建议以 8G 为界。（据说 JDK 9 默认的是 G1）。因为应用设置的内存都比较小，所以选择 CMS 收集器。下面的参数也是针对 CMS 收集器的，等之后如果有需要，再补充 G1 收集器的参数。

### CMS 设置

1. -XX:+UseConcMarkSweepGC：启用 CMS 垃圾收集器
2. -XX:CMSInitiatingOccupancyFraction=80 与 -XX:+UseCMSInitiatingOccupancyOnly：两个参数需要配合使用，否则第一个参数的 75 只是一个参考值，JVM 会重新计算 GC 的时间。
3. -XX:MaxTenuringThreshold=15：对象在 Survivor 区熬过多少次 Young GC 后晋升到年老代，默认是 15。Young GC 是最大的应用停顿来源，而新生代里 GC 后存活对象的多少又直接影响停顿的时间，所以如果清楚 Young GC 的执行频率和应用里大部分临时对象的最长生命周期，可以把它设的更短一点，让其实不是临时对象的新生代长期对象赶紧晋升到年老代。
4. -XX:-DisableExplicitGC：允许使用 System.gc() 主动调用 GC。这里需要说明下，有的 JVM 优化建议是设置-XX:-DisableExplicitGC，关闭手动调用 System.gc()。这是因为 System.gc() 是触发 Full GC，频繁的 Full GC 会严重影响性能。但是很多 NIO 框架，比如 Netty，会使用堆外内存，如果没有 Full GC 的话，堆外内存就无法回收。如果不主动调用 System.gc()，就需要等到 JVM 自己触发 Full GC，这个时候，就可能引起长时间的停顿（STW），而且机器负载也会升高。所以不能够完全禁止 System.gc()，又得缩短 Full GC 的时间，那就使用`-XX:+ExplicitGCInvokesConcurrent`或`-XX:+ExplicitGCInvokesConcurrentAndUnloadsClasses`选项，使用 CMS 收集器来触发 Full GC。这两个选项需要配合`-XX:+UseConcMarkSweepGC`使用。
5. -XX:+ExplicitGCInvokesConcurrent：使用 System.gc() 时触发 CMS GC，而不是 Full GC。默认是不开启的，只有使用-XX:+UseConcMarkSweepGC 选项的时候才能开启这个选项。
6. -XX:+ExplicitGCInvokesConcurrentAndUnloadsClasses：使用 System.gc() 时，永久代也被包括进 CMS 范围内。只有使用-XX:+UseConcMarkSweepGC 选项的时候才能开启这个选项。
7. -XX:+ParallelRefProcEnabled：默认为 false，并行的处理 Reference 对象，如 WeakReference，除非在 GC log 里出现 Reference 处理时间较长的日志，否则效果不会很明显。
8. -XX:+ScavengeBeforeFullGC：在 Full GC 执行前先执行一次 Young GC。
9. -XX:+UseGCOverheadLimit： 限制 GC 的运行时间。如果 GC 耗时过长，就抛 OOM。
10. -XX:+UseParallelGC：设置并行垃圾收集器
11. -XX:+UseParallelOldGC：设置老年代使用并行垃圾收集器
12. -XX:-UseSerialGC：关闭串行垃圾收集器
13. -XX:+CMSParallelInitialMarkEnabled 和 -XX:+CMSParallelRemarkEnabled：降低标记停顿
14. -XX:+CMSScavengeBeforeRemark：默认为关闭，在 CMS remark 前，先执行一次 minor GC 将新生代清掉，这样从老生代的对象引用到的新生代对象的个数就少了，停止全世界的 CMS remark 阶段就短一些。如果看到 GC 日志里 remark 阶段的时间超长，可以打开此项看看有没有效果，否则还是不要打开了，白白多了次 YGC。
15. -XX:CMSWaitDuration=10000：设置垃圾收集的最大时间间隔，默认是 2000。
16. -XX:+CMSClassUnloadingEnabled：在 CMS 中清理永久代中的过期的 Class 而不等到 Full GC，JDK7 默认关闭而 JDK8 打开。看自己情况，比如有没有运行动态语言脚本如 Groovy 产生大量的临时类。它会增加 CMS remark 的暂停时间，所以如果新类加载并不频繁，这个参数还是不开的好。

### GC 日志

GC 过程可以通过 GC 日志来提供优化依据。



1. -XX:+PrintGCDetails：启用 gc 日志打印功能
2. -Xloggc:/path/to/gc.log：指定 gc 日志位置
3. -XX:+PrintHeapAtGC：打印 GC 前后的详细堆栈信息
4. -XX:+PrintGCDateStamps：打印可读的日期而不是时间戳
5. -XX:+PrintGCApplicationStoppedTime：打印所有引起 JVM 停顿时间，如果真的发现了一些不知什么的停顿，再临时加上`-XX:+PrintSafepointStatistics -XX: PrintSafepointStatisticsCount=1`找原因。
6. -XX:+PrintGCApplicationConcurrentTime：打印 JVM 在两次停顿之间正常运行时间，与`-XX:+PrintGCApplicationStoppedTime`一起使用效果更佳。
7. -XX:+PrintTenuringDistribution：查看每次 minor GC 后新的存活周期的阈值
8. -XX:+UseGCLogFileRotation 与 -XX:NumberOfGCLogFiles=10 与 -XX:GCLogFileSize=10M：GC 日志在重启之后会清空，但是如果一个应用长时间不重启，那 GC 日志会增加，所以添加这 3 个参数，是 GC 日志滚动写入文件，但是如果重启，可能名字会出现混乱。
9. -XX:PrintFLSStatistics=1：打印每次 GC 前后内存碎片的统计信息

## 其他参数设置

1. -ea：启用断言，这个没有什么好说的，可以选择启用，或者选择不启用，没有什么大的差异。完全根据自己的系统进行处理。
2. -XX:+UseThreadPriorities：启用线程优先级，主要是因为我们可以给予周期性任务更低的优先级，以避免干扰客户端工作。在我当前的环境中，是默认启用的。
3. -XX:ThreadPriorityPolicy=42：允许降低线程优先级
4. -XX:+HeapDumpOnOutOfMemoryError：发生内存溢出时进行 heap-dump
5. -XX:HeapDumpPath=/path/to/java_pid<pid>.hprof：这个参数与`-XX:+HeapDumpOnOutOfMemoryError`共同作用，设置 heap-dump 时内容输出文件。
6. -XX:ErrorFile=/path/to/hs_err_pid<pid>.log：指定致命错误日志位置。一般在 JVM 发生致命错误时会输出类似 hs_err_pid<pid>.log 的文件，默认是在工作目录中（如果没有权限，会尝试在/tmp 中创建），不过还是自己指定位置更好一些，便于收集和查找，避免丢失。
7. -XX:StringTableSize=1000003：指定字符串常量池大小，默认值是 60013。对 Java 稍微有点常识的应该知道，字符串是常量，创建之后就不可修改了，这些常量所在的地方叫做字符串常量池。如果自己系统中有很多字符串的操作，且这些字符串值比较固定，在允许的情况下，可以适当调大一些池子大小。
8. -XX:+AlwaysPreTouch：在启动时把所有参数定义的内存全部捋一遍。使用这个参数可能会使启动变慢，但是在后面内存使用过程中会更快。可以保证内存页面连续分配，新生代晋升时不会因为申请内存页面使 GC 停顿加长。通常只有在内存大于 32G 的时候才会有感觉。
9. -XX:-UseBiasedLocking：禁用偏向锁（在存在大量锁对象的创建且高度并发的环境下（即非多线程高并发应用）禁用偏向锁能够带来一定的性能优化）
10. -XX:AutoBoxCacheMax=20000：增加数字对象自动装箱的范围，JDK 默认-128～127 的 int 和 long，超出范围就会即时创建对象，所以，增加范围可以提高性能，但是也是需要测试。
11. -XX:-OmitStackTraceInFastThrow：不忽略重复异常的栈，这是 JDK 的优化，大量重复的 JDK 异常不再打印其 StackTrace。但是如果系统是长时间不重启的系统，在同一个地方跑了 N 多次异常，结果就被 JDK 忽略了，那岂不是查看日志的时候就看不到具体的 StackTrace，那还怎么调试，所以还是关了的好。
12. -XX:+PerfDisableSharedMem：启用标准内存使用。JVM 控制分为标准或共享内存，区别在于一个是在 JVM 内存中，一个是生成/tmp/hsperfdata_{userid}/{pid}文件，存储统计数据，通过 mmap 映射到内存中，别的进程可以通过文件访问内容。通过这个参数，可以禁止 JVM 写在文件中写统计数据，代价就是 jps、jstat 这些命令用不了了，只能通过 jmx 获取数据。但是在问题排查时，jps、jstat 这些小工具是很好用的，比 jmx 这种很重的东西好用很多，所以需要自己取舍。[这里](http://www.evanjones.ca/jvm-mmap-pause.html) 有个 GC 停顿的例子。
13. -Djava.net.preferIPv4Stack=true：这个参数是属于网络问题的一个参数，可以根据需要设置。在某些开启 ipv6 的机器中，通过`InetAddress.getLocalHost().getHostName()`可以获取完整的机器名，但是在 ipv4 的机器中，可能通过这个方法获取的机器名不完整，可以通过这个参数来获取完整机器名。

## 大神给出的例子

下面贴上大神给出的例子，可以参考使用，不过还是建议在自己的环境中有针对的验证之后再使用，毕竟大神的环境和自己的环境还是不同。

### 性能相关

-XX:-UseBiasedLocking -XX:-UseCounterDecay -XX:AutoBoxCacheMax=20000-XX:+PerfDisableSharedMem（可选） -XX:+AlwaysPreTouch -Djava.security.egd=file:/dev/./urandom

### 内存大小相关 (JDK7)

-Xms4096m -Xmx4096m -Xmn2048m -XX:MaxDirectMemorySize=4096m-XX:PermSize=128m -XX:MaxPermSize=512m -XX:ReservedCodeCacheSize=240M



> 如果使用 jdk8，就把-XX:PermSize=128m -XX:MaxPermSize=512m 换成-XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m，正如前面所说的，这两套参数是为了保证安全的，建议还是加上。

### CMS GC 相关

-XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75-XX:+UseCMSInitiatingOccupancyOnly -XX:MaxTenuringThreshold=6-XX:+ExplicitGCInvokesConcurrent -XX:+ParallelRefProcEnabled

### GC 日志相关

-Xloggc:/dev/shm/app-gc.log -XX:+PrintGCApplicationStoppedTime-XX:+PrintGCDateStamps -XX:+PrintGCDetails

### 异常日志相关

-XX:-OmitStackTraceInFastThrow -XX:ErrorFile={LOGDIR}/hs_err_%p.log-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath={LOGDIR}/

### JMX 相关

-Dcom.sun.management.jmxremote.port=${JMX_PORT} -Dcom.sun.management.jmxremote-Djava.rmi.server.hostname=127.0.0.1 -Dcom.sun.management.jmxremote.authenticate=false-Dcom.sun.management.jmxremote.ssl=false

## 参考

1. [Java 性能优化指南 1.8 版，及唯品会的实战](http://calvin1978.blogcn.com/articles/javatuning.html)
2. [Java 中的逃逸分析和 TLAB 以及 Java 对象分配](http://blog.csdn.net/yangzl2008/article/details/43202969)
3. [The Four Month Bug: JVM statistics cause garbage collection pauses](http://www.evanjones.ca/jvm-mmap-pause.html)