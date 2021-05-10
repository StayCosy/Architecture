# Kubernetes 网络概念初探

Kubernetes 网络是 Kubernetes 中一个核心概念。简而言之，Kubernetes 网络模型可以确保集群上所有 Kubernetes pod 都能进行通信。此外，在 Kubernetes 网络模型的基础上，Kubernetes 还有其他核心概念，即 Kubernetes Services 和 Kubernetes Ingress。



本文将使用系统模型的方法探索 Kubernetes 网络。我们将开发一个简单的模型来了解容器与容器间的通信以及 Pod 之间的通信。



![img](https://static001.geekbang.org/infoq/ab/abcc44a6f0de84e2f04d8dcc5a50892b.png)

## 如何看待网络

毫无疑问，网络是一个极为广泛且复杂的领域，它需要多年的理论积累以及实践才能精通。在本文中，我们将在概念层面对网络进行梳理，暂时不涉及实现层面的细节。



![img](https://static001.geekbang.org/infoq/34/3425852a0a8a8f18cff2a7c8d52378d1.png)

理想的网络模型



上图将网络描述为 Network Graph，该网络由一组节点以及节点之间的链接组成。如果当且仅当节点之间存在联系时，一个节点才可以与另一个节点交换信息。



![img](https://static001.geekbang.org/infoq/a7/a70e738484a7c2761c42fec9c83e7c57.png)

消息交换框架



一个节点，即源节点，通过将消息放入目标的输入队列，与另一个节点，即目标交换消息。消息交换由源节点观察到的 Send Event，Send·M 和在目标节点观察到的相应的 Receive Event，Recv·M 表示。



![img](https://static001.geekbang.org/infoq/a2/a2d4b7aaccf4eafcccbcbd27b9317cf4.png)

消息交换行为



网络中的节点要么是 Process，要么是 Switch。Process 会产生和消耗消息，Switch 根据其转发信息库（FIB）处理消息。



![img](https://static001.geekbang.org/infoq/c5/c5f423dcf35da452d3619d57d694b1df.png)

S1 和 S2 的转发信息库（FIB）



上图描述了 Switch 的转发信息库（FIB）S1 和 S2。在收到消息时，每台 Switch 都会查询其转发信息库，以决定是发送（deliver）、转发（forward）还是丢弃（discard）该消息。



Switch：

- 将信息的请求头，即源地址、源端口、目标地址和目标端口与其转发信息库相匹配
- 执行相关操作，默认为弃置（discard）

## Kubernetes 网络模型

![img](https://static001.geekbang.org/infoq/63/6325597f84d445e3de597e41a342a32d.png)



Kubernetes 网络模型是一个描述性的网络模型，也就是说，任何满足 Kubernetes 网络模型规范的网络都是 Kubernetes 网络。



然而，Kubernetes 并没有规定如何实现网络模型。事实上，现在市面上有许多替代的实现，称为网络插件。



本节将用一组关于消息交换的约束条件来描述 Kubernetes 网络模型。



限制条件：网络可寻址实体



Kubernetes 网络模型定义了 3 个可寻址实体：K8S pod、K8S 节点以及 K8S Service，每个实体都会分配到一个不同的 IP 地址。



```
∧ (K8s-Pod(E₁) ∨ K8s-Node(E₁) ∨ K8s-Service(E₁))∧ (K8s-Pod(E₂) ∨ K8s-Node(E₂) ∨ K8s-Service(E₂)):  addr(E₁, a) ∧ addr(E₂, a)₂   ⟺ E₁ = E₂
```

复制代码



然而，网络模型不对这些 IP 地址做任何进一步的声明。例如，Kubernetes 网络模型不对从这些 IP 地址中提取的 IP 地址空间做任何进一步的声明。

### 限制条件：容器间通信

Kubernetes 网络模型要求在 Pod P 上下文中执行的容器 C1 可以通过 localhost 与在 P 上下文中执行的其他容器 C2 进行通信。



```
K8s-Pod(P) ∧ K8s-Container(C₁, P) ∧ K8s-Container(C₂, P):  open(C₂, p)   ⟹    Send(e, C₁, 127.0.0.1, _, 127.0.0.1, p)      ⟹         Recv(e, C₂, 127.0.0.1, _, 127.0.0.1, p)
```

复制代码

### 限制条件：Pod 到 Pod

Kubernetes 网络模型要求在 Pod P1 上下文中执行的容器 C1 可以通过 P2 的地址与在 P2 上下文中执行的其他容器 C2 进行通信。



```
∧ K8s-Pod(P₁) ∧ K8s-Container(C₁, P₁)∧ K8s-Pod(P₂) ∧ K8s-Container(C2, P₂):addr(P₁, sa) ∧ addr(P₁, ta) ∧ open(C₂, tp)  ⟹   Send(e, C₁, sa, sp, ta, tp)     ⟹      Recv(e, C₂, sa, sp, ta, tp)
```

复制代码

### 限制条件：Process 到 Pod

Kubernetes 网络模型要求托管在节点 N 上的一个 Process，称为 Daemon D，可以通过 P 的地址与托管在 N 上的 Pod P 上下文中执行的任何容器 C 进行通信。



```
K8s-Node(N) ∧ K8s-Daemon(D) ∧ K8s-Pod(P) ∧ K8s-Container(C, P):host(N, D) ∧ host(N, P) ∧ addr(P, a) ∧ open(C, p)  ⟹   Send(e, D, _, _, a, p)    ⟹     Recv(e, C, _, _, a, p)
```

复制代码

## Kubernetes 网络作为 Network Graph

![img](https://static001.geekbang.org/infoq/d8/d87300162ccc632012505efa43056915.png)



本节用 Kubernetes Network Graph 这个理想的模型来描述 Kubernetes 网络模型。



下图描述了本节内容中的用例：Kubernetes 集群 K1 由 2 个节点组成。每个节点托管 2 个 Pod。每个 Pod 执行 2 个容器，一个容器监听 8080 端口，一个容器监听 9090 端口。此外，每个节点托管 1 个 Daemon。



![img](https://static001.geekbang.org/infoq/b3/b3bf71482c19a6383bcadb42c7dd8567.png)



我们可以将 Kubernetes 集群网络建模为一个具有一组节点和一组链接的 Graph。

### 节点

每个 K8S 容器 C 映射到网络 Process C



```
K8s-Pod(P) ∧ K8s-Container(C, P):   Process(C)
```

复制代码



每个 Daemon D 映射到网络 Process C



```
K8s-Daemon(D):   Process(D)
```

复制代码



每个 K8s Pod P 映射到网络 Switch P, Pod 的 Switch



```
K8s-Pod(P):  Switch(P)
```

复制代码



每个 K8S 节点 N 映射到网络 Switch N，节点的 Switch：



```
K8s-Pod(N):  Switch(N)
```

复制代码

### 链接

每个容器 C 会被链接到其 Pod Switch P



```
K8s-Pod(P) ∧ K8s-Container(C, P):  link(C, P)
```

复制代码



每个 Daemon D 会被链接到其节点 Switch N



```
K8s-Node(N) ∧ K8s-Daemon(D): host(N, D)  ⟹   link(D, N)
```

复制代码



每个 Pod Switch P 会被链接到其节点 Switch N



```
K8s-Node(N) ∧ K8s-Pod(P):  host(N, P)    ⟹      link(P, N)
```

复制代码



每个节点 Switch N1 会被链接到其他各节点 Switch N2



```
K8s-Node(N₁) ∧ K8s-Node(N₂):  N₁ ≠ N₂   ⟹     link(N₁, N₂)
```

复制代码

### 在 Pod Switch 的转发信息库

![img](https://static001.geekbang.org/infoq/87/877d8c95e53ceb6f283227dc1d68ac36.png)

P2 的转发信息库



```
1. Delivery on localhostK8s-Pod(P) ∧ K8s-Container(C, P): open(C, p)  ⟹   [* * 127.0.0.1 p Deliver(C)] in FIB[P]2. Delivery on Pod AddressK8s-Pod(P) ∧ K8s-Container(C, P): addr(P, a) ∧ open(C, p)  ⟹   [* * a p Deliver(C)] in FIB[P]3. Local Forwarding RuleK8s-Node(N) ∧ K8s-Pod(P): host(N, P)  ⟹   [* * * * Forward(N)] in FIB[P]
```

复制代码

### 在节点 Switch 的转发信息库

![img](https://static001.geekbang.org/infoq/d4/d40dd7328aa2d0e55555e30b7b16a65e.png)

转发信息库 N2



```
1. Node to Pod Forwarding RuleK8s-Node(N) ∧ K8s-Pod(P):  host(N, P) ∧ addr(P, a)   ⟹    [* * a * Forward(P)] in FIB[N]2. Node to Node Forwalding RuleK8s-Node(N₁) ∧ K8s-Node(N₂) ∧ K8s-Pod(P):   N₁ ≠ N₂ ∧ host(N₂, P) ∧ addr(P, a)    ⟹     [* * a * Forward(N₂)] in FIB[N₁]
```

复制代码

## 示例

本节将通过一些例子，按照 Kubernetes 集群网络 K1 中的消息生命（Life of a Message）来进行讲解。

### 容器到容器

容器 C1.1 需要与容器 C1.2 进行通信：



- C1.1 在 P1 的上下文中执行
- C1.2 在 P1 的上下文中执行



![img](https://static001.geekbang.org/infoq/53/535462847bfe026acd95b1876a4a7564.png)

C1.1 通过 127.0.0.1:9090 到 C1.2

### 节点内 Pod 到 Pod 通信

容器 C 1.1 需要与 C 3.1 进行通信：



- C 1.1 在 N1 节点上的 P1 上下文中执行
- C 3.1 在 N1 节点上的 P3 上下文中执行



![img](https://static001.geekbang.org/infoq/42/4203be817f325ec3d3fb4107f0a3560c.png)

C 1.1 通过 10.1.1.2:8080 到 C 3.1

### 节点间 Pod 到 Pod 通信

容器 C 1.1 需要与容器 C 2.1 进行通信：



- C1.1 是在 N1 节点上托管的 P1 的上下文中执行的
- C2.1 在节点 N2 上的 P2 上下文中执行



![img](https://static001.geekbang.org/infoq/8d/8da5934b3d5890abd993344a134a96fb.png)

C1.1 通过 10.1.2.1:8080 到 C2.1

### Daemon 到 Pod 通信

Daemon D1 需要与容器 C 1.1 通信：



- D1 托管在节点 N1 上
- C 1.1 在 Pod P1 的上下文中执行，该 Pod 托管在节点 N1 上



![img](https://static001.geekbang.org/infoq/c9/c90825330c16e97996ec8455a2426a2e.png)

D1 通过 10.1.1.1:8080 到 C 1.1

## 总结

Kubernetes 网络模型是一个允许性的网络模型，也就是说，任何满足 Kubernetes 网络模型约束的网络都是一个有效的 Kubernetes 网络。



将 Kubernetes 网络模型映射到 Network Graph，使我们能够在概念层面上对网络进行推理，并且跳过了在实现层面上推理所需的一系列细节。



在后续的文章中，我们将使用这个 Network Graph 来讨论 Kubernetes 服务、Kubernetes Ingress 和 Kubernetes 策略。