import defaultAvatar from '@/assets/default-avatar.webp'
import fox from '@/assets/avatars/fox.webp'
import cat from '@/assets/avatars/cat.webp'
import suitMan from '@/assets/avatars/suit-man.webp'
import suitWoman from '@/assets/avatars/suit-woman.webp'
import craftsman from '@/assets/avatars/craftsman.webp'
import scholar from '@/assets/avatars/scholar.webp'
import merchant from '@/assets/avatars/merchant.webp'
import trader from '@/assets/avatars/trader.webp'
import messyGlasses from '@/assets/avatars/messy-glasses.webp'
import curtainBlazer from '@/assets/avatars/curtain-blazer.webp'
import doubleGlasses from '@/assets/avatars/double-glasses.webp'
import batikShirt from '@/assets/avatars/batik-shirt.webp'
import magentaBob from '@/assets/avatars/magenta-bob.webp'
import pearlGlasses from '@/assets/avatars/pearl-glasses.webp'
import earPullGlasses from '@/assets/avatars/ear-pull-glasses.webp'
import bluegrayToast from '@/assets/avatars/bluegray-toast.webp'
import ombreSkewer from '@/assets/avatars/ombre-skewer.webp'
import toothlessPeace from '@/assets/avatars/toothless-peace.webp'
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
  '/images/avatars/messy-glasses.webp': messyGlasses,
  '/images/avatars/curtain-blazer.webp': curtainBlazer,
  '/images/avatars/double-glasses.webp': doubleGlasses,
  '/images/avatars/batik-shirt.webp': batikShirt,
  '/images/avatars/magenta-bob.webp': magentaBob,
  '/images/avatars/pearl-glasses.webp': pearlGlasses,
  '/images/avatars/ear-pull-glasses.webp': earPullGlasses,
  '/images/avatars/bluegray-toast.webp': bluegrayToast,
  '/images/avatars/ombre-skewer.webp': ombreSkewer,
  '/images/avatars/toothless-peace.webp': toothlessPeace,
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
  { id: 'messy-glasses', name: '乱发眼镜', url: '/images/avatars/messy-glasses.webp' },
  { id: 'curtain-blazer', name: '帘发西装', url: '/images/avatars/curtain-blazer.webp' },
  { id: 'double-glasses', name: '双镜行者', url: '/images/avatars/double-glasses.webp' },
  { id: 'batik-shirt', name: '金纹衬衫', url: '/images/avatars/batik-shirt.webp' },
  { id: 'magenta-bob', name: '粉发心坠', url: '/images/avatars/magenta-bob.webp' },
  { id: 'pearl-glasses', name: '珍珠眼镜', url: '/images/avatars/pearl-glasses.webp' },
  { id: 'ear-pull-glasses', name: '拉耳圆框', url: '/images/avatars/ear-pull-glasses.webp' },
  { id: 'bluegray-toast', name: '蓝灰举杯', url: '/images/avatars/bluegray-toast.webp' },
  { id: 'ombre-skewer', name: '渐变串烧', url: '/images/avatars/ombre-skewer.webp' },
  { id: 'toothless-peace', name: '龙帽比耶', url: '/images/avatars/toothless-peace.webp' },
  { id: 'customer-1', name: '暖心雇主', url: '/images/customer/p1.webp' },
  { id: 'customer-2', name: '眼镜行家', url: '/images/customer/p2.webp' },
  { id: 'customer-3', name: '红巾客官', url: '/images/customer/p3.webp' },
]
