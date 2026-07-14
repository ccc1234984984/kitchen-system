package com.kitchen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("dish_category")
public class DishCategory implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer weight;
    private Integer sortOrder;
    private LocalDateTime createTime;
}
