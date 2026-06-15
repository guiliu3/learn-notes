一、explain 执行计划
1. 如何使用
2. 核心字段详解
- id：执行顺序
- select_type：查询类型
- type：访问类型（重点）
system > const > eq_ref > ref > range > index > ALL
- possible_keys：可能用到的索引
- key：实际用到的索引
- key_len：索引使用长度
- rows：预估扫描行数
- Extra：额外信息（重点）
Using index / Using where / Using filesort /
Using temporary / Using index condition(ICP)

二、慢查询日志
1. 开启慢查询日志
slow_query_log = ON
long_query_time = 2
2. 用 mysqldumpslow 分析慢日志
3. 用 pt-query-digest 分析（生产常用）

三、常见优化手段
1. 索引优化（覆盖索引、联合索引顺序）
2. 避免 SELECT *
3. 小表驱动大表（JOIN 顺序）
4. 深分页优化
- 问题：LIMIT 100000, 10 性能极差
- 方案一：子查询 + 覆盖索引
- 方案二：游标分页（WHERE id > last_id）
5. count(*) 优化
- count(*) vs count(1) vs count(字段) 区别
6. ORDER BY 优化
- filesort vs 索引排序
- 如何避免 filesort
7. GROUP BY 优化
- 避免 Using temporary
8. JOIN 优化
- Nested Loop Join 原理
- 驱动表选择原则
- 超过 3 张表不建议 JOIN

四、大表优化方案
1. 分页优化（深分页）
2. 冷热数据分离
3. 垂直分表（列拆分）
4. 水平分表（行拆分）
5. 归档历史数据

五、面试题汇总
Q: explain 的 type 字段各值代表什么？
Q: Extra 出现 Using filesort 怎么优化？
Q: LIMIT 深分页为什么慢，如何优化？
Q: count(*) 和 count(1) 哪个快？
Q: JOIN 查询如何选择驱动表？
Q: 大表如何做优化？