package com.kitchen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kitchen.entity.User;
import com.kitchen.mapper.UserMapper;
import com.kitchen.service.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User loginOrRegister(String phone, String name) {
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            user.setName(name != null && !name.isEmpty() ? name : "顾客" + phone);
            user.setCreateTime(LocalDateTime.now());
            save(user);
        } else if (name != null && !name.isEmpty() && !name.equals(user.getName())) {
            user.setName(name);
            updateById(user);
        }
        return user;
    }
}
