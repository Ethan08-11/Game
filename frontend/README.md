# 这单我们护了！！！！

> Vue 3 + Electron 桌面级双人协作卡牌对战游戏

## 项目概述

双人合作卡牌对战桌面应用。玩家选择公司部门，组队进入房间，通过卡牌对战击败 BOSS 客户。基于 Electron 构建桌面窗口。

- **技术栈**: Vue 3 + Vite + TypeScript（渲染进程），Electron（桌面壳）
- **UI 库**: Element Plus
- **状态管理**: Pinia（4 模块）
- **游戏画布**: PixiJS 7 + GSAP 动画
- **联机通讯**: Socket.IO 客户端（17 个事件常量已定义，服务端待实现）
- **路由**: Vue Router 4（Hash 模式）
- **样式**: SCSS 设计令牌系统 + Element Plus 主题覆盖
- **API 层**: 集中式 API 封装，Mock/真实后端一键切换

---

## 快速开始

```bash
npm run dev        # 开发模式（Electron 桌面窗口 + HMR）
npm run build      # 生产构建（vue-tsc 类型检查 + Vite 打包）
npm run pack       # 打包为 Windows .exe 安装包
npx vue-tsc --noEmit  # 仅类型检查
```

---

## 目录结构

```
owl-card-game/
│
├── index.html                         # SPA 入口 HTML，挂载点 #app
├── package.json                       # 项目依赖与 npm scripts
├── vite.config.ts                     # Vite 配置：@ 别名、Vue 插件、Electron 插件
├── tsconfig.json                      # TypeScript 根配置
├── tsconfig.app.json                  # 前端 TS 配置：@/* 路径映射、严格模式
├── tsconfig.node.json                 # Node 端 TS 配置
├── electron-builder.yml               # Electron 打包配置（NSIS 安装器，64位 Windows）
├── .gitignore                         # Git 忽略规则
├── CLAUDE.md                          # Claude Code 项目指引文档
├── README.md                          # 本文件
│
├── electron/                          # Electron 主进程
│   └── main.ts                        # 创建 1280×800 窗口，隐藏菜单栏，dev/生产双模式加载
│
├── public/                            # 静态资源（直接复制到 dist/）
│   ├── favicon.svg                    # 应用图标
│   └── icons.svg                      # 图标精灵
│
├── docs/                              # 设计文档
│   └── backend-database-design.md     # 后端数据库设计文档
│
└── src/                               # 渲染进程（Vue 应用主体）
    │
    ├── App.vue                        # 根组件，<router-view /> + 页面过渡动画
    ├── main.ts                        # 应用入口：创建 app → 注册 Pinia/Router/ElementPlus → 挂载
    │
    ├── api/                           # API 请求层（集中管理所有后端接口）
    │   ├── client.ts                  # HTTP 客户端：fetch 封装、Bearer token 注入、Mock 开关
    │   ├── auth.ts                    # 认证 API：login、register、logout
    │   ├── user.ts                    # 用户 API：好友、积分、成就、排行榜
    │   ├── game.ts                    # 游戏 API：卡牌列表、游戏配置、部门列表、结算提交
    │   ├── social.ts                  # 社交/商城 API：皮肤商店、任务系统
    │   ├── mock.ts                    # 集中式 Mock 数据（卡牌引用 cardData.ts、好友、排行榜、皮肤、任务等）
    │   └── index.ts                   # 统一导出入口
    │
    ├── router/                        # 路由配置
    │   └── index.ts                   # 11 条路由 + 导航守卫（/cg、/login 免验，其余需 token）
    │
    ├── store/                         # Pinia 状态管理
    │   ├── user.ts                    # 用户认证、好友、积分、成就（异步 API 调用）
    │   ├── room.ts                    # 房间管理：玩家列表、部门选择、准备状态、邀请匹配
    │   ├── game.ts                    # 对战核心：体力、满意度、团队资金、牌堆、BOSS、胜负判定
    │   └── common.ts                  # 全局 UI：弹窗、加载动画、CG 播放状态
    │
    ├── views/                         # 11 个页面组件
    │   ├── CgPage/index.vue           # 开场 CG 视频播放 → 点击跳过/进入登录
    │   ├── LoginPage/index.vue        # 登录/注册双标签页，异步 API 调用
    │   ├── GameHall/index.vue         # 游戏大厅：顶部导航、好友侧边栏、开始游戏入口
    │   ├── MatchMaking/index.vue      # 匹配组队：在线好友列表 + 双人房间槽位 + 准备机制
    │   ├── BattlePage/index.vue       # 对战主界面：玩家体力、BOSS 满意度、手牌操作、回合制
    │   ├── ResultPage/index.vue       # 结算复盘：胜负结果、数据面板、积分奖励
    │   ├── Achievements/index.vue     # 成就系统：已解锁/未解锁成就网格展示
    │   ├── PointsPage/index.vue       # 积分页面：当前积分总量展示
    │   ├── Leaderboard/index.vue      # 排行榜：总榜/周榜切换，API 数据加载
    │   ├── SkinsPage/index.vue        # 皮肤商店：卡背皮肤浏览与解锁
    │   └── QuestsPage/index.vue       # 任务系统：每日/每周任务及进度条
    │
    ├── components/                    # 全局复用组件
    │   ├── BackButton.vue             # 通用返回按钮：SVG 箭头图标插槽 + 路由跳转 + 可选确认
    │   ├── CardItem.vue               # 卡牌展示：费用、名称、类型、效果值、禁用态
    │   ├── OwlDialog.vue              # 全局弹窗（Teleport to body）：tip/confirm/warning
    │   ├── PlayerInfo.vue             # 玩家信息面板：部门图标 + 体力值 + 进度条
    │   ├── PixiGame.vue               # PixiJS 画布封装：shop/battle/overlay 三层容器
    │   ├── CountDown.vue              # 5 分钟倒计时，<30s 红色脉冲，超时触发
    │   ├── FriendPanel.vue            # 可展开好友列表面板：在线/离线分组
    │   └── ResourceBar.vue            # 对战顶部资源条：资金/P1体力/P2体力/满意度
    │
    ├── utils/                         # 工具模块
    │   ├── cardData.ts                # Card 接口定义 + 18 张卡牌数据 + getCardsByDept() / getCardById()
    │   ├── gameCalc.ts                # 满意度计算、护盾抵扣、胜负判定、积分计算、回合资金
    │   ├── randomTool.ts              # 洗牌算法、随机部门、BOSS 随机攻击目标
    │   ├── socket.ts                  # Socket.IO 客户端封装 + 17 个事件常量（4 组）
    │   └── iconMap.ts                 # 语义图标映射：Emoji → Element Plus 图标组件，含 getIcon/getSkinIcon
    │
    ├── style/                         # 全局样式
    │   ├── tokens.scss                # CSS 设计令牌：瑞士水疗极简美学（颜色、间距、排版、圆角、阴影、过渡）
    │   ├── element-overrides.scss     # Element Plus 主题覆盖（自动继承调色板）
    │   ├── global.scss                # 全局重置、动画关键帧、弹窗/遮罩基础样式
    │   └── responsive.scss            # 响应式 Mixin（mobile/tablet/desktop 断点）
    │
    └── assets/                        # 静态资源（全部本地，无 CDN）
        ├── cg/                        # 开场 CG 视频（opening.mp4）
        ├── card/                      # 卡牌图片（按 8 部门分文件夹）
        │   ├── finance/               # 财务部
        │   ├── hr/                    # 人事部
        │   ├── purchase/              # 采购部
        │   ├── sales/                 # 销售部
        │   ├── it/                    # IT 部
        │   ├── design/                # 设计部
        │   ├── admin/                 # 行政部
        │   └── logistics/             # 物流部
        ├── boss/                      # BOSS 立绘、攻击特效
        ├── ui/                        # 按钮、弹窗、大厅背景、部门图标
        └── common/                    # 全局图标、配色背景
```

---

## 页面流转

```
CG → 登录 → 游戏大厅 → 匹配组队 → 双人对战 → 结算复盘
/cg   /login  /game-hall  /matchmaking  /battle    /result

游戏大厅可跳转到子页面：
  /achievements  成就    /points  积分    /leaderboard  排行榜
  /skins         皮肤    /quests  任务
```

| 步骤 | 触发条件 | 目标页面 |
|---|---|---|
| 启动 | 自动 | `/cg` 开场 CG |
| CG 结束/跳过 | 视频播完或点击跳过 | `/login` 登录注册 |
| 登录/注册成功 | API 异步校验通过，token 存 localStorage | `/game-hall` 游戏大厅 |
| 开始游戏 | 大厅点击「开始游戏」 | `/matchmaking` 匹配组队 |
| 邀请好友 + 准备 | 2 人房间 + 双方点击准备 | `/battle` 双人对战 |
| 对局结束 | 满意度 100（胜利）或双方体力 0（失败） | `/result` 结算复盘 |
| 返回大厅 | 结算页点击「返回大厅」或通用返回按钮 | `/game-hall` |

**路由守卫**: `/cg` 和 `/login` 标记 `noAuth: true`，无需登录；其余页面检查 localStorage 中 `token`，缺失则强制跳转 `/login`。

---

## 每个文件的内容说明

### src/api/ — API 请求层

| 文件 | 内容 |
|---|---|
| `client.ts` | 通用 HTTP 客户端。包含 `USE_MOCK` 全局开关、`BASE_URL` 配置、Bearer token 自动注入、`apiCall<T>()` 通用请求封装（Mock 模式下返回 mock 数据并模拟延迟，真实模式下发 fetch 请求） |
| `auth.ts` | 认证接口：`login()`、`register()` 返回 token 和用户信息；`logout()` 通知服务端 |
| `user.ts` | 用户接口：`getFriends()`、`addFriend()` 管理好友；`getPoints()`、`addPoints()` 积分操作；`getAchievements()`、`unlockAchievement()` 成就系统；`getLeaderboard()` 排行榜（支持 total/weekly 类型） |
| `game.ts` | 游戏接口：`getAllCards()` 获取全部卡牌；`getCardsByDept()` 按部门筛选；`getGameConfig()` 获取回合资金/体力等配置；`getDepartments()` 部门列表；`submitGameResult()` 提交对战结算 |
| `social.ts` | 商城接口：`getSkins()`、`purchaseSkin()` 皮肤商店；`getQuests()`、`claimQuestReward()` 任务系统 |
| `mock.ts` | 所有 Mock 数据集中存储：卡牌数据引用自 `cardData.ts`、5 个好友、排行榜数据、6 个皮肤、5 个任务、游戏配置、8 个部门列表 |
| `index.ts` | 统一导出所有 API 函数和类型，业务代码只需 `import { xxx } from '@/api'` |

### src/router/ — 路由

| 文件 | 内容 |
|---|---|
| `index.ts` | 11 条路由定义（含 meta.title 和 meta.noAuth），createWebHashHistory 模式，`beforeEach` 导航守卫检查 token |

### src/store/ — 状态管理

| 文件 | 内容 |
|---|---|
| `user.ts` | `token`、`username`、`avatar`、`friends[]`、`points`、`achievements[]`。computed: `isLoggedIn`。异步方法：`login()`、`register()` 调用 API 获取 token；`loadFriends()`、`addPoints()`、`loadAchievements()` 从 API 加载数据；`logout()` 清除本地状态 |
| `room.ts` | `roomCode`、`players[]`、`isConnected`、`player1Dept/player2Dept`、`player1Ready/player2Ready`、`inviteId`、`invitedBy`、`matchRoomId`、`isHost`。computed: `bothReady`、`canStart`。方法：房间创建/加入、玩家同步、部门选择、准备切换、邀请发送/接收/接受/拒绝 |
| `game.ts` | 对战全部状态：`teamFunds`、`satisfaction`(0→100)、`turnFunds/currentTurnFunds`、`pointsEarned`、`player1Stamina/player2Stamina`、`deck/hand/discardPile/removedCards`、`shield`、`bossTarget/Debuff`、`comboActive`、`isVictory/isGameOver`。方法：`loadGameConfig()`、`startTurn()`、`spendFunds()`、`addSatisfaction()`、`damagePlayer()`、`resetGame()`、`submitResult()` |
| `common.ts` | 全局 UI 状态：`cgPlaying`、`dialogVisible/Type/Message`、`loadingVisible`。方法：`showDialog()`、`hideDialog()`、`showLoading()`、`hideLoading()` |

### src/views/ — 页面

| 文件 | 内容 |
|---|---|
| `CgPage/index.vue` | 全屏 CG 视频播放，加载失败则显示渐变背景 + 标题。跳过按钮 → `/login` |
| `LoginPage/index.vue` | 居中登录卡片，登录/注册两个标签页。表单校验后异步调用 `user.login/register`，含 loading 和错误提示 |
| `GameHall/index.vue` | 顶部：模式标签 + 成就/积分/排行榜导航 + 用户头像 + 退出按钮。左侧：FriendPanel。中间：圆形开始按钮 + 皮肤/任务入口。底部：历史复盘按钮 |
| `MatchMaking/index.vue` | 左侧：在线好友列表（从 API 加载），每人有 + 邀请按钮。右侧：双人房间槽位 + 准备按钮。双方准备后自动跳转对战 |
| `BattlePage/index.vue` | 左侧：两名 PlayerInfo。中间：回合数 + 实时行动日志 + 游戏结束遮罩。右侧：BOSS 卡牌（满意度条、护盾、攻击目标）。底部：牌库/手牌区/弃牌堆 + 资金显示 + 结束回合/切换玩家按钮。卡牌从 API 加载，含离线降级卡组 |
| `ResultPage/index.vue` | 胜负标题、数据面板（回合数/满意度/体力/积分）、复盘/返回大厅/重新组队按钮。积分通过 `submitResult()` 提交后端 |
| `Achievements/index.vue` | 成就网格：已解锁显示时间，未解锁半透明。数据从 `user.achievements` 加载 |
| `PointsPage/index.vue` | 大字体积分总量 + 说明文字。数据从 `user.points` 加载 |
| `Leaderboard/index.vue` | 总榜/周榜切换标签，排名列表（名次/用户名/积分）。数据从 `getLeaderboard()` API 加载 |
| `SkinsPage/index.vue` | 皮肤卡片网格：预览图标、名称、价格/已拥有状态。数据从 `getSkins()` API 加载 |
| `QuestsPage/index.vue` | 任务列表：图标、名称、描述、进度条、奖励。已完成任务左侧绿色边框。数据从 `getQuests()` API 加载 |

### src/components/ — 组件

| 文件 | 内容 |
|---|---|
| `BackButton.vue` | 通用返回按钮。Props: `to`(目标路由)、`text`(按钮文字)。Slots: `icon`(默认 SVG 左箭头)、`default`(文字内容)。绝对定位左上角，半透明毛玻璃样式，hover 高亮。`to` 为空时可配合 `@click` 事件自定义行为 |
| `CardItem.vue` | 单张卡牌展示。Props: `name`、`dept`、`cost`、`type`、`satisfaction`、`shield`、`disabled`。根据 type 显示不同底色标签（攻击红/防御蓝/过牌绿/消耗灰），费用不足时置灰 |
| `OwlDialog.vue` | 全局模态弹窗，Teleported 到 body。根据 `common.dialogType` 显示不同标题（tip→提示/confirm→确认/warning→警告），confirm 类型额外显示取消按钮 |
| `PlayerInfo.vue` | 玩家面板：圆形部门头像 + 体力数值 + 进度条。体力 ≤20 红色，≤50 黄色 |
| `PixiGame.vue` | PixiJS Application 封装。三层 Container：shopLayer（战前商店）、battleLayer（回合对战动画）、overlayLayer（结算/弹窗特效）。监听 gameStore 自动触发画布刷新 |
| `CountDown.vue` | 5 分钟倒计时组件，<30 秒红色脉冲动画，归零时触发 `@timeout` 事件 |
| `FriendPanel.vue` | 可展开的侧边栏好友面板。分组显示在线/离线好友，每人显示头像+用户名+部门 |
| `ResourceBar.vue` | 对战页顶部信息条：当前资金、玩家 1 体力、玩家 2 体力、BOSS 满意度进度条 |

### src/utils/ — 工具

| 文件 | 内容 |
|---|---|
| `cardData.ts` | `Card` 接口定义（id/name/dept/cost/type/satisfaction/shield/description/comboCardId）。18 张卡牌数据（销售6+采购6+财务3+物流3，待扩充至 52 张）。`getCardsByDept()` 按部门筛选卡牌。`getCardById()` 按 ID 查找单张卡牌 |
| `gameCalc.ts` | 5 个纯函数：`calcSatisfactionChange()` 满意度计算（联动翻倍）、`calcShieldDeflect()` 护盾伤害抵扣、`checkWinCondition()` 胜负判定、`calcPointsFromSatisfaction()` 积分奖励计算、`getTurnFunds()` 每回合可用资金 |
| `randomTool.ts` | `shuffle()` Fisher-Yates 洗牌算法、`randomDept()` 随机分配部门、`randomTarget()` 50% 概率选择 BOSS 攻击目标（player1/player2） |
| `socket.ts` | Socket.IO 客户端单例封装。`connectSocket(url)` 建立连接（自动重连最多 10 次）、`getSocket()` 获取实例、`disconnectSocket()` 断开。17 个事件常量分 4 组：ROOM_EVENTS(4)、GAME_EVENTS(5)、RESULT_EVENTS(2)、MATCH_EVENTS(6) |
| `iconMap.ts` | 语义图标映射系统。`iconMap` 对象将语义名称映射到 Element Plus 图标组件；`getIcon(name)` 按名称获取图标；`getIconName(emoji)` 将 Emoji 转为语义名称；`getSkinIcon(name)` 获取皮肤图标（含降级默认值） |

### src/style/ — 样式

| 文件 | 内容 |
|---|---|
| `tokens.scss` | `:root` 下 CSS 自定义属性（瑞士水疗极简美学）：暖调深色背景（`--color-bg-base` 等）、黄铜金点缀色（`--color-accent`）、语义色、8 部门色、4 卡牌类型色、12 级间距（4px 基准）、完整排版系统、圆角、阴影、过渡、布局变量 |
| `element-overrides.scss` | Element Plus 主题变量覆盖：主色、语义色、背景、文字、圆角、输入框、禁用态等全部映射到 tokens.scss 的设计令牌 |
| `global.scss` | CSS reset（margin/padding/box-sizing）、`#app` 全屏占满、`cardFlipIn`/`cardShake`/`fadeIn`/`slideUp` 关键帧动画、`.owl-overlay`/`.owl-dialog` 弹窗基础样式、滚动条美化 |
| `responsive.scss` | 响应式断点 Mixin：`mobile`(≤767px)、`tablet`(768-1023px)、`desktop`(≥1024px)、`not-desktop`(≤1023px) |

### src/assets/ — 静态资源

| 目录 | 内容 |
|---|---|
| `cg/` | 开场 CG 视频 `opening.mp4` |
| `card/{dept}/` | 8 个部门各自的卡牌图片（待添加），按 `{部门}_{卡牌名称}.png` 命名 |
| `boss/` | BOSS 客户立绘、攻击特效帧动画（待添加） |
| `ui/` | 按钮背景、弹窗边框、大厅底图、部门选择图标（待添加） |
| `common/` | 全局图标、loading 动画、默认头像（待添加） |

### 其他根目录文件

| 文件 | 内容 |
|---|---|
| `index.html` | HTML 入口，`<div id="app">` 挂载点，标题"这单我们护了！！！！" |
| `package.json` | 项目元信息、依赖列表、`dev/build/pack` 三个 npm scripts |
| `vite.config.ts` | Vite 构建配置：`@` → `src/` 别名、Vue 插件、Electron 插件（vite-plugin-electron） |
| `tsconfig.json` | TypeScript 根配置，引用 `tsconfig.app.json` 和 `tsconfig.node.json` |
| `tsconfig.app.json` | 前端 TS 配置：`@/*` 路径映射、`strict: true`、包含 `src/**/*` |
| `tsconfig.node.json` | Node 端 TS 配置，仅覆盖 `vite.config.ts` 和 `electron/` |
| `electron-builder.yml` | Electron Builder 打包配置：appId、NSIS 安装器、64 位 Windows、输出 `release/` |
| `.gitignore` | 忽略 `node_modules/`、`dist/`、`dist-electron/`、`release/`、日志文件 |
| `CLAUDE.md` | Claude Code 项目指引：架构概述、命令、路由流程、Store 详情、战斗机制、TODO 清单 |

---

## 路由页面流转图

```
                                 ┌──────────────┐
                                 │   成就       │ ← /achievements
                                 ├──────────────┤
                                 │   积分       │ ← /points
    ┌──────┐   ┌───────┐   ┌────┴──────────┐   ┌────┴───────┐   ┌────────┐   ┌────────┐
    │  CG  │ → │ 登录  │ → │   游戏大厅     │ → │  匹配组队   │ → │ 对战   │ → │ 结算   │
    │ /cg  │   │/login │   │  /game-hall    │   │/matchmaking │   │/battle │   │/result │
    └──────┘   └───────┘   └────┬──────────┘   └─────────────┘   └────────┘   └────────┘
                                 │
                                 ├──────────────┐
                                 │   排行榜     │ ← /leaderboard
                                 ├──────────────┤
                                 │   皮肤商店   │ ← /skins
                                 ├──────────────┤
                                 │   任务       │ ← /quests
                                 └──────────────┘
```

---

## API 对接说明

所有后端接口集中在 `src/api/` 目录。对接真实后端只需两步：

1. **关闭 Mock**：修改 `src/api/client.ts`
   ```ts
   const USE_MOCK = false
   const BASE_URL = 'https://your-server.com/api'
   ```
2. **调整端点路径**（如不一致）：在各模块文件中修改 `apiCall()` 第一个参数

详见各模块文件顶部注释。业务代码通过 `import { xxx } from '@/api'` 调用，函数签名不变。

---

## 待完成事项

- [ ] `utils/cardData.ts` 中卡牌从 18 张扩充至 52 张完整数据
- [ ] 全部卡牌/UI 静态素材（卡牌图、BOSS 立绘、UI 图标）
- [ ] PixiJS 画布完整动画（出牌特效、护盾、满意度跳动）
- [ ] Socket.IO 服务端实现（客户端事件和常量已定义）
- [ ] IndexedDB 对局回放缓存
- [ ] 皮肤商店购买逻辑
- [ ] 任务奖励领取逻辑
