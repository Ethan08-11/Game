# Docker 交付启动说明

## 运行模式
本项目拆分为两部分：

1. **基础设施 Compose**：`docker-compose.infrastructure.yml`
   - MySQL（端口 3308）
   - Redis（端口 6380）
   - RabbitMQ（端口 5673 / 管理台 15673）

2. **应用 Compose**：`docker-compose.app.yml`
   - 后端（端口 8081）
   - 前端（端口 80）

两部分共用外部网络 `wa-net`，已移除 Nacos 依赖。

---

## 前置条件
- 已安装 Docker / Docker Desktop
- 若使用 `docker compose`，请确认 Compose 插件可用
- 若不支持，请用 `docker-compose`（带连字符）代替

---

## 本机首次运行

### 1. 构建前后端镜像（本机只需做一次）
```bash
docker build -f deploy/backend/Dockerfile -t wa-backend:1.1 .
docker build -f deploy/frontend/Dockerfile -t wa-frontend:1.0 .
```

### 2. 创建网络
```bash
docker network create wa-net
```

### 3. 启动基础设施
```bash
docker-compose -f docker-compose.infrastructure.yml up -d
```

### 4. 启动应用
```bash
docker-compose -f docker-compose.app.yml up -d
```

### 5. 访问验证
- `http://localhost`
- `http://localhost:8081/actuator/health`
- `http://localhost:15673`

---

## 别的电脑上如何运行

### 交付文件清单
把下面这些文件一起发给对方：
- `docker-compose.infrastructure.yml`
- `docker-compose.app.yml`
- `.env`（从 `.env.example` 复制并按需修改）
- `mysql/init/001_schema.sql`
- `mysql/init/002_data.sql`
- 镜像包（离线时需要）：
  - `mysql.tar`
  - `redis.tar`
  - `rabbitmq.tar`
  - `wa-backend.tar`
  - `wa-frontend.tar`

对方不需要前端源码、后端源码、Dockerfile、nginx.conf。

### 对方操作步骤

#### 1. 安装 Docker
确认电脑已安装 Docker / Docker Desktop。

#### 2. 把交付文件放到一个目录
推荐结构：
```text
project-root/
├─ docker-compose.infrastructure.yml
├─ docker-compose.app.yml
├─ .env
├─ mysql/
│  └─ init/
│     ├─ 001_schema.sql
│     └─ 002_data.sql
├─ mysql.tar
├─ redis.tar
├─ rabbitmq.tar
├─ wa-backend.tar
└─ wa-frontend.tar
```

#### 3. 导入镜像（离线环境必须，联网环境可跳过）
```bash
docker load -i mysql.tar
docker load -i redis.tar
docker load -i rabbitmq.tar
docker load -i wa-backend.tar
docker load -i wa-frontend.tar
```

#### 4. 创建本地网络
```bash
docker network create wa-net
```

#### 5. 启动基础设施
```bash
docker-compose -f docker-compose.infrastructure.yml up -d
```

#### 6. 等待中间件启动完成
```bash
docker ps
```
确认 MySQL、Redis、RabbitMQ 都是 `Up (healthy)`。

#### 7. 启动前后端
```bash
docker-compose -f docker-compose.app.yml up -d
```

#### 8. 访问项目
- 前端：`http://localhost`
- 后端健康检查：`http://localhost:8081/actuator/health`
- RabbitMQ 管理台：`http://localhost:15673`

---

## 导出镜像（你发给对方之前做）
```bash
docker save -o mysql.tar mysql:8.0.36
docker save -o redis.tar redis:7.2-alpine
docker save -o rabbitmq.tar rabbitmq:3.13-management
docker save -o wa-backend.tar wa-backend:1.1
docker save -o wa-frontend.tar wa-frontend:1.0
```

---

## 常用命令

### 查看容器状态
```bash
docker ps
```

### 查看后端日志
```bash
docker logs -f deploy-backend
```

### 查看前端日志
```bash
docker logs -f deploy-frontend
```

### 停止应用
```bash
docker-compose -f docker-compose.app.yml down
```

### 停止基础设施
```bash
docker-compose -f docker-compose.infrastructure.yml down
```

### 停止并删除数据卷（清空数据库）
```bash
docker-compose -f docker-compose.infrastructure.yml down -v
```

---

## 注意事项
- 基础设施和应用不要用两份 compose 重复启动同名容器
- 如果端口冲突，请修改 `.env` 里的端口
- 数据卷会保留数据库数据，重启容器不会丢失
- 本项目不再依赖 Nacos
- 前后端使用预构建镜像，对方不需要源码
