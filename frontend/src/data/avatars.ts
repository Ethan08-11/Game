import defaultAvatar from '@/assets/default-avatar.webp'
import fox from '@/assets/avatars/fox.webp'
import cat from '@/assets/avatars/cat.webp'
import suitMan from '@/assets/avatars/suit-man.webp'
import suitWoman from '@/assets/avatars/suit-woman.webp'
import craftsman from '@/assets/avatars/craftsman.webp'
import scholar from '@/assets/avatars/scholar.webp'
import merchant from '@/assets/avatars/merchant.webp'
import trader from '@/assets/avatars/trader.webp'
import customer1 from '@/assets/avatars/customer-1.webp'
import customer2 from '@/assets/avatars/customer-2.webp'
import customer3 from '@/assets/avatars/customer-3.webp'

export interface AvatarPreset {
  id: string
  name: string
  /** 写入后端 users.avatar_url 的稳定路径 */
  url: string
}

/** 稳定路径 → 打包后的真实资源，避免开发时 /images 被代理到后端 */
export const PRESET_AVATAR_SRC: Record<string, string> = {
  '/images/avatars/default.webp': defaultAvatar,
  '/images/avatars/fox.webp': fox,
  '/images/avatars/cat.webp': cat,
  '/images/avatars/suit-man.webp': suitMan,
  '/images/avatars/suit-woman.webp': suitWoman,
  '/images/avatars/craftsman.webp': craftsman,
  '/images/avatars/scholar.webp': scholar,
  '/images/avatars/merchant.webp': merchant,
  '/images/avatars/trader.webp': trader,
  '/images/customer/p1.webp': customer1,
  '/images/customer/p2.webp': customer2,
  '/images/customer/p3.webp': customer3,
}

export const PRESET_AVATARS: AvatarPreset[] = [
  { id: 'owl', name: '夜鸮', url: '/images/avatars/default.webp' },
  { id: 'fox', name: '金狐', url: '/images/avatars/fox.webp' },
  { id: 'cat', name: '团子猫', url: '/images/avatars/cat.webp' },
  { id: 'suit-man', name: '西装小伙', url: '/images/avatars/suit-man.webp' },
  { id: 'suit-woman', name: '职场短发', url: '/images/avatars/suit-woman.webp' },
  { id: 'craftsman', name: '护单工匠', url: '/images/avatars/craftsman.webp' },
  { id: 'scholar', name: '核对专员', url: '/images/avatars/scholar.webp' },
  { id: 'merchant', name: '老练客商', url: '/images/avatars/merchant.webp' },
  { id: 'trader', name: '行商姑娘', url: '/images/avatars/trader.webp' },
  { id: 'customer-1', name: '暖心雇主', url: '/images/customer/p1.webp' },
  { id: 'customer-2', name: '眼镜行家', url: '/images/customer/p2.webp' },
  { id: 'customer-3', name: '红巾客官', url: '/images/customer/p3.webp' },
]
