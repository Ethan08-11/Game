# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

这单我们护了！！！！ — a 2-player co-op card battle game as an Electron desktop app. Two players team up by inviting a friend from the matchmaking page, pick departments, ready up, and battle a boss client through card plays. Satisfaction accumulates to 100 to win; both players' stamina hitting 0 means defeat.

- **Stack:** Vue 3 + Vite + TypeScript (renderer), Electron (desktop shell)
- **UI:** Element Plus
- **State:** Pinia (4 modules: user, room, game, common)
- **Canvas:** PixiJS 7; GSAP for animations
- **Real-time:** Raw WebSocket (`src/utils/roomSocket.ts`) — heartbeat, auto-reconnect, pub/sub event system. Server at `ws://192.168.1.25:8080/ws/room`
- **HTTP:** Custom fetch-based client (`src/api/client.ts`) — auto token refresh on 401, 10s timeout, error normalization
- **Routing:** Vue Router 4, hash history
- **Styles:** SCSS via sass-embedded; design tokens in `src/style/tokens.scss`

**Note:** `src/utils/socket.ts` (Socket.IO wrapper + event constants) exists but is **not imported anywhere** — legacy/speculative code.

## Commands

```bash
npm run dev       # Start Electron dev mode (HMR + desktop window)
npm run build     # vue-tsc -b type-check + Vite production build → dist/ + dist-electron/
npm run pack      # Build + electron-builder → .exe installer (output: release/)
```

Type-check without building: `npx vue-tsc --noEmit`

## Architecture

### Electron shell

`electron/main.ts` — 1280×800 window (min 1024×680), `setMenuBarVisibility(false)`, `nodeIntegration: false`, `contextIsolation: true`. Dev loads `localhost:5173`; production loads `dist/index.html`. Built by `vite-plugin-electron` → `dist-electron/main.js`.

### Vite config

- Proxy: `/api` → `http://192.168.1.25:8080`
- Alias: `@` → `src/`
- Bundler: Rolldown (Vite 8.x default)

### Environment variables

```
VITE_API_BASE=/api                          # HTTP API prefix
VITE_ROOM_WS_BASE=ws://192.168.1.25:8080/ws/room  # WebSocket endpoint
```

### App.vue — central WebSocket event hub

`App.vue` is **not just a shell**. It's the central event bus that:

- Connects the room WebSocket on mount and reconnects when `user.token` changes (via `watch`)
- Subscribes to 11+ WebSocket events: `room.invite.*`, `room.created`, `room.closed`, `match.started`, `match.ended`, `friend.presence.changed`, `ws.heartbeat.ack`, `ws.connected`
- Orchestrates global flows: invite dialog, room lifecycle sync, match start/end navigation, friend presence updates
- Runs a 30-second `refreshFriends()` polling fallback
- Handles `beforeunload` to cleanly leave rooms via `fetch` keepalive + disconnect WebSocket

### Routes (15 + 1 redirect)

```
/ → redirect to /cg
/cg → CgPage (noAuth)
/login → LoginPage (noAuth)
/game-hall → GameHall
/customer-current → CustomerCurrent
/customer-intro → CustomerIntro
/matchmaking → MatchMaking
/battle → BattlePage
/result → ResultPage
/achievements → Achievements
/points → PointsPage
/leaderboard → Leaderboard
/skins → SkinsPage
/quests → QuestsPage
/cards → CardsPage
/rules → RulesPage
```

Router guard (`beforeEach`): routes without `meta.noAuth` require `token` in localStorage, or redirect to `/login`.

### WebSocket layer (`roomSocket.ts`)

Singleton raw WebSocket with:

- **Heartbeat**: sends `ws.heartbeat` every 20s, expects `ws.heartbeat.ack`
- **Auto-reconnect**: 3s delay on unexpected close; suppressed when `manuallyClosed = true`
- **Pub/sub**: `subscribeRoomEvent(type, handler)` returns an unsubscribe function. Handlers stored in a module-level `Map<string, Set<Handler>>`
- **Event naming**: dotted prefix — `room.*`, `match.*`, `friend.*`, `ws.*`
- **Token**: passed as `?accessToken=` query param on connect; cached in module-scoped `connectedToken` for reconnect

Key gotcha: `connectedToken` is only updated on explicit `connectRoomSocket()` calls. The HTTP layer's auto-refresh updates `localStorage` but not this variable. The Pinia `user.token` watcher in App.vue triggers reconnection with fresh token.

### API layer

```
src/api/
├── client.ts    # fetch wrapper: auto 401 refresh, 10s timeout, error mapping
├── auth.ts      # login, register, refreshAuth, logout, getMe
├── user.ts      # friends, points, achievements, profile, leaderboard
├── room.ts      # room CRUD, invites (send/accept/reject), department, first-player
├── game.ts      # game config, submit result
├── match.ts     # match detail, deck, play-card, end-turn, reconnect, abandon
├── social.ts    # social features
├── mock.ts      # complete mock data (remove when backend is ready)
└── index.ts     # barrel exports
```

**Token refresh**: on 401, `client.ts` refreshes the token via `/auth/refresh`, retries the request once, and writes the new token to `localStorage`. A `refreshPromise` lock prevents concurrent refreshes.

### Pinia stores

| Store | Key responsibility |
|-------|-------------------|
| `user` | Auth (token/userId/username), friends (with `PresenceStatus`), points, money, achievements, profile, stats. Has `fallbackLogin()` for offline dev. |
| `room` | Room lifecycle: roomId, players, departments, ready states, invite flow, matchId, `isConnected` (WebSocket health). |
| `game` | Battle state: satisfaction, stamina, funds, hand/deck/discard, boss HP, shield, combo, turn management. |
| `common` | Global UI state: dialog visibility/type/message, loading, CG playback. |

### Friend presence system

Four statuses: `OFFLINE` > `IN_MATCH` > `IN_ROOM` > `IDLE` (server-enforced priority).

- **Server computes** status from: Redis WebSocket Presence + DB room membership + DB match state
- **Real-time push**: `friend.presence.changed` WebSocket event → `App.vue` → `user.updateFriendPresence()`
- **Polling fallback**: 30s `refreshFriends()` via REST `GET /api/friends`
- **Login flow**: must call `connectRoomSocket(user.token)` (not `user.userId`!) to establish Presence
- **Logout flow**: must call `leaveRoom` API **before** `disconnectRoomSocket()` — otherwise stale room/match data on server causes next login to show `IN_MATCH` instead of `IDLE`
- **Window close**: `beforeunload` handler sends `fetch(..., { keepalive: true })` to `POST /rooms/{id}/leave`

### Key components

| Component | Purpose |
|-----------|---------|
| `BackButton` | Navigation back button used on most pages |
| `CardItem` | Card display: cost, name, type tag, effect values. Emits `@play`. Disabled when cost > funds. |
| `PlayerInfo` | Department icon + stamina bar. Red ≤20, yellow ≤50. |
| `ResourceBar` | Battle top bar: funds, P1/P2 stamina, satisfaction progress. |
| `FriendPanel` | Expandable sidebar with friend list + presence status icons. |
| `OwlDialog` | Global modal (tip/confirm/warning), teleported to body, driven by `common` store. |
| `PixiGame` | PixiJS Application wrapper with 3 Container layers (shop/battle/overlay). |
| `CountDown` | 5-min countdown, red pulse below 30s, emits `@timeout`. |
| `EmployerCard` | Employer/customer trait display on battle stage. |
| `BullyCard` | Boss HP bar + attack info display. |

### Style system

- `tokens.scss` — 125+ CSS custom properties: colors, spacing (4px grid), typography (minor-second scale), shadows, transitions
- `fonts.scss` — Self-hosted "HuiWen MingChao" (Chinese) and "Coldiac Rough" (English)
- `element-overrides.scss` — Maps Element Plus CSS variables to design tokens
- `global.scss` — Resets, scrollbars, `cardFlipIn`/`cardShake`/`fadeIn`/`slideUp` keyframes
- `responsive.scss` — Breakpoint mixins (mobile/tablet/desktop)

### Conventions

- **Field fallback**: API response handlers check 3-5 alternative field names (`userId ?? id ?? memberId`) for backend version compatibility
- **Department mapping**: normalizes backend values (`"sales"`, `"purchase"`) ↔ Chinese display names (`"销售部"`, `"采购部"`)
- **Offline dev**: `userStore.login()` catches network errors and creates a synthetic `offline-token-*` session, letting the UI work without backend
- **Each view** lives in `src/views/<ViewName>/index.vue`
- **All assets** local in `src/assets/`, no CDN URLs

### Docs

`docs/` contains backend design, game rules, frontend integration docs, and API test documentation — see individual files for server-side expectations.

## Key constraints

- `@` → `src/` alias
- Two-window Electron testing for local multiplayer
- Card images organized by department subfolder under `src/assets/`
- Game replay → IndexedDB (not yet implemented)
