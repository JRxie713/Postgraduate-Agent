import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    // 本地联调用普通 HTTP：localhost:5173，无需 HTTPS 证书与 hosts 映射
    host: 'localhost',
    port: 5173,
    strictPort: true,
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
