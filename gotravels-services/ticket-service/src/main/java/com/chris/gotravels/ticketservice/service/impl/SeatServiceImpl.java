package com.chris.gotravels.ticketservice.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chris.gotravels.frameworks.cache.DistributedCache;
import com.chris.gotravels.ticketservice.common.enums.SeatStatusEnum;
import com.chris.gotravels.ticketservice.dao.entity.SeatDO;
import com.chris.gotravels.ticketservice.dao.mapper.SeatMapper;
import com.chris.gotravels.ticketservice.dto.domain.RouteDTO;
import com.chris.gotravels.ticketservice.service.SeatService;
import com.chris.gotravels.ticketservice.service.TrainStationService;
import com.chris.gotravels.ticketservice.service.handler.dto.TrainPurchaseTicketRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.chris.gotravels.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_CARRIAGE_REMAINING_TICKET;

/**
 * 座位接口层实现
 */
@Service
@RequiredArgsConstructor
public class SeatServiceImpl extends ServiceImpl<SeatMapper, SeatDO> implements SeatService {

    private final SeatMapper seatMapper;
    private final TrainStationService trainStationService;
    private final DistributedCache distributedCache;

    @Override
    public List<String> listAvailableSeat(
            String trainId,
            String carriageNumber,
            Integer seatType,
            String departure,
            String arrival) {

        LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)
                .eq(SeatDO::getTrainId, trainId)
                .eq(SeatDO::getCarriageNumber, carriageNumber)
                .eq(SeatDO::getSeatType, seatType)
                .eq(SeatDO::getStartStation, departure)
                .eq(SeatDO::getEndStation, arrival)
                .eq(SeatDO::getSeatStatus, SeatStatusEnum.AVAILABLE.getCode())
                .select(SeatDO::getSeatNumber);

        List<SeatDO> seatDOList = seatMapper.selectList(queryWrapper);

        return seatDOList.stream()
                .map(SeatDO::getSeatNumber)
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> listSeatRemainingTicket(
            String trainId,
            String departure,
            String arrival,
            List<String> trainCarriageList) {

        String keySuffix = StrUtil.join(
                "_",
                trainId,
                departure,
                arrival
        );

        if (distributedCache.hasKey(TRAIN_STATION_CARRIAGE_REMAINING_TICKET + keySuffix)) {
            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();

            List<Object> trainStationCarriageRemainingTicket = stringRedisTemplate.opsForHash().multiGet(
                    TRAIN_STATION_CARRIAGE_REMAINING_TICKET + keySuffix,
                    Arrays.asList(trainCarriageList.toArray())
            );

            if (CollUtil.isNotEmpty(trainStationCarriageRemainingTicket))
                return trainStationCarriageRemainingTicket.stream()
                            .map(each -> Integer.parseInt(each.toString()))
                            .collect(Collectors.toList());
        }

        SeatDO seatDO = SeatDO.builder()
                .trainId(Long.parseLong(trainId))
                .startStation(departure)
                .endStation(arrival)
                .build();

        return seatMapper.listSeatRemainingTicket(
                seatDO,
                trainCarriageList
        );
    }

    @Override
    public List<String> listUsableCarriageNumber(
            String trainId,
            Integer carriageType,
            String departure,
            String arrival) {

        LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)
                .eq(SeatDO::getTrainId, trainId)
                .eq(SeatDO::getSeatType, carriageType)
                .eq(SeatDO::getStartStation, departure)
                .eq(SeatDO::getEndStation, arrival)
                .eq(SeatDO::getSeatStatus, SeatStatusEnum.AVAILABLE.getCode())
                .groupBy(SeatDO::getCarriageNumber)
                .select(SeatDO::getCarriageNumber);

        List<SeatDO> seatDOList = seatMapper.selectList(queryWrapper);

        return seatDOList.stream()
                .map(SeatDO::getCarriageNumber)
                .collect(Collectors.toList());
    }

    /**
     * 扣减余票
     * <p>
     * 逻辑
     *      锁定数据库的列车座位车票状态记录，从可售状态变更为锁定状态
     *      将缓存中的座位余量进行扣减，卖出去一个自减一，卖出去两个自减二
     * <p>
     * 1. 更新列车座位车票状态
     *      1.2 通过出发站和到达站计算出都需要改变哪些站点的座位状态
     * <p>
     * 经过 lockSeat 方法就可以锁定出发站点和到达站点沿途各站对应的座位状态，从可售变更为锁定
     * */
    @Override
    public void lockSeat(
            String trainId,
            String departure,
            String arrival,
            List<TrainPurchaseTicketRespDTO> trainPurchaseTicketRespList) {

        List<RouteDTO> routeList = trainStationService.listTakeoutTrainStationRoute(
                trainId,
                departure,
                arrival
        );

        trainPurchaseTicketRespList.forEach(
                each -> routeList.forEach(

                        item -> {
                            LambdaUpdateWrapper<SeatDO> updateWrapper = Wrappers.lambdaUpdate(SeatDO.class)
                                    .eq(SeatDO::getTrainId, trainId)
                                    .eq(SeatDO::getCarriageNumber, each.getCarriageNumber())
                                    .eq(SeatDO::getStartStation, item.getStartStation())
                                    .eq(SeatDO::getEndStation, item.getEndStation())
                                    .eq(SeatDO::getSeatNumber, each.getSeatNumber());

                            SeatDO updateSeatDO = SeatDO.builder()
                                    .seatStatus(SeatStatusEnum.LOCKED.getCode())
                                    .build();

                            seatMapper.update(updateSeatDO, updateWrapper);
                        }
                )
        );
    }

    @Override
    public void unlock(
            String trainId,
            String departure,
            String arrival,
            List<TrainPurchaseTicketRespDTO> trainPurchaseTicketResults) {

        List<RouteDTO> routeList = trainStationService.listTakeoutTrainStationRoute(
                trainId,
                departure,
                arrival
        );

        trainPurchaseTicketResults.forEach(each -> routeList.forEach(item -> {
            LambdaUpdateWrapper<SeatDO> updateWrapper = Wrappers.lambdaUpdate(SeatDO.class)
                    .eq(SeatDO::getTrainId, trainId)
                    .eq(SeatDO::getCarriageNumber, each.getCarriageNumber())
                    .eq(SeatDO::getStartStation, item.getStartStation())
                    .eq(SeatDO::getEndStation, item.getEndStation())
                    .eq(SeatDO::getSeatNumber, each.getSeatNumber());

            SeatDO updateSeatDO = SeatDO.builder()
                    .seatStatus(SeatStatusEnum.AVAILABLE.getCode())
                    .build();

            seatMapper.update(updateSeatDO, updateWrapper);
        }));
    }
}
