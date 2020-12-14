# 新一代MQ apache pulsar的架构与核心概念



## Pulsar基本架构

- Pulsar采用存储计算分离的架构，pulsar使用了bookkeeper做消息的存储，bookkeeper保证了消息存储的可靠性和高效性，bookkeeper为pulsar提供了存储的扩展能力
- Pulsar使用zk做元数据存储
- 多租户，pulsar最初的设计就是支持多租户的
- 命名空间：一个租户可以有多个命名空间，一个topic属于一个命名空间，pulsar中的配置都是以命名空间为单位配置的



![img](https://pic4.zhimg.com/80/v2-7392a38c3dd0c81b7ec972cb925e7f63_720w.jpg)

- Pulsar的broker用于处理消息的读写，broker中会有消息的本地缓存，因为多数场景下，消息被写入后会立刻被消费，因此broker中持有的新消息的缓存能非常有效的提高性能和MQ的整体吞吐

![img](https://pic3.zhimg.com/80/v2-d8fb68733adf3b8c939d09c269246542_720w.jpg)

相比kafka、rocketmq等MQ，pulsar基于bookkeeper的存储计算分离架构，使得pulsar的消息存储可以独立于broker而扩展。

## ACK

当一个消息被消费者消费后，pulsar会给broker发送一个ack，pulsar有三种消息的ack模式：

- One by One：依次确认每一个消息，保证确认的顺序
- Cumulative：累积的方式确认，只需要确认一条消息，用于表示这条消息以及之前的消息都已确认
- 每个消息独立确认：shared消费模式下可独立确认每一个消息

## 消息订阅

Pulsar支持exclusive、shared和failover三种消息订阅模式，这三种模式的示意图如下：



![img](https://pic1.zhimg.com/80/v2-bd42369aec778ea58d4e3488f1ad9e24_720w.jpg)

Exclusive模式（独占模式）是pulsar默认的消息订阅模式，在这种模式下，中能有一个consumer消息消息，一个订阅关系中只能有一台机器消费每个topic，如果有多于一个consumer消费此topic则会出错，消费示意图如下：

![img](https://pic3.zhimg.com/80/v2-a16104d3994a0aaf676c47c99eaf5786_720w.jpg)

Failover模式下，一个topic也是只有单个消费消费一个订阅关系的消息，与exclusive模式不同之处在于，failover模式下，每个消费者会被排序，当前面的消费者无法连接上broker后，消息会由下一个消费者消费，消费示意图如下：

![img](https://pic3.zhimg.com/80/v2-973865f04b78a8129e993b776c8ef3ce_720w.jpg)

Shared模式（共享模式）下，消息可被多个consumer同时消费，这种模式下，无法保证消息的顺序，并且无法使用one by one和cumulative的ack模式，消息通过roundrobin的方式投递到每一个消费者，消费示意图如下：

![img](https://pic3.zhimg.com/80/v2-3a4cf9ebed5ade715cb56f9c8868207e_720w.jpg)

key_shared模式是shared模式的一种，不同的是它按key对消息做投递，相同的key的消息会被投递到同一个消费者上，消费示意图如下：

![img](https://pic4.zhimg.com/80/v2-58186abfc97a00bc2e636cdf7c061d53_720w.jpg)

## 消息分区

单个topic的消息一般是由单个broker处理，为了提高topic的消息处理能力，pulsar提供了partitioned topic的支持，与kafka和rocketmq一样，每个partition由不同的broker处理，在消费时，单个partition可选择exclusive, failover和shared模式

Partitioned topic实际上是由n（partition的数量）个内部的topic组成的，每个内部的topic由一个broker处理，每个broker可处理多个topic，当消息发送到broker前，在producer端会通过routing mode将消息路由到某一个partition上，消息的生产与消费示意图如下：

![img](https://pic3.zhimg.com/80/v2-1e6b13338d5451ea072d1bfb4cfaaa26_720w.jpg)

## 消息的存储与过期

默认情况上，当broker会立刻删除所有收到了ack的消息，没有被ack的消息会持久化存储，但是我们可以修改pulsar的行为，pulsar允许我们存储已经收到ack了的消息，也可以给未收到ack的消息设置过期时间（TTL）

## 消息去重

Pulsar支持在broker端对消息做去重，当打开消息去重后，重发的消息（重试等产生的）不会被重新存储，这个特性使得pulsar对流式计算引擎（例如flink）更加友好，流式计算引擎更容易实现exactly-once语义的计算任务，消息去重的存储示意图如下：

![img](https://pic4.zhimg.com/80/v2-523105a710d7ba83adaa2c36fc832c2b_720w.jpg)

## 消息存储

Pulsar使用apache bookkeeper做消息存储，bookkeeper介绍：https://zhuanlan.zhihu.com/p/88376398