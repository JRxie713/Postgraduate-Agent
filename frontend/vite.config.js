import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const certDir = path.join(path.dirname(fileURLToPath(import.meta.url)), 'certs')
const certKey = path.join(certDir, 'kaoyan.localhost-key.pem')
const certCrt = path.join(certDir, 'kaoyan.localhost.pem')
// 证书不进仓库；缺失时（如 CI / 新克隆）退回 HTTP，保证 build / preview 仍可用
const hasCert = fs.existsSync(certKey) && fs.existsSync(certCrt)

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    // 绑定 443（HTTPS 默认端口），访问 https://kaoyan.localhost/ 时地址栏不出现端口号
    host: '127.0.0.1',
    port: 443,
    strictPort: true,
    ...(hasCert
      ? {
          https: {
            key: fs.readFileSync(certKey),
            cert: fs.readFileSync(certCrt)
          }
        }
      : {}),
    allowedHosts: ['kaoyan.localhost'],
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        // vue 运行时单独成块，业务代码改动时它的浏览器缓存不会失效
        manualChunks: {
          vue: ['vue']
        }
      }
    }
  },
  preview: {
    // 生产产物本地验证时同样需要把接口转到后端
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
