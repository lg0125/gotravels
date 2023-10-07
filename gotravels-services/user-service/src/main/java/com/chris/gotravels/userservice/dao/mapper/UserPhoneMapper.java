package com.chris.gotravels.userservice.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chris.gotravels.userservice.dao.entity.UserPhoneDO;


/**
 * 用户手机号持久层
 */
public interface UserPhoneMapper extends BaseMapper<UserPhoneDO> {
    /**
     * 注销用户
     *
     * @param userPhoneDO 注销用户入参
     */
    void deletionUser(UserPhoneDO userPhoneDO);
}
