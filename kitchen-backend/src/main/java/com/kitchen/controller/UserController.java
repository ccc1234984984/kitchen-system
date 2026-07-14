package com.kitchen.controller;

import com.kitchen.common.Result;
import com.kitchen.entity.User;
import com.kitchen.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<User> login(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || !phone.matches("\\d{11}")) {
            return Result.error("请输入有效的11位手机号");
        }
        String name = body.get("name");
        User user = userService.loginOrRegister(phone, name);
        return Result.success(user);
    }

    @PutMapping("/name")
    public Result<User> updateName(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String name = (String) body.get("name");
        if (name == null || name.trim().isEmpty()) {
            return Result.error("姓名不能为空");
        }
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setName(name.trim());
        userService.updateById(user);
        return Result.success(user);
    }
}
