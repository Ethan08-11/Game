import defaultAvatar from '@/assets/default-avatar.webp'
import { PRESET_AVATAR_SRC } from '@/data/avatars'

export function resolveAvatarSrc(url?: string | null): string {
  if (!url) return defaultAvatar
  const trimmed = url.trim()
  if (!trimmed) return defaultAvatar
  if (PRESET_AVATAR_SRC[trimmed]) return PRESET_AVATAR_SRC[trimmed]
  if (/^(https?:|data:|blob:)/i.test(trimmed)) return trimmed
  return trimmed.startsWith('/') ? trimmed : `/${trimmed}`
}

/** 夜鸮素材 421x632，contain 后圆形约占格子的比例 */
export const OWL_SIZE_RATIO = 421 / 632

export function isDefaultOwl(url?: string | null): boolean {
  return !url || url.includes('/images/avatars/default')
}

const CIRCLE_FILL: Record<string, number> = {
  '/images/avatars/fox.webp': 0.652,
  '/images/avatars/cat.webp': 0.794,
  '/images/avatars/suit-man.webp': 0.739,
  '/images/avatars/suit-woman.webp': 0.812,
  '/images/avatars/craftsman.webp': 0.791,
  '/images/avatars/scholar.webp': 0.765,
  '/images/avatars/merchant.webp': 0.743,
  '/images/avatars/trader.webp': 0.710,
}

export function avatarCropScale(url?: string | null): number {
  if (!url || isDefaultOwl(url)) return 1
  const fill = CIRCLE_FILL[url] ?? 0.75
  return 1 / fill
}
