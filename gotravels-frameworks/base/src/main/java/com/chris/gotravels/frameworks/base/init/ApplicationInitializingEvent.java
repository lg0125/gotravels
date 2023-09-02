package com.chris.gotravels.frameworks.base.init;

import org.springframework.context.ApplicationEvent;

/**
 * 应用初始化事件
 * */
public class ApplicationInitializingEvent extends ApplicationEvent {

    public ApplicationInitializingEvent(Object source) {
        super(source);
    }
}

/*
* 4.1 定义初始化事件对象类
* 规约事件，通过此事件可以查看业务系统所有初始化行为
* */

/*
* 4. 安全初始化事件
*
* 在实际应用开发中，会依赖很多应用初始化时执行任务，比如说 InitializingBean、CommandLineRunner等
* 除此之外，还有一些比如依赖 Spring 初始化的一些事件
* 1. ContextRefreshedEvent：
*       当应用程序上下文（ApplicationContext）初始化或刷新完成后触发
*       这通常发生在应用程序启动过程中，并且表示应用程序已准备好接收请求和执行业务逻辑
*       可以使用该事件来执行一些初始化操作，例如加载缓存数据、启动后台任务等
* 2. ContextStartedEvent：
*       当应用程序上下文启动时触发
*       这个事件在调用 ConfigurableApplicationContext 的 start() 方法后被发布
*       可以使用该事件来执行启动应用程序所需的特定逻辑，例如启动定时任务、启动消息监听器等
* 3. ContextStoppedEvent：
*       当应用程序上下文停止时触发
*       这个事件在调用 ConfigurableApplicationContext 的 stop() 方法后被发布
*       可以使用该事件来执行停止应用程序所需的清理逻辑，例如关闭连接、释放资源等
* 4. ContextClosedEvent：
*       当应用程序上下文关闭时触发
*       这个事件在调用 ConfigurableApplicationContext 的 close() 方法后被发布
*       可以使用该事件来执行一些最终的清理操作，例如释放数据库连接、销毁单例对象等
* 5. ServletRequestHandledEvent：
*       当 Spring MVC 处理完一个 HTTP 请求时触发
*       该事件提供了有关请求处理的详细信息，包括请求的处理时间、处理器、处理器适配器等
*       可以使用该事件来进行请求处理的监控、日志记录或统计信息收集等操作
* 6. ApplicationStartedEvent（Spring Boot）：
*       当 Spring Boot 应用程序启动时触发
*       这个事件在 SpringApplication.run() 方法完成后被发布
*       可以使用该事件来执行特定于应用程序的初始化逻辑
* 7. ApplicationReadyEvent（Spring Boot）：
*       当 Spring Boot 应用程序准备就绪时触发
*       这个事件表示应用程序已经启动完毕，并且已经可以提供服务
*       可以使用该事件来执行应用程序的后续初始化操作，例如加载数据、发送通知等
*
*
* 有些场景是依赖 Spring 容器初始化完成后调用的，ContextRefreshedEvent 这个就比较合适，但是它除了初始化调用，容器刷新也会调用
* 为了避免容器刷新造成二次调用初始化逻辑，对一些比较常用的事件简单封装了一层逻辑
* */