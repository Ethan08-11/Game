/** 图鉴和牌面上，只有采购部 Duane 这张守护牌显示「守护」。 */
export function isPurchaseDuaneGuard(card: {
  cardCode?: string | null
  cardName?: string | null
  name?: string | null
  description?: string | null
  imageUrl?: string | null
  type?: string | null
  cardType?: string | null
}): boolean {
  const type = (card.cardType ?? card.type ?? '').trim()
  if (type !== 'defend') return false
  const code = (card.cardCode || '').trim().toUpperCase()
  if (code === 'P-36') return true
  const name = (card.cardName || card.name || '').trim()
  if (name !== 'Duane') return false
  const image = card.imageUrl || ''
  if (image.includes('采购_Duane')) return true
  return (card.description || '').includes('队友有盾时先扣盾')
}
