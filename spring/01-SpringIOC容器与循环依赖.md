# IOC容器原理、Bean生命周期、@Autowired底层（三级缓存解决循环依赖）

## 一、IOC容器原理

### 1.1 什么是IOC

IOC（Inversion of Control，控制反转）是一种设计思想：对象的创建、依赖关系的维护，不再由程序员手动 `new` 出来再组装，而是交给容器统一管理。**DI（依赖注入）是IOC的一种实现方式**——容器在创建对象时，自动把它依赖的其他对象"注入"进来。

一句话记忆：**以前是"我去找依赖"，现在是"容器把依赖给我"**。

### 1.2 核心组件

- **BeanFactory**：IOC容器的顶层接口，定义了最基础的获取Bean的能力（`getBean()`）。
- **ApplicationContext**：BeanFactory的子接口，功能更强大，增加了国际化、事件发布、AOP集成等企业级特性，平时用的Spring容器基本都是它。
- **BeanDefinition**：描述一个Bean的"配置信息"（class是谁、作用域、依赖了谁、初始化方法是什么），容器先把XML/注解解析成BeanDefinition，再根据它去真正创建Bean实例。


### 1.3 容器启动的大致流程

1. **加载并解析配置**：扫描包（`@ComponentScan`）或解析XML，把所有候选类封装成 `BeanDefinition`，注册到 `BeanDefinitionRegistry` 中（这一步还没创建对象，只是登记"有哪些Bean、怎么创建"）。
2. **实例化Bean**：遍历BeanDefinition，通过反射调用构造方法创建对象。
3. **依赖注入**：给对象的字段/setter方法注入它依赖的其他Bean。
4. **初始化**：调用 `Aware` 接口回调、`BeanPostProcessor` 前置处理、`init-method`/`@PostConstruct`、`BeanPostProcessor` 后置处理（AOP代理通常在这一步生成）。
5. **使用**：Bean放入单例池，可以被取用了。
6. **销毁**：容器关闭时调用 `destroy-method`/`@PreDestroy`。

> 口语化理解: 
> 第一步：Spring容器启动的时候，会去扫描那些类需要交给spring管理，然后去解析配置，形成BeanDefinition。
> 第二步：根据第一步形成的BeanDefinition，去实例化Bean,通过反射去创建对象。
> 第三步：第二步形成的对象还会进行字段和依赖对象的注入，而不是自己去new。
> 第四步：对象的初始化操作，前置处理，初始化方法执行，后置处理，此时可以进行AOP增强，比如事务、日志等。
> 第五步：此时对象可以正常使用，将形成的对象放入到单例池中。
> 第六步：应用关闭，Spring容器会逐个调用对象的销毁方法，做对象的收尾工作。


> 面试官常问"Spring容器启动做了什么"，回答框架就是：**解析配置成BeanDefinition → 实例化 → 依赖注入 → 初始化（含AOP代理）→ 放入单例池**。

---

## 二、Bean生命周期（详细版）

把上面流程拆得更细，按真实调用顺序排：

```
1. 实例化（调用构造方法，通过反射 new 出对象）
2. 属性填充（依赖注入，给字段/setter赋值）
3. Aware接口回调（如果实现了相关接口）
   - BeanNameAware       → 注入自己的Bean名称
   - BeanFactoryAware    → 注入BeanFactory
   - ApplicationContextAware → 注入ApplicationContext
4. BeanPostProcessor.postProcessBeforeInitialization（前置处理）
5. 初始化方法
   - @PostConstruct 标注的方法
   - 实现InitializingBean接口的 afterPropertiesSet()
   - 自定义的 init-method
6. BeanPostProcessor.postProcessAfterInitialization（后置处理）
   ⭐ AOP动态代理就是在这一步生成的：AnnotationAwareAspectJAutoProxyCreator
     本质就是一个BeanPostProcessor，在这里判断该Bean是否需要代理，
     如果需要就返回代理对象替换原始对象
7. Bean可以正常使用了（放入singletonObjects单例池）
... 容器运行期间，Bean被使用 ...
8. 销毁前回调
   - @PreDestroy
   - DisposableBean.destroy()
   - 自定义 destroy-method
```

**记忆口诀**：先有壳（实例化）→ 装内容（属性填充）→ 知道自己是谁（Aware）→ 出厂前检查两次（Before/After）→ 中间做初始化（顺带生成AOP代理）→ 上岗使用 → 退休清理。

> **面试加分点**：很多人只答到"实例化→注入→初始化"，能额外指出"AOP代理是在 `postProcessAfterInitialization` 阶段生成的"，会显得理解更深。

---

## 三、@Autowired底层 & 三级缓存解决循环依赖

### 3.1 @Autowired是怎么工作的

`@Autowired` 的解析靠的也是一个 `BeanPostProcessor`：`AutowiredAnnotationBeanPostProcessor`。它在Bean的**属性填充阶段**，扫描该Bean的字段/方法上有没有 `@Autowired` 注解，如果有，就去容器里按**类型**查找匹配的Bean（如果有多个候选，再按**名称**/`@Qualifier`/`@Primary`决定用哪个），然后通过反射把这个依赖塞进去。

一句话：**@Autowired = 反射 + 容器查找 + 类型匹配（多个候选时按名称/Primary/Qualifier裁决）**。

> 口语化：Bean的属性填充阶段时，扫描该Bean的字段或方法是否@Autowired注解，容器就根据类型去匹配Bean，如果只有拥有多个，则根据名称/Qualifier/Primary 做二次判断，然后用反射塞进去。


### 3.2 什么是循环依赖

A依赖B，B又依赖A：

```java
@Component
class A {
    @Autowired
    private B b;
}

@Component
class B {
    @Autowired
    private A a;
}
```

如果按照"先把A完全创建好，再创建B"的思路：创建A → 发现A需要B → 去创建B → 发现B需要A → 又要去创建A → ……死循环。

**Spring只能解决"单例 + 字段/setter注入"的循环依赖，解决不了"构造方法注入"的循环依赖**（因为构造方法注入要求对象创建时就把依赖准备好，根本没有"先造一个半成品"的机会）。这是高频追问点，一定要记住。

### 3.3 三级缓存机制

核心思路：**把对象的"实例化"和"属性填充/初始化"拆成两个阶段，提前暴露一个"半成品"对象出去**。

三级缓存其实是三个Map：

| 缓存 | 名称 | 存的是什么 |
|---|---|---|
| 一级缓存 | `singletonObjects` | 完全初始化好的、可以直接用的成品Bean |
| 二级缓存 | `earlySingletonObjects` | 提前暴露出来的"半成品"原始对象（已实例化，未完成属性填充） |
| 三级缓存 | `singletonFactories` | 一个工厂（`ObjectFactory`），调用它能拿到一个对象——这个对象可能是原始对象，也可能是AOP代理后的对象 |

> 口语化：三级缓存：
>    -  一级缓存：装载的是完全初始化好的，可以正常使用的Bean. 
>    -  二级缓存：装载的是半成品的Bean.只是实例化好了，但是属性没有设置。 
>    -  三级缓存：装载的是一个对象工厂，调用它可以获取对象，产生的对象可能是原始的或者AOP代理的

```java
   // 一级缓存：成品Bean
   Map<String, Object> singletonObjects;

  // 二级缓存：半成品（已实例化未完成属性填充/可能已代理）
  Map<String, Object> earlySingletonObjects;

  // 三级缓存：对象工厂
  Map<String, ObjectFactory<?>> singletonFactories;
```

### 3.4 完整执行过程（拿A、B举例）

1. 开始创建A → 先实例化A（此时A是个"光秃秃的壳"，字段还是null）→ 把这个壳放进**三级缓存**（存一个能生产A的ObjectFactory）。
2. 开始给A做属性填充 → 发现A需要B → 去容器找B，发现B还没创建 → 开始创建B。
3. 实例化B（B也是个壳）→ 把B的工厂放进三级缓存。
4. 给B做属性填充 → 发现B需要A → 去容器找A：
    - 先查一级缓存（没有，A还没完成）
    - 再查二级缓存（没有）
    - 查三级缓存：找到了A的ObjectFactory，**调用它拿到A的早期引用**（如果A需要AOP代理，这一步就会提前完成代理），并把这个引用放进**二级缓存**，同时从三级缓存里删掉
5. B拿到了A的早期引用，把它注入到自己的字段里 → B的属性填充完成 → B走完初始化流程 → **B变成完整成品，放入一级缓存**。
6. 回到A的创建流程：A需要B，现在B已经是成品了，直接拿到 → 注入到A的字段里 → A的属性填充完成 → A走完初始化流程 → **A变成完整成品，放入一级缓存**，同时清掉二级、三级缓存里关于A的记录。

> 口语化：
> - 如果产生构造方法注入的循环依赖，Spring有一个三级缓存的机制，去解决循环依赖注入问题，核心就是将对象实例化和初始化分开，比如 以A 属性有B的对象， B的属性有A的对象为例：
> - A创建对象时，实例化A时，发现A是一个空壳，什么都没有，在三级缓存存一个能够创建A的OobjectFactory.
> - 然后开始做属性填充，发现A需要B，去容器找B，发现没有B，开始创建B
> - 实例化B，创建空壳，并将B的ObjectFactory放入三级缓存，开始对B做属性填充，发现B需要A，去容器找A
> - 先查一级缓存，没有，再查二级缓存，也没有，三级缓存，找到了创建A的ObjectFactory，调用它去拿到A，并把这个A的引用放入二级缓存，同时删掉三级缓存
> - B 拿到了A的早起引用，把它注入到了自己字段，完成B的属性填充，B走完初始化流程，B成了完成品，放入一级缓存。
> - 回到A的创建流程，A需要B，发现B是成品了，直接拿到，注入到自己的字段里。A的属性填充完成，A走完初始化流程，放入一级缓存，清空2级、3级关于的A的记录。

### 3.5 为什么一定要三级，而不是两级就够？

这是面试杀手题，一定要能讲清楚：

- 如果只有"原始对象"的循环依赖，理论上两级缓存（一级成品 + 二级半成品）就够用。
- 但是Spring要兼顾**AOP代理**的场景：一个Bean如果被AOP切面增强了，最终容器里应该放的是**代理对象**，而不是原始对象。
- 正常情况下，代理对象是在`postProcessAfterInitialization`（初始化完成之后）才生成的。但如果这个Bean正好出现在循环依赖中，被别人"提前引用"了，那就必须在它还没走到初始化完成那一步时，**提前生成代理对象**给别人用。
- **第三级缓存（`singletonFactories`）存的就是一个"工厂"**，它的作用是：只有当真的发生循环依赖、被提前调用的时候，才执行一次"判断要不要生成代理、要不要生成代理"的逻辑，并把结果（代理对象或原始对象）提升到二级缓存。
- 如果没有这个三级缓存做"延迟决策"，要么是所有Bean都提前生成代理（性能浪费、且破坏了Spring AOP代理只在初始化后生成一次的设计），要么是循环依赖场景下注入的是原始对象而不是代理对象（导致功能错乱，比如事务/日志增强失效）。

**一句话总结**：三级缓存的本质是**用一个"工厂"做延迟决策，保证不管有没有循环依赖，最终被注入和最终成品都是同一个对象（该是代理就是代理，该是原始对象就是原始对象），同时避免不必要的提前代理。**
> 口语化：因为要兼顾AOP代理的问题，Spring希望代理对象只在真正需要的时候才创建，所以用工厂去占位，在真正发生循环依赖的时候，再去判断是否需要代理。

---

## 自测清单（晚上闭卷口述）

- [ ] 用自己的话说一遍IOC是什么，BeanFactory和ApplicationContext的关系
- [ ] 完整背出Bean生命周期的7个阶段顺序，并指出AOP代理是在哪一步生成的
- [ ] @Autowired底层靠的是哪个组件，工作原理是什么
- [ ] 三级缓存分别叫什么、存什么
- [ ] 用A、B两个类完整口述一遍循环依赖的解决过程
- [ ] 讲清楚"为什么必须是三级缓存，两级不够用"——核心是AOP代理的延迟决策
- [ ] 能说出Spring解决不了哪种循环依赖（构造方法注入）

> 如果上面有任何一项讲不顺，回到对应小节重新过一遍，再口述一次。