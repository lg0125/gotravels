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
import org.springframework.beans.factory.annotation.Autowired;
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

    private String getActualUserPassengerListStr(String username) {
        return  distributedCache.safeGet(
                USER_PASSENGER_LIST + username,

                String.class,

                () -> {
                    LambdaQueryWrapper<PassengerDO> queryWrapper = Wrappers.lambdaQuery(PassengerDO.class)
                            .eq(PassengerDO::getUsername, username);
                    List<PassengerDO> passengerDOList = passengerMapper.selectList(queryWrapper);
                    return CollUtil.isNotEmpty(passengerDOList) ? JSON.toJSONString(passengerDOList) : null;
                },

                1,

                TimeUnit.DAYS
        );
    }

    private PassengerDO selectPassenger(String username, String passengerId) {
        LambdaQueryWrapper<PassengerDO> queryWrapper = Wrappers.lambdaQuery(PassengerDO.class)
                .eq(PassengerDO::getUsername, username)
                .eq(PassengerDO::getId, passengerId);
        return passengerMapper.selectOne(queryWrapper);
    }

    private void delUserPassengerCache(String username) {
        distributedCache.delete(USER_PASSENGER_LIST + username);
    }

    @Override
    public List<PassengerRespDTO> listPassengerQueryByUsername(String username) {
        String actualUserPassengerListStr = getActualUserPassengerListStr(username);

        return Optional.ofNullable(actualUserPassengerListStr)
                .map(each -> JSON.parseArray(each, PassengerDO.class))
                .map(each -> BeanUtil.convert(each, PassengerRespDTO.class))
                .orElse(null);
    }



    @Override
    public List<PassengerActualRespDTO> listPassengerQueryByIds(String username, List<Long> ids) {
        String actualUserPassengerListStr = getActualUserPassengerListStr(username);

        if (StrUtil.isEmpty(actualUserPassengerListStr))
            return null;

        return JSON.parseArray(actualUserPassengerListStr, PassengerDO.class)
                .stream().filter(passengerDO -> ids.contains(passengerDO.getId()))
                .map(each -> BeanUtil.convert(each, PassengerActualRespDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public void savePassenger(PassengerReqDTO requestParam) {
        TransactionDefinition transactionDefinition = new DefaultTransactionDefinition();

        TransactionStatus transactionStatus = transactionManager.getTransaction(transactionDefinition);

        String username = UserContext.getUsername();
        try {
            PassengerDO passengerDO = BeanUtil.convert(requestParam, PassengerDO.class);
            passengerDO.setUsername(username);
            passengerDO.setCreateDate(new Date());
            passengerDO.setVerifyStatus(VerifyStatusEnum.REVIEWED.getCode());

            int inserted = passengerMapper.insert(passengerDO);
            if (!SqlHelper.retBool(inserted))
                throw new ServiceException(String.format("[%s] 新增乘车人失败", username));


            transactionManager.commit(transactionStatus);
        } catch (Exception ex) {
            if (ex instanceof ServiceException)
                log.error("{}，请求参数：{}", ex.getMessage(), JSON.toJSONString(requestParam));
            else
                log.error("[{}] 新增乘车人失败，请求参数：{}", username, JSON.toJSONString(requestParam), ex);

            transactionManager.rollback(transactionStatus);

            throw ex;
        }

        delUserPassengerCache(username);
    }

    @Override
    public void updatePassenger(PassengerReqDTO requestParam) {
        TransactionDefinition transactionDefinition = new DefaultTransactionDefinition();

        TransactionStatus transactionStatus = transactionManager.getTransaction(transactionDefinition);

        String username = UserContext.getUsername();
        try {
            PassengerDO passengerDO = BeanUtil.convert(requestParam, PassengerDO.class);
            passengerDO.setUsername(username);

            LambdaUpdateWrapper<PassengerDO> updateWrapper = Wrappers.lambdaUpdate(PassengerDO.class)
                    .eq(PassengerDO::getUsername, username)
                    .eq(PassengerDO::getId, requestParam.getId());

            int updated = passengerMapper.update(passengerDO, updateWrapper);
            if (!SqlHelper.retBool(updated))
                throw new ServiceException(String.format("[%s] 修改乘车人失败", username));

            transactionManager.commit(transactionStatus);
        } catch (Exception ex) {
            if (ex instanceof ServiceException)
                log.error("{}，请求参数：{}", ex.getMessage(), JSON.toJSONString(requestParam));
            else
                log.error("[{}] 修改乘车人失败，请求参数：{}", username, JSON.toJSONString(requestParam), ex);

            transactionManager.rollback(transactionStatus);

            throw ex;
        }

        delUserPassengerCache(username);
    }

    @Override
    public void removePassenger(PassengerRemoveReqDTO requestParam) {
        TransactionDefinition transactionDefinition = new DefaultTransactionDefinition();

        TransactionStatus transactionStatus = transactionManager.getTransaction(transactionDefinition);

        String username = UserContext.getUsername();

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

            transactionManager.commit(transactionStatus);
        } catch (Exception ex) {
            if (ex instanceof ServiceException)
                log.error("{}，请求参数：{}", ex.getMessage(), JSON.toJSONString(requestParam));
            else
                log.error("[{}] 删除乘车人失败，请求参数：{}", username, JSON.toJSONString(requestParam), ex);

            transactionManager.rollback(transactionStatus);

            throw ex;
        }

        delUserPassengerCache(username);
    }
}
