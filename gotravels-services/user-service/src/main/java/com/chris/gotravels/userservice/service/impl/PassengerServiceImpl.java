package com.chris.gotravels.userservice.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.chris.gotravels.frameworks.cache.DistributedCache;
import com.chris.gotravels.frameworks.common.toolkit.BeanUtil;
import com.chris.gotravels.frameworks.convention.exception.ClientException;
import com.chris.gotravels.frameworks.convention.exception.ServiceException;
import com.chris.gotravels.userservice.common.enums.VerifyStatusEnum;
import com.chris.gotravels.userservice.dao.entity.PassengerDO;
import com.chris.gotravels.userservice.dao.mapper.PassengerMapper;
import com.chris.gotravels.userservice.dto.req.PassengerRemoveReqDTO;
import com.chris.gotravels.userservice.dto.req.PassengerReqDTO;
import com.chris.gotravels.userservice.dto.resp.PassengerActualRespDTO;
import com.chris.gotravels.userservice.dto.resp.PassengerRespDTO;
import com.chris.gotravels.userservice.service.PassengerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import com.chris.gotravels.frameworks.biz.user.core.UserContext;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.chris.gotravels.userservice.common.constant.RedisKeyConstant.USER_PASSENGER_LIST;

/**
 * 乘车人接口实现层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PassengerServiceImpl implements PassengerService {

    private final PassengerMapper passengerMapper;

    private final PlatformTransactionManager transactionManager;

    private final DistributedCache distributedCache;

    /**
     * 根据用户名获得乘车人列表
     * */
    private String getActualUserPassengerListStr(String username) {
        // 通过缓存来防止请求直接打到数据库
        // 封装的缓存组件 safeGet 的作用就是
        //      如果缓存中有，那么就从缓存中返回
        //      如果缓存中没有，就查询数据库，并将查询数据库的结果，同步到缓存
        return  distributedCache.safeGet(
                USER_PASSENGER_LIST + username,

                String.class,

                () -> {
                    LambdaQueryWrapper<PassengerDO> queryWrapper = Wrappers.lambdaQuery(PassengerDO.class)
                            .eq(PassengerDO::getUsername, username);

                    List<PassengerDO> passengerDOList = passengerMapper.selectList(queryWrapper);

                    return CollUtil.isNotEmpty(passengerDOList)
                            ? JSON.toJSONString(passengerDOList)
                            : null;
                },

                1,

                TimeUnit.DAYS
        );
    }

    /**
     * 根据用户名和乘车人ID获得乘车人信息
     * */
    private PassengerDO selectPassenger(String username, String passengerId) {
        // 相当于where语句
        LambdaQueryWrapper<PassengerDO> queryWrapper = Wrappers.lambdaQuery(PassengerDO.class)
                .eq(PassengerDO::getUsername, username)
                .eq(PassengerDO::getId, passengerId);
        return passengerMapper.selectOne(queryWrapper);
    }

    /**
     * 删除用户的乘车人缓存
     * */
    private void delUserPassengerCache(String username) {
        distributedCache.delete(USER_PASSENGER_LIST + username);
    }

    /**
     * 根据用户名获取乘车人列表(脱敏)
     * */
    @Override
    public List<PassengerRespDTO> listPassengerQueryByUsername(String username) {
        String actualUserPassengerListStr = getActualUserPassengerListStr(username);

        return Optional.ofNullable(actualUserPassengerListStr)
                    .map(each -> JSON.parseArray(each, PassengerDO.class))
                    .map(each -> BeanUtil.convert(each, PassengerRespDTO.class))
                    .orElse(null);
    }


    /**
     * 根据用户名和ID列表获取乘车人列表(不脱敏)
     * */
    @Override
    public List<PassengerActualRespDTO> listPassengerQueryByIds(String username, List<Long> ids) {
        String actualUserPassengerListStr = getActualUserPassengerListStr(username);

        if (StrUtil.isEmpty(actualUserPassengerListStr))
            return null;

        return JSON.parseArray(actualUserPassengerListStr, PassengerDO.class).stream()
                .filter(passengerDO -> ids.contains(passengerDO.getId()))
                .map(each -> BeanUtil.convert(each, PassengerActualRespDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * 保存乘车人信息
     * <p>
     * 编程式事务
     * */
    @Override
    public void savePassenger(PassengerReqDTO requestParam) {
        // 获取事务
        TransactionDefinition transactionDefinition     = new DefaultTransactionDefinition();
        TransactionStatus transactionStatus             = transactionManager.getTransaction(transactionDefinition);

        String username = UserContext.getUsername();
        try {
            PassengerDO passengerDO = BeanUtil.convert(
                    requestParam,
                    PassengerDO.class
            );
            passengerDO.setUsername(username);
            passengerDO.setCreateDate(new Date());
            passengerDO.setVerifyStatus(VerifyStatusEnum.REVIEWED.getCode());

            // 乘车人表t_passenger
            int inserted = passengerMapper.insert(passengerDO);
            if (!SqlHelper.retBool(inserted))
                throw new ServiceException(String.format("[%s] 新增乘车人失败", username));

            // 提交事务
            transactionManager.commit(transactionStatus);
        } catch (Exception ex) {
            if (ex instanceof ServiceException)
                log.error(
                        "{}，请求参数：{}",
                        ex.getMessage(),
                        JSON.toJSONString(requestParam)
                );
            else
                log.error(
                        "[{}] 新增乘车人失败，请求参数：{}",
                        username,
                        JSON.toJSONString(requestParam),
                        ex
                );

            // 回滚事务
            transactionManager.rollback(transactionStatus);

            throw ex;
        }

        // 删除乘车人缓存
        delUserPassengerCache(username);
    }

    /**
     * 更新乘车人信息
     * <p>
     * 编程式事务
     * */
    @Override
    public void updatePassenger(PassengerReqDTO requestParam) {
        // 获取事务
        TransactionDefinition transactionDefinition     = new DefaultTransactionDefinition();
        TransactionStatus transactionStatus             = transactionManager.getTransaction(transactionDefinition);

        String username = UserContext.getUsername();
        try {
            PassengerDO passengerDO = BeanUtil.convert(
                    requestParam,
                    PassengerDO.class
            );

            passengerDO.setUsername(username);

            // 相当于where语句
            LambdaUpdateWrapper<PassengerDO> updateWrapper = Wrappers.lambdaUpdate(PassengerDO.class)
                    .eq(PassengerDO::getUsername, username)
                    .eq(PassengerDO::getId, requestParam.getId());

            int updated = passengerMapper.update(passengerDO, updateWrapper);
            if (!SqlHelper.retBool(updated))
                throw new ServiceException(String.format("[%s] 修改乘车人失败", username));

            // 提交事务
            transactionManager.commit(transactionStatus);
        } catch (Exception ex) {
            if (ex instanceof ServiceException)
                log.error(
                        "{}，请求参数：{}",
                        ex.getMessage(),
                        JSON.toJSONString(requestParam)
                );
            else
                log.error(
                        "[{}] 修改乘车人失败，请求参数：{}",
                        username,
                        JSON.toJSONString(requestParam),
                        ex
                );

            // 回滚事务
            transactionManager.rollback(transactionStatus);

            throw ex;
        }

        delUserPassengerCache(username);
    }

    /**
     * 删除乘车人
     * */
    @Override
    public void removePassenger(PassengerRemoveReqDTO requestParam) {
        // 获得事务
        TransactionDefinition transactionDefinition     = new DefaultTransactionDefinition();
        TransactionStatus transactionStatus             = transactionManager.getTransaction(transactionDefinition);

        String username = UserContext.getUsername();

        // 根据用户名和乘车人ID获得乘车人信息
        PassengerDO passengerDO = selectPassenger(username, requestParam.getId());
        if (Objects.isNull(passengerDO))
            throw new ClientException("乘车人数据不存在");

        try {
            LambdaUpdateWrapper<PassengerDO> deleteWrapper = Wrappers.lambdaUpdate(PassengerDO.class)
                    .eq(PassengerDO::getUsername, username)
                    .eq(PassengerDO::getId, requestParam.getId());

            // 逻辑删除，修改数据库表记录 del_flag
            int deleted = passengerMapper.delete(deleteWrapper);
            if (!SqlHelper.retBool(deleted))
                throw new ServiceException(String.format("[%s] 删除乘车人失败", username));

            // 提交事务
            transactionManager.commit(transactionStatus);
        } catch (Exception ex) {
            if (ex instanceof ServiceException)
                log.error(
                        "{}，请求参数：{}",
                        ex.getMessage(),
                        JSON.toJSONString(requestParam)
                );
            else
                log.error(
                        "[{}] 删除乘车人失败，请求参数：{}",
                        username,
                        JSON.toJSONString(requestParam),
                        ex
                );

            // 回滚事务
            transactionManager.rollback(transactionStatus);

            throw ex;
        }

        // 删除乘车人缓存
        delUserPassengerCache(username);
    }
}
