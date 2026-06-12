import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { resolve } from 'path'

export default defineConfig({
  plugins: [
    tailwindcss(),
    vue(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      dts: 'src/components.d.ts',
    }),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@import "@/assets/styles/variables.scss";`,
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-vue': ['vue', 'vue-router', 'pinia'],
          'vendor-echarts': ['echarts'],
          'vendor-lucide': ['lucide-vue-next'],
        },
      },
    },
  },
  server: {
    port: 5001,
    proxy: {
      '/api/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/tenant': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/org': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/role': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/user': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/permission': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/notification': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/demand': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },
      '/api/approval': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },
      '/api/job': {
        target: 'http://localhost:8084',
        changeOrigin: true,
      },
      '/api/candidate': {
        target: 'http://localhost:8085',
        changeOrigin: true,
      },
      '/api/pipeline': {
        target: 'http://localhost:8085',
        changeOrigin: true,
      },
      '/api/resume': {
        target: 'http://localhost:8085',
        changeOrigin: true,
      },
      '/storage': {
        target: 'http://localhost:8085',
        changeOrigin: true,
      },
      '/api/interview': {
        target: 'http://localhost:8086',
        changeOrigin: true,
      },
      '/api/evaluation': {
        target: 'http://localhost:8086',
        changeOrigin: true,
      },
      '/api/offer': {
        target: 'http://localhost:8087',
        changeOrigin: true,
      },
      '/api/onboard': {
        target: 'http://localhost:8088',
        changeOrigin: true,
      },
      '/api/communication': {
        target: 'http://localhost:8089',
        changeOrigin: true,
      },
      '/api/evolution': {
        target: 'http://localhost:8090',
        changeOrigin: true,
      },
      '/api/agent': {
        target: 'http://localhost:8091',
        changeOrigin: true,
      },
      '/api/referral': {
        target: 'http://localhost:8092',
        changeOrigin: true,
      },
      '/api/headhunter': {
        target: 'http://localhost:8093',
        changeOrigin: true,
      },
      '/api/analytics': {
        target: 'http://localhost:8094',
        changeOrigin: true,
      },
      '/api/llm': {
        target: 'http://localhost:8095',
        changeOrigin: true,
      },
      '/api/brain': {
        target: 'http://localhost:8100',
        changeOrigin: true,
      },
      '/api/cognitive': {
        target: 'http://localhost:8100',
        changeOrigin: true,
      },
      '/api/compliance': {
        target: 'http://localhost:8100',
        changeOrigin: true,
      },
      '/api/integration': {
        target: 'http://localhost:8100',
        changeOrigin: true,
      },
      '/api/platform': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
    },
  },
})
