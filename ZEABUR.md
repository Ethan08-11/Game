# Zeabur 联调：登录 502 / Host not found

## 日志含义

```text
backend.zeabur.internal could not be resolved (3: Host not found)
POST /api/auth/login → 502
```

表示 **game 的 Nginx 找不到名为 `backend.zeabur.internal` 的内网主机**。  
这通常不是密码问题，而是：**后端服务内网主机名不是这个**，或后端未在同一项目成功建好网络。

> Zeabur 官方说明：**修改服务显示名称，不会改变内网 hostname。**

---

## 部署 backend（Dockerfile 必填）

仓库根已有默认 **`Dockerfile`**（后端）。请按下面做，不要依赖自动检测。

### backend 服务（任选一种，推荐 1）

**方式 1：什么都不填 Root Directory**
1. **设置 → Root Directory** 留空
2. 直接点 **重新部署**（会使用根目录 `Dockerfile` = 后端）

**方式 2：指定文件**
环境变量：
```text
ZBPACK_DOCKERFILE_PATH=Dockerfile.backend
```
或：
```text
ZBPACK_DOCKERFILE_NAME=backend
```

**方式 3：子目录**
1. Root Directory = `backend`
2. 使用 `backend/Dockerfile`
3. 重新部署

### game 服务（必须指定，否则会误用后端 Dockerfile）

环境变量：
```text
ZBPACK_DOCKERFILE_PATH=Dockerfile.game
BACKEND_UPSTREAM=https://handinhand-api.zeabur.app
```

（`BACKEND_UPSTREAM` 用你 backend 已生成的公网域名。）

你的环境里 `backend.zeabur.internal` 一直 **Host not found**，请改用公网域名反代：

1. **backend → 网络 → 公网访问 → 生成域名**  
   得到例如：`https://xxxx.zeabur.app`
2. 确认 backend **运行中**（端口 HTTP 8080 已暴露）
3. **game → 环境变量**：

```text
BACKEND_UPSTREAM=https://xxxx.zeabur.app
```

注意：不要加路径，不要末尾 `/`，不要写 `:8080`（公网域名已映射到 8080）。

4. **重新部署 / 重启 game**

登录请求会变成：浏览器 → game → `https://xxxx.zeabur.app/api/...` → backend。

---

## 确认 backend 本身是活的

在同一项目里应有：

| 服务 | 状态 |
|------|------|
| mysql | 运行中 |
| redis | 运行中 |
| rabbitmq | 运行中（若没有请从模板添加） |
| backend | 运行中（不能崩溃重试） |

backend 环境变量（用「引用」绑定同项目服务）：

```text
MYSQL_HOST=mysql.zeabur.internal   # 以 mysql→网络 页为准
MYSQL_PORT=3306
MYSQL_DATABASE=wa_demo
MYSQL_USERNAME=root
MYSQL_PASSWORD=（引用 MYSQL_PASSWORD）
REDIS_HOST=redis.zeabur.internal   # 以 redis→网络 页为准
REDIS_PORT=6379
RABBITMQ_HOST=...                  # 以 rabbitmq→网络 页为准
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=...
RABBITMQ_PASSWORD=...
NACOS_DISCOVERY_ENABLED=false
NACOS_CONFIG_ENABLED=false
```

若 backend 一直崩溃：把 **backend 运行日志最后 40 行**发出来。

---

## 数据库 wa_demo

应用库名是 **`wa_demo`**（不是默认的 `zeabur`）。

在 mysql 容器命令行：

```bash
mysql -u"$MYSQL_USERNAME" -p"$PASSWORD" -e "SHOW DATABASES; SELECT COUNT(*) AS tables_cnt FROM information_schema.tables WHERE table_schema='wa_demo';"
```

- `tables_cnt` 应接近 **26**
- 若很少或为 0：需要导入 SQL

### 推荐：Zeabur 精简版（表结构齐全 + 卡牌/用户等基础数据，去掉对战历史）

文件：`backend/sql_file/wa_demo_zeabur.sql`（约 85KB，公网可稳定导入）

本机 cmd：

```bat
copy /Y "项目路径\Game\backend\sql_file\wa_demo_zeabur.sql" "%TEMP%\wa_demo_zeabur.sql"

"C:\Yzr\Mysql5.7\mysql-5.7.37-winx64\bin\mysql.exe" -h 公网IP -P 端口 -u root -p你的密码 --default-character-set=utf8mb4 -e "DROP DATABASE IF EXISTS wa_demo; CREATE DATABASE wa_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

"C:\Yzr\Mysql5.7\mysql-5.7.37-winx64\bin\mysql.exe" -h 公网IP -P 端口 -u root -p你的密码 --default-character-set=utf8mb4 --max_allowed_packet=512M --binary-mode wa_demo < "%TEMP%\wa_demo_zeabur.sql"
```

### 完整最终版（含大量 match_cards 历史，公网易 Lost connection）

文件：`backend/sql_file/wa_demo最终版.sql`

更稳妥在 **mysql 容器内**下载后导入；公网导入常在 `match_cards` 处断线。

```bat
"C:\Yzr\Mysql5.7\mysql-5.7.37-winx64\bin\mysql.exe" -h 43.133.220.242 -P 32030 -u root -p你的密码 --default-character-set=utf8mb4 -e "DROP DATABASE IF EXISTS wa_demo; CREATE DATABASE wa_demo DEFAULT CHARACTER SET utf8mb4;"

"C:\Yzr\Mysql5.7\mysql-5.7.37-winx64\bin\mysql.exe" -h 43.133.220.242 -P 32030 -u root -p你的密码 --default-character-set=utf8mb4 --max_allowed_packet=256M wa_demo < "C:\Users\30543\AppData\Local\Temp\wa_demo_final.sql"
```

（完整版请先把 SQL 复制到无中文路径：`copy` 到 `%TEMP%\wa_demo_final.sql`）

---

## 备选：给 backend 绑公网域名

若内网怎么都解析不了：

1. backend → 网络 → 生成域名（如 `xxx.zeabur.app`）
2. game 环境变量：

```text
BACKEND_UPSTREAM=http://xxx.zeabur.app
```

（仅当 backend 以 HTTP 对外且路径仍是 `/api/...` 时可用；优先还是用内网。）

---

## 检查清单

- [ ] backend「网络」页复制到的内网 Hostname 已写入 game 的 `BACKEND_UPSTREAM`
- [ ] game 已重启/重部署
- [ ] rabbitmq 已添加且 backend 已引用
- [ ] `wa_demo` 表数量约 26
- [ ] 登录不再出现「后端服务未就绪」
