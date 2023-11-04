package com.chris.gotravels.frameworks.idempotent.annotation;

import com.chris.gotravels.frameworks.idempotent.enums.IdempotentSceneEnum;
import com.chris.gotravels.frameworks.idempotent.enums.IdempotentTypeEnum;

import java.lang.annotation.*;

/**
 * 幂等注解
 * <p>
 * 提供一种通用的幂等注解，该注解可用于 RestAPI 和消息队列消息防重复场景
 * <p>
 * 为解决消息队列中的重复消费问题，可以设计一套通用的消息队列幂等组件
 * 这个组件可以被各个应用程序使用，以确保它们的消费逻辑是幂等的
 *
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    /**
     * 幂等Key，只有在 {@link Idempotent#type()} 为 {@link IdempotentTypeEnum#SPEL} 时生效
     */
    String key() default "";

    /**
     * 触发幂等失败逻辑时，返回的错误提示信息
     */
    String message() default "您操作太快，请稍后再试";

    /**
     * 验证幂等类型，支持多种幂等方式
     * RestAPI 建议使用 {@link IdempotentTypeEnum#TOKEN} 或 {@link IdempotentTypeEnum#PARAM}
     * 其它类型幂等验证，使用 {@link IdempotentTypeEnum#SPEL}
     */
    IdempotentTypeEnum type() default IdempotentTypeEnum.PARAM;

    /**
     * 验证幂等场景，支持多种 {@link IdempotentSceneEnum}
     */
    IdempotentSceneEnum scene() default IdempotentSceneEnum.RESTAPI;

    /**
     * 设置防重令牌 Key 前缀，MQ 幂等去重可选设置
     * {@link IdempotentSceneEnum#MQ} and {@link IdempotentTypeEnum#SPEL} 时生效
     */
    String uniqueKeyPrefix() default "";

    /**
     * 设置防重令牌 Key 过期时间，单位秒，默认 1 小时，MQ 幂等去重可选设置
     * {@link IdempotentSceneEnum#MQ} and {@link IdempotentTypeEnum#SPEL} 时生效
     */
    long keyTimeout() default 3600L;
}
