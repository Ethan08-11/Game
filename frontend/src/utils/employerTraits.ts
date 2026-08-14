import type { EmployerTrait } from '@/types/gameEntities'

export const EMPLOYER_TRAITS: EmployerTrait[] = [
  {
    id: 'cooperative',
    name: '配合型',
    description: '雇主积极配合，大概率帮打霸凌者',
    helpChance: 0.7,
    helpMin: 10,
    helpMax: 20,
    hinderMin: 5,
    hinderMax: 10,
  },
  {
    id: 'anxious',
    name: '焦虑型',
    description: '雇主情绪焦虑，容易添乱',
    helpChance: 0.4,
    helpMin: 5,
    helpMax: 10,
    hinderMin: 10,
    hinderMax: 15,
  },
  {
    id: 'assertive',
    name: '强势型',
    description: '雇主非常强势，大概率重击霸凌者',
    helpChance: 0.8,
    helpMin: 15,
    helpMax: 25,
    hinderMin: 5,
    hinderMax: 5,
  },
  {
    id: 'hesitant',
    name: '犹豫型',
    description: '雇主犹豫不决，帮倒忙概率各半',
    helpChance: 0.5,
    helpMin: 5,
    helpMax: 15,
    hinderMin: 5,
    hinderMax: 15,
  },
]

export function rollEmployerTrait(): EmployerTrait {
  const idx = Math.floor(Math.random() * EMPLOYER_TRAITS.length)
  return EMPLOYER_TRAITS[idx]
}

export function getEmployerAction(trait: EmployerTrait): { helped: boolean; amount: number; message: string } {
  const helped = Math.random() < trait.helpChance
  if (helped) {
    const amount = trait.helpMin + Math.floor(Math.random() * (trait.helpMax - trait.helpMin + 1))
    return { helped: true, amount, message: `雇主帮忙！霸凌者受到 ${amount} 点额外伤害` }
  } else {
    const amount = trait.hinderMin + Math.floor(Math.random() * (trait.hinderMax - trait.hinderMin + 1))
    return { helped: false, amount, message: `雇主添乱...霸凌者恢复了 ${amount} 点血量` }
  }
}
