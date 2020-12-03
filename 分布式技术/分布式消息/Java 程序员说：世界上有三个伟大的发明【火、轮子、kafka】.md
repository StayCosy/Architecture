# Java 程序员说：世界上有三个伟大的发明【火、轮子、kafka】

 

1. Apache Kafka 是一个开源 **「消息」** 系统，由 Scala 写成。是由 Apache 软件基金会开发的 一个开源消息系统项目。
2. Kafka 最初是由 LinkedIn 公司开发，用作 LinkedIn 的活动流（Activity Stream）和运营数据处理管道（Pipeline）的基础，现在它已被多家不同类型的公司作为多种类型的数据管道和消息系统使用。
3. **「Kafka 是一个分布式消息队列」**。Kafka 对消息保存时根据 Topic 进行归类，发送消息 者称为 Producer，消息接受者称为 Consumer，此外 kafka 集群有多个 kafka 实例组成，每个 实例(server)称为 broker。
4. 无论是 kafka 集群，还是 consumer 都依赖于 **「Zookeeper」** 集群保存一些 meta 信息， 来保证系统可用性。

# 二、为什么要有 Kafka?

**「kafka」** 之所以受到越来越多的青睐，与它所扮演的三大角色是分不开的的：

- **「消息系统」**：kafka与传统的消息中间件都具备系统解耦、冗余存储、流量削峰、缓冲、异步通信、扩展性、可恢复性等功能。与此同时，kafka还提供了大多数消息系统难以实现的消息顺序性保障及回溯性消费的功能。
- **「存储系统」**：kafka把消息持久化到磁盘，相比于其他基于内存存储的系统而言，有效的降低了消息丢失的风险。这得益于其消息持久化和多副本机制。也可以将kafka作为长期的存储系统来使用，只需要把对应的数据保留策略设置为“永久”或启用主题日志压缩功能。
- **「流式处理平台」**：kafka为流行的流式处理框架提供了可靠的数据来源，还提供了一个完整的流式处理框架，比如窗口、连接、变换和聚合等各类操作。

Kafka特性分布式具备经济、快速、可靠、易扩充、数据共享、设备共享、通讯方便、灵活等，分布式所具备的特性高吞吐量同时为数据生产者和消费者提高吞吐量高可靠性支持多个消费者，当某个消费者失败的时候，能够自动负载均衡离线能将消息持久化，进行批量处理解耦作为各个系统连接的桥梁，避免系统之间的耦合

# 三、Kafka 基本概念

在深入理解 Kafka 之前，可以先了解下 Kafka 的基本概念。

一个典型的 Kafka 包含若干Producer、若干 Broker、若干 Consumer 以及一个 Zookeeper 集群。Zookeeper 是 Kafka 用来负责集群元数据管理、控制器选举等操作的。Producer 是负责将消息发送到 Broker 的，Broker 负责将消息持久化到磁盘，而 Consumer 是负责从Broker 订阅并消费消息。Kafka体系结构如下所示：

![img](https://static001.geekbang.org/infoq/9b/9b5411c9c196d302c918196b04f7ba15.png)

# 概念一：生产者（Producer）与消费者（Consumer）

![img](https://static001.geekbang.org/infoq/44/440192680a5632a85dc67decc4238c7a.png)

生产者和消费者

对于 Kafka 来说客户端有两种基本类型：**「生产者」**（Producer）和 **「消费者」**（Consumer）。除此之外，还有用来做数据集成的 Kafka Connect API 和流式处理的**「Kafka Streams」** 等高阶客户端，但这些高阶客户端底层仍然是生产者和消费者API，只不过是在上层做了封装。

- **「Producer」** ：消息生产者，就是向 Kafka broker 发消息的客户端；
- **「Consumer」** ：消息消费者，向 Kafka broker 取消息的客户端；

# 概念二：Broker 和集群（Cluster）

一个 Kafka 服务器也称为 **「Broker」**，它接受生产者发送的消息并存入磁盘；Broker 同时服务消费者拉取分区消息的请求，返回目前已经提交的消息。使用特定的机器硬件，一个 Broker 每秒可以处理成千上万的分区和百万量级的消息。

若干个 Broker 组成一个 **「集群」**（**「Cluster」**），其中集群内某个 Broker 会成为集群控制器（Cluster Controller），它负责管理集群，包括分配分区到 Broker、监控 Broker 故障等。在集群内，一个分区由一个 Broker 负责，这个 Broker 也称为这个分区的 Leader；当然一个分区可以被复制到多个 Broker 上来实现冗余，这样当存在 Broker 故障时可以将其分区重新分配到其他 Broker 来负责。下图是一个样例：

![img](https://static001.geekbang.org/infoq/f3/f38070afc792a2cd89b3cfa1d7121bbb.png)

Broker 和集群（Cluster）

# 概念三：主题（Topic）与分区（Partition）

![img](https://static001.geekbang.org/infoq/b6/b6ffb37e7ff461ef1464390f629b86a3.png)

主题（Topic）与分区（Partition）

在 Kafka 中，消息以 **「主题」**（**「Topic」**）来分类，每一个主题都对应一个「**「消息队列」**」，这有点儿类似于数据库中的表。但是如果我们把所有同类的消息都塞入到一个“中心”队列中，势必缺少可伸缩性，无论是生产者/消费者数目的增加，还是消息数量的增加，都可能耗尽系统的性能或存储。

我们使用一个生活中的例子来说明：现在 A 城市生产的某商品需要运输到 B 城市，走的是公路，那么单通道的高速公路不论是在「A 城市商品增多」还是「现在 C 城市也要往 B 城市运输东西」这样的情况下都会出现「吞吐量不足」的问题。所以我们现在引入 **「分区」**（**「Partition」**）的概念，类似“允许多修几条道”的方式对我们的主题完成了水平扩展。

# 四、Kafka 工作流程分析

![img](https://static001.geekbang.org/infoq/53/5353034f4adb125f5144907fac9f2068.jpeg?x-oss-process=image/resize,p_80/auto-orient,1)

# 4.1 Kafka 生产过程分析

# 4.1.1 写入方式

producer 采用推（push）模式将消息发布到 broker，每条消息都被追加（append）到分区（patition）中，属于顺序写磁盘（顺序写磁盘效率比随机写内存要高，保障 kafka 吞吐率）

# 4.1.2 分区（Partition）

消息发送时都被发送到一个 topic，其本质就是一个目录，而 topic 是由一些 Partition Logs(分区日志)组成，其组织结构如下图所示：

![img](https://static001.geekbang.org/infoq/df/dfbc0144eb8ff37ae9e27b745b89d8fd.jpeg?x-oss-process=image/resize,p_80/auto-orient,1)

我们可以看到，每个 Partition 中的消息都是 **「有序」** 的，生产的消息被不断追加到 Partition log 上，其中的每一个消息都被赋予了一个唯一的 **「offset」** 值。

**「1）分区的原因」**

1. 方便在集群中扩展，每个 Partition 可以通过调整以适应它所在的机器，而一个 topic 又可以有多个 Partition 组成，因此整个集群就可以适应任意大小的数据了；
2. 可以提高并发，因为可以以 Partition 为单位读写了。

**「2）分区的原则」**

1. 指定了 patition，则直接使用；
2. 未指定 patition 但指定 key，通过对 key 的 value 进行 hash 出一个 patition；
3. patition 和 key 都未指定，使用轮询选出一个 patition。

```
DefaultPartitioner 类 
public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) { 
 List<PartitionInfo> partitions = cluster.partitionsForTopic(topic); 
 int numPartitions = partitions.size(); 
 if (keyBytes == null) {
   int nextValue = nextValue(topic); 
   List<PartitionInfo> availablePartitions = cluster.availablePartitionsForTopic(topic);
   if (availablePartitions.size() > 0) { 
   int part = Utils.toPositive(nextValue) % availablePartitions.size(); 
   return availablePartitions.get(part).partition();
    } else { 
    // no partitions are available, give a non-available partition 
    return Utils.toPositive(nextValue) % numPartitions; 
    } 
    } else { 
    // hash the keyBytes to choose a partition 
    return Utils.toPositive(Utils.murmur2(keyBytes)) % numPartitions; 
    }
 }
﻿
```

# 4.1.3 副本（Replication）

同 一 个 partition 可 能 会 有 多 个 replication （ 对 应 server.properties 配 置 中 的 default.replication.factor=N）。没有 replication 的情况下，一旦 broker 宕机，其上所有 patition 的数据都不可被消费，同时 producer 也不能再将数据存于其上的 patition。引入 replication 之后，同一个 partition 可能会有多个 replication，而这时需要在这些 replication 之间选出一 个 leader，producer 和 consumer 只与这个 leader 交互，其它 replication 作为 follower 从 leader 中复制数据。

# 4.1.4 写入流程

producer 写入消息流程如下：

![img](https://static001.geekbang.org/infoq/91/91de9b7ca38179426f6a71fd57a700ad.png)

1）producer 先从 zookeeper 的 "/brokers/.../state"节点找到该 partition 的 leader ；

2）producer 将消息发送给该 leader ；

3）leader 将消息写入本地 log ；

4）followers 从 leader pull 消息，写入本地 log 后向 leader 发送 ACK ；

5）leader 收到所有 ISR 中的 replication 的 ACK 后，增加 HW（high watermark，最后 commit 的 offset）并向 producer 发送 ACK ；

# 4.2 Broker 保存消息

# 4.2.1 存储方式

物理上把 topic 分成一个或多个 patition（对应 server.properties 中的 num.partitions=3 配 置），每个 patition 物理上对应一个文件夹（该文件夹存储该 patition 的所有消息和索引文 件），如下：

```
[root@hadoop102 logs]$ ll 
drwxrwxr-x. 2 demo demo 4096 8 月 6 14:37 first-0 
drwxrwxr-x. 2 demo demo 4096 8 月 6 14:35 first-1 
drwxrwxr-x. 2 demo demo 4096 8 月 6 14:37 first-2 
﻿
[root@hadoop102 logs]$ cd first-0 
[root@hadoop102 first-0]$ ll 
-rw-rw-r--. 1 demo demo 10485760 8 月 6 14:33 00000000000000000000.index 
-rw-rw-r--. 1 demo demo 219 8 月 6 15:07 00000000000000000000.log 
-rw-rw-r--. 1 demo demo 10485756 8 月 6 14:33 00000000000000000000.timeindex 
-rw-rw-r--. 1 demo demo 8 8 月 6 14:37 leader-epoch-checkpoint
﻿
```

# 4.2.2  存储策略

无论消息是否被消费，kafka 都会保留所有消息。有两种策略可以删除旧数据：

- 基于时间：log.retention.hours=168
- 基于大小：log.retention.bytes=1073741824

需要注意的是，因为 Kafka 读取特定消息的时间复杂度为 O(1)，即与文件大小无关， 所以这里删除过期文件与提高 Kafka 性能无关。

# 4.2.3 Zookeeper 存储结构

![img](https://static001.geekbang.org/infoq/b6/b66721517465af49c786749e1ca1aca3.png)

注意：producer 不在 zk 中注册，消费者在 zk 中注册。

# 4.3 Kafka 消费过程分析

kafka 提供了两套 consumer API：高级 Consumer API 和低级 Consumer API。

# 4.3.1 高级 API

**「1）高级 API 优点」**

- 高级 API 写起来简单
- 不需要自行去管理 offset，系统通过 zookeeper 自行管理。
- 不需要管理分区，副本等情况，系统自动管理。
- 消费者断线会自动根据上一次记录在 zookeeper 中的 offset 去接着获取数据（默认设置 1 分钟更新一下 zookeeper 中存的 offset）
- 可以使用 group 来区分对同一个 topic 的不同程序访问分离开来（不同的 group 记录不同的 offset，这样不同程序读取同一个 topic 才不会因为 offset 互相影响）

**「2）高级 API 缺点」**

- 不能自行控制 offset（对于某些特殊需求来说）
- 不能细化控制如分区、副本、zk 等

# 4.3.2 低级 API

**「1）低级 API 优点」**

- 能够让开发者自己控制 offset，想从哪里读取就从哪里读取。
- 自行控制连接分区，对分区自定义进行负载均衡
- 对 zookeeper 的依赖性降低（如：offset 不一定非要靠 zk 存储，自行存储 offset 即可， 比如存在文件或者内存中）

**「2）低级 API 缺点」**

- 太过复杂，需要自行控制 offset，连接哪个分区，找到分区 leader 等。

# 4.3.3 消费者组

![img](https://static001.geekbang.org/infoq/79/798a6c627cb84378ed9b4d0ecabe2c53.png)

消费者是以 consumer group 消费者组的方式工作，由一个或者多个消费者组成一个组， 共同消费一个 topic。每个分区在同一时间只能由 group 中的一个消费者读取，但是多个 group 可以同时消费这个 partition。在图中，有一个由三个消费者组成的 group，有一个消费者读取主题中的两个分区，另外两个分别读取一个分区。某个消费者读取某个分区，也可以叫做某个消费者是某个分区的拥有者。

在这种情况下，消费者可以通过水平扩展的方式同时读取大量的消息。另外，如果一个消费者失败了，那么其他的 group 成员会自动负载均衡读取之前失败的消费者读取的分区。

# 4.3.4 消费方式

consumer 采用 pull（拉）模式从 broker 中读取数据。

push（推）模式很难适应消费速率不同的消费者，因为消息发送速率是由 broker 决定的。它的目标是尽可能以最快速度传递消息，但是这样很容易造成 consumer 来不及处理消息，典型的表现就是拒绝服务以及网络拥塞。而 pull 模式则可以根据 consumer 的消费能力以适当的速率消费消息。

对于 Kafka 而言，pull 模式更合适，它可简化 broker 的设计，consumer 可自主控制消费 消息的速率，同时 consumer 可以自己控制消费方式——即可批量消费也可逐条消费，同时还能选择不同的提交方式从而实现不同的传输语义。

pull 模式不足之处是，如果 kafka 没有数据，消费者可能会陷入循环中，一直等待数据 到达。为了避免这种情况，我们在我们的拉请求中有参数，允许消费者请求在等待数据到达 的“长轮询”中进行阻塞（并且可选地等待到给定的字节数，以确保大的传输大小）。

# 五、Kafka 安装

# 5.1 安装环境与前提条件

安装环境：Linux

前提条件：

Linux系统下安装好jdk 1.8以上版本，正确配置环境变量 Linux系统下安装好scala 2.11版本

安装ZooKeeper（注：kafka自带一个Zookeeper服务，如果不单独安装，也可以使用自带的ZK）

# 5.2 安装步骤

Apache基金会开源的这些软件基本上安装都比较方便，只需要下载、解压、配置环境变量三步即可完成，kafka也一样，官网选择对应版本下载后直接解压到一个安装目录下就可以使用了，如果为了方便可以在~/.bashrc里配置一下环境变量，这样使用的时候就不需要每次都切换到安装目录了。

具体可参考：Kafka 集群安装与环境测试

# 5.3 测试

接下来可以通过简单的console窗口来测试kafka是否安装正确。

**「（1）首先启动ZooKeeper服务」**

如果启动自己安装的ZooKeeper，使用命令zkServer.sh start即可。

如果使用kafka自带的ZK服务，启动命令如下（启动之后shell不会返回，后续其他命令需要另开一个Terminal）：

```
$ cd /opt/tools/kafka #进入安装目录
$ bin/zookeeper-server-start.sh config/zookeeper.properties
﻿
```

**「（2）第二步启动kafka服务」**

启动Kafka服务的命令如下所示：

```
$ cd /opt/tools/kafka #进入安装目录
$ bin/kafka-server-start.sh config/server.properties
﻿
```

**「（3）第三步创建一个topic，假设为“test”」**

创建topic的命令如下所示，其参数也都比较好理解，依次指定了依赖的ZooKeeper，副本数量，分区数量，topic的名字：

```
$ cd /opt/tools/kafka #进入安装目录
$ bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test1
﻿
```

创建完成后，可以通过如下所示的命令查看topic列表：

```
$ bin/kafka-topics.sh --list --zookeeper localhost:2181 
﻿
```

**「（4）开启Producer和Consumer服务」**

kafka提供了生产者和消费者对应的console窗口程序，可以先通过这两个console程序来进行验证。

首先启动Producer：

```
$ cd /opt/tools/kafka #进入安装目录
$ bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test
﻿
```

然后启动Consumer：

```
$ cd /opt/tools/kafka #进入安装目录
$ bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test --from-beginning
﻿
```

在打开生产者服务的终端输入一些数据，回车后，在打开消费者服务的终端能看到生产者终端输入的数据，即说明kafka安装成功。

# 六、Apache Kafka 简单示例

# 6.1 创建消息队列

```
kafka-topics.sh --create --zookeeper 192.168.56.137:2181 --topic test --replication-factor 1 --partitions 1
﻿
```

# 6.2 pom.xml

```
<!-- https://mvnrepository.com/artifact/org.apache.kafka/kafka-clients -->
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>2.1.1</version>
</dependency>
﻿
```

# 6.3 生产者

```
package com.njbdqn.services;
﻿
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
﻿
import java.util.Properties;
﻿
/**
 * @author：Tokgo J
 * @date：2020/9/11
 * @aim：生产者：往test消息队列写入消息
 */
﻿
public class MyProducer {
    public static void main(String[] args) {
        // 定义配置信息
        Properties prop = new Properties();
        // kafka地址，多个地址用逗号分割  "192.168.23.76:9092,192.168.23.77:9092"
        prop.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"192.168.56.137:9092");
        prop.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        prop.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class);
        KafkaProducer<String,String> prod = new KafkaProducer<String, String>(prop);
﻿
        // 发送消息
        try {
            for(int i=0;i<10;i++) {
                // 生产者记录消息
                ProducerRecord<String, String> pr = new ProducerRecord<String, String>("test", "hello world"+i);
                prod.send(pr);
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            prod.close();
        }
    }
}
﻿
```

注意：

1. kafka如果是集群，多个地址用逗号分割(,) ；
2. Properties的put方法，第一个参数可以是字符串，如:p.put("bootstrap.servers","192.168.23.76:9092") ；
3. kafkaProducer.send(record)可以通过返回的Future来判断是否已经发送到kafka，增强消息的可靠性。同时也可以使用send的第二个参数来回调，通过回调判断是否发送成功；
4. p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class); 设置序列化类，可以写类的全路径。

# 6.4 消费者

```
package com.njbdqn.services;
﻿
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
﻿
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
﻿
/**
 * @author：Tokgo J
 * @date：2020/9/11
 * @aim：消费者：读取kafka数据
 */
﻿
public class MyConsumer {
    public static void main(String[] args) {
        Properties prop = new Properties();
        prop.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.56.137:9092");
        prop.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        prop.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
﻿
        prop.put("session.timeout.ms", "30000");
        //消费者是否自动提交偏移量，默认是true 避免出现重复数据 设为false
        prop.put("enable.auto.commit", "false");
        prop.put("auto.commit.interval.ms", "1000");
        //auto.offset.reset 消费者在读取一个没有偏移量的分区或者偏移量无效的情况下的处理
        //earliest 在偏移量无效的情况下 消费者将从起始位置读取分区的记录
        //latest 在偏移量无效的情况下 消费者将从最新位置读取分区的记录
        prop.put("auto.offset.reset", "earliest");
﻿
        // 设置组名
        prop.put(ConsumerConfig.GROUP_ID_CONFIG, "group");
﻿
        KafkaConsumer<String, String> con = new KafkaConsumer<String, String>(prop);
﻿
        con.subscribe(Collections.singletonList("test"));
﻿
        while (true) {
            ConsumerRecords<String, String> records = con.poll(Duration.ofSeconds(100));
            for (ConsumerRecord<String, String> rec : records) {
                System.out.println(String.format("offset:%d,key:%s,value:%s", rec.offset(), rec.key(), rec.value()));
﻿
            }
        }
    }
}
﻿
```

注意：

1. 订阅消息可以订阅多个主题；
2. ConsumerConfig.GROUP_ID_CONFIG 表示消费者的分组，kafka根据分组名称判断是不是同一组消费者，同一组消费者去消费一个主题的数据的时候，数据将在这一组消费者上面轮询；
3. 主题涉及到分区的概念，同一组消费者的个数不能大于分区数。因为：一个分区只能被同一群组的一个消费者消费。出现分区小于消费者个数的时候，可以动态增加分区；
4. 注意和生产者的对比，Properties中的key和value是反序列化，而生产者是序列化。