package com.chris.gotravels.frameworks.idempotent.core.param;

import com.chris.gotravels.frameworks.convention.exception.ClientException;
import com.chris.gotravels.frameworks.idempotent.core.AbstractIdempotentExecuteHandler;
import com.chris.gotravels.frameworks.idempotent.core.IdempotentParamWrapper;
import org.aspectj.lang.ProceedingJoinPoint;

import org.redisson.api.RedissonClient;
import org.redisson.api.RLock;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;


import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class IdempotentParamExecuteHandler extends AbstractIdempotentExecuteHandler implements IdempotentParamService {

    private final RedissonClient redissonClient;

    private final static String LOCK = "lock:param:restAPI";

    public IdempotentParamExecuteHandler(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {
        String lockKey = String.format(
                "idempotent:path:%s:currentUserId:%s:md5:%s",
                getServletPath(), getCurrentUserId(), calcArgsMD5(joinPoint)
        );

        return IdempotentParamWrapper.builder().lockKey(lockKey).joinPoint(joinPoint).build();
    }

    /**
     * @return 获取当前线程上下文 ServletPath
     */
    private String getServletPath() {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        assert sra != null;
        return sra.getRequest().getServletPath();
    }

    /**
     * @return 当前操作用户 ID
     */
    private String getCurrentUserId() {
        return null;
    }

    /**
     * @return joinPoint md5
     */
    private String calcArgsMD5(ProceedingJoinPoint joinPoint) {
        return DigestUtil.md5Hex(JSON.toJSONBytes(joinPoint.getArgs()));
    }

    @Override
    public void handler(IdempotentParamWrapper wrapper) {
        String lockKey = wrapper.getLockKey();
        RLock lock = redissonClient.getLock(lockKey);
        if (!lock.tryLock()) {
            throw new ClientException(wrapper.getIdempotent().message());
        }
        IdempotentContext.put(LOCK, lock);
    }

    @Override
    public void postProcessing() {
        RLock lock = null;
        try {
            lock = (RLock) IdempotentContext.getKey(LOCK);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

}
