package com.chris.gotravels.ticketservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chris.gotravels.frameworks.common.toolkit.BeanUtil;
import com.chris.gotravels.ticketservice.dao.entity.TrainStationDO;
import com.chris.gotravels.ticketservice.dao.mapper.TrainStationMapper;
import com.chris.gotravels.ticketservice.dto.domain.RouteDTO;
import com.chris.gotravels.ticketservice.dto.resp.TrainStationQueryRespDTO;
import com.chris.gotravels.ticketservice.service.TrainStationService;
import com.chris.gotravels.ticketservice.toolkit.StationCalculateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainStationServiceImpl implements TrainStationService {

    private final TrainStationMapper trainStationMapper;

    @Override
    public List<TrainStationQueryRespDTO> listTrainStationQuery(String trainId) {
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                .eq(TrainStationDO::getTrainId, trainId);

        List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(queryWrapper);

        return BeanUtil.convert(
                trainStationDOList,
                TrainStationQueryRespDTO.class
        );
    }

    @Override
    public List<RouteDTO> listTrainStationRoute(String trainId, String departure, String arrival) {
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                .eq(TrainStationDO::getTrainId, trainId)
                .select(TrainStationDO::getDeparture);

        List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(queryWrapper);

        List<String> trainStationAllList = trainStationDOList.stream()
                .map(TrainStationDO::getDeparture)
                .collect(Collectors.toList());

        return StationCalculateUtil.throughStation(
                    trainStationAllList,
                    departure,
                    arrival
        );
    }

    /**
     * 扣减余票
     * <p>
     * 逻辑
     *      锁定数据库的列车座位车票状态记录，从可售状态变更为锁定状态
     *      将缓存中的座位余量进行扣减，卖出去一个自减一，卖出去两个自减二
     * <p>
     * 1. 更新列车座位车票状态
     *      1.1 获得列车 ID、出发站、到达站以及乘车人和对应的座位信息
     * */
    @Override
    public List<RouteDTO> listTakeoutTrainStationRoute(String trainId, String departure, String arrival) {
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                .eq(TrainStationDO::getTrainId, trainId)
                .select(TrainStationDO::getDeparture);

        // 获取列车所有站点，通过所有站点计算需要锁定座位的站点集合
        List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(queryWrapper);

        List<String> trainStationAllList = trainStationDOList.stream()
                .map(TrainStationDO::getDeparture)
                .collect(Collectors.toList());

        // 根据工具类计算需要扣减沿途关联的站点
        return StationCalculateUtil.takeoutStation(
                    trainStationAllList,
                    departure,
                    arrival
        );
    }
}
