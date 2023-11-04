package com.chris.gotravels.frameworks.idempotent.core;

import com.chris.gotravels.frameworks.idempotent.annotation.Idempotent;
import com.chris.gotravels.frameworks.idempotent.core.param.IdempotentContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * 幂等注解 AOP 拦截器
 * <p>
 * 使用 AOP 技术为方法增强提供通用的幂等性保证，
 * 只需要在需要保证幂等性的方法上添加 @Idempotent 注解，Aspect 就会对该方法进行增强
 * <p>
 * 不仅适用于 RestAPI 场景，还适用于消息队列的防重复消费场景
 * <p>
 * 为了提高通用性和抽象性，该组件采用了模板方法和简单工厂等设计模式，这有助于隔离复杂性和提高可扩展性
 */
@Aspect
public final class IdempotentAspect {
    /**
     * 增强方法标记 {@link Idempotent} 注解逻辑
     */
    @Around("@annotation(com.chris.gotravels.frameworks.idempotent.annotation.Idempotent)")
    public Object idempotentHandler(ProceedingJoinPoint joinPoint) throws Throwable {
        Idempotent idempotent = getIdempotent(joinPoint);

        IdempotentExecuteHandler instance = IdempotentExecuteHandlerFactory.getInstance(
                idempotent.scene(),
                idempotent.type()
        );

        Object resultObj;
        try {
            instance.execute(joinPoint, idempotent);

            resultObj = joinPoint.proceed();

            instance.postProcessing();
        } catch (RepeatConsumptionException ex) {
            /*
             * 触发幂等逻辑时可能有两种情况：
             *    1. 消息还在处理，但是不确定是否执行成功，那么需要返回错误，方便 RocketMQ 再次通过重试队列投递
             *    2. 消息处理成功了，该消息直接返回成功即可
             */
            if (!ex.getError()) return null;
            throw ex;
        } catch (Throwable ex) {
            // 客户端消费存在异常，需要删除幂等标识方便下次 RocketMQ 再次通过重试队列投递
            instance.exceptionProcessing();
            throw ex;
        } finally {
            IdempotentContext.clean();
        }

        return resultObj;
    }

    public static Idempotent getIdempotent(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();

        Method targetMethod = joinPoint.getTarget().getClass().getDeclaredMethod(
                methodSignature.getName(),
                methodSignature.getMethod().getParameterTypes()
        );

        return targetMethod.getAnnotation(Idempotent.class);
    }
}
