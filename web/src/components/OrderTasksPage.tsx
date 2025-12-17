import React, { useState, useEffect } from 'react';
import { Table, Button, Space, Typography, message, Form, Input, DatePicker, Select, Row, Col, Card, Tag, Modal, InputNumber } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { queryTasks, startTasks, createTimeslot } from '../services/orderService';
import type { Task, OrderTaskQueryParams } from '../services/orderService';

const { Title } = Typography;
const { RangePicker } = DatePicker;
const { Option } = Select;

const OrderTasksPage: React.FC = () => {
  const [form] = Form.useForm();
  const [orderTasks, setOrderTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  
  // 创建时间槽相关状态
  const [createTimeslotModalVisible, setCreateTimeslotModalVisible] = useState<boolean>(false);
  const [timeslotForm] = Form.useForm();
  const [creatingTimeslot, setCreatingTimeslot] = useState<boolean>(false);
  
  // 处理创建时间槽
  const handleCreateTimeslot = async () => {
    try {
      const values = await timeslotForm.validateFields();
      
      setCreatingTimeslot(true);
      
      // 提取选中的任务编号
      const taskNos = selectedRowKeys.map(key => String(key));
      
      // 调用API，空数组作为procedureIds参数
      // 确保只传递实际存在的值，使用默认值处理未定义的情况
      const timeValue = values.time !== null && values.time !== undefined ? values.time : 0.5;
      const sliceValue = values.slice !== null && values.slice !== undefined ? values.slice : 0;
      await createTimeslot(taskNos, [], timeValue, sliceValue);
      message.success('时间槽创建成功');
      setCreateTimeslotModalVisible(false);
      timeslotForm.resetFields();
    } catch (error) {
      message.error('时间槽创建失败，请重试');

    } finally {
      setCreatingTimeslot(false);
    }
  };
  
  // 状态选项
  const statusOptions = [
    { label: '待生产', value: '待生产' },
    { label: '生产中', value: '生产中' },
    { label: '生产完成', value: '生产完成' },
    { label: '已暂停', value: '已暂停' },
  ];
  
  // 行选择配置
  const rowSelection = {
    selectedRowKeys,
    onChange: (newSelectedRowKeys: React.Key[]) => {

      setSelectedRowKeys(newSelectedRowKeys);
    },
  };

  // 查询订单任务数据
  const fetchTasks = async (params: OrderTaskQueryParams) => {
    setLoading(true);
    try {
      const response = await queryTasks(params);
      setOrderTasks(response || []);
    } catch (error) {
      message.error('网络错误，获取任务数据失败');

      setOrderTasks([]);
    } finally {
      setLoading(false);
    }
  };

  // 初始加载数据
  useEffect(() => {
    const params: OrderTaskQueryParams = {
      ...form.getFieldsValue(),
    };
    fetchTasks(params);
  }, []);

  // 处理搜索
  const handleSearch = async () => {
    const values = form.getFieldsValue();
    // 处理日期范围
    let startTime: string | undefined;
    let endTime: string | undefined;
    if (values.dateRange && values.dateRange.length === 2) {
      startTime = values.dateRange[0].format('YYYY-MM-DD');
      endTime = values.dateRange[1].format('YYYY-MM-DD');
      delete values.dateRange;
    }
    
    const params: OrderTaskQueryParams = {
      ...values,
      startTime,
      endTime,
    };
    
    fetchTasks(params);
  };

  // 处理重置
  const handleReset = () => {
    form.resetFields();
    // 重新查询
    fetchTasks({
    });
  };

  // 处理开始任务
  const handleStartTasks = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择要开始的任务');
      return;
    }

    try {
      // 提取选中任务的订单编号
      const taskNos = selectedRowKeys.map(key => {
        const task = orderTasks.find(t => t.taskNo === key);
        return task?.taskNo;
      }).filter(Boolean) as string[];

      // 调用API服务中的startTasks函数并获取返回信息
      const messageInfo = await startTasks(taskNos);
      // 使用返回的信息显示提示框
      message.success(messageInfo || '任务开始成功');
    } catch (error) {

      message.error('任务开始失败，请重试');
    }
  };

  // 表格列定义
  const columns: ColumnsType<Task> = [
    {
      title: '任务编号',
      dataIndex: 'taskNo',
      key: 'taskNo',
      width: 180,
    },
    {
      title: '订单编号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 150,
    },
    {
      title: '任务状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 80,
      render: (priority: number) => {
        // 根据优先级数字返回不同深浅的颜色
        let color = 'default'; // 默认灰色
        
        if (priority > 0) {
          // 优先级数字越大，颜色越深
          if (priority <= 20) {
            color = 'blue';
          } else if (priority <= 40) {
            color = 'cyan';
          } else if (priority <= 60) {
            color = 'green';
          } else if (priority <= 80) {
            color = 'orange';
          } else {
            color = 'red';
          }
        }
        
        return <Tag color={color}>{priority}</Tag>;
      },
    },
    {
      title: '计划开始日期',
      dataIndex: 'planStartDate',
      key: 'planStartDate',
      width: 150,
      render: (text: string) => text ? text.split('T')[0] : '',
    },
    {
      title: '计划结束日期',
      dataIndex: 'planEndDate',
      key: 'planEndDate',
      width: 150,
      render: (text: string) => text ? text.split('T')[0] : '',
    },
    {
      title: '实际开始日期',
      dataIndex: 'factStartDate',
      key: 'factStartDate',
      width: 150,
      render: (text: string | null) => text ? text.split('T')[0] : '',
    },
    {
      title: '实际结束日期',
      dataIndex: 'factEndDate',
      key: 'factEndDate',
      width: 150,
      render: (text: string | null) => text ? text.split('T')[0] : '',
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={4}>任务列表</Title>
      <Card title="查询条件" style={{ marginBottom: 24, boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)' }}>
        <Form
          form={form}
          layout="horizontal"
          labelCol={{ span: 6 }}
          wrapperCol={{ span: 10 }}
        >
          <Row gutter={[16, 24]}>
            <Col xs={8} sm={8} md={8}>
              <Form.Item name="orderNo" label="订单编号">
                <Input placeholder="请输入订单编号" />
              </Form.Item>
            </Col>
            <Col xs={8} sm={8} md={8}>
              <Form.Item name="taskStatus" label="状态">
                <Select 
                  placeholder="请选择任务状态"
                  allowClear
                  style={{ width: '100%' }}
                >
                  {statusOptions.map(option => (
                    <Option key={option.value} value={option.value}>{option.label}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="dateRange" label="日期范围">
                <RangePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={24} style={{ textAlign: 'center', paddingTop: 8 }}>
              <Space size="middle">
                <Button type="primary" onClick={handleSearch} size="middle">
                  查询
                </Button>
                <Button onClick={handleReset} size="middle">
                  重置
                </Button>
              </Space>
            </Col>
          </Row>
        </Form>
      </Card>
      <Space style={{ marginBottom: 16 }}>
        <Button 
          type="primary" 
          onClick={handleStartTasks}
          disabled={selectedRowKeys.length === 0}
        >
          开始任务
        </Button>
        <Button 
          type="primary" 
          onClick={() => setCreateTimeslotModalVisible(true)}
          disabled={selectedRowKeys.length === 0}
        >
          创建时间槽
        </Button>
      </Space>
      <Table
        rowKey="taskNo"
        columns={columns}
        dataSource={orderTasks}
        rowSelection={{ type: 'checkbox', ...rowSelection }}
        loading={loading}
        scroll={{ x: 1500 }}
        pagination={{ 
          pageSize: 20, 
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条记录` 
        }}
      />
      {/* 创建时间槽模态框 */}
      <Modal
        title="创建时间槽"
        open={createTimeslotModalVisible}
        onOk={handleCreateTimeslot}
        onCancel={() => {
          setCreateTimeslotModalVisible(false);
          timeslotForm.resetFields();
        }}
        okText="确认"
        cancelText="取消"
        okButtonProps={{ loading: creatingTimeslot }}
      >
        <Form
          form={timeslotForm}
          layout="vertical"
          initialValues={{
            time: 0.5
          }}
        >
          <Form.Item
            name="time"
            label="时间（小时）"
            dependencies={['slice']}
            rules={[
              {
                validator: (_, value, callback) => {
                  const slice = timeslotForm.getFieldValue('slice');
                  if (value !== undefined && value !== null && slice !== undefined && slice !== null && slice !== '') {
                    callback('时间和分片序号只能填写一个');
                  } else if (value === undefined && slice === undefined) {
                    callback('请至少填写时间或分片序号中的一个');
                  } else {
                    callback();
                  }
                }
              }
            ]}
          >
            <InputNumber min={0.1} max={24} step={0.1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="slice"
            label="分片序号"
            dependencies={['time']}
            rules={[
              {
                validator: (_, value, callback) => {
                  const time = timeslotForm.getFieldValue('time');
                  if (value !== undefined && value !== null && value !== '' && time !== undefined && time !== null) {
                    callback('时间和分片序号只能填写一个');
                  } else if (value === undefined && time === undefined) {
                    callback('请至少填写时间或分片序号中的一个');
                  } else {
                    callback();
                  }
                }
              }
            ]}
          >
            <InputNumber min={0} step={1} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default OrderTasksPage;
