# 前端 Vue3 + Electron 双 Token WebSocket 对接示例

本文档说明当前项目中，前端如何在 **Vue3 + Electron** 环境下对接后端的双 token 认证体系，并正确建立 WebSocket 连接。

---

## 1. 背景

当前项目中，HTTP 接口和 WebSocket 连接都需要用户身份信息。

### 双 token 说明

通常包含：

- `accessToken`：短期访问令牌，用于接口鉴权、WebSocket 鉴权；
- `refreshToken`：长期刷新令牌，用于 accessToken 过期后刷新登录态；

### 当前项目中的实际用法

- **HTTP 请求**：使用 `Authorization: Bearer {accessToken}`；
- **WebSocket 连接**：通过 URL 参数传入 `accessToken`；
- **refreshToken**：主要用于 HTTP 登录态续期，不直接用于 WebSocket 握手；

---

## 2. WebSocket 对接原则

### 2.1 WebSocket 只带 accessToken

当前后端 `RoomWebSocketHandler` 的连接逻辑是：

1. 从 WebSocket URL 中读取 `accessToken`；
2. 调用后端 token 校验逻辑解析当前用户；
3. 校验通过后才绑定会话；
4. 校验失败则关闭连接；

因此，前端建立 WebSocket 时，只需要携带 `accessToken`。

### 2.2 refreshToken 不直接参与 WebSocket

`refreshToken` 主要用于：

- accessToken 过期后重新登录；
- HTTP 接口刷新会话；
- 重新获取新的 accessToken；

如果 WebSocket 断开后需要重连，前端应优先保证拿到新的有效 `accessToken`，再重新建立连接。

---

## 3. 推荐的前端存储方式

### 3.1 Electron 主进程 / 渲染进程

建议前端把 token 放在统一的认证状态里，例如：

- Vuex / Pinia；
- Electron 本地持久化存储；
- 浏览器本地存储（如果你的安全策略允许）；

### 3.2 推荐保存字段

- `accessToken`
- `refreshToken`
- `userId`
- `expireAt`（可选）

---

## 4. HTTP 接口调用方式

### 4.1 请求头

前端调用 HTTP 接口时，统一使用：

```http
Authorization: Bearer {accessToken}
```

### 4.2 示例

```js
async function requestJson(url, options = {}) {
  const headers = {
    ...(options.headers || {}),
    Authorization: `Bearer ${store.accessToken}`,
    'Content-Type': 'application/json',
  };

  const resp = await fetch(url, {
    ...options,
    headers,
  });

  return await resp.json();
}
```

---

## 5. WebSocket 连接方式

### 5.1 连接 URL

当前后端 WebSocket 连接需要在 URL 参数里带上 `accessToken`：

```text
ws://127.0.0.1:8080/ws/room?accessToken=xxxxx
```

如果是生产环境，可以是：

```text
wss://your-domain.com/ws/room?accessToken=xxxxx
```

### 5.2 Vue3 示例

```js
function connectWebSocket(accessToken) {
  const url = new URL('ws://127.0.0.1:8080/ws/room');
  url.searchParams.set('accessToken', accessToken);

  const ws = new WebSocket(url.toString());

  ws.onopen = () => {
    console.log('WebSocket connected');
  };

  ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log('WS message:', data);
  };

  ws.onclose = () => {
    console.log('WebSocket closed');
  };

  ws.onerror = (err) => {
    console.error('WebSocket error:', err);
  };

  return ws;
}
```

---

## 6. Electron 场景建议

### 6.1 主进程持有 token

在 Electron 中，建议由主进程统一保存 token，然后渲染进程通过 IPC 获取。

这样可以避免：

- 多个窗口 token 不一致；
- 页面刷新后 token 丢失；
- WebSocket 重连时拿不到最新 token；

### 6.2 渲染进程发起连接

渲染进程拿到 `accessToken` 后：

1. 先调用 HTTP 接口确认登录态；
2. 再建立 WebSocket；
3. WebSocket 断线后，若 accessToken 过期，先刷新再重连；

---

## 7. 建议的双 token 流程

### 7.1 登录成功后

后端返回：

- `accessToken`
- `refreshToken`
- `expireIn`

前端保存后：

1. 使用 accessToken 调 HTTP；
2. 使用 accessToken 建立 WebSocket；

### 7.2 accessToken 过期后

如果 HTTP 请求返回 401：

1. 使用 refreshToken 调刷新接口；
2. 获取新的 accessToken；
3. 更新本地 token；
4. 重新发起 HTTP 请求；
5. 如果 WebSocket 已断开，使用新 accessToken 重连；

### 7.3 WebSocket 重连

如果 WebSocket 断开：

1. 检查当前 accessToken 是否仍有效；
2. 如果有效，直接重连；
3. 如果无效，先刷新 accessToken；
4. 再重新连接 WebSocket；

---

## 8. 推荐封装方式

### 8.1 统一 token 管理

建议前端封装一个认证模块：

- `getAccessToken()`
- `getRefreshToken()`
- `setTokens()`
- `clearTokens()`
- `refreshAccessToken()`

### 8.2 统一 WebSocket 管理

建议封装一个 WebSocket 管理器：

- `connect()`
- `disconnect()`
- `reconnect()`
- `sendHeartbeat()`
- `onMessage()`

---

## 9. 示例实现

### 9.1 token 管理

```js
const authStore = {
  accessToken: '',
  refreshToken: '',

  setTokens({ accessToken, refreshToken }) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  },

  clearTokens() {
    this.accessToken = '';
    this.refreshToken = '';
  },
};
```

### 9.2 WebSocket 管理

```js
class WsManager {
  constructor(baseUrl, getAccessToken) {
    this.baseUrl = baseUrl;
    this.getAccessToken = getAccessToken;
    this.socket = null;
    this.heartbeatTimer = null;
  }

  connect() {
    const token = this.getAccessToken();
    if (!token) throw new Error('缺少 accessToken');

    const url = new URL(this.baseUrl);
    url.searchParams.set('accessToken', token);

    this.socket = new WebSocket(url.toString());

    this.socket.onopen = () => {
      this.startHeartbeat();
    };

    this.socket.onclose = () => {
      this.stopHeartbeat();
    };

    this.socket.onerror = () => {
      this.stopHeartbeat();
    };
  }

  startHeartbeat() {
    this.stopHeartbeat();
    this.heartbeatTimer = setInterval(() => {
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        this.socket.send(JSON.stringify({ type: 'ws.heartbeat', timestamp: Date.now() }));
      }
    }, 20000);
  }

  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  disconnect() {
    this.stopHeartbeat();
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
  }
}
```

---

## 10. 断线重连建议

### 10.1 重连前检查

如果 WebSocket 断开，前端先判断：

- accessToken 是否过期；
- 当前是否还在对局中；
- 当前对局是否处于 `RECONNECT_WAIT`；

### 10.2 重连后动作

重连成功后建议：

1. 重新调用 `GET /api/matches/{matchId}`；
2. 重新同步对局状态；
3. 更新手牌、阶段、等待重连状态；
4. 恢复界面；

---

## 11. 常见错误

### 11.1 WebSocket 连接失败

可能原因：

- accessToken 过期；
- token 为空；
- URL 写错；
- 服务端未启动；

### 11.2 HTTP 正常但 WS 失败

可能原因：

- HTTP 用了新的 accessToken；
- WS 还在用旧 token；

### 11.3 刷新后 WS 断开

可能原因：

- 只刷新了 HTTP token；
- 没有同步更新 WS 连接使用的 token；

---

## 12. 最推荐的前端接入顺序

1. 登录拿到双 token；
2. 保存 `accessToken` 和 `refreshToken`；
3. 所有 HTTP 请求统一带 `Authorization`；
4. WebSocket URL 参数带 `accessToken`；
5. 断线时先检查 accessToken；
6. 过期则刷新，没过期就直接重连；
7. 重连后重新同步对局状态；

---

## 13. 总结

当前项目的双 token 对接思路可以概括为：

- **HTTP 接口**：`Authorization: Bearer accessToken`
- **WebSocket**：`ws://.../ws/room?accessToken=...`
- **refreshToken**：用于 accessToken 失效后的刷新

也就是说，前端并不是把两个 token 都塞进 WebSocket，而是：

- 用 accessToken 做连接鉴权；
- 用 refreshToken 做续期；
- WebSocket 断线后先保证 accessToken 有效，再重连；
