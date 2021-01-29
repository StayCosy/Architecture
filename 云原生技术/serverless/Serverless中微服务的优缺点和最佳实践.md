# [Serverless中微服务的优缺点和最佳实践](https://www.kubernetes.org.cn/8687.html)



> Serverless是一种构建和管理基于微服务架构的完整流程，允许你在服务部署级别而不是服务器部署级别来管理你的应用部署。
>
> 它与传统架构的不同之处在于，完全由第三方管理，由事件触发，存在于无状态（Stateless）、暂存（可能只存在于一次调用的过程中）计算容器内。构建无服务器应用程序意味着开发者可以专注在产品代码上，而无须管理和操作云端或本地的服务器或运行时。
>
> Serverless真正做到了部署应用无需涉及基础设施的建设，自动构建、部署和启动服务。

**微服务的概念非常适合Serverless功能的结构，可以轻松实现不同服务在部署和运行时隔离。**在数据存储方面，使用诸如DynamoDB之类的数据库，还使得每个微服务都能拥有独立的数据库，并在需要独立扩展它们时变得更加容易。

在我们深入研究细节之前，请考虑微服务对于你的特定项目和团队而言，其好处是否大于其缺点。 请不要因为“微服务是趋势”，就要必须选择它。[单体架构(Monolith)](https://www.martinfowler.com/bliki/MonolithFirst.html)也有他的适用场景。

## Serverless中微服务的优势

### 1. 选择性地可伸缩性和并发性

Serverless功能使管理应用程序的并发性和可伸缩性变得容易。在微服务架构中，我们充分利用了这一点，每个微服务都可以根据需要具有自己的并发/可伸缩性设置。

这是有价值的：减轻DDoS攻击的可能性，减少无法控制的云账单的财务风险，更好地分配资源等等。

### 2. 细粒度的资源分配

通过选择性的可伸缩性和并发性，可以对资源分配优先级进行详细控制。

每个（微）服务可以根据其需求和目的具有不同级别的内存分配。面向客户的服务可以分配更高的内存，因为它将有助于缩短执行时间，提高响应速度。可以通过[优化的内存设置](https://medium.com/hackernoon/lower-your-aws-lambda-bill-by-increasing-memory-size-yep-e591ae499692)来部署对延迟不敏感的内部服务。

存储机制也是如此，DynamoDB或Aurora Serverless等数据库可以根据（微）服务的需求而具有不同级别的容量分配。

### 3. 松耦合

这是微服务的基本属性，这样它可以使具有不同用途的系统组件更容易[解耦](https://dashbird.io/blog/using-api-gateway-to-decouple-and-scale-serverless-architectures/)。

### 4. 支持多运行时环境

Serverless功能配置，部署和执行的简便性为基于多个运行时的系统提供了可能性。

尽管Node.js是后端Web应用程序中最流行的技术之一，但它不可能成为每个任务的最佳工具。但，对于数据密集型任务，预测分析和任何形式的机器学习，Python可能会成为你的选择。专用平台（例如[SageMaker](https://aws.amazon.com/sagemaker/)）则更适合于大型项目。

借助Serverless基础架构，在运维方面，你无需再花额外的精力就可以为常规后端项目选择Node.js，为数据密集型选择Python。当然，这将在代码维护和团队管理方面增加一些工作。

### 5. 开发团队的独立性

不同的开发人员或团队可以在各自的（微）服务上工作，修复错误，扩展功能等。

[AWS SAM](https://aws.amazon.com/serverless/sam/)，[Serverless](https://www.serverless.com/)之类的工具使得在操作方面也具有更大的独立性。 [AWS CDK constructs](https://docs.aws.amazon.com/cdk/latest/guide/constructs.html) 可实现更大的独立性，而无需牺牲更高级别的质量和运维标准。

## Serverless中微服务的缺点

### 1. 难以监视和调试

Serverless带来的许多挑战中，监视和调试是最有难度的。因为计算和存储系统分散在许多不同节点中，更不用说缓存等的其他服务了。

但是，有专业平台可以解决所有这些问题。

### 2. 可能会经历更多的冷启动

> 调用功能时，Lambda会检查microVM是否已激活。如果有空闲的microVM可用，它将用于服务新的传入请求。在这种特殊情况下，没有启动时间，因为microVM已经启动并且代码包已在内存中。这称为 热启动。
>
> 相反的方法-必须从头开始提供新的microVM来满足传入的请求-被称为 冷启动。

**当FaaS(Function as a Services)平台（例如Lambda）需要启动新的虚拟机来运行功能代码时，就会发生[冷启动](https://dashbird.io/knowledge-base/aws-lambda/cold-starts/?utm_source=dashbird-blog&utm_medium=article&utm_campaign=well-architected&utm_content=microservices-in-faas)。如果你的应用对延迟很敏感，则它们可能会出现问题，因为冷启动会在总启动时间中增加几百毫秒到几秒钟。**

因为在完成一个请求后，FaaS平台通常会将microVM闲置一段时间，然后在10-60分钟后关闭。因此，函数执行的频率越高，microVM越有可能运行传入的请求（避免冷启动）。

当我们将应用程序分散在数百或数千个（微）服务中时，我们还可能分散每个服务的调用时间，从而导致每个功能的调用频率降低，可能会经历更多的冷启动。

### 3. 其他缺点

微服务概念本身还具有其他固有的缺点。这些并不是与Serverless固有的联系。尽管如此，每个采用这种架构的团队都应努力降低其潜在的风险和成本：

- 确定服务边界并非易事
- 更广泛的攻击面
- 服务编排开销
- 同步计算和存储并不容易

## Serverless微服务的挑战和最佳实践

### 1. Serverless中微服务应该是多大

> 微服务（MicroService）是软件架构领域业另一个热门的话题。如果说微服务是以专注于单一责任与功能的小型功能块为基础，利用模组化的方式组合出复杂的大型应用程序，那么我们还可以进一步认为Serverless架构可以提供一种更加“代码碎片化”的软件架构范式，我们称之为Function as a Services（FaaS）。
>
> 而所谓的“函数”（Function）提供的是相比微服务更加细小的程序单元。例如，可以通过微服务代表为某个客户执行所有CRUD操作所需的代码，而FaaS中的“函数”可以代表客户所要执行的每个操作：创建、读取、更新，以及删除。当触发“创建账户”事件后，将通过AWS Lambda函数的方式执行相应的“函数”。从这一层意思来说，我们可以简单地将Serverless架构与FaaS概念等同起来。

在Serverless中，经常会将“Function as a Services（FaaS）”的概念与你所选择的编程语言中的函数语句混淆。

我们正在进入一个无法画出完美界限的领域，但是[经验](https://dev.to/rehanvdm/comment/121pa)表明，使用非常小的Serverless功能并不是一个好主意。

你应该记住的一件事是，当决定将（微）服务分拆为单独的功能时，你将不得不应对[Serverless困境](https://dashbird.io/knowledge-base/well-architected/serverless-trilemma/?utm_source=dashbird-blog&utm_medium=article&utm_campaign=well-architected&utm_content=microservices-in-faas)。只要有可能，将相关逻辑保持在单个功能中就会有很多好处。

决策过程也应考虑到拥有单独的微服务的优势。

如果我分拆这个微服务，

- 这将使不同的团队独立工作吗？
- 我可以从细粒度的资源分配或选择性可伸缩性功能中受益吗？

如果不是，则应该考虑将该服务与所需的组件等捆绑在一起。

### 2. 松耦合你的架构

通过[组成Serverless功能](https://dashbird.io/knowledge-base/well-architected/serverless-functions-composition-strategies/?utm_source=dashbird-blog&utm_medium=article&utm_campaign=well-architected&utm_content=microservices-in-faas)来协调微服务的方法有很多。

当需要同步通信时，可以进行直接调用（即[AWS Lambda RequestResponse调用方法](https://dashbird.io/knowledge-base/aws-lambda/invocation-methods-and-integrations/?utm_source=dashbird-blog&utm_medium=article&utm_campaign=well-architected&utm_content=microservices-in-faas#synchronous)），但这会导致高度耦合的架构。更好的选择是使用[Lambda Layers](https://dashbird.io/knowledge-base/aws-lambda/lambda-layers/?utm_source=dashbird-blog&utm_medium=article&utm_campaign=well-architected&utm_content=microservices-in-faas)或[HTTP API](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api.html)，这使得以后可以在不中断客户端请求的情况下，修改或迁移服务成为可能。

对于异步通信，我们有几种选择：消息队列（[SQS](https://dashbird.io/knowledge-base/sqs/intro-to-sqs-queue-service/)），主题通知（[SNS](https://aws.amazon.com/sns/)），[Event Bridge](https://dashbird.io/knowledge-base/event-bridge/intro-to-event-bridge/)或者[DynamoDB Streams](https://dashbird.io/knowledge-base/dynamodb/operations-and-data-access/?utm_source=dashbird-blog&utm_medium=article&utm_campaign=well-architected&utm_content=microservices-in-faas#streams)。

### 3. 组件隔离

理想情况下，微服务不应将实现细节暴露给用户。

诸如Lambda之类的Serverless平台已经提供了隔离功能的API。但这本身就会导致实现细节泄漏，理想情况下，我们将在功能之上添加一个不可知的HTTP API层，以使其真正隔离。

### 4. 使用并发限制策略

为了减轻DDoS攻击，在使用AWS API Gateway等服务时，请确保为每个面向公众的终端节点[设置单独的并发限制策略](https://dev.to/theburningmonk/the-api-gateway-security-flaw-you-need-to-pay-attention-to-44)。

此类服务在云平台中具有针对全局并发配额。如果你没有基于端点的限制，则攻击者只需将一个端点作为目标即可耗尽你的资源配额并使整个系统瘫痪。

## 总结

无论你是迁移旧系统还是从头开始构建某些产品，确保其按预期顺利运行都是一个持续的挑战。在本文中，我们研究了Serverless的优点和缺点，Serverless的微服务挑战和最佳实践等等。