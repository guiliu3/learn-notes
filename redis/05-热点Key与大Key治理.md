# 05-热点Key与大Key治理

## 1. 面试先给结论

Redis 线上问题不能只会说缓存穿透、击穿、雪崩。对 8 年 Java 来说，更要能讲清楚热点 Key、大 Key、缓存命中率下降、Redis 慢命令、内存淘汰、集群倾斜这些真实生产问题。

面试表达可以这样说：

> 我理解 Redis 线上治理重点是两类问题：一类是流量问题，比如热点 Key 被大量访问，导致单个 Redis 节点压力过高；另一类是数据结构问题，比如大 Key 造成网络传输慢、删除阻塞、集群迁移困难。排查时我会结合 Redis 命中率、QPS、慢日志、内存、网络、key 大小分布和业务访问模式来判断，而不是只看应用报错。

## 2. 热点 Key 是什么

热点 Key 指某个 Key 被极高频访问。

例如：

```text
exam:score:notice
exam:query:config
exam:school:1001:rank
product:detail:10086
```

特点：

- 单个 Key QPS 很高。
- 压力集中在一个 Redis 节点。
- 可能导致该节点 CPU、网络打满。
- 集群模式下也无法靠自动分片解决，因为同一个 Key 只能落到一个 slot。❓



## 3. 热点 Key 常见场景

中考查询系统：

- 查询开放时间配置。
- 系统公告。
- 某学校成绩统计。
- 查询结果状态。

数据同步平台：

- 同步任务全局配置。
- 某个业务类型的同步开关。
- 某批次同步状态。
- 某个失败批次被大量刷新。

电商系统：

- 秒杀商品详情。
- 热门商品库存。
- 首页配置。
- 活动信息。

## 4. 热点 Key 有什么危害

### 4.1 单节点压力过高

Redis Cluster 按 slot 分片，但同一个 Key 只会落在一个节点。

一个热点 Key 会导致：

- 单个节点 CPU 高。
- 网络出口高。
- RT 增加。
- 慢命令增多。
- 其他 Key 访问受影响。

### 4.2 缓存击穿

热点 Key 过期瞬间，大量请求同时打到数据库。

例如中考查询系统：

```text
成绩查询配置 key 过期
大量请求同时读取配置
缓存未命中
请求打到 DB
```

### 4.3 影响集群均衡

集群中某个节点因为热点 Key 压力远高于其他节点，形成访问倾斜。

## 5. 热点 Key 怎么发现

### 5.1 业务日志统计

在应用层统计访问频率最高的 Key。

优点：

- 能结合业务维度。
- 能知道是谁在访问。

缺点：

- 需要提前埋点。

### 5.2 Redis 监控

关注：

- Redis QPS。
- CPU。
- 网络输入输出。
- 慢命令。
- 命中率。
- 单节点负载是否倾斜。

### 5.3 Redis 命令

可以使用：

```bash
redis-cli --hotkeys
```

注意：

- 该命令依赖 LFU 相关统计。
- 生产环境使用要谨慎。
- 大集群下更推荐通过监控和采样发现。

## 6. 热点 Key 怎么治理

### 6.1 本地缓存

适合读多写少、允许短时间不一致的数据。

例如：

- 查询开放时间。
- 系统公告。
- 字典配置。
- 限流规则。

架构：

```text
应用本地缓存 -> Redis -> DB
```

优点：

- 减少 Redis 压力。
- 响应快。

风险：

- 多实例本地缓存一致性问题。
- 更新后需要通知刷新。
- 示例：
```java
@Service
public class ConfigService {

    private final Cache<String, String> localCache =
            Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(Duration.ofMinutes(5))
                    .build();

    private final StringRedisTemplate redisTemplate;

    public ConfigService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String getConfig(String key) {
        return localCache.get(key, cacheKey ->
                redisTemplate.opsForValue().get(cacheKey)
        );
    }
}
```

- 数据一致性解决办法：
```text
更新数据库
  ↓
删除 Redis 缓存
  ↓
通过 MQ / Redis PubSub 通知各应用节点
  ↓
各节点删除 Caffeine 本地缓存
```


### 6.2 Key 拆分

把一个热点 Key 拆成多个副本 Key。

例如：

```text
exam:notice:0
exam:notice:1
exam:notice:2
exam:notice:3
```

读取时随机读一个副本。

适合：

- 内容相同。
- 更新频率低。
- 读流量极高。
- 示例：
```java
        int index = ThreadLocalRandom.current().nextInt(1, 5);
        String key = "product:10001:copy:" + index;
        String value = redisTemplate.opsForValue().get(key);
```
### 6.3 永不过期 + 异步刷新

对热点配置类数据，可以不设置短 TTL，而是后台异步刷新。

示例：

```text
缓存中保存数据 + 逻辑过期时间
请求发现逻辑过期
先返回旧值
异步线程刷新缓存
```

优点：

- 避免热点 Key 物理过期瞬间打穿 DB。

缺点：

- 可能短时间返回旧数据。

### 6.4 分布式锁重建缓存

热点 Key 失效时，只允许一个线程重建缓存。

流程：

```text
1. 查询 Redis 未命中。
2. 尝试获取分布式锁。
3. 获取锁的线程查 DB 并写缓存。
4. 其他线程短暂等待或返回兜底。
```

注意：

- 锁要有过期时间。
- 重建失败要释放锁。
- 不要让大量线程无限等待。

## 7. 大 Key 是什么

大 Key 指单个 Key 占用内存过大，或者集合元素过多。

常见例子：

```text
String value 超过几 MB
Hash 有几十万字段
List 有几十万元素
Set/ZSet 有大量成员
```

没有绝对统一标准，但常见判断：

- String 类型超过 1MB 要关注。
- Hash/List/Set/ZSet 元素超过几万要关注。
- 单次读取或删除耗时明显增加要关注。

## 8. 大 Key 有什么危害

### 8.1 网络传输慢

读取大 Key 会产生大报文，增加网络和序列化成本。

### 8.2 阻塞 Redis

Redis 单线程执行命令，大 Key 删除、遍历、聚合可能阻塞其他请求。

### 8.3 集群迁移困难

Redis Cluster 迁移 slot 时，大 Key 会导致迁移慢、阻塞时间长。

### 8.4 内存倾斜

某个节点因为大 Key 占用大量内存，导致集群不均衡。

## 9. 大 Key 怎么发现

### 9.1 redis-cli bigkeys

```bash
redis-cli --bigkeys
```

作用：

- 扫描各类型最大的 Key。

注意：

- 生产环境使用要谨慎。
- 大实例建议低峰期执行。
- 更推荐从备库或离线 RDB 分析。

### 9.2 MEMORY USAGE

```bash
MEMORY USAGE key
```

查看某个 Key 的内存占用。

### 9.3 慢日志

```bash
SLOWLOG GET 10
```

如果频繁出现大集合操作，要重点关注。

### 9.4 RDB 离线分析

生产环境更稳妥：

- 导出 RDB。
- 离线分析 Key 大小。
- 找出大 Key 和类型分布。

## 10. 大 Key 怎么治理

### 10.1 拆分 Key

例如一个 Hash 太大：

```text
user:all
```

拆为：

```text
user:bucket:0
user:bucket:1
user:bucket:2
```

或按业务维度拆：

```text
exam:school:1001:students
exam:school:1002:students
```

### 10.2 分页读取

不要一次性读取全部集合。

使用：

- SSCAN。
- HSCAN。
- ZSCAN。

避免：

```bash
HGETALL big_hash
SMEMBERS big_set
LRANGE big_list 0 -1
```

### 10.3 异步删除

删除大 Key 不要直接 `DEL`，优先使用：

```bash
UNLINK key
```

`UNLINK` 会异步释放内存，减少阻塞。

### 10.4 控制 value 大小

不要把完整大对象、列表、报表结果全部塞进一个 String。

可以：

- 只缓存必要字段。
- 按页缓存。
- 按业务维度拆分。
- 大文件放对象存储，Redis 只存引用。

## 11. 中考查询系统中的 Redis 治理

### 11.1 热点 Key

可能热点：

- 查询开放时间配置。
- 成绩查询公告。
- 某些学校查询结果。
- 准考证号查询结果。

设计建议：

- 查询开放时间用本地缓存 + Nacos/Redis 刷新。
- 查询公告使用多副本 Key 或本地缓存。
- 成绩结果缓存设置随机 TTL，避免集中失效。
- 对准考证号做合法性校验，避免穿透。
- 对热点参数做 Sentinel 限流。

### 11.2 大 Key

不要把全校、全县成绩列表作为一个大 Key。

错误做法：

```text
exam:all:scores -> 包含 5.5 万考生成绩
```

更合理：

```text
exam:score:{ticketNo}
exam:school:{schoolId}:summary
exam:query:result:{requestId}
```

面试表达：

> 中考查询系统里，我不会把所有成绩作为一个大 Key 缓存在 Redis。高峰查询主要按准考证号查，应该按准考证号粒度缓存结果，并设置随机 TTL。查询开放时间、公告这种热点配置可以用本地缓存或多副本 Key，避免单个 Redis 节点压力过大。

## 12. 数据同步平台中的 Redis 治理

Redis 可以用于：

- 同步任务开关。
- 短期同步状态缓存。
- 幂等标记。
- 分布式锁。
- 失败统计。

注意：

- 幂等最终不能只依赖 Redis，要有数据库唯一约束兜底。
- 同步状态缓存不能替代数据库真实状态。
- 批量失败记录不要全部塞进一个 Key。
- 重推批次状态要避免大 Hash。

面试表达：

> 数据同步平台里，Redis 可以缓存同步开关、短期状态和失败统计，但不能把它作为最终一致性的唯一依据。比如幂等判断可以用 Redis 提前拦截，但最终还要靠数据库唯一索引。失败记录也不能全部放到一个大 Hash 里，否则查询和删除都会有风险。

## 13. 高频问题

### 13.1 什么是热点 Key

答法：

> 热点 Key 是被大量请求集中访问的 Key，可能导致单个 Redis 节点 CPU 或网络压力过高。即使是 Redis Cluster，同一个 Key 也只能落在一个 slot，所以热点 Key 不能靠普通分片自动解决。

### 13.2 热点 Key 怎么处理

答法：

> 可以用本地缓存、多副本 Key、逻辑过期异步刷新、分布式锁重建缓存等方式。具体要看数据是否允许短时间不一致、更新频率和访问量。

### 13.3 什么是大 Key

答法：

> 大 Key 是单个 Key 占用内存很大，或者集合元素很多。比如几 MB 的 String、几十万字段的 Hash、几十万元素的 Set/List/ZSet。

### 13.4 大 Key 有什么危害

答法：

> 大 Key 会导致网络传输慢、命令执行阻塞、删除阻塞、集群迁移困难和内存倾斜。

### 13.5 大 Key 怎么治理

答法：

> 先通过 bigkeys、MEMORY USAGE、慢日志或 RDB 离线分析发现。治理上可以拆分 Key、分页读取、避免 HGETALL/SMEMBERS 全量操作、删除时用 UNLINK，并控制缓存对象大小。

## 14. 今日学习任务

你需要能口述：

- 热点 Key 是什么，为什么 Redis Cluster 也解决不了单 Key 热点。
- 热点 Key 的发现和治理方式。
- 大 Key 是什么，有什么危害。
- 为什么不能把全量成绩列表塞到一个 Redis Key。
- 数据同步平台里 Redis 幂等为什么不能替代 DB 唯一索引。

