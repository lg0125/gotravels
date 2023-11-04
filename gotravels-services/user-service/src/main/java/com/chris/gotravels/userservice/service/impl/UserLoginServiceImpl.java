package com.chris.gotravels.userservice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chris.gotravels.frameworks.biz.user.core.UserContext;
import com.chris.gotravels.frameworks.biz.user.core.UserInfoDTO;
import com.chris.gotravels.frameworks.biz.user.toolkit.JWTUtil;
import com.chris.gotravels.frameworks.common.toolkit.BeanUtil;
import com.chris.gotravels.frameworks.convention.exception.ClientException;
import com.chris.gotravels.frameworks.convention.exception.ServiceException;
import com.chris.gotravels.frameworks.pattern.chain.AbstractChainContext;

import com.chris.gotravels.userservice.common.enums.UserChainMarkEnum;
import com.chris.gotravels.userservice.dao.entity.*;
import com.chris.gotravels.userservice.dao.mapper.*;
import com.chris.gotravels.userservice.dto.req.UserDeletionReqDTO;
import com.chris.gotravels.userservice.dto.req.UserLoginReqDTO;
import com.chris.gotravels.userservice.dto.req.UserRegisterReqDTO;
import com.chris.gotravels.userservice.dto.resp.UserLoginRespDTO;
import com.chris.gotravels.userservice.dto.resp.UserQueryRespDTO;
import com.chris.gotravels.userservice.dto.resp.UserRegisterRespDTO;
import com.chris.gotravels.userservice.service.UserLoginService;
import com.chris.gotravels.userservice.service.UserService;

import com.chris.gotravels.frameworks.cache.DistributedCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.chris.gotravels.userservice.common.constant.RedisKeyConstant.*;
import static com.chris.gotravels.userservice.common.enums.UserRegisterErrorCodeEnum.*;
import static com.chris.gotravels.userservice.toolkit.UserReuseUtil.hashShardingIdx;

/**
 * 用户登录接口实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginServiceImpl implements UserLoginService {

    private final UserService userService;
    private final UserMapper userMapper;
    private final UserReuseMapper userReuseMapper;
    private final UserDeletionMapper userDeletionMapper;
    private final UserPhoneMapper userPhoneMapper;
    private final UserMailMapper userMailMapper;
    private final RedissonClient redissonClient;
    private final DistributedCache distributedCache;
    private final AbstractChainContext<UserRegisterReqDTO> abstractChainContext;
    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {

        // 用户名/邮箱/手机号
        String usernameOrMailOrPhone = requestParam.getUsernameOrMailOrPhone();

        boolean mailFlag = false;
        // 时间复杂度最佳 O(1) indexOf or contains 时间复杂度为 O(n)
        for (char c : usernameOrMailOrPhone.toCharArray())
            if (c == '@') {
                mailFlag = true;
                break;
            }

        String username;
        if (mailFlag) {
            // usernameOrMailOrPhone为邮箱

            // 根据邮箱查询用户名
            LambdaQueryWrapper<UserMailDO> queryWrapper = Wrappers.lambdaQuery(UserMailDO.class)
                    .eq(UserMailDO::getMail, usernameOrMailOrPhone);

            username = Optional.ofNullable(userMailMapper.selectOne(queryWrapper))
                            .map(UserMailDO::getUsername)
                            .orElseThrow(() -> new ClientException("用户名/手机号/邮箱不存在"));
        } else {
            // usernameOrMailOrPhone为手机号

            // 根据手机号查询用户名
            LambdaQueryWrapper<UserPhoneDO> queryWrapper = Wrappers.lambdaQuery(UserPhoneDO.class)
                    .eq(UserPhoneDO::getPhone, usernameOrMailOrPhone);

            username = Optional.ofNullable(userPhoneMapper.selectOne(queryWrapper))
                            .map(UserPhoneDO::getUsername)
                            .orElse(null);
        }

        // username为空，则usernameOrMailOrPhone为用户名，将其赋值给username
        username = Optional.ofNullable(username).orElse(usernameOrMailOrPhone);

        // 根据用户名username和密码password查询用户
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username)
                .eq(UserDO::getPassword, requestParam.getPassword())
                .select(UserDO::getId, UserDO::getUsername, UserDO::getRealName);
        UserDO userDO = userMapper.selectOne(queryWrapper);

        if (userDO != null) {
            // 用户信息
            UserInfoDTO userInfo = UserInfoDTO.builder()
                    .userId(String.valueOf(userDO.getId()))
                    .username(userDO.getUsername())
                    .realName(userDO.getRealName())
                    .build();

            // JWT token
            String accessToken = JWTUtil.generateAccessToken(userInfo);

            // 用户登录信息
            UserLoginRespDTO actual = new UserLoginRespDTO(
                    userInfo.getUserId(),
                    requestParam.getUsernameOrMailOrPhone(),
                    userDO.getRealName(),
                    accessToken
            );

            // 缓存用户登录信息 token
            distributedCache.put(
                    accessToken,
                    JSON.toJSONString(actual),
                    30,
                    TimeUnit.MINUTES
            );

            return actual;
        }

        throw new ServiceException("账号不存在或密码错误");
    }

    /**
     * 检查登录状态
     * <p>
     * 分布式缓存 查询token是否存在
     * */
    @Override
    public UserLoginRespDTO checkLogin(String accessToken) {
        return distributedCache.get(accessToken, UserLoginRespDTO.class);
    }

    /**
     * 用户退出登录
     * <p>
     * 如果token非空，则在分布式缓存中删除该token
     * */
    @Override
    public void logout(String accessToken) {
        if (StrUtil.isNotBlank(accessToken))
            distributedCache.delete(accessToken);
    }

    /**
     * 检查用户名是否已存在
     */
    @Override
    public Boolean hasUsername(String username) {
        // 通过布隆过滤器判断用户名是否存在
        boolean hasUsername = userRegisterCachePenetrationBloomFilter.contains(username);

        // 布隆过滤器判断存在，由于存在误判的可能
        // 进一步查询分布式缓存该用户名是否存在
        if (hasUsername) {
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();

            return instance.opsForSet().isMember(
                    USER_REGISTER_REUSE_SHARDING + hashShardingIdx(username),
                    username
            );
        }

        return true;
    }

    /**
     * 注册用户
     * <p>
     * 声明式事务
     * */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserRegisterRespDTO register(UserRegisterReqDTO requestParam) {
        // 责任链模式 验证注册用户的请求参数是否合规
        abstractChainContext.handler(
                UserChainMarkEnum.USER_REGISTER_FILTER.name(),
                requestParam
        );

        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER + requestParam.getUsername());

        boolean tryLock = lock.tryLock();
        if (!tryLock)
            throw new ServiceException(HAS_USERNAME_NOTNULL);

        try {
            try {
                // 注册用户的信息
                int inserted = userMapper.insert(
                        BeanUtil.convert(
                                requestParam,
                                UserDO.class
                        )
                );

                if (inserted < 1)
                    throw new ServiceException(USER_REGISTER_FAIL);
            } catch (DuplicateKeyException dke) {
                log.error("用户名 [{}] 重复注册", requestParam.getUsername());
                throw new ServiceException(HAS_USERNAME_NOTNULL);
            }

            UserPhoneDO userPhoneDO = UserPhoneDO.builder()
                    .phone(requestParam.getPhone())
                    .username(requestParam.getUsername())
                    .build();
            try {
                // 注册用户的手机号信息
                userPhoneMapper.insert(userPhoneDO);
            } catch (DuplicateKeyException dke) {
                log.error("用户 [{}] 注册手机号 [{}] 重复", requestParam.getUsername(), requestParam.getPhone());
                throw new ServiceException(PHONE_REGISTERED);
            }

            if (StrUtil.isNotBlank(requestParam.getMail())) {
                UserMailDO userMailDO = UserMailDO.builder()
                        .mail(requestParam.getMail())
                        .username(requestParam.getUsername())
                        .build();
                try {
                    // 注册用户的邮箱信息
                    userMailMapper.insert(userMailDO);
                } catch (DuplicateKeyException dke) {
                    log.error("用户 [{}] 注册邮箱 [{}] 重复", requestParam.getUsername(), requestParam.getMail());
                    throw new ServiceException(MAIL_REGISTERED);
                }
            }

            String username = requestParam.getUsername();

            // 删除 用户可复用数据
            userReuseMapper.delete(Wrappers.update(new UserReuseDO(username)));
            // 删除 用户可复用缓存数据
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            instance.opsForSet().remove(
                    USER_REGISTER_REUSE_SHARDING + hashShardingIdx(username),
                    username
            );

            // 增加已存在用户名布隆过滤器
            // 布隆过滤器设计问题：设置多大、碰撞率以及初始容量不够了怎么办？
            userRegisterCachePenetrationBloomFilter.add(username);
        } finally {
            lock.unlock();
        }

        return BeanUtil.convert(
                requestParam,
                UserRegisterRespDTO.class
        );
    }

    /**
     * 注销用户
     * <p>
     * 声明式事务
     * */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deletion(UserDeletionReqDTO requestParam) {
        String username = UserContext.getUsername();
        if (!Objects.equals(username, requestParam.getUsername()))
            // 此处严谨来说，需要上报风控中心进行异常检测
            throw new ClientException("注销账号与登录账号不一致");


        RLock lock = redissonClient.getLock(USER_DELETION + requestParam.getUsername());

        lock.lock();
        try {
            // 根据用户名查询用户
            UserQueryRespDTO userQueryRespDTO = userService.queryUserByUsername(username);

            // 将用户信息插入到用户注销表t_user_deletion
            UserDeletionDO userDeletionDO = UserDeletionDO.builder()
                    .idType(userQueryRespDTO.getIdType())
                    .idCard(userQueryRespDTO.getIdCard())
                    .build();
            userDeletionMapper.insert(userDeletionDO);

            // 用户表t_user 逻辑删除
            UserDO userDO = new UserDO();
            userDO.setDeletionTime(System.currentTimeMillis());
            userDO.setUsername(username);
            userMapper.deletionUser(userDO);

            // 用户手机表t_user_phone 删除
            UserPhoneDO userPhoneDO = UserPhoneDO.builder()
                    .phone(userQueryRespDTO.getPhone())
                    .deletionTime(System.currentTimeMillis())
                    .build();
            userPhoneMapper.deletionUser(userPhoneDO);

            if (StrUtil.isNotBlank(userQueryRespDTO.getMail())) {
                // 用户邮箱表t_user_mail 删除
                UserMailDO userMailDO = UserMailDO.builder()
                        .mail(userQueryRespDTO.getMail())
                        .deletionTime(System.currentTimeMillis())
                        .build();
                userMailMapper.deletionUser(userMailDO);
            }

            // 分布式缓存 删除token
            distributedCache.delete(UserContext.getToken());

            // 用户重用表 插入
            userReuseMapper.insert(new UserReuseDO(username));
            // 缓存可重用的用户名
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            instance.opsForSet().add(
                    USER_REGISTER_REUSE_SHARDING + hashShardingIdx(username),
                    username
            );
        } finally {
            lock.unlock();
        }
    }
}
