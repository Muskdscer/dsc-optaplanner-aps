import React, { useState } from 'react';
import { Form, Input, Button, Card, Typography, message, Layout, Divider } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, PhoneOutlined } from '@ant-design/icons';
import { Link, useNavigate } from 'react-router-dom';
import { register } from '../services/orderService';

const { Title, Text } = Typography;
const { Content } = Layout;

const RegisterPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();
  const navigate = useNavigate();

  const handleRegister = async (values: { 
    username: string; 
    password: string; 
    email: string; 
    phone: string;
    confirmPassword?: string;
  }) => {
    setLoading(true);
    try {
      // 移除确认密码字段，不发送到后端
      const { confirmPassword, ...registerData } = values;
      
      const response = await register(registerData);
      if (response.code === 200) {
        message.success('注册成功！正在跳转到登录页面...');
        // 注册成功后跳转到登录页
        setTimeout(() => {
          navigate('/login');
        }, 1500);
      } else {
        message.error(response.msg || '注册失败');
      }
    } catch (error) {
      message.error('注册失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  // 自定义密码验证规则
  const validatePassword = (_rule: any, value: string) => {
    if (value && value.length < 6) {
      return Promise.reject(new Error('密码长度至少为6位'));
    }
    return Promise.resolve();
  };

  // 确认密码验证规则
  const validateConfirmPassword = (_rule: any, value: string) => {
    const password = form.getFieldValue('password');
    if (value && value !== password) {
      return Promise.reject(new Error('两次输入的密码不一致'));
    }
    return Promise.resolve();
  };

  return (
    <Layout className="register-layout" style={{ minHeight: '100vh' }}>
      <Content
        className="register-content"
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: '#f0f2f5',
          padding: '24px',
        }}
      >
        <Card
          className="register-card"
          style={{
            width: 450,
            borderRadius: 8,
            boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
            padding: '24px',
          }}
        >
          <div style={{ textAlign: 'center', marginBottom: '24px' }}>
            <Title level={2}>用户注册</Title>
            <Text type="secondary">创建您的账户</Text>
          </div>
          
          <Form
            form={form}
            layout="vertical"
            onFinish={handleRegister}
            autoComplete="off"
          >
            <Form.Item
              name="username"
              label="用户名"
              rules={[
                { required: true, message: '请输入用户名' },
                { min: 4, max: 50, message: '用户名长度为4-50个字符' }
              ]}
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
              rules={[
                { required: true, message: '请输入密码' },
                { validator: validatePassword }
              ]}
            >
              <Input.Password
                prefix={<LockOutlined className="site-form-item-icon" />}
                placeholder="请输入密码（至少6位）"
                autoComplete="new-password"
              />
            </Form.Item>
            
            <Form.Item
              name="confirmPassword"
              label="确认密码"
              dependencies={['password']}
              rules={[
                { required: true, message: '请确认密码' },
                { validator: validateConfirmPassword }
              ]}
            >
              <Input.Password
                prefix={<LockOutlined className="site-form-item-icon" />}
                placeholder="请再次输入密码"
                autoComplete="new-password"
              />
            </Form.Item>
            
            <Form.Item
              name="email"
              label="邮箱"
              rules={[
                { required: true, message: '请输入邮箱' },
                { type: 'email', message: '请输入有效的邮箱地址' }
              ]}
            >
              <Input
                prefix={<MailOutlined className="site-form-item-icon" />}
                placeholder="请输入邮箱地址"
                autoComplete="email"
              />
            </Form.Item>
            
            <Form.Item
              name="phone"
              label="手机号"
              rules={[
                { required: true, message: '请输入手机号' },
                { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码' }
              ]}
            >
              <Input
                prefix={<PhoneOutlined className="site-form-item-icon" />}
                placeholder="请输入手机号"
                autoComplete="tel"
              />
            </Form.Item>
            
            <Form.Item style={{ marginTop: '24px' }}>
              <Button
                type="primary"
                htmlType="submit"
                className="register-button"
                loading={loading}
                block
                size="large"
                style={{ height: '40px' }}
              >
                注册
              </Button>
            </Form.Item>
            
            <Divider />
            
            <div style={{ textAlign: 'center' }}>
              <Text type="secondary">已有账户？</Text>
              <Link to="/login" style={{ marginLeft: '8px' }}>
                立即登录
              </Link>
            </div>
          </Form>
        </Card>
      </Content>
    </Layout>
  );
};

export default RegisterPage;