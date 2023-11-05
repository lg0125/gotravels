package com.chris.gotravels.ticketservice.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chris.gotravels.frameworks.cache.DistributedCache;
import com.chris.gotravels.frameworks.common.toolkit.EnvUtil;
import com.chris.gotravels.ticketservice.dao.entity.RegionDO;
import com.chris.gotravels.ticketservice.dao.entity.TrainStationRelationDO;
import com.chris.gotravels.ticketservice.dao.mapper.RegionMapper;
import com.chris.gotravels.ticketservice.dao.mapper.TrainStationRelationMapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.chris.gotravels.ticketservice.common.constant.GotravelsConstant.ADVANCE_TICKET_DAY;
import static com.chris.gotravels.ticketservice.common.constant.RedisKeyConstant.REGION_TRAIN_STATION;

/**
 * 地区站点查询定时任务
 */
@RestController
@RequiredArgsConstructor
public class RegionTrainStationJobHandler extends IJobHandler {

    private final RegionMapper regionMapper;
    private final TrainStationRelationMapper trainStationRelationMapper;
    private final DistributedCache distributedCache;

    @XxlJob(value = "regionTrainStationJobHandler")
    @GetMapping("/api/ticket-service/region-train-station/job/cache-init/execute")
    @Override
    public void execute() {
        List<String> regionList =
                regionMapper.selectList(Wrappers.emptyWrapper())
                        .stream()
                        .map(RegionDO::getName)
                        .toList();

        String requestParam = getJobRequestParam();

        var dateTime = StrUtil.isNotBlank(requestParam)
                ? requestParam
                : DateUtil.tomorrow().toDateStr();

        for (int i = 0; i < regionList.size(); i++)
            for (int j = 0; j < regionList.size(); j++)
                if (i != j) {
                    String startRegion  = regionList.get(i);
                    String endRegion    = regionList.get(j);

                    LambdaQueryWrapper<TrainStationRelationDO> relationQueryWrapper =
                            Wrappers.lambdaQuery(TrainStationRelationDO.class)
                                .eq(TrainStationRelationDO::getStartRegion, startRegion)
                                .eq(TrainStationRelationDO::getEndRegion, endRegion);

                    List<TrainStationRelationDO> trainStationRelationDOList =
                            trainStationRelationMapper.selectList(relationQueryWrapper);

                    if (CollUtil.isEmpty(trainStationRelationDOList))
                        continue;

                    Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
                    for (var item : trainStationRelationDOList) {

                        String zSetKey = StrUtil.join(
                                "_",
                                item.getTrainId(),
                                item.getDeparture(),
                                item.getArrival()
                        );

                        ZSetOperations.TypedTuple<String> tuple = ZSetOperations.TypedTuple.of(
                                zSetKey,
                                (double) item.getDepartureTime().getTime()
                        );

                        tuples.add(tuple);
                    }

                    StringRedisTemplate stringRedisTemplate =
                            (StringRedisTemplate) distributedCache.getInstance();

                    String buildCacheKey = REGION_TRAIN_STATION + StrUtil.join(
                            "_",
                            startRegion,
                            endRegion,
                            dateTime
                    );

                    stringRedisTemplate.opsForZSet().add(
                            buildCacheKey,
                            tuples
                    );

                    stringRedisTemplate.expire(
                            buildCacheKey,
                            ADVANCE_TICKET_DAY,
                            TimeUnit.DAYS
                    );
                }
    }

    private String getJobRequestParam() {
        return EnvUtil.isDevEnvironment()
                    ? ((ServletRequestAttributes) Objects.requireNonNull(
                            RequestContextHolder.getRequestAttributes())
                    ).getRequest().getHeader("requestParam")
                    : XxlJobHelper.getJobParam();
    }
}
