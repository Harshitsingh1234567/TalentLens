import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  // The backend (frozen as of Phase 7) does not send CORS headers, so a
  // direct cross-origin fetch from the Vite dev server's origin to the
  // backend's origin would be blocked by the browser. Proxying /api
  // requests through the dev server keeps them same-origin from the
  // browser's point of view, entirely on the frontend side - no backend
  // change needed. See .env.example for VITE_DEV_API_PROXY_TARGET.
  const proxyTarget = env.VITE_DEV_API_PROXY_TARGET || 'http://localhost:8080'

  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
        },
      },
    },
  }
})
