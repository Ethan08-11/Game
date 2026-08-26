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
          <p>两位玩家合作，通过打出卡牌将顾客满意度提升至 100 即可获胜。若双方体力同时降为 0，则游戏失败。</p>
        </section>
        <section class="rule-section">
          <h3>基本流程</h3>
          <ul>
            <li>每人牌库尽量凑满 30 张。销售为：本部门基础约 14 张（每种 2 张）+ 本部门收藏 8 种各 1 张 + 公共收藏 6 张 + 公共基础 2 张。采购基础卡较少时，多出的位置优先给本部门收藏。不含对方部门。解锁不足时可少于 30 张，够了就必须满 30 张</li>
            <li>每回合从牌堆抽满 5 张手牌</li>
            <li>玩家轮流打出卡牌，消耗资金</li>
            <li>回合结束后 BOSS 会攻击其中一名玩家</li>
            <li>满意度达到 100 即胜利</li>
          </ul>
        </section>
        <section class="rule-section">
          <h3>卡牌类型</h3>
          <ul>
            <li><strong>攻击牌</strong>：增加顾客满意度</li>
            <li><strong>防御牌</strong>：为队友提供护盾，抵挡 BOSS 伤害</li>
            <li><strong>抽牌牌</strong>：本回合额外抽牌，未打出的牌仍会在结束回合时弃掉</li>
            <li><strong>消耗牌</strong>：一次性强力效果</li>
            <li><strong>全体牌</strong>：同时为双方增加护盾或恢复体力，无需选择目标</li>
            <li><strong>压制牌</strong>：降低本回合霸凌者攻击</li>
          </ul>
        </section>
        <section class="rule-section">
          <h3>资金系统</h3>
          <p>每回合共有 3 点资金（共享池），卡牌消耗 0-3 点不等。资金不累积到下一回合。</p>
        </section>
        <section class="rule-section">
          <h3>体力与护盾</h3>
          <ul>
            <li>每位玩家初始体力 100 点</li>
            <li>BOSS 每回合攻击造成 5-14 点伤害</li>
            <li>护盾可抵挡 BOSS 伤害</li>
            <li>体力 ≤20 为危险状态（红色预警）</li>
          </ul>
        </section>
        <section class="rule-section">
          <h3>胜利奖励</h3>
          <p>胜利后根据满意度获得积分奖励，满意度达到 100 可额外获得 50 点积分加成。每位玩家还会随机解锁一张尚未拥有的收藏卡，收入卡牌图鉴；未解锁的卡不会出现在对局中。本部门基础卡每局都会带上；已解锁收藏卡有单独名额，最近没上场的会优先抽到。</p>
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
