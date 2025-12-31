import React, {useState, useEffect, useCallback} from 'react';
import {
    Table,
    Pagination,
    Button,
    Space,
    Typography,
    message,
    Form,
    Input,
    DatePicker,
    Select,
    Row,
    Col,
    Card
} from 'antd';
import type {ColumnsType} from 'antd/es/table';
import {queryOrderTasksWithPagination, syncOrderData} from '../services/api.ts';
import type {OrderTask, OrderTaskQueryParams} from '../services/model.ts';

const {Title} = Typography;
const {RangePicker} = DatePicker;
const {Option} = Select;

const OrderQueryPage: React.FC = () => {
    const [form] = Form.useForm();
    const [orderTasks, setOrderTasks] = useState<OrderTask[]>([]);
    const [loading, setLoading] = useState<boolean>(false);
    const [total, setTotal] = useState<number>(0);
    const [currentPage, setCurrentPage] = useState<number>(1);
    const [pageSize, setPageSize] = useState<number>(20);
    const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

    // 状态选项
    const statusOptions = [
        {label: '待生产', value: '待生产'},
        {label: '生产中', value: '生产中'},
        {label: '生产完成', value: '生产完成'},
        {label: '已暂停', value: '已暂停'},
    ];
    message.config({
        top: 100,
        duration: 3,
        maxCount: 3,
    });
    // 行选择配置
    const rowSelection = {
        selectedRowKeys,
        onChange: (newSelectedRowKeys: React.Key[]) => {
            setSelectedRowKeys(newSelectedRowKeys);
        },
    };

    // 分页变化处理
    const handlePageChange = (page: number, size: number) => {
        setCurrentPage(page);
        setPageSize(size);
    };

    // 查询订单任务数据
    const fetchOrders = useCallback(async (params: OrderTaskQueryParams) => {
        setLoading(true);
        try {
            const response = await queryOrderTasksWithPagination(params);
            setOrderTasks(response.records || []);
            setTotal(response.total || 0);
        } catch {
            message.error('网络错误，获取订单任务数据失败');
            setOrderTasks([]);
            setTotal(0);
        } finally {
            setLoading(false);
        }
    }, [setOrderTasks, setTotal, setLoading]);

    // 初始加载和分页变化时获取数据
    useEffect(() => {
        const params: OrderTaskQueryParams = {
            pageNum: currentPage,
            pageSize,
            ...form.getFieldsValue(),
        };
        fetchOrders(params);
    }, [currentPage, pageSize, form, fetchOrders]);

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
            pageNum: 1,
            pageSize,
        };

        // 重置页码为1
        setCurrentPage(1);

        fetchOrders(params);
    };

    // 处理重置
    const handleReset = () => {
        form.resetFields();
        // 重置页码为1
        setCurrentPage(1);
        // 重新查询
        fetchOrders({
            pageNum: 1,
            pageSize,
        });
    };

    // 表格列定义
    const columns: ColumnsType<OrderTask> = [
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
          title: '产品名称',
          dataIndex: 'productName',
          key: 'productName',
          minWidth: 200,
        },
        {
            title: '产品编码',
            dataIndex: 'productCode',
            key: 'productCode',
            width: 150,
        },
        {
            title: '合同编号',
            dataIndex: 'contractNum',
            key: 'contractNum',
            width: 150,
        },
        {
            title: '任务状态',
            dataIndex: 'taskStatus',
            key: 'taskStatus',
            minWidth: 90,
        },
        {
            title: '订单状态',
            dataIndex: 'orderStatus',
            key: 'orderStatus',
            minWidth: 90,
        },
        {
            title: '计划数量',
            dataIndex: 'planQuantity',
            key: 'planQuantity',
            minWidth: 90,
        },
        {
            title: '计划开始日期',
            dataIndex: 'planStartDate',
            key: 'planStartDate',
            minWidth: 120,
        },
        {
            title: '计划结束日期',
            dataIndex: 'planEndDate',
            key: 'planEndDate',
            minWidth: 120,
        },
        {
            title: '实际开始日期',
            dataIndex: 'factStartDate',
            key: 'factStartDate',
            minWidth: 180,
            render: (date) => date || '-',
        },
        {
            title: '实际结束日期',
            dataIndex: 'factEndDate',
            key: 'factEndDate',
            minWidth: 180,
            render: (date) => date || '-',
        },
        {
            title: '工艺路线',
            dataIndex: 'routeSeq',
            key: 'routeSeq',
            minWidth: 120,
            render: (route) => route || '-',
        },
    ];

    return (
        <div style={{padding: 24}}>
            <Title level={4}>订单任务查询</Title>
            <Card title="查询条件" style={{marginBottom: 24, boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)'}}>
                <Form
                    form={form}
                    layout="horizontal"
                    labelCol={{span: 6}}
                    wrapperCol={{span: 10}}
                >
                    <Row gutter={[16, 24]}>
                        <Col xs={8} sm={8} md={8}>
                            <Form.Item name="orderName" label="单号">
                                <Input placeholder="请输入订单名称"/>
                            </Form.Item>
                        </Col>
                        <Col xs={8} sm={8} md={8}>
                            <Form.Item name="statusList" label="状态">
                                <Select
                                    placeholder="请选择订单状态"
                                    mode="multiple"
                                    allowClear
                                    style={{width: '100%'}}
                                >
                                    {statusOptions.map(option => (
                                        <Option key={option.value} value={option.value}>{option.label}</Option>
                                    ))}
                                </Select>
                            </Form.Item>
                        </Col>
                        <Col xs={24} md={8}>
                            <Form.Item name="dateRange" label="日期范围">
                                <RangePicker style={{width: '100%'}}/>
                            </Form.Item>
                        </Col>
                        <Col xs={24} style={{textAlign: 'center', paddingTop: 8}}>
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
            <Space style={{marginBottom: 16}}>
                <Button
                    type="primary"
                    disabled={selectedRowKeys.length === 0}
                    loading={loading}
                    onClick={async () => {
                        if (selectedRowKeys.length === 0) {
                            message.warning('请先选择要同步的订单');
                            return;
                        }
                        setLoading(true);
                        const selectedTaskNos = selectedRowKeys.map(key => key.toString());
                        try {
                            await syncOrderData(selectedTaskNos);
                            message.success(`成功同步 ${selectedTaskNos.length} 条数据到MES系统`);
                        } catch {
                            message.error('操作处理失败，请稍后重试');
                        } finally {
                            setLoading(false);
                        }
                    }}
                >
                    同步数据
                </Button>
            </Space>
            <Table
                rowKey="taskNo"
                columns={columns}
                dataSource={orderTasks}
                rowSelection={{type: 'checkbox', ...rowSelection}}
                loading={loading}
                pagination={false}
                scroll={{x: 1500}}
            />
            <Pagination
                current={currentPage}
                pageSize={pageSize}
                total={total}
                onChange={handlePageChange}
                showSizeChanger
                showQuickJumper
                showTotal={(total) => `共 ${total} 条记录`}
                style={{marginTop: '16px', textAlign: 'right'}}
            />
        </div>
    );
};

export default OrderQueryPage;
