export interface EmployerTrait {
  id: string
  name: string
  description: string
  helpChance: number
  helpMin: number
  helpMax: number
  hinderMin: number
  hinderMax: number
  effectType?: 'attack' | 'hp'
  effectValue?: number
  typeTriggerRate?: number
  effectTriggerRate?: number
  imageUrl?: string | null
}

export interface BullyState {
  name: string
  minDamage: number
  maxDamage: number
  description: string
}
