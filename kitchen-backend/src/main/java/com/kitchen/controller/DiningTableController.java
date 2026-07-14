package com.kitchen.controller;

import com.kitchen.common.Result;
import com.kitchen.service.DiningTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tables")
public class DiningTableController {

    @Autowired
    private DiningTableService diningTableService;

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.success(diningTableService.getTableList());
    }

    @GetMapping("/{id}/order")
    public Result<Map<String, Object>> orderDetail(@PathVariable Long id) {
        return Result.success(diningTableService.getTableOrderDetail(id));
    }

    @PostMapping("/{id}/clear")
    public Result<Void> clear(@PathVariable Long id) {
        diningTableService.clearTable(id);
        return Result.success();
    }
}
