package com.chris.gotravels.ticketservice.service.handler.base;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.chris.gotravels.frameworks.base.ApplicationContextHolder;
import com.chris.gotravels.frameworks.cache.DistributedCache;
import com.chris.gotravels.frameworks.pattern.strategy.AbstractExecuteStrategy;
import com.chris.gotravels.ticketservice.dto.domain.RouteDTO;
import com.chris.gotravels.ticketservice.dto.domain.TrainSeatBaseDTO;
import com.chris.gotravels.ticketservice.service.TrainStationService;
import com.chris.gotravels.ticketservice.service.handler.dto.SelectSeatDTO;
import com.chris.gotravels.ticketservice.service.handler.dto.TrainPurchaseTicketRespDTO;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static com.chris.gotravels.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * 抽象高铁购票模板基础服务
 */
public abstract class AbstractTrainPurchaseTicketTemplate
        implements IPurchaseTicket,
        CommandLineRunner,
        AbstractExecuteStrategy<SelectSeatDTO, List<TrainPurchaseTicketRespDTO>> {

    private DistributedCache distributedCache;
    private String ticketAvailabilityCacheUpdateType;
    private TrainStationService trainStationService;

    /**
     * 选择座位
     *
     * @param requestParam 购票请求入参
     * @return 乘车人座位
     */
    protected abstract List<TrainPurchaseTicketRespDTO> selectSeats(SelectSeatDTO requestParam);

    protected TrainSeatBaseDTO buildTrainSeatBaseDTO(SelectSeatDTO requestParam) {
        return TrainSeatBaseDTO.builder()
                .trainId(requestParam.getRequestParam().getTrainId())
                .departure(requestParam.getRequestParam().getDeparture())
                .arrival(requestParam.getRequestParam().getArrival())
                .chooseSeatList(requestParam.getRequestParam().getChooseSeats())
                .passengerSeatDetails(requestParam.getPassengerSeatDetails())
                .build();
    }

    @Override
    public List<TrainPurchaseTicketRespDTO> executeResp(SelectSeatDTO requestParam) {
        List<TrainPurchaseTicketRespDTO> actualResult = selectSeats(requestParam);

        // 扣减车厢余票缓存，扣减站点余票缓存
        if (CollUtil.isNotEmpty(actualResult) && !StrUtil.equals(ticketAvailabilityCacheUpdateType, "binlog")) {
            String trainId = requestParam.getRequestParam().getTrainId();

            String departure = requestParam.getRequestParam().getDeparture();

            String arrival = requestParam.getRequestParam().getArrival();

            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();

            List<RouteDTO> routeDTOList = trainStationService.listTakeoutTrainStationRoute(
                    trainId, departure, arrival
            );

            routeDTOList.forEach(each -> {
                String keySuffix = StrUtil.join(
                        "_", trainId,
                        each.getStartStation(),
                        each.getEndStation()
                );

                stringRedisTemplate.opsForHash().increment(
                        TRAIN_STATION_REMAINING_TICKET + keySuffix,
                        String.valueOf(requestParam.getSeatType()),
                        -actualResult.size()
                );
            });
        }

        return actualResult;
    }

    @Override
    public void run(String... args) throws Exception {
        distributedCache = ApplicationContextHolder.getBean(DistributedCache.class);

        trainStationService = ApplicationContextHolder.getBean(TrainStationService.class);

        ConfigurableEnvironment configurableEnvironment = ApplicationContextHolder.getBean(ConfigurableEnvironment.class);

        ticketAvailabilityCacheUpdateType = configurableEnvironment.getProperty("ticket.availability.cache-update.type", "");
    }
}
