import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import electron from 'vite-plugin-electron'
import electronRenderer from 'vite-plugin-electron-renderer'

export default defineConfig(({ mode }) => {
  const enableElectron = mode !== 'web'

  return {
    plugins: [
      vue(),
      ...(enableElectron
        ? [
            electron([
              {
                entry: 'electron/main.ts',
                vite: {
                  build: {
                    outDir: 'dist-electron',
                    rollupOptions: {
                      external: ['electron'],
                    },
                  },
                },
              },
            ]),
            electronRenderer(),
          ]
        : []),
    ],
    server: {
      proxy: {
        '/api': {
          target: 'http://127.0.0.1:8080',
          changeOrigin: true,
        },
        '/images': {
          target: 'http://127.0.0.1:8080',
          changeOrigin: true,
          bypass(req) {
            const url = (req.url || '').split('?')[0]
            if (url.startsWith('/images/avatars/')) return url
            // 原卡打在 frontend/public，避免被代理回旧后端静态资源
            if (url.startsWith('/images/cards/Card_')) return url
            if (url.includes('/images/cards/技术_Ethan') || url.includes('/images/cards/%E6%8A%80%E6%9C%AF_Ethan')) return url
            return null
          },
        },
        '/ws': {
          target: 'http://127.0.0.1:8080',
          changeOrigin: true,
          ws: true,
        },
      },
    },
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
      },
    },
  }
})
