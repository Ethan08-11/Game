// 洗牌算法
export function shuffle<T>(arr: T[]): T[] {
  const result = [...arr]
  for (let i = result.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[result[i], result[j]] = [result[j], result[i]]
  }
  return result
}

// 随机分配部门
export function randomDept(): string {
  const depts = ['财务', '人事', '采购', '销售', 'IT', '设计', '行政', '物流']
  return depts[Math.floor(Math.random() * depts.length)]
}

// BOSS 50%概率随机选择攻击目标
export function randomTarget(): 'player1' | 'player2' {
  return Math.random() < 0.5 ? 'player1' : 'player2'
}
