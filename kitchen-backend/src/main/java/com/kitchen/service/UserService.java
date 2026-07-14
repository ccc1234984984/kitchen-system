package com.kitchen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kitchen.entity.User;

public interface UserService extends IService<User> {
    User loginOrRegister(String phone, String name);
}
