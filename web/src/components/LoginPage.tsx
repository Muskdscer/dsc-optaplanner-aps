import React, { useState } from 'react';
import { Form, Input, Button, Card, Typography, message, Layout, Divider } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';
import { login, type LoginResponse } from '../services/orderService';

const { Title, Text } = Typography;
const { Content } = Layout;

const LoginPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();

  const handleLogin = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      const response: LoginResponse = await login(values.username, values.password);
      if (response.type === "Bearer") {
        message.success('登录成功');
        // 登录成功后跳转到首页
        window.location.href = '/';
      } else {
        message.error('登录失败，请重试');
        console.error('登录错误:', response);
      }
    } catch (error) {
      message.error('登录失败，请重试');
      console.error('登录错误:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout className="login-layout" style={{ minHeight: '100vh' }}>
      <Content
        className="login-content"
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: '#f0f2f5',
          padding: '24px',
        }}
      >
        <Card
          className="login-card"
          style={{
            width: 400,
            borderRadius: 8,
            boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
            padding: '24px',
          }}
        >
          <div style={{ textAlign: 'center', marginBottom: '24px' }}>
            <Title level={2}>登录系统</Title>
          </div>
          <Form
            form={form}
            layout="vertical"
            onFinish={handleLogin}
          >
            <Form.Item
              name="username"
              label="用户名"
              rules={[{ required: true, message: '请输入用户名' }]}
            >
              <Input
                prefix={<UserOutlined className="site-form-item-icon" />}
                placeholder="请输入用户名"
                autoComplete="username"
              />
            </Form.Item>
            
            <Form.Item
              name="password"
              label="密码"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password
                prefix={<LockOutlined className="site-form-item-icon" />}
                placeholder="请输入密码"
                autoComplete="current-password"
              />
            </Form.Item>
            
            <Form.Item style={{ marginTop: '24px' }}>
              <Button
                type="primary"
                htmlType="submit"
                className="login-button"
                loading={loading}
                block
                size="large"
                style={{ height: '40px' }}
              >
                登录
              </Button>
            </Form.Item>
            
            <Divider />
            
            <div style={{ textAlign: 'center' }}>
              <Text type="secondary">还没有账户？</Text>
              <Link to="/register" style={{ marginLeft: '8px' }}>
                立即注册
              </Link>
            </div>
            
            <div style={{ textAlign: 'center', color: '#999', marginTop: '16px' }}>
              <p>提示：用户名：admin，密码：123456</p>
            </div>
          </Form>
        </Card>
      </Content>
    </Layout>
  );
};

export default LoginPage;
