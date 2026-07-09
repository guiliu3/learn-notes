## 一、Spring MVC 请求流程
  SpringMVC 是 一个WebMVC的框架，能够进行请求分发，参数绑定，方法调用，返回值处理。

### 1.1 Spring MVC 核心组件
- DispatcherServlet：DispatcherServlet是SpringMVC的前端控制器，它负责将前端请求转发到合适的HandlerAdapter去执行真正的controller方法。
- HandlerMapping:负责根据请求路径，请求方法类型，找到对应的Hanlder.HanlderMapping返回的不是单独的controller方法，而是一个HandlerExecutionChain，包含了-Handler  - Interceptor拦截器链。
- HandlerAdapter：
- HandlerInterceptor：拦截器用于在controller请求方法执行后做一些增强处理。
- ViewResolver: 视图解析器
- HttpMessageConverter：负责HTTP 请求体和Java对象之间的转换。
- HandlerMethodArgumentResolver:参数解析器被HTTP请求中的参数转换成Controller参数的方法
- HandlerMethodReturnValueHandler:返回值处理器负责处理controller方法的返回值

### 1.2 一次 HTTP 请求的完整流程
  以下面的接口请求为例：
```java
  @RestController
  @RequestMapping("/users")
  public class UserController {

      @GetMapping("/{id}")
      public UserDTO getUser(@PathVariable Long id) {
          return userService.getUser(id);
      }
  }
```
1. 浏览器发送HTTP请求，请求首先进入Servlet容器，比如Tomcat
2. Tomcat根据映射规则，把请求交给DispatcherServlet
3. DispatcherServlet调用HandlerMapping，根据请求路径/users/100和请求Get方法，查找对应的Handler
4. HandllerMapping找到UserController#getUser方法，并返回HandlerExecutionChain,其中包含目标Hanlder和匹配到的拦截器链。
5. DispatcherServlet执行拦截器的preHanlder方法。
6. DispatcherServlet根据Handler类找到合适的Handler Adapter。
7. HandlerAdapter 调用 Controller 方法
8. 参数解析器解析请求参数
9. Controller 执行业务逻辑
10. 返回值处理器处理返回结果
11. HttpMessageConverter 转 JSON
12. 执行拦截器 postHandle / afterCompletion
13. Tomcat把Http响应客户端。


### 1.3 、过滤器、拦截器、AOP 的区别

####  Filter

Filter 是 Servlet 规范里的组件，作用在 Spring MVC 之前。

特点：
- 由 Servlet 容器管理
- 拦截的是 Servlet 请求
- 执行时机早于 DispatcherServlet
- 适合做编码处理、跨域、认证、请求包装等底层 Web 处理

#### Interceptor

Interceptor 是 Spring MVC 提供的组件，作用在 DispatcherServlet 之后、Controller 之前后。

特点：
- 由 Spring 容器管理
- 能拿到 Handler 信息
- 更适合做登录校验、权限校验、接口日志、接口耗时统计

####  AOP

AOP 是 Spring 的通用方法增强机制，作用在 Bean 方法调用层面。

特点：
- 不是只针对 Web 请求
- 可以拦截 Service、Repository 等 Spring Bean 方法
- 适合做事务、日志、权限、监控等横切逻辑


### 1.3 常见面试题
**Q：Spring MVC 一次请求的执行流程？**
> 答：请求先进入Tomcat,然后根据Servlet映射到DispatcherServlet，DispatcherServlet通过handlerMapping找到对应的Handler和拦截器链，然后执行拦截器的preHand，接着HandlerAdapter调用controller方法，调用前由参数解析器完成参数绑定，controller执行业务逻辑后返回结果，返回值处理器根据返回类型处理结果，最后执行拦截器的postHandler和afterCompletion，响应客户端。

**Q：DispatcherServlet 的作用是什么？**
> 答：SpringMVC的前端控制器，它负责将前端请求转发到合适的HandlerAdapter去执行真正的controller方法。

**Q：HandlerMapping 和 HandlerAdapter 有什么区别？**
> 答：HandlerMapping负责根据请求找到具体谁处理，找到对应的controller方法和拦截器链。HandlerAdapter负责真正调用这个Handler。

**Q：@RequestBody 和 @ResponseBody 底层靠什么实现？**
> 答：@RequestBody由参数解析器识别，底层通过HttpMessageConverter把请求体中的JSON转换成对象实体，@ResponBody由返回值处理器是被，底层同样是HttpMessageConverter把java对象转换成JSON响应。

**Q：拦截器和过滤器有什么区别？**
> 答：Filter 属于 Servlet 规范，由 Servlet 容器管理，执行在 DispatcherServlet 之前，拦截的是原始 HTTP 请求。Interceptor 属于 Spring MVC，由 Spring 容器管理，执行在 DispatcherServlet 之后、Controller 前后，能拿到 Handler 信息，更适合做登录
校验、权限校验和接口日志。
  ---