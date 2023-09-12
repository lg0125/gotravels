package com.chris.gotravels.frameworks.idempotent.core.spel;

import com.chris.gotravels.frameworks.cache.DistributedCache;
import com.chris.gotravels.frameworks.idempotent.annotation.Idempotent;
import com.chris.gotravels.frameworks.idempotent.core.AbstractIdempotentExecuteHandler;
import com.chris.gotravels.frameworks.idempotent.core.IdempotentAspect;
import com.chris.gotravels.frameworks.idempotent.core.IdempotentParamWrapper;
import com.chris.gotravels.frameworks.idempotent.core.RepeatConsumptionException;
import com.chris.gotravels.frameworks.idempotent.core.param.IdempotentContext;
import com.chris.gotravels.frameworks.idempotent.enums.IdempotentMQConsumeStatusEnum;
import com.chris.gotravels.frameworks.idempotent.toolkit.LogUtil;
import com.chris.gotravels.frameworks.idempotent.toolkit.SpELUtil;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public final class IdempotentSpELByMQExecuteHandler
        extends AbstractIdempotentExecuteHandler
        implements IdempotentSpELService {
    private final DistributedCache distributedCache;

    private final static int TIMEOUT = 600;
    private final static String WRAPPER = "wrapper:spEL:MQ";

    public IdempotentSpELByMQExecuteHandler(DistributedCache distributedCache) {
        this.distributedCache = distributedCache;
    }

    @SneakyThrows
    @Override
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {
        Idempotent idempotent = IdempotentAspect.getIdempotent(joinPoint);
        String key = (String) SpELUtil.parseKey(idempotent.key(), ((MethodSignature) joinPoint.getSignature()).getMethod(), joinPoint.getArgs());
        return IdempotentParamWrapper.builder().lockKey(key).joinPoint(joinPoint).build();
    }

    @Override
    public void handler(IdempotentParamWrapper wrapper) {
        String uniqueKey = wrapper.getIdempotent().uniqueKeyPrefix() + wrapper.getLockKey();
        Boolean setIfAbsent = ((StringRedisTemplate) distributedCache.getInstance())
                .opsForValue()
                .setIfAbsent(uniqueKey, IdempotentMQConsumeStatusEnum.CONSUMING.getCode(), TIMEOUT, TimeUnit.SECONDS);
        if (setIfAbsent != null && !setIfAbsent) {
            String consumeStatus = distributedCache.get(uniqueKey, String.class);
            boolean error = IdempotentMQConsumeStatusEnum.isError(consumeStatus);
            LogUtil.getLog(wrapper.getJoinPoint()).warn("[{}] MQ repeated consumption, {}.", uniqueKey, error ? "Wait for the client to delay consumption" : "Status is completed");
            throw new RepeatConsumptionException(error);
        }
        IdempotentContext.put(WRAPPER, wrapper);
    }

    @Override
    public void exceptionProcessing() {
        IdempotentParamWrapper wrapper = (IdempotentParamWrapper) IdempotentContext.getKey(WRAPPER);
        if (wrapper != null) {
            Idempotent idempotent = wrapper.getIdempotent();
            String uniqueKey = idempotent.uniqueKeyPrefix() + wrapper.getLockKey();
            try {
                distributedCache.delete(uniqueKey);
            } catch (Throwable ex) {
                LogUtil.getLog(wrapper.getJoinPoint()).error("[{}] Failed to del MQ anti-heavy token.", uniqueKey);
            }
        }
    }

    @Override
    public void postProcessing() {
        IdempotentParamWrapper wrapper = (IdempotentParamWrapper) IdempotentContext.getKey(WRAPPER);
        if (wrapper != null) {
            Idempotent idempotent = wrapper.getIdempotent();
            String uniqueKey = idempotent.uniqueKeyPrefix() + wrapper.getLockKey();
            try {
                distributedCache.put(uniqueKey, IdempotentMQConsumeStatusEnum.CONSUMED.getCode(), idempotent.keyTimeout(), TimeUnit.SECONDS);
            } catch (Throwable ex) {
                LogUtil.getLog(wrapper.getJoinPoint()).error("[{}] Failed to set MQ anti-heavy token.", uniqueKey);
            }
        }
    }
}
