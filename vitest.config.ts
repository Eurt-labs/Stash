import { defineConfig } from 'vitest/config'
import path from 'path'

export default defineConfig({
  test: {
    environment: 'node',
    globals: true
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@main': path.resolve(__dirname, './src/main'),
      '@features': path.resolve(__dirname, './src/main/features'),
      '@core': path.resolve(__dirname, './src/main/core'),
      '@shared': path.resolve(__dirname, './src/shared')
    }
  }
})
