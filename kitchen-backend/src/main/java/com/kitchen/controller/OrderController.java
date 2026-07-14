package com.kitchen.controller;

import com.kitchen.common.PageResult;
import com.kitchen.common.Result;
import com.kitchen.dto.CreateOrderRequest;
import com.kitchen.dto.UpdatePriorityRequest;
import com.kitchen.entity.OrderItem;
import com.kitchen.entity.Orders;
import com.kitchen.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public Result<Orders> create(@Validated @RequestBody CreateOrderRequest request, HttpSession session) {
        Orders order = orderService.createOrder(request.getTableNo(), request.getTableId(), request.getDishIds(), request.getUserId());
        // 管理端下单自动支付
        if (session.getAttribute("adminId") != null) {
            orderService.payOrder(order.getId());
        }
        order = orderService.getById(order.getId());
        return Result.success(order);
    }

    @PostMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        orderService.payOrder(id);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) Long userId) {
        return Result.success(orderService.getOrderList(userId));
    }

    @GetMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(orderService.getOrderPage(userId, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Map<String, Object> detail = orderService.getOrderDetail(id);
        if (detail == null) {
            return Result.error("订单不存在");
        }
        return Result.success(detail);
    }

    @GetMapping("/queue")
    public Result<List<Map<String, Object>>> queue() {
        return Result.success(orderService.getQueue());
    }

    @PostMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id) {
        orderService.acceptOrder(id);
        return Result.success();
    }

    @PostMapping("/{id}/start")
    public Result<Void> start(@PathVariable Long id) {
        orderService.startCooking(id);
        return Result.success();
    }

    @PostMapping("/items/{itemId}/finish")
    public Result<Void> finishDish(@PathVariable Long itemId) {
        orderService.finishDish(itemId);
        return Result.success();
    }

    @PostMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        orderService.completeOrder(id);
        return Result.success();
    }

    @GetMapping("/{id}/items")
    public Result<List<OrderItem>> items(@PathVariable Long id) {
        return Result.success(orderService.getOrderItems(id));
    }

    @PostMapping("/{id}/priority")
    public Result<Void> updatePriority(@PathVariable Long id, @Validated @RequestBody UpdatePriorityRequest request) {
        orderService.updatePriority(id, request.getScore());
        return Result.success();
    }
}
