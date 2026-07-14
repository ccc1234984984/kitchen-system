-- 制作任务表
CREATE TABLE IF NOT EXISTS cooking_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dish_id BIGINT NOT NULL COMMENT '菜品ID',
    dish_name VARCHAR(100) NOT NULL COMMENT '菜品名称',
    quantity INT NOT NULL DEFAULT 1 COMMENT '制作数量',
    status INT NOT NULL DEFAULT 0 COMMENT '状态：0-待制作，1-制作中，2-已完成',
    priority_score DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优先级分数',
    avg_wait_minutes BIGINT NOT NULL DEFAULT 0 COMMENT '平均等待时间（分钟）',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    start_time DATETIME COMMENT '开始制作时间',
    finish_time DATETIME COMMENT '完成时间',
    INDEX idx_dish_id (dish_id),
    INDEX idx_status (status),
    INDEX idx_priority (priority_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='制作任务表';

-- 任务关联表
CREATE TABLE IF NOT EXISTS task_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL COMMENT '任务ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_item_id BIGINT NOT NULL COMMENT '订单明细ID',
    table_no VARCHAR(20) NOT NULL COMMENT '桌号',
    wait_minutes BIGINT NOT NULL DEFAULT 0 COMMENT '等待时间（分钟）',
    INDEX idx_task_id (task_id),
    INDEX idx_order_id (order_id),
    INDEX idx_order_item_id (order_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务关联表';
