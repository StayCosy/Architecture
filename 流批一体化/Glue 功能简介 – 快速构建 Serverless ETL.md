# Glue 功能简介 – 快速构建 Serverless ETL

by [AWS Team](https://aws.amazon.com/cn/blogs/china/author/tiansl/) | on 12 DEC 2019 | in [AWS Big Data](https://aws.amazon.com/cn/blogs/china/category/big-data/) | [Permalink](https://aws.amazon.com/cn/blogs/china/glue-function-introduction-build-serverless-etl-quickly/) | [ Share](https://aws.amazon.com/cn/blogs/china/glue-function-introduction-build-serverless-etl-quickly/#)

AWS Glue 是一项完全托管的提取、转换和加载 (ETL) 服务，您可以用来登记、清理和丰富数据，并可以在数据存储之间可靠地移动数据。

Glue的组件包括:

- 数据目录
- 爬网程序和分类器
- ETL操作
- 作业系统

在本篇文章中，主要展示了Glue的爬网程序，数据目录和ETL操作的功能，并通过一个业务场景完成一个简单的demo。

先决条件: 到https://mockaroo.com/生成一份测试数据，上传到s3://myglue-sample-data/rawdata/sampledata/。具体步骤，请参考：https://docs.aws.amazon.com/AmazonS3/latest/gsg/CreatingABucket.html

## 一.AWS Glue 数据目录

AWS Glue 数据目录是您的持久性元数据存储。它是一项托管服务，可让您在 AWS 云中存储、注释和共享元数据，就像在 Apache Hive 元存储中一样。

数据目录可以用于：

- Athena
- Redshift Spectrum
- EMR
- 以及自建Hadoop的Hive

使用Glue托管的数据目录有很多好处

1. 只需通过爬虫爬取一次表结构就可以在多个地方使用
2. 您无需再为数据目录进行每天备份，无需额外创建RDS来存储，节省维护成本
3. Glue托管的数据目录是通过IAM策略进行管理，可以对不同的IAM用户限制相应的库表权限，非常灵活
4. 同时Glue的库表也可以跨region，跨账户授权，非常适合多账户，跨region的集中式的元数据管理

 

### 数据库的创建

进入Glue控制台，在左侧导航栏选择<数据库>,点击添加数据库

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly1.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly1.jpg)

创建后，数据库会在下面显示

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly2.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly2.jpg)

在添加完数据库后，需要使用爬虫来爬取表，将表信息写入到数据库里

 

## 二.通过爬网程序解析并存储数据的元数据（metadata）

在传统的大数据业务场景中，一般都需要先建表，然后再进行操作，Glue的爬网程序很好的解决了这部分操作，通过爬虫爬取源数据和ETL后数据的数据结构，并保存在DataCatalog相应的Database下。

### 具体操作

爬取s3://myglue-sample-data/rawdata/sampledata/sampledata.csv的数据结构

在Glue控制台，在左侧导航栏选择<爬网程序>，右侧主窗口点击“添加爬往程序”，输入爬网程序的名称

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly3.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly3.jpg)

选择Data Stores，即创建新的表

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly4.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly4.jpg)

源数据在S3上，所以选择S3，选择S3源数据的位置，表名不写，爬虫会自动根据文件名创建表名
[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly5.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly5.jpg)

添加另一个数据存储，选“否”

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly6.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly6.jpg)

下一步，创建执行爬虫做需要的Role，

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly7.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly7.jpg)

为此爬往程序创建计划，选择“按需运行“

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly8.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly8.jpg)

设置爬往程序的输出，选择之前创建过的mymetastore

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly9.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly9.jpg)

点击“下一步“，点击”完成“

 

至此，在“爬往程序“窗口，您可以看到新创建的爬虫程序已经在列表里。

点击“立即运行“

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly10.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly10.jpg)

在页面下方，您可以看到爬网程序的运行状态。等待该爬网程序运行结束。

在左侧导航栏中，选择“表“，右侧筛选框中，输入之前创建的数据库“mymetastore“，您可以看到表sampledata_csv已经创建完毕

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly11.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly11.jpg)

点击表，查看表结构如下

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly12.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly12.jpg)

 

## 三.Serverless ETL

下面我们对该表进行ETL，把源数据的字段进行rename后，写入到另一个S3位置

在左侧导航栏中，选择“表“，右侧页面点击”添加作业“

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly13.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly13.jpg)

输入作业名称，Role选择之前创建的AWSGlueServiceRole-myfirstgluerole

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly14.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly14.jpg)

指定数据源，选择要进行ETL的数据源

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly15.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly15.jpg)

转换类型，选择默认值

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly16.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly16.jpg)

指定数据目标，这里将ETL之后的数据存放在S3上的一个新位置，并以Parquet格式存储。这里选S3://myglue-sample-data/etldata/sampledata

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly17.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly17.jpg)

这里修改字段名称，然后点击”保存作业并编辑脚本”

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly18.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly18.jpg)

进入Glue ETL UI编辑页面，可以看到代码部分，这里只做简单功能展示(具体Glue可以进行的操作见https://docs.aws.amazon.com/zh_cn/glue/latest/dg/aws-glue-programming.html)，点击”运行作业”

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly19.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly19.jpg)

显示运行成功后，到S3里可以看到文件已经成功输出

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly20.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly20.jpg)

重复前述的第2步，创建一个新的爬网程序，对ETL后的数据进行爬取。

完成后，你将会看到新的数据结构已经变化

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly21.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly21.jpg)

 

## 附: Data Catalog 在Athena和EMR里的应用

进入Athena的页面，左侧导航栏<数据库>下面选择”mymetastore”,

我们可以看到Athena里已经有这两张表了，对其中一张表进行查询

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly22.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly22.jpg)

在创建EMR的时候，选择Glue作为Hive的metastore

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly23.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly23.jpg)

Glue的DB如下

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly24.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly24.jpg)

Hive里显示的如下

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly25.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly25.jpg)

依次执行

```python
use mymetastore;

show tables;

desc sampledata;

desc sampledata_csv;
```

可以看出表结构信息和Glue console显示的一样，

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly26.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly26.jpg)

执行简单的Hive SQL 查询，成功

```bash
select * from sampledata limit 10;

select count(1) from sampledata;
```

[![img](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly27.png)](https://s3.cn-north-1.amazonaws.com.cn/awschinablog/glue-function-introduction-build-serverless-etl-quickly27.jpg)