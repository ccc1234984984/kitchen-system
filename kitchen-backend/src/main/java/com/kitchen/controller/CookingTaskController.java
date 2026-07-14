package com.kitchen.controller;

import com.kitchen.common.PageResult;
import com.kitchen.common.Result;
import com.kitchen.service.CookingTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cooking-tasks")
public class CookingTaskController {

    @Autowired
    private CookingTaskService cookingTaskService;

    @PostMapping("/generate")
    public Result<Void> generate() {
        cookingTaskService.generateTasks();
        return Result.success();
    }

    @GetMapping
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "12") int pageSize) {
        return Result.success(cookingTaskService.getTaskList(pageNum, pageSize));
    }

    @PostMapping("/{id}/finish")
    public Result<Void> finish(@PathVariable Long id) {
        cookingTaskService.finishTask(id);
        return Result.success();
    }
}
