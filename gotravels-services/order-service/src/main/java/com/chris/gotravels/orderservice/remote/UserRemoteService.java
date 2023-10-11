package com.chris.gotravels.orderservice.remote;

import com.chris.gotravels.frameworks.convention.result.Result;
import com.chris.gotravels.orderservice.remote.dto.UserQueryActualRespDTO;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户远程服务调用
 */
@FeignClient(value = "gotravels-user${unique-name:}-service", url = "${aggregation.remote-url:}")
public interface UserRemoteService {
    /**
     * 根据乘车人 ID 集合查询乘车人列表
     */
    @GetMapping("/api/user-service/actual/query")
    Result<UserQueryActualRespDTO> queryActualUserByUsername(@RequestParam("username") @NotEmpty String username);
}
