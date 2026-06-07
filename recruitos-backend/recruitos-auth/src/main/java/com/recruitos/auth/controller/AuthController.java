package com.recruitos.auth.controller;

import com.recruitos.auth.dto.LoginDTO;
import com.recruitos.auth.dto.UserInfoVO;
import com.recruitos.auth.service.AuthService;
import com.recruitos.common.result.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

/**
 * Authentication controller
 */
@Api(tags = "Authentication")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @ApiOperation("Login")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody @Valid LoginDTO loginDTO) {
        Map<String, Object> result = authService.login(loginDTO);
        return R.ok(result);
    }

    @ApiOperation("Logout")
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    @ApiOperation("Refresh token")
    @PostMapping("/refresh")
    public R<String> refreshToken() {
        String token = authService.refreshToken();
        return R.ok(token);
    }

    @ApiOperation("Get current user info")
    @GetMapping("/me")
    public R<UserInfoVO> getCurrentUser() {
        UserInfoVO userInfo = authService.getCurrentUser();
        return R.ok(userInfo);
    }
}
