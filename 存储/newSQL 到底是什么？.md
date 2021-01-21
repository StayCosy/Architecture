# newSQL 到底是什么？

数据库发展至今已经有3代了：

1. SQL，传统关系型数据库，例如 MySQL
2. noSQL，例如 MongoDB
3. newSQL

## SQL 的问题

互联网在本世纪初开始迅速发展，互联网应用的用户规模、数据量都越来越大，并且要求7X24小时在线。

传统关系型数据库在这种环境下成为了瓶颈，通常有2种解决方法：

- 升级服务器硬件

虽然提升了性能，但总有天花板。

- 数据分片，使用分布式集群结构

对单点数据库进行数据分片，存放到由廉价机器组成的分布式的集群里。

可扩展性更好了，但也带来了新的麻烦。

以前在一个库里的数据，现在跨了多个库，应用系统不能自己去多个库中操作，需要使用数据库分片中间件。

分片中间件做简单的数据操作时还好，但涉及到跨库join、跨库事务时就很头疼了，很多人干脆自己在业务层处理，复杂度较高。

## noSQL 的优势与不足

后来 noSQL 出现了，放弃了传统SQL的强事务保证和关系模型，重点放在数据库的高可用性和可扩展性。

noSQL 的主要优势：

- 高可用性和可扩展性，自动分区，轻松扩展
- 不保证强一致性，性能大幅提升
- 没有关系模型的限制，极其灵活

noSQL 不保证强一致性，对于普通应用没问题，但还是有不少像金融一样的企业级应用有强一致性的需求。

而且 noSQL 不支持 SQL 语句，兼容性是个大问题，不同的 noSQL 数据库都有自己的 api 操作数据，比较复杂。

## newSQL 特性

newSQL 提供了与 noSQL 相同的可扩展性，而且仍基于关系模型，还保留了极其成熟的 SQL 作为查询语言，保证了ACID事务特性。

简单来讲，newSQL 就是在传统关系型数据库上集成了 noSQL 强大的可扩展性。

传统的SQL架构设计基因中是没有分布式的，而 newSQL 生于云时代，天生就是分布式架构。

noSQL 的主要特性：

- SQL 支持，支持复杂查询和大数据分析。
- 支持 ACID 事务，支持隔离级别。
- 弹性伸缩，扩容缩容对于业务层完全透明。
- 高可用，自动容灾。



![img](https://pic4.zhimg.com/80/v2-962dca68f912e2712d94fb59206a8447_720w.jpg)



## 主流newSQL项目

VoltDB

[http://voltdb.com/](https://link.zhihu.com/?target=http%3A//voltdb.com/)

ClustrixDB

http : //[http://www.clustrix.com/](https://link.zhihu.com/?target=http%3A//www.clustrix.com/)

MemSQL

[http://www.memsql.com/](https://link.zhihu.com/?target=http%3A//www.memsql.com/)

ScaleDB

http : //[http://scaledb.com/](https://link.zhihu.com/?target=http%3A//scaledb.com/)

TiDB

[https://pingcap.com/](https://link.zhihu.com/?target=https%3A//pingcap.com/)

参考资料：

[https://db.cs.cmu.edu/papers/2016/pavlo-newsql-sigmodrec2016.pdf](https://link.zhihu.com/?target=https%3A//db.cs.cmu.edu/papers/2016/pavlo-newsql-sigmodrec2016.pdf)