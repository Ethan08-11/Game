# Zeabur 全栈部署步骤（前后端）

仓库：`Ethan08-11/Game`  
前端服务名：`game`（已有）  
后端服务名：**必须叫 `backend`**（内网域名 `backend.zeabur.internal`）

---

## 一、在同一项目添加中间件（模板）

依次：**添加服务 → 从模板部署**

1. **MySQL**
2. **Redis**
3. **RabbitMQ**

等待三个服务都变成「运行中」。

### 初始化 MySQL 数据

1. 打开 MySQL 服务，记下连接信息 / Instruction
2. 用客户端或 Zeabur 提供的连接命令，导入仓库文件：

```text
backend/sql_file/wa_demo最终版.sql
```

（若数据库名不是 `wa_demo`，先建库 `wa_demo`，或把 Zeabur `MYSQL_DATABASE` 改成 `wa_demo` 后重建。）

---

## 二、部署后端服务

1. **添加服务 → Git** → 选择 `Ethan08-11/Game`，分支 `main`
2. **服务名称改成：`backend`**（重要）
3. Zeabur 会匹配根目录 `Dockerfile.backend`
4. 在后端「环境变量」中确认 / 填写（多数可由模板自动注入后引用）：

| 变量 | 说明 |
|------|------|
| `MYSQL_HOST` | 引用 MySQL 服务注入值 |
| `MYSQL_PORT` | 通常 `3306` |
| `MYSQL_DATABASE` | `wa_demo` |
| `MYSQL_USERNAME` | MySQL 用户 |
| `MYSQL_PASSWORD` | MySQL 密码 |
| `REDIS_HOST` | Redis 主机 |
| `REDIS_PORT` | `6379` |
| `REDIS_PASSWORD` | 若有则填 |
| `RABBITMQ_HOST` | RabbitMQ 主机 |
| `RABBITMQ_PORT` | `5672` |
| `RABBITMQ_USERNAME` 或 `RABBITMQ_DEFAULT_USER` | 用户名 |
| `RABBITMQ_PASSWORD` 或 `RABBITMQ_DEFAULT_PASS` | 密码 |
| `NACOS_DISCOVERY_ENABLED` | `false` |
| `NACOS_CONFIG_ENABLED` | `false` |

5. 端口：HTTP `8080`（或使用 Zeabur 注入的 `PORT`，镜像已支持）
6. 部署成功后，日志里应出现 Spring Boot 启动完成；`/actuator/health` 可用

Zeabur 变量引用示例（在变量值里选「引用其他服务」）：

```text
MYSQL_HOST = ${MYSQL_HOST}
MYSQL_PASSWORD = ${MYSQL_PASSWORD}
...
```

（以控制台实际可引用名为准。）

---

## 三、把前端接到后端

打开已有 **`game`** 服务 → 环境变量：

```text
BACKEND_UPSTREAM=http://backend.zeabur.internal:8080
```

然后 **重新部署** `game`。

前端 Nginx 会把同源的 `/api`、`/ws`、`/images` 反代到后端，因此浏览器仍访问：

`https://handinhandgame.zeabur.app`

无需给后端单独绑公网域名（也可以绑，便于查日志）。

---

## 四、验证

1. 打开站点 → 登录
2. 不应再出现「后端服务未就绪」
3. 若仍 502：看 `backend` 是否 Running、MySQL 是否已导入、`BACKEND_UPSTREAM` 是否写对

---

## 费用与规格提示

后端 + 三个中间件会明显增加资源占用。建议后端至少 **1C 1GB+**；首次 Maven 构建较慢属正常。
