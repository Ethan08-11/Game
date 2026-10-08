import { contextBridge } from 'electron'

function readDeviceId(): string {
  const arg = process.argv.find((item) => item.startsWith('--app-device-id='))
  if (!arg) return ''
  return arg.slice('--app-device-id='.length)
}

contextBridge.exposeInMainWorld('desktopApp', {
  deviceId: readDeviceId(),
})
