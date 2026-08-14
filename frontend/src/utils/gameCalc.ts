// 护盾抵扣伤害
export function calcShieldDeflect(damage: number, shield: number): { remainingShield: number; actualDamage: number } {
  const actualDamage = Math.max(0, damage - shield)
  const remainingShield = Math.max(0, shield - damage)
  return { remainingShield, actualDamage }
}

// 胜负判定
export function checkWinCondition(bullyHP: number, player1Stamina: number, player2Stamina: number): 'win' | 'lose' | 'ongoing' {
  if (bullyHP <= 0) return 'win'
  if (player1Stamina <= 0 && player2Stamina <= 0) return 'lose'
  return 'ongoing'
}

// 酬劳积分计算
export function calcPointsFromDamage(totalDamage: number, isVictory: boolean): number {
  return isVictory ? totalDamage + 100 : 0
}

// 每回合资金
export function getTurnFunds(): number {
  return 3
}
