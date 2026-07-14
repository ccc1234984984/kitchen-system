package com.kitchen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kitchen.common.PageResult;
import com.kitchen.entity.*;
import com.kitchen.mapper.*;
import com.kitchen.service.CookingTaskService;
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
public class CookingTaskServiceImpl extends ServiceImpl<CookingTaskMapper, CookingTask> implements CookingTaskService {

    @Autowired
    private TaskOrderItemMapper taskOrderItemMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishCategoryMapper categoryMapper;

    private static final int TIME_WINDOW_MINUTES = 15;
    private static final double TIME_WEIGHT = 1.0;
    private static final double CATEGORY_WEIGHT = 0.5;
    private static final double PREP_TIME_WEIGHT = 0.3;

    @Override
    @Transactional
    public void generateTasks() {
        // 只处理已支付的订单
        List<Orders> paidOrders = ordersMapper.selectList(
            new LambdaQueryWrapper<Orders>().eq(Orders::getPaymentStatus, 1));
        if (paidOrders.isEmpty()) return;
        Set<Long> paidOrderIds = paidOrders.stream().map(Orders::getId).collect(Collectors.toSet());

        // 获取所有待制作的订单明细（状态0），仅限已支付订单
        List<OrderItem> pendingItems = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getStatus, 0)
                .in(OrderItem::getOrderId, paidOrderIds));

        if (pendingItems.isEmpty()) {
            return;
        }

        // 排除已在制作中任务(状态1)里的明细
        List<CookingTask> cookingTasks = list(new LambdaQueryWrapper<CookingTask>()
            .eq(CookingTask::getStatus, 1));
        Set<Long> cookingItemIds;
        if (!cookingTasks.isEmpty()) {
            List<Long> cookingTaskIds = cookingTasks.stream()
                .map(CookingTask::getId).collect(Collectors.toList());
            List<TaskOrderItem> cookingItems = taskOrderItemMapper.selectList(
                new LambdaQueryWrapper<TaskOrderItem>().in(TaskOrderItem::getTaskId, cookingTaskIds));
            cookingItemIds = cookingItems.stream()
                .map(TaskOrderItem::getOrderItemId).collect(Collectors.toSet());
        } else {
            cookingItemIds = Collections.emptySet();
        }

        List<OrderItem> newItems = pendingItems.stream()
            .filter(item -> !cookingItemIds.contains(item.getId()))
            .collect(Collectors.toList());

        if (newItems.isEmpty()) {
            return;
        }

        // 删除所有待制作(状态0)的任务及其关联，重新生成
        List<CookingTask> pendingTasks = list(new LambdaQueryWrapper<CookingTask>()
            .eq(CookingTask::getStatus, 0));
        if (!pendingTasks.isEmpty()) {
            List<Long> pendingTaskIds = pendingTasks.stream()
                .map(CookingTask::getId).collect(Collectors.toList());
            taskOrderItemMapper.delete(new LambdaQueryWrapper<TaskOrderItem>()
                .in(TaskOrderItem::getTaskId, pendingTaskIds));
            removeByIds(pendingTaskIds);
        }

        LocalDateTime now = LocalDateTime.now();

        // 按菜品分组
        Map<Long, List<OrderItem>> itemsByDish = newItems.stream()
            .collect(Collectors.groupingBy(OrderItem::getDishId));

        // 为每个菜品处理
        for (Map.Entry<Long, List<OrderItem>> entry : itemsByDish.entrySet()) {
            Long dishId = entry.getKey();
            List<OrderItem> items = entry.getValue();

            // 计算每个订单明细的等待时间并排序
            List<OrderItemWithWait> itemsWithWait = items.stream()
                .map(item -> {
                    Orders order = ordersMapper.selectById(item.getOrderId());
                    long waitMinutes = Duration.between(order.getCreateTime(), now).toMinutes();
                    return new OrderItemWithWait(item, waitMinutes);
                })
                .sorted(Comparator.comparingLong(OrderItemWithWait::getWaitMinutes))
                .collect(Collectors.toList());

            // 按订单分组（同一订单的相同菜品不能拆分）
            Map<Long, List<OrderItemWithWait>> byOrder = itemsWithWait.stream()
                .collect(Collectors.groupingBy(w -> w.getItem().getOrderId(), LinkedHashMap::new, Collectors.toList()));

            // 按时间窗口(15min) + 最大数量(10) 分组
            List<List<OrderItemWithWait>> groups = new ArrayList<>();

            for (List<OrderItemWithWait> orderGroup : byOrder.values()) {
                int orderQty = orderGroup.size();
                long orderWait = orderGroup.get(0).getWaitMinutes();

                boolean added = false;
                for (List<OrderItemWithWait> taskGroup : groups) {
                    long firstWait = taskGroup.get(0).getWaitMinutes();
                    int groupQty = taskGroup.size();
                    if (orderWait - firstWait <= TIME_WINDOW_MINUTES && groupQty + orderQty <= 10) {
                        taskGroup.addAll(orderGroup);
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    List<OrderItemWithWait> newGroup = new ArrayList<>(orderGroup);
                    groups.add(newGroup);
                }
            }

            // 为每个分组创建任务
            for (List<OrderItemWithWait> group : groups) {
                Dish dish = dishMapper.selectById(dishId);
                DishCategory category = categoryMapper.selectById(dish.getCategoryId());

                CookingTask task = new CookingTask();
                task.setDishId(dishId);
                task.setDishName(dish.getName());
                task.setQuantity(group.size());
                task.setStatus(0);
                task.setCreateTime(now);

                // 计算平均等待时间
                long avgWait = group.stream()
                    .mapToLong(OrderItemWithWait::getWaitMinutes)
                    .sum() / group.size();
                task.setAvgWaitMinutes(avgWait);

                // 计算优先级分数（等待时间越长优先级越高，制作时间越短优先级越高）
                double priorityScore = avgWait * TIME_WEIGHT + 
                    (category != null ? category.getWeight() : 0) * CATEGORY_WEIGHT -
                    dish.getEstimatedTime() * PREP_TIME_WEIGHT;
                task.setPriorityScore(BigDecimal.valueOf(priorityScore));

                save(task);

                // 创建关联记录
                for (OrderItemWithWait itemWithWait : group) {
                    TaskOrderItem taskOrderItem = new TaskOrderItem();
                    taskOrderItem.setTaskId(task.getId());
                    taskOrderItem.setOrderId(itemWithWait.getItem().getOrderId());
                    taskOrderItem.setOrderItemId(itemWithWait.getItem().getId());
                    Orders order = ordersMapper.selectById(itemWithWait.getItem().getOrderId());
                    taskOrderItem.setTableNo(order.getTableNo());
                    taskOrderItem.setWaitMinutes(itemWithWait.getWaitMinutes());
                    taskOrderItemMapper.insert(taskOrderItem);
                }
            }
        }
    }

    @Override
    public PageResult<Map<String, Object>> getTaskList(int pageNum, int pageSize) {
        List<CookingTask> tasks = list(new LambdaQueryWrapper<CookingTask>()
            .in(CookingTask::getStatus, 0, 1)
            .orderByDesc(CookingTask::getPriorityScore));

        if (tasks.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, pageNum, pageSize);
        }

        // 自动将最高优先级的待制作任务设为制作中
        boolean hasCooking = tasks.stream().anyMatch(t -> t.getStatus() == 1);
        if (!hasCooking) {
            for (CookingTask t : tasks) {
                if (t.getStatus() == 0) {
                    t.setStatus(1);
                    t.setStartTime(LocalDateTime.now());
                    updateById(t);
                    break;
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<Long> taskIds = tasks.stream().map(CookingTask::getId).collect(Collectors.toList());
        List<TaskOrderItem> allItems = taskOrderItemMapper.selectList(
            new LambdaQueryWrapper<TaskOrderItem>().in(TaskOrderItem::getTaskId, taskIds));
        Map<Long, List<TaskOrderItem>> itemsByTaskId = allItems.stream()
            .collect(Collectors.groupingBy(TaskOrderItem::getTaskId));

        List<Map<String, Object>> allRecords = tasks.stream().map(task -> {
            List<TaskOrderItem> items = itemsByTaskId.getOrDefault(task.getId(), Collections.emptyList());

            long totalWait = 0;
            for (TaskOrderItem item : items) {
                Orders order = ordersMapper.selectById(item.getOrderId());
                if (order != null) {
                    long waitMinutes = Duration.between(order.getCreateTime(), now).toMinutes();
                    totalWait += waitMinutes;
                }
            }
            long avgWaitMinutes = items.isEmpty() ? 0 : totalWait / items.size();

            Dish dish = dishMapper.selectById(task.getDishId());
            DishCategory category = dish != null ? categoryMapper.selectById(dish.getCategoryId()) : null;
            double categoryWeight = (category != null ? category.getWeight() : 0) * CATEGORY_WEIGHT;
            double prepTime = (dish != null ? dish.getEstimatedTime() : 0) * PREP_TIME_WEIGHT;
            double priorityScore = avgWaitMinutes * TIME_WEIGHT + categoryWeight - prepTime;

            Map<String, Object> map = new HashMap<>();
            map.put("id", task.getId());
            map.put("dishId", task.getDishId());
            map.put("dishName", task.getDishName());
            map.put("quantity", task.getQuantity());
            map.put("status", task.getStatus());
            map.put("priorityScore", BigDecimal.valueOf(priorityScore));
            map.put("avgWaitMinutes", avgWaitMinutes);

            List<String> tableNos = items.stream()
                .map(TaskOrderItem::getTableNo)
                .distinct()
                .collect(Collectors.toList());
            map.put("tableNos", tableNos);

            return map;
        }).sorted((a, b) -> {
            double scoreA = ((BigDecimal) a.get("priorityScore")).doubleValue();
            double scoreB = ((BigDecimal) b.get("priorityScore")).doubleValue();
            return Double.compare(scoreB, scoreA);
        }).collect(Collectors.toList());

        long total = allRecords.size();
        int fromIndex = (pageNum - 1) * pageSize;
        if (fromIndex >= total) {
            return PageResult.of(Collections.emptyList(), total, pageNum, pageSize);
        }
        int toIndex = (int) Math.min(fromIndex + pageSize, total);
        List<Map<String, Object>> pageRecords = allRecords.subList(fromIndex, toIndex);

        return PageResult.of(pageRecords, total, pageNum, pageSize);
    }

    @Override
    @Transactional
    public void finishTask(Long taskId) {
        CookingTask task = getById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        if (task.getStatus() != 0 && task.getStatus() != 1) {
            throw new RuntimeException("任务状态不正确");
        }
        task.setStatus(2);
        task.setStartTime(LocalDateTime.now());
        task.setFinishTime(LocalDateTime.now());
        updateById(task);

        // 更新所有关联的订单明细状态为已完成
        List<TaskOrderItem> items = taskOrderItemMapper.selectList(
            new LambdaQueryWrapper<TaskOrderItem>().eq(TaskOrderItem::getTaskId, taskId));

        for (TaskOrderItem item : items) {
            OrderItem orderItem = orderItemMapper.selectById(item.getOrderItemId());
            if (orderItem != null) {
                orderItem.setStatus(2);
                orderItem.setFinishTime(LocalDateTime.now());
                orderItemMapper.updateById(orderItem);

                // 更新订单的已完成菜品数
                ordersMapper.incrementCompletedDishes(item.getOrderId());

                // 检查订单是否全部完成
                Orders order = ordersMapper.selectById(item.getOrderId());
                if (order != null && order.getCompletedDishes() + 1 >= order.getTotalDishes()) {
                    order.setStatus(3);
                    order.setCompletedDishes(order.getCompletedDishes() + 1);
                    ordersMapper.updateById(order);
                }
            }
        }
    }

    @Scheduled(fixedRate = 30000)
    public void autoGenerateTasks() {
        generateTasks();
    }

    private static class OrderItemWithWait {
        private OrderItem item;
        private long waitMinutes;

        public OrderItemWithWait(OrderItem item, long waitMinutes) {
            this.item = item;
            this.waitMinutes = waitMinutes;
        }

        public OrderItem getItem() {
            return item;
        }

        public long getWaitMinutes() {
            return waitMinutes;
        }
    }
}
