# Kafka Java 代码实验

> 组件启动放在 `component-lab/kafka`，Kafka 概念理解放在这里的 Java 代码。

## 先启动 Kafka

打开 PowerShell：

```powershell
cd D:\DpSoftware\workspace\idea_github\component-lab\kafka\scripts
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\02-start-kafka.ps1
```

如果还没有初始化过，先执行一次：

```powershell
.\01-format-storage.ps1
```

`02-start-kafka.ps1` 的窗口不要关。

## 在 IDE 中按顺序运行

### 1. `Kafka01AdminTopicDemo`

作用：

- 创建 `government-sync` Topic。
- 设置 3 个 Partition。
- 查看 Topic、Partition、Leader、Replicas、ISR。

理解：

- Topic 是逻辑分类。
- Partition 是真正存储消息的单位。
- 单节点实验里 Leader、Replicas、ISR 都是同一个 Broker。

### 2. `Kafka02ProducerKeyPartitionDemo`

作用：

- 发送模拟政务办件消息。
- 使用 `bizId` 作为 Kafka message key。
- 打印每条消息进入哪个 Partition、Offset 是多少。

理解：

- Producer 负责写消息。
- Key 会影响消息进入哪个 Partition。
- 同一个 bizId 通常会进入同一个 Partition。

### 3. `Kafka03ConsumerGroupDemo`

作用：

- 启动消费者组 `sync-service-group`。
- 消费 `government-sync` 消息。
- 手动提交 offset。

建议：

- 在 IDE 里启动两个 `Kafka03ConsumerGroupDemo` 实例。
- 再运行 Producer 发送消息。
- 观察两个 Consumer 如何分摊消息。

理解：

- 同一个 Consumer Group 内，一条消息只会被一个 Consumer 消费。
- Offset 表示消费进度。
- 业务处理成功后再提交 offset，可以避免业务失败导致消息丢失。

### 4. `Kafka04ConsumerGroupLagDemo`

作用：

- 查看 `sync-service-group` 每个 Partition 的 currentOffset、logEndOffset、lag。

理解：

- `currentOffset`：消费组提交到哪里。
- `logEndOffset`：Partition 最新位置。
- `lag`：还积压多少消息。

### 5. `Kafka05ConsumerGroupAssignmentDemo`

作用：

- 查看 `sync-service-group` 中当前有哪些消费者实例。
- 查看每个消费者实例被分配了哪些 Partition。
- 反向查看每个 Partition 当前归哪个 Consumer 消费。

使用方式：

- 先启动两个 `Kafka03ConsumerGroupDemo` 实例，不要关闭。
- 再运行 `Kafka05ConsumerGroupAssignmentDemo`。

理解：

- Consumer Group 的分配单位是 Partition，不是单条消息。
- 同一个 Partition 同一时刻只会分配给组内一个 Consumer。
- 如果消息都落到一个 Partition，就只有负责这个 Partition 的 Consumer 能消费到。

## 推荐学习方式

1. 先运行 `Kafka01AdminTopicDemo` 看 Topic 和 Partition。
2. 运行 `Kafka02ProducerKeyPartitionDemo` 发消息，看 key、partition、offset。
3. 运行 `Kafka03ConsumerGroupDemo` 消费消息。
4. 再运行 `Kafka04ConsumerGroupLagDemo` 看 lag。
5. 运行 `Kafka05ConsumerGroupAssignmentDemo` 看消费者和分区分配关系。
6. 回到 Kafka UI 里观察同样的数据。
