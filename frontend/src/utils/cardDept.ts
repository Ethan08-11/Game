const PUBLIC_GAMEPLAY_DEPTS = new Set([
  'public',
  'tech',
  'it',
  'neutral',
  'passerby',
  '公共部',
  '中立',
  '路人部',
])

export function flavorDeptFromImageUrl(imageUrl?: string | null): string | null {
  if (!imageUrl) return null
  try {
    const path = imageUrl.split('?')[0].split('#')[0]
    const file = decodeURIComponent(path.split('/').pop() || '')
    const base = file.replace(/\.[^.]+$/, '')
    const sep = base.indexOf('_')
    if (sep <= 0) return null
    const prefix = base.slice(0, sep).trim()
    if (!prefix || /^card$/i.test(prefix) || /^\d+$/.test(prefix)) return null
    return prefix
  } catch {
    return null
  }
}

export function isPublicGameplayDept(dept?: string | null): boolean {
  const key = (dept || '').trim()
  if (!key) return false
  return PUBLIC_GAMEPLAY_DEPTS.has(key) || PUBLIC_GAMEPLAY_DEPTS.has(key.toLowerCase())
}

/** 公共部卡面显示图片前缀（如 文员_宫廷抄写员 → 文员）；玩法部门仍为 public。 */
export function displayCardDept(dept?: string | null, imageUrl?: string | null, fallback = ''): string {
  const flavor = flavorDeptFromImageUrl(imageUrl)
  if (flavor && isPublicGameplayDept(dept)) return flavor
  return fallback || dept || ''
}
