<template>
  <div class="result-page">
    <BackButton to="/game-hall" text="返回大厅" />
    <div class="result-header">
      <el-icon :size="48" :color="isVictory ? 'var(--color-accent)' : 'var(--color-text-tertiary)'">
        <component :is="isVictory ? Present : CircleCloseFilled" />
      </el-icon>
      <h1 :class="isVictory ? 'win' : 'lose'">
        {{ isVictory ? '击败霸凌者！雇主安全了' : '保护失败' }}
      </h1>
    </div>
    <div class="stats-panel">
      <p>对局回合：{{ rounds }}</p>
      <p>对局结果：{{ isVictory ? '胜利' : '失败' }}</p>
      <p>霸凌者剩余 HP：{{ game.bullyHP }}/{{ game.maxBullyHP }}</p>
      <p v-if="isVictory" class="points-reward">获得酬劳：+{{ rewardMoney }} 金币</p>
    </div>
    <div v-if="!isVictory" class="revive-section">
      <el-button class="revive-btn" type="primary" :loading="reviving" @click="handleRevive">
        复活！！
      </el-button>
      <p class="revive-hint">观看广告免费复活一次</p>
    </div>
    <div class="btn-group">
      <el-button type="primary" @click="$router.push('/game-hall')">返回大厅</el-button>
      <el-button @click="$router.push('/matchmaking')">重新组队</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Present, CircleCloseFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useGameStore } from '@/store/game'
import { useUserStore } from '@/store/user'
import { useRoomStore } from '@/store/room'
import { getMatchSettlement, getMatchReviveStatus, requestMatchRevive, type MatchSettlementResp } from '@/api'
import BackButton from '@/components/BackButton.vue'

const route = useRoute()
const router = useRouter()
const game = useGameStore()
const user = useUserStore()
const room = useRoomStore()
const rounds = ref(0)
const settlement = ref<MatchSettlementResp | null>(null)
const reviving = ref(false)
const isVictory = computed(() => {
  if (settlement.value) {
    return settlement.value.victory ?? (settlement.value.winnerType === 1)
  }
  return game.isVictory
})
const rewardMoney = computed(() => {
  const list = settlement.value?.players ?? []
  const mine = list.find((p: any) => String(p.userId) === String(user.userId)) || list[0]
  const awarded = Number(mine?.moneyAwarded ?? game.pointsEarned)
  if (Number.isFinite(awarded) && awarded > 0) return awarded
  return isVictory.value ? 50 : 0
})

onMounted(async () => {
  const matchId = String(route.params.matchId || room.matchId || '')
  if (matchId) {
    settlement.value = await getMatchSettlement(matchId)
    rounds.value = settlement.value.totalRounds ?? rounds.value
    game.isVictory = settlement.value.victory ?? settlement.value.winnerType === 1
    game.maxBullyHP = settlement.value.bossMaxHp ?? game.maxBullyHP
    game.bullyHP = settlement.value.bossRemainingHp ?? game.bullyHP
    game.pointsEarned = rewardMoney.value
  } else if (game.isVictory && game.pointsEarned <= 0) {
    game.pointsEarned = 50
  }
  void user.loadMe().catch(() => {})
})

async function handleRevive() {
  const matchId = String(route.params.matchId || room.matchId || '')
  if (!matchId || !user.userId) {
    ElMessage.warning('无法获取对局信息')
    return
  }
  reviving.value = true
  try {
    const status = await getMatchReviveStatus(matchId, user.userId)
    if (!status.canRevive) {
      ElMessage.warning(status.message || '当前无法复活')
      return
    }
    await requestMatchRevive(matchId, {
      userId: user.userId,
      adRequestId: `revive-${matchId}-${user.userId}-${Date.now()}`,
      adPlatform: 'manual',
      reviveReason: 'watch_ad',
      adCallbackRaw: JSON.stringify({ completed: true }),
    })
    ElMessage.success('复活成功！重返战场！')
    game.resetGameOver()
    router.push(`/battle/${matchId}`)
  } catch (error: any) {
    ElMessage.error(error?.message || '复活失败')
  } finally {
    reviving.value = false
  }
}
</script>

<style scoped>
.result-page { position: relative; text-align: center; padding: var(--space-16) var(--space-10); color: var(--color-text-primary); }
.result-header { margin-bottom: var(--space-6); }
h1 { font-size: var(--text-4xl); margin: var(--space-3) 0; font-weight: var(--weight-bold); }
h1.win { color: var(--color-accent); }
h1.lose { color: var(--color-text-tertiary); }
.stats-panel {
  background: var(--color-surface-02);
  border: 1px solid var(--color-border-subtle);
  padding: var(--space-6) var(--space-8);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-8);
  display: inline-block;
  text-align: left;
}
.stats-panel p { margin: var(--space-2) 0; }
.points-reward { color: var(--color-accent); font-weight: var(--weight-bold); font-size: var(--text-xl); }
.revive-section {
  margin-bottom: var(--space-6);
}
.revive-btn {
  font-size: var(--text-xl);
  padding: var(--space-4) var(--space-16);
  border-radius: var(--radius-lg);
  animation: revivePulse 1.5s ease-in-out infinite;
}
.revive-hint {
  margin-top: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
}
@keyframes revivePulse {
  0%, 100% { box-shadow: 0 0 8px rgba(64, 158, 255, 0.4); }
  50% { box-shadow: 0 0 20px rgba(64, 158, 255, 0.8); }
}
.btn-group { display: flex; gap: var(--space-4); justify-content: center; flex-wrap: wrap; }
</style>
