import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/cg',
  },
  {
    path: '/cg',
    name: 'CgPage',
    component: () => import('@/views/CgPage/index.vue'),
    meta: { title: '开场CG', noAuth: true },
  },
  {
    path: '/login',
    name: 'LoginPage',
    component: () => import('@/views/LoginPage/index.vue'),
    meta: { title: '登录注册', noAuth: true },
  },
  {
    path: '/game-hall',
    name: 'GameHall',
    component: () => import('@/views/GameHall/index.vue'),
    meta: { title: '游戏大厅' },
  },
  {
    path: '/customer-current',
    name: 'CustomerCurrent',
    component: () => import('@/views/CustomerCurrent/index.vue'),
    meta: { title: '本局顾客' },
  },
  {
    path: '/customer-intro',
    name: 'CustomerIntro',
    component: () => import('@/views/CustomerIntro/index.vue'),
    meta: { title: '顾客图鉴' },
  },
  {
    path: '/matchmaking',
    name: 'MatchMaking',
    component: () => import('@/views/MatchMaking/index.vue'),
    meta: { title: '匹配组队' },
  },
  {
    path: '/battle/:matchId?',
    name: 'BattlePage',
    component: () => import('@/views/BattlePage/index.vue'),
    meta: { title: '双人对战' },
  },
  {
    path: '/result/:matchId?',
    name: 'ResultPage',
    component: () => import('@/views/ResultPage/index.vue'),
    meta: { title: '结算复盘' },
  },
  {
    path: '/achievements',
    name: 'Achievements',
    component: () => import('@/views/Achievements/index.vue'),
    meta: { title: '成就' },
  },
  {
    path: '/points',
    name: 'PointsPage',
    component: () => import('@/views/PointsPage/index.vue'),
    meta: { title: '积分' },
  },
  {
    path: '/leaderboard',
    name: 'Leaderboard',
    component: () => import('@/views/Leaderboard/index.vue'),
    meta: { title: '排行榜' },
  },
  {
    path: '/cards',
    name: 'CardsPage',
    component: () => import('@/views/CardsPage/index.vue'),
    meta: { title: '卡牌图鉴' },
  },
  {
    path: '/skins',
    name: 'SkinsPage',
    component: () => import('@/views/SkinsPage/index.vue'),
    meta: { title: '皮肤商店' },
  },
  {
    path: '/quests',
    name: 'QuestsPage',
    component: () => import('@/views/QuestsPage/index.vue'),
    meta: { title: '任务' },
  },
  {
    path: '/rules',
    name: 'RulesPage',
    component: () => import('@/views/RulesPage/index.vue'),
    meta: { title: '规则说明' },
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) || 'Help-Ing Hand'

  const token = localStorage.getItem('token')
  const matchId = sessionStorage.getItem('activeMatchId')
  const resumeBattle = Boolean(token && matchId)
    && to.name !== 'BattlePage'
    && to.name !== 'ResultPage'
    && (to.meta.noAuth || to.name === 'GameHall')

  if (resumeBattle) {
    next(`/battle/${matchId}`)
    return
  }

  if (to.meta.noAuth) {
    next()
    return
  }

  if (!token) {
    next('/login')
    return
  }

  next()
})

export default router
