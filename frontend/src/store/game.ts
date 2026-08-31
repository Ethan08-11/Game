import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getCurrentCustomer, getGameConfig, submitGameResult } from '@/api'
import { rollEmployerTrait, getEmployerAction } from '@/utils/employerTraits'
import { randomBullyName, randomEmployerName } from '@/utils/bullyData'
import type { EmployerTrait } from '@/types/gameEntities'

export const useGameStore = defineStore('game', () => {
  const teamFunds = ref<number>(0)
  const bullyHP = ref<number>(110)
  const maxBullyHP = ref<number>(110)
  const totalDamageDealt = ref<number>(0)
  const turnFunds = ref<number>(3)
  const currentTurnFunds = ref<number>(3)
  const pointsEarned = ref<number>(0)

  const player1Stamina = ref<number>(100)
  const player2Stamina = ref<number>(100)

  const deck = ref<any[]>([])
  const hand = ref<any[]>([])
  const discardPile = ref<any[]>([])
  const removedCards = ref<any[]>([])

  const bullyName = ref<string>('霸凌者')
  const bullyMinDamage = ref<number>(5)
  const bullyMaxDamage = ref<number>(14)
  const bullyDebuff = ref<string>('')
  const bullyTarget = ref<string>('')
  const shield = ref<number>(0)
  const bullyDefense = ref<number>(0)
  const comboActive = ref<boolean>(false)

  const employerName = ref<string>('雇主')
  const employerTrait = ref<EmployerTrait | null>(null)
  const employerLastAction = ref<string>('')

  const isVictory = ref<boolean>(false)
  const isGameOver = ref<boolean>(false)

  async function loadGameConfig() {
    try {
      const config = await getGameConfig()
      turnFunds.value = config.turnFunds
      currentTurnFunds.value = config.turnFunds
      maxBullyHP.value = config.maxBullyHP
      bullyMinDamage.value = config.bullyMinDamage ?? config.bossMinDamage
      bullyMaxDamage.value = config.bullyMaxDamage ?? config.bossMaxDamage
    } catch { /* 使用默认值 */ }
  }

  function initGameEntities() {
    employerTrait.value = rollEmployerTrait()
    employerName.value = randomEmployerName()
    bullyName.value = randomBullyName()
    employerLastAction.value = ''
  }

  async function loadCurrentCustomer() {
    try {
      employerTrait.value = await getCurrentCustomer()
      employerName.value = employerTrait.value.name
    } catch {
      if (!employerTrait.value) initGameEntities()
    }
  }

  function startTurn() {
    currentTurnFunds.value = turnFunds.value
  }

  function spendFunds(amount: number): boolean {
    if (currentTurnFunds.value < amount) return false
    currentTurnFunds.value -= amount
    return true
  }

  function damageBully(amount: number, combo: boolean) {
    const damage = combo ? amount * 2 : amount
    const blocked = Math.min(bullyDefense.value, damage)
    bullyDefense.value -= blocked
    const hpDamage = damage - blocked
    bullyHP.value = Math.max(0, bullyHP.value - hpDamage)
    totalDamageDealt.value += hpDamage
    if (bullyHP.value <= 0) {
      isVictory.value = true
      isGameOver.value = true
      pointsEarned.value = 0
    }
  }

  function healBully(amount: number) {
    bullyHP.value = Math.min(maxBullyHP.value, bullyHP.value + amount)
  }

  function applyEmployerTrait(): string {
    if (!employerTrait.value) return ''
    const action = getEmployerAction(employerTrait.value)
    if (action.helped) {
      damageBully(action.amount, false)
    } else {
      healBully(action.amount)
    }
    employerLastAction.value = action.message
    return action.message
  }

  function damagePlayer(playerIndex: 0 | 1, amount: number) {
    if (playerIndex === 0) {
      player1Stamina.value = Math.max(0, player1Stamina.value - amount)
    } else {
      player2Stamina.value = Math.max(0, player2Stamina.value - amount)
    }
    if (player1Stamina.value <= 0 && player2Stamina.value <= 0) {
      isGameOver.value = true
      isVictory.value = false
    }
  }

  function resetGameOver() {
    isVictory.value = false
    isGameOver.value = false
  }

  function resetGame() {
    teamFunds.value = 0
    bullyHP.value = maxBullyHP.value
    totalDamageDealt.value = 0
    currentTurnFunds.value = turnFunds.value
    pointsEarned.value = 0
    player1Stamina.value = 100
    player2Stamina.value = 100
    deck.value = []
    hand.value = []
    discardPile.value = []
    removedCards.value = []
    bullyDebuff.value = ''
    bullyTarget.value = ''
    shield.value = 0
    bullyDefense.value = 0
    comboActive.value = false
    isVictory.value = false
    isGameOver.value = false
    employerLastAction.value = ''
    initGameEntities()
  }

  async function submitResult(rounds: number): Promise<number> {
    try {
      await submitGameResult({
        totalDamage: totalDamageDealt.value,
        player1Stamina: player1Stamina.value,
        player2Stamina: player2Stamina.value,
        rounds,
        isVictory: isVictory.value,
      })
      pointsEarned.value = 0
      return 0
    } catch {
      pointsEarned.value = 0
      return 0
    }
  }

  return {
    teamFunds, bullyHP, maxBullyHP, totalDamageDealt, turnFunds, currentTurnFunds, pointsEarned,
    player1Stamina, player2Stamina,
    deck, hand, discardPile, removedCards,
    bullyName, bullyMinDamage, bullyMaxDamage, bullyDebuff, bullyTarget, shield, bullyDefense,
    employerName, employerTrait, employerLastAction,
    comboActive, isVictory, isGameOver,
    loadGameConfig, initGameEntities, loadCurrentCustomer, startTurn, spendFunds,
    damageBully, healBully, applyEmployerTrait,
    damagePlayer, resetGameOver, resetGame, submitResult,
  }
})
