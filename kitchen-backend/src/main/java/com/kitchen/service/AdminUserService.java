package com.kitchen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kitchen.entity.AdminUser;

public interface AdminUserService extends IService<AdminUser> {
    AdminUser login(String username, String password);
    void changePassword(Long userId, String oldPassword, String newPassword, String confirmPassword);
}
