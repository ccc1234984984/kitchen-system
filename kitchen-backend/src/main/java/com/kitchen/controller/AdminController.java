package com.kitchen.controller;

import com.kitchen.common.Result;
import com.kitchen.dto.LoginRequest;
import com.kitchen.dto.ChangePasswordRequest;
import com.kitchen.entity.AdminUser;
import com.kitchen.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminUserService adminUserService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Validated @RequestBody LoginRequest request, HttpSession session) {
        AdminUser user = adminUserService.login(request.getUsername(), request.getPassword());
        session.setAttribute("adminId", user.getId());
        session.setAttribute("adminName", user.getUsername());
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        return Result.success(result);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        session.invalidate();
        return Result.success();
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@Validated @RequestBody ChangePasswordRequest request, HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return Result.error(401, "未登录");
        }
        adminUserService.changePassword(adminId, request.getOldPassword(), request.getNewPassword(), request.getConfirmPassword());
        return Result.success();
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me(HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return Result.error(401, "未登录");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", adminId);
        result.put("username", session.getAttribute("adminName"));
        return Result.success(result);
    }
}
