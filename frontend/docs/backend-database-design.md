# 这单我们护了！！！！ — 后端与数据库设计文档

> 基于游戏规则 3.0 文档，配合前端现有页面结构设计。

---

## 一、技术栈建议

| 层 | 选型 | 说明 |
|---|------|------|
| 运行时 | Node.js 20 LTS | 与前端 Electron 生态一致 |
| 框架 | Express.js + Socket.IO | REST API + 实时通信 |
| 数据库 | PostgreSQL 16 | 强类型、ACID 事务、JSONB 灵活字段 |
| 缓存 | Redis | Session、排行榜、匹配队列、在线状态 |
| ORM | Prisma / Drizzle | 类型安全，与 TypeScript 前端风格统一 |
| 认证 | JWT (access + refresh token) | 无状态，适合 WebSocket |

---

## 二、ER 关系总览

```
users ──1:N──> friends
users ──1:N──> user_achievements ──N:1──> achievements
users ──1:N──> user_quests ──N:1──> quests
users ──1:N──> user_skins ──N:1──> skins
users ──1:N──> game_records (as player1 or player2)
users ──1:N──> matchmaking_queue
game_records ──1:N──> game_turns
game_turns ──1:N──> game_card_plays
cards ──1:N──> game_card_plays
```

---

## 三、数据库表设计

### 3.1 用户与账号

#### `users` — 用户账号

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | 主键 |
| `username` | VARCHAR(32) | UNIQUE, NOT NULL | 昵称 |
| `password_hash` | VARCHAR(255) | NOT NULL | bcrypt 哈希 |
| `email` | VARCHAR(128) | UNIQUE | 邮箱（可选） |
| `avatar` | VARCHAR(255) | DEFAULT '' | 头像 URL |
| `is_online` | BOOLEAN | DEFAULT false | 在线状态（由 Redis 实时同步） |
| `last_login_at` | TIMESTAMPTZ | | 最后登录时间 |
| `created_at` | TIMESTAMPTZ | DEFAULT NOW() | 注册时间 |
| `status` | VARCHAR(16) | DEFAULT 'active' | active / banned |

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(32) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(128) UNIQUE,
    avatar VARCHAR(255) DEFAULT '',
    is_online BOOLEAN DEFAULT false,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    status VARCHAR(16) DEFAULT 'active'
);
```

#### `user_profiles` — 用户游戏档案

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `user_id` | UUID | PK, FK → users.id | 主键 |
| `points` | INT | DEFAULT 0 | 总积分 |
| `total_games` | INT | DEFAULT 0 | 总对局数 |
| `wins` | INT | DEFAULT 0 | 胜场 |
| `losses` | INT | DEFAULT 0 | 败场 |
| `win_streak` | INT | DEFAULT 0 | 当前连胜 |
| `max_win_streak` | INT | DEFAULT 0 | 最高连胜 |
| `current_skin_id` | INT | FK → skins.id | 当前装备皮肤 |
| `level` | INT | DEFAULT 1 | 等级 |
| `exp` | INT | DEFAULT 0 | 经验值 |

```sql
CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    points INT DEFAULT 0,
    total_games INT DEFAULT 0,
    wins INT DEFAULT 0,
    losses INT DEFAULT 0,
    win_streak INT DEFAULT 0,
    max_win_streak INT DEFAULT 0,
    current_skin_id INT,
    level INT DEFAULT 1,
    exp INT DEFAULT 0
);
```

---

### 3.2 好友与公会

#### `friends` — 好友关系

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | UUID | PK | 主键 |
| `user_id` | UUID | FK → users.id | 发起方 |
| `friend_id` | UUID | FK → users.id | 接收方 |
| `status` | VARCHAR(12) | DEFAULT 'pending' | pending / accepted / blocked |
| `created_at` | TIMESTAMPTZ | DEFAULT NOW() | |

```sql
CREATE TABLE friends (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    friend_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(12) DEFAULT 'pending',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, friend_id)
);
```

---

### 3.3 卡牌系统（共 50 张）

#### `cards` — 卡牌定义

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | INT | PK | 主键, 自增 |
| `code` | VARCHAR(6) | UNIQUE, NOT NULL | 编号: S-01~S-06, P-01~P-04, O-01~O-15, L-01~L-02 |
| `name` | VARCHAR(32) | NOT NULL | 卡牌名称 |
| `dept` | VARCHAR(16) | NOT NULL | 部门大类: sales / procurement / other / passerby |
| `sub_dept` | VARCHAR(16) | | 子部门: IT / marketing / admin / logistics / hr |
| `cost` | INT | NOT NULL, CHECK 0-3 | 费用 |
| `effect_type` | VARCHAR(24) | NOT NULL | 效果类型: satisfaction / rage_reduce / confidence_restore / extra_funds / multiplier |
| `effect_value` | INT | NOT NULL | 效果数值 |
| `effect_duration` | INT | DEFAULT 1 | 持续回合数: 1=即时, 2=本回合+下回合, 3=三回合 |
| `effect_target` | VARCHAR(16) | DEFAULT 'self' | 目标: self / ally / all / boss |
| `delayed_value` | INT | DEFAULT 0 | 延迟效果数值（下回合触发） |
| `description` | VARCHAR(200) | | 效果描述文本 |
| `rarity` | VARCHAR(12) | DEFAULT 'common' | common / rare / epic |
| `copy_index` | INT | DEFAULT 1 | 同编号复制索引 1 或 2（路人卡仅 1） |
| `sort_order` | INT | DEFAULT 0 | 排序 |

```sql
CREATE TABLE cards (
    id SERIAL PRIMARY KEY,
    code VARCHAR(6) UNIQUE NOT NULL,
    name VARCHAR(32) NOT NULL,
    dept VARCHAR(16) NOT NULL CHECK (dept IN ('sales', 'procurement', 'other', 'passerby')),
    sub_dept VARCHAR(16) CHECK (sub_dept IN ('IT', 'marketing', 'admin', 'logistics', 'hr')),
    cost INT NOT NULL CHECK (cost >= 0 AND cost <= 3),
    effect_type VARCHAR(24) NOT NULL CHECK (effect_type IN ('satisfaction', 'rage_reduce', 'confidence_restore', 'extra_funds', 'multiplier')),
    effect_value INT NOT NULL,
    effect_duration INT DEFAULT 1,
    effect_target VARCHAR(16) DEFAULT 'self' CHECK (effect_target IN ('self', 'ally', 'all', 'boss')),
    delayed_value INT DEFAULT 0,
    description VARCHAR(200),
    rarity VARCHAR(12) DEFAULT 'common',
    copy_index INT DEFAULT 1 CHECK (copy_index IN (1, 2)),
    sort_order INT DEFAULT 0
);
```

#### 卡牌种子数据（共 54 行 = 27 种 × 2 复制，其中 L 卡仅 2 行）

<details>
<summary><b>销售部（6 种 × 2 = 12 张）</b></summary>

| code | name | dept | cost | effect_type | value | duration | delayed | target | description |
|------|------|------|------|-------------|-------|----------|---------|--------|-------------|
| S-01 | 破冰礼遇 | sales | 0 | satisfaction | 2 | 1 | 0 | boss | 立即+2满意度 |
| S-02 | 尊享矩阵 | sales | 1 | satisfaction | 3 | 1 | 0 | boss | 立即+3满意度 |
| S-03 | 羁绊重塑 | sales | 2 | satisfaction | 3 | 2 | 2 | boss | 本回合+3，下回合+2满意度 |
| S-04 | 盛惠风暴 | sales | 3 | satisfaction | 6 | 1 | 0 | boss | 立即+6满意度 |
| S-05 | 忠诚缔约 | sales | 1 | satisfaction | 2 | 2 | 1 | boss | 本回合+2，下回合+1满意度 |
| S-06 | 巅峰冲刺 | sales | 2 | satisfaction | 4 | 1 | 0 | boss | 立即+4满意度 |

</details>

<details>
<summary><b>采购部（4 种 × 2 = 8 张）</b></summary>

| code | name | dept | cost | effect_type | value | duration | delayed | target | description |
|------|------|------|------|-------------|-------|----------|---------|--------|-------------|
| P-01 | 危机斡旋 | procurement | 0 | rage_reduce | 3 | 1 | 0 | boss | 立即-3怒气值 |
| P-02 | 闪电溯源 | procurement | 1 | rage_reduce | 4 | 1 | 0 | boss | 立即-4怒气值 |
| P-03 | 溢价折冲 | procurement | 2 | rage_reduce | 3 | 2 | 2 | boss | 本回合-3，下回合-2怒气 |
| P-04 | 总裁绿灯 | procurement | 3 | rage_reduce | 3 | 2 | 0 | boss | -3怒气，持续两回合 |

</details>

<details>
<summary><b>其他部门 — IT 部（3 种 × 2 = 6 张）</b></summary>

| code | name | dept | sub | cost | effect_type | value | duration | target | description |
|------|------|------|-----|------|-------------|-------|----------|--------|-------------|
| O-07 | 系统热修复 | other | IT | 0 | confidence_restore | 2 | 1 | self | 恢复2点信心值 |
| O-09 | 算力超频 | other | IT | 1 | confidence_restore | 3 | 1 | self | 恢复3点信心值 |
| O-11 | 权限提级 | other | IT | 2 | confidence_restore | 4 | 1 | self | 恢复4点信心值 |

</details>

<details>
<summary><b>其他部门 — 营销部（3 种 × 2 = 6 张）</b></summary>

| code | name | dept | sub | cost | effect_type | value | duration | delayed | target | description |
|------|------|------|-----|------|-------------|-------|----------|---------|--------|-------------|
| O-08 | 品牌共鸣 | other | marketing | 1 | satisfaction | 3 | 1 | 0 | boss | +3点满意度 |
| O-12 | 破圈引流 | other | marketing | 2 | satisfaction | 4 | 1 | 0 | boss | +4点满意度 |
| O-15 | 全渠道赋能 | other | marketing | 3 | multiplier | 2 | 1 | 0 | self | 本回合你所有满意度/怒气效果×2 |

</details>

<details>
<summary><b>其他部门 — 行政部（3 种 × 2 = 6 张）</b></summary>

| code | name | dept | sub | cost | effect_type | value | duration | delayed | target | description |
|------|------|------|-----|------|-------------|-------|----------|---------|--------|-------------|
| O-01 | 紧急拨付 | other | admin | 1 | extra_funds | 2 | 1 | 0 | self | 本回合额外获得2资金 |
| O-02 | 后勤保障 | other | admin | 0 | extra_funds | 1 | 1 | 1 | self | 下回合额外获得1资金 |
| O-03 | 战略储备 | other | admin | 2 | extra_funds | 1 | 3 | 0 | self | 持续三个回合额外获得1资金 |

</details>

<details>
<summary><b>其他部门 — 物流部（3 种 × 2 = 6 张）</b></summary>

| code | name | dept | sub | cost | effect_type | value | duration | target | description |
|------|------|------|-----|------|-------------|-------|----------|--------|-------------|
| O-10 | 全线空投 | other | logistics | 1 | confidence_restore | 3 | 1 | all | 所有玩家恢复3点信心值 |
| O-13 | 极速调拨 | other | logistics | 0 | confidence_restore | 2 | 1 | all | 所有玩家分别恢复2点信心值 |
| O-14 | 供应链润滑 | other | logistics | 2 | satisfaction | 2 | 2 | boss | 持续2个回合+2满意度 |

</details>

<details>
<summary><b>其他部门 — 人事部（3 种 × 2 = 6 张）</b></summary>

| code | name | dept | sub | cost | effect_type | value | duration | delayed | target | description |
|------|------|------|-----|------|-------------|-------|----------|---------|--------|-------------|
| O-04 | 核心猎聘 | other | hr | 3 | satisfaction | 4 | 2 | 2 | boss | 本回合+4，下回合+2满意度 |
| O-05 | 绩效激励 | other | hr | 1 | satisfaction | 3 | 1 | 0 | boss | 满意度+3 |
| O-06 | 潜能激发 | other | hr | 2 | satisfaction | 4 | 2 | 1 | boss | 本回合+4，下回合+1满意度 |

</details>

<details>
<summary><b>路人部门（2 种 × 1 = 2 张，无复制）</b></summary>

| code | name | dept | cost | effect_type | value | target | description |
|------|------|------|------|-------------|-------|--------|-------------|
| L-01 | 自来水好评 | passerby | 1 | satisfaction | 3 | boss | 加3点满意度 |
| L-02 | 我的发 | passerby | 1 | confidence_restore | 3 | self | 信心+3 |

</details>

---

### 3.4 成就系统

#### `achievements` — 成就定义

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | SERIAL | PK | 主键 |
| `name` | VARCHAR(64) | NOT NULL | 成就名称 |
| `description` | VARCHAR(200) | | 描述 |
| `icon` | VARCHAR(8) | DEFAULT '🏆' | 图标 emoji |
| `category` | VARCHAR(16) | DEFAULT 'general' | general / battle / social / collection |
| `condition_type` | VARCHAR(24) | NOT NULL | wins / games_played / points / satisfaction_max / streak |
| `condition_value` | INT | NOT NULL | 目标值 |
| `reward_points` | INT | DEFAULT 0 | 积分奖励 |
| `sort_order` | INT | DEFAULT 0 | 排序 |

```sql
CREATE TABLE achievements (
    id SERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(200),
    icon VARCHAR(8) DEFAULT '🏆',
    category VARCHAR(16) DEFAULT 'general',
    condition_type VARCHAR(24) NOT NULL,
    condition_value INT NOT NULL,
    reward_points INT DEFAULT 0,
    sort_order INT DEFAULT 0
);
```

#### `user_achievements` — 用户成就进度

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | UUID | PK | 主键 |
| `user_id` | UUID | FK → users.id | |
| `achievement_id` | INT | FK → achievements.id | |
| `progress` | INT | DEFAULT 0 | 当前进度 |
| `is_unlocked` | BOOLEAN | DEFAULT false | 是否解锁 |
| `is_claimed` | BOOLEAN | DEFAULT false | 是否领取奖励 |
| `unlocked_at` | TIMESTAMPTZ | | 解锁时间 |
| `claimed_at` | TIMESTAMPTZ | | 领取时间 |

```sql
CREATE TABLE user_achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_id INT NOT NULL REFERENCES achievements(id) ON DELETE CASCADE,
    progress INT DEFAULT 0,
    is_unlocked BOOLEAN DEFAULT false,
    is_claimed BOOLEAN DEFAULT false,
    unlocked_at TIMESTAMPTZ,
    claimed_at TIMESTAMPTZ,
    UNIQUE(user_id, achievement_id)
);
```

---

### 3.5 任务系统

#### `quests` — 任务定义

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | SERIAL | PK | 主键 |
| `name` | VARCHAR(64) | NOT NULL | 任务名称 |
| `description` | VARCHAR(200) | | 描述 |
| `icon` | VARCHAR(8) | DEFAULT '📋' | 图标 |
| `type` | VARCHAR(8) | NOT NULL | daily / weekly |
| `condition_type` | VARCHAR(24) | NOT NULL | play_games / win_games / play_with_friend / reach_satisfaction |
| `condition_value` | INT | NOT NULL | 目标值 |
| `reward_points` | INT | DEFAULT 0 | 积分奖励 |
| `is_active` | BOOLEAN | DEFAULT true | 是否启用 |
| `sort_order` | INT | DEFAULT 0 | |

```sql
CREATE TABLE quests (
    id SERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(200),
    icon VARCHAR(8) DEFAULT '📋',
    type VARCHAR(8) NOT NULL CHECK (type IN ('daily', 'weekly')),
    condition_type VARCHAR(24) NOT NULL,
    condition_value INT NOT NULL,
    reward_points INT DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    sort_order INT DEFAULT 0
);
```

#### `user_quests` — 用户任务进度

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | UUID | PK | 主键 |
| `user_id` | UUID | FK → users.id | |
| `quest_id` | INT | FK → quests.id | |
| `progress` | INT | DEFAULT 0 | 当前进度 |
| `target` | INT | NOT NULL | 目标值（快照） |
| `is_completed` | BOOLEAN | DEFAULT false | |
| `is_claimed` | BOOLEAN | DEFAULT false | 奖励是否领取 |
| `assigned_at` | TIMESTAMPTZ | DEFAULT NOW() | 分配时间 |
| `expires_at` | TIMESTAMPTZ | | 过期时间 |

```sql
CREATE TABLE user_quests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    quest_id INT NOT NULL REFERENCES quests(id) ON DELETE CASCADE,
    progress INT DEFAULT 0,
    target INT NOT NULL,
    is_completed BOOLEAN DEFAULT false,
    is_claimed BOOLEAN DEFAULT false,
    assigned_at TIMESTAMPTZ DEFAULT NOW(),
    expires_at TIMESTAMPTZ
);
```

---

### 3.6 皮肤系统

#### `skins` — 皮肤定义

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | SERIAL | PK | 主键 |
| `name` | VARCHAR(64) | NOT NULL | 皮肤名称 |
| `description` | VARCHAR(200) | | |
| `type` | VARCHAR(16) | DEFAULT 'card_back' | card_back / theme / effect |
| `price` | INT | DEFAULT 0 | 积分价格 |
| `image_url` | VARCHAR(255) | | 预览图 |
| `is_default` | BOOLEAN | DEFAULT false | 默认皮肤（不可购买，初始拥有） |
| `sort_order` | INT | DEFAULT 0 | |

```sql
CREATE TABLE skins (
    id SERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(200),
    type VARCHAR(16) DEFAULT 'card_back',
    price INT DEFAULT 0,
    image_url VARCHAR(255),
    is_default BOOLEAN DEFAULT false,
    sort_order INT DEFAULT 0
);
```

#### `user_skins` — 用户皮肤

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | UUID | PK | 主键 |
| `user_id` | UUID | FK → users.id | |
| `skin_id` | INT | FK → skins.id | |
| `acquired_at` | TIMESTAMPTZ | DEFAULT NOW() | |
| `is_equipped` | BOOLEAN | DEFAULT false | 是否当前装备 |

```sql
CREATE TABLE user_skins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skin_id INT NOT NULL REFERENCES skins(id) ON DELETE CASCADE,
    acquired_at TIMESTAMPTZ DEFAULT NOW(),
    is_equipped BOOLEAN DEFAULT false,
    UNIQUE(user_id, skin_id)
);
```

---

### 3.7 对局系统（核心）

#### `game_records` — 对局记录

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | UUID | PK | 主键 |
| `room_code` | VARCHAR(8) | NOT NULL | 房间号 |
| `boss_satisfaction_baseline` | INT | NOT NULL | 本局满意度目标值 (80-150 随机) |
| `final_satisfaction` | INT | | 最终满意度 |
| `result` | VARCHAR(8) | | win / lose / draw |
| `turns_played` | INT | DEFAULT 0 | 总局数 |
| `player1_id` | UUID | FK → users.id | |
| `player1_dept` | VARCHAR(16) | | sales / procurement |
| `player1_final_confidence` | INT | | 终局信心值 |
| `player2_id` | UUID | FK → users.id | |
| `player2_dept` | VARCHAR(16) | | sales / procurement |
| `player2_final_confidence` | INT | | 终局信心值 |
| `points_earned_p1` | INT | DEFAULT 0 | P1 获得积分 |
| `points_earned_p2` | INT | DEFAULT 0 | P2 获得积分 |
| `started_at` | TIMESTAMPTZ | DEFAULT NOW() | |
| `ended_at` | TIMESTAMPTZ | | |
| `duration_seconds` | INT | | |

```sql
CREATE TABLE game_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_code VARCHAR(8) NOT NULL,
    boss_satisfaction_baseline INT NOT NULL,
    final_satisfaction INT,
    result VARCHAR(8) CHECK (result IN ('win', 'lose')),
    turns_played INT DEFAULT 0,
    player1_id UUID NOT NULL REFERENCES users(id),
    player1_dept VARCHAR(16) CHECK (player1_dept IN ('sales', 'procurement')),
    player1_final_confidence INT,
    player2_id UUID NOT NULL REFERENCES users(id),
    player2_dept VARCHAR(16) CHECK (player2_dept IN ('sales', 'procurement')),
    player2_final_confidence INT,
    points_earned_p1 INT DEFAULT 0,
    points_earned_p2 INT DEFAULT 0,
    started_at TIMESTAMPTZ DEFAULT NOW(),
    ended_at TIMESTAMPTZ,
    duration_seconds INT
);
```

#### `game_turns` — 回合记录

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | UUID | PK | 主键 |
| `game_record_id` | UUID | FK → game_records.id | |
| `turn_number` | INT | NOT NULL | 回合序号 |
| `boss_satisfaction_before` | INT | NOT NULL | 本回合开始满意度 |
| `boss_rage_before` | INT | NOT NULL | 本回合开始怒气值 |
| `p1_confidence_before` | INT | NOT NULL | P1 回合初信心 |
| `p2_confidence_before` | INT | NOT NULL | P2 回合初信心 |
| `p1_funds` | INT | DEFAULT 3 | P1 本回合资金 |
| `p2_funds` | INT | DEFAULT 3 | P2 本回合资金 |
| `boss_damage` | INT | DEFAULT 0 | BOSS 本回合造成伤害（怒气值） |
| `boss_satisfaction_after` | INT | NOT NULL | 本回合结束满意度 |
| `boss_rage_after` | INT | NOT NULL | 本回合结束怒气值 |
| `p1_confidence_after` | INT | NOT NULL | P1 回合末信心 |
| `p2_confidence_after` | INT | NOT NULL | P2 回合末信心 |
| `pending_effects_json` | JSONB | DEFAULT '[]' | 下回合生效的延迟效果 |

```sql
CREATE TABLE game_turns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_record_id UUID NOT NULL REFERENCES game_records(id) ON DELETE CASCADE,
    turn_number INT NOT NULL,
    boss_satisfaction_before INT NOT NULL,
    boss_rage_before INT NOT NULL,
    p1_confidence_before INT NOT NULL,
    p2_confidence_before INT NOT NULL,
    p1_funds INT DEFAULT 3,
    p2_funds INT DEFAULT 3,
    boss_damage INT DEFAULT 0,
    boss_satisfaction_after INT NOT NULL,
    boss_rage_after INT NOT NULL,
    p1_confidence_after INT NOT NULL,
    p2_confidence_after INT NOT NULL,
    pending_effects_json JSONB DEFAULT '[]'
);
```

#### `game_card_plays` — 出牌记录

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | UUID | PK | 主键 |
| `game_turn_id` | UUID | FK → game_turns.id | |
| `player_id` | UUID | FK → users.id | 出牌玩家 |
| `card_id` | INT | FK → cards.id | 卡牌 |
| `played_at` | TIMESTAMPTZ | DEFAULT NOW() | |
| `satisfaction_change` | INT | DEFAULT 0 | 满意度变化 |
| `rage_change` | INT | DEFAULT 0 | 怒气变化 |
| `confidence_change` | INT | DEFAULT 0 | 信心变化 |
| `funds_change` | INT | DEFAULT 0 | 资金变化 |

```sql
CREATE TABLE game_card_plays (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_turn_id UUID NOT NULL REFERENCES game_turns(id) ON DELETE CASCADE,
    player_id UUID NOT NULL REFERENCES users(id),
    card_id INT NOT NULL REFERENCES cards(id),
    played_at TIMESTAMPTZ DEFAULT NOW(),
    satisfaction_change INT DEFAULT 0,
    rage_change INT DEFAULT 0,
    confidence_change INT DEFAULT 0,
    funds_change INT DEFAULT 0
);
```

#### `active_games` — 进行中的对局（实时状态）

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | UUID | PK | 主键 |
| `game_record_id` | UUID | FK → game_records.id | 关联记录 |
| `room_code` | VARCHAR(8) | NOT NULL | |
| `current_turn` | INT | DEFAULT 1 | 当前回合 |
| `phase` | VARCHAR(20) | DEFAULT 'preparation' | preparation / player_action / boss_settlement / check |
| `current_player_id` | UUID | FK → users.id | 当前行动玩家 |
| `boss_satisfaction` | INT | DEFAULT 0 | 实时满意度 |
| `boss_rage` | INT | DEFAULT 0 | 实时怒气值 |
| `boss_satisfaction_baseline` | INT | NOT NULL | |
| `p1_id` | UUID | FK → users.id | |
| `p1_confidence` | INT | | (销售 15 / 采购 20) |
| `p1_funds_current` | INT | DEFAULT 3 | |
| `p1_hand_json` | JSONB | DEFAULT '[]' | 手牌 [{card_id, code}] |
| `p1_discard_json` | JSONB | DEFAULT '[]' | 弃牌堆 |
| `p1_deck_json` | JSONB | DEFAULT '[]' | 牌堆 |
| `p2_id` | UUID | FK → users.id | |
| `p2_confidence` | INT | | |
| `p2_funds_current` | INT | DEFAULT 3 | |
| `p2_hand_json` | JSONB | DEFAULT '[]' | |
| `p2_discard_json` | JSONB | DEFAULT '[]' | |
| `p2_deck_json` | JSONB | DEFAULT '[]' | |
| `pending_effects_json` | JSONB | DEFAULT '[]' | 全局延迟效果队列 |
| `state_json` | JSONB | DEFAULT '{}' | 扩展状态 |
| `created_at` | TIMESTAMPTZ | DEFAULT NOW() | |
| `updated_at` | TIMESTAMPTZ | DEFAULT NOW() | |

```sql
CREATE TABLE active_games (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_record_id UUID REFERENCES game_records(id),
    room_code VARCHAR(8) NOT NULL,
    current_turn INT DEFAULT 1,
    phase VARCHAR(20) DEFAULT 'preparation',
    current_player_id UUID REFERENCES users(id),
    boss_satisfaction INT DEFAULT 0,
    boss_rage INT DEFAULT 0,
    boss_satisfaction_baseline INT NOT NULL,
    p1_id UUID NOT NULL REFERENCES users(id),
    p1_confidence INT NOT NULL,
    p1_funds_current INT DEFAULT 3,
    p1_hand_json JSONB DEFAULT '[]',
    p1_discard_json JSONB DEFAULT '[]',
    p1_deck_json JSONB DEFAULT '[]',
    p2_id UUID NOT NULL REFERENCES users(id),
    p2_confidence INT NOT NULL,
    p2_funds_current INT DEFAULT 3,
    p2_hand_json JSONB DEFAULT '[]',
    p2_discard_json JSONB DEFAULT '[]',
    p2_deck_json JSONB DEFAULT '[]',
    pending_effects_json JSONB DEFAULT '[]',
    state_json JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

---

### 3.8 排行榜与匹配

#### `leaderboard` — 排行榜缓存（定时刷新）

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | UUID | PK | 主键 |
| `user_id` | UUID | FK → users.id | |
| `points` | INT | NOT NULL | 积分 |
| `rank` | INT | NOT NULL | 排名 |
| `type` | VARCHAR(8) | NOT NULL | total / weekly |
| `period_start` | DATE | | 周期起始 |
| `period_end` | DATE | | 周期结束 |
| `updated_at` | TIMESTAMPTZ | DEFAULT NOW() | |

```sql
CREATE TABLE leaderboard (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    points INT NOT NULL,
    rank INT NOT NULL,
    type VARCHAR(8) NOT NULL CHECK (type IN ('total', 'weekly')),
    period_start DATE,
    period_end DATE,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

#### `matchmaking_queue` — 匹配队列（Redis 为主，PG 为持久备份）

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | UUID | PK | 主键 |
| `user_id` | UUID | FK → users.id (UNIQUE) | |
| `dept_preference` | VARCHAR(16) | | sales / procurement / any |
| `status` | VARCHAR(12) | DEFAULT 'waiting' | waiting / matched / cancelled |
| `created_at` | TIMESTAMPTZ | DEFAULT NOW() | |
| `matched_at` | TIMESTAMPTZ | | |
| `matched_with_id` | UUID | FK → users.id | |

```sql
CREATE TABLE matchmaking_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    dept_preference VARCHAR(16),
    status VARCHAR(12) DEFAULT 'waiting',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    matched_at TIMESTAMPTZ,
    matched_with_id UUID REFERENCES users(id)
);
```

---

## 四、Redis 缓存设计

| Key 模式 | 类型 | 说明 | TTL |
|----------|------|------|-----|
| `session:{userId}` | Hash | 用户 Session（token、在线状态） | 7d |
| `online:{userId}` | String | 在线标记 | 心跳 30s |
| `matchmaking:queue` | Sorted Set | 匹配队列，score=入队时间戳 | — |
| `matchmaking:user:{userId}` | String | 用户匹配状态 | 5min |
| `leaderboard:total` | Sorted Set | 总榜积分排名 | 每小时刷新 |
| `leaderboard:weekly:{weekId}` | Sorted Set | 周榜积分排名 | 当周 |
| `game:{roomCode}` | Hash | 活跃对局快速状态（双写 PG） | 对局结束删除 |
| `rate:{userId}:{action}` | String | 接口限流 | 按需 |
| `quests:daily:{userId}` | Set | 今日已分配任务 ID | 次日 0 点过期 |
| `quests:weekly:{userId}` | Set | 本周已分配任务 ID | 周日 0 点过期 |

---

## 五、REST API 设计

### 5.1 认证 `/api/auth`

| 方法 | 路径 | 说明 | 请求体 |
|------|------|------|--------|
| POST | `/api/auth/register` | 注册 | `{ username, password }` |
| POST | `/api/auth/login` | 登录 | `{ username, password }` → `{ token, refreshToken, user }` |
| POST | `/api/auth/refresh` | 刷新 token | `{ refreshToken }` → `{ token }` |
| POST | `/api/auth/logout` | 登出 | — |
| GET | `/api/auth/me` | 当前用户信息 | — |

### 5.2 用户 `/api/users`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/users/:id/profile` | 获取用户档案（积分、战绩等） |
| PUT | `/api/users/me/profile` | 更新个人资料（头像等） |
| GET | `/api/users/:id/stats` | 获取用户战绩统计 |

### 5.3 好友 `/api/friends`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/friends` | 我的好友列表（含在线状态） |
| POST | `/api/friends/request` | 发送好友申请 `{ friendId }` |
| PUT | `/api/friends/:id/accept` | 接受好友申请 |
| DELETE | `/api/friends/:id` | 删除好友 / 拒绝申请 |

### 5.4 卡牌 `/api/cards`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/cards` | 全部卡牌列表（可按 dept / sub_dept 筛选） |
| GET | `/api/cards/:id` | 单张卡牌详情 |

### 5.5 成就 `/api/achievements`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/achievements` | 全部成就定义 |
| GET | `/api/achievements/me` | 我的成就进度 |
| POST | `/api/achievements/:id/claim` | 领取成就奖励 |

### 5.6 任务 `/api/quests`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/quests` | 全部任务定义 |
| GET | `/api/quests/me` | 我的当日/当周任务进度 |
| POST | `/api/quests/:id/claim` | 领取任务奖励 |

### 5.7 皮肤 `/api/skins`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/skins` | 全部皮肤列表 |
| GET | `/api/skins/me` | 我拥有的皮肤 |
| POST | `/api/skins/:id/purchase` | 购买皮肤 |
| PUT | `/api/skins/:id/equip` | 装备皮肤 |

### 5.8 排行榜 `/api/leaderboard`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/leaderboard?type=total&page=1&limit=20` | 排行榜（total / weekly） |
| GET | `/api/leaderboard/me` | 我的排名 |

### 5.9 对局记录 `/api/games`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/games/history?page=1&limit=10` | 我的对局历史 |
| GET | `/api/games/:id` | 对局详情（含回合明细） |
| GET | `/api/games/:id/replay` | 对局回放数据（用于复盘动画） |

---

## 六、Socket.IO 实时通信

### 6.1 房间事件

| 事件 | 方向 | Payload | 说明 |
|------|------|---------|------|
| `room:create` | C→S | `{ }` | 创建房间，返回 roomCode |
| `room:join` | C→S | `{ roomCode }` | 加入房间 |
| `room:playerJoin` | S→C | `{ player }` | 有新玩家加入 |
| `room:playerLeave` | S→C | `{ playerId }` | 玩家离开 |
| `room:selectDept` | C→S | `{ dept }` | 选择职业（sales/procurement） |
| `room:deptSelected` | S→C | `{ playerId, dept }` | 职业选定广播 |
| `room:toggleReady` | C→S | `{ }` | 准备/取消准备 |
| `room:readyState` | S→C | `{ playerId, ready }` | 准备状态广播 |
| `room:gameStart` | S→C | `{ gameState }` | 双方就绪，通知跳转 Battle |

### 6.2 匹配事件

| 事件 | 方向 | Payload | 说明 |
|------|------|---------|------|
| `match:invite` | C→S | `{ targetUserId }` | 邀请好友 |
| `match:invited` | S→C | `{ fromUser, roomCode }` | 收到邀请 |
| `match:accept` | C→S | `{ roomCode }` | 接受邀请 |
| `match:decline` | C→S | `{ roomCode }` | 拒绝邀请 |
| `match:cancelled` | S→C | `{ }` | 邀请被取消 |
| `match:matched` | S→C | `{ roomCode, teammate }` | 匹配成功 |
| `match:joinQueue` | C→S | `{ deptPreference }` | 加入匹配队列 |
| `match:leaveQueue` | C→S | `{ }` | 离开匹配队列 |

### 6.3 游戏事件

| 事件 | 方向 | Payload | 说明 |
|------|------|---------|------|
| `game:syncState` | S→C | `{ fullGameState }` | 同步全局游戏状态（进入对局/重连） |
| `game:playCard` | C→S | `{ cardId, target? }` | 出牌 |
| `game:cardPlayed` | S→C | `{ playerId, cardId, effects }` | 出牌结果广播 |
| `game:endTurn` | C→S | `{ }` | 玩家结束自己回合 |
| `game:turnStart` | S→C | `{ turn, p1Funds, p2Funds, hands }` | 新回合开始，发牌+发资金 |
| `game:bossAction` | S→C | `{ rage, damage, targetPlayer }` | BOSS 怒气结算 |
| `game:effectApplied` | S→C | `{ effects[], newStates }` | 持续/延迟效果触发 |
| `game:gameOver` | S→C | `{ result, stats }` | 游戏结束通知 |
| `game:cardDrawn` | S→C | `{ playerId, cards[] }` | 抽牌通知 |
| `game:error` | S→C | `{ code, message }` | 操作非法（如费用不足） |

### 6.4 回放事件

| 事件 | 方向 | Payload | 说明 |
|------|------|---------|------|
| `result:gameOver` | S→C | `{ result, stats, pointsEarned }` | 结算数据 |
| `result:replayData` | S→C | `{ turns[], cardPlays[] }` | 回放数据 |

---

## 七、游戏引擎核心逻辑

### 7.1 初始化

```
1. 创建 game_record → 随机生成 bossSatisfactionBaseline (80-150)
2. 构建牌堆：
   - 销售部玩家：从 12 张 sales 牌中随机 6 张 + 从 30 张 other 牌中随机 14 张 = 20 张牌堆
   - 采购部玩家：从 8 张 procurement 牌中随机 4 张 + 从 30 张 other 牌中随机 16 张 = 20 张牌堆
   - 各从牌堆抽 5 张手牌
3. 设置初始信心值：销售 15, 采购 20
4. 每玩家分配 3 资金
5. BOSS 满意度 = 0, 怒气值 = 0
6. 创建 active_game 记录
7. 广播 game:syncState → 双方客户端
```

### 7.2 回合流程

```
Phase 1 — 准备阶段:
  - 每玩家抽牌至 5 张（牌堆不足则洗弃牌堆）
  - 发放 3 资金
  - 触发延迟效果（上回合标记的 "下回合" 效果）
  - 清除到期效果
  - 广播 game:turnStart

Phase 2 — 玩家行动阶段:
  - 当前玩家在资金限制内出牌
  - 校验：职业限制、费用 ≤ 当前资金
  - 即时结算效果（满意度/怒气/信心/资金）
  - 标记延迟效果到 pending_effects_json
  - 玩家点"结束回合" → 未用手牌进入个人弃牌堆
  - 资金清零
  - 切换当前玩家；若双方均结束 → 进入 Phase 3

Phase 3 — BOSS 结算阶段:
  - BOSS 怒气值 = 本回合被降低后的剩余怒气值
  - 若怒气值 > 0：对指定目标玩家造成伤害（扣减信心值）
  - BOSS 默认目标：随机选择一名玩家
  - 信心值不可低于 0
  - 广播 game:bossAction

Phase 4 — 判定阶段:
  - 检查胜利条件：满意度 ≥ baseline → 胜利
  - 检查失败条件：任一玩家信心 ≤ 0 → 失败
  - 若均未触发 → turn+1，进入 Phase 1
  - 若触发 → game:gameOver + 结算积分
```

### 7.3 积分结算

```
胜利: 每人获得 satisfaction 最终值 + 50 额外奖励
失败: 每人获得 satisfaction 最终值（无额外奖励）
括号: 满足度 ≥ baseline 且到达时触发胜利，不与 100 固定值挂钩
```

### 7.4 卡牌效果处理

```typescript
interface PendingEffect {
  type: 'satisfaction' | 'rage_reduce' | 'confidence_restore' | 'extra_funds' | 'multiplier'
  value: number
  targetPlayerId: string      // 哪个玩家触发
  remainingTurns: number      // 剩余持续回合数
  sourceCardCode: string
}
```

- 即时效果（duration=1, delayed=0）：立即执行，不加入 pending
- 延迟效果（delayed>0）：加入 pending，下回合 Phase 1 执行
- 持续效果（duration>1）：加入 pending，每回合 Phase 1 执行一次，remainingTurns-1
- 倍率效果（multiplier）：仅本回合，该玩家所有 satisfaction/rage 效果 ×2

---

## 八、数据库表结构图

```
┌──────────────────────┐       ┌──────────────────────┐
│       users          │       │    user_profiles     │
├──────────────────────┤       ├──────────────────────┤
│ id (PK)         UUID │──┐    │ user_id (PK,FK) UUID │
│ username   VARCHAR(32)│  └───│ points           INT │
│ password_hash VARCHAR │       │ total_games      INT │
│ email      VARCHAR    │       │ wins             INT │
│ avatar     VARCHAR    │       │ losses           INT │
│ is_online  BOOL       │       │ win_streak       INT │
│ last_login_at  TS     │       │ max_win_streak   INT │
│ created_at      TS    │       │ current_skin_id  INT │
│ status     VARCHAR    │       │ level            INT │
└──────────────────────┘       │ exp              INT │
         │                     └──────────────────────┘
         │
         │  ┌──────────────────────────┐
         ├──│         friends          │
         │  ├──────────────────────────┤
         │  │ id (PK)             UUID │
         │  │ user_id (FK)        UUID │──────┐
         │  │ friend_id (FK)      UUID │──────┤ (both → users.id)
         │  │ status          VARCHAR  │      │
         │  │ created_at           TS  │      │
         │  └──────────────────────────┘      │
         │                                     │
         │  ┌──────────────────────────────┐      │
         ├──│     user_achievements        │      │
         │  ├──────────────────────────────┤      │
         │  │ id (PK)                 UUID │      │
         │  │ user_id (FK)            UUID │──────┘
         │  │ achievement_id (FK)      INT │──┐
         │  │ progress                 INT │  │
         │  │ is_unlocked             BOOL │  │
         │  │ is_claimed              BOOL │  │
         │  │ unlocked_at              TS  │  │
         │  │ claimed_at               TS  │  │
         │  └──────────────────────────────┘  │
         │           │                         │
         │           │  ┌──────────────────────┼──┐
         │           └──│    achievements      │  │
         │              ├──────────────────────┤  │
         │              │ id (PK)         INT  │  │
         │              │ name        VARCHAR  │  │
         │              │ description VARCHAR  │  │
         │              │ icon         VARCHAR  │  │
         │              │ category     VARCHAR  │  │
         │              │ condition_type VARCHAR│  │
         │              │ condition_value INT   │  │
         │              │ reward_points  INT   │  │
         │              │ sort_order     INT   │  │
         │              └──────────────────────┘  │
         │                                        │
         │  ┌──────────────────────────────┐      │
         ├──│        user_quests           │      │
         │  ├──────────────────────────────┤      │
         │  │ id (PK)                 UUID │      │
         │  │ user_id (FK)            UUID │──────┘
         │  │ quest_id (FK)            INT │──┐
         │  │ progress                 INT │  │
         │  │ target                   INT │  │
         │  │ is_completed            BOOL │  │
         │  │ is_claimed              BOOL │  │
         │  │ assigned_at              TS  │  │
         │  │ expires_at               TS  │  │
         │  └──────────────────────────────┘  │
         │           │                         │
         │           │  ┌──────────────────────┼──┐
         │           └──│       quests         │  │
         │              ├──────────────────────┤  │
         │              │ id (PK)         INT  │  │
         │              │ name        VARCHAR  │  │
         │              │ description VARCHAR  │  │
         │              │ icon         VARCHAR  │  │
         │              │ type         VARCHAR  │  │
         │              │ condition_type VARCHAR│  │
         │              │ condition_value INT   │  │
         │              │ reward_points  INT   │  │
         │              │ is_active     BOOL   │  │
         │              │ sort_order     INT   │  │
         │              └──────────────────────┘  │
         │                                        │
         │  ┌──────────────────────────────┐      │
         ├──│        user_skins            │      │
         │  ├──────────────────────────────┤      │
         │  │ id (PK)                 UUID │      │
         │  │ user_id (FK)            UUID │──────┘
         │  │ skin_id (FK)             INT │──┐
         │  │ acquired_at              TS  │  │
         │  │ is_equipped             BOOL │  │
         │  └──────────────────────────────┘  │
         │           │                         │
         │           │  ┌──────────────────────┼──┐
         │           └──│       skins          │  │
         │              ├──────────────────────┤  │
         │              │ id (PK)         INT  │  │
         │              │ name        VARCHAR  │  │
         │              │ description VARCHAR  │  │
         │              │ type         VARCHAR  │  │
         │              │ price           INT  │  │
         │              │ image_url    VARCHAR  │  │
         │              │ is_default     BOOL  │  │
         │              │ sort_order      INT  │  │
         │              └──────────────────────┘  │
         │                                        │
         │  ┌──────────────────────────────────────┼──────────┐
         ├──│              game_records           │          │
         │  ├──────────────────────────────────────┤          │
         │  │ id (PK)                         UUID │          │
         │  │ room_code                   VARCHAR │          │
         │  │ boss_satisfaction_baseline     INT  │          │
         │  │ final_satisfaction             INT  │          │
         │  │ result                     VARCHAR  │          │
         │  │ turns_played                   INT  │          │
         │  │ player1_id (FK)               UUID  │──────────┘ (→ users.id)
         │  │ player1_dept              VARCHAR  │
         │  │ player1_final_confidence      INT  │
         │  │ player2_id (FK)               UUID  │──────────┐ (→ users.id)
         │  │ player2_dept              VARCHAR  │          │
         │  │ player2_final_confidence      INT  │          │
         │  │ points_earned_p1              INT  │          │
         │  │ points_earned_p2              INT  │          │
         │  │ started_at                     TS  │          │
         │  │ ended_at                       TS  │          │
         │  │ duration_seconds               INT  │          │
         │  └──────────────────────────────────────┘          │
         │           │                                        │
         │           │  ┌──────────────────────────────┐      │
         │           └──│        game_turns            │      │
         │              ├──────────────────────────────┤      │
         │              │ id (PK)                 UUID │      │
         │              │ game_record_id (FK)     UUID │      │
         │              │ turn_number              INT │      │
         │              │ boss_satisfaction_before INT │      │
         │              │ boss_rage_before         INT │      │
         │              │ p1_confidence_before     INT │      │
         │              │ p2_confidence_before     INT │      │
         │              │ p1_funds                 INT │      │
         │              │ p2_funds                 INT │      │
         │              │ boss_damage              INT │      │
         │              │ boss_satisfaction_after  INT │      │
         │              │ boss_rage_after          INT │      │
         │              │ p1_confidence_after      INT │      │
         │              │ p2_confidence_after      INT │      │
         │              │ pending_effects_json   JSONB │      │
         │              └──────────────────────────────┘      │
         │                    │                               │
         │                    │  ┌──────────────────────────┐ │
         │                    └──│    game_card_plays       │ │
         │                       ├──────────────────────────┤ │
         │                       │ id (PK)             UUID │ │
         │                       │ game_turn_id (FK)   UUID │ │
         │                       │ player_id (FK)      UUID │─┘ (→ users.id)
         │                       │ card_id (FK)         INT │──┐
         │                       │ played_at             TS  │  │
         │                       │ satisfaction_change   INT  │  │
         │                       │ rage_change           INT  │  │
         │                       │ confidence_change     INT  │  │
         │                       │ funds_change          INT  │  │
         │                       └──────────────────────────┘  │
         │                                                     │
         │  ┌──────────────────────────────┐                   │
         ├──│       active_games           │                   │
         │  ├──────────────────────────────┤                   │
         │  │ id (PK)                 UUID │                   │
         │  │ game_record_id (FK)     UUID │                   │
         │  │ room_code            VARCHAR │                   │
         │  │ current_turn             INT │                   │
         │  │ phase                VARCHAR │                   │
         │  │ current_player_id(FK)   UUID │───────────────────┘
         │  │ boss_satisfaction        INT │
         │  │ boss_rage                INT │
         │  │ boss_satisfaction_baseline INT│
         │  │ p1_id (FK)              UUID │───────────────────┐
         │  │ p1_confidence            INT │                   │
         │  │ p1_funds_current         INT │                   │
         │  │ p1_hand_json           JSONB │                   │
         │  │ p1_discard_json        JSONB │                   │
         │  │ p1_deck_json           JSONB │                   │
         │  │ p2_id (FK)              UUID │───────────────────┤ (→ users.id)
         │  │ p2_confidence            INT │                   │
         │  │ p2_funds_current         INT │                   │
         │  │ p2_hand_json           JSONB │                   │
         │  │ p2_discard_json        JSONB │                   │
         │  │ p2_deck_json           JSONB │                   │
         │  │ pending_effects_json   JSONB │                   │
         │  │ state_json             JSONB │                   │
         │  │ created_at                TS │                   │
         │  │ updated_at                TS │                   │
         │  └──────────────────────────────┘                   │
         │                                                     │
         │  ┌──────────────────────────────┐                   │
         ├──│       leaderboard            │                   │
         │  ├──────────────────────────────┤                   │
         │  │ id (PK)                 UUID │                   │
         │  │ user_id (FK)            UUID │───────────────────┘
         │  │ points                   INT │
         │  │ rank                     INT │
         │  │ type                 VARCHAR │
         │  │ period_start            DATE │
         │  │ period_end              DATE │
         │  │ updated_at                TS │
         │  └──────────────────────────────┘
         │
         │  ┌──────────────────────────────┐       ┌──────────────────────┐
         ├──│     matchmaking_queue        │       │        cards         │
         │  ├──────────────────────────────┤       ├──────────────────────┤
         │  │ id (PK)                 UUID │       │ id (PK)         INT  │
         │  │ user_id (FK)            UUID │       │ code        VARCHAR  │
         │  │ dept_preference      VARCHAR │       │ name        VARCHAR  │
         │  │ status               VARCHAR │       │ dept        VARCHAR  │
         │  │ created_at                TS │       │ sub_dept    VARCHAR  │
         │  │ matched_at                TS │       │ cost            INT  │
         │  │ matched_with_id(FK)     UUID │       │ effect_type VARCHAR  │
         │  └──────────────────────────────┘       │ effect_value    INT  │
         │                                         │ effect_duration INT  │
         │  (→ users.id for user_id,               │ effect_target VARCHAR │
         │   matched_with_id)                       │ delayed_value   INT  │
         │                                         │ description  VARCHAR │
         │                                         │ rarity       VARCHAR │
         │                                         │ copy_index       INT │
         │                                         │ sort_order       INT │
         │                                         └──────────────────────┘
         │
         └──── (all user_id / player_id FKs point to users.id)
```

---

## 九、关键数据流

### 9.1 登录 → 进入大厅

```
Client                    Server                     DB/Redis
  │  POST /api/auth/login   │                          │
  │─────────────────────────>│  SELECT users            │
  │                          │─────────────────────────>│
  │                          │  bcrypt.compare()        │
  │                          │  jwt.sign({userId})      │
  │                          │  Redis: session + online │
  │  { token, user }        │                          │
  │<─────────────────────────│                          │
  │                         │                          │
  │  Socket.IO connect      │                          │
  │─────────────────────────>│  Redis: online:{userId} │
  │                         │─────────────────────────>│
  │                         │  批量查询:                │
  │                         │  - friends (含在线状态)   │
  │                         │  - achievements progress  │
  │                         │  - quests progress        │
  │                         │  - points (user_profiles) │
  │  { fullProfile }        │                          │
  │<─────────────────────────│                          │
```

### 9.2 对战流程

```
P1 Client              Server                  P2 Client        DB
  │ room:create           │                       │              │
  │──────────────────────>│ INSERT game_record     │              │
  │                       │─────────────────────────────────────>│
  │ { roomCode }          │                       │              │
  │<──────────────────────│                       │              │
  │                       │                       │              │
  │                       │  room:join            │              │
  │                       │<──────────────────────│              │
  │ room:playerJoin       │  room:playerJoin      │              │
  │<──────────────────────│──────────────────────>│              │
  │                       │                       │              │
  │ room:selectDept('sales')                      │              │
  │──────────────────────>│                       │              │
  │                       │  room:selectDept('procurement')      │
  │                       │<──────────────────────│              │
  │                       │                       │              │
  │ room:toggleReady      │  room:toggleReady     │              │
  │──────────────────────>│<──────────────────────│              │
  │                       │                       │              │
  │                       │ bothReady = true      │              │
  │                       │ 构建牌堆、发牌         │              │
  │                       │ INSERT active_games    │              │
  │                       │─────────────────────────────────────>│
  │  room:gameStart       │  room:gameStart       │              │
  │<──────────────────────│──────────────────────>│              │
  │  (双方跳转 /battle)    │                       │              │
  │                       │                       │              │
  │  game:syncState       │  game:syncState       │              │
  │<──────────────────────│──────────────────────>│              │
```

---

## 十、前后端字段映射（规则 3.0 变更对照）

| 前端当前字段 | 规则 3.0 字段 | 变更说明 |
|-------------|-------------|---------|
| `satisfaction` (max 100) | `满意度` (baseline 80-150) | 上限改为随机，不再固定 100 |
| `playerStamina` (max 100) | `信心值` (销售 15 / 采购 20) | 改名为 confidence，上限大幅降低 |
| `turnFunds` / `teamFunds` (共享) | `资金` (每人 3 点/回合) | 从共享资金池改为每人独立资金 |
| `shield` | `怒气值降低` | 防御机制从护盾改为怒气削减 |
| `bossTarget` / `bossDebuff` | `BOSS怒气伤害` | BOSS 伤害基于怒气值而非固定范围 |
| `attack` 卡牌 | `satisfaction` 效果卡 | 攻击 → 满意度提升 |
| `defend` 卡牌 | `rage_reduce` / `confidence_restore` 效果卡 | 防御 → 降怒/恢复信心 |
| `draw` 卡牌 | `extra_funds` 效果卡 | 过牌 → 额外资金 |
| `consume` 卡牌 | `multiplier` 效果卡 | 消耗品 → 倍率增强 |
| `comboActive` | `multiplier` 效果 | 连击 → 倍率卡触发 |
| 5-14 随机 BOSS 伤害 | BOSS 怒气值 = 本回合扣减后剩余值 | 伤害不再随机，来自怒气结算 |

---

## 十一、部署架构

```
┌──────────────────────────────────────────────────┐
│                   Electron App                    │
│  ┌────────────────  ┌─────────────────────────┐  │
│  │  Renderer (Vue)  │  Renderer (Vue)         │  │
│  │  Player 1        │  Player 2               │  │
│  └──────┬───────────┴──────┬──────────────────┘  │
│         │   Socket.IO       │                     │
└─────────┼───────────────────┼─────────────────────┘
          │                   │
    ┌─────┴───────────────────┴──────┐
    │        Nginx (TLS)              │
    │  /api/* → Express              │
    │  /socket.io/* → Socket.IO      │
    └─────────────┬───────────────────┘
                  │
    ┌─────────────┴───────────────────┐
    │     Node.js Server              │
    │  ┌───────────────────────────┐  │
    │  │  Express REST API         │  │
    │  │  Socket.IO Game Server    │  │
    │  │  Game Engine (回合逻辑)    │  │
    │  └───────────┬───────────────┘  │
    └──────────────┼──────────────────┘
                   │
    ┌──────────────┼──────────────────┐
    │              │                  │
    │  ┌───────────┴──┐  ┌──────────┐ │
    │  │ PostgreSQL   │  │  Redis   │ │
    │  │ (16)         │  │  (7)     │ │
    │  └──────────────┘  └──────────┘ │
    └─────────────────────────────────┘
```
