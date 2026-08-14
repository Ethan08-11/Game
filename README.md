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

## Zeabur 部署（前端静态站）

仓库根已放 `zbpack.json`：构建目录 `frontend/`，产物 `dist/`，使用 `npm run build:web`（不含 Electron）。

若控制台仍从仓库根构建，请到服务 **设置 → Root Directory** 填 `frontend` 后重新部署。

当前配置只保证前端页面可打开；后端（MySQL / Redis / RabbitMQ / Spring Boot）需另建服务并配置 API 地址。

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
