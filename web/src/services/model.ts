// 订单任务数据类型定义
export interface OrderTask {
  taskNo: string;
  orderNo: string;
  orderName: string;
  routeSeq: string | null;
  planQuantity: number;
  taskStatus: string;
  planStartDate: string;
  planEndDate: string;
  factStartDate: string | null;
  factEndDate: string | null;
  orderPlanQuantity: number;
  orderStatus: string;
  contractNum: string;
}

// 任务数据类型定义（适配/api/orders/tasks接口）
export interface Task {
  taskNo: string;
  orderNo: string;
  status: string;
  factStartDate: string | null;
  factEndDate: string | null;
  planStartDate: string;
  planEndDate: string;
  priority: number;
}

// 查询参数接口
export interface OrderTaskQueryParams {
  orderName?: string;
  startTime?: string;
  endTime?: string;
  statusList?: string[];
  pageNum?: number;
  pageSize?: number;
  orderNo?: string;
  taskNo?: string;
  taskStatus?: string;
}

// API响应接口，适配新的返回格式
export interface ApiResponse<T> {
  code: number;
  msg: string;
  data: T;
  total?: number;
}

// 排序接口
export interface Sort {
  empty: boolean;
  sorted: boolean;
  unsorted: boolean;
}

// 分页信息接口
export interface Pageable {
  sort: Sort;
  offset: number;
  pageNumber: number;
  pageSize: number;
  paged: boolean;
  unpaged: boolean;
}

// Spring Data Page接口结构，适配新的返回格式
export interface SpringDataPage<T> {
  content: T[];
  pageable: Pageable;
  last: boolean;
  totalElements: number;
  totalPages: number;
  number: number; // 从0开始的页码
  first: boolean;
  sort: Sort;
  size: number;
  numberOfElements: number;
  empty: boolean;
}

// 前端分页响应接口
export interface PageResponse {
  total: number;
  records: OrderTask[];
  totalPages: number;
  pageSize: number;
  pageNum: number;
}
