export interface Card {
  id: string
  name: string
  dept: string
  cost: 0 | 1 | 2 | 3
  type: 'attack' | 'defend' | 'draw' | 'consume' | 'support'
  damage: number
  shield: number
  description: string
  comboCardId: string | null
}

// 52 张完整卡牌待录入，当前 18 张示例
export const allCards: Card[] = [
  // 销售部 (6张)
  { id: 'c_sales_1', name: '推销话术', dept: '销售部', cost: 1, type: 'attack', damage: 8, shield: 0, description: '基础推销，造成8点伤害', comboCardId: null },
  { id: 'c_sales_2', name: '价格谈判', dept: '销售部', cost: 2, type: 'attack', damage: 15, shield: 0, description: '强力谈判，造成15点伤害', comboCardId: null },
  { id: 'c_sales_3', name: '需求分析', dept: '销售部', cost: 2, type: 'attack', damage: 12, shield: 0, description: '分析客户需求，造成12点伤害', comboCardId: null },
  { id: 'c_sales_4', name: '客户拜访', dept: '销售部', cost: 0, type: 'attack', damage: 5, shield: 0, description: '免费拜访，造成5点伤害', comboCardId: null },
  { id: 'c_sales_5', name: '最终报价', dept: '销售部', cost: 3, type: 'attack', damage: 20, shield: 0, description: '强力一击，造成20点伤害', comboCardId: null },
  { id: 'c_sales_6', name: '客户关系', dept: '销售部', cost: 2, type: 'attack', damage: 10, shield: 2, description: '维护客户关系，伤害+10，护盾+2', comboCardId: null },
  // 采购部 (6张)
  { id: 'c_purch_1', name: '合同防守', dept: '采购部', cost: 1, type: 'defend', damage: 0, shield: 5, description: '获得5点护盾', comboCardId: null },
  { id: 'c_purch_2', name: '供应链优化', dept: '采购部', cost: 3, type: 'defend', damage: 0, shield: 8, description: '优化供应链，获得8点护盾', comboCardId: null },
  { id: 'c_purch_3', name: '砍价技巧', dept: '采购部', cost: 1, type: 'attack', damage: 7, shield: 0, description: '砍价，造成7点伤害', comboCardId: null },
  { id: 'c_purch_4', name: '市场调研', dept: '采购部', cost: 1, type: 'draw', damage: 0, shield: 0, description: '抽2张牌', comboCardId: null },
  { id: 'c_purch_5', name: '供应商筛选', dept: '采购部', cost: 2, type: 'attack', damage: 10, shield: 3, description: '伤害+10，护盾+3', comboCardId: null },
  { id: 'c_purch_6', name: '成本控制', dept: '采购部', cost: 2, type: 'defend', damage: 0, shield: 6, description: '控制成本，获得6点护盾', comboCardId: null },
  // 财务部 (3张)
  { id: 'c_fin_1', name: '报价审核', dept: '财务部', cost: 1, type: 'defend', damage: 0, shield: 4, description: '审核报价，获得4点护盾', comboCardId: null },
  { id: 'c_fin_2', name: '预算审批', dept: '财务部', cost: 2, type: 'attack', damage: 11, shield: 0, description: '审批预算，造成11点伤害', comboCardId: null },
  { id: 'c_fin_3', name: '税务优化', dept: '财务部', cost: 3, type: 'attack', damage: 18, shield: 0, description: '税务方案，造成18点伤害', comboCardId: null },
  // 物流部 (3张)
  { id: 'c_logi_1', name: '物流调度', dept: '物流部', cost: 1, type: 'draw', damage: 0, shield: 0, description: '调度物流，抽2张牌', comboCardId: null },
  { id: 'c_logi_2', name: '通关加速', dept: '物流部', cost: 2, type: 'attack', damage: 10, shield: 2, description: '加速通关，伤害+10，护盾+2', comboCardId: null },
  { id: 'c_logi_3', name: '仓储管理', dept: '物流部', cost: 1, type: 'defend', damage: 0, shield: 3, description: '管理仓储，获得3点护盾', comboCardId: null },
]

export function getCardsByDept(dept: string): Card[] {
  return allCards.filter(c => c.dept === dept)
}

export function getCardById(id: string): Card | undefined {
  return allCards.find(c => c.id === id)
}
