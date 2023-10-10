package com.chris.gotravels.payservice.controller;

import com.chris.gotravels.frameworks.convention.result.Result;
import com.chris.gotravels.frameworks.web.Results;
import com.chris.gotravels.payservice.dto.RefundReqDTO;
import com.chris.gotravels.payservice.dto.RefundRespDTO;
import com.chris.gotravels.payservice.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 退款控制层
 */
@RestController
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    /**
     * 公共退款接口
     */
    @PostMapping("/api/pay-service/common/refund")
    public Result<RefundRespDTO> commonRefund(@RequestBody RefundReqDTO requestParam) {
        return Results.success(
                refundService.commonRefund(requestParam)
        );
    }
}
