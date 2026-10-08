import { app, BrowserWindow } from 'electron'
import { execSync } from 'node:child_process'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)

const windows: BrowserWindow[] = []
const machineId = resolveMachineId()

function resolveMachineId(): string {
  if (process.platform === 'win32') {
    try {
      const out = execSync('reg query "HKLM\\SOFTWARE\\Microsoft\\Cryptography" /v MachineGuid', {
        encoding: 'utf8',
        windowsHide: true,
        timeout: 3000,
      })
      const match = out.match(/MachineGuid\s+REG_SZ\s+([0-9a-fA-F-]{8,})/i)
      if (match) return `win:${match[1].toLowerCase()}`
    } catch {
      return ''
    }
  }
  return ''
}

function createWindow(titleSuffix = '') {
  const win = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 1024,
    minHeight: 680,
    title: `这单我们护了！！！！${titleSuffix}`,
    webPreferences: {
      preload: join(__dirname, 'preload.cjs'),
      additionalArguments: machineId ? [`--app-device-id=${machineId}`] : [],
      nodeIntegration: false,
      contextIsolation: true,
    },
  })

  win.setMenuBarVisibility(false)

  if (process.env.VITE_DEV_SERVER_URL) {
    win.loadURL(process.env.VITE_DEV_SERVER_URL)
  } else {
    win.loadFile(join(__dirname, '../dist/index.html'))
  }

  win.on('closed', () => {
    const idx = windows.indexOf(win)
    if (idx !== -1) windows.splice(idx, 1)
  })

  windows.push(win)
  return win
}

const gotLock = app.requestSingleInstanceLock()
if (!gotLock) {
  app.quit()
} else {
  app.on('second-instance', () => {
    const win = windows[0]
    if (!win) return
    if (win.isMinimized()) win.restore()
    win.focus()
  })

  app.whenReady().then(() => {
    if (windows.length === 0) createWindow()
  })

  app.on('window-all-closed', () => {
    app.quit()
  })

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow()
    }
  })
}
