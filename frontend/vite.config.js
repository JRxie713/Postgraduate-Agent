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
    host: '0.0.0.0',
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
        changeOrigin: true,
        // 将访问者的 IP 传给后端，避免所有经 Vite 转发的组员共用 127.0.0.1 限流计数。
        xfwd: true
      }
    }
  }
})
