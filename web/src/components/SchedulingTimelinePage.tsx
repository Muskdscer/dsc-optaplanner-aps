import React, {useEffect, useState} from 'react';
import {Card, InputNumber, message, Modal, Spin, Table, Tag, Tooltip} from 'antd';
import type {ColumnType} from 'antd/es/table';
import {getTimeslotList, splitOutsourcingTimeslot} from '../services/api';
import type {Procedure, Timeslot} from '../services/model';
import moment from 'moment';

interface TaskData {
  [key: string]: { timeslots: Timeslot[]; dateMap: Map<string, Timeslot[]> };
}

interface TableData {
  key: string;
  taskNo: string;
  [dateKey: string]: string | Timeslot[] | undefined;
}

const SchedulingTimelinePage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [tableData, setTableData] = useState<TableData[]>([]);
  const [dateColumns, setDateColumns] = useState<string[]>([]);
  // 弹窗状态管理
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [currentTimeslotId, setCurrentTimeslotId] = useState("");
  const [days, setDays] = useState(0);

  // 格式化工序详情
  const formatProcedureDetail = (timeslot: Timeslot) => {
    const { procedure, workCenter, startTime, endTime, duration } = timeslot;
    const durationHours = duration ? Number((duration).toFixed(2)) : 0;
    return (
      <div style={{ fontSize: '12px', lineHeight: '1.5' }}>
        <p><strong>工序名称：</strong>{procedure?.procedureName || '未知'}（{procedure?.procedureNo || '未知'}）</p>
        <p><strong>工作中心：</strong>{workCenter?.name || '未知'}</p>
        <p><strong>开始时间：</strong>{startTime || '未知'}</p>
        <p><strong>结束时间：</strong>{endTime || '未知'}</p>
        <p><strong>持续时间：</strong>{durationHours} 分钟</p>
        <p><strong>工序状态：</strong>{procedure?.status || '未知'}</p>
        <p><strong>工序序号：</strong>{procedure?.procedureNo || '未知'}</p>
        {procedure?.parallel && <p><strong>并行工序：</strong>是</p>}
        <p><strong>机器工时：</strong>{procedure?.machineMinutes || 0} 分钟</p>
      </div>
    );
  };

  // 为工序分配颜色，确保nextProcedureNo中下一道工序的背景颜色相同
  const getProcedureColor = (procedure: Procedure | undefined) => {
    // 只有并行工序显示背景颜色，其他工序使用默认颜色
    if (!procedure || !procedure.parallel) {
      return '#f5f5f5'; // 非并行工序使用默认背景色，不突出显示
    }
    
    // 为不同的并行工序组分配不同的颜色
    const colors = [
      '#e6f7ff', // 浅蓝色
      '#f6ffed', // 浅绿色
      '#fff7e6', // 浅黄色
      '#fff1f0', // 浅红色
      '#f9f0ff', // 浅紫色
      '#e6fffb', // 浅青色
      '#fffbe6', // 浅金色
      '#f0f5ff'  // 浅蓝紫色
    ];
    
    // 使用工序号的十位数字作为颜色分配的依据
    // 这样可以确保同一组的并行工序（如50和60，它们的十位都是5）有相同的颜色
    const tensDigit = Math.floor(procedure.procedureNo / 10);
    const index = tensDigit % colors.length;
    return colors[index];
  };

  // 显示拆分弹窗
  const showSplitModal = (timeslotId: string) => {
    setCurrentTimeslotId(timeslotId);
    setDays(0); // 重置天数为0
    setIsModalVisible(true);
  };

  // 渲染单元格内容
  const renderCellContent = (timeslots?: Timeslot[], currentDate?: string) => {
    if (!timeslots || timeslots.length === 0) {
      return <div style={{ textAlign: 'center', padding: '8px', color: '#999' }}>未安排</div>;
    }
    
    // 显示时间槽在当前日期的时间段
    const getDisplayTimeRange = (timeslot: Timeslot, date: string) => {
      if (!timeslot.startTime || !timeslot.endTime) return '';
      
      const startTime = timeslot.startTime;
      const endTime = timeslot.endTime;
      const startDate = startTime.substring(0, 10);
      const endDate = endTime.substring(0, 10);
      
      if (startDate === date && endDate === date) {
        // 同一日期内的时间槽
        return `${startTime.substring(11, 16)} - ${endTime.substring(11, 16)}`;
      } else if (startDate === date) {
        // 开始日期
        return `${startTime.substring(11, 16)} - 24:00`;
      } else if (endDate === date) {
        // 结束日期
        return `00:00 - ${endTime.substring(11, 16)}`;
      } else {
        // 中间日期
        return '00:00 - 24:00';
      }
    };
    
    return (
      <div style={{ fontSize: '11px', display: 'flex', flexDirection: 'column', gap: '2px' }}>
        {timeslots.map((ts) => {
          const baseBgColor = getProcedureColor(ts.procedure);
          return (
            <Tooltip 
              key={ts.id}
              title={formatProcedureDetail(ts)}
              placement="topLeft"
              mouseEnterDelay={1} // 1秒后显示
              overlayStyle={{ 
                maxWidth: '300px', 
                padding: '10px',
                borderRadius: '6px',
                boxShadow: '0 4px 12px rgba(0,0,0,0.15)'
              }}
            >
              <div 
                style={{ 
                  padding: '4px', 
                  background: baseBgColor, 
                  borderRadius: '4px',
                  border: '1px solid #e8e8e8',
                  boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
                  cursor: 'pointer',
                  transition: 'all 0.3s ease'
                }}
                onMouseOver={(e) => {
                  // 添加悬停效果
                  e.currentTarget.style.background = '#e6f7ff';
                  e.currentTarget.style.borderColor = '#91d5ff';
                }}
                onMouseOut={(e) => {
                  // 移除悬停效果
                  e.currentTarget.style.background = baseBgColor;
                  e.currentTarget.style.borderColor = '#e8e8e8';
                }}
              >
                <div 
                  style={{ 
                    fontWeight: 'bold', 
                    color: '#333', 
                    marginBottom: '2px',
                    cursor: ts.workCenter?.workCenterCode === "PM10W200" ? 'pointer' : 'default'
                  }}
                  onClick={() => {
                    if (ts.workCenter?.workCenterCode === "PM10W200") {
                      showSplitModal(ts.id);
                    }
                  }}
                >
                  {ts.procedure?.procedureName || '未知'}（{ts.procedure?.procedureNo || '未知'}）
                </div>
                {currentDate && (
                  <div style={{ color: '#666', fontSize: '10px' }}>
                    {getDisplayTimeRange(ts, currentDate)}
                  </div>
                )}
                <div style={{ marginTop: '2px' }}>
                  <Tag 
                    color={
                      ts.procedure?.status === '执行中' ? 'blue' :
                      ts.procedure?.status === '执行完成' ? 'green' :
                      ts.procedure?.status === '待执行' ? 'orange' : 'yellow'
                    }>
                    {ts.procedure?.status || '未知'}
                  </Tag>
                </div>
              </div>
            </Tooltip>
          );
        })}
      </div>
    );
  };

  // 格式化日期显示
  const formatDate = (dateStr: string) => {
    // 将YYYY-MM-DD格式转换为更友好的显示
    return moment(dateStr).format('M月D日');
  };

  // 动态生成表格列
  const generateColumns = () => {
    const columns: ColumnType<TableData>[] = [
      {
        title: '任务号',
        dataIndex: 'taskNo',
        key: 'taskNo',
        width: 200,
        fixed: 'left' as const,
        align: 'center' as const,
        ellipsis: true,
        render: (text: string) => (
          <div style={{ fontWeight: 'bold', color: '#1890ff' }}>{text}</div>
        )
      },
    ];

    // 添加日期列
    dateColumns.forEach(date => {
      columns.push({
        title: formatDate(date),
        dataIndex: date,
        key: date,
        width: 180,
        align: 'center' as const,
        render: (timeslots: Timeslot[]) => renderCellContent(timeslots, date),
        className: 'date-column'
      });
    });

    return columns;
  };

  // 获取时间槽跨越的所有日期
  const getDatesInRange = (startTime: string, endTime: string): string[] => {
    const startDate = moment(startTime.substring(0, 10));
    const endDate = moment(endTime.substring(0, 10));
    const dates: string[] = [];
    
    const currentDate = moment(startDate);
    while (currentDate.isSameOrBefore(endDate, 'day')) {
      dates.push(currentDate.format('YYYY-MM-DD'));
      currentDate.add(1, 'day');
    }
    
    return dates;
  };

  // 根据任务号分组数据
  const groupTimeslotsByTask = (timeslots: Timeslot[]): TaskData => {
    return timeslots.reduce((acc, timeslot) => {
      // 空值检查
      if (!timeslot.task || !timeslot.procedure || !timeslot.startTime || !timeslot.endTime) {
        return acc;
      }
      
      const key = timeslot.task.taskNo;
      if (!acc[key]) {
        acc[key] = { timeslots: [], dateMap: new Map() };
      }
      acc[key].timeslots.push(timeslot);
      
      // 按日期分组时间槽
      const dates = getDatesInRange(timeslot.startTime, timeslot.endTime);
      dates.forEach(date => {
        if (!acc[key].dateMap.has(date)) {
          acc[key].dateMap.set(date, []);
        }
        acc[key].dateMap.get(date)?.push(timeslot);
      });
      
      return acc;
    }, {} as TaskData);
  };

  // 提取所有日期
  const extractDates = (timeslots: Timeslot[]): string[] => {
    const dateSet = new Set<string>();
    
    timeslots.forEach(timeslot => {
      if (timeslot.startTime) {
        const startDate = timeslot.startTime.substring(0, 10);
        dateSet.add(startDate);
      }
      if (timeslot.endTime) {
        const endDate = timeslot.endTime.substring(0, 10);
        dateSet.add(endDate);
      }
    });
    
    // 按日期排序
    return Array.from(dateSet).sort();
  };

  // 构建表格数据
  const buildTableData = (groupedData: TaskData, dates: string[]): TableData[] => {
    return Object.entries(groupedData).map(([, data]) => {
      // 从第一个timeslot中直接获取taskNo
      const firstTimeslot = data.timeslots[0];
      const taskNo = firstTimeslot?.task?.taskNo || '';
      const row: TableData = {
        key: taskNo, // 将任务号作为key
        taskNo
      };
      
      // 为每个日期添加列数据
      dates.forEach(date => {
          row[date] = data.dateMap.get(date) || [];
      });
      
      return row;
    });
  };

  useEffect(() => {

    // 获取数据并预处理
    const fetchData = async () => {
      setLoading(true);
      try {
        const response = await getTimeslotList();
        // 使用类型断言处理响应
        const apiResponse = response as unknown as { code: number; msg: string; data: { timeslots: Timeslot[] } };
        if (apiResponse.code === 200) {
          const timeslots = apiResponse.data.timeslots;
          // 按订单号和任务号分组数据
          const grouped = groupTimeslotsByTask(timeslots);
          // 提取所有日期
          const dates = extractDates(timeslots);
          setDateColumns(dates);
          // 构建表格数据
          const table = buildTableData(grouped, dates);
          setTableData(table);
        } else {
          message.error('网络请求失败');
        }
      } catch {
        message.error('网络请求失败');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  // 处理弹窗确认
  const handleOk = async () => {
    if (days <= 0) {
      message.warning('请输入大于0的天数');
      return;
    }

    try {
      await splitOutsourcingTimeslot(currentTimeslotId, days);
      message.success('拆分成功');
      setIsModalVisible(false);
      // 刷新数据
      const response = await getTimeslotList();
      const apiResponse = response as unknown as { code: number; msg: string; data: { timeslots: Timeslot[] } };
      if (apiResponse.code === 200) {
        const timeslots = apiResponse.data.timeslots;
        // 重新构建表格数据
        const grouped = groupTimeslotsByTask(timeslots);
        const dates = extractDates(timeslots);
        setDateColumns(dates);
        setTableData(buildTableData(grouped, dates));
      } else {
        message.error('获取数据失败: ' + apiResponse.msg);
      }
    } catch (error) {
      message.error('拆分失败: ' + (error instanceof Error ? error.message : '未知错误'));
    }
  };

  // 处理弹窗取消
  const handleCancel = () => {
    setIsModalVisible(false);
  };

  return (
    <div style={{ padding: '20px' }}>
      <h1 style={{ marginBottom: '20px', fontSize: '20px', color: '#262626' }}>生产调度时序表</h1>
      <Spin spinning={loading}>
        <Card>
          <Table
            columns={generateColumns()}
            dataSource={tableData}
            scroll={{ x: 'max-content', y: 600 }}
            pagination={false}
            size="middle"
            bordered
            rowKey="key"
            className="scheduling-timeline-table"
            style={{ borderCollapse: 'collapse' }}
          />
        </Card>
        {!loading && tableData.length === 0 && (
          <div style={{ textAlign: 'center', padding: '50px', color: '#999', fontSize: '16px' }}>
            暂无调度数据
          </div>
        )}
      </Spin>

      {/* 外协工序时间槽拆分弹窗 */}
      <Modal
        title="输入预计完成天数"
        open={isModalVisible}
        onOk={handleOk}
        onCancel={handleCancel}
        okText="确认"
        cancelText="取消"
      >
        <div style={{ marginBottom: '16px' }}>
          <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>预计完成天数：</label>
          <InputNumber
            min={1}
            value={days}
            onChange={(value) => setDays(value || 0)}
            style={{ width: '100%' }}
            placeholder="请输入天数"
          />
        </div>
      </Modal>
    </div>
  );
};

export default SchedulingTimelinePage;
