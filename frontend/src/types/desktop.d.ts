export {}

declare global {
  interface Window {
    desktopApp?: {
      deviceId?: string
    }
  }
}
