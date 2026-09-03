<template>
  <div class="rules-page" :style="{ '--rules-bg': bgImage ? `url(${bgImage})` : '' }">
    <button class="back-btn" @click="$router.push('/game-hall')">
      <span class="back-arrow">←</span>
      <span>返回大厅</span>
    </button>

    <div class="rules-content">
      <h1 class="rules-title">规则说明</h1>
      <div class="rules-body">
        <section class="rule-section">
          <h3>游戏目标</h3>
          <p>两位玩家合作保护顾客，打出卡牌将霸凌者血值降到 0 即可获胜。玩家与霸凌者都使用血值：我方血值为 0 会倒下；任意一名玩家倒下时，若队友仍存活，可尝试复活。双方都倒下、放弃复活或复活超时，则保护失败。</p>
        </section>
        <section class="rule-section">
          <h3>基本流程</h3>
          <ul>
            <li>每人牌库尽量凑满 30 张。当前只用本部门成员卡：基础每种 2 张 + 本部门收藏各 1 张。公共部、中立卡已下架，不再补公共牌。不含对方部门。解锁不足时可少于 30 张，够了就必须满 30 张</li>
            <li>开局双方查看手牌后，由房主决定谁先出牌</li>
            <li>每回合从牌堆抽满 5 张手牌</li>
            <li>玩家轮流打出卡牌，消耗调用机会；结束回合时手中剩余牌进入弃牌</li>
            <li>双方都结束后，霸凌者按本局模板出手：点名恶霸只打当前血更虚的那人（按剩余血量比例，持平时随机）；其余仍同时打两名护卫。面板目标显示部门，不显示 P1 / P2</li>
            <li>将霸凌者血值打到 0，顾客获救，对局胜利</li>
          </ul>
        </section>
        <section class="rule-section">
          <h3>卡牌类型</h3>
          <ul>
            <li><strong>攻击牌</strong>：降低霸凌者血值</li>
            <li><strong>防御牌</strong>：为自己或队友增加护盾，抵挡霸凌者伤害</li>
            <li><strong>抽牌牌</strong>：本回合额外抽牌，未打出的牌仍会在结束回合时弃掉</li>
            <li><strong>治疗牌</strong>：恢复自己或队友血值</li>
            <li><strong>辅助牌</strong>：增加调用机会、抽牌或提供其他支援</li>
            <li><strong>压制牌</strong>：降低本回合霸凌者攻击</li>
          </ul>
        </section>
        <section class="rule-section">
          <h3>调用机会</h3>
          <p>每位玩家每回合有 3 点调用机会，出牌按费用 0～3 点消耗。未用完的点数不会留到下一回合；部分卡牌可以在本回合或下回合增加调用机会。</p>
        </section>
        <section class="rule-section">
          <h3>血值与护盾</h3>
          <ul>
            <li>销售部初始血值 50，采购部初始血值 75</li>
            <li>霸凌者血量 150。与玩家一样用血值；将其打到 0 即可救下顾客</li>
            <li>每回合双方出完牌后，霸凌者出手：销售+采购时攻击 23～26，双销售 21～24。约八成五打满、约一成五自己 +14 盾且攻击减半但仍出手。第 2 回合起顾客可能再加减攻击。点名恶霸只打血更虚的那人，护盾可以挡住；硬扛恶霸的一成五回合就是这层 14 点盾；针对恶霸约八成五回合给本回合打得最疼的人再加 8 点；不落单恶霸在两人都掉血不超过 3 点时，下回合再对两人追加一次半伤</li>
            <li>护盾优先抵挡伤害，结算后清空</li>
            <li>血值 ≤20 为危险状态（红色预警）</li>
          </ul>
        </section>
        <section class="rule-section">
          <h3>复活</h3>
          <ul>
            <li>一名玩家倒下且队友仍存活时，倒下的玩家可观看广告复活，每局最多 1 次</li>
            <li>复活后血值在 16～22 点之间随机，且不超过自身上限</li>
            <li>超时未复活或主动放弃，保护失败；双方同时倒下则无法复活</li>
          </ul>
        </section>
        <section class="rule-section">
          <h3>对局奖励</h3>
          <p>对局结束不再发放金币，金币只能通过任务领取。胜利时每位玩家还会随机解锁一张尚未拥有的收藏卡，收入卡牌图鉴；未解锁的卡不会出现在对局中。本部门基础卡每局都会带上；已解锁收藏卡有单独名额，最近没上场的会优先抽到。</p>
        </section>
        <section class="rule-section">
          <h3>每日任务与离开对局</h3>
          <ul>
            <li>当天前 3 局：打完第 1 / 2 / 3 局可领 30 / 40 / 50 金币（输赢都算）；该局获胜再各领 10 金币</li>
            <li>主动放弃或掉线超时：记失败，占用当日第 N 局，不算完成、不发任务金币</li>
            <li>对局卡死且重连不上：离开菜单可申请「对局异常」作废本局（约 3 分钟无有效操作），不占用任务局数</li>
          </ul>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import bg1 from '@/assets/hall-bg.webp'
import bg2 from '@/assets/hall-bg2.webp'

const bgImage = ref('')

onMounted(() => {
  const hour = new Date().getHours()
  bgImage.value = hour >= 6 && hour < 18 ? bg2 : bg1
})
</script>

<style scoped>
.rules-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  color: var(--color-text-primary);
  position: relative;
  isolation: isolate;
  overflow: auto;
}
.rules-page::before {
  content: '';
  position: fixed;
  inset: 0;
  background: var(--rules-bg) center/cover no-repeat;
  filter: blur(12px);
  z-index: -1;
  transform: scale(1.1);
}

.back-btn {
  position: fixed;
  top: 20px;
  left: 20px;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: var(--color-text-primary);
  font-size: var(--text-base);
  cursor: pointer;
  transition: all var(--transition-fast);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
}
.back-btn:hover {
  background: rgba(255, 255, 255, 0.18);
  border-color: var(--color-accent);
}
.back-arrow {
  font-size: 18px;
  line-height: 1;
}

.rules-content {
  flex: 1;
  max-width: 720px;
  margin: 80px auto 60px;
  padding: 40px 48px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-xl);
  box-shadow: 0 4px 32px rgba(0, 0, 0, 0.25);
}
.rules-title {
  text-align: center;
  font-size: 28px;
  font-weight: var(--weight-bold);
  margin-bottom: 32px;
  color: var(--color-accent);
}
.rules-body {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.rule-section h3 {
  font-size: 18px;
  font-weight: var(--weight-semibold);
  margin-bottom: 8px;
  color: var(--color-text-primary);
}
.rule-section p,
.rule-section li {
  font-size: 15px;
  line-height: 1.7;
  color: var(--color-text-secondary);
}
.rule-section ul {
  padding-left: 20px;
  list-style: disc;
}
.rule-section li {
  margin-bottom: 4px;
}
</style>
