package com.chris.gotravels.userservice.service.handler;

import com.chris.gotravels.frameworks.convention.exception.ClientException;
import com.chris.gotravels.userservice.common.enums.UserRegisterErrorCodeEnum;
import com.chris.gotravels.userservice.dto.req.UserRegisterReqDTO;
import com.chris.gotravels.userservice.service.filter.UserRegisterCreateChainFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.chris.gotravels.userservice.service.UserLoginService;

/**
 * 用户注册用户名唯一检验
 * <p>
 * 责任链模式 3——定义责任链业务具体处理器
 * <p>
 * 验证用户名是否可用
 */
@Component
@RequiredArgsConstructor
public final class UserRegisterHasUsernameChainHandler
        implements UserRegisterCreateChainFilter<UserRegisterReqDTO> {

    private final UserLoginService userLoginService;

    @Override
    public void handler(UserRegisterReqDTO requestParam) {
        if (!userLoginService.hasUsername(requestParam.getUsername()))
            throw new ClientException(
                    UserRegisterErrorCodeEnum.HAS_USERNAME_NOTNULL
            );
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
