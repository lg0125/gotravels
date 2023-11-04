package com.chris.gotravels.userservice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chris.gotravels.frameworks.common.toolkit.BeanUtil;
import com.chris.gotravels.frameworks.convention.exception.ClientException;
import com.chris.gotravels.userservice.dao.entity.UserDO;
import com.chris.gotravels.userservice.dao.entity.UserMailDO;
import com.chris.gotravels.userservice.dao.mapper.UserDeletionMapper;
import com.chris.gotravels.userservice.dao.mapper.UserMailMapper;
import com.chris.gotravels.userservice.dao.mapper.UserMapper;
import com.chris.gotravels.userservice.dto.req.UserUpdateReqDTO;
import com.chris.gotravels.userservice.dto.resp.UserQueryActualRespDTO;
import com.chris.gotravels.userservice.dto.resp.UserQueryRespDTO;
import com.chris.gotravels.userservice.dao.entity.UserDeletionDO;
import com.chris.gotravels.userservice.service.UserService;


import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.Optional;

/**
 * 用户信息接口实现层
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserDeletionMapper userDeletionMapper;
    private final UserMailMapper userMailMapper;

    /**
     * 根据用户ID查询用户
     * */
    @Override
    public UserQueryRespDTO queryUserByUserId(String userId) {
        // 等价于where语句
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getId, userId);

        UserDO userDO = userMapper.selectOne(queryWrapper);
        if (userDO == null)
            throw new ClientException("用户不存在，请检查用户ID是否正确");


        return BeanUtil.convert(
                userDO,
                UserQueryRespDTO.class
        );
    }

    /**
     * 根据用户名查询用户
     * */
    @Override
    public UserQueryRespDTO queryUserByUsername(String username) {
        // 等价于where语句
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username);

        UserDO userDO = userMapper.selectOne(queryWrapper);
        if (userDO == null)
            throw new ClientException("用户不存在，请检查用户名是否正确");

        return BeanUtil.convert(
                userDO,
                UserQueryRespDTO.class
        );
    }

    /**
     * 根据用户名查询用户(不脱敏)
     * */
    @Override
    public UserQueryActualRespDTO queryActualUserByUsername(String username) {
        return BeanUtil.convert(
                queryUserByUsername(username),
                UserQueryActualRespDTO.class
        );
    }

    /**
     * 查询用户注销数量
     * */
    @Override
    public Integer queryUserDeletionNum(Integer idType, String idCard) {
        // 等价于where语句
        LambdaQueryWrapper<UserDeletionDO> queryWrapper = Wrappers.lambdaQuery(UserDeletionDO.class)
                .eq(UserDeletionDO::getIdType, idType)
                .eq(UserDeletionDO::getIdCard, idCard);

        // TODO 此处应该先查缓存
        Long deletionCount = userDeletionMapper.selectCount(queryWrapper);

        return Optional.ofNullable(deletionCount)
                    .map(Long::intValue)
                    .orElse(0);
    }

    /**
     * 修改用户信息(用户表t_user)
     * */
    @Override
    public void update(UserUpdateReqDTO requestParam) {
        // 根据用户名查询用户
        UserQueryRespDTO userQueryRespDTO = queryUserByUsername(requestParam.getUsername());

        // UserUpdateReqDTO -> UserDO
        UserDO userDO = BeanUtil.convert(
                requestParam,
                UserDO.class
        );

        // 用户表t_user 修改
        // 等价于where语句
        LambdaUpdateWrapper<UserDO> userUpdateWrapper =
                Wrappers.lambdaUpdate(UserDO.class).eq(UserDO::getUsername, requestParam.getUsername());
        userMapper.update(userDO, userUpdateWrapper);

        if (StrUtil.isNotBlank(requestParam.getMail())
                && !Objects.equals(requestParam.getMail(), userQueryRespDTO.getMail())) {

            // 用户邮箱表t_user_mail 删除
            LambdaUpdateWrapper<UserMailDO> updateWrapper =
                    Wrappers.lambdaUpdate(UserMailDO.class).eq(UserMailDO::getMail, userQueryRespDTO.getMail());
            userMailMapper.delete(updateWrapper);

            // 用户邮箱表t_user_mail 新增
            UserMailDO userMailDO = UserMailDO.builder()
                    .mail(requestParam.getMail())
                    .username(requestParam.getUsername())
                    .build();
            userMailMapper.insert(userMailDO);
        }
    }
}
