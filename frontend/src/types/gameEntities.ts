export interface EmployerTrait {
  id: string
  name: string
  description: string
  helpChance: number
  helpMin: number
  helpMax: number
  hinderMin: number
  hinderMax: number
  effectType?: 'attack' | 'hp' | 'player_hp'
  effectValue?: number
  typeTriggerRate?: number
  effectTriggerRate?: number
  imageUrl?: string | null
  bullyName?: string | null
  bullyDescription?: string | null
  bullySkillSummary?: string | null
  bullySkillChance?: number | null
}

export interface BullyState {
  name: string
  minDamage: number
  maxDamage: number
  description: string
}
