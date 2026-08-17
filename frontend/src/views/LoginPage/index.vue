<template>
  <div class="login-page" :style="{ '--login-bg': `url(${bgImage})` }">
    <div class="parchment-layer" :style="{ '--parchment': `url(${parchmentBg})` }"></div>
    <h1 class="logo">这单我们护了！！！！</h1>
    <div class="login-card" @keydown.enter="onEnterKey">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="登录" name="login">
          <el-form @submit.prevent="onPanelLogin">
            <el-form-item><el-input :model-value="loginForm.username" placeholder="用户名" @update:model-value="onLoginUsername" /></el-form-item>
            <el-form-item><el-input v-model="loginForm.password" type="password" placeholder="密码" /></el-form-item>
            <el-form-item><el-button type="primary" @click="onPanelLogin" style="width:100%">登录</el-button></el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form @submit.prevent="handleRegister">
            <el-form-item><el-input :model-value="regForm.username" placeholder="用户名（4-50位字母、数字或下划线）" @update:model-value="onRegUsername" /></el-form-item>
            <el-form-item><el-input v-model="regForm.password" type="password" placeholder="密码（至少3位）" /></el-form-item>
            <el-form-item><el-button type="success" @click="handleRegister" style="width:100%">注册</el-button></el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
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
import { capitalizeUsername } from '@/utils/playerName'
import bgImage from '@/assets/login-bg2.webp'
import parchmentBg from '@/assets/login-parchment-bg.webp'


const router = useRouter()
const user = useUserStore()
const common = useCommonStore()
const activeTab = ref('login')

const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', password: '' })

function onLoginUsername(value: string | number) {
  loginForm.username = capitalizeUsername(String(value ?? ''))
}

function onRegUsername(value: string | number) {
  regForm.username = capitalizeUsername(String(value ?? ''))
}

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

async function handleLogin() {
  if (!loginForm.username.trim() || !loginForm.password) {
    ElMessage.warning('请输入完整登录信息')
    return
  }
  ElMessage.closeAll()
  common.showLoading()
  try {
    await user.login(capitalizeUsername(loginForm.username), loginForm.password)
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
  const username = capitalizeUsername(regForm.username)
  const password = regForm.password
  if (!username || !password) {
    ElMessage.warning('请输入完整注册信息')
    return
  }
  if (!/^[A-Za-z0-9_]{4,50}$/.test(username)) {
    ElMessage.warning('用户名须为4-50位字母、数字或下划线')
    return
  }
  if (password.length < 3 || password.length > 64) {
    ElMessage.warning('密码须为3-64位')
    return
  }
  try {
    common.showLoading()
    await user.register(username, password)
    if (user.token) connectRoomSocket(user.token)
    ElMessage.success('注册成功')
    router.push('/game-hall')
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
}
.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  padding: var(--space-10);
  padding-top: 8px;
}
.logo {
  position: absolute;
  left: 70px;
  top: 228px;
  width: 520px;
  z-index: 2;
  margin: 0;
  text-align: center;
  color: #4a3520;
  font-size: var(--text-4xl);
  font-weight: var(--weight-semibold);
  text-shadow: 0 1px 0 rgba(255, 248, 230, 0.85);
  pointer-events: none;
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

@media (max-width: 767px) {
  .login-card {
    max-width: 100%;
    padding: var(--space-6);
  }
}
</style>
