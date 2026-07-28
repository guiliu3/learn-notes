
InheritableThreadLocal存在，为什么还需要TransmittableThreadLocal?
>答：InheritableThreadLocal是为了父子线程之间参数传递的，因为InheritableThreadLocal是在Thread的init方法里去复制父线程的InheritableThreadLocal的值。但是大多数线程是通过线程池创建的，因为线程池中的线程是预先创建好并复用的，任务提交时并不会创建新线程。不会再次执行“继承拷贝”逻辑


java.lang.Thread.init(java.lang.ThreadGroup, java.lang.Runnable, java.lang.String, long, java.security.AccessControlContext, boolean)
```java
if (inheritThreadLocals && parent.inheritableThreadLocals != null)
            this.inheritableThreadLocals =
                ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);

```

TransmittableThreadLocal是阿里开源的一个方案
>TransmittableThreadLocal是阿里开源的一个方案,是针对IheritableThreadLocal的加强，TransmittableThreadLocal，简称 TTL

- 使用方法：
```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>transmittable-thread-local</artifactId>
    <version>2.14.2</version>
</dependency>
```

- TransmittableThreadLocal，简称 TTL
>TTL 的核心能力，就是把提交任务线程中的上下文快照，传递给实际执行任务的线程

- 演示代码可以参考：
- com.learn.thread.InheritableThreadLocalDemo

- 如何使用TTL
```java
import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ExecutorConfig {

    @Bean("businessExecutor")
    public ExecutorService businessExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                8,
                16,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName("business-async-" + thread.getId());
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        return TtlExecutors.getTtlExecutorService(executor);
    }
}

```

    
