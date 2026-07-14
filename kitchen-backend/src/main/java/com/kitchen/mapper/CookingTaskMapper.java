package com.kitchen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kitchen.entity.CookingTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CookingTaskMapper extends BaseMapper<CookingTask> {
}
