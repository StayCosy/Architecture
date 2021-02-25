# Arthas使用

[![img](https://upload.jianshu.io/users/upload_avatars/2143704/064002e82a4f?imageMogr2/auto-orient/strip|imageView2/1/w/96/h/96)](https://www.jianshu.com/u/c323ec8e077b)

[AlanKim](https://www.jianshu.com/u/c323ec8e077b)

12019.05.21 14:49:32字数 1,993阅读 31,435

Authas — 开源的java诊断工具

#### 下载安装

authas是一个jar包，可以直接下载后运行

```shell
wget https://alibaba.github.io/arthas/arthas-boot.jar

java -jar arthas-boot.jar
```

就可以启动起来。启动后，authas会自动检测存在的java进程，这时候需要选择你想要诊断的进程，回车即可。如下图所示：

![img](https://upload-images.jianshu.io/upload_images/2143704-a00c0f81e741f53a.png?imageMogr2/auto-orient/strip|imageView2/2/w/561)

image-20190515215103606.png

如果不知道某个java进程的详情，可以使用

```undefined
jps -mlVv
或
ps -ef| grep java
```

来查看.

![img](https://upload-images.jianshu.io/upload_images/2143704-faa03b929c42463e.png?imageMogr2/auto-orient/strip|imageView2/2/w/968)

image-20190515215504917.png

#### 常用命令

##### 基础命令

```bash
help——查看命令帮助信息
cat——打印文件内容，和linux里的cat命令类似
pwd——返回当前的工作目录，和linux命令类似
cls——清空当前屏幕区域
session——查看当前会话的信息
reset——重置增强类，将被 Arthas 增强过的类全部还原，Arthas 服务端关闭时会重置所有增强过的类
version——输出当前目标 Java 进程所加载的 Arthas 版本号
history——打印命令历史
quit——退出当前 Arthas 客户端，其他 Arthas 客户端不受影响
shutdown——关闭 Arthas 服务端，所有 Arthas 客户端全部退出
keymap——Arthas快捷键列表及自定义快捷键
```

##### JVM相关

###### Dashboard

```shell
dashboard
 USAGE:
   dashboard [-b] [-i <value>] [-n <value>]

 SUMMARY:
   Overview of target jvm's thread, memory, gc, vm, tomcat info.

 EXAMPLES:
   dashboard
   dashboard -n 10
   dashboard -i 2000

 WIKI:
   https://alibaba.github.io/arthas/dashboard

 OPTIONS:
 -b, --batch                                   Execute this command in batch mode.
 -i, --interval <value>                        The interval (in ms) between two executions, default is 5000 ms.
 -n, --number-of-execution <value>             The number of times this command will be executed.
```

![img](https://upload-images.jianshu.io/upload_images/2143704-be23b5fc3d8cbe73.png?imageMogr2/auto-orient/strip|imageView2/2/w/994)

image-20190515215857569.png

可以看到，这里会显示出线程(按照cpu占用百分比倒排)、内存(堆空间实时情况)、GC情况等数据。

常用的参数：

-i  400ms 每次执行间隔时间

-n  执行多少次dashboard，不指定的话会一直刷新

###### thread

thread命令用来查看当前jvm中的线程信息的，如下图所示：

![img](https://upload-images.jianshu.io/upload_images/2143704-675427ee245c725c.png?imageMogr2/auto-orient/strip|imageView2/2/w/973)

image-20190515220246248.png

可以看到，默认执行thread命令，会直接打印出当前所有的线程。

```shell
$ thread
 USAGE:
   thread [-b] [-i <value>] [-n <value>] [id]

 SUMMARY:
   Display thread info, thread stack

 EXAMPLES:
   thread
   thread 51
   thread -n -1
   thread -n 5
   thread -b
   thread -i 2000

 WIKI:
   https://alibaba.github.io/arthas/thread

 OPTIONS:
 -b, --include-blocking-th  Find the thread who is holding a lock that blocks t
 read                       he most number of threads.
 -i, --sample-interval <va  Specify the sampling interval (in ms) when calculat
 lue>                       ing cpu usage.
 -n, --top-n-threads <valu  The number of thread(s) to show, ordered by cpu uti
 e>                         lization, -1 to show all.
 <id>                       Show thread stack
```

常用参数：

-b 查看目前block的线程

-i 5000ms 查看在接下来的多长时间内 统计cpu利用率

-n 5  查看cpu占用率前5的线程的堆栈信息

<thread_id> 直接跟着线程id，可以看到thread的堆栈信息

如下：

```shell
$ thread -n 1
"as-command-execute-daemon" Id=396 cpuUsage=53% RUNNABLE
    at sun.management.ThreadImpl.dumpThreads0(Native Method)
    at sun.management.ThreadImpl.getThreadInfo(ThreadImpl.java:448)
    at com.taobao.arthas.core.command.monitor200.ThreadCommand.processTopBusyThreads(ThreadCommand.java:133)
    at com.taobao.arthas.core.command.monitor200.ThreadCommand.process(ThreadCommand.java:79)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl.process(AnnotatedCommandImpl.java:82)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl.access$100(AnnotatedCommandImpl.java:18)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl$ProcessHandler.handle(AnnotatedCommandImpl.java:111)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl$ProcessHandler.handle(AnnotatedCommandImpl.java:108)
    at com.taobao.arthas.core.shell.system.impl.ProcessImpl$CommandProcessTask.run(ProcessImpl.java:370)
    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
    at java.lang.Thread.run(Thread.java:748)

    Number of locked synchronizers = 1
    - java.util.concurrent.ThreadPoolExecutor$Worker@21b51d37


Affect(row-cnt:0) cost in 541 ms.
```

可以看到这里占用cpu最高的一个线程的栈信息。

###### jvm

查看当前jvm信息

###### sysprop

查看和修改JVM的系统属性

###### sysenv

查看JVM的环境变量

###### getstatic

查看类的静态属性

##### class/classloader相关

###### mc

Memory Compiler/内存编译器，编译`.java`文件生成`.class`。

```undefined
mc /tmp/Test.java
```

###### redefine

加载外部的`.class`文件，redefine jvm已加载的类。

```kotlin
redefine /tmp/Test.class
redefine -c 327a647b /tmp/Test.class /tmp/Test\$Inner.class
```

###### sc

Search  class

查看jvm中已加载的类，不过直接执行sc并没有太多信息，如下：

```ruby
$ sc com.google.common.hash.HashFunction
com.google.common.hash.AbstractHashFunction
com.google.common.hash.HashFunction
com.google.common.hash.MessageDigestHashFunction
Affect(row-cnt:3) cost in 213 ms.
```

需要加入-d参数，如下

```shell
$ sc -d com.google.common.hash.HashFunction
 class-info        com.google.common.hash.AbstractHashFunction
 code-source
 name              com.google.common.hash.AbstractHashFunction
 isInterface       false
 isAnnotation      false
 isEnum            false
 isAnonymousClass  false
 isArray           false
 isLocalClass      false
 isMemberClass     false
 isPrimitive       false
 isSynthetic       false
 simple-name       AbstractHashFunction
 modifier          abstract
 annotation        com.google.errorprone.annotations.Immutable
 interfaces        com.google.common.hash.HashFunction
 super-class       +-java.lang.Object
 class-loader      +-com.intellij.util.lang.UrlClassLoader@277050dc
 classLoaderHash   277050dc

 class-info        com.google.common.hash.HashFunction
 code-source
 name              com.google.common.hash.HashFunction
 isInterface       true
 isAnnotation      false
 isEnum            false
 isAnonymousClass  false
 isArray           false
 isLocalClass      false
 isMemberClass     false
 isPrimitive       false
 isSynthetic       false
 simple-name       HashFunction
 modifier          abstract,interface,public
 annotation        com.google.errorprone.annotations.Immutable
 interfaces
 super-class
 class-loader      +-com.intellij.util.lang.UrlClassLoader@277050dc
 classLoaderHash   277050dc

 class-info        com.google.common.hash.MessageDigestHashFunction
 code-source
 name              com.google.common.hash.MessageDigestHashFunction
 isInterface       false
 isAnnotation      false
 isEnum            false
 isAnonymousClass  false
 isArray           false
 isLocalClass      false
 isMemberClass     false
 isPrimitive       false
 isSynthetic       false
 simple-name       MessageDigestHashFunction
 modifier          final
 annotation        com.google.errorprone.annotations.Immutable
 interfaces        java.io.Serializable
 super-class       +-com.google.common.hash.AbstractHashFunction
                     +-java.lang.Object
 class-loader      +-com.intellij.util.lang.UrlClassLoader@277050dc
 classLoaderHash   277050dc

Affect(row-cnt:3) cost in 102 ms.
```

可以看到这里有更详细的信息。

具体的参数如下：

```shell
$ help sc
 USAGE:
   sc [-d] [-x <value>] [-f] [-h] [-E] class-pattern

 SUMMARY:
   Search all the classes loaded by JVM

 EXAMPLES:
   sc -d org.apache.commons.lang.StringUtils
   sc -d org/apache/commons/lang/StringUtils
   sc -d *StringUtils
   sc -d -f org.apache.commons.lang.StringUtils
   sc -E org\\.apache\\.commons\\.lang\\.StringUtils

 WIKI:
   https://alibaba.github.io/arthas/sc

 OPTIONS:
 -d, --details                                                       Display the details of class
 -x, --expand <value>                                                Expand level of object (0 by default)
 -f, --field                                                         Display all the member variables
 -h, --help                                                          this help
 -E, --regex                                                         Enable regular expression to match (wildcard matching by default)
 <class-pattern>                                                     Class name pattern, use either '.' or '/' as separator
```

###### sm

search method

```shell
$ sm xx.xxx.xx.xx.xxx.xx.CouponService
xx.xxx.xx.xx.xxx.xx.CouponServiceImpl <init>()V
xx.xxx.xx.xx.xxx.xx.CouponServiceImpl matchCouponOnShopIds(Ljava/lang/Long;Ljava/util/List;)Ljava/util/Map;
xx.xxx.xx.xx.xxx.xx.CouponServiceImpl$$EnhancerByGuice$$9b64c3b8 <init>()V
xx.xxx.xx.xx.xxx.xx.CouponServiceImpl$$EnhancerByGuice$$9b64c3b8 CGLIB$matchCouponOnShopIds$0(Ljava/lang/Long;Ljava/util/List;)Ljava/util/Map;
xx.xxx.xx.xx.xxx.xx.CouponServiceImpl$$EnhancerByGuice$$9b64c3b8 CGLIB$SET_THREAD_CALLBACKS([Lcom/google/inject/internal/cglib/pr$ oxy/$Callback;)V
xx.xxx.xx.xx.xxx.xx.CouponServiceImpl$$EnhancerByGuice$$9b64c3b8 CGLIB$SET_STATIC_CALLBACKS([Lcom/google/inject/internal/cglib/proxy/$Callback;)V
xx.xxx.xx.xx.xxx.xx.CouponServiceImpl$$EnhancerByGuice$$9b64c3b8 CGLIB$STATICHOOK35()V
xx.xxx.xx.xx.xxx.xx.CouponServiceImpl$$EnhancerByGuice$$9b64c3b8 CGLIB$BIND_CALLBACKS(Ljava/lang/Object;)V
xx.xxx.xx.xx.xxx.xx.CouponServiceImpl$$EnhancerByGuice$$9b64c3b8 CGLIB$findMethodProxy(Lcom/google/inject/internal/cglib/core/$Signature;)Lcom/google/inject/internal/cglib/proxy/$MethodProxy;
xx.xxx.xx.xx.xxx.xx.CouponServiceImpl$$EnhancerByGuice$$9b64c3b8 matchCouponOnShopIds(Ljava/lang/Long;Ljava/util/List;)Ljava/util/Map;
xx.xxx.xx.xx.xxx.xx.CouponService matchCouponOnShopIds(Ljava/lang/Long;Ljava/util/List;)Ljava/util/Map;
com.sun.proxy.$Proxy197 <init>(Ljava/lang/reflect/InvocationHandler;)V
com.sun.proxy.$Proxy197 matchCouponOnShopIds(Ljava/lang/Long;Ljava/util/List;)Ljava/util/Map;
com.sun.proxy.$Proxy197 equals(Ljava/lang/Object;)Z
com.sun.proxy.$Proxy197 toString()Ljava/lang/String;
com.sun.proxy.$Proxy197 hashCode()I
Affect(row-cnt:16) cost in 53 ms.
```

可以看到，通过sm命令可以查到对应类的所有方法

通过加入-d参数可以获取method的信息信息

```shell
$ sm xx.xxx.xx.xx.xxx.xx.CouponService -d
 declaring-class   xx.xxx.xx.xx.xxx.xx.CouponServiceImpl
 constructor-name  <init>
 modifier          public
 annotation
 parameters
 exceptions

 declaring-class  xx.xxx.xx.xx.xxx.xx.CouponServiceImpl
 method-name      matchCouponOnShopIds
 modifier         public
 annotation
 parameters       java.lang.Long
                  java.util.List
 return           java.util.Map
 exceptions       xx.xxx.xx.xx.xxx.xx.ServiceException
 
 ...
```

sh

###### jad

反编译代码，如下

```java
$ jad com.google.common.hash.HashFunction

ClassLoader:
+-com.intellij.util.lang.UrlClassLoader@277050dc

Location:


/*
 * Decompiled with CFR 0_132.
 *
 * Could not load the following classes:
 *  com.google.common.hash.Funnel
 *  com.google.common.hash.HashCode
 *  com.google.common.hash.Hasher
 *  com.google.errorprone.annotations.Immutable
 */
package com.google.common.hash;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.errorprone.annotations.Immutable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

@Immutable
public interface HashFunction {
    public int bits();

    public HashCode hashLong(long var1);

    public HashCode hashInt(int var1);

    public <T> HashCode hashObject(T var1, Funnel<? super T> var2);

    public HashCode hashUnencodedChars(CharSequence var1);

    public HashCode hashString(CharSequence var1, Charset var2);

    public HashCode hashBytes(ByteBuffer var1);

    public HashCode hashBytes(byte[] var1, int var2, int var3);

    public HashCode hashBytes(byte[] var1);

    public Hasher newHasher();

    public Hasher newHasher(int var1);
}

Affect(row-cnt:1) cost in 1730 ms.
```

###### classloader

查看classloader的继承树，urls，类加载信息

![img](https://upload-images.jianshu.io/upload_images/2143704-1ab5f767fd9c6578.png?imageMogr2/auto-orient/strip|imageView2/2/w/1200)

image-20190520141344323.png

查看其参数如下

```shell
$ help classloader
 USAGE:
   classloader [-a] [-c <value>] [-h] [-i] [-l] [--load <value>] [-r <value>] [-t]

 SUMMARY:
   Show classloader info

 EXAMPLES:
   classloader
   classloader -t
   classloader -l
   classloader -c 327a647b
   classloader -c 327a647b -r META-INF/MANIFEST.MF
   classloader -a
   classloader -a -c 327a647b
   classloader -c 659e0bfd --load demo.MathGame

 WIKI:
   https://alibaba.github.io/arthas/classloader

 OPTIONS:
 -a, --all                                                           Display all classes loaded by ClassLoader
 -c, --classloader <value>                                           The hash code of the special ClassLoader
 -h, --help                                                          this help
 -i, --include-reflection-classloader                                Include sun.reflect.DelegatingClassLoader
 -l, --list-classloader                                              Display statistics info by classloader instance
     --load <value>                                                  Use ClassLoader to load class, won't work without -c specified
 -r, --resource <value>                                              Use ClassLoader to find resources, won't work without -c specified
 -t, --tree                                                          Display ClassLoader tree
```

-t : 把classloader的树打印出来，也会打印出来粗略的hashcode

-l : 根据classloader实例的个数来打印classloader，会打印出来hashcode

-c : 用classloader对应的hashcode 来查看对应的jar urls

查看classloader tree信息

```shell
$ classloader -t
+-BootstrapClassLoader
+-sun.misc.Launcher$ExtClassLoader@6379eb
  +-com.taobao.arthas.agent.ArthasClassloader@5ab570f5
  +-sun.misc.Launcher$AppClassLoader@18b4aac2
    +-com.alibaba.fastjson.util.ASMClassLoader@34c3f8d2
    +-com.alibaba.fastjson.util.ASMClassLoader@463b35ac
    +-com.alibaba.jvm.sandbox.agent.SandboxClassLoader@567d299b
    +-ModuleClassLoader[crc32=3750510264;file=/data/.ewatch/ewatch-install/sandbox/lib/../module/sandbox-mgr-module.jar;]
    +-ModuleClassLoader[crc32=2030492332;file=/data/.ewatch/ewatch-install/sandbox/ewatch/ewatch-agent-jar-with-dependencies.jar;]
    +-com.alibaba.jvm.sandbox.core.classloader.ProviderClassLoader@42f93a98
Affect(row-cnt:10) cost in 20 ms.
```

查看URLClassLoader实际的urls

```jsx
$ classloader -c 5ab570f5
file:/root/.arthas/lib/3.1.1/arthas/arthas-core.jar

Affect(row-cnt:1790) cost in 19 ms.
```

使用ClassLoader去查找resource

```jsx
$ classloader -c 3d4eac69  -r META-INF/MANIFEST.MF
 jar:file:/System/Library/Java/Extensions/MRJToolkit.jar!/META-INF/MANIFEST.MF
 jar:file:/private/tmp/arthas-demo.jar!/META-INF/MANIFEST.MF
 jar:file:/Users/hengyunabc/.arthas/lib/3.0.5/arthas/arthas-agent.jar!/META-INF/MANIFEST.MF
```

也可以尝试查找类的class文件：

```jsx
$ classloader -c 1b6d3586 -r java/lang/String.class
 jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/rt.jar!/java/lang/String.class
```

使用ClassLoader去加载类

```kotlin
$ classloader -c 3d4eac69 --load demo.MathGame
load class success.
 class-info        demo.MathGame
 code-source       /private/tmp/arthas-demo.jar
 name              demo.MathGame
 isInterface       false
 isAnnotation      false
 isEnum            false
 isAnonymousClass  false
 isArray           false
 isLocalClass      false
 isMemberClass     false
 isPrimitive       false
 isSynthetic       false
 simple-name       MathGame
 modifier          public
 annotation
 interfaces
 super-class       +-java.lang.Object
 class-loader      +-sun.misc.Launcher$AppClassLoader@3d4eac69
                     +-sun.misc.Launcher$ExtClassLoader@66350f69
 classLoaderHash   3d4eac69
```

##### monitor/watch/trace相关

###### monitor

对匹配 `class-pattern`／`method-pattern`的类、方法的调用进行监控。

`monitor` 命令是一个非实时返回命令.

实时返回命令是输入之后立即返回，而非实时返回的命令，则是不断的等待目标 Java 进程返回信息，直到用户输入 `Ctrl+C` 为止。

服务端是以任务的形式在后台跑任务，植入的代码随着任务的中止而不会被执行，所以任务关闭后，不会对原有性能产生太大影响，而且原则上，任何Arthas命令不会引起原有业务逻辑的改变。

监控的维度说明

| 监控项    | 说明                       |
| --------- | -------------------------- |
| timestamp | 时间戳                     |
| class     | Java类                     |
| method    | 方法（构造方法、普通方法） |
| total     | 调用次数                   |
| success   | 成功次数                   |
| fail      | 失败次数                   |
| rt        | 平均RT                     |
| fail-rate | 失败率                     |

```shell
$ monitor -c 5 demo.MathGame primeFactors
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 94 ms.
 timestamp            class          method        total  success  fail  avg-rt(ms)  fail-rate
-----------------------------------------------------------------------------------------------
 2018-12-03 19:06:38  demo.MathGame  primeFactors  5      1        4     1.15        80.00%
 
 timestamp            class          method        total  success  fail  avg-rt(ms)  fail-rate
-----------------------------------------------------------------------------------------------
 2018-12-03 19:06:43  demo.MathGame  primeFactors  5      3        2     42.29       40.00%
 
 timestamp            class          method        total  success  fail  avg-rt(ms)  fail-rate
-----------------------------------------------------------------------------------------------
 2018-12-03 19:06:48  demo.MathGame  primeFactors  5      3        2     67.92       40.00%
 
 timestamp            class          method        total  success  fail  avg-rt(ms)  fail-rate
-----------------------------------------------------------------------------------------------
 2018-12-03 19:06:53  demo.MathGame  primeFactors  5      2        3     0.25        60.00%
 
 timestamp            class          method        total  success  fail  avg-rt(ms)  fail-rate
-----------------------------------------------------------------------------------------------
 2018-12-03 19:06:58  demo.MathGame  primeFactors  1      1        0     0.45        0.00%
 
 timestamp            class          method        total  success  fail  avg-rt(ms)  fail-rate
-----------------------------------------------------------------------------------------------
 2018-12-03 19:07:03  demo.MathGame  primeFactors  2      2        0     3182.72     0.00%
```

###### watch

查看函数的参数、返回值、异常信息，如果有请求触发，就回打印对应的数据。用法如下

```shell
$ help watch
 USAGE:
   watch [-b] [-e] [-x <value>] [-f] [-h] [-n <value>] [-E] [-M <value>] [-s] class-pattern method-pattern express [condition-express]

 SUMMARY:
   Display the input/output parameter, return object, and thrown exception of specified method invocation
   The express may be one of the following expression (evaluated dynamically):
           target : the object
            clazz : the object's class
           method : the constructor or method
           params : the parameters array of method
     params[0..n] : the element of parameters array
        returnObj : the returned object of method
         throwExp : the throw exception of method
         isReturn : the method ended by return
          isThrow : the method ended by throwing exception
            #cost : the execution time in ms of method invocation
 Examples:
   watch -b org.apache.commons.lang.StringUtils isBlank params
   watch -f org.apache.commons.lang.StringUtils isBlank returnObj
   watch org.apache.commons.lang.StringUtils isBlank '{params, target, returnObj}' -x 2
   watch -bf *StringUtils isBlank params
   watch *StringUtils isBlank params[0]
   watch *StringUtils isBlank params[0] params[0].length==1
   watch *StringUtils isBlank params '#cost>100'
   watch -E -b org\.apache\.commons\.lang\.StringUtils isBlank params[0]

 OPTIONS:
 -b, --before                                                        Watch before invocation
 -e, --exception                                                     Watch after throw exception
 -x, --expand <value>                                                Expand level of object (1 by default)
 -f, --finish                                                        Watch after invocation, enable by default
 -h, --help                                                          this help
 -n, --limits <value>                                                Threshold of execution times
 -E, --regex                                                         Enable regular expression to match (wildcard matching by default)
 -M, --sizeLimit <value>                                             Upper size limit in bytes for the result (10 * 1024 * 1024 by default)
 -s, --success                                                       Watch after successful invocation
 <class-pattern>                                                     The full qualified class name you want to watch
 <method-pattern>                                                    The method name you want to watch
 <express>                                                           the content you want to watch, written by ognl.
                                                                     Examples:
                                                                       params
                                                                       params[0]
                                                                       'params[0]+params[1]'
                                                                       '{params[0], target, returnObj}'
                                                                       returnObj
                                                                       throwExp
                                                                       target
                                                                       clazz
                                                                       method

 <condition-express>                                                 Conditional expression in ognl style, for example:
                                                                       TRUE  : 1==1
                                                                       TRUE  : true
                                                                       FALSE : false
                                                                       TRUE  : 'params.length>=0'
                                                                       FALSE : 1==2
```

具体执行如下（查看请求参数）：

```shell
$ watch xxx.xxx.xxx.xxx.BerlinService view params

Press Q or Ctrl+C to abort.
Affect(class-cnt:3 , method-cnt:3) cost in 393 ms.
ts=2019-05-17 14:43:36; [cost=37.391098ms] result=@Object[][
    @BerlinRequest[BerlinRequest(activityId=RC_12, userId=309314913, latitude=31.224360913038254, longitude=121.55064392834902, userAgent=Rajax/1 MI_MAX/helium Android/7.0 Display/NRD90M me/8.16.1 Channel/tengxun ID/e75ef7ec-4069-38ee-83c4-51fb7067f3e1; KERNEL_VERSION:3.10.84-perf-gd39c060 API_Level:24 Hardware: Mozilla/5.0 (Linux; Android 7.0; MI MAX Build/NRD90M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/64.0.3282.137 Mobile Safari/537.36 AliApp(ELMC/8.16.1) TTID/offical WindVane/8.5.0,UT4Aplus/0.2.16, deviceId=null, limit=8, offset=0)],
]
ts=2019-05-17 14:43:36; [cost=46.358372ms] result=@Object[][
    @BerlinRequest[BerlinRequest(activityId=RC_12, userId=309314913, latitude=31.224360913038254, longitude=121.55064392834902, userAgent=Rajax/1 MI_MAX/helium Android/7.0 Display/NRD90M me/8.16.1 Channel/tengxun ID/e75ef7ec-4069-38ee-83c4-51fb7067f3e1; KERNEL_VERSION:3.10.84-perf-gd39c060 API_Level:24 Hardware: Mozilla/5.0 (Linux; Android 7.0; MI MAX Build/NRD90M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/64.0.3282.137 Mobile Safari/537.36 AliApp(ELMC/8.16.1) TTID/offical WindVane/8.5.0,UT4Aplus/0.2.16, deviceId=null, limit=8, offset=0)],
]
```

查看请求参数及相应

```shell
$ watch xx.xxx.xx.xx.xxx.xx.BerlinService view '{params[0], returnObj}'

Press Q or Ctrl+C to abort.
Affect(class-cnt:3 , method-cnt:3) cost in 416 ms.
ts=2019-05-17 14:52:38; [cost=33.968815ms] result=@ArrayList[
    @BerlinRequest[BerlinRequest(activityId=RC_12, userId=117043063, latitude=22.682775497436523, longitude=114.04010772705078, userAgent=Rajax/1 Apple/iPhone9,2 iOS/12.2 me/8.16.3 ID/6E92FDB8-FF9F-4F11-BC1A-490E67B39047; IsJailbroken/0 ASI/30F47DE7-0DE5-488E-A479-C94EB9284301 Mozilla/5.0 (iPhone; CPUiPhone OS 12_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 AliApp(ELMC/8.16.3) UT4Aplus/0.0.4 WindVane/8.5.0 1242x2208 WK, deviceId=null, limit=8, offset=0)],
   @BerlinCommonResponse[xx.xxx.xx.xx.xxx.xx.BerlinCommonResponse@4c5df030],
]
```

可以看到，包含了请求及相应数据、整体cost-time,不过需要注意的是，这里并没有打印出所有详细数据。如果需要打印所有数据，加入`-x`参数表示遍历深度，可以调整来打印具体的参数和结果内容。默认-x 的深度为1，可以指定 2 3 等,如下：

```shell
$ watch xx.xxx.xx.xx.xxx.xx.BerlinService view '{params, returnObj}' -x 2
ts=2019-05-20 11:12:57; [cost=41.677987ms] result=@ArrayList[
    @Object[][
        @BerlinRequest[BerlinRequest(activityId=RC_12, userId=840800602, latitude=36.042474, longitude=103.849928, userAgent=Mozilla/5.0 (Linux; U; Android 8.1.0; zh-CN; DUB-AL00 Build/HUAWEIDUB-AL00) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.2987.108 UCBrowser/11.8.8.968 UWS/2.13.2.74 Mobile Safari/537.36 UCBS/2.13.2.74_190419112332 NebulaSDK/1.8.100112 Nebula AlipayDefined(nt:4G,ws:360|0|2.0) AliApp(AP/10.1.62.5549) AlipayClient/10.1.62.5549 Language/zh-Hans useStatusBar/true isConcaveScreen/true, deviceId=null, limit=8, offset=0)],
    ],
    @BerlinCommonResponse[
        code=@String[200],
        message=@String[成功],
        data=@ArrayList[isEmpty=false;size=7],
    ],
]
```

如过需要按照耗时进行过滤，需要加入： '#cost>200' 代表耗时超过200ms的才会打印出来。

**watch/stack/trace这个三个命令都支持`#cost`**

###### trace

方法内部调用路径，并输出方法路径上的每个节点上耗时

查看help

```shell
$ help trace
 USAGE:
   trace [-h] [-j] [-n <value>] [-p <value>] [-E] class-pattern method-pattern [condition-express]

 SUMMARY:
   Trace the execution time of specified method invocation.
                                                      FALSE : false
                                                      TRUE  : 'params.length>=0'
                                                      FALSE : 1==2

   The express may be one of the following expression (evaluated dynamically):
           target : the object
            clazz : the object's class
           method : the constructor or method
           params : the parameters array of method
     params[0..n] : the element of parameters array
        returnObj : the returned object of method
         throwExp : the throw exception of method
         isReturn : the method ended by return
          isThrow : the method ended by throwing exception
            #cost : the execution time in ms of method invocation
 EXAMPLES:
   trace org.apache.commons.lang.StringUtils isBlank
   trace *StringUtils isBlank
   trace *StringUtils isBlank params[0].length==1
   trace *StringUtils isBlank '#cost>100'
   trace -E org\\.apache\\.commons\\.lang\\.StringUtils isBlank
   trace -E com.test.ClassA|org.test.ClassB method1|method2|method3

 WIKI:
   https://alibaba.github.io/arthas/trace

 OPTIONS:
 -h, --help                                         this help
 -j, --jdkMethodSkip                                skip jdk method trace
 -n, --limits <value>                               Threshold of execution times
 -p, --path <value>                                 path tracing pattern
 -E, --regex                                        Enable regular expression to match (wildcard matching by default)
 <class-pattern>                                    Class name pattern, use either '.' or '/' as separator
 <method-pattern>                                   Method name pattern
 <condition-express>                                Conditional expression in ognl style, for example:
                                                      TRUE  : 1==1
                                                      TRUE  : true
```

平时主要使用-j (忽略jdk method trace)、'#cost>10'(过滤耗时时间) -n (执行次数)

```shell
$ trace xx.xxx.xx.xx.xxx.xx.QueryAction queryByReq  -j '#cost>5' -n 5

Press Q or Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 781 ms.
`---ts=2019-05-20 15:27:10;thread_name=thread-272;id=1fb;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@18b4aac2
    `---[12.250248ms] xx.xxx.xx.xx.xxx.xx.QueryAction:queryByReq()
    ...
    ...
```

###### stack

输出当前方法被调用的调用路径

很多时候我们都知道一个方法被执行，但这个方法被执行的路径非常多，或者你根本就不知道这个方法是从那里被执行了，此时你需要的是 stack 命令。

主要使用-n 命令，用于控制执行次数

```css
stack xx.xxx.xx.xx.xxx.xx.QueryAction queryByReq -n 1
```

###### tt

先看个例子

```bash
$ tt -t xx.xxx.xx.xx.xxx.xx.QueryAction queryByReq -n 5
Press Q or Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 995 ms.
 IND  TIMESTAMP   COST(  IS-  IS  OBJECT    CLASS             METHOD
 EX               ms)    RET  -E
                              XP
--------------------------------------------------------------------------------
 100  2019-05-20  17.52  tru  fa  0x7df629  QueryValidSkuAct  queryValidSkuActi
 5     15:41:29   694    e    ls  87        ivityAction       vityByReq
                              e
 100  2019-05-20  10.26  tru  fa  0x4bb52c  QueryValidSkuAct  queryValidSkuActi
 6     15:41:29   7017   e    ls  96        ivityAction       vityByReq
                              e
 100  2019-05-20  19.80  tru  fa  0x20d351  QueryValidSkuAct  queryValidSkuActi
 7     15:41:29   7279   e    ls  d3        ivityAction       vityByReq
                              e
 100  2019-05-20  19.84  tru  fa  0x286366  QueryValidSkuAct  queryValidSkuActi
 8     15:41:29   4526   e    ls  b4        ivityAction       vityByReq
                              e
 100  2019-05-20  19.91  tru  fa  0x31297b  QueryValidSkuAct  queryValidSkuActi
 9     15:41:29   0582   e    ls  01        ivityAction       vityByReq
                              e
Command execution times exceed limit: 5, so command will exit. You can set it with -n option.
 101  2019-05-20  19.94  tru  fa  0x7a3c2f  QueryValidSkuAct  queryValidSkuActi
 0     15:41:29   8529   e    ls  c4        ivityAction       vityByReq
```

- 表格字段说明

| 表格字段  | 字段解释                                                     |
| --------- | ------------------------------------------------------------ |
| INDEX     | 时间片段记录编号，每一个编号代表着一次调用，后续tt还有很多命令都是基于此编号指定记录操作，非常重要。 |
| TIMESTAMP | 方法执行的本机时间，记录了这个时间片段所发生的本机时间       |
| COST(ms)  | 方法执行的耗时                                               |
| IS-RET    | 方法是否以正常返回的形式结束                                 |
| IS-EXP    | 方法是否以抛异常的形式结束                                   |
| OBJECT    | 执行对象的`hashCode()`，注意，曾经有人误认为是对象在JVM中的内存地址，但很遗憾他不是。但他能帮助你简单的标记当前执行方法的类实体 |
| CLASS     | 执行的类名                                                   |
| METHOD    | 执行的方法名                                                 |

```
-t
```

tt 命令有很多个主参数，`-t` 就是其中之一。这个参数的表明希望记录下类 `*Test` 的 `print` 方法的每次执行情况。

```
-n 3
```

当你执行一个调用量不高的方法时可能你还能有足够的时间用 `CTRL+C` 中断 tt 命令记录的过程，但如果遇到调用量非常大的方法，瞬间就能将你的 JVM 内存撑爆。

此时你可以通过 `-n` 参数指定你需要记录的次数，当达到记录次数时 Arthas 会主动中断tt命令的记录过程，避免人工操作无法停止的情况。

检索调用记录

当你用 `tt` 记录了一大片的时间片段之后，你希望能从中筛选出自己需要的时间片段，这个时候你就需要对现有记录进行检索。

假设我们有这些记录

```objectivec
$ tt -l
 INDEX   TIMESTAMP            COST(ms)  IS-RET  IS-EXP   OBJECT         CLASS                          METHOD
-------------------------------------------------------------------------------------------------------------------------------------
 1000    2018-12-04 11:15:38  1.096236  false   true     0x4b67cf4d     MathGame                       primeFactors
 1001    2018-12-04 11:15:39  0.191848  false   true     0x4b67cf4d     MathGame                       primeFactors
 1002    2018-12-04 11:15:40  0.069523  false   true     0x4b67cf4d     MathGame                       primeFactors
 1003    2018-12-04 11:15:41  0.186073  false   true     0x4b67cf4d     MathGame                       primeFactors
 1004    2018-12-04 11:15:42  17.76437  true    false    0x4b67cf4d     MathGame                       primeFactors
                              9
 1005    2018-12-04 11:15:43  0.4776    false   true     0x4b67cf4d     MathGame                       primeFactors
Affect(row-cnt:6) cost in 4 ms.
```

我需要筛选出 `primeFactors` 方法的调用信息

```ruby
$ tt -s 'method.name=="primeFactors"'
 INDEX   TIMESTAMP            COST(ms)  IS-RET  IS-EXP   OBJECT         CLASS                          METHOD
-------------------------------------------------------------------------------------------------------------------------------------
 1000    2018-12-04 11:15:38  1.096236  false   true     0x4b67cf4d     MathGame                       primeFactors
 1001    2018-12-04 11:15:39  0.191848  false   true     0x4b67cf4d     MathGame                       primeFactors
 1002    2018-12-04 11:15:40  0.069523  false   true     0x4b67cf4d     MathGame                       primeFactors
 1003    2018-12-04 11:15:41  0.186073  false   true     0x4b67cf4d     MathGame                       primeFactors
 1004    2018-12-04 11:15:42  17.76437  true    false    0x4b67cf4d     MathGame                       primeFactors
                              9
 1005    2018-12-04 11:15:43  0.4776    false   true     0x4b67cf4d     MathGame                       primeFactors
Affect(row-cnt:6) cost in 607 ms.
```

你需要一个 `-s` 参数。同样的，搜索表达式的核心对象依旧是 `Advice` 对象。

查看调用信息

对于具体一个时间片的信息而言，你可以通过 `-i` 参数后边跟着对应的 `INDEX` 编号查看到他的详细信息。

```dart
$ tt -i 1003
 INDEX            1003
 GMT-CREATE       2018-12-04 11:15:41
 COST(ms)         0.186073
 OBJECT           0x4b67cf4d
 CLASS            demo.MathGame
 METHOD           primeFactors
 IS-RETURN        false
 IS-EXCEPTION     true
 PARAMETERS[0]    @Integer[-564322413]
 THROW-EXCEPTION  java.lang.IllegalArgumentException: number is: -564322413, need >= 2
                      at demo.MathGame.primeFactors(MathGame.java:46)
                      at demo.MathGame.run(MathGame.java:24)
                      at demo.MathGame.main(MathGame.java:16)
 
Affect(row-cnt:1) cost in 11 ms.
```

重做一次调用

当你稍稍做了一些调整之后，你可能需要前端系统重新触发一次你的调用，此时得求爷爷告奶奶的需要前端配合联调的同学再次发起一次调用。而有些场景下，这个调用不是这么好触发的。

`tt` 命令由于保存了当时调用的所有现场信息，所以我们可以自己主动对一个 `INDEX` 编号的时间片自主发起一次调用，从而解放你的沟通成本。此时你需要 `-p` 参数。通过 `--replay-times` 指定 调用次数，通过 `--replay-interval` 指定多次调用间隔(单位ms, 默认1000ms)

```ruby
$ tt -i 1004 -p
 RE-INDEX       1004
 GMT-REPLAY     2018-12-04 11:26:00
 OBJECT         0x4b67cf4d
 CLASS          demo.MathGame
 METHOD         primeFactors
 PARAMETERS[0]  @Integer[946738738]
 IS-RETURN      true
 IS-EXCEPTION   false
 COST(ms)         0.186073
 RETURN-OBJ     @ArrayList[
                    @Integer[2],
                    @Integer[11],
                    @Integer[17],
                    @Integer[2531387],
                ]
Time fragment[1004] successfully replayed.
Affect(row-cnt:1) cost in 14 ms.
```

你会发现结果虽然一样，但调用的路径发生了变化，有原来的程序发起变成了 Arthas 自己的内部线程发起的调用了。

- 需要强调的点

  1. **ThreadLocal 信息丢失**

     很多框架偷偷的将一些环境变量信息塞到了发起调用线程的 ThreadLocal 中，由于调用线程发生了变化，这些 ThreadLocal 线程信息无法通过 Arthas 保存，所以这些信息将会丢失。

     一些常见的 CASE 比如：鹰眼的 TraceId 等。

  2. **引用的对象**

     需要强调的是，`tt` 命令是将当前环境的对象引用保存起来，但仅仅也只能保存一个引用而已。如果方法内部对入参进行了变更，或者返回的对象经过了后续的处理，那么在 `tt` 查看的时候将无法看到当时最准确的值。这也是为什么 `watch` 命令存在的意义。