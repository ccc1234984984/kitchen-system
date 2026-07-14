package com.kitchen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kitchen.common.BusinessException;
import com.kitchen.entity.DiningTable;
import com.kitchen.entity.OrderItem;
import com.kitchen.entity.Orders;
import com.kitchen.entity.User;
import com.kitchen.mapper.DiningTableMapper;
import com.kitchen.mapper.OrderItemMapper;
import com.kitchen.mapper.OrdersMapper;
import com.kitchen.mapper.UserMapper;
import com.kitchen.service.DiningTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ZERO;

@Service
public class DiningTableServiceImpl extends ServiceImpl<DiningTableMapper, DiningTable> implements DiningTableService {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<Map<String, Object>> getTableList() {
        List<DiningTable> tables = list(new LambdaQueryWrapper<DiningTable>()
            .orderByAsc(DiningTable::getArea)
            .orderByAsc(DiningTable::getTableNo));

        if (tables.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> orderIds = tables.stream()
            .filter(t -> t.getCurrentOrderId() != null)
            .map(DiningTable::getCurrentOrderId)
            .collect(Collectors.toList());

        Map<Long, Orders> orderMap = orderIds.isEmpty() ? Collections.emptyMap() : ordersMapper.selectBatchIds(orderIds)
            .stream().collect(Collectors.toMap(Orders::getId, o -> o));

        LocalDateTime now = LocalDateTime.now();
        return tables.stream().map(table -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", table.getId());
            map.put("tableNo", table.getTableNo());
            map.put("area", table.getArea());
            map.put("type", table.getType());
            map.put("baseCharge", calcBaseCharge(table.getArea()));
            map.put("status", table.getStatus());

            Orders order = orderMap.get(table.getCurrentOrderId());
            if (order != null) {
                map.put("totalPrice", order.getTotalPrice());
                map.put("orderId", order.getId());

                long minutes = Duration.between(order.getCreateTime(), now).toMinutes();
                map.put("duration", minutes);
            }

            return map;
        }).collect(Collectors.toList());
    }

    private BigDecimal calcBaseCharge(String area) {
        if ("B".equals(area)) return BigDecimal.valueOf(20);
        if ("C".equals(area)) return BigDecimal.valueOf(50);
        return ZERO;
    }

    @Override
    public Map<String, Object> getTableOrderDetail(Long tableId) {
        DiningTable table = getById(tableId);
        if (table == null) {
            throw new BusinessException("餐桌不存在");
        }
        if (table.getCurrentOrderId() == null) {
            throw new BusinessException("该餐桌暂无订单");
        }

        Orders order = ordersMapper.selectById(table.getCurrentOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        List<OrderItem> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("tableNo", order.getTableNo());
        result.put("status", order.getStatus());
        result.put("totalPrice", order.getTotalPrice());
        result.put("baseCharge", calcBaseCharge(table.getArea()));
        result.put("totalDishes", order.getTotalDishes());
        result.put("completedDishes", order.getCompletedDishes());
        result.put("createTime", order.getCreateTime());
        result.put("items", items);

        long minutes = Duration.between(order.getCreateTime(), LocalDateTime.now()).toMinutes();
        result.put("duration", minutes);

        // 用户信息
        if (order.getUserId() != null) {
            User user = userMapper.selectById(order.getUserId());
            if (user != null) {
                result.put("userName", user.getName());
                result.put("userPhone", user.getPhone());
            }
        }

        return result;
    }

    @Override
    @Transactional
    public void clearTable(Long tableId) {
        DiningTable table = getById(tableId);
        if (table == null) {
            throw new BusinessException("餐桌不存在");
        }
        // 如果有进行中的订单，先完成订单
        if (table.getCurrentOrderId() != null) {
            Orders order = ordersMapper.selectById(table.getCurrentOrderId());
            if (order != null && order.getStatus() < 4) {
                order.setStatus(4);
                ordersMapper.updateById(order);
            }
        }
        baseMapper.clearTable(tableId, 0);
    }
}
