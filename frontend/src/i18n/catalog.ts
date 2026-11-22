import { lang, t } from './index'
import { translateEffectTemplate } from './effects.en'
import { CARD_NAME_EN } from './cardNames.en'

const CUSTOMERS: Record<string, { name: string; description: string }> = {
  CUSTOMER_KIND: {
    name: 'Kind Steady',
    description: 'Calm and steady. On trigger, the Bully’s attack is −1 this round.',
  },
  CUSTOMER_TIMID: {
    name: 'Timid',
    description: 'Easily rattled. On trigger, the Bully gains 3 to 6 HP, and the maximum rises too.',
  },
  CUSTOMER_ANXIOUS: {
    name: 'Anxious',
    description: 'On edge. On trigger, the Bully’s attack is +2 to +4 this round.',
  },
  CUSTOMER_WINDOW: {
    name: 'Strolling Pair',
    description: 'They stay together. On trigger, each living guard heals 1.',
  },
  CUSTOMER_WEALTHY: {
    name: 'Wealthy',
    description: 'A generous spender. On trigger, one living guard gains 1 Call.',
  },
  CUSTOMER_HARSH: {
    name: 'Harsh',
    description: 'Sharp-tongued. On trigger, one guard loses 1 Call and 3 HP. Shield does not block it.',
  },
  CUSTOMER_CLEMENT: {
    name: 'Ready Guest',
    description: 'Says OK with a smile. On trigger, each living guard draws 1.',
  },
  CUSTOMER_HOTPOT: {
    name: 'Empty Asker',
    description: 'Asks for a quote and buys elsewhere. On trigger, one living guard discards 1 card.',
  },
  CUSTOMER_DELAYED: {
    name: 'Late Payer',
    description: 'Pays after a wait. On trigger, each living guard gains 4 shield at the next round start.',
  },
  CUSTOMER_JUJUBE: {
    name: 'Jujube Two-Face',
    description: 'Normal matches flip good or bad each round. Good clears the Bully shield. Bad makes Sales’ next card do nothing. Pressure matches keep only the bad face.',
  },
  CUSTOMER_PHONE: {
    name: 'Smile Then Sharp',
    description: 'Normal matches flip good or bad each round. Good doubles the next defense card. Bad makes Purchase defense cards give 0 shield. Pressure matches keep only the bad face.',
  },
}

const TASKS: Record<string, { name: string; description: string }> = {
  'T-DAILY-MATCH-1': {
    name: 'Finish match 1',
    description: 'Finish your 1st match today. Win or loss both count. Forfeiting uses the slot and does not pay this gold.',
  },
  'T-DAILY-WIN-1': {
    name: 'Win match 1',
    description: 'Win today’s 1st match. A win after a revive still counts. Forfeit or a dropped connection uses the slot.',
  },
  'T-DAILY-MATCH-2': {
    name: 'Finish match 2',
    description: 'Finish your 2nd match today. Win or loss both count. Forfeiting uses the slot and does not pay this gold.',
  },
  'T-DAILY-WIN-2': {
    name: 'Win match 2',
    description: 'Win today’s 2nd match. A win after a revive still counts. Forfeit or a dropped connection uses the slot.',
  },
  'T-DAILY-MATCH-3': {
    name: 'Finish match 3',
    description: 'Finish your 3rd match today. Win or loss both count. Forfeiting uses the slot and does not pay this gold.',
  },
  'T-DAILY-WIN-3': {
    name: 'Win match 3',
    description: 'Win today’s 3rd match. A win after a revive still counts. Forfeit or a dropped connection uses the slot.',
  },
  'T-WEEKLY-TEAM-10': {
    name: 'Team with 10 people',
    description: 'In this week’s first 3 daily matches, team with 10 different people. Makeup waits until those 3 are done and does not add a teammate. A forfeited match uses one of those slots. A voided stuck match does not. At most 4 claims a month.',
  },
}

const ACHIEVEMENTS: Record<string, { name: string; description: string }> = {
  'ACH-001': { name: 'First Win', description: 'Win your first match.' },
  'ACH-002': { name: 'Veteran', description: 'Win 10 matches.' },
  'ACH-003': { name: 'Quest Hand', description: 'Claim 20 quests.' },
  'ACH-004': { name: 'Social Lead', description: 'Add 5 friends.' },
  'ACH-005': { name: 'Hidden Egg', description: 'Unlock every achievement.' },
  'ACH-012': { name: 'Steady Output', description: 'Finish 20 matches. Voids do not count.' },
  'ACH-014': { name: 'Three-Day Mark', description: 'Claim gold on 3 work days.' },
  'ACH-016': { name: 'New Partners', description: 'Team with 5 different people in this week’s first 3 daily matches.' },
  'ACH-017': { name: 'Win Streak 3', description: 'Win 3 in a row. A loss or forfeit breaks it. A void does not.' },
  'ACH-018': { name: 'Mixed Team', description: 'Win 5 matches as Sales with Purchase.' },
  'ACH-019': { name: 'Collector', description: 'Unlock 8 gallery cards.' },
  'ACH-020': { name: 'Under Pressure', description: 'Win 1 match teamed with today’s top five.' },
  'ACH-021': { name: 'Fifty Wins', description: 'Win 50 matches.' },
  'ACH-022': { name: 'Full Week', description: 'Claim 5 work days in the same calendar week.' },
  'ACH-023': { name: 'Ten in Ten', description: 'Team with 10 different people in this week’s first 3 daily matches.' },
  'ACH-024': { name: 'Win Streak 5', description: 'Win 5 in a row.' },
  'ACH-025': { name: 'Ten Clean Wins', description: 'Win 10 matches with no revive.' },
  'ACH-026': { name: 'Both Desks', description: 'Win 20 as Sales and 20 as Purchase.' },
  'ACH-027': { name: 'Gallery Half', description: 'Unlock half of the collectible cards.' },
  'ACH-028': { name: 'Five Under Pressure', description: 'Win 5 matches teamed with today’s top five.' },
  'ACH-029': { name: 'Halfway Month', description: 'Claim at least half of this month’s work-day quota.' },
  'ACH-030': { name: 'Hundred Wins', description: 'Win 100 matches.' },
  'ACH-031': { name: 'Win Streak 10', description: 'Win 10 in a row.' },
  'ACH-032': { name: 'October Full', description: 'Claim all 24 work days in October 2026.' },
  'ACH-033': { name: 'Gallery Master', description: 'Unlock every collectible card currently available.' },
  'ACH-034': { name: 'Hidden · Last Stand', description: 'Win after both guards have been at 5 HP or below.' },
  'ACH-035': { name: 'Hidden · Perfect Day', description: 'Finish and win matches 1, 2, and 3 on the same day.' },
  'ACH-036': { name: 'Hidden · Pressure Streak', description: 'Win 3 in a row while teamed with that day’s projected top five.' },
  'ACH-037': { name: 'Hidden · Three Harsh', description: 'Win once each, with no revive, against Timid, Anxious, and Harsh.' },
  'ACH-038': { name: 'Hidden · Low and Clean', description: 'Both guards hit 5 HP or below, nobody revives, and you still win.' },
  'ACH-039': { name: 'Hidden · Both Desks Clean', description: 'Win 10 as Sales and 10 as Purchase with no revive.' },
  'ACH-040': { name: 'Hidden · Quick Clear', description: 'Win within 8 rounds with no revive, 3 times.' },
  'ACH-041': { name: 'Hidden · Perfect Week', description: 'On 5 days of the same week, finish and win matches 1, 2, and 3.' },
  'ACH-042': { name: 'Long Campaign', description: 'Finish 60 matches. Voids do not count.' },
  'ACH-043': { name: 'Quest Veteran', description: 'Claim 60 quest rewards.' },
  'ACH-044': { name: 'Ten Friends', description: 'Add 10 friends.' },
  'ACH-045': { name: 'Three Perfect Days', description: 'In one month, finish and win matches 1, 2, and 3 on 3 different days.' },
  'ACH-046': { name: 'Five Customers', description: 'Win at least once for 5 different customers.' },
  'ACH-047': { name: 'Clutch', description: 'Win 3 matches after your own HP has been 10 or below.' },
}

const EFFECT_PATTERNS: Array<[RegExp, (match: RegExpMatchArray) => string]> = [
  [/^立即对霸凌者造成\s*(\d+)\s*点伤害$/, (m) => `Deal ${m[1]} to the Bully now`],
  [/^为一名玩家增加\s*(\d+)\s*点防御$/, (m) => `Give one guard ${m[1]} Shield`],
  [/^为自己增加\s*(\d+)\s*点防御$/, (m) => `Gain ${m[1]} Shield`],
  [/^本回合对霸凌者造成\s*(\d+)\s*点伤害，下回合再造成\s*(\d+)\s*点伤害$/, (m) => `Deal ${m[1]} to the Bully this round, then ${m[2]} next round`],
  [/^双方各恢复\s*(\d+)\s*点血量$/, (m) => `Both guards heal ${m[1]}`],
  [/^双方各获得\s*(\d+)\s*点防御$/, (m) => `Both guards gain ${m[1]} Shield`],
  [/^恢复一名玩家\s*(\d+)\s*点血量$/, (m) => `Heal one guard ${m[1]}`],
  [/^恢复一名玩家\s*(\d+)\s*点血量，并为霸凌者恢复\s*(\d+)\s*点血量$/, (m) => `Heal one guard ${m[1]}, and heal the Bully ${m[2]}`],
  [/^抽\s*(\d+)\s*张牌$/, (m) => `Draw ${m[1]}`],
  [/^立即造成\s*(\d+)\s*点伤害，并为一名玩家增加\s*(\d+)\s*点防御$/, (m) => `Deal ${m[1]} now and give one guard ${m[2]} Shield`],
  [/^立即造成\s*(\d+)\s*点伤害，并抽\s*(\d+)\s*张牌$/, (m) => `Deal ${m[1]} now and draw ${m[2]}`],
  [/^立即造成\s*(\d+)\s*点伤害，并恢复自己\s*(\d+)\s*点血量$/, (m) => `Deal ${m[1]} now and heal yourself ${m[2]}`],
  [/^本回合为一名玩家增加\s*(\d+)\s*点防御，下回合再增加\s*(\d+)\s*点防御$/, (m) => `Give one guard ${m[1]} Shield this round, then ${m[2]} next round`],
  [/^本回合恢复一名玩家\s*(\d+)\s*点血量，下回合再恢复\s*(\d+)\s*点血量$/, (m) => `Heal one guard ${m[1]} this round, then ${m[2]} next round`],
  [/^本回合员工调用机会\s*\+\s*(\d+)，下回合员工调用机会再\s*\+\s*(\d+)$/, (m) => `Calls +${m[1]} this round, then +${m[2]} next round`],
  [/^本回合员工调用机会\s*\+\s*(\d+)，下回合员工调用机会\s*\+\s*(\d+)$/, (m) => `Calls +${m[1]} this round, then +${m[2]} next round`],
  [/^使自己打出的下一张牌数值效果翻倍$/, () => `Your next card's numbers are doubled`],
  [/^霸凌者血量\+3～6$/, () => 'Bully HP +3 to 6'],
  [/^霸凌者基础攻击\+2～4$/, () => 'Bully attack +2 to 4'],
  [/^两名存活护卫各抽 1 张牌$/, () => 'Each living guard draws 1'],
  [/^一名玩家调用机会-1且失血(\d+)$/, (m) => `One guard loses 1 Call and ${m[1]} HP`],
  [/^我方血值\+(\d+)$/, (m) => `Guards +${m[1]} HP`],
  [/^一名玩家调用机会\+(\d+)$/, (m) => `One guard Call +${m[1]}`],
  [/^霸凌者血量\+(\d+)$/, (m) => `Bully HP +${m[1]}`],
  [/^霸凌者基础攻击([+-]\d+)$/, (m) => `Bully attack ${m[1]}`],
  [/^容易紧张。效果触发时，霸凌者血量随机 \+3～6（上限同步提高）。$/, () => CUSTOMERS.CUSTOMER_TIMID.description],
  [/^情绪波动较大。效果触发时，本回合霸凌者攻击随机 \+2～4。$/, () => CUSTOMERS.CUSTOMER_ANXIOUS.description],
  [/^结伴闲逛却从不落单。效果触发时，给两名存活护卫各恢复 1 点血值。$/, () => CUSTOMERS.CUSTOMER_WINDOW.description],
  [/^调用机会-1且失血(\d+)$/, (m) => `Call −1 and lose ${m[1]} HP`],
  [/^调用机会([+-]\d+)$/, (m) => `Call ${m[1]}`],
  [/^血值([+-]\d+)$/, (m) => `HP ${m[1]}`],
  [/^攻击([+-]\d+)$/, (m) => `Attack ${m[1]}`],
  [/^HP([+-]\d+)$/, (m) => `HP ${m[1]}`],
]

export function localizeText(zh: string): string {
  return t(zh)
}

export function localizeCardName(name?: string | null): string {
  const raw = String(name ?? '')
  if (lang.value !== 'en' || !raw) return raw
  return CARD_NAME_EN[raw] ?? t(raw)
}

export function localizeEffect(text?: string | null): string {
  const raw = String(text ?? '')
  if (lang.value !== 'en' || !raw) return raw
  const known = t(raw)
  if (known !== raw) return known
  const templated = translateEffectTemplate(raw)
  if (templated) return templated
  for (const [pattern, format] of EFFECT_PATTERNS) {
    const match = raw.match(pattern)
    if (match) return format(match)
  }
  return raw
}

export function localizeCustomer(input: { code?: string | null; name?: string | null; description?: string | null }) {
  const name = input.name || ''
  const description = input.description || ''
  if (lang.value !== 'en') return { name, description }
  const byCode = input.code ? CUSTOMERS[input.code] : undefined
  if (byCode) return byCode
  return {
    name: t(name) === name ? name : t(name),
    description: localizeEffect(description),
  }
}

export function localizeTask(code: string | undefined, name: string, description: string) {
  if (lang.value !== 'en') return { name, description }
  return TASKS[code || ''] || { name, description: localizeEffect(description) }
}

export function localizeAchievement(id: string | undefined, name: string, description: string) {
  if (lang.value !== 'en') return { name, description }
  return ACHIEVEMENTS[id || ''] || { name, description }
}
