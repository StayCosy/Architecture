# Java即时编译器原理解析及实践



- 跟其他常见的编程语言不同，Java 将编译过程分成了两个部分，这就对性能带来了一定的影响。而即时（Just In Time, JIT）编译器能够提高 Java 程序的运行速度。
  本文会先解析一下即时编译器的原理，然后再分享一些在美团实践的经验，希望能对大家有所帮助或者启发。

## 一、导读

常见的编译型语言如 C++，通常会把代码直接编译成 CPU 所能理解的机器码来运行。而 Java 为了实现“一次编译，处处运行”的特性，把编译的过程分成两部分，首先它会先由 javac 编译成通用的中间形式——字节码，然后再由解释器逐条将字节码解释为机器码来执行。所以在性能上，Java 通常不如 C++ 这类编译型语言。

为了优化 Java 的性能 ，JVM 在解释器之外引入了即时（Just In Time）编译器：当程序运行时，解释器首先发挥作用，代码可以直接执行。随着时间推移，即时编译器逐渐发挥作用，把越来越多的代码编译优化成本地代码，来获取更高的执行效率。解释器这时可以作为编译运行的降级手段，在一些不可靠的编译优化出现问题时，再切换回解释执行，保证程序可以正常运行。

即时编译器极大地提高了 Java 程序的运行速度，而且跟静态编译相比，即时编译器可以选择性地编译热点代码，省去了很多编译时间，也节省很多的空间。目前，即时编译器已经非常成熟了，在性能层面甚至可以和编译型语言相比。不过在这个领域，大家依然在不断探索如何结合不同的编译方式，使用更加智能的手段来提升程序的运行速度。

## 二、Java 的执行过程

Java 的执行过程整体可以分为两个部分，第一步由 javac 将源码编译成字节码，在这个过程中会进行词法分析、语法分析、语义分析，编译原理中这部分的编译称为前端编译。接下来无需编译直接逐条将字节码解释执行，在解释执行的过程中，虚拟机同时对程序运行的信息进行收集，在这些信息的基础上，编译器会逐渐发挥作用，它会进行后端编译——把字节码编译成机器码，但不是所有的代码都会被编译，只有被 JVM 认定为的热点代码，才可能被编译。

怎么样才会被认为是热点代码呢？JVM 中会设置一个阈值，当方法或者代码块的在一定时间内的调用次数超过这个阈值时就会被编译，存入 codeCache 中。当下次执行时，再遇到这段代码，就会从 codeCache 中读取机器码，直接执行，以此来提升程序运行的性能。整体的执行过程大致如下图所示：

![img](https://static001.infoq.cn/resource/image/55/26/556yyd08ef7e7aef1yy22d6b980cfb26.png)

### 1. JVM 中的编译器

JVM 中集成了两种编译器，Client Compiler 和 Server Compiler，它们的作用也不同。Client Compiler 注重启动速度和局部的优化，Server Compiler 则更加关注全局的优化，性能会更好，但由于会进行更多的全局分析，所以启动速度会变慢。两种编译器有着不同的应用场景，在虚拟机中同时发挥作用。

**Client Compiler**

HotSpot VM 带有一个 Client Compiler C1 编译器。这种编译器启动速度快，但是性能比较 Server Compiler 来说会差一些。C1 会做三件事：

- 局部简单可靠的优化，比如字节码上进行的一些基础优化，方法内联、常量传播等，放弃许多耗时较长的全局优化。
- 将字节码构造成高级中间表示（High-level Intermediate Representation，以下称为 HIR），HIR 与平台无关，通常采用图结构，更适合 JVM 对程序进行优化。
- 最后将 HIR 转换成低级中间表示（Low-level Intermediate Representation，以下称为 LIR），在 LIR 的基础上会进行寄存器分配、窥孔优化（局部的优化方式，编译器在一个基本块或者多个基本块中，针对已经生成的代码，结合 CPU 自己指令的特点，通过一些认为可能带来性能提升的转换规则或者通过整体的分析，进行指令转换，来提升代码性能）等操作，最终生成机器码。

**Server Compiler**

Server Compiler 主要关注一些编译耗时较长的全局优化，甚至会还会根据程序运行的信息进行一些不可靠的激进优化。这种编译器的启动时间长，适用于长时间运行的后台程序，它的性能通常比 Client Compiler 高 30% 以上。目前，Hotspot 虚拟机中使用的 Server Compiler 有两种：C2 和 Graal。

**C2 Compiler**

在 Hotspot VM 中，默认的 Server Compiler 是 C2 编译器。

C2 编译器在进行编译优化时，会使用一种控制流与数据流结合的图数据结构，称为 Ideal Graph。Ideal Graph 表示当前程序的数据流向和指令间的依赖关系，依靠这种图结构，某些优化步骤（尤其是涉及浮动代码块的那些优化步骤）变得不那么复杂。

Ideal Graph 的构建是在解析字节码的时候，根据字节码中的指令向一个空的 Graph 中添加节点，Graph 中的节点通常对应一个指令块，每个指令块包含多条相关联的指令，JVM 会利用一些优化技术对这些指令进行优化，比如 Global Value Numbering、常量折叠等，解析结束后，还会进行一些死代码剔除的操作。生成 Ideal Graph 后，会在这个基础上结合收集的程序运行信息来进行一些全局的优化，这个阶段如果 JVM 判断此时没有全局优化的必要，就会跳过这部分优化。

无论是否进行全局优化，Ideal Graph 都会被转化为一种更接近机器层面的 MachNode Graph，最后编译的机器码就是从 MachNode Graph 中得的，生成机器码前还会有一些包括寄存器分配、窥孔优化等操作。关于 Ideal Graph 和各种全局的优化手段会在后面的章节详细介绍。Server Compiler 编译优化的过程如下图所示：

![img](https://static001.infoq.cn/resource/image/7f/ea/7f019750be7e065c6b7da772739d31ea.png)

**Graal Compiler**

从 JDK 9 开始，Hotspot VM 中集成了一种新的 Server Compiler，Graal 编译器。相比 C2 编译器，Graal 有这样几种关键特性：

- 前文有提到，JVM 会在解释执行的时候收集程序运行的各种信息，然后编译器会根据这些信息进行一些基于预测的激进优化，比如分支预测，根据程序不同分支的运行概率，选择性地编译一些概率较大的分支。Graal 比 C2 更加青睐这种优化，所以 Graal 的峰值性能通常要比 C2 更好。
- 使用 Java 编写，对于 Java 语言，尤其是新特性，比如 Lambda、Stream 等更加友好。
- 更深层次的优化，比如虚函数的内联、部分逃逸分析等。

Graal 编译器可以通过 Java 虚拟机参数 **-XX:+UnlockExperimentalVMOptions -XX:+UseJVMCICompiler** 启用。当启用时，它将替换掉 HotSpot 中的 C2 编译器，并响应原本由 C2 负责的编译请求。

### 2. 分层编译

在 Java 7 以前，需要研发人员根据服务的性质去选择编译器。对于需要快速启动的，或者一些不会长期运行的服务，可以采用编译效率较高的 C1，对应参数 -client。长期运行的服务，或者对峰值性能有要求的后台服务，可以采用峰值性能更好的 C2，对应参数 -server。Java 7 开始引入了分层编译的概念，它结合了 C1 和 C2 的优势，追求启动速度和峰值性能的一个平衡。分层编译将 JVM 的执行状态分为了五个层次。五个层级分别是：

- 解释执行。
- 执行不带 profiling 的 C1 代码。
- 执行仅带方法调用次数以及循环回边执行次数 profiling 的 C1 代码。
- 执行带所有 profiling 的 C1 代码。
- 执行 C2 代码。

profiling 就是收集能够反映程序执行状态的数据。其中最基本的统计数据就是方法的调用次数，以及循环回边的执行次数。

通常情况下，C2 代码的执行效率要比 C1 代码的高出 30% 以上。C1 层执行的代码，按执行效率排序从高至低则是 1 层 >2 层 >3 层。这 5 个层次中，1 层和 4 层都是终止状态，当一个方法到达终止状态后，只要编译后的代码并没有失效，那么 JVM 就不会再次发出该方法的编译请求的。服务实际运行时，JVM 会根据服务运行情况，从解释执行开始，选择不同的编译路径，直到到达终止状态。下图中就列举了几种常见的编译路径：

![img](https://static001.infoq.cn/resource/image/ac/52/ace41f7be7f92735bf2814d9ef46e052.png)

- 图中第①条路径，代表编译的一般情况，热点方法从解释执行到被 3 层的 C1 编译，最后被 4 层的 C2 编译。
- 如果方法比较小（比如 Java 服务中常见的 getter/setter 方法），3 层的 profiling 没有收集到有价值的数据，JVM 就会断定该方法对于 C1 代码和 C2 代码的执行效率相同，就会执行图中第②条路径。在这种情况下，JVM 会在 3 层编译之后，放弃进入 C2 编译，直接选择用 1 层的 C1 编译运行。
- 在 C1 忙碌的情况下，执行图中第③条路径，在解释执行过程中对程序进行 profiling ，根据信息直接由第 4 层的 C2 编译。
- 前文提到 C1 中的执行效率是 1 层 >2 层 >3 层，第 3 层一般要比第 2 层慢 35% 以上，所以在 C2 忙碌的情况下，执行图中第④条路径。这时方法会被 2 层的 C1 编译，然后再被 3 层的 C1 编译，以减少方法在 3 层的执行时间。
- 如果编译器做了一些比较激进的优化，比如分支预测，在实际运行时发现预测出错，这时就会进行反优化，重新进入解释执行，图中第⑤条执行路径代表的就是反优化。

总的来说，C1 的编译速度更快，C2 的编译质量更高，分层编译的不同编译路径，也就是 JVM 根据当前服务的运行情况来寻找当前服务的最佳平衡点的一个过程。从 JDK 8 开始，JVM 默认开启分层编译。

### 3. 即时编译的触发

Java 虚拟机根据方法的调用次数以及循环回边的执行次数来触发即时编译。循环回边是一个控制流图中的概念，程序中可以简单理解为往回跳转的指令，比如下面这段代码：

**循环回边**

复制代码

```
public void nlp(Object obj) {
  int sum = 0;
  for (int i = 0; i < 200; i++) {
    sum += i;
  }
}
```

上面这段代码经过编译生成下面的字节码。其中，偏移量为 18 的字节码将往回跳至偏移量为 4 的字节码中。在解释执行时，每当运行一次该指令，Java 虚拟机便会将该方法的循环回边计数器加 1。

**字节码**

复制代码

```
public void nlp(java.lang.Object);
    Code:
       0: iconst_0
       1: istore_1
       2: iconst_0
       3: istore_2
       4: iload_2
       5: sipush        200
       8: if_icmpge     21
      11: iload_1
      12: iload_2
      13: iadd
      14: istore_1
      15: iinc          2, 1
      18: goto          4
      21: return
```

在即时编译过程中，编译器会识别循环的头部和尾部。上面这段字节码中，循环体的头部和尾部分别为偏移量为 11 的字节码和偏移量为 15 的字节码。编译器将在循环体结尾增加循环回边计数器的代码，来对循环进行计数。

当方法的调用次数和循环回边的次数的和，超过由参数 **-XX:CompileThreshold** 指定的阈值时（使用 C1 时，默认值为 1500；使用 C2 时，默认值为 10000），就会触发即时编译。

开启分层编译的情况下，**-XX:CompileThreshold** 参数设置的阈值将会失效，触发编译会由以下的条件来判断：

- 方法调用次数大于由参数 **-XX:TierXInvocationThreshold** 指定的阈值乘以系数。
- 方法调用次数大于由参数 **-XX:TierXMINInvocationThreshold** 指定的阈值乘以系数，并且方法调用次数和循环回边次数之和大于由参数 **-XX:TierXCompileThreshold** 指定的阈值乘以系数时。

**分层编译触发条件公式**

复制代码

```
i > TierXInvocationThreshold * s || (i > TierXMinInvocationThreshold * s  && i + b > TierXCompileThreshold * s) 
i 为调用次数，b 是循环回边次数
```

上述满足其中一个条件就会触发即时编译，并且 JVM 会根据当前的编译方法数以及编译线程数动态调整系数 s。

## 三、编译优化

即时编译器会对正在运行的服务进行一系列的优化，包括字节码解析过程中的分析，根据编译过程中代码的一些中间形式来做局部优化，还会根据程序依赖图进行全局优化，最后才会生成机器码。

### 1. 中间表达形式（Intermediate Representation）

在编译原理中，通常把编译器分为前端和后端，前端编译经过词法分析、语法分析、语义分析生成中间表达形式（Intermediate Representation，以下称为 IR），后端会对 IR 进行优化，生成目标代码。

Java 字节码就是一种 IR，但是字节码的结构复杂，字节码这样代码形式的 IR 也不适合做全局的分析优化。现代编译器一般采用图结构的 IR，静态单赋值（Static Single Assignment，SSA）IR 是目前比较常用的一种。这种 IR 的特点是每个变量只能被赋值一次，而且只有当变量被赋值之后才能使用。举个例子：

**SSA IR**

复制代码

```
Plain Text
{
  a = 1;
  a = 2;
  b = a;
}
```

上述代码中我们可以轻易地发现 a = 1 的赋值是冗余的，但是编译器不能。传统的编译器需要借助数据流分析，从后至前依次确认哪些变量的值被覆盖掉。不过，如果借助了 SSA IR，编译器则可以很容易识别冗余赋值。

上面代码的 SSA IR 形式的伪代码可以表示为：

**SSA IR**

复制代码

```
Plain Text
{
  a_1 = 1;
  a_2 = 2;
  b_1 = a_2;
}
```

由于 SSA IR 中每个变量只能赋值一次，所以代码中的 a 在 SSA IR 中会分成 a_1、a_2 两个变量来赋值，这样编译器就可以很容易通过扫描这些变量来发现 a_1 的赋值后并没有使用，赋值是冗余的。

除此之外，SSA IR 对其他优化方式也有很大的帮助，例如下面这个死代码删除（Dead Code Elimination）的例子：

**DeadCodeElimination**

复制代码

```
public void DeadCodeElimination{
  int a = 2;
  int b = 0
  if(2 > 1){
    a = 1;
  } else{
    b = 2;
  }
  add(a,b)
}
```

可以得到 SSA IR 伪代码：

**DeadCodeElimination**

复制代码

```
a_1 = 2;
b_1 = 0
if true:
  a_2 = 1;
else
  b_2 = 2;
add(a,b)
```

编译器通过执行字节码可以发现 b_2 赋值后不会被使用，else 分支不会被执行。经过死代码删除后就可以得到代码：

**DeadCodeElimination**

复制代码

```
public void DeadCodeElimination{
  int a = 1;
  int b = 0;
  add(a,b)
}
```

我们可以将编译器的每一种优化看成一个图优化算法，它接收一个 IR 图，并输出经过转换后的 IR 图。编译器优化的过程就是一个个图节点的优化串联起来的。

**C1 中的中间表达形式**

前文提及 C1 编译器内部使用高级中间表达形式 HIR，低级中间表达形式 LIR 来进行各种优化，这两种 IR 都是 SSA 形式的。

HIR 是由很多基本块（Basic Block）组成的控制流图结构，每个块包含很多 SSA 形式的指令。基本块的结构如下图所示：

![img](https://static001.infoq.cn/resource/image/38/30/388651324b7b709d9c17fc4d172e4c30.png)

其中，predecessors 表示前驱基本块（由于前驱可能是多个，所以是 BlockList 结构，是多个 BlockBegin 组成的可扩容数组）。同样，successors 表示多个后继基本块 BlockEnd。除了这两部分就是主体块，里面包含程序执行的指令和一个 next 指针，指向下一个执行的主体块。

从字节码到 HIR 的构造最终调用的是 GraphBuilder，GraphBuilder 会遍历字节码构造所有代码基本块储存为一个链表结构，但是这个时候的基本块只有 BlockBegin，不包括具体的指令。第二步 GraphBuilder 会用一个 ValueStack 作为操作数栈和局部变量表，模拟执行字节码，构造出对应的 HIR，填充之前空的基本块，这里给出简单字节码块构造 HIR 的过程示例，如下所示：

**字节码构造 HIR**

复制代码

```
        字节码                     Local Value             operand stack              HIR
      5: iload_1                  [i1,i2]                 [i1]
      6: iload_2                  [i1,i2]                 [i1,i2]   
                                  ................................................   i3: i1 * i2
      7: imul                                   
      8: istore_3                 [i1,i2，i3]              [i3]
```

可以看出，当执行 iload_1 时，操作数栈压入变量 i1，执行 iload_2 时，操作数栈压入变量 i2，执行相乘指令 imul 时弹出栈顶两个值，构造出 HIR i3 : i1 * i2，生成的 i3 入栈。

C1 编译器优化大部分都是在 HIR 之上完成的。当优化完成之后它会将 HIR 转化为 LIR，LIR 和 HIR 类似，也是一种编译器内部用到的 IR，HIR 通过优化消除一些中间节点就可以生成 LIR，形式上更加简化。

**Sea-of-Nodes IR**

C2 编译器中的 Ideal Graph 采用的是一种名为 Sea-of-Nodes 中间表达形式，同样也是 SSA 形式的。它最大特点是去除了变量的概念，直接采用值来进行运算。为了方便理解，可以利用 IR 可视化工具 Ideal Graph Visualizer（IGV），来展示具体的 IR 图。比如下面这段代码：

**example**

复制代码

```
public static int foo(int count) {
  int sum = 0;
  for (int i = 0; i < count; i++) {
    sum += i;
  }
  return sum;
}
```

对应的 IR 图如下所示：

![img](https://static001.infoq.cn/resource/image/88/6f/887yyb50e2d3ae1536eb3f6c7e04726f.png)

图中若干个顺序执行的节点将被包含在同一个基本块之中，如图中的 B0、B1 等。B0 基本块中 0 号 Start 节点是方法入口，B3 中 21 号 Return 节点是方法出口。红色加粗线条为控制流，蓝色线条为数据流，而其他颜色的线条则是特殊的控制流或数据流。被控制流边所连接的是固定节点，其他的则是浮动节点（浮动节点指只要能满足数据依赖关系，可以放在不同位置的节点，浮动节点变动的这个过程称为 Schedule）。

这种图具有轻量级的边结构。图中的边仅由指向另一个节点的指针表示。节点是 Node 子类的实例，带有指定输入边的指针数组。这种表示的优点是改变节点的输入边很快，如果想要改变输入边，只要将指针指向 Node，然后存入 Node 的指针数组就可以了。

依赖于这种图结构，通过收集程序运行的信息，JVM 可以通过 Schedule 那些浮动节点，从而获得最好的编译效果。

**Phi And Region Nodes**

Ideal Graph 是 SSA IR。由于没有变量的概念，这会带来一个问题，就是不同执行路径可能会对同一变量设置不同的值。例如下面这段代码 if 语句的两个分支中，分别返回 5 和 6。此时，根据不同的执行路径，所读取到的值很有可能不同。

**example**

复制代码

```
int test(int x) {
int a = 0;
  if(x == 1) {
    a = 5;
  } else {
    a = 6;
  }
  return a;
}
```

为了解决这个问题，就引入一个 Phi Nodes 的概念，能够根据不同的执行路径选择不同的值。于是，上面这段代码可以表示为下面这张图：

![img](https://static001.infoq.cn/resource/image/dc/63/dccd0278084b9a18e4d0ee458d5yy463.png)

Phi Nodes 中保存不同路径上包含的所有值，Region Nodes 根据不同路径的判断条件，从 Phi Nodes 取得当前执行路径中变量应该赋予的值，带有 Phi 节点的 SSA 形式的伪代码如下：

**Phi Nodes**

复制代码

```
int test(int x) {
  a_1 = 0;
  if(x == 1){
    a_2 = 5;
  }else {
    a_3 = 6;
  }
  a_4 = Phi(a_2,a_3);
  return a_4;
}
```

**Global Value Numbering**

Global Value Numbering（GVN） 是一种因为 Sea-of-Nodes 变得非常容易的优化技术 。

GVN 是指为每一个计算得到的值分配一个独一无二的编号，然后遍历指令寻找优化的机会，它可以发现并消除等价计算的优化技术。如果一段程序中出现了多次操作数相同的乘法，那么即时编译器可以将这些乘法合并为一个，从而降低输出机器码的大小。如果这些乘法出现在同一执行路径上，那么 GVN 还将省下冗余的乘法操作。在 Sea-of-Nodes 中，由于只存在值的概念，因此 GVN 算法将非常简单：即时编译器只需判断该浮动节点是否与已存在的浮动节点的编号相同，所输入的 IR 节点是否一致，便可以将这两个浮动节点归并成一个。比如下面这段代码：

**GVN**

复制代码

```
a = 1;
b = 2;
c = a + b;
d = a + b;
e = d;
```

GVN 会利用 Hash 算法编号，计算 a = 1 时，得到编号 1，计算 b = 2 时得到编号 2，计算 c = a + b 时得到编号 3，这些编号都会放入 Hash 表中保存，在计算 d = a + b 时，会发现 a + b 已经存在 Hash 表中，就不会再进行计算，直接从 Hash 表中取出计算过的值。最后的 e = d 也可以由 Hash 表中查到而进行复用。

可以将 GVN 理解为在 IR 图上的公共子表达式消除（Common Subexpression Elimination，CSE）。两者区别在于，GVN 直接比较值的相同与否，而 CSE 是借助词法分析器来判断两个表达式相同与否。

### 2. 方法内联

方法内联，是指在编译过程中遇到方法调用时，将目标方法的方法体纳入编译范围之中，并取代原方法调用的优化手段。JIT 大部分的优化都是在内联的基础上进行的，方法内联是即时编译器中非常重要的一环。

Java 服务中存在大量 getter/setter 方法，如果没有方法内联，在调用 getter/setter 时，程序执行时需要保存当前方法的执行位置，创建并压入用于 getter/setter 的栈帧、访问字段、弹出栈帧，最后再恢复当前方法的执行。内联了对 getter/setter 的方法调用后，上述操作仅剩字段访问。在 C2 编译器 中，方法内联在解析字节码的过程中完成。当遇到方法调用字节码时，编译器将根据一些阈值参数决定是否需要内联当前方法的调用。如果需要内联，则开始解析目标方法的字节码。比如下面这个示例（来源于网络）：

**方法内联的过程**

复制代码

```
public static boolean flag = true;
public static int value0 = 0;
public static int value1 = 1;
 
public static int foo(int value) {
    int result = bar(flag);
    if (result != 0) {
        return result;
    } else {
        return value;
    }
}
 
public static int bar(boolean flag) {
    return flag ? value0 : value1;
}
```

bar 方法的 IR 图：

![img](https://static001.infoq.cn/resource/image/7f/bb/7f983243ae53da308dc5fbfe6943ebbb.png)

内联后的 IR 图：

![img](https://static001.infoq.cn/resource/image/57/e3/577f4ed0c6066yy36edfaeaf6a5b12e3.png)

内联不仅将被调用方法的 IR 图节点复制到调用者方法的 IR 图中，还要完成其他操作。

1. 被调用方法的参数替换为调用者方法进行方法调用时所传入参数。上面例子中，将 bar 方法中的 1 号 P(0) 节点替换为 foo 方法 3 号 LoadField 节点。
2. 调用者方法的 IR 图中，方法调用节点的数据依赖会变成被调用方法的返回。如果存在多个返回节点，会生成一个 Phi 节点，将这些返回值聚合起来，并作为原方法调用节点的替换对象。图中就是将 8 号 == 节点，以及 12 号 Return 节点连接到原 5 号 Invoke 节点的边，然后指向新生成的 24 号 Phi 节点中。
3. 如果被调用方法将抛出某种类型的异常，而调用者方法恰好有该异常类型的处理器，并且该异常处理器覆盖这一方法调用，那么即时编译器需要将被调用方法抛出异常的路径，与调用者方法的异常处理器相连接。

**方法内联的条件**

编译器的大部分优化都是在方法内联的基础上。所以一般来说，内联的方法越多，生成代码的执行效率越高。但是对于即时编译器来说，内联的方法越多，编译时间也就越长，程序达到峰值性能的时刻也就比较晚。

可以通过虚拟机参数 **-XX:MaxInlineLevel** 调整内联的层数，以及 1 层的直接递归调用（可以通过虚拟机参数 **-XX:MaxRecursiveInlineLevel** 调整）。一些常见的内联相关的参数如下表所示：

![img](https://static001.infoq.cn/resource/image/60/7d/6047b630fb932c15a83ecd405f141a7d.png)

**虚函数内联**

内联是 JIT 提升性能的主要手段，但是虚函数使得内联是很难的，因为在内联阶段并不知道他们会调用哪个方法。例如，我们有一个数据处理的接口，这个接口中的一个方法有三种实现 add、sub 和 multi，JVM 是通过保存虚函数表 Virtual Method Table（以下称为 VMT）存储 class 对象中所有的虚函数，class 的实例对象保存着一个 VMT 的指针，程序运行时首先加载实例对象，然后通过实例对象找到 VMT，通过 VMT 找到对应方法的地址，所以虚函数的调用比直接指向方法地址的 classic call 性能上会差一些。很不幸的是，Java 中所有非私有的成员函数的调用都是虚调用。

C2 编译器已经足够智能，能够检测这种情况并会对虚调用进行优化。比如下面这段代码例子：

**virtual call**

复制代码

```
public class SimpleInliningTest
{
    public static void main(String[] args) throws InterruptedException {
        VirtualInvokeTest obj = new VirtualInvokeTest();
        VirtualInvoke1 obj1 = new VirtualInvoke1();
        for (int i = 0; i < 100000; i++) {
            invokeMethod(obj);
            invokeMethod(obj1);
        }
        Thread.sleep(1000);
    }
 
    public static void invokeMethod(VirtualInvokeTest obj) {
        obj.methodCall();
    }
 
    private static class VirtualInvokeTest {
        public void methodCall() {
            System.out.println("virtual call");
        }
    }
 
    private static class VirtualInvoke1 extends VirtualInvokeTest {
        @Override
        public void methodCall() {
            super.methodCall();
        }
    }
}
```

经过 JIT 编译器优化后，进行反汇编得到下面这段汇编代码：

复制代码

```
 0x0000000113369d37: callq  0x00000001132950a0  ; OopMap{off=476}
                                                ;*invokevirtual methodCall  // 代表虚调用
                                                ; - SimpleInliningTest::invokeMethod@1 (line 18)
                                                ;   {optimized virtual_call}  // 虚调用已经被优化
```

可以看到 JIT 对 methodCall 方法进行了虚调用优化 optimized virtual_call。经过优化后的方法可以被内联。但是 C2 编译器的能力有限，对于多个实现方法的虚调用就“无能为力”了。

比如下面这段代码，我们增加一个实现：

**多实现的虚调用**

复制代码

```
public class SimpleInliningTest
{
    public static void main(String[] args) throws InterruptedException {
        VirtualInvokeTest obj = new VirtualInvokeTest();
        VirtualInvoke1 obj1 = new VirtualInvoke1();
        VirtualInvoke2 obj2 = new VirtualInvoke2();
        for (int i = 0; i < 100000; i++) {
            invokeMethod(obj);
            invokeMethod(obj1);
        invokeMethod(obj2);
        }
        Thread.sleep(1000);
    }
 
    public static void invokeMethod(VirtualInvokeTest obj) {
        obj.methodCall();
    }
 
    private static class VirtualInvokeTest {
        public void methodCall() {
            System.out.println("virtual call");
        }
    }
 
    private static class VirtualInvoke1 extends VirtualInvokeTest {
        @Override
        public void methodCall() {
            super.methodCall();
        }
    }
    private static class VirtualInvoke2 extends VirtualInvokeTest {
        @Override
        public void methodCall() {
            super.methodCall();
        }
    }
}
```

经过反编译得到下面的汇编代码：

复制代码

```
 0x000000011f5f0a37: callq  0x000000011f4fd2e0  ; OopMap{off=28}
                                                ;*invokevirtual methodCall  // 代表虚调用
                                                ; - SimpleInliningTest::invokeMethod@1 (line 20)
                                                ;   {virtual_call}  // 虚调用未被优化
```

可以看到多个实现的虚调用未被优化，依然是 virtual_call。

Graal 编译器针对这种情况，会去收集这部分执行的信息，比如在一段时间，发现前面的接口方法的调用 add 和 sub 是各占 50% 的几率，那么 JVM 就会在每次运行时，遇到 add 就把 add 内联进来，遇到 sub 的情况再把 sub 函数内联进来，这样这两个路径的执行效率就会提升。在后续如果遇到其他不常见的情况，JVM 就会进行去优化的操作，在那个位置做标记，再遇到这种情况时切换回解释执行。

### 3. 逃逸分析

逃逸分析是“一种确定指针动态范围的静态分析，它可以分析在程序的哪些地方可以访问到指针”。Java 虚拟机的即时编译器会对新建的对象进行逃逸分析，判断对象是否逃逸出线程或者方法。即时编译器判断对象是否逃逸的依据有两种：

1. 对象是否被存入堆中（静态字段或者堆中对象的实例字段），一旦对象被存入堆中，其他线程便能获得该对象的引用，即时编译器就无法追踪所有使用该对象的代码位置。
2. 对象是否被传入未知代码中，即时编译器会将未被内联的代码当成未知代码，因为它无法确认该方法调用会不会将调用者或所传入的参数存储至堆中，这种情况，可以直接认为方法调用的调用者以及参数是逃逸的。

逃逸分析通常是在方法内联的基础上进行的，即时编译器可以根据逃逸分析的结果进行诸如锁消除、栈上分配以及标量替换的优化。下面这段代码的就是对象未逃逸的例子：

复制代码

```
pulbic class Example{
    public static void main(String[] args) {
      example();
    }
    public static void example() {
      Foo foo = new Foo();
      Bar bar = new Bar();
      bar.setFoo(foo);
    }
  }
 
  class Foo {}
 
  class Bar {
    private Foo foo;
    public void setFoo(Foo foo) {
      this.foo = foo;
    }
  }
}
```

在这个例子中，创建了两个对象 foo 和 bar，其中一个作为另一个方法的参数提供。该方法 setFoo() 存储对收到的 Foo 对象的引用。如果 Bar 对象在堆上，则对 Foo 的引用将逃逸。但是在这种情况下，编译器可以通过逃逸分析确定 Bar 对象本身不会对逃逸出 example() 的调用。这意味着对 Foo 的引用也不能逃逸。因此，编译器可以安全地在栈上分配两个对象。

**锁消除**

在学习 Java 并发编程时会了解锁消除，而锁消除就是在逃逸分析的基础上进行的。

如果即时编译器能够证明锁对象不逃逸，那么对该锁对象的加锁、解锁操作没就有意义。因为线程并不能获得该锁对象。在这种情况下，即时编译器会消除对该不逃逸锁对象的加锁、解锁操作。实际上，编译器仅需证明锁对象不逃逸出线程，便可以进行锁消除。由于 Java 虚拟机即时编译的限制，上述条件被强化为证明锁对象不逃逸出当前编译的方法。不过，基于逃逸分析的锁消除实际上并不多见。

**栈上分配**

我们都知道 Java 的对象是在堆上分配的，而堆是对所有对象可见的。同时，JVM 需要对所分配的堆内存进行管理，并且在对象不再被引用时回收其所占据的内存。如果逃逸分析能够证明某些新建的对象不逃逸，那么 JVM 完全可以将其分配至栈上，并且在 new 语句所在的方法退出时，通过弹出当前方法的栈桢来自动回收所分配的内存空间。

这样一来，我们便无须借助垃圾回收器来处理不再被引用的对象。不过 Hotspot 虚拟机，并没有进行实际的栈上分配，而是使用了标量替换这一技术。所谓的标量，就是仅能存储一个值的变量，比如 Java 代码中的基本类型。与之相反，聚合量则可能同时存储多个值，其中一个典型的例子便是 Java 的对象。编译器会在方法内将未逃逸的聚合量分解成多个标量，以此来减少堆上分配。下面是一个标量替换的例子：

**标量替换**

复制代码

```
public class Example{
  @AllArgsConstructor
  class Cat{
    int age;
    int weight;
  }
  public static void example(){
    Cat cat = new Cat(1,10);
    addAgeAndWeight(cat.age,Cat.weight);
  }
}
```

经过逃逸分析，cat 对象未逃逸出 example() 的调用，因此可以对聚合量 cat 进行分解，得到两个标量 age 和 weight，进行标量替换后的伪代码：

复制代码

```
public class Example{
  @AllArgsConstructor
  class Cat{
    int age;
    int weight;
  }
  public static void example(){
    int age = 1;
    int weight = 10;
    addAgeAndWeight(age,weight);
  }
}
```

**部分逃逸分析**

部分逃逸分析也是 Graal 对于概率预测的应用。通常来说，如果发现一个对象逃逸出了方法或者线程，JVM 就不会去进行优化，但是 Graal 编译器依然会去分析当前程序的执行路径，它会在逃逸分析基础上收集、判断哪些路径上对象会逃逸，哪些不会。然后根据这些信息，在不会逃逸的路径上进行锁消除、栈上分配这些优化手段。

### 4. Loop Transformations

在文章中介绍 C2 编译器的部分有提及到，C2 编译器在构建 Ideal Graph 后会进行很多的全局优化，其中就包括对循环的转换，最重要的两种转换就是循环展开和循环分离。

**循环展开**

循环展开是一种循环转换技术，它试图以牺牲程序二进制码大小为代价来优化程序的执行速度，是一种用空间换时间的优化手段。

循环展开通过减少或消除控制程序循环的指令，来减少计算开销，这种开销包括增加指向数组中下一个索引或者指令的指针算数等。如果编译器可以提前计算这些索引，并且构建到机器代码指令中，那么程序运行时就可以不必进行这种计算。也就是说有些循环可以写成一些重复独立的代码。比如下面这个循环：

**循环展开**

复制代码

```
public void loopRolling(){
  for(int i = 0;i<200;i++){
    delete(i);  
  }
}
```

上面的代码需要循环删除 200 次，通过循环展开可以得到下面这段代码：

**循环展开**

复制代码

```
public void loopRolling(){
  for(int i = 0;i<200;i+=5){
    delete(i);
    delete(i+1);
    delete(i+2);
    delete(i+3);
    delete(i+4);
  }
}
```

这样展开就可以减少循环的次数，每次循环内的计算也可以利用 CPU 的流水线提升效率。当然这只是一个示例，实际进行展开时，JVM 会去评估展开带来的收益，再决定是否进行展开。

**循环分离**

循环分离也是循环转换的一种手段。它把循环中一次或多次的特殊迭代分离出来，在循环外执行。举个例子，下面这段代码：

**循环分离**

复制代码

```
int a = 10;
for(int i = 0;i<10;i++){
  b[i] = x[i] + x[a];
  a = i;
}
```

可以看出这段代码除了第一次循环 a = 10 以外，其他的情况 a 都等于 i-1。所以可以把特殊情况分离出去，变成下面这段代码：

**循环分离**

复制代码

```
b[0] = x[0] + 10;
for(int i = 1;i<10;i++){
  b[i] = x[i] + x[i-1];
}
```

这种等效的转换消除了在循环中对 a 变量的需求，从而减少了开销。

### 5. 窥孔优化与寄存器分配

前文提到的窥孔优化是优化的最后一步，这之后就会程序就会转换成机器码，窥孔优化就是将编译器所生成的中间代码（或目标代码）中相邻指令，将其中的某些组合替换为效率更高的指令组，常见的比如强度削减、常数合并等，看下面这个例子就是一个强度削减的例子：

**强度削减**

复制代码

```
y1=x1*3  经过强度削减后得到  y1=(x1<<1)+x1
```

编译器使用移位和加法削减乘法的强度，使用更高效率的指令组。

寄存器分配也是一种编译的优化手段，在 C2 编译器中普遍的使用。它是通过把频繁使用的变量保存在寄存器中，CPU 访问寄存器的速度比内存快得多，可以提升程序的运行速度。

寄存器分配和窥孔优化是程序优化的最后一步。经过寄存器分配和窥孔优化之后，程序就会被转换成机器码保存在 codeCache 中。

## 四、实践

即时编译器情况复杂，同时网络上也很少有实战经验，以下是我们团队的一些调整经验。

### 1. 编译相关的重要参数

- **-XX:+TieredCompilation**：开启分层编译，JDK8 之后默认开启
- **-XX:+CICompilerCount=N**：编译线程数，设置数量后，JVM 会自动分配线程数，C1:C2 = 1:2
- **-XX:TierXBackEdgeThreshold**：OSR 编译的阈值
- **-XX:TierXMinInvocationThreshold**：开启分层编译后各层调用的阈值
- **-XX:TierXCompileThreshold**：开启分层编译后的编译阈值
- **-XX:ReservedCodeCacheSize**：codeCache 最大大小
- **-XX:InitialCodeCacheSize**：codeCache 初始大小

**-XX:TierXMinInvocationThreshold** 是开启分层编译的情况下，触发编译的阈值参数，当方法调用次数大于由参数 **-XX:TierXInvocationThreshold** 指定的阈值乘以系数，或者当方法调用次数大于由参数 **-XX:TierXMINInvocationThreshold** 指定的阈值乘以系数，并且方法调用次数和循环回边次数之和大于由参数 **-XX:TierXCompileThreshold** 指定的阈值乘以系数时，便会触发 X 层即时编译。分层编译开启下会乘以一个系数，系数根据当前编译的方法和编译线程数确定，降低阈值可以提升编译方法数，一些常用但是不能编译的方法可以编译优化提升性能。

由于编译情况复杂，JVM 也会动态调整相关的阈值来保证 JVM 的性能，所以不建议手动调整编译相关的参数。除非一些特定的 Case，比如 codeCache 满了停止了编译，可以适当增加 codeCache 大小，或者一些非常常用的方法，未被内联到，拖累了性能，可以调整内敛层数或者内联方法的大小来解决。

### 2. 通过 JITwatch 分析编译日志

通过增加 **-XX:+UnlockDiagnosticVMOptions -XX:+PrintCompilation -XX:+PrintInlining -XX:+PrintCodeCache -XX:+PrintCodeCacheOnCompilation -XX:+TraceClassLoading -XX:+LogCompilation -XX:LogFile=LogPath** 参数可以输出编译、内联、codeCache 信息到文件。但是打印的编译日志多且复杂很难直接从其中得到信息，可以使用 JITwatch 的工具来分析编译日志。JITwatch 首页的 Open Log 选中日志文件，点击 Start 就可以开始分析日志。

![img](https://static001.infoq.cn/resource/image/34/e0/34e05644942568620727674e1e5d62e0.png)

![img](https://static001.infoq.cn/resource/image/b8/ec/b825a79be7cca375229ee92a6baa14ec.png)

如上图所示，区域 1 中是整个项目 Java Class 包括引入的第三方依赖；区域 2 是功能区 Timeline 以图形的形式展示 JIT 编译的时间轴，Histo 是直方图展示一些信息，TopList 里面是编译中产生的一些对象和数据的排序，Cache 是空闲 codeCache 空间，NMethod 是 Native 方法，Threads 是 JIT 编译的线程；区域 3 是 JITwatch 对日志分析结果的展示，其中 Suggestions 中会给出一些代码优化的建议，举个例子，如下图中：

![img](https://static001.infoq.cn/resource/image/bb/74/bb89e68acd97261c179f800753998874.png)

我们可以看到在调用 ZipInputStream 的 read 方法时，因为该方法没有被标记为热点方法，同时又“太大了”，导致无法被内联到。使用 **-XX:CompileCommand** 中 inline 指令可以强制方法进行内联，不过还是建议谨慎使用，除非确定某个方法内联会带来不少的性能提升，否则不建议使用，并且过多使用对编译线程和 codeCache 都会带来不小的压力。

区域 3 中的 -Allocs 和 -Locks 逃逸分析后 JVM 对代码做的优化，包括栈上分配、锁消除等。

### 3. 使用 Graal 编译器

由于 JVM 会去根据当前的编译方法数和编译线程数对编译阈值进行动态的调整，所以实际服务中对这一部分的调整空间是不大的，JVM 做的已经足够多了。

为了提升性能，在服务中尝试了最新的 Graal 编译器。只需要使用 **-XX:+UnlockExperimentalVMOptions -XX:+UseJVMCICompiler** 就可以启动 Graal 编译器来代替 C2 编译器，并且响应 C2 的编译请求，不过要注意的是，Graal 编译器与 ZGC 不兼容，只能与 G1 搭配使用。

前文有提到过，Graal 是一个用 Java 写的即时编译器，它从 Java 9 开始便被集成自 JDK 中，作为实验性质的即时编译器。Graal 编译器就是脱身于 GraalVM，GraalVM 是一个高性能的、支持多种编程语言的执行环境。它既可以在传统的 OpenJDK 上运行，也可以通过 AOT（Ahead-Of-Time）编译成可执行文件单独运行，甚至可以集成至数据库中运行。

前文提到过数次，Graal 的优化都基于某种假设（Assumption）。当假设出错的情况下，Java 虚拟机会借助去优化（Deoptimization）这项机制，从执行即时编译器生成的机器码切换回解释执行，在必要情况下，它甚至会废弃这份机器码，并在重新收集程序 profile 之后，再进行编译。

这些中激进的手段使得 Graal 的峰值性能要好于 C2，而且在 Scale、Ruby 这种语言 Graal 表现更加出色，Twitter 目前已经在服务中大量的使用 Graal 来提升性能，企业版的 GraalVM 使得 Twitter 服务性能提升了 22%。

**使用 Graal 编译器后性能表现**

在我们的线上服务中，启用 Graal 编译后，TP9999 从 60ms -> 50ms ，下降 10ms，下降幅度达 16.7%。

运行过程中的峰值性能会更高。可以看出对于该服务，Graal 编译器带来了一定的性能提升。

**Graal 编译器的问题**

Graal 编译器的优化方式更加激进，因此在启动时会进行更多的编译，Graal 编译器本身也需要被即时编译，所以服务刚启动时性能会比较差。

**考虑的解决办法** ：JDK 9 开始提供工具 jaotc，同时 GraalVM 的 Native Image 都是可以通过静态编译，极大地提升服务的启动速度的方式，但是 GraalVM 会使用自己的垃圾回收，这是一种很原始的基于复制算法的垃圾回收，相比 G1、ZGC（[新一代垃圾回收器ZGC 的探索与实践](http://mp.weixin.qq.com/s?__biz=MjM5NjQ5MTI5OA==&mid=2651752559&idx=1&sn=c720b67e93db1885d72dab8799bba78c&chksm=bd1251228a65d834db610deb2ce55003e0fc1f90793e84873096db19027936f6add301242545&scene=21#wechat_redirect)）这些优秀的新型垃圾回收器，它的性能并不好。同时GraalVM 对Java 的一些特性支持也不够，比如基于配置的支持，比如反射就需要把所有需要反射的类配置一个JSON 文件，在大量使用反射的服务，这样的配置会是很大的工作量。我们也在做这方面的调研。

## 五、总结

本文主要介绍了JIT 即时编译的原理以及在美团一些实践的经验，还有最前沿的即时编译器的使用效果。作为一项解释型语言中提升性能的技术，JIT 已经比较成熟了，在很多语言中都有使用。对于Java 服务，JVM 本身已经做了足够多，但是我们还应该不断深入了解JIT 的优化原理和最新的编译技术，从而弥补JIT 的劣势，提升Java 服务的性能，不断追求卓越。

## 六、参考文献

[1]《深入理解 Java 虚拟机》

[2]《Proceedings of the Java™ Virtual Machine Research and Technology Symposium》Monterey, California, USA April 23–24, 2001

[3]《Visualization of Program Dependence Graphs》 Thomas Würthinger

[4]《深入拆解 Java 虚拟机》 郑宇迪

[5] [JIT 的 Profile 神器 JITWatch](https://mp.weixin.qq.com/s?__biz=MzIxMTI0NzcyMQ==&mid=2650932520&idx=1&sn=e1d50389631eb91959aafef24d359bbc&scene=21#wechat_redirect)