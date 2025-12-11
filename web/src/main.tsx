// 导入polyfill以支持Chrome 75等旧版浏览器
// 使用标准导入方式
import 'core-js/stable';
import 'regenerator-runtime/runtime';

import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
// 导入Ant Design React 19兼容性包
import '@ant-design/v5-patch-for-react-19';
import './index.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
