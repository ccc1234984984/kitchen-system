-- 后厨智能出餐调度系统 - 数据库脚本

CREATE DATABASE IF NOT EXISTS kitchen_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE kitchen_db;

-- 菜品分类表
CREATE TABLE IF NOT EXISTS `dish_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `weight` INT DEFAULT 0 COMMENT '制作耗时权重(越大越耗时)',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品分类表';

-- 菜品表
CREATE TABLE IF NOT EXISTS `dish` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜品ID',
    `name` VARCHAR(100) NOT NULL COMMENT '菜品名称',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '价格',
    `estimated_time` INT DEFAULT 10 COMMENT '预估制作时间(分钟)',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0:下架 1:上架',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品表';

-- 订单表
CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `table_no` VARCHAR(20) NOT NULL COMMENT '桌号',
    `status` TINYINT DEFAULT 0 COMMENT '订单状态 0:待接单 1:已接单 2:制作中 3:已出餐 4:已完成',
    `total_dishes` INT DEFAULT 0 COMMENT '总菜品数',
    `completed_dishes` INT DEFAULT 0 COMMENT '已出餐数',
    `priority_score` DECIMAL(10,2) DEFAULT 0 COMMENT '优先级得分',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 订单明细表
CREATE TABLE IF NOT EXISTS `order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `dish_id` BIGINT NOT NULL COMMENT '菜品ID',
    `dish_name` VARCHAR(100) COMMENT '菜品名称',
    `category_name` VARCHAR(50) COMMENT '分类名称',
    `status` TINYINT DEFAULT 0 COMMENT '状态 0:待制作 1:制作中 2:已完成',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `finish_time` DATETIME COMMENT '完成时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 初始化数据
INSERT INTO `dish_category` (`name`, `weight`, `sort_order`) VALUES
('冷菜', 1, 1),
('热菜', 3, 2),
('汤品', 2, 3),
('主食', 2, 4),
('甜点', 1, 5);

INSERT INTO `dish` (`name`, `category_id`, `price`, `estimated_time`) VALUES
('凉拌黄瓜', 1, 12.00, 3),
('凉拌木耳', 1, 15.00, 3),
('拍黄瓜', 1, 10.00, 2),
('红烧肉', 2, 38.00, 15),
('糖醋排骨', 2, 42.00, 18),
('宫保鸡丁', 2, 32.00, 12),
('麻婆豆腐', 2, 22.00, 10),
('清炒时蔬', 2, 18.00, 8),
('西红柿蛋汤', 3, 15.00, 8),
('酸辣汤', 3, 18.00, 10),
('米饭', 4, 3.00, 2),
('馒头', 4, 2.00, 3),
('蛋炒饭', 4, 18.00, 8),
('水果拼盘', 5, 25.00, 5),
('冰淇淋', 5, 15.00, 2);
