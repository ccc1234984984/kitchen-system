package com.kitchen.controller;

import com.kitchen.common.PageResult;
import com.kitchen.common.Result;
import com.kitchen.entity.Dish;
import com.kitchen.service.DishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dishes")
public class DishController {

    @Autowired
    private DishService dishService;

    @GetMapping
    public Result<List<Dish>> list(@RequestParam(required = false) Long categoryId) {
        List<Dish> dishes = categoryId != null ?
            dishService.listByCategory(categoryId) : dishService.listAll();
        return Result.success(dishes);
    }

    @GetMapping("/admin")
    public Result<PageResult<Dish>> listAdmin(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(dishService.listAllAdmin(pageNum, pageSize));
    }

    @PostMapping
    public Result<Dish> create(@RequestBody Dish dish) {
        if (dish.getStatus() == null) dish.setStatus(1);
        dish.setId(null);
        dishService.save(dish);
        return Result.success(dish);
    }

    @PutMapping("/{id}")
    public Result<Dish> update(@PathVariable Long id, @RequestBody Dish dish) {
        Dish existing = dishService.getById(id);
        if (existing == null) {
            return Result.error("菜品不存在");
        }
        dish.setId(id);
        dishService.updateById(dish);
        return Result.success(dishService.getById(id));
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        Dish existing = dishService.getById(id);
        if (existing == null) {
            return Result.error("菜品不存在");
        }
        dishService.removeById(id);
        return Result.success("删除成功");
    }

    @PutMapping("/{id}/status")
    public Result<Dish> toggleStatus(@PathVariable Long id) {
        Dish dish = dishService.getById(id);
        if (dish == null) {
            return Result.error("菜品不存在");
        }
        dish.setStatus(dish.getStatus() == 1 ? 0 : 1);
        dishService.updateById(dish);
        return Result.success(dish);
    }

    @PutMapping("/{id}/image")
    public Result<String> updateImage(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String imageUrl = body.get("imageUrl");
        Dish dish = dishService.getById(id);
        if (dish == null) {
            return Result.error("菜品不存在");
        }
        dish.setImageUrl(imageUrl);
        dishService.updateById(dish);
        return Result.success("更新成功");
    }
}
