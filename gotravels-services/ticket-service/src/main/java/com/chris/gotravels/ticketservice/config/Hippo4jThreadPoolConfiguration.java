package com.chris.gotravels.ticketservice.config;

import cn.hippo4j.common.executor.support.BlockingQueueTypeEnum;
import cn.hippo4j.core.executor.DynamicThreadPool;
import cn.hippo4j.core.executor.support.ThreadPoolBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Hippo4j 动态线程池配置
 * 异步线程池框架，支持线程池动态变更&监控&报警
 * <p>
 * 使用线程池的场景：
 * 1. 异步任务处理： 当需要执行一些异步任务，但又不希望为每个任务都创建一个新线程时，可以使用线程池。例如，处理用户提交的后台任务、异步日志记录等
 * 2. 服务器端应用程序： 在服务器端开发中，通常需要同时处理多个客户端请求。线程池可以管理这些客户端请求的处理，以避免为每个请求创建新线程，从而提高性能和资源利用率
 * 3. 并发编程： 在多线程编程中，如果需要控制并发度，限制同时运行的线程数量，线程池是一个有用的工具。它可以避免创建太多线程，导致资源竞争和性能问题
 * 4. 定时任务： JDK 线程池还支持定时执行任务。可以使用 ScheduledExecutorService 接口来执行按计划运行的任务，例如定时器、定期数据备份等
 * 5. 资源管理： 在一些需要管理有限资源的情况下，线程池可以帮助控制资源的分配。例如，数据库连接池通常使用线程池来管理数据库连接
 * 6. 避免线程创建和销毁的开销： 创建和销毁线程是昂贵的操作，线程池可以重用线程，减少这些开销，提高性能
 * <p>
 * 线程池线程数是有限的，如果大量任务堆积在阻塞队列，这对于业务的执行是有比较大的延迟
 * 期望的结果是，线程池不堆积任务，如果线程池的线程满了，就让提交任务的主线程（Web容器执行线程）执行分配座位逻辑
 * 简单来说，就是线程池线程都在执行任务时，我们将并行的逻辑降级为串行
 */
@Configuration
public class Hippo4jThreadPoolConfiguration {
    /**
     * 分配一个用户购买不同类型车票的线程池
     * 线程池参数如何设置
     * <p>
     * 通过 Hippo4j 修改创建线程池的阻塞队列和拒绝策略
     * 将阻塞队列设置为 SynchronousQueue，一个没有容量的阻塞队列，没有容量自然也不会有堆积
     * 如果线程池满了会触发拒绝策略，从 AbortPolicy 修改为 CallerRunsPolicy，代表如果触发拒绝策略，则由提交任务的线程运行该任务。
     */
    @Bean
    @DynamicThreadPool
    public ThreadPoolExecutor selectSeatThreadPoolExecutor() {
        String threadPoolId = "select-seat-thread-pool-executor";

        return ThreadPoolBuilder.builder()
                    .threadPoolId(threadPoolId)
                    .threadFactory(threadPoolId)
                    .workQueue(BlockingQueueTypeEnum.SYNCHRONOUS_QUEUE)
                    .corePoolSize(24)
                    .maximumPoolSize(36)
                    .allowCoreThreadTimeOut(true)
                    .keepAliveTime(60, TimeUnit.MINUTES)
                    .rejected(new ThreadPoolExecutor.CallerRunsPolicy())
                    .dynamicPool()
                    .build();
    }
}
