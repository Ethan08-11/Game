/** 图鉴和顾客卡片上的固定说明。对局里仍显示当回合随机到的具体数字。 */
export function rangedCustomerEffectText(input: { id?: string | null; name?: string | null }): string | null {
  const id = input.id || ''
  const name = input.name || ''
  if (id === 'CUSTOMER_TIMID' || name.includes('胆小怕事')) return '霸凌者血量+3～6'
  if (id === 'CUSTOMER_ANXIOUS' || name.includes('焦虑难安')) return '霸凌者基础攻击+2～4'
  return null
}

export function customerEffectSummary(input: {
  id?: string | null
  name?: string | null
  effectType?: string | null
}): string | null {
  const ranged = rangedCustomerEffectText(input)
  if (ranged) return ranged
  if (input.effectType === 'player_draw') return '两名存活护卫各抽 1 张牌'
  if (input.effectType === 'player_discard') return '随机一名存活护卫弃掉 1 张牌'
  if (input.effectType === 'player_shield_next') return '下回合两名存活护卫各得 4 点护盾'
  if (input.effectType === 'dual_jujube') return '好面清盾；坏面让销售的下一张牌无效'
  if (input.effectType === 'dual_phone') return '好面防御翻倍；坏面采购的防御牌没有护盾'
  return null
}
