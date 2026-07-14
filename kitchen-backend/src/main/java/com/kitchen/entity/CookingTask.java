package com.kitchen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("cooking_task")
public class CookingTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dishId;
    private String dishName;
    private Integer quantity;
    private Integer status;
    private BigDecimal priorityScore;
    private Long avgWaitMinutes;
    private LocalDateTime createTime;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
}
