## 三、Spring Boot 启动流程

Spring Boot 项目的启动入口通常是：

  ```java
  @SpringBootApplication
  public class Application {
      public static void main(String[] args) {
          SpringApplication.run(Application.class, args);
      }
  }
 ```
  SpringApplication.run() 是 Spring Boot 启动的核心入口。

  它主要完成几件事：

  1. 创建 SpringApplication 对象
  2. 准备运行环境 Environment
  3. 创建 ApplicationContext 容器
  4. 调用 refresh() 刷新容器
  5. 完成 Bean 创建、自动配置、内嵌 Web 容器启动
  6. 执行 Runner 回调

### 3.1 @SpringBootConfiguration
 是一个组合注解注释，核心包含：
 - @SpringBootConfiguration: 本质是@Configuration,表示当前类是一个配置类。
 - @EnableAutoConfiguration：开启自动配置，通过AutoConfigurationImportSelector加载自动配置清单，通过条件注解决定那些配置生效。
 - @ComponentScan：扫描当前启动类所在包及其子包下的组件。


### 3.2 SpringApplication.run() 大致流程
1. 创建 SpringApplication 对象
2. 推断应用类型：是普通应用、Servlet Web应用、Reactive Web应用
3. 加载 ApplicationContextInitializer
4. 加载 ApplicationListener
5. 准备 Environment: jvm参数、系统环境变量、yml文件、profile信息
6. 打印 Banner
7. 创建 ApplicationContext：SpringBoot根据不同的应用类型创建不同的ApplicationContext
8. refresh 容器:Spring 容器真正创建Bean、处理自动配置、启动Web容器
9. 执行 CommandLineRunner / ApplicationRunner:容器启动完成后，会执行命令

### 3.2 refresh() 核心流程
它来自 Spring Framework，不是 Spring Boot 独有。

核心步骤可以简化理解为：

  ```text
  准备容器环境
  -> 获取 BeanFactory
  -> 执行 BeanFactoryPostProcessor
  -> 注册 BeanPostProcessor
  -> 初始化事件广播器
  -> 注册监听器
  -> 实例化非懒加载单例 Bean
  -> 完成刷新
  ```
```java
    public void refresh() throws BeansException, IllegalStateException {
   synchronized(this.startupShutdownMonitor) {
        StartupStep contextRefresh = this.applicationStartup.start("spring.context.refresh");
        this.prepareRefresh();
        ConfigurableListableBeanFactory beanFactory = this.obtainFreshBeanFactory();
        this.prepareBeanFactory(beanFactory);

        try {
        this.postProcessBeanFactory(beanFactory);
        StartupStep beanPostProcess = this.applicationStartup.start("spring.context.beans.post-process");
        this.invokeBeanFactoryPostProcessors(beanFactory);
        this.registerBeanPostProcessors(beanFactory);
        beanPostProcess.end();
        this.initMessageSource();
        this.initApplicationEventMulticaster();
        this.onRefresh();
        this.registerListeners();
        this.finishBeanFactoryInitialization(beanFactory);
        this.finishRefresh();
        } catch (BeansException var10) {
        if (this.logger.isWarnEnabled()) {
        this.logger.warn("Exception encountered during context initialization - cancelling refresh attempt: " + var10);
        }

        this.destroyBeans();
        this.cancelRefresh(var10);
        throw var10;
        } finally {
        this.resetCommonCaches();
        contextRefresh.end();
        }

        }
        } 
```


### 3.3 自动配置和启动流程的关系
- SpringApplication.run 启动容器
- refresh 过程中解析配置类
- @SpringBootApplication 引入 @EnableAutoConfiguration
- AutoConfigurationImportSelector 加载自动配置类
- 条件注解决定配置是否生效

### 3.4 常见面试题
**Q：Spring Boot 启动流程大概是什么？**
> 答：
> Spring Boot 启动从 SpringApplication.run() 开始。首先创建 SpringApplication 对象，推断应用类型，加载初始化器和监听器；然后准备 Environment，加载配置文件和命令行参数；接着根据应用类型创建 ApplicationContext；然后调用 refresh() 刷新容
器，在这个过程中会解析配置类、注册 BeanDefinition、执行 BeanFactoryPostProcessor 和 BeanPostProcessor、实例化非懒加载单例 Bean，并完成自动配置和 AOP 代理创建。如果是 Web 应用，还会创建并启动内嵌 Tomcat。最后执行 ApplicationRunner 和
CommandLineRunner。

**Q：@SpringBootApplication 包含哪些核心注解？**
> 答：
> 主要包含 @SpringBootConfiguration、@ComponentScan、@EnableAutoConfiguration。@SpringBootConfiguration 表示当前类是配置类，@ComponentScan 负责扫描启动类所在包及子包下的组件，@EnableAutoConfiguration 负责开启自动配置。

**Q：自动配置是在什么时候生效的？**
> 答：
> 自动配置是在容器启动 refresh 过程中生效的。@EnableAutoConfiguration 通过 AutoConfigurationImportSelector 加载自动配置类候选清单，这些配置类会被解析成 BeanDefinition，再通过 @ConditionalOnClass、@ConditionalOnMissingBean 等条件注解决
定是否真正注册 Bean。

**Q：refresh() 为什么重要？**
> 答：
> refresh() 是 Spring 容器启动的核心方法。它完成 BeanFactory 准备、BeanFactoryPostProcessor 执行、BeanPostProcessor 注册、事件广播器初始化、监听器注册、非懒加载单例 Bean 实例化、AOP 代理创建以及容器刷新完成等工作。简单说，Bean 真正被创
建和初始化主要发生在 refresh() 过程中。

**Q：Spring Boot 内嵌 Tomcat 是怎么启动的？**
> 答：
> Web 应用启动时，Spring Boot 创建的是 ServletWebServerApplicationContext。它在 refresh() 的 onRefresh 阶段调用 createWebServer()，通过 TomcatServletWebServerFactory 创建 TomcatWebServer，并启动内嵌 Tomcat。

**Q：ApplicationRunner 和 CommandLineRunner 有什么区别？**
> 答：
> 它们都会在 Spring Boot 容器启动完成后执行。CommandLineRunner 接收原始的 String 数组参数，ApplicationRunner 接收封装后的 ApplicationArguments，后者对命令行参数解析更方便。

  ---