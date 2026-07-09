一、explain 执行计划
### 1. 如何使用
  
    EXPLAIN SELECT * FROM user WHERE age = 20;

### 2. 核心字段详解

 - id：执行顺序
 - select_type：查询类型
 - type：访问类型（重点）
    system > const > eq_ref > ref > range > index > ALL
   - system:表只有一行数据（系统表）,极少见。
   - const：主键或唯一索引等值查询，最多命中一行； 
       - eg：select * from user where id= 1; -- type = const;
   - eq_ref: -JOIN时，被驱动表用主键或者唯一索引关联，每次只返回一行
       - eg：select * from order o JOIN user u ON o.user_id = u.id
       - 解释：u是被驱动表，user_id是主键 -> eq_ref;
   - ref： 普通索引等值查询，可能命中多行
       - eg: select * from user where age=20;  --age是普通索引 ->ref;
   - rang: 索引范围查询
      - eg: select * from user where id IN(1,2,3);
   - index:扫描整棵索引树（比ALL好，因为索引比数据小），覆盖索引但需要全扫
   - ALL：全表扫描，最差需要优化。
      - eg：select * from user WHERE name ='张三'; --name 无索引 ->ALL
   - 建议：生产要求：至少达到range级别，核心要达到ref以上。
   
- possible_keys：可能用到的索引
- key：实际用到的索引
- key_len：索引使用长度，越大，说明用的索引列越多。
- rows：预估扫描行数，越小越好，说明索引效果好。
- Extra：额外信息（重点）
  - Using index
    - 覆盖索引，不需要回表，最好的情况
      SELECT id, age FROM user WHERE age = 20;  -- (age) 索引覆盖了所有列

  - Using where
    - 从存储引擎取回数据后，Server 层还需要过滤
    - 说明索引没有完全过滤掉数据，或者没走索引

  - Using index condition
    - ICP 生效，存储引擎层提前过滤，减少回表

  - Using filesort
    - 排序无法用索引，需要额外排序操作，需要优化
      -- age 有索引，但 ORDER BY name 没索引 → filesort
      SELECT * FROM user WHERE age = 20 ORDER BY name;

  - Using temporary
    - 使用了临时表，常见于 GROUP BY、ORDER BY，需要优化
      SELECT age, COUNT(*) FROM user GROUP BY age;  -- age 无索引 → temporary

  - Using index + Using where
    - 走了覆盖索引，但还有条件需要在索引上过滤（不是回表过滤）

二、慢查询日志
1. 开启慢查询日志
slow_query_log = ON
long_query_time = 2
2. 用 mysqldumpslow 分析慢日志
3. 用 pt-query-digest 分析（生产常用）

三、常见优化手段
### 1. 索引优化（覆盖索引、联合索引顺序）
  - 联合索引，最左原则，防止索引失效
### 2. 避免 SELECT *
 - 拉取所有列，无法使用覆盖索引，必须回表，且多余的列占用网络带宽。

### 3. 小表驱动大表（JOIN 顺序）
 - JOIN 查询时，用小表作为驱动表（外层循环），大表作为被驱动表（内层循环）
 
### 4. 深分页优化
- 问题：LIMIT 100000, 10 性能极差
 > SELECT * FROM order ORDER BY id LIMIT 100000, 10;
 > 
 >MySQL 执行过程：扫描前 100010 行，丢掉前 100000 行，返回 10 行。白白扫了 10 万行。

- 方案一：子查询 + 覆盖索引 ，先用覆盖索引找到第 100000 行的 id（不回表，快） ，再用 id 定位后 10 行。
    ```mysql
      
        SELECT * FROM order
        WHERE id >= (
        SELECT id FROM order ORDER BY id LIMIT 100000, 1
        )
        LIMIT 10;
    ```
- 方案二：游标分页（WHERE id > last_id）,前端记住上一页最后一条的 id,下一页直接从 last_id 之后取.
    ```mysql
    SELECT * FROM order WHERE id > last_id ORDER BY id LIMIT 10;
    ```
  走索引直接定位，无论翻到第几页速度都一样快。缺点是不能跳页。


### 5. count(*) 优化
- count(*) vs count(1) vs count(字段) 区别
  1. count(*)        -- 统计所有行，包含 NULL，InnoDB 优化过，推荐
  2. count(1)        -- 和 count(*) 性能相同，推荐
  3. count(字段)     -- 只统计该字段非 NULL 的行，会判断 NULL，略慢
  4. count(主键)     -- 走主键索引，不如 count(*) 快（要取值判断）
- 结论：用 count(*) 或 count(1)，两者性能一样，语义清晰用 count(*)。

### 6. ORDER BY 优化
1. -- 触发 filesort（慢）
  ```text 
     SELECT * FROM user WHERE age = 20 ORDER BY name;
      -- age 有索引，但 name 无索引，排序用不了索引
  ```
2. -- 走索引排序（快）
    ```text
        -- 建联合索引 (age, name)
        SELECT * FROM user WHERE age = 20 ORDER BY name;
        -- age 等值 + name 有序 → Using index，无 filesort
    ```

3. ORDER BY 走索引的条件：
 - 排序列在索引中，且符合最左前缀
 - 排序方向与索引方向一致（都 ASC 或都 DESC）

4. -- 索引 (age ASC, name ASC)
    ```mysql
    ORDER BY age, name          -- 走索引 ✓
    ORDER BY age DESC, name DESC -- 走索引（反向扫）✓
    ORDER BY age ASC, name DESC  -- filesort ✗（方向不一致）
   ```
### 7. GROUP BY 优化
- 避免 Using temporary

### 8. JOIN 优化
- Nested Loop Join 原理
    ```text
      for each row in 驱动表:          ← 外层循环
         for each row in 被驱动表:    ← 内层循环
              if 条件匹配: 输出结果
    ```
- 驱动表选择原则
- 超过 3 张表不建议 JOIN 

四、大表优化方案
 
大表：数据量超过500万行，表文件超过2GB的，查询效率变慢。

1. 冷热数据分离（垂直拆分行）：把不常用的列（冷数据）拆到另一张表。

   - eg:
      ```text
      --- 原表：user（字段很多，行很宽）
      id | name | age | phone | address | last_login | avatar | intro | ...
    
      -- 拆分后：
      user_base（热数据，频繁查询）
      id | name | age | phone | last_login
    
      user_detail（冷数据，很少查）
      id | user_id | address | avatar | intro | ...
      ```
 - 好处：
   1. 热表行更窄，每页能放更多行，IO 效率更高
   2. 热表索引更小，查询更快

2. 垂直分表（按列拆分）
  
   本质和冷热分离一样，按列的访问频率拆表，常见于列数很多（50列以上）的宽表。
    ```text
        原表：order（60列）
        ↓
        order_base（核心字段，20列）
        order_pay（支付相关，15列）
        order_logistics（物流相关，15列）
        order_extra（扩展信息，10列）
    ```
   
3. 水平分表（按行拆分）

数据量太大时，把数据按某个规则拆到多张结构相同的表。

- 按范围分（Range）：
  - eg:
  ```text
    -- 按时间范围分表
    order_2022  -- 2022年的订单
    order_2023  -- 2023年的订单
    order_2024  -- 2024年的订单
   ```
  - 优点：扩展方便，新年建新表
  - 缺点：数据不均匀（热点集中在当前年的表）

- 按哈希分（Hash）：
  - eg：
    ```text
        -- 按 user_id % 4 分到4张表
        order_0  -- user_id % 4 = 0
        order_1  -- user_id % 4 = 1
        order_2  -- user_id % 4 = 2
        order_3  -- user_id % 4 = 3
    ```
  - 优点：数据均匀
  - 缺点：扩容困难（取模数变了，数据要重新迁移）

- 水平分表的问题：
  - 跨表查询复杂（分页、排序、聚合都要改造）
  - 事务跨表变成分布式事务
  - 一般配合 ShardingSphere 或 MyCat 中间件使用

4. 归档历史数据

  把历史冷数据迁移到归档表或归档库，保持主表数据量可控。
 - eg：
    ```text
    -- 每月把3个月前的订单归档
    INSERT INTO order_archive SELECT * FROM order WHERE create_time < '2024-01-01';
    DELETE FROM order WHERE create_time < '2024-01-01';
    ```

   生产一般用定时任务（xxl-job）或 pt-archiver 工具执行，避免一次性大批量操作锁表。


6. 选型建议

 - 数据量 < 500万        → 先优化索引和SQL，不急于拆表 
 - 数据量 500万~5000万   → 冷热分离 + 归档历史数据
 - 数据量 > 5000万       → 水平分表（ShardingSphere）
 - 列数特别多（> 30列）  → 垂直分表
 - 查询时间跨度固定      → 考虑分区表