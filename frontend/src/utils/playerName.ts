/** 玩家展示名：英文首字母大写，其余小写。ETHAN → Ethan */
export function formatPlayerName(name?: string | null): string {
  const raw = String(name ?? '').trim()
  if (!raw) return ''
  if (/^玩家\d*$/.test(raw)) return raw
  return raw.replace(/[A-Za-z][A-Za-z']*/g, (word) => (
    word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
  ))
}
