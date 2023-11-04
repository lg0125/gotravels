package com.chris.gotravels.ticketservice.service.handler.select;

import com.chris.gotravels.frameworks.biz.user.core.UserContext;
import com.chris.gotravels.frameworks.convention.exception.RemoteException;
import com.chris.gotravels.frameworks.convention.exception.ServiceException;
import com.chris.gotravels.frameworks.convention.result.Result;
import com.chris.gotravels.frameworks.pattern.strategy.AbstractStrategyChoose;
import com.chris.gotravels.ticketservice.common.enums.VehicleSeatTypeEnum;
import com.chris.gotravels.ticketservice.common.enums.VehicleTypeEnum;
import com.chris.gotravels.ticketservice.dao.entity.TrainStationPriceDO;
import com.chris.gotravels.ticketservice.dao.mapper.TrainStationPriceMapper;
import com.chris.gotravels.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import com.chris.gotravels.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.chris.gotravels.ticketservice.remote.UserRemoteService;
import com.chris.gotravels.ticketservice.remote.dto.PassengerRespDTO;
import com.chris.gotravels.ticketservice.service.SeatService;
import com.chris.gotravels.ticketservice.service.handler.dto.SelectSeatDTO;
import org.springframework.stereotype.Component;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import com.chris.gotravels.ticketservice.service.handler.dto.TrainPurchaseTicketRespDTO;

/**
 * 购票时列车座位选择器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class TrainSeatTypeSelector {

    private final SeatService seatService;
    private final UserRemoteService userRemoteService;
    private final TrainStationPriceMapper trainStationPriceMapper;
    private final AbstractStrategyChoose abstractStrategyChoose;
    private final ThreadPoolExecutor selectSeatThreadPoolExecutor;

    /**
     * 如果一个用户购买多种类型的车座，比如同一订单购买了一等座和二等座，那么就要按照不同的座位逻辑进行选座
     * 引入线程池去做 单订单多座位 购票场景
     * 首先，引入动态可监控线程池 Hippo4j，通过中间件定义的增强后的动态线程池帮助完成并行业务
     * */
    public List<TrainPurchaseTicketRespDTO> select(Integer trainType, PurchaseTicketReqDTO requestParam) {
        List<PurchaseTicketPassengerDetailDTO> passengerDetails = requestParam.getPassengers();

        // 根据用户购买的座位类型进行分组
        Map<Integer, List<PurchaseTicketPassengerDetailDTO>> seatTypeMap = passengerDetails.stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType));

        // 分配好用户相关的座位信息，容器修改为线程安全的数组集合
        List<TrainPurchaseTicketRespDTO> actualResult = new CopyOnWriteArrayList<>();

        // 如果座位类型超过单个并行分配座位
        if (seatTypeMap.size() > 1) {
            // 添加多线程执行结果类获取 Future，流程执行完成后获取
            List<Future<List<TrainPurchaseTicketRespDTO>>> futureResults = new ArrayList<>();

            // 座位分配流程，比如先执行一等座，再执行二等座...
            seatTypeMap.forEach((seatType, passengerSeatDetails) -> {
                // 线程池参数如何设置？
                Future<List<TrainPurchaseTicketRespDTO>> completableFuture = selectSeatThreadPoolExecutor.submit(
                        () -> distributeSeats(
                                trainType,
                                seatType,
                                requestParam,
                                passengerSeatDetails
                        )
                );

                futureResults.add(completableFuture);
            });

            // 并行流极端情况下有坑
            // 获取座位分配结果
            futureResults.parallelStream().forEach(completableFuture -> {
                try {
                    // 分配好座位的用户信息添加到返回集合中
                    actualResult.addAll(completableFuture.get());
                } catch (Exception e) {
                    throw new ServiceException("站点余票不足，请尝试更换座位类型或选择其它站点");
                }
            });
        } else {
            // 串行化执行座位分配流程，比如先执行一等座，再执行二等座...
            seatTypeMap.forEach((seatType, passengerSeatDetails) -> {
                // 执行策略模式分配座位
                List<TrainPurchaseTicketRespDTO> aggregationResult = distributeSeats(
                        trainType,
                        seatType,
                        requestParam,
                        passengerSeatDetails
                );

                // 分配好座位的用户信息添加到返回集合中
                actualResult.addAll(aggregationResult);
            });
        }

        // 为了避免座位分配数量出现问题，我们应该在座位全部分配完成后进行一次验证
        if (CollUtil.isEmpty(actualResult) || !Objects.equals(actualResult.size(), passengerDetails.size()))
            throw new ServiceException("站点余票不足，请尝试更换座位类型或选择其它站点");

        List<String> passengerIds = actualResult.stream()
                .map(TrainPurchaseTicketRespDTO::getPassengerId)
                .collect(Collectors.toList());

        Result<List<PassengerRespDTO>> passengerRemoteResult;
        List<PassengerRespDTO> passengerRemoteResultList;
        try {
            passengerRemoteResult = userRemoteService.listPassengerQueryByIds(
                    UserContext.getUsername(),
                    passengerIds
            );

            if (!passengerRemoteResult.isSuccess()
                    || CollUtil.isEmpty(passengerRemoteResultList = passengerRemoteResult.getData()))
                throw new RemoteException("用户服务远程调用查询乘车人相信信息错误");
        } catch (Throwable ex) {
            if (ex instanceof RemoteException)
                log.error(
                        "用户服务远程调用查询乘车人相信信息错误，当前用户：{}，请求参数：{}",
                        UserContext.getUsername(),
                        passengerIds
                );
            else
                log.error(
                        "用户服务远程调用查询乘车人相信信息错误，当前用户：{}，请求参数：{}",
                        UserContext.getUsername(),
                        passengerIds,
                        ex
                );

            throw ex;
        }

        actualResult.forEach(each -> {
            String passengerId = each.getPassengerId();

            passengerRemoteResultList.stream()
                    .filter(item -> Objects.equals(item.getId(), passengerId))
                    .findFirst()
                    .ifPresent(passenger -> {
                        each.setIdCard(passenger.getIdCard());
                        each.setPhone(passenger.getPhone());
                        each.setUserType(passenger.getDiscountType());
                        each.setIdType(passenger.getIdType());
                        each.setRealName(passenger.getRealName());
                    });

            LambdaQueryWrapper<TrainStationPriceDO> lambdaQueryWrapper = Wrappers.lambdaQuery(TrainStationPriceDO.class)
                    .eq(TrainStationPriceDO::getTrainId, requestParam.getTrainId())
                    .eq(TrainStationPriceDO::getDeparture, requestParam.getDeparture())
                    .eq(TrainStationPriceDO::getArrival, requestParam.getArrival())
                    .eq(TrainStationPriceDO::getSeatType, each.getSeatType())
                    .select(TrainStationPriceDO::getPrice);

            TrainStationPriceDO trainStationPriceDO = trainStationPriceMapper.selectOne(lambdaQueryWrapper);

            each.setAmount(trainStationPriceDO.getPrice());
        });

        // 购买列车中间站点余票如何更新？
        seatService.lockSeat(
                requestParam.getTrainId(),
                requestParam.getDeparture(),
                requestParam.getArrival(),
                actualResult
        );

        return actualResult;
    }

    private List<TrainPurchaseTicketRespDTO> distributeSeats(
            Integer trainType, Integer seatType,
            PurchaseTicketReqDTO requestParam,
            List<PurchaseTicketPassengerDetailDTO> passengerSeatDetails) {

        String buildStrategyKey = VehicleTypeEnum.findNameByCode(trainType) + VehicleSeatTypeEnum.findNameByCode(seatType);

        SelectSeatDTO selectSeatDTO = SelectSeatDTO.builder()
                .seatType(seatType)
                .passengerSeatDetails(passengerSeatDetails)
                .requestParam(requestParam)
                .build();

        try {
            return abstractStrategyChoose.chooseAndExecuteResp(buildStrategyKey, selectSeatDTO);
        } catch (ServiceException ex) {
            throw new ServiceException("当前车次列车类型暂未适配，请购买G35或G39车次");
        }
    }
}
