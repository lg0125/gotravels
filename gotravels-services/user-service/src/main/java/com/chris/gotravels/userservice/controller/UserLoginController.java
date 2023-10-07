package com.chris.gotravels.userservice.controller;

import com.chris.gotravels.frameworks.convention.result.Result;
import com.chris.gotravels.frameworks.web.Results;
import com.chris.gotravels.userservice.dto.req.UserLoginReqDTO;
import com.chris.gotravels.userservice.dto.resp.UserLoginRespDTO;
import com.chris.gotravels.userservice.service.UserLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


/**
 * 用户登录控制层
 */
@RestController
@RequiredArgsConstructor
public class UserLoginController {

    private final UserLoginService userLoginService;

    /**
     * 用户登录
     */
    @PostMapping("/api/user-service/v1/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam) {
        return Results.success(
                userLoginService.login(requestParam)
        );
    }

    /**
     * 通过 Token 检查用户是否登录
     */
    @GetMapping("/api/user-service/check-login")
    public Result<UserLoginRespDTO> checkLogin(@RequestParam("accessToken") String accessToken) {
        UserLoginRespDTO result = userLoginService.checkLogin(accessToken);

        return Results.success(result);
    }

    /**
     * 用户退出登录
     */
    @GetMapping("/api/user-service/logout")
    public Result<Void> logout(@RequestParam(required = false) String accessToken) {
        userLoginService.logout(accessToken);

        return Results.success();
    }
}
