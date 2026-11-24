/** 玩家展示名：英文首字母大写，其余小写。ETHAN → Ethan */
export function formatPlayerName(name?: string | null): string {
  const raw = String(name ?? '').trim()
  if (!raw) return ''
  if (/^玩家\d*$/.test(raw)) return raw
  return raw.replace(/[A-Za-z][A-Za-z']*/g, (word) => (
    word.toUpperCase() === 'HIH' ? 'HIH' : word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
  ))
}

/** 用户名：首字母大写，其余小写。ethan / ETHAN → Ethan */
export function capitalizeUsername(name?: string | null): string {
  const raw = String(name ?? '').trim().replace(/\s+/g, ' ')
  if (!raw) return ''
  return raw.split(' ').map((part) => {
    if (!part) return ''
    return part.toLowerCase() === 'hih' ? 'HIH' : part.charAt(0).toUpperCase() + part.slice(1).toLowerCase()
  }).join(' ')
}
