

import { Layout, Menu, Button } from 'antd';
import { Routes, Route, Link, BrowserRouter, Navigate } from 'react-router-dom';
import { HomeOutlined, FileTextOutlined, BarChartOutlined, SettingOutlined, LogoutOutlined } from '@ant-design/icons';
import HomePage from './components/HomePage';
import OrderQueryPage from './components/OrderQueryPage';
import OrderTasksPage from './components/OrderTasksPage';
import LoginPage from './components/LoginPage';
import RegisterPage from './components/RegisterPage';
import { isLoggedIn, logout } from './services/orderService';
import 'antd/dist/reset.css';
import './App.css';

const { Header, Content, Sider } = Layout;

// 菜单配置项
const menuItems = [
  {
    key: '1',
    icon: <HomeOutlined />,
    label: <Link to="/">首页</Link>,
  },
  {
    key: '2',
    icon: <FileTextOutlined />,
    label: <Link to="/order-query">订单查询</Link>,
  },
  {
    key: '5',
    icon: <FileTextOutlined />,
    label: <Link to="/order-tasks">任务列表</Link>,
  },
  {
    key: '3',
    icon: <BarChartOutlined />,
    label: <Link to="/reports">数据报表</Link>,
  },
  {
    key: '4',
    icon: <SettingOutlined />,
    label: <Link to="/settings">系统设置</Link>,
  },
];

// 受保护的路由组件
const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  if (!isLoggedIn()) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
};

// 头部组件（包含登出按钮）
const AppHeader = () => {
  const handleLogout = async () => {
    await logout();
  };
  
  return (
    <Header className="header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
      <div className="logo">生产计划管理系统</div>
      {isLoggedIn() && (
        <Button 
          danger
          type="primary"
          icon={<LogoutOutlined />} 
          onClick={handleLogout}
          style={{ marginLeft: 'auto' }}
        >
          登出
        </Button>
      )}
    </Header>
  );
};

// 应用内部布局组件
const AppContent = () => {
  return (
    <Layout className="app-layout">
      <AppHeader />
      {isLoggedIn() && (
        <Layout>
          <Sider width={200} className="sider" theme="light">
            <Menu
              mode="inline"
              items={menuItems}
              defaultSelectedKeys={['1']}
              className="menu"
            />
          </Sider>
          <Layout className="content-wrapper">
            <Content className="content">
              <Routes>
                <Route path="/" element={<ProtectedRoute><HomePage /></ProtectedRoute>} />
                <Route path="/order-query" element={<ProtectedRoute><OrderQueryPage /></ProtectedRoute>} />
                <Route path="/order-tasks" element={<ProtectedRoute><OrderTasksPage /></ProtectedRoute>} />
                {/* 其他受保护的路由可以在这里添加 */}
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </Content>
          </Layout>
        </Layout>
      )}
      {!isLoggedIn() && (
        <Layout className="login-wrapper">
          <Content className="login-content">
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
          </Content>
        </Layout>
      )}
    </Layout>
  );
};

function App() {
  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  );
}

export default App
