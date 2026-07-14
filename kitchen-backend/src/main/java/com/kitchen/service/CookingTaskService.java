package com.kitchen.service;

import com.kitchen.common.PageResult;
import com.kitchen.entity.CookingTask;

import java.util.Map;

public interface CookingTaskService {
    void generateTasks();
    PageResult<Map<String, Object>> getTaskList(int pageNum, int pageSize);
    void finishTask(Long taskId);
}
