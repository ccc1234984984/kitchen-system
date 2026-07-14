package com.kitchen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kitchen.entity.DiningTable;
import com.kitchen.entity.OrderItem;
import com.kitchen.entity.Orders;

import java.util.List;
import java.util.Map;

public interface DiningTableService extends IService<DiningTable> {
    List<Map<String, Object>> getTableList();
    Map<String, Object> getTableOrderDetail(Long tableId);
    void clearTable(Long tableId);
}
