package com.kitchen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kitchen.common.PageResult;
import com.kitchen.entity.Dish;
import com.kitchen.mapper.DishMapper;
import com.kitchen.service.DishService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Override
    public List<Dish> listByCategory(Long categoryId) {
        return list(new LambdaQueryWrapper<Dish>()
            .eq(Dish::getCategoryId, categoryId)
            .eq(Dish::getStatus, 1));
    }

    @Override
    public List<Dish> listAll() {
        return list(new LambdaQueryWrapper<Dish>().eq(Dish::getStatus, 1));
    }

    @Override
    public PageResult<Dish> listAllAdmin(int pageNum, int pageSize) {
        IPage<Dish> page = page(new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<Dish>().orderByAsc(Dish::getCategoryId));
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }
}
