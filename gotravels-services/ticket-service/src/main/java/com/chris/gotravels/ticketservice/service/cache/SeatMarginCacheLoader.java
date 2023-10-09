package com.chris.gotravels.ticketservice.service.cache;

import com.chris.gotravels.frameworks.cache.DistributedCache;
import com.chris.gotravels.frameworks.cache.toolkit.CacheUtil;
import com.chris.gotravels.ticketservice.common.enums.SeatStatusEnum;
import com.chris.gotravels.ticketservice.common.enums.VehicleTypeEnum;
import com.chris.gotravels.ticketservice.dao.entity.SeatDO;
import com.chris.gotravels.ticketservice.dao.entity.TrainDO;
import com.chris.gotravels.ticketservice.dao.mapper.SeatMapper;
import com.chris.gotravels.ticketservice.dao.mapper.TrainMapper;
import com.chris.gotravels.ticketservice.dto.domain.RouteDTO;
import com.chris.gotravels.ticketservice.service.TrainStationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import static com.chris.gotravels.ticketservice.common.constant.GotravelsConstant.ADVANCE_TICKET_DAY;
import static com.chris.gotravels.ticketservice.common.constant.RedisKeyConstant.*;

/**
 * 座位余量缓存加载
 */
@Component
@RequiredArgsConstructor
public class SeatMarginCacheLoader {
    private final TrainMapper trainMapper;
    private final SeatMapper seatMapper;
    private final DistributedCache distributedCache;
    private final RedissonClient redissonClient;
    private final TrainStationService trainStationService;

    private String selectSeatMargin(String trainId, Integer type, String departure, String arrival) {
        LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)
                .eq(SeatDO::getTrainId, trainId)
                .eq(SeatDO::getSeatType, type)
                .eq(SeatDO::getSeatStatus, SeatStatusEnum.AVAILABLE.getCode())
                .eq(SeatDO::getStartStation, departure)
                .eq(SeatDO::getEndStation, arrival);

        return Optional.ofNullable(seatMapper.selectCount(queryWrapper))
                .map(String::valueOf)
                .orElse("0");
    }

    public Map<String, String> load(String trainId, String seatType, String departure, String arrival) {

        Map<String, Map<String, String>> trainStationRemainingTicketMaps = new LinkedHashMap<>();

        String keySuffix = CacheUtil.buildKey(trainId, departure, arrival);

        // 缓存带来的分布式互斥锁还有哪些优化项？详情查看：https://nageoffer.com/12306/question
        RLock lock = redissonClient.getLock(String.format(LOCK_SAFE_LOAD_SEAT_MARGIN_GET, keySuffix));
        lock.lock();
        try {
            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();

            Object quantityObj = stringRedisTemplate.opsForHash().get(
                    TRAIN_STATION_REMAINING_TICKET + keySuffix,
                    seatType
            );

            if (CacheUtil.isNullOrBlank(quantityObj)) {

                TrainDO trainDO = distributedCache.safeGet(
                        TRAIN_INFO + trainId,
                        TrainDO.class,
                        () -> trainMapper.selectById(trainId),
                        ADVANCE_TICKET_DAY,
                        TimeUnit.DAYS
                );

                List<RouteDTO> routeDTOList = trainStationService.listTrainStationRoute(
                        trainId,
                        trainDO.getStartStation(),
                        trainDO.getEndStation()
                );

                if (CollUtil.isNotEmpty(routeDTOList)) {
                    switch (trainDO.getTrainType()) {
                        // TODO 通过已有列车类型座位枚举重构
                        case 0 -> {
                            for (RouteDTO each : routeDTOList) {
                                Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();

                                trainStationRemainingTicket.put(
                                        "0",
                                        selectSeatMargin(
                                                trainId,
                                                0,
                                                each.getStartStation(),
                                                each.getEndStation()
                                        )
                                );

                                trainStationRemainingTicket.put(
                                        "1",
                                        selectSeatMargin(
                                                trainId,
                                                1,
                                                each.getStartStation(),
                                                each.getEndStation()
                                        )
                                );

                                trainStationRemainingTicket.put(
                                        "2",
                                        selectSeatMargin(
                                                trainId,
                                                2,
                                                each.getStartStation(),
                                                each.getEndStation()
                                        )
                                );

                                String actualKeySuffix = CacheUtil.buildKey(
                                        trainId,
                                        each.getStartStation(),
                                        each.getEndStation()
                                );

                                trainStationRemainingTicketMaps.put(
                                        TRAIN_STATION_REMAINING_TICKET + actualKeySuffix,
                                        trainStationRemainingTicket
                                );
                            }
                        }

                        case 1 -> {
                            for (RouteDTO each : routeDTOList) {
                                Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();

                                trainStationRemainingTicket.put(
                                        "3",
                                        selectSeatMargin(
                                                trainId,
                                                3,
                                                each.getStartStation(),
                                                each.getEndStation()
                                        )
                                );

                                trainStationRemainingTicket.put(
                                        "4",
                                        selectSeatMargin(
                                                trainId,
                                                4,
                                                each.getStartStation(),
                                                each.getEndStation()
                                        )
                                );

                                trainStationRemainingTicket.put(
                                        "5",
                                        selectSeatMargin(
                                                trainId,
                                                5,
                                                each.getStartStation(),
                                                each.getEndStation()
                                        )
                                );

                                trainStationRemainingTicket.put(
                                        "13",
                                        selectSeatMargin(
                                                trainId,
                                                13,
                                                each.getStartStation(),
                                                each.getEndStation()
                                        )
                                );

                                String actualKeySuffix = CacheUtil.buildKey(
                                        trainId,
                                        each.getStartStation(),
                                        each.getEndStation()
                                );

                                trainStationRemainingTicketMaps.put(
                                        TRAIN_STATION_REMAINING_TICKET + actualKeySuffix,
                                        trainStationRemainingTicket
                                );
                            }
                        }

                        case 2 -> {
                            for (RouteDTO each : routeDTOList) {
                                Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();

                                trainStationRemainingTicket.put(
                                        "6",
                                        selectSeatMargin(
                                                trainId,
                                                6,
                                                each.getStartStation(),
                                                each.getEndStation()
                                        )
                                );

                                trainStationRemainingTicket.put(
                                        "7",
                                        selectSeatMargin(
                                                trainId,
                                                7,
                                                each.getStartStation(),
                                                each.getEndStation()
                                        )
                                );

                                trainStationRemainingTicket.put(
                                        "8", selectSeatMargin(
                                                trainId,
                                                8,
                                                each.getStartStation(),
                                                each.getEndStation()
                                        )
                                );

                                trainStationRemainingTicket.put(
                                        "13",
                                        selectSeatMargin(
                                                trainId,
                                                13,
                                                each.getStartStation(),
                                                each.getEndStation()
                                        )
                                );

                                String actualKeySuffix = CacheUtil.buildKey(
                                        trainId,
                                        each.getStartStation(),
                                        each.getEndStation()
                                );

                                trainStationRemainingTicketMaps.put(
                                        TRAIN_STATION_REMAINING_TICKET + actualKeySuffix,
                                        trainStationRemainingTicket
                                );
                            }
                        }
                    }
                } else {
                    Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();

                    VehicleTypeEnum.findSeatTypesByCode(trainDO.getTrainType()).forEach(
                            each -> trainStationRemainingTicket.put(String.valueOf(each), "0")
                    );

                    trainStationRemainingTicketMaps.put(
                            TRAIN_STATION_REMAINING_TICKET + keySuffix,
                            trainStationRemainingTicket
                    );
                }

                // TODO LUA 脚本执行
                trainStationRemainingTicketMaps.forEach(
                        (cacheKey, cacheMap) -> stringRedisTemplate.opsForHash().putAll(cacheKey, cacheMap)
                );
            }
        } finally {
            lock.unlock();
        }

        return Optional.ofNullable(
                    trainStationRemainingTicketMaps.get(TRAIN_STATION_REMAINING_TICKET + keySuffix)
                )
                .orElse(new LinkedHashMap<>());
    }
}
