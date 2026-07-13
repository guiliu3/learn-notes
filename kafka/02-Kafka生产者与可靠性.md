# 02-Kafka 生产者与可靠性

> 学习目标：理解 Producer 发送消息的完整流程，掌握 `acks`、重试、幂等生产者、分区策略、批量发送等关键点，并能结合“政务数据同步平台”解释如何避免消息丢失。

## 一、Producer 的职责

Producer 是 Kafka 的消息生产者，负责把业务事件发送到指定 Topic。

在你的政务数据同步平台中，Producer 的典型职责是：

- 门户系统产生办件申请事件。
- 将业务唯一值、业务类型、事件时间等信息封装成消息。
- 发送到 Kafka Topic。
- 根据发送结果更新同步状态或记录发送失败日志。

Producer 不应该承担太重的下游同步逻辑。

更好的边界是：

> Producer 负责把“业务事件已经发生”可靠地写入 Kafka；Consumer 负责根据消息执行具体同步逻辑。

## 二、Producer 发送消息流程

简化流程：

```text
业务代码
  |
  v
KafkaProducer.send(record)
  |
  v
Serializer 序列化 Key / Value
  |
  v
Partitioner 选择 Partition
  |
  v
RecordAccumulator 批量缓冲
  |
  v
Sender 线程发送到 Broker
  |
  v
Partition Leader 写入日志
  |
  v
Broker 根据 acks 返回结果
```

核心点：

- `send()` 默认是异步发送。
- 消息会先进入本地缓冲区。
- Kafka 会批量发送消息，提高吞吐。
- 真正发送网络请求的是 Producer 内部 Sender 线程。
- Broker 返回成功，不一定代表所有副本都写成功，取决于 `acks`。

## 三、消息 Record 结构

一条 Kafka 消息通常包含：

- Topic：发送到哪个主题。
- Partition：可指定，也可由 Kafka 计算。
- Key：用于分区和业务标识。
- Value：消息内容。
- Headers：扩展元数据。
- Timestamp：时间戳。

政务数据同步平台建议消息结构：

```json
{
  "bizId": "AQ202607090001",
  "bizType": "PORTAL_APPLY",
  "eventType": "CREATE",
  "sourceSystem": "portal",
  "eventTime": "2026-07-09 10:00:00",
  "traceId": "trace-xxx"
}
```

如果只发送业务唯一值，也建议至少带上：

- 业务 ID。
- 业务类型。
- 事件类型。
- 事件时间。
- TraceId。

这样排查问题时能知道这条消息从哪里来、属于什么业务、何时产生。

## 四、分区策略

Producer 发送消息时，需要决定消息进入哪个 Partition。

常见策略：

### 1. 指定 Partition

业务代码直接指定 Partition。

优点：

- 控制最强。

缺点：

- 业务代码和 Kafka 分区数量耦合。
- 扩容 Partition 后容易出问题。

一般不推荐业务代码强指定 Partition。

### 2. 指定 Key

Kafka 根据 Key 计算 Partition。

例如：

```text
partition = hash(key) % partitionCount
```

优点：

- 相同 Key 的消息进入同一个 Partition。
- 可以保证同一业务对象在单 Partition 内有序。

适合：

- 同一个办件状态变更。
- 同一个订单事件流。
- 同一个用户相关事件。

你的项目建议：

> 如果同一个办件可能产生多次同步消息，建议使用办件唯一 ID 作为 Key，保证同一办件的消息进入同一个 Partition，避免状态乱序。

### 3. 不指定 Key

Kafka 使用默认策略分配 Partition。

优点：

- 负载更均衡。
- 吞吐更容易打满。

缺点：

- 不保证同一业务对象的顺序。

适合：

- 日志采集。
- 对顺序无要求的数据同步。
- 独立事件。

## 五、acks 参数

`acks` 决定 Producer 需要等 Broker 确认到什么程度才认为发送成功。

### 1. acks=0

Producer 发出去就认为成功，不等待 Broker 响应。

优点：

- 延迟最低。
- 吞吐最高。

缺点：

- 消息可能丢失，Producer 不知道。

适合：

- 极少数允许丢失的日志场景。

不适合政务数据同步。

### 2. acks=1

Partition Leader 写入成功后返回成功。

优点：

- 性能和可靠性折中。

缺点：

- Leader 写成功但 Follower 还没同步时，如果 Leader 宕机，消息可能丢失。

适合：

- 一般业务消息。
- 对极端故障下少量丢失可接受的场景。

### 3. acks=all

Leader 和 ISR 中的副本都确认后才返回成功。

优点：

- 可靠性最高。

缺点：

- 延迟更高。
- 吞吐下降。
- ISR 不足时可能发送失败。

适合：

- 核心业务消息。
- 数据同步。
- 订单、支付、审批等重要事件。

项目建议：

> 政务数据同步属于业务数据流转，不建议使用 `acks=0`。如果数据可靠性要求较高，推荐 `acks=all`，并配合重试、幂等生产者和失败记录。

## 六、重试机制

Producer 发送失败时，可以自动重试。

关键参数：

- `retries`：重试次数。
- `retry.backoff.ms`：每次重试间隔。
- `delivery.timeout.ms`：消息发送总超时时间。
- `request.timeout.ms`：单次请求超时时间。

需要注意：

- 重试可能导致消息重复。
- 旧版本 Kafka 中，重试可能导致消息乱序。
- 开启幂等生产者后，可以减少重复写入和乱序风险。

面试表达：

> Kafka Producer 的重试解决的是临时网络异常或 Broker 短暂不可用问题，但重试不等于绝对可靠。业务上仍然要考虑重复消息和失败落库。

## 七、幂等生产者

幂等生产者用于解决 Producer 重试导致的重复写入问题。

开启参数：

```properties
enable.idempotence=true
```

开启后，Kafka 会为 Producer 分配 Producer ID，并为每个 Partition 维护序列号。

Broker 可以根据 Producer ID + Partition + Sequence Number 判断重复消息，避免同一条消息因重试被写入多次。

适合：

- Producer 发送失败自动重试。
- 业务不希望同一条消息被重复写入 Kafka。

注意：

- 幂等生产者保证的是单个 Producer 会话内、单个 Partition 上的发送幂等。
- 它不能替代 Consumer 端业务幂等。
- 如果业务代码自己调用 `send()` 两次发送两条相同业务消息，Kafka 幂等生产者不一定认为它们是重复。

面试表达：

> 幂等生产者主要解决 Producer 重试导致的 Kafka 内部重复写入，但不能解决所有业务重复。消费端仍然要基于业务唯一键做幂等。

## 八、批量发送

Kafka 高吞吐的重要原因之一是批量发送。

相关参数：

- `batch.size`：同一个 Partition 的批次大小。
- `linger.ms`：消息不满一批时，最多等待多久再发送。
- `buffer.memory`：Producer 本地缓冲区总大小。
- `compression.type`：压缩方式，如 `snappy`、`lz4`、`zstd`。

基本理解：

- `batch.size` 越大，吞吐可能越高，但延迟可能增加。
- `linger.ms` 越大，越容易凑批，但单条消息等待时间更长。
- 开启压缩可以降低网络传输量，但会增加 CPU 消耗。

项目建议：

> 数据同步场景通常不需要极致低延迟，可以适当使用批量和压缩提升吞吐。但政务审批类数据也不能为了吞吐把 `linger.ms` 设置得太大，否则会增加同步延迟。

## 九、Producer 发送方式

### 1. 发送后不关心结果

```java
producer.send(record);
```

问题：

- 发送失败不容易感知。
- 不适合重要业务。

### 2. 同步等待结果

```java
producer.send(record).get();
```

优点：

- 能明确知道发送成功或失败。

缺点：

- 阻塞当前线程。
- 吞吐较低。

适合：

- 管理后台手动触发少量补偿。
- 对发送结果强依赖的低频场景。

### 3. 异步回调

```java
producer.send(record, (metadata, exception) -> {
    if (exception != null) {
        // 记录失败，后续补偿
    } else {
        // 记录发送成功
    }
});
```

优点：

- 不阻塞主线程。
- 可以感知发送结果。

适合：

- 大多数业务生产场景。

项目建议：

> 政务数据同步平台推荐使用异步发送 + 回调记录结果。发送失败时记录失败日志或同步状态，后续由补偿任务重新发送。

## 十、Producer 可靠性设计

只靠 Kafka 参数不够，业务侧也要设计可靠性闭环。

推荐方案：

```text
1. 业务数据入库成功
2. 生成待同步事件
3. Producer 发送 Kafka
4. 发送成功：更新事件状态为 SENT
5. 发送失败：记录 FAILED 和失败原因
6. 定时任务扫描 FAILED / 超时未发送事件
7. 重新发送
```

如果要求更高，可以引入本地消息表：

```text
业务事务内：
  - 写业务表
  - 写本地消息表，状态 NEW

异步任务：
  - 扫描 NEW 消息
  - 发送 Kafka
  - 成功后更新 SENT
  - 失败后增加 retry_count
```

这样可以避免业务入库成功但 Kafka 发送失败后没有记录的问题。

## 十一、结合你的项目如何表达

你可以这样讲：

> 在政务数据同步平台里，Producer 不是简单发一条消息，而是负责把门户系统产生的办件事件可靠写入 Kafka。我们会用业务唯一 ID 作为消息 Key，让同一办件进入同一 Partition，降低乱序风险。可靠性上，生产端可以配置 `acks=all`、重试和幂等生产者；业务侧需要记录发送状态，发送失败时通过定时任务或人工补偿接口重新触发。这样可以避免因为下游系统异常或网络抖动影响门户主流程。

如果面试官追问“你们当时有没有这么做”：

> 当时项目主要做了人工可控的补偿机制，没有把 Producer 可靠性做到完整本地消息表模式。如果后续优化，我会补充发送状态表、失败原因、重试次数、最大重试限制和告警，形成自动补偿闭环。

## 十二、常见面试题

### 1. Kafka Producer 是同步发送还是异步发送

答：

> Kafka Producer 的 `send()` 默认是异步的，消息会进入本地缓冲区，由 Sender 线程批量发送。如果调用 `send().get()`，业务线程会阻塞等待发送结果，这就变成同步等待。

### 2. `acks=1` 和 `acks=all` 有什么区别

答：

> `acks=1` 只要求 Partition Leader 写入成功就返回，性能较好，但 Leader 宕机且 Follower 未同步时可能丢消息。`acks=all` 要求 ISR 中副本都确认后才返回，可靠性更高，但延迟和吞吐会受影响。

### 3. Producer 重试会带来什么问题

答：

> 重试可以解决临时网络异常，但可能带来重复消息。旧版本或配置不当时还可能带来乱序。通常会配合幂等生产者，并在消费端使用业务唯一键保证幂等。

### 4. 幂等生产者解决什么问题

答：

> 幂等生产者解决的是 Producer 因重试导致的重复写入 Kafka 问题。它通过 Producer ID 和序列号让 Broker 识别重复发送。但它不能替代业务幂等，消费者仍然要处理重复消费。

### 5. 消息 Key 有什么作用

答：

> Key 主要用于分区。相同 Key 的消息通常会进入同一个 Partition，这样可以保证同一业务对象在单 Partition 内有序。比如同一个办件的多次状态变更，可以用办件 ID 作为 Key。

## 十三、自测清单

- Producer 发送消息的流程是什么？
- `send()` 为什么说是异步的？
- `acks=0`、`acks=1`、`acks=all` 分别适合什么场景？
- Producer 重试为什么可能导致重复？
- 幂等生产者能解决哪些问题，不能解决哪些问题？
- 为什么同一个办件建议用办件 ID 作为消息 Key？
- 批量发送提高吞吐的代价是什么？
- 政务数据同步平台如何设计 Producer 失败补偿？
- 本地消息表解决的是什么问题？
- 如果面试官问“你们怎么保证 Kafka 生产端不丢消息”，你怎么回答？

## 十四、今天记住的三句话

- Producer 的可靠性不是一个参数解决的，而是 `acks`、重试、幂等、失败记录和补偿机制共同完成。
- 消息 Key 决定分区，相同业务 Key 进入同一 Partition 才能保证局部有序。
- 幂等生产者防的是 Producer 重试重复写入，消费端仍然必须做业务幂等。
