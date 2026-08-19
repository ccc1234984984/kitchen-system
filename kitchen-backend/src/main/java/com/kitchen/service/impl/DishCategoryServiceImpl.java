package com.kitchen.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kitchen.entity.DishCategory;
import com.kitchen.mapper.DishCategoryMapper;
import com.kitchen.service.DishCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;

@Service
public class DishCategoryServiceImpl extends ServiceImpl<DishCategoryMapper, DishCategory> implements DishCategoryService {

    private static final String KEY_CATEGORIES_ALL = "cache:categories:all";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<DishCategory> listAll() {
        String cached = getFromCache(KEY_CATEGORIES_ALL);
        if (cached != null) {
            return JSON.parseObject(cached, new TypeReference<List<DishCategory>>() {});
        }
        List<DishCategory> categories = list(new LambdaQueryWrapper<DishCategory>().orderByAsc(DishCategory::getSortOrder));
        putToCache(KEY_CATEGORIES_ALL, categories);
        return categories;
    }

    @Override
    public boolean save(DishCategory category) {
        boolean ok = super.save(category);
        evictCategoryCache();
        return ok;
    }

    @Override
    public boolean updateById(DishCategory category) {
        boolean ok = super.updateById(category);
        evictCategoryCache();
        return ok;
    }

    @Override
    public boolean removeById(Serializable id) {
        boolean ok = super.removeById(id);
        evictCategoryCache();
        return ok;
    }

    private void evictCategoryCache() {
        try {
            stringRedisTemplate.delete(KEY_CATEGORIES_ALL);
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
