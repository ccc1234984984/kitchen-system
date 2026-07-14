package com.kitchen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kitchen.common.BusinessException;
import com.kitchen.common.PageResult;
import com.kitchen.entity.*;
import com.kitchen.mapper.DiningTableMapper;
import com.kitchen.mapper.DishCategoryMapper;
import com.kitchen.mapper.DishMapper;
import com.kitchen.mapper.OrdersMapper;
import com.kitchen.mapper.OrderItemMapper;
import com.kitchen.service.CookingTaskService;
import com.kitchen.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrderService {

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishCategoryMapper categoryMapper;

    @Autowired
    private DiningTableMapper diningTableMapper;

    @Autowired
    private CookingTaskService cookingTaskService;

    private static final double TIME_WEIGHT = 1.0;
    private static final double DISH_WEIGHT = 0.5;

    @Override
    @Transactional
    public Orders createOrder(String tableNo, Long tableId, List<Long> dishIds, Long userId) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (Long dishId : dishIds) {
            Dish dish = dishMapper.selectById(dishId);
            if (dish != null) {
                totalPrice = totalPrice.add(dish.getPrice());
            }
        }

        DiningTable table = diningTableMapper.selectById(tableId);
        BigDecimal baseCharge = BigDecimal.ZERO;
        if (table != null) {
            if ("B".equals(table.getArea())) baseCharge = BigDecimal.valueOf(20);
            else if ("C".equals(table.getArea())) baseCharge = BigDecimal.valueOf(50);
        }
        totalPrice = totalPrice.add(baseCharge);

        Orders order = new Orders();
        order.setTableNo(tableNo);
        order.setTableId(tableId);
        order.setUserId(userId);
        order.setPaymentStatus(0);
        order.setStatus(0);
        order.setTotalDishes(dishIds.size());
        order.setCompletedDishes(0);
        order.setPriorityScore(BigDecimal.ZERO);
        order.setTotalPrice(totalPrice);
        order.setCreateTime(LocalDateTime.now());
        save(order);

        for (Long dishId : dishIds) {
            Dish dish = dishMapper.selectById(dishId);
            if (dish != null) {
                DishCategory category = categoryMapper.selectById(dish.getCategoryId());
                OrderItem item = new OrderItem();
                item.setOrderId(order.getId());
                item.setDishId(dishId);
                item.setDishName(dish.getName());
                item.setCategoryName(category != null ? category.getName() : "");
                item.setStatus(0);
                item.setCreateTime(LocalDateTime.now());
                orderItemMapper.insert(item);
            }
        }

        diningTableMapper.updateStatusAndOrder(tableId, 1, order.getId());

        recalculatePriority(order.getId());
        return order;
    }

    @Override
    @Transactional
    public void payOrder(Long orderId) {
        Orders order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getPaymentStatus() == 1) {
            throw new BusinessException("订单已支付");
        }
        order.setPaymentStatus(1);
        order.setPaymentTime(LocalDateTime.now());
        updateById(order);

        recalculatePriority(orderId);
        cookingTaskService.generateTasks();
    }

    @Override
    public List<Map<String, Object>> getQueue() {
        List<Orders> orders = list(new LambdaQueryWrapper<Orders>()
            .eq(Orders::getPaymentStatus, 1)
            .in(Orders::getStatus, 0, 1, 2)
            .orderByDesc(Orders::getPriorityScore)
            .orderByAsc(Orders::getCreateTime));

        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> orderIds = orders.stream().map(Orders::getId).collect(Collectors.toList());
        List<OrderItem> allItems = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));
        Map<Long, List<OrderItem>> itemsByOrderId = allItems.stream()
            .collect(Collectors.groupingBy(OrderItem::getOrderId));

        LocalDateTime now = LocalDateTime.now();
        return orders.stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("tableNo", order.getTableNo());
            map.put("status", order.getStatus());
            map.put("totalDishes", order.getTotalDishes());
            map.put("completedDishes", order.getCompletedDishes());
            map.put("priorityScore", order.getPriorityScore());

            long waitMinutes = Duration.between(order.getCreateTime(), now).toMinutes();
            map.put("waitMinutes", waitMinutes);

            map.put("items", itemsByOrderId.getOrDefault(order.getId(), Collections.emptyList()));

            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getOrderList(Long userId) {
        return getOrderPage(userId, 1, Integer.MAX_VALUE).getRecords();
    }

    @Override
    public PageResult<Map<String, Object>> getOrderPage(Long userId, int pageNum, int pageSize) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Orders> mpPage =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<Orders>()
            .orderByDesc(Orders::getCreateTime);
        if (userId != null) {
            wrapper.eq(Orders::getUserId, userId);
        }
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Orders> result = page(mpPage, wrapper);
        List<Map<String, Object>> records = result.getRecords().stream().map(order -> {
            List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("tableNo", order.getTableNo());
            map.put("status", order.getStatus());
            map.put("paymentStatus", order.getPaymentStatus());
            map.put("totalDishes", order.getTotalDishes());
            map.put("completedDishes", order.getCompletedDishes());
            map.put("totalPrice", order.getTotalPrice());
            map.put("createTime", order.getCreateTime());
            map.put("items", items);
            return map;
        }).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public Map<String, Object> getOrderDetail(Long orderId) {
        Orders order = getById(orderId);
        if (order == null) return null;
        List<OrderItem> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("tableNo", order.getTableNo());
        map.put("status", order.getStatus());
        map.put("paymentStatus", order.getPaymentStatus());
        map.put("totalDishes", order.getTotalDishes());
        map.put("completedDishes", order.getCompletedDishes());
        map.put("totalPrice", order.getTotalPrice());
        map.put("createTime", order.getCreateTime());
        map.put("items", items);
        return map;
    }

    @Override
    public void acceptOrder(Long orderId) {
        Orders order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态不正确，无法接单");
        }
        order.setStatus(1);
        updateById(order);
    }

    @Override
    public void startCooking(Long orderId) {
        Orders order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new BusinessException("订单状态不正确，无法开始制作");
        }
        order.setStatus(2);
        updateById(order);
    }

    @Override
    @Transactional
    public void finishDish(Long orderItemId) {
        OrderItem item = orderItemMapper.selectById(orderItemId);
        if (item == null) {
            throw new BusinessException("订单明细不存在");
        }
        if (item.getStatus() == 2) {
            throw new BusinessException("该菜品已完成");
        }
        item.setStatus(2);
        item.setFinishTime(LocalDateTime.now());
        orderItemMapper.updateById(item);

        baseMapper.incrementCompletedDishes(item.getOrderId());

        Orders order = getById(item.getOrderId());
        if (order != null && order.getCompletedDishes() + 1 >= order.getTotalDishes()) {
            order.setStatus(3);
            order.setCompletedDishes(order.getCompletedDishes() + 1);
            updateById(order);
        }
    }

    @Override
    @Transactional
    public void completeOrder(Long orderId) {
        Orders order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 3) {
            throw new BusinessException("订单状态不正确，无法完成");
        }
        order.setStatus(4);
        updateById(order);

        if (order.getTableId() != null) {
            diningTableMapper.updateStatusAndOrder(order.getTableId(), 2, null);
        }
    }

    @Override
    public void updatePriority(Long orderId, Double score) {
        Orders order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setPriorityScore(BigDecimal.valueOf(score));
        updateById(order);
    }

    @Override
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
    }

    private void recalculatePriority(Long orderId) {
        Orders order = getById(orderId);
        if (order == null) return;

        List<OrderItem> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));

        int categoryWeight = 0;
        Set<Long> categoryIds = items.stream()
            .map(OrderItem::getDishId)
            .collect(Collectors.toSet());

        if (!categoryIds.isEmpty()) {
            List<Dish> dishes = dishMapper.selectBatchIds(categoryIds);
            Set<Long> catIds = dishes.stream()
                .map(Dish::getCategoryId)
                .collect(Collectors.toSet());
            if (!catIds.isEmpty()) {
                List<DishCategory> categories = categoryMapper.selectBatchIds(catIds);
                categoryWeight = categories.stream()
                    .mapToInt(DishCategory::getWeight)
                    .sum();
            }
        }

        long waitMinutes = Duration.between(order.getCreateTime(), LocalDateTime.now()).toMinutes();
        double score = calculatePriority(waitMinutes, order.getTotalDishes(), categoryWeight);
        updatePriority(orderId, score);
    }

    public double calculatePriority(long waitMinutes, int totalDishes, int categoryWeight) {
        return waitMinutes * TIME_WEIGHT + totalDishes * DISH_WEIGHT + categoryWeight;
    }

    @Scheduled(fixedRate = 60000)
    public void refreshActiveOrdersPriority() {
        List<Orders> activeOrders = list(new LambdaQueryWrapper<Orders>()
            .eq(Orders::getPaymentStatus, 1)
            .in(Orders::getStatus, 0, 1, 2));
        for (Orders order : activeOrders) {
            recalculatePriority(order.getId());
        }
    }
}
