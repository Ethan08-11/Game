export interface MakeupOffer {
  available?: boolean
  owedDay?: string | null
  slot?: number
  finishGold?: number
  winGold?: number
  usedToday?: boolean
  paysGold?: boolean
  owedCount?: number
}

export function makeupLine(offer: MakeupOffer | null | undefined, english: boolean): string {
  if (!offer || !offer.owedCount || !offer.slot) return ''
  const when = formatOwedDay(offer.owedDay, english)
  const slot = offer.slot
  const left = offer.owedCount
  if (offer.usedToday) {
    return english
      ? `Today's makeup is used. ${left} still owed. Next is ${when}, match ${slot}.`
      : `今天的补打已经用过。还欠 ${left} 局，下一局是${when}第 ${slot} 局，明天可补。`
  }
  if (offer.paysGold === false) {
    return english
      ? `${when}, match ${slot} can be made up today. The work-day quota is full, so this one pays no gold. ${left} still owed.`
      : `还可补${when}第 ${slot} 局。本月工作日已满，这局补打不发金币。今天可补 1 局，开局前勾选。还欠 ${left} 局。`
  }
  const finish = offer.finishGold ?? 0
  const win = offer.winGold ?? 10
  return english
    ? `${when}, match ${slot}: ${finish} for finishing, plus ${win} if you win. It still counts as today’s win or loss. Check “makeup” before the match. ${left} still owed.`
    : `还可补${when}第 ${slot} 局：打完 ${finish}，赢了再加 ${win}。这局仍算今天的胜负。今天可补 1 局，开局前勾选。还欠 ${left} 局。`
}

function formatOwedDay(value: string | null | undefined, english: boolean): string {
  if (!value) return english ? 'an earlier day' : '更早一天'
  const parts = value.split('-')
  if (parts.length !== 3) return value
  const month = Number(parts[1])
  const day = Number(parts[2])
  if (!month || !day) return value
  return english ? `${month}/${day}` : `${month}月${day}日`
}
