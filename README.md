# 枭塔外贸远征

前后端整合仓库（monorepo）。

| 目录 | 说明 |
|------|------|
| `frontend/` | Vue 3 + Vite / Electron 前端 |
| `backend/` | Spring Boot 后端（MySQL / Redis / RabbitMQ） |

原分离仓库仍可用：

- 前端：https://github.com/Ethan08-11/Game_Frontend（分支 `frontapi`）
- 后端：https://github.com/Ethan08-11/Game_backend（分支 `develop`）

## 前端启动

```bash
cd frontend
npm install
npm run dev
```

默认请求后端：`http://192.168.1.25:8080`（见 `frontend/vite.config.ts`；本地可自建 `.env`，勿提交）。

## Zeabur 部署

### 前端（服务名建议 `game`）

使用 `Dockerfile.game`。Nginx 会把 `/api`、`/ws`、`/images` 反代到后端。

环境变量（可选）：

- `BACKEND_UPSTREAM`：默认 `http://backend:8080`（需与后端服务名一致）

### 后端（服务名必须为 `backend`，或改 `BACKEND_UPSTREAM`）

使用 `Dockerfile.backend`。同项目内还需添加：

1. **MySQL**（导入 `backend/sql_file` 中的初始化脚本）
2. **Redis**
3. **RabbitMQ**

后端常用环境变量：

```text
MYSQL_HOST=...
MYSQL_PORT=3306
MYSQL_DATABASE=wa_demo
MYSQL_USERNAME=...
MYSQL_PASSWORD=...
REDIS_HOST=...
REDIS_PORT=6379
REDIS_PASSWORD=...
RABBITMQ_HOST=...
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=...
RABBITMQ_PASSWORD=...
NACOS_DISCOVERY_ENABLED=false
NACOS_CONFIG_ENABLED=false
```

只部署前端、不部署后端时，登录会返回 **HTTP 405**（请求打到 Nginx 静态站）。


## 后端启动

详见 `backend/README.md` 与 `backend/项目配置与运行说明.md`。

常见依赖（示例端口）：

- MySQL `3308`
- Redis `6379`
- RabbitMQ `5672`
- 应用 `8080`

可用 `backend/docker-compose*.yml` 拉起基础设施后再编译运行 `wa-demo-service`。

## 目录约定

- 不提交 `.env`、`node_modules/`、`**/target/`、构建产物
- 敏感配置请用本地环境变量或未入库的 `.env`
