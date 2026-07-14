-- 更新菜品制作时间（estimated_time字段，单位：分钟）
-- 米饭馒头制作时间为0，其他根据菜品类型设置合理时间

-- 主食类：米饭、馒头等 - 0分钟
UPDATE dish SET estimated_time = 0 WHERE name LIKE '%米饭%' OR name LIKE '%馒头%' OR name LIKE '%粥%';

-- 凉拌菜：2-3分钟
UPDATE dish SET estimated_time = 2 WHERE name LIKE '%凉拌%' OR name LIKE '%冷拌%' OR name LIKE '%拍黄瓜%';

-- 快炒菜：5-8分钟
UPDATE dish SET estimated_time = 5 WHERE name LIKE '%炒蛋%' OR name LIKE '%青菜%' OR name LIKE '%豆芽%';

-- 普通炒菜：8-12分钟
UPDATE dish SET estimated_time = 10 WHERE name LIKE '%炒%' AND estimated_time IS NULL;

-- 红烧、炖菜：15-20分钟
UPDATE dish SET estimated_time = 15 WHERE name LIKE '%红烧%' OR name LIKE '%炖%' OR name LIKE '%排骨%';

-- 蒸菜：10-15分钟
UPDATE dish SET estimated_time = 12 WHERE name LIKE '%蒸%';

-- 汤类：10-15分钟
UPDATE dish SET estimated_time = 12 WHERE name LIKE '%汤%';

-- 炸物：8-12分钟
UPDATE dish SET estimated_time = 10 WHERE name LIKE '%炸%';

-- 如果还有未设置的，默认为8分钟
UPDATE dish SET estimated_time = 8 WHERE estimated_time IS NULL;
