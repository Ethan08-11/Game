export interface MakeupOffer {
  available?: boolean
  owedDay?: string | null
  slot?: number
  finishGold?: number
  winGold?: number
  usedToday?: boolean
  paysGold?: boolean
  owedCount?: number
  partialDebt?: boolean
  monthClear?: boolean
  dailyReady?: boolean
  questDay?: string | null
  questSlot?: number
  questFinishGold?: number
  questWinGold?: number
  finishReady?: boolean
  winReady?: boolean
}

export function makeupLine(offer: MakeupOffer | null | undefined, english: boolean): string {
  if (!offer) return ''
  const when = formatOwedDay(offer.owedDay, english)
  const slot = offer.slot || 0
  const left = offer.owedCount || 0
  const finish = offer.finishGold ?? 0
  const win = offer.winGold ?? 10
  if (offer.monthClear === true && offer.partialDebt === false && slot === 1 && left >= 3) {
    return optionalRecentDay(offer, when, finish, win, english)
  }
  const owed = owedSentence(offer, when, left, english)
  if (offer.dailyReady === false && slot > 0) {
    return english
      ? `Finish today's 3 matches before makeup. Next is ${when}, match ${slot}: claim ${finish}, plus ${win} for a win. ${owed}`
      : `今天打满 3 局后才能补打。下一档是${when}第 ${slot} 局：打完领 ${finish}，赢了再领 ${win}。${owed}`
  }
  if (!slot || !left) {
    return offer.finishReady || offer.winReady
      ? (english ? 'Makeup gold is waiting on the quest list.' : '补打奖励在任务列表里领取。')
      : ''
  }
  if (offer.usedToday) {
    return english
      ? `Today's makeup is used. ${owed} Next is ${when}, match ${slot}: claim ${finish}, plus ${win} for a win.`
      : `今天的补打已经用过。${owed}下一局是${when}第 ${slot} 局，打完领 ${finish}，赢了再领 ${win}。`
  }
  if (offer.paysGold === false) {
    return english
      ? `${when}, match ${slot} can be made up today. The work-day quota is full, so this one pays no gold. ${owed}`
      : `还可补${when}第 ${slot} 局。本月工作日已满，这局补打不发金币。今天 3 局已满，开局前勾选。${owed}`
  }
  return english
    ? `${when}, match ${slot}: claim ${finish} for finishing, plus ${win} if you win. Today's 3 matches are done. Check “makeup” before the match. ${owed}`
    : `还可补${when}第 ${slot} 局：打完领 ${finish}，赢了再领 ${win}。今天 3 局已满，开局前勾选。${owed}`
}

function optionalRecentDay(offer: MakeupOffer, when: string, finish: number, win: number, english: boolean): string {
  const choice = english
    ? `Played normally this month, nothing owed. You can make up the most recent day, ${when}: claim ${finish}, plus ${win} for a win.`
    : `本月正常打，不欠。可补最近的一天${when}：打完领 ${finish}，赢了再领 ${win}。`
  if (offer.dailyReady === false) {
    return english ? `Finish today's 3 matches before makeup. ${choice}` : `今天打满 3 局后才能补打。${choice}`
  }
  if (offer.paysGold === false) {
    return english
      ? `Played normally this month, nothing owed. You can make up the most recent day, ${when}. The work-day quota is full, so it pays no gold.`
      : `本月正常打，不欠。可补最近的一天${when}。本月工作日已满，这局补打不发金币。`
  }
  if (offer.usedToday) {
    return english ? `Today's makeup is used. ${choice}` : `今天的补打已经用过。${choice}`
  }
  return english
    ? `${choice} Today's 3 matches are done. Check “makeup” before the match.`
    : `${choice}今天 3 局已满，开局前勾选。`
}

function owedSentence(offer: MakeupOffer, when: string, left: number, english: boolean): string {
  if (offer.partialDebt === false) {
    return english
      ? `Making up the most recent day, ${when}. ${left} still left.`
      : `补最近的一天${when}，还欠 ${left} 局。`
  }
  return english ? `${left} missed matches still owed.` : `还欠 ${left} 局。`
}

export function formatOwedDay(value: string | null | undefined, english: boolean): string {
  if (!value) return english ? 'an earlier day' : '更早一天'
  const parts = value.split('-')
  if (parts.length !== 3) return value
  const month = Number(parts[1])
  const day = Number(parts[2])
  if (!month || !day) return value
  return english ? `${month}/${day}` : `${month}月${day}日`
}
