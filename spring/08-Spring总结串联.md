## 五、Spring 总结串联

### 5.1 IOC 主线
配置解析 -> BeanDefinition -> 实例化 -> 属性填充 -> 初始化 -> BeanPostProcessor -> AOP 代理 -> 单例池

### 5.2 AOP 主线
切点匹配 -> 创建代理对象 -> 方法调用进入代理 -> 执行增强逻辑 -> 调用目标方法

### 5.3 事务主线
@Transactional -> AOP 代理 -> TransactionInterceptor -> 获取连接 -> 开启事务 -> 执行业务 -> 提交 / 回滚

### 5.4 Boot 主线
@SpringBootApplication -> @EnableAutoConfiguration -> AutoConfigurationImportSelector -> 自动配置类 -> 条件注解 -> Bean 注册

### 5.5 MVC 主线
请求 -> DispatcherServlet -> HandlerMapping -> HandlerAdapter -> Controller -> 参数解析 -> 返回值处理 -> JSON 响应