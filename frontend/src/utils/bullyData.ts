const BULLY_NAMES = ['职场霸凌者', '恶意胁迫者', '权力滥用者', '办公室恶霸', '刁难客户代表', '黑心竞争对手']

const EMPLOYER_NAMES = ['王总', '李老板', '陈经理', '张董事', '刘主管']

export function randomBullyName(): string {
  return BULLY_NAMES[Math.floor(Math.random() * BULLY_NAMES.length)]
}

export function randomEmployerName(): string {
  return EMPLOYER_NAMES[Math.floor(Math.random() * EMPLOYER_NAMES.length)]
}
