// ====================================
// 基础数据模型定义
// ====================================

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

// ====================================
// 查询参数接口定义
// ====================================

// 订单任务查询参数接口
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

// ====================================
// 分页相关接口定义
// ====================================

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

// ====================================
// API通用响应接口定义
// ====================================

// API响应接口，适配新的返回格式
export interface ApiResponse<T> {
    code: number;
    msg: string;
    data: T;
    total?: number;
}

// ====================================
// 认证相关接口定义
// ====================================

// 登录请求接口
export interface LoginRequest {
    username: string;
    password: string;
}

// 登录响应接口
export interface LoginResponse {
    token: string;
    type: string;
    username: string;
    email: string;
    phone: string;
}

// 注册请求接口
export interface RegisterRequest {
    username: string;
    password: string;
    email: string;
    phone: string;
}

// 注册响应接口
export interface RegisterResponse {
    code: number;
    msg: string;
    data: string;
}

// 用户信息接口
export interface UserInfo {
    username: string;
    name?: string;
    permissions?: string[];
    isLoggedIn: boolean;
}

// 工作中心类型定义
export interface WorkCenter {
    id: string;
    workCenterCode: string;
    name: string;
    status: string;
}

// 工序类型定义
export interface Procedure {
    id: string;
    taskNo: string;
    orderNo: string;
    workCenterId: WorkCenter;
    procedureName: string;
    procedureNo: number;
    machineMinutes: number;
    nextProcedureNo: number[];
    startTime: string;
    endTime: string;
    planStartDate: string | null;
    planEndDate: string | null;
    status: string;
    parallel: boolean;
    index: number;
}

// 订单类型定义
export interface Order {
    orderNo: string;
    erpStatus: string;
    orderStatus: string;
    planStartDate: string;
    planEndDate: string;
    factStartDate: string | null;
    factEndDate: string | null;
}

// 维护计划类型定义
export interface Maintenance {
    id: string;
    workCenter: WorkCenter;
    year: number;
    date: string;
    capacity: number;
    status: string;
    description: string | null;
    startTime: string;
    endTime: string;
    usageTime: number;
    remainingCapacity: number;
}

// 时间槽类型定义
export interface Timeslot {
    id: string;
    problemId: number;
    procedure: Procedure;
    order: Order;
    task: Task;
    workCenter: WorkCenter;
    duration: number;
    priority: number;
    startTime: string;
    endTime: string;
    maintenance: Maintenance;
    parallel: boolean;
    manual: boolean;
    index: number;
    total: number;
    procedureIndex: number;
}
