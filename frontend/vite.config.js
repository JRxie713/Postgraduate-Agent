import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const certDir = path.join(path.dirname(fileURLToPath(import.meta.url)), 'certs')

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    // 绑定 443（HTTPS 默认端口），访问 https://kaoyan.localhost/ 时地址栏不出现端口号
    host: '127.0.0.1',
    port: 443,
    strictPort: true,
    https: {
      key: fs.readFileSync(path.join(certDir, 'kaoyan.localhost-key.pem')),
      cert: fs.readFileSync(path.join(certDir, 'kaoyan.localhost.pem'))
    },
    allowedHosts: ['kaoyan.localhost'],
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
