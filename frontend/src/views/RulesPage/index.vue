<template>
  <div class="rules-page" :style="{ '--rules-bg': bgImage ? `url(${bgImage})` : '' }">
    <button class="back-btn" @click="$router.push('/game-hall')">
      <span class="back-arrow">←</span>
      <span>{{ t('返回大厅') }}</span>
    </button>
    <LangSwitch class="rules-lang" />

    <div class="rules-content">
      <h1 class="rules-title">{{ t('规则说明') }}</h1>
      <div v-if="lang !== 'en'" class="rules-body">
        <section class="rule-section">
          <h3>游戏目标</h3>
          <p>两位玩家合作保护顾客，打出卡牌将霸凌者血值降到 0 即可获胜。玩家与霸凌者都使用血值：我方血值为 0 会倒下；任意一名玩家倒下时，若队友仍存活，可尝试复活。双方都倒下、放弃复活或复活超时，则保护失败。</p>
        </section>
        <section class="rule-section">
          <h3>基本流程</h3>
          <ul>
            <li>每人牌库尽量凑满 30 张。当前用本部门成员卡：基础每种 2 张 + 本部门收藏各 1 张，再用公共部基础卡补位（雇佣兵、物流骑士、营销大力士）。设计部文艺射手、技术部工兵、财务部补给牧师、人事部占卜家与行政部行政文职为公共部收藏，需胜利解锁后才会进入对局。中立卡已下架。不含对方部门。解锁不足时可少于 30 张，够了就必须满 30 张</li>
            <li>开局双方查看手牌后，由房主决定谁先出牌</li>
            <li>每回合从牌堆抽满 5 张手牌</li>
            <li>玩家轮流打出卡牌，消耗调用机会；结束回合时手中剩余牌进入弃牌</li>
            <li>双方都结束后，硬扛恶霸同时打两名护卫。面板目标显示部门，不显示 P1 / P2</li>
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
            <li>各组队霸凌者血量均为 180。与玩家一样用血值；将其打到 0 即可救下顾客</li>
            <li>每回合双方出完牌后，硬扛恶霸出手：各组队攻击均为 17～20。约八成五打满、约一成五自己 +14 盾且攻击减半但仍出手。第 2 回合起顾客可能再加减攻击。护盾可以挡住</li>
            <li>护盾优先抵挡伤害，结算后清空</li>
            <li>血值 ≤20 为危险状态（红色预警）</li>
          </ul>
        </section>
        <section class="rule-section">
          <h3>复活</h3>
          <ul>
            <li>一名玩家倒下且队友仍存活时，可观看广告复活。全队每局只允许一名玩家复活，本局仍按最终胜负计入排行榜</li>
            <li>复活后血值在 16～22 点之间随机，且不超过自身上限</li>
            <li>若两名玩家都复活，立即判定保护失败，并计入败场。超时未复活、主动放弃或双方同时倒下，同样保护失败</li>
          </ul>
        </section>
        <section class="rule-section">
          <h3>对局奖励</h3>
          <p>对局结束不再发放金币，金币只能通过任务领取。胜利时每位玩家还会随机解锁一张尚未拥有的收藏卡，收入卡牌图鉴；未解锁的卡不会出现在对局中。本部门基础卡每局都会带上；已解锁收藏卡有单独名额，最近没上场的会优先抽到。</p>
        </section>
        <section class="rule-section">
          <h3>账号与组队</h3>
          <ul>
            <li>同一账号同一时间只能在一端登录。其他端登录后，本端会立即下线</li>
            <li>不能与同一台电脑或同一网络下的账号组队。本地开发双窗口测试不受此限制</li>
            <li>大厅本周模式按北京时间的日历周换标题（周一开始，含 1 日的那一周为第一周），每段七个字：第一周「暴风雨前的宁静」（高压不生效）；第二周「暴雨点名前五人」；第三周「雷霆压住前五名」；第四周「狂风不放前五席」；第五周及以后「风暴收紧前五网」</li>
            <li>全天高压名单最多 5 个账号：每月日历第一周不生效。第二周起，每天 0 点按预测总金币锁定前五，当天一直用这份名单，白天打完和领金币都不再改名次。预测总金币 = 已经到手的金币 + 从今天到月底还来得及打的工作日按全胜 150 + 这个月还来得及领的周任务（每次 500，本月最多 4 次）。已经结束又没领的周次不能补，不计入预测；只算还没结束的周，本周没领也算一次，周次更少时按还能领到的次数算。工作日也一样：定额还剩得更多、但日历不够时，来不及打的天数不计入。没开打的人不进名单。房间里有这 5 人之一时，本局从「胆小怕事」「焦虑难安」「刻薄尖客」三人里平分抽取，抽中后本局不变。不在高压名单里则七位顾客平分抽取</li>
          </ul>
        </section>
        <section class="rule-section">
          <h3>每日任务与离开对局</h3>
          <ul>
            <li>每日任务每天 0 点刷新。当天前 3 局：打完第 1 / 2 / 3 局可领 30 / 40 / 50 金币（输赢都算）；该局获胜再各领 10 金币</li>
            <li>漏打的局可以补。今天打满 3 局后，开局前才能勾选「这局算补打」。按北京时间，本月第一天没有补打。从第二天起，先补打过但没满 3 局的缺档，从最早的那天开始；这些补完后，再补最近一个一把没打的日子，按第 1、2、3 局。本月最后一天，正在补的那一天如果还欠后面的局，可以当天继续补完。已经结算的补打金币不重算。任务列表有「补打第 N 局」和「补打获胜」：第 1 / 2 / 3 档打完领 30 / 40 / 50，赢了再领 10，金币跟着当前这一档变。平时每天最多补 1 局，不占今天的前 3 局，也不加周任务队友。放弃这局，这一档作废、金币为 0，今天不能再补。作废不消耗，可以再开。补打记普通胜负和经验，不记入今天的前 3 局，完不成「完美一日」。欠账不跨月。补已经领过金币的那一天，定额满了也发；补还没记过的一天要占 1 个工作日，定额满了就不发金币</li>
            <li>每月固定扣 4 天单休：只有「当月天数 − 4」天能领金币。2026 年 11 月每人 21 天。2026 年 10 月休假 7 天，31 天只有 24 天能领。2026 年 9 月从 12 日起算，全员只算 16 天。领满后天数为休息日，不发金币（含周常）</li>
            <li>周任务按北京时间自然月计算，每人每月最多领取 4 次。当月领满后，本周即使凑满 10 位同事也不能再领</li>
            <li>周任务未领取前，任务页只推荐 3 名还没组过的玩家。优先本周上线并且打过的人，推荐不计入 10 人进度</li>
            <li>掉线超过 5 分钟仍未重连：如果本局玩家今天都还没作废过，本局作废，不记失败、不占今日局数和每周前三局。只要有一名玩家当天已经作废过，这局就不能再作废，会记为失败并占用今日局数。主动放弃仍然记失败</li>
            <li>对局卡死且重连不上：离开菜单可申请「对局异常」作废本局（约 5 分钟无有效操作，每人每天 1 次）。只要局里有人当天已经作废过，其他人也不能再作废。作废不记胜负、不占用当日任务局数，也不计入每周前 3 局队友</li>
          </ul>
        </section>
      </div>
      <div v-else class="rules-body">
        <section class="rule-section">
          <h3>Goal</h3>
          <p>Two players protect the customer together. Play cards until the Bully’s HP is 0. Both sides use HP. A guard at 0 is down. If one is down and the other is still up, that guard may try to revive. If both are down, a revive is refused, or the revive timer runs out, the customer is lost.</p>
        </section>
        <section class="rule-section">
          <h3>Flow</h3>
          <ul>
            <li>Each deck aims for 30 cards: 2 copies of each department basic card, 1 of each unlocked department collectible, then public basics to fill. Public collectibles enter only after a win unlocks them. Neutral cards are retired. You do not bring the other department. A short collection may be under 30; once you have enough, the deck must be 30.</li>
            <li>Both sides see their hands, then the host chooses who plays first.</li>
            <li>Each round draws back up to 5 cards.</li>
            <li>Players take turns spending Calls. Cards still in hand are discarded at the end of the turn.</li>
            <li>After both have finished, the Hard Bully hits both guards. The panel shows departments, not P1 / P2.</li>
            <li>Bring the Bully to 0 HP and the customer is saved.</li>
          </ul>
        </section>
        <section class="rule-section">
          <h3>Card types</h3>
          <ul>
            <li><strong>Attack</strong>: lower the Bully’s HP.</li>
            <li><strong>Shield</strong>: give yourself or your partner Shield.</li>
            <li><strong>Draw</strong>: draw extra cards this round. Unplayed cards are still discarded when the turn ends.</li>
            <li><strong>Heal</strong>: restore your HP or your partner’s.</li>
            <li><strong>Aid</strong>: Calls, draws, or other help.</li>
            <li><strong>Suppress</strong>: lower the Bully’s attack this round.</li>
          </ul>
        </section>
        <section class="rule-section">
          <h3>Calls</h3>
          <p>Each guard has 3 Calls a round. Cards cost 0 to 3. Unused Calls do not carry over. Some cards add Calls this round or next round.</p>
        </section>
        <section class="rule-section">
          <h3>HP and Shield</h3>
          <ul>
            <li>Sales starts at 50 HP. Purchase starts at 75.</li>
            <li>The Bully starts at 180 HP in every pairing. Reduce it to 0 to save the customer.</li>
            <li>After both sides finish, the Hard Bully attacks for 17–20. About 85% of rounds are a full swing. About 15% the Bully gains 14 Shield and halves its attack, but still swings. From round 2 the customer may change that attack. Shield can block it.</li>
            <li>Shield is spent before HP, then cleared.</li>
            <li>HP at 20 or below is danger.</li>
          </ul>
        </section>
        <section class="rule-section">
          <h3>Revive</h3>
          <ul>
            <li>When one guard is down and the partner is up, that guard may watch an ad to revive. Only one revive per match. The match still counts by its final result.</li>
            <li>Revive HP is a random 16–22, and never above that guard’s maximum.</li>
            <li>If both guards revive, the customer is lost at once and it counts as a loss. The same happens if the timer runs out, a revive is refused, or both are down together.</li>
          </ul>
        </section>
        <section class="rule-section">
          <h3>Rewards</h3>
          <p>Matches no longer pay gold. Gold comes from quests. A win also unlocks one collectible card you do not own yet. Locked cards do not appear in matches. Department basics are always in the deck. Unlocked collectibles have their own slots, and cards that have sat out are drawn first.</p>
        </section>
        <section class="rule-section">
          <h3>Accounts and teams</h3>
          <ul>
            <li>One account can be online in one place. Another login signs this one out.</li>
            <li>You cannot team with an account on the same computer or the same network. Local two-window testing is exempt.</li>
            <li>The hall week title follows Beijing calendar weeks, Monday start. Week 1, the week that contains the 1st, is Calm Before the Storm and pressure is off. Then Rain Names the Five, Thunder Holds the Five, Gale Keeps the Five, and from week 5 Storm Draws the Net.</li>
            <li>The pressure list holds at most 5 accounts and is off during week 1. From week 2 it locks at 00:00 by forecast gold and stays for the day. Forecast gold is gold in hand, plus remaining playable work days at 150 for a full win, plus weekly quests still claimable this month (500 each, at most 4). Expired unclaimed weeks are left out. Someone who has not played is not listed. If the room includes one of the five, the customer is drawn evenly from Timid, Anxious, and Harsh. Otherwise the seven customers are even.</li>
          </ul>
        </section>
        <section class="rule-section">
          <h3>Daily quests and leaving</h3>
          <ul>
            <li>Daily quests refresh at 00:00. The first 3 matches pay 30 / 40 / 50 gold for finishing, win or loss, plus 10 more if that match is won.</li>
            <li>Unfinished earlier matches can be made up. A day you played but did not finish comes first, oldest day first. After those, the newest day you did not play starts at match 1. On the last day of the month, the remaining matches of that day can be finished the same day. Gold already paid is not recalculated. One extra match a day otherwise. The checkbox appears only after today’s 3 matches are done. The quest list shows Makeup match N and Makeup win: slots 1 / 2 / 3 pay 30 / 40 / 50 when claimed, plus 10 for a win, and the gold follows that slot. It does not use today’s first 3 matches or add a weekly teammate. A forfeit spends that slot for 0 gold and blocks another makeup today. A void does not. The first day of the month has nothing to make up, and debt does not cross months. A day that already paid gold still pays when the quota is full. A day that never paid needs a free work day, or the makeup pays nothing.</li>
            <li>Each month drops 4 rest days. November 2026 has 21 paid days. October 2026 has 24. September 2026 counts 16 days from the 12th. After the quota, the rest of the month pays no gold, including the weekly quest.</li>
            <li>Weekly quests follow the Beijing month and can be claimed at most 4 times. After that, a full set of 10 teammates pays nothing.</li>
            <li>Before the weekly quest is claimed, the page suggests 3 people you have not teamed with. Suggestions are not progress.</li>
            <li>A disconnect longer than 5 minutes voids the match if nobody at the table has used today’s void. A void is not a loss and does not use a daily slot. If someone already voided today, the match is a loss and uses a slot. A forfeit is always a loss.</li>
            <li>A stuck match can be voided from the leave menu after about 5 minutes without a real action, once per player per day. If anyone at the table already voided today, nobody else can. A void is not a win or a loss, does not use a daily slot, and does not count toward the weekly teammates.</li>
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
import { lang, t } from '@/i18n'
import LangSwitch from '@/components/LangSwitch.vue'

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
.rules-lang {
  position: absolute;
  top: 18px;
  right: 24px;
  z-index: 4;
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
