## 二、Spring 事务进阶
 Spring事务本质：声明事务的核心是@Transactional，底层通过AOP代理实现，若无异常则正常提交事务，异常则回滚。
 
 Spring事务= AOP代理 + TransactionalInterceptor+数据库连接+提交/回滚控制

### 2.1 事务隔离级别
Spring 的事务隔离级别最终会映射到底层数据库的隔离级别。

常见隔离级别：

| 隔离级别             | 脏读  | 不可重复读 | 幻读                | 说明            |
|------------------|-----|-------|-------------------|---------------|
| READ_UNCOMMITTED | 可能  | 可能    | 可能                | 可以读到未提交数据     |
| READ_COMMITTED   | 不会  | 可能    | 可能                | 每次读都读已提交版本    |
| REPEATABLE_READ  | 不会  | 不会    | MySQL InnoDB 基本解决 | 同一事务内多次读取结果一致 |
| SERIALIZABLE     | 不会  | 不会    | 不会                | 串行化，性能最低      |

Spring 默认隔离级别：Spring 默认是 Isolation.DEFAULT，Spring 不主动指定隔离级别，而是使用数据库默认隔离级别。

比如：
- MySQL InnoDB 默认是 REPEATABLE_READ
- Oracle 默认是 READ_COMMITTED

timeout/readOnly
- readOnly：readOnly = true 表示这是一个只读事务。
- timeout：@Transactional(timeout = 3)，表示事务最多执行 3 秒，超时后会触发回滚。


### 2.2 Spring 事务传播机制重点
- REQUIRED：默认传播行为，如果当前已经有事务，就加入当前事务，如果当前没有事务，就新建一个事务。
  示例：

  ```java
  @Transactional
  public void createOrder() {
      orderMapper.insert(order);
      stockService.deductStock();
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void deductStock() {
      stockMapper.update(stock);
  }

  如果 createOrder() 已经开启事务，deductStock() 会加入同一个事务。

  结果：

  - 外层回滚，内层也回滚
  - 内层抛异常，默认也会导致整个事务回滚
  - 本质上只有一个事务，一个 Connection
  
- REQUIRES_NEW: REQUIRES_NEW 表示无论当前有没有事务，都新建一个独立事务。
  规则：

    - 如果当前没有事务，就新建事务
    - 如果当前已有事务，先挂起外层事务，再新建一个内层事务
    - 内层事务执行完后提交或回滚，再恢复外层事务

  示例：
    ```java
      @Transactional
      public void createOrder() {
      orderMapper.insert(order);
      auditService.saveAuditLog();
      throw new RuntimeException();
      }
    
      @Transactional(propagation = Propagation.REQUIRES_NEW)
      public void saveAuditLog() {
      auditMapper.insert(log);
      }

  结果：
    - saveAuditLog() 使用独立事务
    - 它可以先提交
    - 即使外层 createOrder() 后面回滚，审计日志也不会回滚

- NESTED: 表示嵌套事务
- SUPPORTS：当前有事务就加入，以非事务执行。
- NOT_SUPPORTED：当前有事务就挂起，以非事务方式执行
- MANDATORY：必须在已有事务中执行，如果当前没有事务就抛异常
- NEVER:必须在非事务环境执行，如果当前有事务就抛异常


### 2.3 事务和数据库连接
- Spring 事务本质绑定数据库连接，控制数据库事务的提交和回滚。
- 一个事务方法中，Spring会从连接池获取一个Connection,并关闭事务的自动提交，然后把这个Connection绑定到当前线程，后续在同一个线程中使用数据库的连接都复用这个Conection.
- TransactionSynchronizationManager:Sping使用TransactionSynchronizationManager管理当前线程的事务资源。
- ThreadLocal：底层使用ThreadLocal保存：
   1. 当前线程绑定的的数据库连接
   2. 当前事务名称
   3. 当前事务是否只读
   4. 当前事务的隔离级别
   5. 事务同步回调
  
- 多线程事务为什么会失效：Spring事务资源通过ThreadLocal绑定当前线程，新线程拿不到父线程绑定的conection，所以多线程场景下，不会自动共享事务。


### 2.4 常见面试题
**Q：Spring 事务的底层实现原理？**
> 答：
> Spring 声明式事务底层是 AOP。调用事务方法时，实际进入的是代理对象，代理对象通过 TransactionInterceptor 获取事务属性，然后通过事务管理器获取数据库连接、关闭自动提交、执行业务方法，最后根据方法是否抛出需要回滚的异常来提交或回滚事务。

**Q：REQUIRED 和 REQUIRES_NEW 有什么区别？**
> 答：
> REQUIRED 是默认传播机制，如果外层有事务就加入外层事务，内外层共用一个事务；REQUIRES_NEW 会挂起外层事务，重新开启一个独立事务，内层事务可以独立提交或回滚，外层后续回滚不会影响已经提交的内层事务。典型场景是审计日志、操作流水这类希望独
立落库的数据。

**Q：REQUIRES_NEW 为什么能独立提交？**
> 答：
> 因为它不是加入外层事务，而是挂起外层事务资源，重新获取连接开启一个新的物理事务。内层事务执行完成后先提交或回滚，再恢复外层事务，所以两个事务的边界是独立的。

**Q：NESTED 和 REQUIRES_NEW 有什么区别？**
> 答：
> REQUIRES_NEW 是真正的新事务，有独立连接和独立提交回滚边界；NESTED 是在当前事务里创建 savepoint，本质还是同一个物理事务。NESTED 可以实现内层局部回滚，但如果外层最终回滚，内层操作也会一起回滚。

**Q：为什么 Spring 事务在多线程中会失效？**
> 答：
> Spring 事务资源是通过 TransactionSynchronizationManager 绑定到当前线程的，底层使用 ThreadLocal 保存连接等事务资源。新线程拿不到父线程绑定的 Connection，所以不会自动加入父线程事务。

**Q：Spring 事务和 MySQL MVCC 是什么关系？**
> 答：
> Spring 管事务边界，比如开启、提交、回滚、设置隔离级别；MySQL 负责真正实现事务隔离和并发控制，比如 MVCC、undo log、read view 和锁。Spring 不实现 MVCC，它只是把隔离级别等配置传递给数据库。

  ---
