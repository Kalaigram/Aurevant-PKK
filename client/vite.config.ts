import { fileURLToPath, URL } from 'node:url'

process.env.TOKIO_WORKER_THREADS ??= '4'
process.env.RAYON_NUM_THREADS ??= '4'
process.env.UV_THREADPOOL_SIZE ??= '4'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    tailwindcss(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
