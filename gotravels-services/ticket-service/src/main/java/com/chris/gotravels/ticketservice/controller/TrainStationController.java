package com.chris.gotravels.ticketservice.controller;

import com.chris.gotravels.frameworks.convention.result.Result;
import com.chris.gotravels.frameworks.web.Results;
import com.chris.gotravels.ticketservice.dto.resp.TrainStationQueryRespDTO;
import com.chris.gotravels.ticketservice.service.TrainStationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 列车站点控制层
 */
@RestController
@RequiredArgsConstructor
public class TrainStationController {

    private final TrainStationService trainStationService;

    /**
     * 根据列车 ID 查询站点信息
     */
    @GetMapping("/api/ticket-service/train-station/query")
    public Result<List<TrainStationQueryRespDTO>> listTrainStationQuery(String trainId) {
        return Results.success(
                trainStationService.listTrainStationQuery(trainId)
        );
    }
}
