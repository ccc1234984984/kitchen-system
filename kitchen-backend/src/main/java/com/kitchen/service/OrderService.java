package com.kitchen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kitchen.common.PageResult;
import com.kitchen.entity.Orders;
import com.kitchen.entity.OrderItem;
import java.util.List;
import java.util.Map;

public interface OrderService extends IService<Orders> {
    Orders createOrder(String tableNo, Long tableId, List<Long> dishIds, Long userId);
    void payOrder(Long orderId);
    List<Map<String, Object>> getQueue();
    List<Map<String, Object>> getOrderList(Long userId);
    PageResult<Map<String, Object>> getOrderPage(Long userId, int pageNum, int pageSize);
    Map<String, Object> getOrderDetail(Long orderId);
    void acceptOrder(Long orderId);
    void startCooking(Long orderId);
    void finishDish(Long orderItemId);
    void completeOrder(Long orderId);
    void updatePriority(Long orderId, Double score);
    List<OrderItem> getOrderItems(Long orderId);
}
