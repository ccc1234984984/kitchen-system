package com.kitchen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Orders implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tableNo;
    private Long tableId;
    private Long userId;
    private Integer paymentStatus;
    private LocalDateTime paymentTime;
    private Integer status;
    private Integer totalDishes;
    private Integer completedDishes;
    private BigDecimal priorityScore;
    private BigDecimal totalPrice;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
