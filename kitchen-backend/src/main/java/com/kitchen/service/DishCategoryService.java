package com.kitchen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kitchen.entity.DishCategory;
import java.util.List;

public interface DishCategoryService extends IService<DishCategory> {
    List<DishCategory> listAll();
}
