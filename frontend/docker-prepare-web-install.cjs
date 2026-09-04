'use strict'

const fs = require('fs')
const { execSync } = require('child_process')

const lockPath = 'package-lock.json'
fs.writeFileSync(
  lockPath,
  fs.readFileSync(lockPath, 'utf8').replaceAll(
    'https://registry.npmmirror.com',
    'https://registry.npmjs.org',
  ),
)

execSync('npm ci --omit=optional --no-audit --no-fund', { stdio: 'inherit' })

if (process.platform !== 'linux') {
  console.log(`skip linux native bindings on ${process.platform}`)
  process.exit(0)
}

const lock = JSON.parse(fs.readFileSync(lockPath, 'utf8'))
const rolldown = lock.packages['node_modules/rolldown']?.version
const sass = lock.packages['node_modules/sass-embedded']?.version
const cpu = process.arch === 'arm64' ? 'arm64' : 'x64'
const pkgs = []
if (rolldown) pkgs.push(`@rolldown/binding-linux-${cpu}-musl@${rolldown}`)
if (sass) pkgs.push(`sass-embedded-linux-musl-${cpu}@${sass}`)
if (pkgs.length) {
  execSync(`npm install --no-save --no-audit --no-fund ${pkgs.join(' ')}`, { stdio: 'inherit' })
}
