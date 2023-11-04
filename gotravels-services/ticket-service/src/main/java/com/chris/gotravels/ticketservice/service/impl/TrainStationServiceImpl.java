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

    @Override
    public List<RouteDTO> listTakeoutTrainStationRoute(String trainId, String departure, String arrival) {
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                .eq(TrainStationDO::getTrainId, trainId)
                .select(TrainStationDO::getDeparture);

        List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(queryWrapper);

        List<String> trainStationAllList = trainStationDOList.stream()
                .map(TrainStationDO::getDeparture)
                .collect(Collectors.toList());

        return StationCalculateUtil.takeoutStation(
                    trainStationAllList,
                    departure,
                    arrival
        );
    }
}
