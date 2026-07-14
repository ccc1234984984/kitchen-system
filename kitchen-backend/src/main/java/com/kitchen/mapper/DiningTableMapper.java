package com.kitchen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kitchen.entity.DiningTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DiningTableMapper extends BaseMapper<DiningTable> {

    @Update("UPDATE dining_table SET status = #{status}, current_order_id = #{orderId} WHERE id = #{id}")
    int updateStatusAndOrder(@Param("id") Long id, @Param("status") Integer status, @Param("orderId") Long orderId);

    @Update("UPDATE dining_table SET status = #{status}, current_order_id = NULL WHERE id = #{id}")
    int clearTable(@Param("id") Long id, @Param("status") Integer status);

}
