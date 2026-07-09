## 四、Spring 常用扩展点
 Spring容器不是一个封闭的盒子，它在启动和Bean创建的不同阶段，预留了很多扩展点，这些扩展点允许开发者或者框架在Sprign生命周期插入自定义逻辑。

 常见场景：
- 修改 BeanDefinition
- 处理自定义注解
- 创建代理对象
- 动态导入配置类
- 创建复杂 Bean
- 发布和监听容器事件

### 4.1 BeanPostProcessor
- 作用：Bean 初始化前后扩展
- AOP 代理就是通过 BeanPostProcessor 完成
- 典型实现：AutowiredAnnotationBeanPostProcessor、AnnotationAwareAspectJAutoProxyCreator

### 4.2 BeanFactoryPostProcessor
- 作用：修改 BeanDefinition
- 执行时机早于 Bean 实例化
- 典型实现：PropertySourcesPlaceholderConfigurer

### 4.3 FactoryBean
- 作用：自定义复杂 Bean 的创建逻辑
- getObject()
- getObjectType()
- isSingleton()+
- 和普通 BeanFactory 的区别

### 4.4 ImportSelector
- 作用：根据条件动态导入配置类
- 自动配置底层使用 AutoConfigurationImportSelector

### 4.5 ApplicationListener / ApplicationEvent
- 事件发布订阅机制
- 容器刷新事件
- 业务解耦场景

### 4.6 常见面试题
**Q：BeanPostProcessor 和 BeanFactoryPostProcessor 有什么区别？**

**Q：AOP 为什么和 BeanPostProcessor 有关系？**

**Q：FactoryBean 和 BeanFactory 有什么区别？**

**Q：ImportSelector 在 Spring Boot 自动配置里起什么作用？**

**Q：Spring 事件机制适合解决什么问题？**

  ---