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
            const url = req.url || ''
            if (url.startsWith('/images/avatars/')) return url
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
