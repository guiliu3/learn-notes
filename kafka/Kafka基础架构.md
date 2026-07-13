# Kafka 基础架构

> 学习目标：能画出 Kafka 的整体架构，能讲清楚 Topic、Partition、Replica、ISR、Producer、Consumer Group 的关系，并能结合“政务数据同步平台”说明为什么使用 Kafka。

## 一、Kafka 是什么

Kafka 是一个分布式事件流平台，也可以理解为高吞吐、可持久化、可水平扩展的消息中间件。

它常用于：

- 系统间异步解耦。
- 削峰填谷。
- 日志采集。
- 数据同步。
- 事件驱动架构。
- 流式处理的上游数据管道。

在你的政务数据同步平台中，Kafka 的核心价值不是“发一条消息”这么简单，而是：

- 门户系统不直接依赖审批系统、监察系统、云平台等下游系统。
- 主业务请求可以快速返回，不被下游系统响应速度拖慢。
- 下游系统临时异常时，消息仍可保存在 Kafka 中，后续继续消费或补偿。
- 多个系统可以订阅同一类业务事件，实现一处生产、多处消费。

## 二、整体架构

Kafka 的核心角色：

```text
Producer  ->  Kafka Cluster  ->  Consumer Group
              Broker 1
              Broker 2
              Broker 3
```

更细一点：

```text
Producer
   |
   | send(record)
   v
Topic: government-sync
   |
   |-- Partition 0: msg1, msg4, msg7 ...
   |-- Partition 1: msg2, msg5, msg8 ...
   |-- Partition 2: msg3, msg6, msg9 ...
         |
         | replicas
         v
Broker 集群
   |
   v
Consumer Group: sync-service-group
   |-- Consumer A 消费 Partition 0
   |-- Consumer B 消费 Partition 1
   |-- Consumer C 消费 Partition 2
```

一句话理解：

> Producer 把消息写入 Topic，Topic 被拆成多个 Partition，Partition 分布在多个 Broker 上，Consumer Group 中的消费者并行消费这些 Partition。

## 三、核心概念

### 1. Broker

Broker 是 Kafka 集群中的一台服务节点。

一个 Kafka 集群通常由多个 Broker 组成，每个 Broker 负责存储一部分 Partition 数据，并处理生产者写入和消费者读取请求。

面试表达：

> Broker 可以理解为 Kafka 集群中的服务器节点。Topic 的 Partition 会分布在不同 Broker 上，这样 Kafka 才能实现水平扩展和高吞吐。

### 2. Topic

Topic 是消息的逻辑分类。

例如政务数据同步平台可以按业务类型设计 Topic：

- `portal-apply-sync`：门户办件申请同步。
- `supervision-sync`：电子监察同步。
- `cloud-platform-sync`：云平台数据同步。

Topic 本身只是逻辑概念，真正存储消息的是 Partition。

面试表达：

> Topic 用来区分不同类型的业务消息。生产者把消息发送到指定 Topic，消费者订阅 Topic 后进行消费。




 >❓ 定义的Topic存储在哪里（内存里，但是多节点，消息拉取是底层原理是什么个逻辑），如果是多节点的话，如果一个topic有多个Partition，是在多个Broker节点上，


### 3. Partition

Partition 是 Topic 的物理分片。

Kafka 用 Partition 解决三个问题：

- 存储扩展：一个 Topic 的数据可以分散到多个 Broker。
- 并行消费：多个消费者可以分别消费不同 Partition。
- 局部有序：同一个 Partition 内消息有序。

需要注意：

- Kafka 只能保证单个 Partition 内有序。
- Kafka 不保证一个 Topic 下所有 Partition 的全局有序。
- 如果某类业务必须严格有序，应该让同一业务 Key 进入同一个 Partition。

面试表达：

> Partition 是 Kafka 高吞吐的关键。Topic 拆成多个 Partition 后，生产和消费都可以并行。但顺序性只在单个 Partition 内成立，所以如果业务要求同一办件按顺序处理，就要用办件唯一 ID 作为 Key，让同一办件的消息落到同一个 Partition。

>💡 Partition分区在物理层是一个目录（Topic-Partition），包含多个 Segment。

### 4. Offset

Offset 是消息在 Partition 中的位置编号。

每条消息写入 Partition 后，都会得到一个递增的 Offset。

例如：

```text
Partition 0:
offset 0 -> msgA
offset 1 -> msgB
offset 2 -> msgC
```

消费者通过 Offset 记录自己消费到哪里。

面试表达：

> Offset 可以理解为消费者的消费进度。Kafka 不会主动记住业务是否处理成功，而是消费者通过提交 Offset 表示“我已经消费到这里了”。

### 5. Replica

Replica 是 Partition 的副本。

为了避免单个 Broker 宕机导致数据丢失，Kafka 会为 Partition 创建多个副本。

例如：

```text
Topic: sync-topic
Partition 0
  Leader Replica   -> Broker 1
  Follower Replica -> Broker 2
  Follower Replica -> Broker 3
```

副本分为：

- Leader Replica：负责读写请求。
- Follower Replica：从 Leader 拉取数据做备份。

正常情况下，生产者和消费者只和 Leader 副本交互。

>💡 Replica基本单位应该是Partition,而不是Topic,一个Partition的副本一定分布到不同 Broker。单节点 Kafka,只有 Leader没有Follower.

### 6. Leader / Follower

每个 Partition 的多个副本中，只有一个 Leader，其余是 Follower。

Leader 的职责：

- 接收生产者写入。
- 处理消费者读取。
- 维护消息顺序。

Follower 的职责：

- 从 Leader 同步数据。
- 在 Leader 故障后参与选举，成为新的 Leader。

面试表达：

> Kafka 的读写请求主要走 Partition Leader。Follower 平时负责复制数据，当 Leader 所在 Broker 故障时，Kafka 会从可用副本中选出新的 Leader，保证服务继续可用。

### 7. ISR

ISR 是 In-Sync Replicas，表示和 Leader 保持同步的副本集合。

不是所有 Follower 都一定是可靠副本。只有同步进度跟得上 Leader 的副本，才会留在 ISR 中。

例如：

```text
Partition 0:
Leader: Broker 1
Follower: Broker 2
Follower: Broker 3

ISR = [Broker 1, Broker 2]
```

这表示 Broker 3 可能同步太慢，暂时不在 ISR 里。

ISR 的意义：

- 保证 Leader 故障后，新 Leader 尽量从数据较新的副本中选出。
- 配合 `acks=all` 提高消息可靠性。

面试表达：

> ISR 是 Kafka 副本可靠性的核心。生产者如果配置 `acks=all`，通常要求消息被 Leader 和 ISR 中的副本确认后才算写入成功，这样可以降低 Leader 宕机导致消息丢失的风险。

### 8. Producer

Producer 是消息生产者。

Producer 写消息的大致流程：

1. 构造消息 Record。
2. 根据 Topic 和 Key 选择 Partition。
3. 批量发送给 Partition Leader。
4. 等待 Broker 返回确认。

影响 Producer 可靠性的关键参数：

- `acks`：生产者等待多少副本确认。
- `retries`：发送失败是否重试。
- `batch.size`：批量大小。
- `linger.ms`：等待多久凑批。
- `enable.idempotence`：是否开启幂等生产者。

本篇先只理解架构，参数细节放到生产者专题。

### 9. Consumer

Consumer 是消息消费者。

Consumer 从 Topic 的 Partition 中拉取消息，处理完成后提交 Offset。

消费者需要重点理解：

- Kafka 是 Pull 模型，消费者主动拉取消息。
- 消费成功后提交 Offset。
- 如果处理成功但 Offset 提交失败，可能重复消费。
- 如果先提交 Offset 再处理业务，业务失败时可能丢消息。

面试表达：

> 消费者处理消息和提交 Offset 的顺序非常关键。一般业务更能接受重复消费，而不能接受消息丢失，所以会先处理业务，再提交 Offset，并通过业务唯一键保证幂等。

### 10. Consumer Group

Consumer Group 是消费者组。

一个 Topic 可以被多个 Consumer Group 消费，彼此互不影响。

同一个 Consumer Group 内：

- 一个 Partition 同一时刻只能被组内一个 Consumer 消费。
- 一个 Consumer 可以消费多个 Partition。
- Consumer 数量超过 Partition 数量时，多出来的 Consumer 会空闲。

例如：

```text
Topic 有 3 个 Partition
Consumer Group 有 2 个 Consumer

Consumer A -> Partition 0, Partition 1
Consumer B -> Partition 2
```

如果有 4 个 Consumer：

```text
Consumer A -> Partition 0
Consumer B -> Partition 1
Consumer C -> Partition 2
Consumer D -> 空闲
```

面试表达：

> Consumer Group 决定了 Kafka 的消费扩展能力。增加消费者可以提升并行消费能力，但最大并行度受 Partition 数量限制。

## 四、消息写入流程

简化流程：

```text
1. Producer 发送消息到 Topic
2. Partitioner 根据 Key 选择 Partition
3. Producer 找到该 Partition 的 Leader Broker
4. Leader Broker 写入本地日志
5. Follower 从 Leader 同步数据
6. Broker 根据 acks 配置返回成功或失败
```

如果消息指定了 Key：

- 相同 Key 通常会进入同一个 Partition。
- 可以保证相同 Key 的消息在单 Partition 内有序。

如果没有指定 Key：

- Kafka 会按默认策略分配 Partition。
- 更利于负载均衡。
- 不保证同一业务对象的顺序。

结合你的项目：

> 数据同步平台中，如果一个办件可能产生多次状态变更，建议使用办件唯一 ID 作为 Key，让同一办件的消息进入同一个 Partition，避免同一办件状态被乱序处理。

## 五、消息读取流程

简化流程：

```text
1. Consumer 加入 Consumer Group
2. Kafka 为 Consumer 分配 Partition
3. Consumer 从分配到的 Partition 拉取消息
4. Consumer 执行业务逻辑
5. 业务处理成功后提交 Offset
6. 下次从新的 Offset 继续消费
```

需要注意：

- Kafka 保存的是消息日志，不是传统队列中的“取出即删除”。
- 消息是否还能被消费，取决于保留时间和 Offset。
- 不同 Consumer Group 可以重复消费同一 Topic 的消息。

> 💡 Kafka的消息是日志的形式记录，所以不存在用完删除，而是根据保留时间一直保留在日志里。

## 六、Kafka 为什么吞吐高

Kafka 高吞吐主要来自这些设计：

- 顺序写磁盘：追加写日志，比随机写效率高。
- Page Cache：充分利用操作系统缓存。
- 批量发送：Producer 可以批量发送消息。
- 零拷贝：减少内核态和用户态之间的数据复制。
- Partition 并行：多个 Partition 可以并行读写。
- Pull 模型：消费者按自己的能力拉取数据。

面试表达：

> Kafka 快不是因为它不落盘，而是因为它通过顺序写、批量、Page Cache、零拷贝和分区并行，把磁盘和网络利用率做得很高。

## 七、Kafka 和传统 MQ 的差异

| 维度 | Kafka | 传统队列模型 |
| --- | --- | --- |
| 数据模型 | 分布式日志 | 队列 |
| 消费方式 | Pull | Push 或 Pull |
| 消息删除 | 按保留策略删除 | 消费后删除或确认后删除 |
| 顺序性 | Partition 内有序 | 队列内通常有序 |
| 扩展方式 | 增加 Partition 和 Broker | 增加队列或节点 |
| 典型场景 | 日志、数据同步、事件流 | 业务解耦、任务队列 |

这不是说 Kafka 一定比 RabbitMQ 好，而是适用场景不同。

Kafka 更适合：

- 高吞吐数据流。
- 多消费者订阅。
- 数据同步。
- 日志采集。
- 流处理。

RabbitMQ 更适合：

- 复杂路由。
- 延迟队列。
- 传统任务队列。
- 需要更灵活交换机模型的场景。

## 八、结合政务数据同步平台的设计解释

### 1. 为什么不用同步接口调用

同步接口调用的问题：

- 门户系统会直接依赖下游系统。
- 下游系统响应慢会拖慢主业务。
- 下游不可用会影响门户申请流程。
- 下游系统增加时，门户系统也要跟着改。

更好的表达：

> 门户系统的核心职责是受理业务，不应该等待多个下游系统同步完成。使用 Kafka 后，门户只需要发送业务事件，下游系统按自己的节奏消费，主业务和同步流程解耦。

### 2. 为什么不用原来的 Kettle/Spoon

Kettle 定时同步的问题：

- 实时性差。
- 失败后补偿不够灵活。
- 同步链路可观测性弱。
- 复杂业务逻辑维护困难。

更好的表达：

> Kettle 更适合批量 ETL，不适合对实时性和业务编排要求较高的同步场景。我们的数据同步平台需要在门户申请后尽快把数据流转到审批端，所以选择 Kafka 做异步事件管道，再由消费者侧编排责任链处理业务同步。

### 3. Kafka 在项目中的定位

在你的项目里，Kafka 承担：

- 异步解耦：生产者和消费者不直接依赖。
- 可靠传输：消息先落 Kafka，再由消费者处理。
- 削峰：高峰期消息可先进入 Topic。
- 扩展：新增下游系统可以新增 Consumer Group。
- 可补偿：失败数据可以基于业务主键重新推送。

### 4. 项目里消息为什么只放业务唯一值

优点：

- 消息体小，降低网络和 Kafka 存储压力。
- 生产者逻辑简单，不需要组装完整业务对象。
- 消费者可以根据业务唯一值查询最新源数据。
- 后续字段变化时，不容易影响消息格式。

代价：

- 消费者依赖源库查询。
- 源数据被修改后，消费者拿到的可能不是消息产生瞬间的快照。
- 如果源库不可用，消费会失败，需要重试或补偿。

面试表达：

> 当时选择只发送业务唯一值，是在消息体大小、扩展性和数据实时性之间做的取舍。这个方案适合我们当时的数据同步场景，但如果业务要求严格保留事件发生时的完整快照，就应该把关键业务字段也放入消息中，甚至引入事件版本号。

## 九、常见面试题

### 1. Kafka 为什么要有 Partition

答：

> Partition 是 Kafka 水平扩展和并行消费的基础。一个 Topic 拆成多个 Partition 后，可以分布在多个 Broker 上，提高存储和写入能力；消费者组内多个 Consumer 也可以分别消费不同 Partition，提高消费吞吐。同时 Kafka 只能保证单个 Partition 内消息有序。

### 2. Consumer 数量越多越好吗

答：

> 不是。一个 Consumer Group 内，同一个 Partition 同一时间只能被一个 Consumer 消费，所以消费并行度上限由 Partition 数量决定。如果 Consumer 数量超过 Partition 数量，多出来的 Consumer 会空闲。

### 3. Kafka 如何保证消息不丢

答：

> 需要生产端、Broker 端、消费端一起保证。生产端可以配置 `acks=all`、重试和幂等生产者；Broker 端通过副本和 ISR 保证数据冗余；消费端要在业务处理成功后再提交 Offset，并通过业务唯一键处理重复消费。

### 4. Kafka 能保证消息顺序吗

答：

> Kafka 只能保证单个 Partition 内有序，不能保证 Topic 全局有序。如果业务要求同一对象有序，比如同一个办件的状态变更，就要使用相同业务 Key，让这些消息进入同一个 Partition。

### 5. 为什么 Kafka 适合数据同步

答：

> 数据同步通常需要异步解耦、削峰、可靠传输和多下游订阅。Kafka 的 Topic 和 Consumer Group 模型很适合这种场景。生产者只负责发送业务事件，下游系统通过不同 Consumer Group 独立消费，互不影响。

## 十、自测清单

闭卷回答下面问题：

- Broker、Topic、Partition 三者是什么关系？
- 为什么 Partition 是 Kafka 高吞吐的关键？
- Kafka 的顺序性边界在哪里？
- Leader Replica 和 Follower Replica 分别做什么？
- ISR 是什么，为什么重要？
- Consumer Group 如何实现并行消费？
- 为什么 Consumer 数量超过 Partition 数量后不会继续提升吞吐？
- Offset 提交时机为什么会影响重复消费和消息丢失？
- Kafka 在你的政务数据同步平台中解决了哪些问题？
- 如果让你重新优化数据同步平台，Kafka 可靠性会补哪些能力？

## 十一、今天需要记住的三句话

- Kafka 的本质是分布式日志，Topic 是逻辑分类，Partition 是真正的存储和并行单位。
- Kafka 只保证单个 Partition 内有序，业务要有序就用业务 Key 控制分区。
- 数据同步平台使用 Kafka 的核心原因是异步解耦、可靠传输、削峰和多下游扩展。
