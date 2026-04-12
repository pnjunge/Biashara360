import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const previewPort = Number(process.env.PORT || 4173)

export default defineConfig({
  plugins: [react()],
  server: { host: '0.0.0.0', port: 3000 },
  preview: {
    host: '0.0.0.0',
    port: previewPort,
    strictPort: true,
    allowedHosts: ['web-production-76557.up.railway.app', '*.up.railway.app', 'localhost'],
  },
  build: { outDir: 'dist' }
})
