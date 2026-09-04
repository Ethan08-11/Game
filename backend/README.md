# 项目部署、运行与后端说明

本文档面向本项目的**本地开发、局域网联机、以及交付部署**场景，统一整理以下内容：

- 项目整体结构
- 后端模块职责
- 接口功能总览
- 中间件说明：MySQL / Redis / Nacos / RabbitMQ
- 最终版数据库结构说明
- 项目运行与配置方式
- 关键业务流程说明

数据库最终版来源于：

```text
sql_file/wa_demo最终版.sql
```

---

## 1. 项目定位

这是一个基于 **Spring Boot + Vue + MySQL + Redis + Nacos + RabbitMQ** 的对战类游戏项目。

当前运行模式通常是：

- **后端**：本机运行
- **前端**：本机运行（Vite / Vue）
- **MySQL、Redis、Nacos、RabbitMQ**：运行在 Docker 中
- 局域网内其他用户通过你的本机 IP 访问前端页面

典型访问方式如下：

- 前端：`http://你的局域网IP:5173`
- 后端：`http://你的局域网IP:8080`
- Nacos：`http://你的局域网IP:8848`
- RabbitMQ 管理台：`http://你的局域网IP:15672`

---

## 2. 仓库整体结构

```text
Game_backend1/
├─ wa-demo-service/          # 后端主服务（Spring Boot）
├─ wa-api/                   # Feign 接口与远程 DTO
├─ wa-common/                # 通用返回体、常量、异常、事件对象
├─ Game_Frontend/            # Vue + Vite 前端
├─ sql_file/wa_demo最终版.sql # 最终版数据库结构与初始化数据
├─ mysql/init/               # Docker 初始化 SQL
├─ deploy/                   # 部署说明、Dockerfile、Nginx 配置
├─ docker-compose.yml        # 本地开发基础设施（含 Nacos）
├─ docker-compose.infrastructure.yml # 交付版基础设施
├─ docker-compose.app.yml    # 交付版前后端
└─ README.md
```

---

## 3. 后端模块职责

### 3.1 `wa-demo-service`

这是核心业务服务，包含：

- Controller：对外 HTTP 接口
- Service / ServiceImpl：核心业务逻辑
- Mapper：MyBatis-Plus 数据访问
- Entity：数据库实体
- DTO：请求与响应对象
- Config：Spring / WebSocket / CORS / MyBatis / RabbitMQ / Redis / Nacos 等配置
- MQ：消息生产与消费
- Task：定时任务
- Security：Token 与鉴权
- Common：分布式锁、通用工具等

### 3.2 `wa-api`

存放远程调用相关接口与 DTO，主要用于：

- Feign Client
- 远程用户信息返回对象
- 跨模块复用接口定义

### 3.3 `wa-common`

存放跨模块通用内容，例如：

- `Result` / `ResultCode`
- `BusinessException`
- 事件对象
- 常量类

---

## 4. 后端配置说明

后端配置文件：

```text
wa-demo-service/src/main/resources/application.yml
```

### 4.1 端口与基础配置

- 后端默认端口：`8080`
- 应用名：`wa-demo-service`
- 静态资源：Spring Boot `resources/static` 目录下的内容

### 4.2 数据源

后端默认读取如下环境变量：

- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`

默认值通常可以直接连到 Docker 中的 MySQL：

- 主机：`127.0.0.1`
- 端口：`3308`
- 数据库：`wa_demo`

### 4.3 Redis

后端默认读取：

- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`

Redis 主要用于：

- Token / 会话相关能力
- 分布式锁
- 房间状态与在线状态缓存
- 某些临时业务状态

### 4.4 RabbitMQ

后端默认读取：

- `RABBITMQ_HOST`
- `RABBITMQ_PORT`
- `RABBITMQ_USERNAME`
- `RABBITMQ_PASSWORD`
- `RABBITMQ_VHOST`

RabbitMQ 主要用于：

- 房间事件通知
- 邀请创建 / 接受 / 拒绝
- 准备状态同步
-  حضور / 在线状态广播

### 4.5 Nacos

项目支持 Nacos 远程配置与服务发现，配置项如下：

- `NACOS_SERVER_ADDR`
- `NACOS_NAMESPACE`
- `NACOS_GROUP`
- `NACOS_USERNAME`
- `NACOS_PASSWORD`

当前后端配置为 `optional:nacos`，也就是说：

- **Nacos 可用时，后端可读取远程配置**
- **Nacos 不可用时，本地也能按默认配置启动**

---

## 5. 前端配置说明

前端项目位于：

```text
Game_Frontend
```

### 5.1 开发服务

前端开发服务由 Vite 提供：

- 默认端口：`5173`
- 监听地址：`0.0.0.0`

### 5.2 接口代理

当前前端只代理了 `/api`，即：

- `/api/**` 会转发到后端
- `/images/**` 这类静态资源不会自动代理到后端

所以图片如果要被前端正常访问，通常需要：

- 直接使用后端真实地址
- 或者在前端 / 网关层增加静态资源代理

### 5.3 静态图片

后端图片资源位于：

```text
wa-demo-service/src/main/resources/static/images/
```

例如：

- `/images/cards/Card_Carl.png`
- `/images/customer/p1.png`

在浏览器中，最终要访问的完整地址应指向后端端口，例如：

```text
http://你的局域网IP:8080/images/cards/Card_Carl.png
```

---

## 6. 中间件说明

本项目的中间件由 Docker 承载，分别负责数据库、缓存、配置中心与消息队列。

### 6.1 MySQL

用途：

- 持久化账号、好友、房间、对局、卡牌、任务、成就等全部核心数据

建议版本：

- MySQL 8.0.x

常见端口映射：

- 本地开发：`3308:3306`
- 交付环境：可按 compose 调整

### 6.2 Redis

用途：

- Token / 登录态
- 分布式锁
- 临时状态缓存
- 在线状态 / 房间状态同步

常见端口映射：

- 本地开发：`6379`
- 交付环境：可按 compose 调整

### 6.3 Nacos

用途：

- 配置中心
- 服务注册发现

说明：

- 本地开发可启用
- 若不使用远程配置，也不会影响后端按默认值启动

### 6.4 RabbitMQ

用途：

- 邀请、准备、房间状态、存在感广播等异步事件
- 解耦房间操作与通知逻辑

常见端口映射：

- AMQP：`5672`
- 管理台：`15672`

---

## 7. 本地运行顺序

### 7.1 启动 Docker 中间件

如果你使用的是本地开发 compose：

```powershell
docker compose up -d
```

如果你只需要交付版基础设施，可使用：

```powershell
docker-compose -f docker-compose.infrastructure.yml up -d
```

### 7.2 启动后端

进入后端目录：

```powershell
cd wa-demo-service
```

启动：

```powershell
mvn spring-boot:run
```

或者在 IDEA 中直接运行 `DemoApplication`。

### 7.3 启动前端

进入前端目录：

```powershell
cd Game_Frontend
npm install
npm run dev
```

### 7.4 访问验证

- 前端：`http://127.0.0.1:5173`
- 后端健康检查：`http://127.0.0.1:8080/actuator/health`
- Nacos：`http://127.0.0.1:8848`
- RabbitMQ：`http://127.0.0.1:15672`

---

## 8. 后端接口总览

下面按照 Controller 分组说明各接口的职责。接口统一以 `Result<T>` 返回，调用前大多需要携带 `Authorization`。

### 8.1 `AuthController`  `/api/auth`

负责账号认证与登录态管理。

- `POST /register`：注册新用户
- `POST /login`：账号密码登录
- `POST /refresh`：使用刷新令牌换取新访问令牌
- `POST /logout`：退出登录，失效 Token
- `GET /me`：获取当前登录用户信息
- `PUT /password`：修改密码

### 8.2 `UserController`  `/api/users`

负责用户资料、统计和搜索。

- `GET /{id}/profile`：查看用户资料
- `PUT /me/profile`：更新自己的资料
- `GET /{id}/stats`：查看用户战绩统计
- `GET /search?keyword=`：按用户名或 ID 搜索用户

### 8.3 `FriendController`  `/api/friends`

负责好友关系、拉黑与备注。

- `GET /`：我的好友列表
- `POST /request`：发送好友申请
- `PUT /{id}/accept`：接受好友申请
- `DELETE /requests/{id}/reject`：拒绝好友申请
- `GET /requests/incoming`：我收到的申请
- `GET /requests/outgoing`：我发出的申请
- `PUT /{id}/remark`：修改好友备注
- `DELETE /{id}`：删除好友或拒绝关系
- `POST /block`：拉黑用户
- `DELETE /block/{id}`：取消拉黑
- `GET /blocked`：我的拉黑列表

### 8.4 `RoomController`  `/api/rooms`

负责组队房间、邀请、部门选择、准备与离开。

- `POST /invites`：邀请好友进房
- `POST /invites/{id}/accept`：接受邀请并创建/加入房间
- `POST /invites/{id}/reject`：拒绝邀请
- `GET /{roomId}`：查询房间详情
- `POST /{roomId}/department`：选择部门
- `POST /{roomId}/ready`：点击准备
- `POST /{roomId}/leave`：离开房间

### 8.5 `MatchController`  `/api/matches`

负责对局查询和战斗行为。

- `GET /{matchId}`：查询对局状态
- `GET /{matchId}/deck`：查询玩家牌堆状态
- `GET /{matchId}/settlement`：查询结算结果
- `POST /{matchId}/choose-first-player`：选择先手
- `POST /{matchId}/reconnect`：重连对局
- `POST /{matchId}/abandon`：放弃对局
- `POST /{matchId}/actions/play-card`：出牌
- `POST /{matchId}/actions/end-turn`：结束回合

### 8.6 `MatchReviveController`  `/api/matches`

负责广告复活 / 复活状态查询。

- `GET /{matchId}/revive/status?userId=`：查询复活状态
- `POST /{matchId}/revive/request`：发起复活请求

### 8.7 `CardController`  `/api/cards`

负责卡牌基础信息查询。

- `GET /`：卡牌列表
- `GET /{cardId}`：卡牌详情
- `GET /{cardId}/effects`：卡牌效果明细

### 8.8 `CustomerController`  `/api/customers`

负责顾客类型查询。

- `GET /current`：当前局顾客类型
- `GET /`：顾客类型列表

### 8.9 `TaskController`  `/api/tasks`

负责任务系统。

- `GET /`：任务列表
- `GET /me`：我的任务进度
- `POST /{userTaskId}/claim`：领取任务奖励

### 8.10 `AchievementController`  `/api/achievements`

负责成就系统。

- `GET /`：成就列表
- `GET /me`：我的成就进度与解锁情况

### 8.11 `LeaderboardController`  `/api/leaderboard`

负责排行榜。

- `GET /`：排行榜分页列表
- `GET /me`：我的排名

### 8.12 `ExampleController`  `/api/examples`

示例接口，主要用于演示基础 CRUD 能力。

- `GET /{id}`：按 ID 查询
- `GET /`：分页查询
- `POST /`：创建
- `DELETE /{id}`：删除

---

## 9. 后端核心业务流程

### 9.1 账号与资料

基本流程：

1. 用户注册
2. 登录得到访问令牌 / 刷新令牌
3. 拉取 `me` 信息
4. 更新个人资料
5. 查询个人战绩与排行榜

### 9.2 好友与拉黑

基本流程：

1. 搜索用户
2. 发送好友申请
3. 接收方接受/拒绝
4. 可设置好友备注
5. 可拉黑 / 取消拉黑

### 9.3 房间与组队

基本流程：

1. 邀请好友进房
2. 对方接受邀请
3. 双方进入同一房间
4. 选择部门
5. 双方准备
6. 自动进入对局

### 9.4 对局与结算

基本流程：

1. 初始化对局
2. 创建双方玩家记录
3. 根据部门固定配置生成 20 张牌
4. 玩家出牌、结束回合
5. 霸凌者自动行动
6. 更新 `matches` / `match_rounds` / `match_players` / `match_cards` / `match_actions`
7. 满足胜负条件后结算
8. 写入战绩、任务、成就、积分等奖励

### 9.5 复活

如果玩家在对局中死亡：

1. 进入可复活状态
2. 前端查询复活状态
3. 发起复活请求
4. 后端恢复血量并记录日志
5. 若超时未复活，则判负并结束对局

---

## 10. 最终版数据库结构说明

数据库最终版文件：

```text
sql_file/wa_demo最终版.sql
```

下面按业务域说明主要表及字段含义。

---

### 10.1 账号与用户资料

#### `users`

用户基础账号表。

关键字段：

- `id`：用户主键
- `username`：登录用户名
- `password_hash`：密码哈希
- `email` / `phone`：邮箱与手机号
- `avatar_url`：头像地址
- `status`：账号状态，1正常 / 0封禁
- `last_login_at`：最近登录时间

#### `user_profiles`

用户扩展资料与战绩统计表。

关键字段：

- `user_id`：关联 `users.id`
- `display_name`：游戏内昵称
- `signature`：个性签名
- `gender`：性别
- `level`：等级
- `exp`：经验
- `win_count`：胜场
- `lose_count`：败场
- `draw_count`：平局或未完成局数
- `money`：游戏内余额 / 奖励

---

### 10.2 好友、备注与社交关系

#### `friendships`

好友关系表，采用**单向记录、双向互通**的设计。

关键字段：

- `user_id`：发起方
- `friend_id`：目标方
- `status`：关系状态
  - `1`：好友
  - `0`：待同意
  - `2`：拉黑
- `remark_name`：单向备注名

说明：

- 只要任一方向存在有效好友记录，前端/后端业务上就认为双方互为好友。

#### `friend_remark`

好友备注表。

关键字段：

- `user_id`：当前用户
- `friend_user_id`：被备注好友
- `remark_name`：备注内容

---

### 10.3 房间与邀请

#### `game_rooms`

组队房间表。

关键字段：

- `room_code`：房间码
- `host_user_id`：房主
- `status`：房间状态
  - `0`：等待中
  - `1`：准备中
  - `2`：对战中
  - `3`：结束
- `player_count`：当前人数
- `max_players`：最大人数
- `match_id`：绑定的对局 ID
- `closed_at`：关闭时间

#### `room_invites`

房间邀请记录表。

关键字段：

- `from_user_id`：邀请发起人
- `to_user_id`：被邀请人
- `room_id`：接受后绑定的房间
- `status`：邀请状态
  - `0`：待处理
  - `1`：已接受
  - `2`：已拒绝
  - `3`：已超时
- `expired_at`：过期时间

#### `room_members`

房间内部成员与准备状态表。

关键字段：

- `room_id`：所属房间
- `user_id`：玩家
- `seat_no`：座位号
- `dept_type`：选择的部门
- `ready_status`：准备状态
- `online_status`：在线状态
- `left_at`：离开时间

---

### 10.4 对局主表与回合

#### `matches`

对局主表，记录一局游戏的整体状态。

关键字段：

- `match_code`：对局唯一编号
- `room_id`：来源房间
- `customer_type_id`：顾客类型
- `bully_id`：霸凌者模板
- `boss_name`：BOSS 名称
- `status`：对局状态
  - `0`：初始化中
  - `1`：进行中
  - `2`：已结束
  - `3`：已中断
- `phase`：当前阶段
  - `INITIALIZING`
  - `SELECT_FIRST_PLAYER`
  - `PLAYER_ACTION`
  - `BOSS_ACTION`
  - `ROUND_SETTLEMENT`
  - `RECONNECT_WAIT`
  - `FINISHED`
- `current_round`：当前回合
- `boss_max_hp` / `boss_current_hp`：霸凌者血量快照
- `boss_base_attack` / `boss_current_attack`：霸凌者攻击快照
- `winner_type`：结果
  - `0`：未结束
  - `1`：玩家胜利
  - `2`：玩家失败
  - `3`：异常中断
- `version`：并发版本号
- `started_at` / `ended_at`：开始与结束时间

#### `match_rounds`

对局内回合进度表。

关键字段：

- `round_no`：回合号
- `first_player_user_id`：本回合先手
- `chosen_by_user_id`：谁选择了先手
- `round_status`：回合状态，0进行中 / 1已结束
- `phase`：回合阶段
- `boss_attack`：本回合霸凌者最终攻击力
- `customer_triggered`：是否触发顾客效果
- `customer_effect_type` / `customer_effect_value`：顾客效果信息
- `boss_rage_value`：怒气值
- `satisfaction_delta`：满意度变化
- `funds_per_player`：每回合玩家资金

---

### 10.5 对局玩家、卡牌与动作流水

#### `match_players`

对局中的玩家战斗快照。

关键字段：

- `match_id`：对局 ID
- `user_id`：玩家 ID
- `seat_no`：座位号
- `dept_type`：实际参赛部门
- `max_hp` / `current_hp`：血量
- `shield`：护盾
- `base_action_points` / `action_points`：行动点
- `ended_turn`：是否结束回合
- `player_status`：战斗状态
- `revive_count` / `revive_limit`：复活次数
- `last_revive_at`：最近复活时间
- `revive_status`：复活状态
- `initial_confidence` / `final_confidence`：兼容字段
- `initial_funds`：每回合初始资金
- `total_funds_used`：累计花费
- `cards_played_count`：出牌总数
- `damage_dealt`：输出总量
- `damage_taken`：承伤总量
- `healing_done`：治疗总量
- `shield_granted`：累计护盾

#### `match_cards`

本局卡牌实例表，记录每一张牌在对局中的区域状态。

关键字段：

- `match_id`：对局 ID
- `match_player_id`：所属对局玩家记录
- `user_id`：卡牌所属用户
- `card_id`：卡牌模板 ID
- `zone`：卡牌所在区域
  - `DECK`
  - `HAND`
  - `DISCARD`
  - `EXHAUST`
- `deck_order`：牌堆顺序
- `drawn_round`：最近一次抽到手牌的回合
- `discarded_round`：最近一次进入弃牌堆的回合
- `version`：卡牌实例版本号

#### `match_actions`

对局动作流水表，是整局战斗的时间线核心。

关键字段：

- `match_id`：对局 ID
- `round_id`：所属回合
- `actor_type`：执行者类型
  - `player`
  - `boss`
  - `system`
- `actor_user_id`：执行者玩家 ID
- `action_type`：动作类型
  - `play_card`
  - `end_turn`
  - `boss_attack`
  - 等
- `card_id`：使用的卡牌模板
- `target_user_id`：动作目标玩家
- `before_value` / `after_value` / `delta_value`：数值变化
- `extra_data`：附加 JSON，例如卡牌实例 ID、倍率、伤害细节等

说明：

- 这是排查战斗过程最重要的表之一
- 可以用来还原一局游戏的完整操作链

#### `match_pending_effects`

待结算 / 延迟效果队列表。

关键字段：

- `match_id`：对局 ID
- `match_player_id`：所属玩家
- `source_user_id`：来源玩家
- `source_card_instance_id`：来源卡牌实例
- `effect_type`：效果类型
- `target_type`：目标类型
- `target_user_id`：目标玩家
- `effect_value`：实际效果数值
- `trigger_round`：触发回合
- `remaining_triggers`：剩余触发次数
- `status`：`PENDING` / `RESOLVED` / `CANCELLED`

#### `match_revive_logs`

广告复活 / 手动复活日志表。

关键字段：

- `match_id`：对局 ID
- `round_no`：回合号
- `user_id`：复活玩家
- `before_hp` / `after_hp`：复活前后血量
- `status`：成功或失败
- `ad_platform`：广告来源
- `ad_request_id`：广告请求 ID
- `ad_callback_raw`：回调原文
- `verify_status`：验证结果
- `revive_reason`：备注

#### `match_replays`

对局回放索引表。

关键字段：

- `match_id`：对局 ID
- `replay_type`：`local` / `cloud`
- `replay_url`：回放地址
- `replay_hash`：完整性校验哈希
- `data_size`：数据大小
- `version`：回放解析版本

---

### 10.6 卡牌基础数据

#### `card_depts`

卡牌部门字典表。

关键字段：

- `dept_code`：部门编码
- `dept_name`：部门名称
- `dept_category`：部门大类，例如 `sales` / `purchase` / `public` / `neutral`
- `sort_no`：排序
- `status`：状态

#### `cards`

卡牌基础信息表。

关键字段：

- `card_code`：卡牌唯一编号
- `card_name`：卡牌名称
- `dept_id`：所属部门
- `dept_type`：部门分类冗余字段
- `cost`：出牌消耗资金
- `card_type`：卡牌类型
- `description`：效果描述
- `image_url`：卡面图片地址
- `combo_card_id`：连携卡 ID
- `is_unique`：是否唯一卡
- `status`：是否启用

#### `card_effects`

卡牌复杂效果配置表。

关键字段：

- `card_id`：归属卡牌
- `effect_order`：结算顺序
- `effect_scope`：目标范围
- `effect_type`：效果类型
- `trigger_timing`：触发时机
- `trigger_delay`：延迟回合
- `remaining_triggers`：可触发次数
- `stack_rule`：叠加规则
- `duration_rounds`：持续回合
- `value`：基础数值
- `target_rule`：目标规则
- `extra_data`：扩展配置

#### `deck_card_configs`

部门固定牌组配置表。

关键字段：

- `dept_type`：部门类型
- `card_id`：卡牌模板
- `card_count`：该牌在 20 张牌组中的数量
- `sort_no`：展示顺序
- `status`：启用状态

说明：

- 这个表决定了**每个部门开局 20 张牌如何构成**
- 后端会严格校验每个部门启用配置的总数必须等于 `20`

---

### 10.7 顾客与霸凌者配置

#### `customer_types`

顾客类型配置表。

关键字段：

- `customer_code`：顾客编码
- `customer_name`：顾客名称
- `description`：背景描述
- `image_url`：图片地址
- `effect_type`：对局效果类型
- `effect_value`：效果数值
- `trigger_chance`：触发概率
- `selection_weight`：随机权重
- `status`：启用状态
- `sort_no`：排序

#### `bullies`

霸凌者模板表。

关键字段：

- `bully_code`：霸凌者编码
- `bully_name`：霸凌者名称
- `description`：说明
- `hp`：初始血量
- `attack_power`：基础攻击
- `defense_value`：基础防御
- `speed_value`：速度
- `rage_value`：怒气初始值
- `skill_data`：技能 JSON
- `status`：启用状态

说明：当前对局所有顾客共用 `BULLY_SHIELD_001`（硬扛恶霸）。点名 / 针对 / 不落单模板停用。实际血量与攻击以 Java `BullyCatalog` 为准（销售+采购、双采购血量 200、攻击 17～20；双销售血量 250、攻击 18～21）。

---

### 10.8 系统配置

#### `system_configs`

系统全局配置表。

关键字段：

- `config_key`：配置键
- `config_value`：配置值
- `config_type`：类型
- `description`：说明
- `status`：启用状态

示例配置：

- `boss.satisfaction.min`
- `boss.satisfaction.max`
- `player.fundsPerRound`
- `combat.mode`
- `combat.customer.count`
- `combat.bully.count`
- `combat.bully.baseHp`

---

### 10.9 任务与成就

#### `tasks`

任务定义表。

关键字段：

- `task_code`：任务编码
- `task_name`：任务名称
- `task_type`：任务类型
- `reset_type`：重置类型
- `period_scope`：统计周期
- `progress_type`：进度类型
- `description`：任务描述
- `condition_type`：完成条件类型
- `condition_value`：条件参数
- `reward_type`：奖励类型
- `reward_value`：奖励内容
- `target_count`：目标次数
- `status`：启用状态

#### `user_tasks`

用户任务进度表。

关键字段：

- `user_id`：用户
- `task_id`：任务
- `period_key`：周期标识
- `progress_value`：当前进度
- `target_value`：目标值快照
- `status`：任务状态
  - `0`：未开始
  - `1`：进行中
  - `2`：已完成
  - `3`：已领取
- `completed_at` / `claimed_at`：完成与领取时间

#### `achievement_defs`

成就定义表。

关键字段：

- `achievement_code`：成就编码
- `achievement_name`：成就名称
- `category`：分类
- `description`：达成条件描述
- `condition_type`：条件类型
- `condition_value`：条件阈值
- `reward_type`：奖励类型
- `reward_value`：奖励内容
- `status`：启用状态

#### `user_achievements`

用户成就明细表。

关键字段：

- `user_id`：用户
- `achievement_id`：成就定义
- `progress_value`：当前进度
- `unlock_status`：是否解锁
- `unlocked_at`：解锁时间
- `claimed_status`：是否领取奖励
- `claimed_at`：领取时间

---

## 11. 关键数据关系

### 11.1 对局链路

```text
users
→ room_members / room_invites
→ game_rooms
→ matches
→ match_rounds
→ match_players
→ match_cards
→ match_actions
→ match_pending_effects
→ match_revive_logs
→ match_replays
```

### 11.2 社交链路

```text
users
→ friendships
→ friend_remark
→ room_invites
```

### 11.3 任务与成长链路

```text
users
→ user_profiles
→ user_tasks
→ tasks
→ user_achievements
→ achievement_defs
```

---

## 12. 业务实现重点总结

### 12.1 好友关系

- 好友关系是单向存储、业务上双向互通
- `friendships.status = 1` 时认为是好友
- `friend_remark` 用于记录单向备注名

### 12.2 房间与对局

- 房间用于组队和准备
- 双方都准备后自动创建对局
- 对局结束后更新房间状态并关闭房间

### 12.3 卡组与出牌

- 每个部门开局会根据 `deck_card_configs` 生成 20 张牌
- `match_cards` 记录本局卡牌实例
- `match_actions` 记录每次出牌、结束回合、霸凌者攻击
- `match_pending_effects` 记录延迟触发效果

### 12.4 胜负结算

- 霸凌者血量降到 0 时，玩家胜利
- 玩家死亡并超时未复活时，玩家失败
- 结算会同步更新战绩、任务、成就和排行榜相关数据

---

## 13. 调试与排查建议

### 13.1 查看数据库是否是最终版

先确认连接的数据库就是：

```text
wa_demo
```

并且导入的是：

```text
sql_file/wa_demo最终版.sql
```

### 13.2 查看对局是否写入流水

可以重点看：

- `matches`
- `match_rounds`
- `match_players`
- `match_cards`
- `match_actions`

### 13.3 查看图片是否能被访问

直接访问后端静态资源，例如：

```text
http://你的局域网IP:8080/images/cards/Card_Carl.png
```

如果这个地址能打开，说明后端静态资源本身没问题。

### 13.4 查看前端接口是否代理正确

前端只代理 `/api`，所以静态资源路径要格外注意。

---

## 14. 推荐的日常开发顺序

1. 启动 Docker 中的 MySQL / Redis / Nacos / RabbitMQ
2. 启动后端 `wa-demo-service`
3. 启动前端 `Game_Frontend`
4. 用浏览器访问局域网 IP 进行联调
5. 用 Navicat / DataGrip 查看数据库
6. 修改配置后重新开新对局验证数据

---

## 15. 一句话总结

这个项目的本质是：

- **MySQL** 保存所有核心业务数据
- **Redis** 负责临时状态、锁和缓存
- **Nacos** 管理配置与发现
- **RabbitMQ** 负责异步通知与事件
- **前端本机运行，后端本机运行，中间件运行在 Docker**
- **最终版数据库以 `sql_file/wa_demo最终版.sql` 为准**
