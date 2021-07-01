# Dapr：我不是 Service Mesh！我只是长得很像



![Dapr：我不是Service Mesh！我只是长得很像](https://static001.geekbang.org/infoq/ff/ff7bed6c60abbb4725fb1f9ee3c92c54.png)

# 引言

Dapr：“许多人都说我是 Service Mesh，但我不是！我是 Service Mesh 下一代的发展方向”。



# Service Mesh

## （一）什么是 Service Mesh

说到 Service Mesh 就不得不提一下微服务架构。随着具有分布式能力的微服务技术越来越火热，许多公司都已经完成了架构的技术转型。

但是这些框架技术多数都是以 lib 库依赖的形式集成在 SDK 中，并与业务代码部署在一起。SDK 中 lib 库的升级无法对业务系统做到完全透明：业务系统会因为和业务无关的 lib 库升级而不得不进行升级。

分布式架构技术的火热，使得一些额外的分布式能力比如：如负载均衡、熔断限流、链路监控等也开始集成到 SDK 中。带来的好处是屏蔽了底层的实现细节，但是却使得应用程序的依赖变得越来越臃肿且难以维护。并且一些主流的微服务框架通常都与某种特定开发语言进行绑定，这就很难做到语言无关。

Service Mesh 的出现使得上面提到的问题迎刃而解。优秀的架构设计使得 Service Mesh 天生就具有业务隔离和多语言支持的特点。如图 1 所示。

![img](https://static001.geekbang.org/infoq/78/78faeb3fa63efb10f9dff2cb1c212808.png)

图1 Service Mesh与传统架构对比



Mesh，即网格的意思。Service Mesh 就是将服务像网格一样穿在一起，而真正进行网络通信的是网格内与应用程序（图 2 中绿色的部分）一起部署的 Sidecar（图 2 中蓝色的部分）。

![img](https://static001.geekbang.org/infoq/4b/4bd76cb70bbfb72cb0ab0460fc4f7e24.png)

图2 Service Mesh示意图

在 Service Mesh 中，Sidecar 与应用程序部署在不同的进程中，不管 Sidecar 进行何种变化，应用程序对此都是是无感知的。

Service Mesh 将原来集成到 SDK 中的服务发现、负载均衡等分布式能力下沉到 Sidecar 中，通过 Sidecar 的流量劫持转发应用程序的请求，所有的服务都通过自己的 Sidecar 进行相互通信。

所有的 Sidecar 集合到一起，就组成了 Service Mesh 的数据平面（图 3 中的所有的浅蓝色方块）。数据平面中的 Sidecar 则统一由控制平面进行管理（图 3 中的深蓝色方块）。

![img](https://static001.geekbang.org/infoq/a7/a789e3919cc418aefb449b9a38578539.png)

图3 数据平面和控制平面

## （二）Mesh 技术的蓬勃发展

### 1.istio

Istio 是由 Google、IBM 和 Lyft 共同开发的一款开源 Service Mesh，通过 Istio 可以轻松的为已经部署的服务创建一个服务网格，而服务的代码只需要很少更改甚至无需更改。Istio 也是一个与 k8s 紧密结合的适用于云原生场景的 Service Mesh 产品，通过 Istio 平台可以更方便的进行服务治理。

Istio 具有的强大特性提供了一种统一的、更有效的方式来保护、连接和监视服务。Istio 只需要进行简单的配置就可实现服务的负载均衡、服务到服务的身份验证等分布式功能。Isito 的控制平面非常强大，它可以对 Istio 进行配置和管理，包括：

- 使用 TLS 加密、强身份认证和授权的集群内服务到服务的安全通信
- 自动负载均衡的 HTTP、gRPC、WebSocket 和 TCP 流量
- 通过丰富的路由规则、重试、故障转移和故障注入对流量行为进行细粒度控制
- 一个可插入的策略层和配置 API，支持访问控制、速率限制和配额
- 对集群内的所有流量(包括集群入口和出口)进行自动度量、日志和跟踪

![img](https://static001.geekbang.org/infoq/8e/8e8f96d4a617829e5c9333afd505dbb8.png)

图4 Istio示意图

### 2.Linkerd

Linkerd 是运行在 Kubernetes 上的 Service Mesh，它提供了运行时服务调式、服务可观察性、可靠性和安全性的能力，并且服务的所有代码都无需进行修改。

Linkerd 的工作原理是在每个服务实例所在的环境中部署一组超轻的透明代理，并且这些代理会自动处理所有来往于服务的流量。这些代理相对应用程序来说是透明的，他们可以高效的直接向控制平面发送遥测数据并接收控制信号，而不会被应用程序所感知。这种设计允许 Linkerd 在不引入过多延迟的情况下测量和操纵进出服务的流量。

![img](https://static001.geekbang.org/infoq/25/257922a6305578c93ed1dc158b5360b1.png)

图5 Linkerd示意图

### 3.云原生技术实践者层出不穷

在云原生技术蓬勃发展的今天，通过相关权威机构如 CNCF 的管理，国内外很多优秀的开源云原生技术都已面世。如 Conduit、Consul、Envoy、华为的 ServiceMesher、新浪微博 MotanMesh、Mosn 等等。

国内的大型互联网厂商如 Alibaba、字节跳动，以及一些优秀的公司如中原银行、中移在线等，都在不遗余力的拥抱云原生，拥抱下一代 Mesh 化的微服务架构。



## （三）Dapr：下一代 Service Mesh 的发展方向

当 Mesh 化技术在如火如荼的进行实践落地的时候，业界内又逐渐喊出了“将 Mesh 进行到底”的口号。

现在已经面世的几款优秀的 Servcie Mesh 产品主流定位大多是通讯层的透明代理，在网络层面解决问题，“将 Mesh 进行到底”却不仅仅局限于此。随着 Dapr 1.0 版本的发布，新一代 Service Mesh 架构的发展方向逐渐开始走进大家的视野。



# Dapr

## （一）Multi-Runtime（运行时）软件架构

Multi-Runtime（运行时）架构是一种未来架构趋势，Dapr 便是基于此架构，从需求出发，分布式应用的主要需求包括以下四大类：

- 生命周期：主要是弹性伸缩和异常快速恢复的诉求
- 网络：可靠的网络、可靠的路由的需求
- 状态：对于服务编排、服务调度、状态管理等需求
- 绑定：与外部系统、中间件的通讯的需求

![img](https://static001.geekbang.org/infoq/57/57eb79d84b45f9f18704782563f7156e.jpeg?x-oss-process=image/resize,p_80/auto-orient,1)

图6 Multi-Runtime软件架构

在此需求基础上，Service Mesh 架构将网络层抽出为独立的边车进程，而参考 Service Mesh 架构，Multi-Runtime 架构则是把各种边车提供的能力统一抽象成若干个 Runtime，这样应用从面向基础组件开发就演变成了面向各种分布式能力开发。

![img](https://static001.geekbang.org/infoq/51/5188dc089688907bf4efb2a1fbc617b9.jpeg?x-oss-process=image/resize,p_80/auto-orient,1)

图7 Multi-Runtime面向分布式能力开发

## （二）什么是 Dapr

Dapr（Distributed Application Runtime，分布式运行时），是微软内部团队的一个开源项目。Dapr 同样使用 Sidecar 架构，以独立进程的形式与应用程序同时运行，同时兼具 Service Mesh 中 Sidecar/proxy 的优点和高度可扩展的特性。

Service Mesh 的发展为我们指明了一个发展方向：将 SDK 中的分布式能力外移到独立的 Sidecar 中。但是我们可以想象一下，在未来是否可以将我们现在集成的中间件能力也外移到独立的 Sidecar 中呢？比如说将数据库 Mesh 化、将消息中间件 Mesh 化、将缓存 Mesh 化，将除了业务代码程序之外的全部 SDK 都 Mesh 化。

在蚂蚁金服，已经有团队开始将 Mesh 化推广到中间件领域，如图 8 所示的在 Mesh 层出现的 DB Mesh、Cache Mesh、Msg Mesh 等 Mesh 化形态的模块。

![img](https://static001.geekbang.org/infoq/3d/3d0c77ae55f11b30692b2d1a41568b23.png)

图8 更多的Mesh化模块

我们可以将这些独立出来的 Mesh 化模块统称为提供不同功能的“运行时”组件。

过多的“运行时”组件出现之后，应用程序在运行时就会依赖一个或多个这样的 Mesh 化模块。虽然做到了将 SDK 移出了应用程序，但是这种多依赖的结果显然不是我们所期望的形式。

Dapr 的出现将这些提供不同分布式能力以及中间件能力的“运行时”模块进行了整合，开发人员可以按照自己的需求，通过 yaml 文件的方式，将提供不同功能的组件整合到 Dapr 的构建块（Building Block）中。应用程序可以通过 Dapr 提供的标准 API，访问构建块来获得并使用这些能力。

![img](https://static001.geekbang.org/infoq/70/70835164473e0ba5b34f7a5fc1df433c.png)

图9 k8s中的Dapr

以图 9 为例，在 k8s 环境中，应用程序代码和 Dapr 的 Sidecar 分别运行在两个不同的容器中。应用程序代码可以通过标准的 HTTP/gRPC 协议使用 Dapr 提供的各种分布式能力，而 Dapr 中提供能力的构建块（Building Block）又是可以根据开发人员的需求进行可插拔和高度扩展的。

Dapr 的这种模式极大的提升了 Service Mesh 体系中 Sidecar 的灵活性，并对各种不同的 Mesh 进行了统一的整合。可以说 Dapr 的这种模式是未来 Service Mesh 未来发展的一种新方向。



## （三）Dapr 的功能和架构

Dapr 虽然也使用 Sidecar 架构，但是却提供了更多的能力和使用场景。

除了提供和 Service Mesh 一样的服务间远程调用（Service-to-service invocation）能力外，还提供了状态管理（State management）的能力来帮助开发人员构建出弹性的、有/无状态的应用程序，并且提供了发布订阅（Publish and subscribe）、资源绑定（Resource bindings）等额外的能力。

Dapr 中所有功能都是通过使用 Dapr 中的构建块（Building Block）来进行提供。图 10 中蓝色方框中的内容，都是 Dapr 目前已经提供给应用开发人员使用的构建块。

![img](https://static001.geekbang.org/infoq/77/77e92c1e5be1d4e9b9539ed313faf954.png)

图10 Dapr官方提供的构建块

在 Dapr 的架构中，有三个主要的组成部分：Dapr API、构建块（Building Block）和组件（Component），他们之间的关系如图 11 所示。

![img](https://static001.geekbang.org/infoq/8a/8a1f5dd900e55d94a8a0f266ecb5af2b.png)

图11 Dapr主要的组成部分

应用程序可以通过标准的 Dapr API 与构建块进行通信，构建块作为 Dapr 对外提供分布式能力的基本单元，将各种分布式能力进行了抽象，并将自己内部整合的分布式能力提供给应用程序。



组件（Component）是构建块的具体实现，每个构建块都是由一个或多个组件组成，并且所有的组件都是可插拔和高度扩展的。Dapr 内部有自己的一套 SPI 扩展机制，任何开源的或者商业化的产品都可以很方便的集成到一个组件中。



例如想要将一个 Redis 集成到 Dapr 中，只需要将 Redis 集成到一个 State 的组件中就可以很方便的在应用程序中通过 HTTP/gRPC 协议使用 Redis 的功能，而不需要在应用程序中依赖操作 Redis 的 SDK。

Dapr 的主要架构可以总结为以下三点：

1. Dapr API 通过标准的 HTTP/gRPC 协议对外暴露构建块的能力。
2. 构建块则对分布式能力进行一个抽象，并提供各种分布式运行时能力
3. 组件是构建块能力的具体实现者。



应用开发者只需要基于 Dapr 多语言的 SDK，并且面向能力的方式对 Dapr 进行编程，而底层的具体实现方式由 Dapr 以 yaml 文件的方式进行激活。应用无需感知到自己使用的分布式能力是由哪种方式实现的。Dapr 整体的功能图如图 12 所示。

![img](https://static001.geekbang.org/infoq/d5/d55a0d70171753d9a07cc876a7a3c89a.png)

图12 Dapr整体功能图

## （四）Dapr 的特性

Dapr 主要有两大特性：一个是跨语言、多运行环境支持（Any language，anywhere），一个是组件的可插拔、可替换。

应用开发者可以基于 Dapr 多语言的 SDK 面向 Dapr 的分布式能力进行编程，通过集成 Dapr 的 SDK，可以使用任何语言、任何框架构建自己的微服务应用，并将应用运行在任何有 Dapr 的环境中。如图 13 所示。

![img](https://static001.geekbang.org/infoq/ff/ff7bed6c60abbb4725fb1f9ee3c92c54.png)

图13 Dapr架构图

同时 Dapr 也可以部署在任何环境里面，包括自己本地的环境、边缘计算的场景、拥有 k8s 的环境或者是任何的商业化云产品开发厂商的环境中。

在非 k8s 的环境中，Dapr 和应用程序分别单独运行在自己的进程中，应用程序可以通过 Dapr API 与之进行通信，Dapr 自己则以一个代理的身份为应用程序提供各种分布式能力。

![img](https://static001.geekbang.org/infoq/0c/0c95e2e9d253ba7419b01deaa5e41e93.png)

图14 在非k8s环境中部署Dapr

在 k8s 的环境中，Dapr 和应用程序运行在同一个 Pod 里，但是在不同的容器中。Dapr 的构建块则分布在其他的 Pod 中，通过 yaml 文件的方式进行激活并让 Dapr 感知到。

![img](https://static001.geekbang.org/infoq/df/dff991a85c630ad5d0a728bc5b19f1d3.png)

图15 在k8s环境中部署Dapr

在构建微服务应用时，每个组件都是独立的。开发人员可以采用其中一个或多个或全部来构建应用，并且组件的更新对应用来说是无感知的，应用程序不会感知到底层组件的升级。这也就是 Dapr 的第二个特性：组件的可插拔、可替换特性。

Dapr 通过把一些构建微服务应用所需的最佳实践内置到开放、独立的构建块（building block）中，让开发人员只需专注于业务逻辑代码的编写，即可开发出功能强大的微服务应用，截至现在，Dapr 社区中已经有 70 多个组件可供开发人员进行使用。



# Dapr 与 Service Mesh

虽然 Dapr 和 Service Mesh 在架构上都是使用的 Sidecar 模式，并且在功能上也存在一些重叠部分，但是不能将 Dapr 简单的定义为 Service Mesh。

图 16 展示了 Dapr 和 Service Mesh 提供的重叠功能和独特功能。

![img](https://static001.geekbang.org/infoq/fc/fc6bf43a3d83160e4173371207f64bcc.png)

图16 Dapr与Service Mesh的比较

## （一）不同点

通过上面的讲解，我们可以明白，Service Mesh 主要专注于服务调用和网络问题，而 Dapr 是为了给应用服务提供更多的**分布式能力**而诞生的，两者的关注点在本质上就不一样。

Service Mesh 主要以基础设施为中心：

1. Service Mesh 更加聚焦于网络问题的处理，通过拦截网络流量，可以使应用程序无感知的部署在包含 Service Mesh 的环境中。
2. 并且 Service Mesh 主要由系统操作员进行管理和部署，使 Service Mesh 更像是一种特殊的“基础设施”。开发人员无需考虑一些其他的细节，因为 Service Mesh 已经将网络概念扁平化。
3. Service Mesh 通过按照原协议转发的方式来进行流量拦截，可以给业务系统带来零侵入的体验。



与 Service Mesh 不同，Dapr 是以开发人员为中心：

1. 当开发人员在代码中需要使某种分布式能力时，开发人员需要明确调用 Dapr API。Dapr 为开发者提供了标准的分布式 API，这种 API 带来了多语言的、面向能力的、统一的编程体验。
2. Dapr 提供了应用级别的构建块（Building Block）和 70 多种分布式能力的抽象集成，使得开发人员更容易将应用程序构建为弹性的微服务。
3. Dapr 通过采用多语言 SDK+标准 API+各种分布式能力的方式为应用程序提供服务。

## （二）相同点

如图 17 红框中所示，Dapr 与服务网格都有的一些常见功能包括：

- 基于 mTLS 加密的服务到服务安全通信
- 服务到服务的度量指标收集
- 服务到服务分布式跟踪
- 故障重试恢复能力

![img](https://static001.geekbang.org/infoq/c3/c3046d33165ded071c1e5e949f5edd52.png)

图17 Dapr与Service Mesh的重叠部分

Dapr 与 Service Mesh 虽然有异同点，具体选择哪种技术还是要取决于具体的需求。可以只选择两者中的某一种，也可以两者全部使用，同时使用它们是没有任何限制的。



# 前景展望

Dapr 为开发者提供了标准的 API，这种 API 带来了支持多语言的、面向能力的、统一的编程体验。

Dapr 目前仍处于不断迭代升级的状态，虽然社区的关注度很高，但是在将面向能力的 API 标准化并能持续发展的方向上 Dapr 还需要继续努力，在我们的调研实践中，我们总结了以下几个方面的期望和诉求：

（一）易用性

- 控制平面的建设，在目前的 Dapr 社区发展上来看，Dapr 仅支持在每个 Dapr 实例上进行配置，维护成本较高，参考业界 Istio、Linkerd 等开源框架建设控制平面，对所有配置进行统一纳管，统一下发。
- 异地多活的解决方案，目前 Dapr 仅支持 k8s 集群或虚拟机部署，暂时不能感知多集群、多机房环境，异地多活的解决方案目前处于缺失状态。我们期望 Dapr 社区能提出解决方案，这也是 Dapr 成熟落地不可或缺的一步。

（二）可靠性

- 逃生模式，其实对于所有 Service Mesh 架构的组件都有类似的需求，当 Dapr 处于异常情况下，如何尽快恢复业务应该也是落地 Dapr 必须考虑的问题之一。

（三）可扩展性

- 目前 Dapr 已经对接业界大多数开源中间件和支持常见的 Rpc 协议，但很多公司都存在自研 Rpc 协议或自研中间件的情况。我们期望 Dapr 能提供一套成熟的插件式开发框架，满足各类用户的个性化需求。



在如今这个云原生技术蓬勃发展的时代，以 Service Mesh 和 Dapr 为代表的将分布式能力持续下沉的技术还将会继续向前演进。k8s、Serverless 等云原生技术的发展，相信也会带动越来越多的公司进行云原生技术的转型，也会有更多的优秀产品在生产上进行落地。



拥抱云原生，我们需要一同努力探索。



# 参考文档

Dapr 官方文档：

http://docs.dapr.io/

Isio 官方文档：

https://istio.io/latest/zh/about/service-mesh/

Linkerd 官方文档：

https://linkerd.io/2.10/reference/architecture/

2021 阿里巴巴研发效能峰会直播-Dapr 在阿里云云原生的实践

https://developer.aliyun.com/topic/n2021

Alibaba 云原生技术专栏：

https://blog.csdn.net/alisystemsoftware/article/details/115332138

Conduit 和 Linkerd 的创建者威廉·摩根(William Morgan)的文章：

https://thenewstack.io/history-service-mesh/

ServiceMesher 社区文章：

https://www.servicemesher.com/awesome-servicemesh/

InfoQ 社区文章：

https://www.infoq.com/articles/multi-runtime-microservice-architecture/