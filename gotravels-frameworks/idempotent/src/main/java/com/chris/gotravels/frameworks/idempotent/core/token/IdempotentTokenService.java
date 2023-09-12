package com.chris.gotravels.frameworks.idempotent.core.token;

import com.chris.gotravels.frameworks.idempotent.core.IdempotentExecuteHandler;

/**
 * Token 实现幂等接口
 */
public interface IdempotentTokenService extends IdempotentExecuteHandler {
    /**
     * 创建幂等验证Token
     */
    String createToken();
}
