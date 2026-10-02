/** 图鉴和顾客卡片上的固定说明。对局里仍显示当回合随机到的具体数字。 */
export function rangedCustomerEffectText(input: { id?: string | null; name?: string | null }): string | null {
  const id = input.id || ''
  const name = input.name || ''
  if (id === 'CUSTOMER_TIMID' || name.includes('胆小怕事')) return '霸凌者血量+3～4'
  if (id === 'CUSTOMER_ANXIOUS' || name.includes('焦虑难安')) return '霸凌者基础攻击+2～3'
  return null
}
