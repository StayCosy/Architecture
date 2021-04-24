## 独家对话阿里云函数计算负责人不瞋：你所不知道的 Serverless

> Serverless 其实离我们并没有那么遥远。



如果你是一名互联网研发人员，那么极有可能了解并应用过 Serverless 这套技术体系。纵观 Serverless 过去十年，它其实因云而生，也在同时改变云的计算方式。如果套用技术成熟度曲线来描述的话，那么它已经走过了萌芽期、认知破灭期，开始朝着成熟稳定的方向发展。未来，市场对 Serverless 的接受程度将越来越高。



不要惊讶，阿里云团队在真正开始构建 Serverless 产品体系的最开始的一两年里，也曾遭遇内部的一些争议。而今，单从阿里集团内部的很多业务线来看，已经在朝着 Serverless 化的方向发展了。



日前，阿里云凭借函数计算产品能力全球第一的优势，入选 Forrester 2021 年第一季度 FaaS 平台评估报告，成为比肩亚马逊成为全球前三的 FaaS 领导者。这也是首次有国内科技公司进入 FaaS 领导者象限。



![图片](https://mmbiz.qpic.cn/mmbiz_jpg/yvBJb5Iiafvk1Nvx7Zib0LnE5icuCTibM1xzZibpXlhoZ60icQ1ysiaWW0iaIpnz8e2bmJJR7icAf5vDDHEYABqvVCl1wEA/640?wx_fmt=jpeg&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)



在与雷锋网的访谈中，阿里云 Serverless 负责人不瞋阐释了 Serverless 的演进历程、引入 Serverless 面临的难点与挑战、以及有关云原生的趋势预判。



> “一定要想明白做这件事的终局是什么，包括产品体系的定位，对开发者、对服务商的价值等等这些问题。这要求我们不断通过实践和认识的深化，让这些问题的回答能够逐渐清晰起来。这也是我们这么多年实践积累的宝贵经验。”不瞋指出。



尽管企业的实践还存在种种疑惑和挑战，但 Serverless 实际上离我们并没有那么遥远。举一个最近的例子，新冠疫情让远程办公、在线教育、在线游戏的应用需求短期内增加。业务规模的爆发式增长，对每一个需求的响应需要更加及时，这对应用架构的弹性，对底层计算的速度，对研发效率的提升等，都要求业务加速向新技术架构演进。



而不瞋的理想就是，帮助更广泛的客户实现向新技术架构的平滑迁移，让 Serverless 渗透到所有的云应用中。



不瞋作为阿里云 Serverless 产品体系的负责人，也是国内 Serverless 的早期实践者。以下将呈现是对这次访谈的完整总结。



![图片](https://mmbiz.qpic.cn/mmbiz_gif/US10Gcd0tQGY9ddd5GpbmVRuaRfuaESAUBGE7uHX5G0nxxLSub2QTKZdu538V7GaHXS5jsTCebYCUibaHsjg0ow/640?wx_fmt=gif&tp=webp&wxfrom=5&wx_lazy=1)

**Serverless 的定义**



在讨论之前，我们先明确 Serverless 的定义，确保大家对 Serverless 的认知是一致的。



现在 Serverless 越来越热，无论是工业界还是学术界，都将 Serverless 视为云计算发展的下一阶段。Serverless 有很多种表述，其中伯克利大学的定义相对严谨一些。



> 注：2019 年 2 月，加州大学伯克利分校发表的《Cloud Programming Simplified: A Berkerley View on Serverless Computing》论文，曾在业界引发诸多讨论和关注。



大致来讲，Serverless 实际对应的是一整套的产品体系，而不是单独一两个产品；同时，这些产品/服务之间还具备以下特征：服务之间彼此配合、全托管、用户通过 API 调用就可完成整个功能或应用的开发而无需关注底层基础设施。



这套产品体系目前可分为两类：一类是计算，即 FaaS（Function as a Service）；还有一类是 BaaS（Backend as a Service），比如消息中间件、对象存储，都可以看做是 Serverless 化的 BaaS 服务。 



![图片](https://mmbiz.qpic.cn/mmbiz_gif/US10Gcd0tQGY9ddd5GpbmVRuaRfuaESAUBGE7uHX5G0nxxLSub2QTKZdu538V7GaHXS5jsTCebYCUibaHsjg0ow/640?wx_fmt=gif&tp=webp&wxfrom=5&wx_lazy=1)

**Serverless 的演进**



一个新技术通常会经历几个阶段：



- **第一个阶段**，是因为其巨大潜力引起广泛关注的阶段。
- **第二阶段**，是认知破灭的阶段，在这个阶段由于产品初期本身能力不是很强健，或案例不全等因素，导致用户在使用过程中往往会遇到挫败感。
- **第三个阶段**，是伴随实践的增加和产品能力本身的发展，又会逐步提升认知，进而进入一个稳健增长的阶段。



![图片](https://mmbiz.qpic.cn/mmbiz_jpg/yvBJb5Iiafvk1Nvx7Zib0LnE5icuCTibM1xzibceWambl6iceMia2picnRwOzRA23fxFribsibibZ4gDdC49E7M4XXDOibYVrw/640?wx_fmt=jpeg&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)



需要明确的是，Serverless 并不是一个非常新的技术。像阿里云的 OSS、AWS 的 S3 对象存储，它们都是最早发布的产品之一，一开始其实就是 Serverless 的形态。



但业界对 Serverless 的认知，确实是因 AWS 的 Lambda 带起来的，2014 年 AWS 推出了 Lambda。



2017 年到 2019 年上半年，这段时间，业界对 Serverless 的讨论很多同时又有很多困扰，不知道如何落地，或者用了之后才突然觉得跟自己想象的不太一样。



国内外技术发展保持着相似的节奏，国外相对来讲更快一些。从去年开始，国内也开始进入到了稳定发展的阶段。现在国际上主流云供应商提供的新功能或新产品，80% 以上都是 Serverless 的形态。



阿里云从 2017 年开始打造 Serverless，并于当年正式启动商业化。



目前在阿里集团内部已经开始落地 Serverless 了，例如飞猪、淘宝、高德等等。在企业赋能方面，尤其是疫情之后，能够看到用户对 Serverless 的认知比之前确实深入了许多，在很多场景下，切换到 Serverless 架构确实能够为用户带来明显的收益，用户也认可这项技术。



举一项数据来看，目前阿里云 Serverless 已经服务了上万家付费客户，拥有 100+ 的典型案例，函数日调用量超过 120 亿次、函数总量达到 100 万。



![图片](https://mmbiz.qpic.cn/mmbiz_gif/US10Gcd0tQGY9ddd5GpbmVRuaRfuaESAUBGE7uHX5G0nxxLSub2QTKZdu538V7GaHXS5jsTCebYCUibaHsjg0ow/640?wx_fmt=gif&tp=webp&wxfrom=5&wx_lazy=1)

**新旧观念的转变**



对于阿里云自身而言，在最开始构建 Serverless 之初，其实最大的挑战不仅仅是技术层面的，更多的还有观念上的不对称。



首先，Serverless 本身的形态跟以往的计算形态差异比较大，整个研发和运维的体系跟传统应用是割裂的。如果开发 Serverless 应用，其研发运维的流程和工具跟虚拟化（VM）或容器化的方式不太一样，**很多用户会担心供应商锁定（lock-in）的问题，不太希望自身的技术栈跟某个供应商绑定**。



其次，AWS 的 Lambda 最开始做了一个榜样，但它也实际也只适合于 AWS 的产品体系，如果放在其他的产品体系里会面临非常大的挑战，不易于被用户接受，且限制条件也很多，应用场景也有限。这就要求在技术层面，包括资源调度、安全隔离、多租户管理、流控等方面有很高要求，做起来非常辛苦。因为在此之前没有一个产品的计算形态是如此细粒度、动态的使用资源。



这种挑战，一开始即便在阿里内部，也曾面临过许多争议。



我们这么多年实践积累的宝贵经验是：一定要想明白做这件事的终局是什么，包括在产品体系中的定位，对开发者、对云服务商的价值等等这些问题。这要求我们不断通过实践和认识的深化，让这些问题的回答能够逐渐清晰起来。



![图片](https://mmbiz.qpic.cn/mmbiz_png/yvBJb5Iiafvk1Nvx7Zib0LnE5icuCTibM1xzvCEVLepqibKQXHz7fQsCiaVm0rYXrqDVs1icsKJNomC4PhaYPA2ytMGicA/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)



![图片](https://mmbiz.qpic.cn/mmbiz_gif/US10Gcd0tQGY9ddd5GpbmVRuaRfuaESAUBGE7uHX5G0nxxLSub2QTKZdu538V7GaHXS5jsTCebYCUibaHsjg0ow/640?wx_fmt=gif&tp=webp&wxfrom=5&wx_lazy=1)

**引入 Serverless 的顾虑**



站在客户层面，不同类型的客户对引入第三方的 Serverless 技术其实会有不同层面的考虑。



对于超大型企业，比如 Facebook、字节跳动，企业本身就有非常强的基础设施团队，通常他们会选择自己内部开发这方面技术。



还有一些企业，没有采用 Serverless 并不是说他们对这个技术有什么抵触，而是**当下的落地实践或本身的工具链还无法做到完全消除供应商锁定的问题，又或者是因为工具链跟传统开发太过割裂，企业自身无法同时维护两套开发框架**。



这种情况下，用户的系统架构一定会面临一个中间状态：既有老的又有新的。如果整个迁移的过程不是那么平滑的话，供应商的这部分优势在客户那里是不存在的， 因为老的系统实际是需要维护的。如此，对用户的吸引力其实就没有那么大了。



阿里云最近开源的 Serverless Devs 解决的就是这样的问题。其定位是帮助用户更简单地开发和运维自己的 Serverless 化和容器化应用，提供应用全生命周期管理的能力。



本质上，Serverless 的环境是在远端，跟用户本地开发环境是天然割裂的，那么在这个过程中，从调试、部署、发布、监控等各个环节，Serverless Devs 都希望能为用户提供更好的体验。但用户可自由使用其中一个或几个功能，不需要将已有的研发运维的流程完全迁移到我们定义的这套规范里。



![图片](https://mmbiz.qpic.cn/mmbiz_gif/US10Gcd0tQGY9ddd5GpbmVRuaRfuaESAUBGE7uHX5G0nxxLSub2QTKZdu538V7GaHXS5jsTCebYCUibaHsjg0ow/640?wx_fmt=gif&tp=webp&wxfrom=5&wx_lazy=1)

**过去一年的重大升级**



2020 年，疫情的背景下，其实也是阿里云 Serverless 技术升级的关键一年。这一年里，团队做了很多大的升级，包括：



- 架构层面，已经升级到神龙裸金属服务器+袋鼠安全容器的下一代架构。好处是能够带来非常高的计算密度，进一步提升弹性能力和性能。

 

- 缓存方面，发布容器镜像加速技术，能够让GB级别的容器镜像非常快地实现秒级启动。目前已经演进到了下一代，通过阿里内部大规模业务场景进行打磨。

 

- 运行时方面，去年阿里云重写整个语言运行时，使得更具有可扩展性，启动速度更快。



![图片](https://mmbiz.qpic.cn/mmbiz_png/yvBJb5Iiafvk1Nvx7Zib0LnE5icuCTibM1xzLB9LhhKAAAUicHXRllsqtexUhvOXQlWPNTHQKXianGSGfMXicicWsrJIDw/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

阿里云函数计算全景图



总结起来，两方面因素推动阿里云Serverless在过去一年做出重大技术升级：



一是来自用户本身的诉求。比如在教育场景中，老师对开课这件事是有时效性要求的，这就要求后台能够短时间内启动可能数千个实例进行响应。



二是来自内部对产品效能的要求。对于云服务商而言，Serverless 最核心的一个定位，是能够将云上资源更好地利用起来。整个计算架构确实需要通过新的虚拟化技术、容器技术，同时跟新的硬件结合起来，从而提供一个非常细粒度的、启动非常快、非常弹性的计算模型。这也是为什么我们要进行架构升级，从原来的虚拟机架构演进到神龙裸金属服务器+袋鼠安全容器的架构，将对整体产品的发展产生一个核心推力。



![图片](https://mmbiz.qpic.cn/mmbiz_gif/US10Gcd0tQGY9ddd5GpbmVRuaRfuaESAUBGE7uHX5G0nxxLSub2QTKZdu538V7GaHXS5jsTCebYCUibaHsjg0ow/640?wx_fmt=gif&tp=webp&wxfrom=5&wx_lazy=1)

**攻克下一城**



阿里云采用“三位一体”的策略打造整个 Serverless 产品矩阵——自身实践-开源-商业化。即通过集团内部超大规模、超复杂的业务场景来锤炼技术，将技术不断打磨产品化，然后对云上客户提供商业化服务，在这个过程中，还会将一些技术、工具进行开源，遵循开源开放的标准，跟开源生态融合。



只有对客户的业务产生价值和帮助，客户才会认可 Serverless。



短期来看，无论是业务规模，还是产品、技术层面，阿里云 Serverless 都在以非常稳健地方式按照自身的节奏向前演进。



- 一是业务规模会更大，预计每年会有三倍以上的增长。
- 二是产品层面，以客户为中心，解决用户痛点仍然是首要的。今年将在产品细节体验上继续补强，在工具链、可观测性等方面为用户提供更好的体验。
- 三是技术层面，包括计算、网络、缓存、运行时等核心部分，继续夯实技术细节，实现极致性能。



![图片](https://mmbiz.qpic.cn/mmbiz_gif/US10Gcd0tQGY9ddd5GpbmVRuaRfuaESAUBGE7uHX5G0nxxLSub2QTKZdu538V7GaHXS5jsTCebYCUibaHsjg0ow/640?wx_fmt=gif&tp=webp&wxfrom=5&wx_lazy=1)

**云时代的新机遇**



在应用场景上来看，Serverless 不再仅仅是小程序，还有电商大促、音视频转码、AI 算法服务、游戏应用包分发、文件实时处理、物联网数据处理、微服务等场景。



Serverless 将继续和容器、微服务等生态融合，降低开发者使用 Serverless 技术的门槛，反过来也将促进传统应用的云原生化。



Serverless 另一个核心要素是“被集成”，被集成的对象有两类：



- 一类跟一方云服务进行接入，阿里云函数计算已被 30 多个一方云服务产品集成。

 

- 第二类是通过 EventBridge 事件总线和三方生态被集成。例如和钉钉等 SaaS 应用集成。钉钉的业务中常常需要以简洁、轻量的方式完成用户的定制化需求，这和 Serverless 的应用形态是高度匹配的。

今天，我们可以非常明确地看到，整个云的未来一定是 Serverless 形态的。阿里云内部对这个也没有争议，因为这么多年来，整个产品体系就是朝着 Serverless 方向发展的。



不是因为有了 Serverless 计算，云才向 Serverless 演进。恰恰相反，因为云的产品体系已经向 Serverless 演进，才催生了 Serverless 计算。单纯的 Serverless 计算并不能实现很多功能，前提一定是跟其他云服务及其生态配合，才能体现出其自身的优势。



无论是工业界还是学术界，都已经认可这样一个趋势。 