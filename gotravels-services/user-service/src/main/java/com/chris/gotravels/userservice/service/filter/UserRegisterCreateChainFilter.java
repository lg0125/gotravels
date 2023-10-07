package com.chris.gotravels.userservice.service.filter;

import com.chris.gotravels.frameworks.pattern.chain.AbstractChainHandler;
import com.chris.gotravels.userservice.common.enums.UserChainMarkEnum;
import com.chris.gotravels.userservice.dto.req.UserRegisterReqDTO;

public interface UserRegisterCreateChainFilter <T extends UserRegisterReqDTO>
        extends AbstractChainHandler<UserRegisterReqDTO> {

    @Override
    default String mark() {
        return UserChainMarkEnum.USER_REGISTER_FILTER.name();
    }
}
