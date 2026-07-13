# 03-Kafka 消费者与 Rebalance

> 学习目标：理解 Consumer Group、Partition 分配、Offset 提交、重复消费、消息丢失、Rebalance 的触发和影响，并能结合“政务数据同步平台”解释消费端如何保证业务幂等和最终一致。

❓: 幂等如何理解

## 一、Consumer 的职责

Consumer 是 Kafka 的消息消费者，负责从 Topic 中拉取消息并执行业务处理。

在你的政务数据同步平台中，Consumer 的职责通常是：

- 从同步 Topic 拉取办件消息。
- 根据业务唯一 ID 查询源业务数据。
- 将数据放入同步上下文。
- 执行责任链 Handler。
- 写入目标库。
- 记录同步成功或失败状态。
- 处理重复消息和补偿消息。

Consumer 是数据同步平台最容易被追问的部分，因为它涉及：

- Offset 提交。
- 重复消费。
- 消息丢失。
- 幂等。
- Rebalance。
- 消费积压。
- 失败重试。

## 二、Kafka 是 Pull 消费模型

Kafka Consumer 主动向 Broker 拉取消息。

```text
Consumer -> poll() -> Broker
Consumer <- records <- Broker
```

Pull 模型的好处：

- Consumer 可以按自己的处理能力拉取。
- 不容易被 Broker 强行推爆。
- 适合不同消费速度的下游系统。

缺点：

- 如果 Consumer 处理太慢，可能产生消费积压。
- 需要正确设置 `poll`、心跳和超时时间。

面试表达：

> Kafka 使用 Pull 模型，消费者主动拉取消息。这样消费者可以根据自己的处理能力控制节奏，但如果处理逻辑慢或者消费者数量不足，就会出现消费积压。

## 三、Consumer Group

Consumer Group 是消费者组。

规则：

- 同一个 Consumer Group 内，一条消息只会被一个 Consumer 消费。
- 不同 Consumer Group 可以独立消费同一个 Topic。
- 一个 Partition 同一时间只能分配给组内一个 Consumer。
- 一个 Consumer 可以消费多个 Partition。

示例：

```text
Topic: sync-topic
Partition: P0, P1, P2

Consumer Group: sync-service
Consumer A -> P0
Consumer B -> P1
Consumer C -> P2
```

如果只有两个 Consumer：

```text
Consumer A -> P0, P1
Consumer B -> P2
```

如果有四个 Consumer：

```text
Consumer A -> P0
Consumer B -> P1
Consumer C -> P2
Consumer D -> 空闲
```

结论：

> 同一个 Consumer Group 的最大并行消费能力受 Partition 数量限制。

## 四、Offset 是什么

Offset 是消息在 Partition 中的位置，也是消费者的消费进度。

```text
Partition 0:
offset 0 -> msgA
offset 1 -> msgB
offset 2 -> msgC
```

Consumer 提交 Offset 表示：

> 这个 Offset 之前的消息，我已经处理过了。

注意：

- Offset 是 Consumer Group 维度的。
- 不同 Consumer Group 有不同 Offset。
- Kafka 不知道你的业务是否真的处理成功，它只认你提交的 Offset。

❓: 虽然同一个topic可以被多个消费组去消费，但是每一个消费组的offset不一致，也就说，2个消费组消费的消息的内容加在一起，并不是合集。


## 五、自动提交与手动提交

### 1. 自动提交

配置：

```properties
enable.auto.commit=true
```

特点：

- Kafka 客户端定期自动提交 Offset。
- 使用简单。

风险：

- 消息还没处理完，Offset 已经提交。
- 如果业务处理失败或应用宕机，消息可能丢失。

不适合重要业务同步场景。

### 2. 手动提交

配置：

```properties
enable.auto.commit=false
```

业务处理成功后主动提交：

```java
consumer.commitSync();
```

优点：

- 可以控制提交时机。
- 更适合重要业务。

缺点：

- 代码复杂度更高。
- 提交失败需要处理。

项目建议：

> 政务数据同步平台建议关闭自动提交，在责任链处理成功后再提交 Offset。这样即使消费过程中失败，也可以重新消费，再通过业务唯一键保证幂等。

## 六、先处理业务还是先提交 Offset

这是 Kafka 消费端最常见的面试题。

### 1. 先提交 Offset，再处理业务

流程：

```text
拉取消息 -> 提交 Offset -> 执行业务
```

风险：

- Offset 已提交。
- 业务处理失败或应用宕机。
- Kafka 认为消息已经消费。
- 这条消息不会再被消费。
- 造成消息丢失。

一般不推荐。

### 2. 先处理业务，再提交 Offset

流程：

```text
拉取消息 -> 执行业务 -> 提交 Offset
```

风险：

- 业务处理成功。
- 提交 Offset 前应用宕机。
- 重启后消息会再次消费。
- 造成重复消费。

这是更常见的选择。

原因：

> 大多数业务更能接受重复消费，不能接受消息丢失。重复消费可以通过幂等解决，消息丢失通常更难补。

## 七、重复消费

Kafka 中重复消费是正常现象，不应该假设消息只会被消费一次。

可能原因：

- 业务处理成功但 Offset 提交失败。
- Consumer 处理超时导致 Rebalance。
- Producer 发送了重复业务消息。
- 补偿任务重新发送。
- Consumer 重启后从旧 Offset 开始消费。

解决方式：

- 使用业务唯一键幂等。
- 目标表建立唯一索引。
- 插入前判断是否已处理。
- 维护消费记录表。
- 更新类操作保证可重复执行。

项目表达：

> 我们不会依赖 Kafka 保证业务绝对不重复，而是在消费端基于业务唯一主键保证幂等。即使同一个办件消息被重复消费，下游也不会生成多条重复数据。

## 八、消息丢失

消费端消息丢失通常来自：

- 自动提交 Offset。
- 先提交 Offset 后处理业务。
- 异常被吞掉但仍然提交 Offset。
- 批量消费时部分成功部分失败，仍提交了整个批次 Offset。

规避方式：

- 关闭自动提交。
- 业务处理成功后再提交 Offset。
- 异常向外抛出，不要假成功。
- 批量消费要处理部分失败。
- 失败消息记录到失败表或死信 Topic。

面试表达：

> 消费端防丢的核心是不要在业务成功前提交 Offset。业务失败时应该记录失败原因，触发重试或补偿，而不是吞掉异常继续提交。

## 九、Rebalance 是什么

Rebalance 是 Consumer Group 内 Partition 重新分配的过程。

当消费者组成员或订阅关系变化时，Kafka 会重新分配 Partition。

触发场景：

- 新 Consumer 加入 Consumer Group。
- 某个 Consumer 下线。
- Consumer 心跳超时。
- Topic Partition 数量变化。
- Consumer 订阅的 Topic 变化。

示例：

```text
原来：
Consumer A -> P0, P1
Consumer B -> P2

新增 Consumer C 后：
Consumer A -> P0
Consumer B -> P1
Consumer C -> P2
```

## 十、Rebalance 的影响

Rebalance 的主要问题：

- Rebalance 期间消费者可能暂停消费。
- Partition 被重新分配后，未提交 Offset 的消息会被重复消费。
- 如果业务处理时间太长，可能被认为 Consumer 不健康，触发 Rebalance。
- 频繁 Rebalance 会导致消费抖动和延迟升高。

面试表达：

> Rebalance 本身是 Kafka Consumer Group 扩缩容和故障恢复的机制，但频繁 Rebalance 会影响消费稳定性，可能导致短暂停止消费和重复消费。

## 十一、心跳与 poll

Consumer 需要定期向 Broker 发送心跳，表示自己还活着。

关键参数：

- `session.timeout.ms`：多久收不到心跳认为 Consumer 死亡。
- `heartbeat.interval.ms`：心跳发送间隔。
- `max.poll.interval.ms`：两次 `poll()` 之间允许的最大间隔。
- `max.poll.records`：一次最多拉取多少条消息。

常见问题：

> 消费者业务处理太慢，超过 `max.poll.interval.ms`，Kafka 会认为它卡死，从而触发 Rebalance。

解决方式：

- 减小 `max.poll.records`。
- 优化单条消息处理耗时。
- 增大 `max.poll.interval.ms`。
- 把耗时业务放到线程池，但要谨慎处理 Offset 提交。

项目建议：

> 如果数据同步责任链处理较重，不要一次拉太多消息。可以控制 `max.poll.records`，避免单轮处理时间过长导致 Rebalance。

## 十二、消费积压

消费积压是指 Producer 生产速度大于 Consumer 消费速度。

表现：

- Consumer Lag 持续升高。
- 同步延迟变大。
- 下游系统数据长时间未更新。

原因：

- Consumer 实例太少。
- Partition 数量太少。
- 单条消息处理慢。
- 下游数据库慢。
- Handler 链路太重。
- 消费失败反复重试。

处理方式：

- 增加 Consumer 实例。
- 增加 Topic Partition 数量。
- 优化 Handler 逻辑。
- 批量写入目标库。
- 优化数据库索引。
- 限制重试次数，把异常消息转入失败表或死信 Topic。

注意：

> 增加 Consumer 实例的前提是 Partition 数量足够，否则多出来的 Consumer 会空闲。

## 十三、消费端幂等设计

政务数据同步平台推荐幂等方案：

### 1. 业务唯一键

目标表使用业务唯一 ID。

```text
biz_id 唯一索引
```

重复消费时：

- 如果不存在，则插入。
- 如果存在，则更新。
- 或者直接跳过。

### 2. 消费记录表

记录每条消息处理状态。

字段示例：

```text
id
topic
partition
offset
biz_id
status
retry_count
error_message
create_time
update_time
```

优点：

- 便于排查。
- 便于补偿。
- 可以防重复处理。

### 3. 状态机校验

如果业务有状态流转：

```text
NEW -> PROCESSING -> FINISHED
```

重复或乱序消息到达时，根据状态机判断是否允许处理。

## 十四、失败处理

消费失败不能简单无限重试。

推荐策略：

```text
1. 消费失败
2. 记录失败原因
3. 判断重试次数
4. 未超过次数：延迟重试或重新投递
5. 超过次数：进入失败表或死信 Topic
6. 人工排查后补偿
```

你的项目当前表达：

> 当时项目主要采用人工可控的补偿机制，基于同步状态和时间范围重新触发补推。后续优化可以增加消费失败记录表、重试次数、错误原因、死信 Topic 和补偿页面。

## 十五、结合责任链如何提交 Offset

你的数据同步平台是责任链处理多个 Handler。

建议流程：

```text
poll 消息
  |
  v
根据 bizId 查询源数据
  |
  v
放入 ThreadLocal 上下文
  |
  v
执行 Handler 责任链
  |
  |-- 任一 Handler 失败 -> 抛异常，记录失败，不提交 Offset 或转失败表
  |
  v
全部成功
  |
  v
提交 Offset
  |
  v
清理 ThreadLocal
```

注意：

- `ThreadLocal.remove()` 必须放在 `finally`。
- 如果目标库是同一个库，责任链外层可以加事务。
- 如果涉及多数据源，要靠幂等和补偿保证最终一致。

## 十六、常见面试题

### 1. Kafka 为什么会重复消费

答：

> 重复消费通常发生在业务处理成功但 Offset 还没提交时，消费者宕机或发生 Rebalance，消息会从旧 Offset 再次拉取。另外 Producer 重试、补偿任务也可能产生重复业务消息。所以消费端必须做业务幂等。

### 2. Kafka 会不会丢消息

答：

> 会，取决于配置和处理方式。消费端如果自动提交 Offset，或者先提交 Offset 再处理业务，业务失败时就可能丢消息。通常要关闭自动提交，在业务处理成功后再提交 Offset。

### 3. Rebalance 是什么

答：

> Rebalance 是 Consumer Group 内 Partition 重新分配的过程。当消费者加入、退出、心跳超时或 Partition 数量变化时会触发。它能实现扩缩容和故障恢复，但频繁 Rebalance 会导致消费暂停和重复消费。

### 4. 如何解决消费积压

答：

> 先看 Consumer Lag，判断是消费实例不足、Partition 不够、单条处理慢还是下游数据库慢。解决方式包括增加 Consumer 实例、增加 Partition、优化业务处理、批量写库、降低单次拉取数量、失败消息转死信避免阻塞。

### 5. 消费端如何保证幂等

答：

> 不能只依赖 Kafka，需要业务层保证。常见做法是使用业务唯一键、目标表唯一索引、消费记录表、状态机校验。比如政务数据同步平台可以用办件唯一 ID 作为幂等键，重复消费时执行更新或跳过。

## 十七、自测清单

- Consumer Group 和 Consumer 是什么关系？
- 为什么 Consumer 数量超过 Partition 数量没有用？
- Offset 是什么，提交 Offset 表示什么？
- 自动提交 Offset 有什么风险？
- 为什么一般选择先处理业务再提交 Offset？
- 重复消费有哪些原因？
- 消息丢失通常怎么发生？
- Rebalance 什么时候触发？
- Rebalance 会带来什么问题？
- 消费积压应该怎么排查？
- 你的数据同步平台如何做消费端幂等？
- Handler 执行到一半失败，Offset 应该提交吗？

## 十八、今天记住的三句话

- Kafka 消费端更应该防消息丢失，重复消费交给业务幂等解决。
- Rebalance 是消费者组的再分配机制，但频繁 Rebalance 会造成消费抖动和重复消费。
- 数据同步平台消费端的核心不是把消息取出来，而是处理好幂等、失败、补偿和 Offset 提交。
