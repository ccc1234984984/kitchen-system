package com.kitchen.controller;

import com.kitchen.common.Result;
import com.kitchen.entity.DishCategory;
import com.kitchen.service.DishCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class DishCategoryController {

    @Autowired
    private DishCategoryService categoryService;

    @GetMapping
    public Result<List<DishCategory>> list() {
        return Result.success(categoryService.listAll());
    }
}
