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

## Zeabur 部署（前端）

仓库根使用 `Dockerfile.game`（与控制台服务名 `game` 对应）构建 `frontend` 的 Web 产物并用 Nginx 在 `8080` 托管。

部署后打开域名应能看到前端页面。若仍 404：打开服务 **设置**，确认未错误指定 Root Directory，或点 **重新部署**。

当前只部署前端；后端（MySQL / Redis / RabbitMQ / Spring Boot）需另建服务并配置 API 地址。

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
