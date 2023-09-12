package com.chris.gotravels.frameworks.idempotent.annotation;

import com.chris.gotravels.frameworks.idempotent.enums.IdempotentSceneEnum;
import com.chris.gotravels.frameworks.idempotent.enums.IdempotentTypeEnum;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;

/**
 * MQ 业务场景幂等注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Idempotent(scene = IdempotentSceneEnum.MQ)
public @interface MQIdempotent {
    /**
     * {@link Idempotent#key} 的别名
     */
    @AliasFor(annotation = Idempotent.class, attribute = "key")
    String key() default "";

    /**
     * {@link Idempotent#type} 的别名
     */
    @AliasFor(annotation = Idempotent.class, attribute = "type")
    IdempotentTypeEnum type() default IdempotentTypeEnum.SPEL;
}
