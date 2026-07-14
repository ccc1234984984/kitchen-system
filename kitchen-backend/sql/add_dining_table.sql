-- 餐桌表
CREATE TABLE IF NOT EXISTS `dining_table` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '餐桌ID',
    `table_no` VARCHAR(20) NOT NULL COMMENT '桌号（A101, B201, C301等）',
    `area` VARCHAR(10) NOT NULL COMMENT '区域（A/B/C）',
    `type` VARCHAR(20) NOT NULL COMMENT '类型（普通餐桌/普通包间/豪华包间）',
    `status` TINYINT DEFAULT 0 COMMENT '状态 0:空闲 1:就餐中 2:待清理',
    `current_order_id` BIGINT DEFAULT NULL COMMENT '当前订单ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_table_no` (`table_no`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='餐桌表';

-- 订单表增加总价字段和餐桌ID关联
ALTER TABLE `orders`
    ADD COLUMN `total_price` DECIMAL(10,2) DEFAULT 0 COMMENT '订单总价' AFTER `priority_score`,
    ADD COLUMN `table_id` BIGINT DEFAULT NULL COMMENT '关联餐桌ID' AFTER `table_no`,
    ADD KEY `idx_table_id` (`table_id`);

-- 初始化餐桌数据（A区 普通餐桌 1-9）
INSERT INTO `dining_table` (`table_no`, `area`, `type`, `status`) VALUES
('A101', 'A', '普通餐桌', 0),
('A102', 'A', '普通餐桌', 0),
('A103', 'A', '普通餐桌', 0),
('A104', 'A', '普通餐桌', 0),
('A105', 'A', '普通餐桌', 0),
('A106', 'A', '普通餐桌', 0),
('A107', 'A', '普通餐桌', 0),
('A108', 'A', '普通餐桌', 0),
('A109', 'A', '普通餐桌', 0);

-- B区 普通包间 1-6
INSERT INTO `dining_table` (`table_no`, `area`, `type`, `status`) VALUES
('B201', 'B', '普通包间', 0),
('B202', 'B', '普通包间', 0),
('B203', 'B', '普通包间', 0),
('B204', 'B', '普通包间', 0),
('B205', 'B', '普通包间', 0),
('B206', 'B', '普通包间', 0);

-- C区 豪华包间 1-4
INSERT INTO `dining_table` (`table_no`, `area`, `type`, `status`) VALUES
('C301', 'C', '豪华包间', 0),
('C302', 'C', '豪华包间', 0),
('C303', 'C', '豪华包间', 0),
('C304', 'C', '豪华包间', 0);
