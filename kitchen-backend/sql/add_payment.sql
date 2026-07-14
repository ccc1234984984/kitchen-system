ALTER TABLE orders ADD COLUMN payment_status INT DEFAULT 0 COMMENT '支付状态 0-待支付 1-已支付' AFTER user_id;
ALTER TABLE orders ADD COLUMN payment_time DATETIME NULL COMMENT '支付时间' AFTER payment_status;
