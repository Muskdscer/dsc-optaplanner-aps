import React from 'react';
import { Card, Typography, Row, Col, Statistic, Button } from 'antd';
import { Link } from 'react-router-dom';
import { ShoppingCartOutlined, CalendarOutlined, BarChartOutlined } from '@ant-design/icons';

const { Title, Paragraph } = Typography;

const HomePage: React.FC = () => {
  return (
    <div style={{ padding: '20px' }}>
      <Title level={2}>欢迎使用生产计划管理系统</Title>
      
      <Paragraph>
        本系统用于管理和查询生产订单任务，提供订单数据的可视化展示和状态监控。
      </Paragraph>

      <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
        <Col span={8}>
          <Card hoverable>
            <Statistic
              title="待处理订单"
              value={42}
              valueStyle={{ color: '#3f8600' }}
              prefix={<ShoppingCartOutlined />}
              suffix="个"
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card hoverable>
            <Statistic
              title="进行中任务"
              value={28}
              valueStyle={{ color: '#1890ff' }}
              prefix={<CalendarOutlined />}
              suffix="个"
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card hoverable>
            <Statistic
              title="本月完成率"
              value={78.5}
              precision={1}
              valueStyle={{ color: '#cf1322' }}
              prefix={<BarChartOutlined />}
              suffix="%"
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card title="快速操作">
            <Row gutter={[16, 16]}>
              <Col span={6}>
                <Link to="/order-query">
                <Button type="primary" icon={<ShoppingCartOutlined />} block>
                  订单查询
                </Button>
              </Link>
              </Col>
              <Col span={6}>
                <Button icon={<CalendarOutlined />} block>
                  生产计划
                </Button>
              </Col>
              <Col span={6}>
                <Button icon={<BarChartOutlined />} block>
                  数据报表
                </Button>
              </Col>
              <Col span={6}>
                <Button icon={<ShoppingCartOutlined />} block>
                  系统设置
                </Button>
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default HomePage;