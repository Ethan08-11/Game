export interface EmployerTrait {
  id: string
  name: string
  description: string
  helpChance: number
  helpMin: number
  helpMax: number
  hinderMin: number
  hinderMax: number
  effectType?: 'attack' | 'hp' | 'player_hp' | 'player_action' | 'player_harsh' | 'player_draw'
    | 'player_discard' | 'player_shield_next' | 'clear_boss_shield' | 'nullify_next_card'
    | 'zero_defense_shield' | 'multiply_next_shield' | 'dual_jujube' | 'dual_phone'
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
