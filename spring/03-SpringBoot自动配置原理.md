# SpringBoot自动配置原理（@EnableAutoConfiguration、条件注解、starter机制）


## 一、先搞懂SpringBoot解决了什么问题

写传统Spring项目的时候，要用一个东西（比如Redis），得自己：
1. 引入Redis相关jar包
2. 手动写一堆配置类，配`RedisTemplate`、连接池、序列化方式
3. 把这些配置类注册到容器里

非常繁琐。SpringBoot的核心理念是**"约定大于配置"**——你只要引入一个 `spring-boot-starter-data-redis` 依赖，几乎不用写任何配置，SpringBoot自动帮你把`RedisTemplate`等一系列Bean都配好放进容器，你直接`@Autowired`拿来用就行。

一句话记忆：**SpringBoot自动配置 = 把"你大概率会怎么配"提前帮你写好，你不用配置就能用，需要自定义时再覆盖**。
> 口语化： SpringBoot的核心理念就是约定大于配置，之前传统项目，需要引用很多的繁琐的配置，现在只需要引入Spring的组件包，就可以直接@Autowired直接使用。
---

## 二、@EnableAutoConfiguration：自动配置的入口

### 2.1 它藏在哪里

写SpringBoot项目，启动类上有个`@SpringBootApplication`注解，这个注解其实是**三个注解的组合**：

```java
@SpringBootConfiguration   // 本质就是@Configuration，标记这是个配置类
@ComponentScan             // 扫描当前包及子包下的@Component等注解
@EnableAutoConfiguration   // 重点！开启自动配置的开关
```

### 2.2 @EnableAutoConfiguration做了什么

这个注解内部通过`@Import(AutoConfigurationImportSelector.class)`，导入了一个**选择器类**。这个选择器的核心工作是：

1. **去找"候选配置类清单"**：SpringBoot会去扫描所有jar包里一个叫 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 的文件（老版本是`spring.factories`），这个文件里**列出了所有可能要自动配置的类**，比如`RedisAutoConfiguration`、`DataSourceAutoConfiguration`等等，可能有上百个。
2. **把这些候选配置类全部加载进来，逐一判断要不要真正生效**——这一步就要靠"条件注解"来过滤。

> 记忆点：**`@EnableAutoConfiguration`不是"直接配置好一切"，而是"先列出一份很长的候选清单，再用条件注解去筛选哪些真正生效"**。
> 口语化： EnableAutoConfiguration，Spring自动扫描所有的jar包去拿到需要自动配置的候选清单，然后逐个去加载，根据注解条件去判断那些真正需要自动配置并使用。
---

## 三、条件注解：决定"要不要生效"的筛选器

每一个自动配置类，上面都贴满了各种`@Conditional`系列的注解，常见的有：

| 注解 | 含义 |
|---|---|
| `@ConditionalOnClass` | 当前classpath下存在某个类，才生效（比如有Redis相关的jar包） |
| `@ConditionalOnMissingBean` | 容器里**还没有**某个Bean时才生效（你自己配置了就不用我帮你配） |
| `@ConditionalOnBean` | 容器里**已经有**某个Bean时才生效（依赖别的Bean先存在） |
| `@ConditionalOnProperty` | 配置文件里某个属性满足特定值时才生效（比如`spring.redis.enabled=true`） |
| `@ConditionalOnWebApplication` | 当前是Web应用时才生效 |

### 3.1 举一个真实的例子：RedisAutoConfiguration

简化版逻辑大概是这样：

```java
@Configuration
@ConditionalOnClass(RedisOperations.class) // classpath里有Redis相关类才生效
@EnableConfigurationProperties(RedisProperties.class)
public class RedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate") // 你自己没配过，我才帮你配
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        return template;
    }
}
```

**这就是为什么**：
- 如果你项目里没引入Redis相关依赖（classpath里没有`RedisOperations`这个类），`RedisAutoConfiguration`整个类都不会生效，自然不会有多余的Bean。
- 如果你自己在代码里写了一个`@Bean public RedisTemplate redisTemplate()`，SpringBoot会"让位"——`@ConditionalOnMissingBean`检测到你已经有了，就不会再用它默认的那一份去覆盖你的。

> 这个"让位"机制就是**自动配置可以被覆盖**的原理，面试官常问"如果我自己配了一个Bean，SpringBoot默认的还会生效吗"，答案就是不会，靠的就是`@ConditionalOnMissingBean`。
> 让位机制，Spring如果发现你自己配了一个bean，它就不会生效，靠的是@ConditionalOnMissingBean
---


## 四、Starter机制：约定优于配置的具体落地

### 4.1 Starter到底是什么

**Starter本身不包含任何业务代码，它就是一个"依赖整合器"**——一个`pom.xml`，把"用某个技术需要的所有相关依赖"打包到一起。

比如 `spring-boot-starter-data-redis` 这个starter，它的`pom.xml`里其实引入了：
- `spring-data-redis`（Redis操作的核心库）
- `lettuce-core`（默认的Redis客户端）
- 一些通用的`spring-boot-starter`基础依赖

你只要在自己项目里引入这一个starter，相当于**自动引入了一整套配套的依赖**，不需要自己一个个去找、去对版本号。    

>口语化： Starts本身不包含任何业务代码，其实就将一个技术的所有的依赖打包在一起，比如spring-boot-starter-data-redis。引入了，就可以直接使用redis。

### 4.2 Starter和自动配置是怎么配合的

完整链路是这样的：

```
你引入 spring-boot-starter-data-redis
        ↓
这个starter把 Redis相关的jar包（包括spring-boot-autoconfigure里的RedisAutoConfiguration所在的包）都带进了你的classpath
        ↓
项目启动时，@EnableAutoConfiguration扫描候选配置类清单，发现了RedisAutoConfiguration
        ↓
RedisAutoConfiguration上的@ConditionalOnClass检测：classpath里有没有Redis相关的类？
        ↓
因为你刚好通过starter引入了这些类，条件满足 → RedisAutoConfiguration生效
        ↓
RedisTemplate等一系列Bean被自动创建，放入容器
        ↓
你直接 @Autowired 就能用
```

**一句话总结这条链路**：**starter负责"把依赖带进来"，自动配置类负责"判断该不该生效、生效了配什么"，条件注解是这两者之间的"裁判"**。

### 4.3 命名规律（顺带记一下）

- `spring-boot-starter-xxx`：Spring官方提供的starter
- `xxx-spring-boot-starter`：第三方公司/个人提供的starter（比如`mybatis-plus-spring-boot-starter`）

这是Spring官方定的命名规范，方便区分"官方的"和"第三方的"。

---

## 五、整体串联记忆

```
你引入一个starter（依赖整合）
   ↓
classpath里出现了某些特定的类
   ↓
@EnableAutoConfiguration 加载候选配置类清单（可能有上百个自动配置类）
   ↓
每个候选配置类身上的 @ConditionalOnClass / @ConditionalOnMissingBean 等条件注解
   逐一判断："classpath里有没有对应的类？容器里是否已经有相关Bean？"
   ↓
条件满足的配置类才真正生效，把对应的Bean注册进容器
   ↓
你@Autowired直接用，不用自己手写一堆配置
```
>口语化： 介绍starter引入到bean使用的大致流程：比如引入一个redis的starter，redis的依赖集合，classpath有redis的特定类，
> @EnableAutoConfiguration加载候选配置清单，根据候选配置清单的条件，判断是否满足，满足，则将Bean加入到容器里，然后@Autowired注解真正使用。

记住这个流程图，基本可以应对面试官从starter问到条件注解、再问到自动配置类的连环追问。

---

## 自测清单（晚上闭卷口述）

- [ ] SpringBoot自动配置解决了什么问题，"约定大于配置"具体指什么
- [ ] @SpringBootApplication由哪三个注解组成，各自的作用
- [ ] @EnableAutoConfiguration做了什么——重点是"候选清单"这个概念，清单在哪个文件里
- [ ] 至少说出3个常见的条件注解及其含义
- [ ] 完整讲一遍"为什么自己配了一个Bean，SpringBoot默认的不会覆盖它"——靠的是哪个注解
- [ ] Starter本身包含业务代码吗？它的本质作用是什么
- [ ] 完整串讲一遍"引入一个starter到Bean能被使用"的整条链路

> 如果上面有任何一项讲不顺，回到对应小节重新过一遍，再口述一次。