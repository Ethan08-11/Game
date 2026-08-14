<template>
  <div class="login-page" :style="{ '--login-bg': `url(${bgImage})` }">
    <div class="parchment-layer" :style="{ '--parchment': `url(${parchmentBg})`, transform: `translate(${shiftX}px, ${shiftY}px)` }"></div>
    <div class="login-card" :style="{ transform: `translate(${shiftX}px, ${shiftY}px)` }" @keydown.enter="onEnterKey">
      <h1 class="logo">这单我们护了！！！！</h1>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="登录" name="login">
          <el-form @submit.prevent="onPanelLogin">
            <el-form-item><el-input v-model="loginForm.username" placeholder="用户名" /></el-form-item>
            <el-form-item><el-input v-model="loginForm.password" type="password" placeholder="密码" /></el-form-item>
            <el-form-item><el-button type="primary" @click="onPanelLogin" style="width:100%">登录</el-button></el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form @submit.prevent="handleRegister">
            <el-form-item><el-input v-model="regForm.username" placeholder="自定义账号" /></el-form-item>
            <el-form-item><el-input v-model="regForm.password" type="password" placeholder="密码" /></el-form-item>
            <el-form-item><el-button type="success" @click="handleRegister" style="width:100%">注册</el-button></el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>

    <div class="action-buttons">
      <button class="action-btn reset-btn" @click="resetPanel">重置</button>
      <button class="action-btn login-btn" @click="handleLogin">登 录</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useCommonStore } from '@/store/common'

import { connectRoomSocket } from '@/utils/roomSocket'
import bgImage from '@/assets/login-bg2.webp'
import parchmentBg from '@/assets/login-parchment-bg.webp'


const router = useRouter()
const user = useUserStore()
const common = useCommonStore()
const activeTab = ref('login')
const shiftX = ref(0)
const shiftY = ref(0)

const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', password: '' })

function onEnterKey() {
  if (activeTab.value === 'login') {
    handleLogin()
  } else {
    handleRegister()
  }
}

function onPanelLogin() {
  void handleLogin()
}

function resetPanel() {
  shiftX.value = 0
  shiftY.value = 0
  loginForm.username = ''
  loginForm.password = ''
}

async function handleLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入完整登录信息')
    return
  }
  common.showLoading()
  try {
    await user.login(loginForm.username, loginForm.password)
    if (user.userId) common.showLoading()
    if (user.token) connectRoomSocket(user.token)
    ElMessage.success('登录成功')
    router.push('/game-hall')
  } catch (e: any) {
    const msg = e?.message || '登录失败，请检查账号密码'
    console.error('[LoginPage] 登录失败:', e)
    ElMessage.error(msg)
  } finally {
    common.hideLoading()
  }
}

async function handleRegister() {
  if (!regForm.username || !regForm.password) {
    ElMessage.warning('请输入完整注册信息')
    return
  }
  if (regForm.password.length < 6) {
    ElMessage.warning('密码至少6位')
    return
  }
  try {
    common.showLoading()
    await user.register(regForm.username, regForm.password)
    ElMessage.success('注册成功')
    activeTab.value = 'login'
    loginForm.username = regForm.username
    regForm.username = ''
    regForm.password = ''
  } catch (e: any) {
    console.error('[LoginPage] 注册失败:', e)
    ElMessage.error(e.message || '注册失败，请稍后重试')
  } finally {
    common.hideLoading()
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: flex-start;
  height: 100%;
  padding: var(--space-4);
  padding-bottom: 180px;
  padding-left: 160px;
  isolation: isolate;
}
.login-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--login-bg) center/cover no-repeat;
  filter: brightness(0.6);
  z-index: 0;
}
.parchment-layer {
  position: absolute;
  left: -30px;
  top: -230px;
  width: 750px;
  height: 980px;
  background: var(--parchment) no-repeat center bottom / 100% auto;
  z-index: 0;
  pointer-events: none;
  transition: transform 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  padding: var(--space-10);
  transition: transform 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.logo {
  text-align: center;
  color: #4a3520;
  margin-bottom: calc(var(--space-6) + 12px);
  margin-top: -16px;
  font-size: var(--text-4xl);
  font-weight: var(--weight-semibold);
}

.login-card :deep(.el-tabs__item) {
  color: #5c3d2e;
  font-size: 18px;
}
.login-card :deep(.el-tabs__item.is-active) {
  color: #3e2a14;
  font-weight: bold;
}
.login-card :deep(.el-tabs__active-bar) {
  background-color: #4a3520;
}
.login-card :deep(.el-input__inner) {
  color: #4a3520;
  font-size: 16px;
}
.login-card :deep(.el-input__inner::placeholder) {
  color: #8b6b4a;
  font-size: 16px;
}
.login-card :deep(.el-button--primary) {
  background-color: #4a3520;
  border-color: #4a3520;
  color: #e0d5c0;
  font-size: 16px;
}
.login-card :deep(.el-button--primary:hover) {
  background-color: #5c3d2e;
  border-color: #5c3d2e;
}
.login-card :deep(.el-button--success) {
  background-color: #4a3520;
  border-color: #4a3520;
  color: #e0d5c0;
  font-size: 16px;
}
.login-card :deep(.el-button--success:hover) {
  background-color: #5c3d2e;
  border-color: #5c3d2e;
}

.action-buttons {
  position: absolute;
  bottom: 24px;
  left: 40px;
  z-index: 2;
  display: flex;
  gap: var(--space-4);
}
.action-btn {
  padding: var(--space-2) var(--space-6);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: var(--radius-md);
  background: rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  color: #2a2218;
  font-size: var(--text-md);
  font-weight: var(--weight-bold);
  cursor: pointer;
  transition: all var(--transition-fast);
}
.action-btn:hover {
  background: rgba(0, 0, 0, 0.6);
  border-color: var(--color-accent);
}
.login-btn {
  background: rgba(74, 53, 32, 0.1);
  border-color: rgba(74, 53, 32, 0.3);
  color: #2a2218;
}
.login-btn:hover {
  background: rgba(74, 53, 32, 0.9);
}

@media (max-width: 767px) {
  .login-card {
    max-width: 100%;
    padding: var(--space-6);
  }
}
</style>
