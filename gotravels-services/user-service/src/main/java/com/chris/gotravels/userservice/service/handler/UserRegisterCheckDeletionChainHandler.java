package com.chris.gotravels.userservice.service.handler;

import com.chris.gotravels.frameworks.convention.exception.ClientException;
import com.chris.gotravels.userservice.dto.req.UserRegisterReqDTO;
import com.chris.gotravels.userservice.service.UserService;
import com.chris.gotravels.userservice.service.filter.UserRegisterCreateChainFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 用户注册检查证件号是否多次注销
 * <p>
 * 责任链模式 3——定义责任链业务具体处理器
 * <p>
 * 验证证件号是否多次注销，如果是的话加入黑名单
 */
@Component
@RequiredArgsConstructor
public final class UserRegisterCheckDeletionChainHandler
        implements UserRegisterCreateChainFilter<UserRegisterReqDTO> {

    private final UserService userService;

    @Override
    public void handler(UserRegisterReqDTO requestParam) {
        Integer userDeletionNum = userService.queryUserDeletionNum(
                requestParam.getIdType(),
                requestParam.getIdCard()
        );

        if (userDeletionNum >= 5)
            throw new ClientException("证件号多次注销账号已被加入黑名单");
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
