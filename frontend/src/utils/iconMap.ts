// ============================================================
// 图标映射 — 语义名称 → Element Plus 图标组件
// 所有 Emoji 统一替换为专业图标
// ============================================================

import {
  Switch, Aim, Download, Delete, Coin, Trophy, Lock, Check, Plus,
  Box, WarningFilled, Present, CircleCloseFilled, CaretTop, CaretBottom,
  QuestionFilled, Tickets, Medal, Star, StarFilled, Clock, MagicStick,
  Refresh, ArrowRight, ArrowLeft, DArrowLeft, Position, Promotion,
} from '@element-plus/icons-vue'
import type { Component } from 'vue'

const iconMap: Record<string, Component> = {
  // 卡牌类型
  defend: Switch,
  attack: Aim,
  draw: Download,
  consume: Delete,

  // 通用 UI
  funds: Coin,
  coin: Coin,
  trophy: Trophy,
  lock: Lock,
  check: Check,
  plus: Plus,
  close: CircleCloseFilled,
  question: QuestionFilled,
  box: Box,
  warning: WarningFilled,
  present: Present,
  medal: Medal,
  star: Star,
  starFilled: StarFilled,
  tickets: Tickets,
  clock: Clock,
  magic: MagicStick,
  refresh: Refresh,
  promotion: Promotion,

  // 方向
  arrowRight: ArrowRight,
  arrowLeft: ArrowLeft,
  arrowDown: CaretBottom,
  arrowUp: CaretTop,
  caretTop: CaretTop,
  caretBottom: CaretBottom,
  caretLeft: DArrowLeft,

  // 状态
  success: Check,
  danger: CircleCloseFilled,
  target: Position,
}

export function getIcon(name: string): Component | undefined {
  return iconMap[name]
}

export function getIconName(emoji: string): string {
  const emojiMap: Record<string, string> = {
    '🛡': 'defend',
    '🛡️': 'defend',
    '⚔': 'attack',
    '⚔️': 'attack',
    '📥': 'draw',
    '🗑': 'consume',
    '💰': 'funds',
    '👈': 'arrowLeft',
    '🎉': 'present',
    '💀': 'danger',
    '📦': 'box',
    '⚠': 'warning',
    '⚠️': 'warning',
    '🏆': 'trophy',
    '🎴': 'tickets',
    '🔒': 'lock',
    '✓': 'check',
    '▲': 'caretTop',
    '▼': 'caretBottom',
    '❓': 'question',
    '➕': 'plus',
    '🔥': 'promotion',
    '✨': 'magic',
    '⚡': 'magic',
    '💯': 'medal',
    '👥': 'star',
    '🤝': 'promotion',
    '🃏': 'tickets',
  }
  return emojiMap[emoji] || 'star'
}

export function getSkinIcon(name: string): Component {
  return iconMap[name] || iconMap.star
}

export default iconMap
