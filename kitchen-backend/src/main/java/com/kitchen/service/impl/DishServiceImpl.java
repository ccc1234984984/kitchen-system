package com.kitchen.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kitchen.common.PageResult;
import com.kitchen.entity.Dish;
import com.kitchen.mapper.DishMapper;
import com.kitchen.service.DishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    private static final String KEY_DISHES_ALL = "cache:dishes:all";
    private static final String KEY_DISHES_CATEGORY_PREFIX = "cache:dishes:category:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<Dish> listByCategory(Long categoryId) {
        String key = KEY_DISHES_CATEGORY_PREFIX + categoryId;
        String cached = getFromCache(key);
        if (cached != null) {
            return JSON.parseObject(cached, new TypeReference<List<Dish>>() {});
        }
        List<Dish> dishes = list(new LambdaQueryWrapper<Dish>()
            .eq(Dish::getCategoryId, categoryId)
            .eq(Dish::getStatus, 1));
        putToCache(key, dishes);
        return dishes;
    }

    @Override
    public List<Dish> listAll() {
        String cached = getFromCache(KEY_DISHES_ALL);
        if (cached != null) {
            return JSON.parseObject(cached, new TypeReference<List<Dish>>() {});
        }
        List<Dish> dishes = list(new LambdaQueryWrapper<Dish>().eq(Dish::getStatus, 1));
        putToCache(KEY_DISHES_ALL, dishes);
        return dishes;
    }

    @Override
    public PageResult<Dish> listAllAdmin(int pageNum, int pageSize) {
        IPage<Dish> page = page(new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<Dish>().orderByAsc(Dish::getCategoryId));
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public boolean save(Dish dish) {
        boolean ok = super.save(dish);
        evictDishCache();
        return ok;
    }

    @Override
    public boolean updateById(Dish dish) {
        boolean ok = super.updateById(dish);
        evictDishCache();
        return ok;
    }

    @Override
    public boolean removeById(Serializable id) {
        boolean ok = super.removeById(id);
        evictDishCache();
        return ok;
    }

    private void evictDishCache() {
        try {
            stringRedisTemplate.delete(KEY_DISHES_ALL);
            Set<String> keys = stringRedisTemplate.keys(KEY_DISHES_CATEGORY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // Redis 异常不影响主流程
        }
    }

    private String getFromCache(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            return null;
        }
    }

    private void putToCache(String key, Object value) {
        try {
            stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(value), CACHE_TTL);
        } catch (Exception e) {
            // Redis 异常不影响主流程
        }
    }
}
