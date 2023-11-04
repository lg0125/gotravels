package com.chris.gotravels.userservice.service.filter;

import com.chris.gotravels.frameworks.pattern.chain.AbstractChainHandler;
import com.chris.gotravels.userservice.common.enums.UserChainMarkEnum;
import com.chris.gotravels.userservice.dto.req.UserRegisterReqDTO;

/**
 * 用户注册责任链过滤器
 * <p>
 * 责任链模式 2——定义业务责任链接口
 */
public interface UserRegisterCreateChainFilter <T extends UserRegisterReqDTO>
        extends AbstractChainHandler<UserRegisterReqDTO> {

    @Override
    default String mark() {
        return UserChainMarkEnum.USER_REGISTER_FILTER.name();
    }
}
