import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      // Proxy /api/* → Spring Boot (authenticated endpoints)
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // Proxy /auth/* → Spring Boot (login / register — no /api prefix)
      '/auth': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    // jsdom gives us a browser-like DOM for Vitest tests
    environment: 'jsdom',
    // Auto-import @testing-library/jest-dom matchers in every test file
    setupFiles: ['./src/tests/setup.js'],
    globals: true,
  },
})
