import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react({
      // 配置Babel以支持Chrome 75
      babel: {
        presets: [
          [
            '@babel/preset-env',
            {
              targets: {
                chrome: '75'
              },
              useBuiltIns: 'usage',
              corejs: 3,
              // 确保使用ESM模块格式
              modules: false
            }
          ]
        ],
        plugins: [
          // 确保async/await等特性正确转译
          '@babel/plugin-transform-runtime'
        ]
      }
    })
  ],
  // 优化依赖项
  optimizeDeps: {
    esbuildOptions: {
      // 设置ESBuild目标
      target: 'chrome75',
      // 确保使用ESM格式
      format: 'esm'
    }
  },
  build: {
    // 设置目标浏览器以支持Chrome 75
    target: 'chrome75',
    // 确保代码分割兼容性
    cssCodeSplit: true,
    // 指定输出为ESM格式
    modulePreload: {
      polyfill: false
    },
    // 避免CommonJS转换
    rollupOptions: {
      output: {
        format: 'esm'
      }
    }
  },
  // 开发服务器配置
  server: {
    port: 3000,
    open: true
  },
  // 确保使用ESM模块系统
  resolve: {
    alias: {
      // 避免模块解析问题
      'core-js/stable': 'core-js/stable/index.js',
      'regenerator-runtime/runtime': 'regenerator-runtime/runtime.js'
    }
  }
})
