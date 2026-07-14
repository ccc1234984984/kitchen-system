package com.kitchen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kitchen.entity.DishCategory;
import com.kitchen.mapper.DishCategoryMapper;
import com.kitchen.service.DishCategoryService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DishCategoryServiceImpl extends ServiceImpl<DishCategoryMapper, DishCategory> implements DishCategoryService {
    @Override
    public List<DishCategory> listAll() {
        return list(new LambdaQueryWrapper<DishCategory>().orderByAsc(DishCategory::getSortOrder));
    }
}
