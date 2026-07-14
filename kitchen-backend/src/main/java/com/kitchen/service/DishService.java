package com.kitchen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kitchen.common.PageResult;
import com.kitchen.entity.Dish;
import java.util.List;

public interface DishService extends IService<Dish> {
    List<Dish> listByCategory(Long categoryId);
    List<Dish> listAll();
    PageResult<Dish> listAllAdmin(int pageNum, int pageSize);
}
