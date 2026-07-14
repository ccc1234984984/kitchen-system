# 后厨智能出餐调度系统

## 项目介绍

解决餐厅后厨出餐顺序不合理的问题，通过智能调度算法自动计算订单优先级，生成科学合理的出餐队列。

## 技术栈

- 后端：Spring Boot 2.7 + Mybatis-plus 3.5
- 前端：Vue 3 + Element Plus + Vite
- 数据库：MySQL 8.0

## 项目结构

```
kitchen-system/
├── kitchen-backend/          # 后端项目
│   ├── src/main/java/com/kitchen/
│   │   ├── controller/       # 控制器
│   │   ├── service/          # 业务逻辑
│   │   ├── mapper/           # 数据访问
│   │   ├── entity/           # 实体类
│   │   └── config/           # 配置类
│   ├── src/main/resources/
│   │   └── application.yml   # 配置文件
│   └── sql/
│       └── init.sql          # 数据库脚本
│
└── kitchen-frontend/         # 前端项目
    ├── src/
    │   ├── api/              # API 接口
    │   ├── views/            # 页面组件
    │   ├── App.vue
    │   └── main.js
    └── package.json
```

## 快速启动

### 1. 初始化数据库

```sql
mysql -u root -p < kitchen-backend/sql/init.sql
```

修改 `application.yml` 中的数据库连接配置。

### 2. 启动后端

```bash
cd kitchen-backend
mvn spring-boot:run
```

后端启动在 http://localhost:8080

### 3. 启动前端

```bash
cd kitchen-frontend
npm install
npm run dev
```

前端启动在 http://localhost:5173

## 核心功能

### 智能调度算法

```
优先级得分 = 等待时间(分钟) × 1.0 + 菜品数量 × 0.5 + 分类权重
```

- 等待时间越长、菜品越多，优先级越高
- 队列按优先级得分降序排列
- 每5秒自动刷新队列

### 订单状态流转

```
待接单(0) → 已接单(1) → 制作中(2) → 已出餐(3) → 已完成(4)
```

### 页面功能

- **后厨看板**：显示当前出餐队列，支持接单、开始制作、完成菜品等操作
- **点餐前台**：选择桌号和菜品，创建新订单
