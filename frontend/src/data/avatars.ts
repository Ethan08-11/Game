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
import curlyLeather from '@/assets/avatars/curly-leather.webp'
import fluffyStripe from '@/assets/avatars/fluffy-stripe.webp'
import beanieStripe from '@/assets/avatars/beanie-stripe.webp'
import purpleCapBraids from '@/assets/avatars/purple-cap-braids.webp'
import appleDress from '@/assets/avatars/apple-dress.webp'
import purpleOffshoulder from '@/assets/avatars/purple-offshoulder.webp'
import birthdayCake from '@/assets/avatars/birthday-cake.webp'
import stripeTouchHair from '@/assets/avatars/stripe-touch-hair.webp'
import meshWhiteTee from '@/assets/avatars/mesh-white-tee.webp'
import cowboyHat from '@/assets/avatars/cowboy-hat.webp'
import stripeGlassesGuy from '@/assets/avatars/stripe-glasses-guy.webp'
import blondeCake from '@/assets/avatars/blonde-cake.webp'
import flowerOffshoulder from '@/assets/avatars/flower-offshoulder.webp'
import chefRat from '@/assets/avatars/chef-rat.webp'
import navyVest from '@/assets/avatars/navy-vest.webp'
import pinkShrug from '@/assets/avatars/pink-shrug.webp'
import bangsGlasses from '@/assets/avatars/bangs-glasses.webp'
import thickGlassesSmile from '@/assets/avatars/thick-glasses-smile.webp'
import appleRabbit from '@/assets/avatars/apple-rabbit.webp'
import tartPeace from '@/assets/avatars/tart-peace.webp'
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
  '/images/avatars/curly-leather.webp': curlyLeather,
  '/images/avatars/fluffy-stripe.webp': fluffyStripe,
  '/images/avatars/beanie-stripe.webp': beanieStripe,
  '/images/avatars/purple-cap-braids.webp': purpleCapBraids,
  '/images/avatars/apple-dress.webp': appleDress,
  '/images/avatars/purple-offshoulder.webp': purpleOffshoulder,
  '/images/avatars/birthday-cake.webp': birthdayCake,
  '/images/avatars/stripe-touch-hair.webp': stripeTouchHair,
  '/images/avatars/mesh-white-tee.webp': meshWhiteTee,
  '/images/avatars/cowboy-hat.webp': cowboyHat,
  '/images/avatars/stripe-glasses-guy.webp': stripeGlassesGuy,
  '/images/avatars/blonde-cake.webp': blondeCake,
  '/images/avatars/flower-offshoulder.webp': flowerOffshoulder,
  '/images/avatars/chef-rat.webp': chefRat,
  '/images/avatars/navy-vest.webp': navyVest,
  '/images/avatars/pink-shrug.webp': pinkShrug,
  '/images/avatars/bangs-glasses.webp': bangsGlasses,
  '/images/avatars/thick-glasses-smile.webp': thickGlassesSmile,
  '/images/avatars/apple-rabbit.webp': appleRabbit,
  '/images/avatars/tart-peace.webp': tartPeace,
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
  { id: 'curly-leather', name: '比耶皮衣', url: '/images/avatars/curly-leather.webp' },
  { id: 'fluffy-stripe', name: '条纹蓬发', url: '/images/avatars/fluffy-stripe.webp' },
  { id: 'beanie-stripe', name: '冷帽条纹', url: '/images/avatars/beanie-stripe.webp' },
  { id: 'purple-cap-braids', name: '紫帽编发', url: '/images/avatars/purple-cap-braids.webp' },
  { id: 'apple-dress', name: '双苹短发', url: '/images/avatars/apple-dress.webp' },
  { id: 'purple-offshoulder', name: '紫肩长发', url: '/images/avatars/purple-offshoulder.webp' },
  { id: 'birthday-cake', name: '生日蛋糕', url: '/images/avatars/birthday-cake.webp' },
  { id: 'stripe-touch-hair', name: '条纹扶发', url: '/images/avatars/stripe-touch-hair.webp' },
  { id: 'mesh-white-tee', name: '纱网白T', url: '/images/avatars/mesh-white-tee.webp' },
  { id: 'cowboy-hat', name: '牛仔草帽', url: '/images/avatars/cowboy-hat.webp' },
  { id: 'stripe-glasses-guy', name: '条纹方镜', url: '/images/avatars/stripe-glasses-guy.webp' },
  { id: 'blonde-cake', name: '金发条纹', url: '/images/avatars/blonde-cake.webp' },
  { id: 'flower-offshoulder', name: '蓝花露肩', url: '/images/avatars/flower-offshoulder.webp' },
  { id: 'chef-rat', name: '厨师鼠', url: '/images/avatars/chef-rat.webp' },
  { id: 'navy-vest', name: '方镜马甲', url: '/images/avatars/navy-vest.webp' },
  { id: 'pink-shrug', name: '粉衫摊手', url: '/images/avatars/pink-shrug.webp' },
  { id: 'bangs-glasses', name: '刘海圆镜', url: '/images/avatars/bangs-glasses.webp' },
  { id: 'thick-glasses-smile', name: '厚框竖中', url: '/images/avatars/thick-glasses-smile.webp' },
  { id: 'apple-rabbit', name: '苹果白兔', url: '/images/avatars/apple-rabbit.webp' },
  { id: 'tart-peace', name: '蛋挞比耶', url: '/images/avatars/tart-peace.webp' },
  { id: 'customer-1', name: '暖心雇主', url: '/images/customer/p1.webp' },
  { id: 'customer-2', name: '眼镜行家', url: '/images/customer/p2.webp' },
  { id: 'customer-3', name: '红巾客官', url: '/images/customer/p3.webp' },
]
