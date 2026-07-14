package com.kitchen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kitchen.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {
    @Update("UPDATE orders SET priority_score = #{score} WHERE id = #{id}")
    int updatePriorityScore(@Param("id") Long id, @Param("score") Double score);

    @Update("UPDATE orders SET completed_dishes = completed_dishes + 1 WHERE id = #{id}")
    int incrementCompletedDishes(@Param("id") Long id);
}
