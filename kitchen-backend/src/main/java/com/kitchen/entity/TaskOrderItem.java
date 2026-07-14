package com.kitchen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("task_order_item")
public class TaskOrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long orderId;
    private Long orderItemId;
    private String tableNo;
    private Long waitMinutes;
}
