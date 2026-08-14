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

## 一步到位：用真实内网主机名

1. 打开 **backend** 服务 → **网络 / Networking**
2. 复制 **内网访问** 里的 Hostname（例如可能是 `backend.zeabur.internal`，也可能是别的名字如 `wa-demo.zeabur.internal`）
3. 打开 **game** → **环境变量**，设置：

```text
BACKEND_UPSTREAM=http://【上一步复制的主机名】:8080
```

示例：

```text
BACKEND_UPSTREAM=http://backend.zeabur.internal:8080
```

4. **保存后重新部署 / 重启 game**

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
- 若很少或为 0：需要重新完整导入 `backend/sql_file/wa_demo最终版.sql`

公网导入请在**本机 cmd**（不要用 PowerShell 的 `<`）：

```bat
"C:\Yzr\Mysql5.7\mysql-5.7.37-winx64\bin\mysql.exe" -h 43.133.220.242 -P 32030 -u root -p你的密码 --default-character-set=utf8mb4 -e "DROP DATABASE IF EXISTS wa_demo; CREATE DATABASE wa_demo DEFAULT CHARACTER SET utf8mb4;"

"C:\Yzr\Mysql5.7\mysql-5.7.37-winx64\bin\mysql.exe" -h 43.133.220.242 -P 32030 -u root -p你的密码 --default-character-set=utf8mb4 --max_allowed_packet=256M wa_demo < "C:\Users\30543\AppData\Local\Temp\wa_demo_final.sql"
```

（先把 SQL 复制到无中文路径：`copy` 到 `%TEMP%\wa_demo_final.sql`）

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
