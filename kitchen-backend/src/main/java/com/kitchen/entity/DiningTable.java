package com.kitchen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("dining_table")
public class DiningTable implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tableNo;
    private String area;
    private String type;
    @TableField(exist = false)
    private BigDecimal baseCharge;
    private Integer status;
    private Long currentOrderId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
