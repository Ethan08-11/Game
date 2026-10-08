const DEVICE_PATTERN = /^[A-Za-z0-9:._-]{8,128}$/

export function clientDeviceId(): string {
  const value = window.desktopApp?.deviceId?.trim() || ''
  return DEVICE_PATTERN.test(value) ? value : ''
}
