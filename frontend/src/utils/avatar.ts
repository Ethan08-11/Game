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
  '/images/avatars/messy-glasses.webp': 0.820,
  '/images/avatars/curtain-blazer.webp': 0.790,
  '/images/avatars/double-glasses.webp': 0.780,
  '/images/avatars/batik-shirt.webp': 0.765,
  '/images/avatars/magenta-bob.webp': 0.885,
  '/images/avatars/pearl-glasses.webp': 0.865,
  '/images/avatars/ear-pull-glasses.webp': 0.860,
  '/images/avatars/bluegray-toast.webp': 0.885,
  '/images/avatars/ombre-skewer.webp': 0.900,
  '/images/avatars/toothless-peace.webp': 0.910,
  '/images/avatars/curly-leather.webp': 0.850,
  '/images/avatars/fluffy-stripe.webp': 0.855,
  '/images/avatars/beanie-stripe.webp': 0.900,
  '/images/avatars/purple-cap-braids.webp': 0.910,
  '/images/avatars/apple-dress.webp': 0.905,
  '/images/avatars/purple-offshoulder.webp': 0.860,
  '/images/avatars/birthday-cake.webp': 0.900,
  '/images/avatars/stripe-touch-hair.webp': 0.875,
  '/images/avatars/mesh-white-tee.webp': 0.900,
  '/images/avatars/cowboy-hat.webp': 0.910,
  '/images/avatars/stripe-glasses-guy.webp': 0.875,
  '/images/avatars/blonde-cake.webp': 0.900,
  '/images/avatars/flower-offshoulder.webp': 0.930,
  '/images/avatars/chef-rat.webp': 0.900,
  '/images/avatars/navy-vest.webp': 0.880,
  '/images/avatars/pink-shrug.webp': 0.900,
  '/images/avatars/bangs-glasses.webp': 0.885,
}

export function avatarCropScale(url?: string | null): number {
  if (!url || isDefaultOwl(url)) return 1
  const fill = CIRCLE_FILL[url] ?? 0.75
  return 1 / fill
}
