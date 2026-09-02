import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite' // 🌟 1. 載入 Tailwind 插件

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(), // 🌟 2. 把它加進 plugins 陣列裡
  ],
})