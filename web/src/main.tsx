// 导入polyfill以支持Chrome 75等旧版浏览器
// 使用标准导入方式
import 'core-js/stable';
import 'regenerator-runtime/runtime';

// 添加date-fns v3 isValid方法的polyfill，解决rc-picker依赖问题
import * as dateFns from 'date-fns';
if (typeof (dateFns as any).isValid !== 'function') {
  Object.defineProperty(dateFns, 'isValid', {
    value: (date: Date) => {
      return date instanceof Date && !Number.isNaN(date.getTime());
    },
    writable: false,
    enumerable: true,
    configurable: true
  });
}

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
