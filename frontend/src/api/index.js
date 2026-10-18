import request from './request'

/** 生成标准 RESTful CRUD 接口集 */
export function crudApi(base) {
  return {
    page: (params) => request.get(`${base}/page`, { params }),
    all: () => request.get(`${base}/all`),
    detail: (id) => request.get(`${base}/${id}`),
    add: (data) => request.post(base, data),
    edit: (data) => request.put(base, data),
    remove: (ids) => request.delete(`${base}/${[].concat(ids).join(',')}`)
  }
}

// ---------------- 登录
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  info: () => request.get('/auth/info'),
  logout: () => request.post('/auth/logout')
}

// ---------------- 业务模块
export const teacherApi = crudApi('/teacher')
export const clazzApi = crudApi('/clazz')
export const childApi = crudApi('/child')
export const attendanceApi = crudApi('/attendance')
export const courseApi = crudApi('/course')
export const feeApi = crudApi('/fee')
export const leaveApi = crudApi('/leave')
export const noticeApi = crudApi('/notice')
export const menuApi = crudApi('/menu')
export const foodApi = crudApi('/food')

// 食谱：周视图 / 复制上周
export const menuWeek = (weekStart) => request.get('/menu/week', { params: { weekStart } })
export const copyLastWeekMenu = (weekStart) =>
  request.post('/menu/copy-last-week', null, { params: { weekStart } })

// 食材出入库
export const foodOverview = () => request.get('/food/overview')
export const stockChange = (id, data) => request.post(`/food/${id}/stock`, data)
export const stockRecords = (params) => request.get('/food/record/page', { params })

// 家长端：我的孩子（档案 + 今日考勤 + 近 7 天趋势 + 账单 + 请假）
export const myChildApi = () => request.get('/child/mine')

export const dictApi = crudApi('/dict')
// 按类型取字典项（下拉框用）
dictApi.type = (dictType) => request.get(`/dict/type/${dictType}`)
export const logApi = crudApi('/log')

// 业务动作
export const payFee = (id) => request.put(`/fee/pay/${id}`)
export const approveLeave = (id, pass, approver) =>
  request.put(`/leave/approve/${id}`, null, { params: { pass, approver } })
export const cleanLog = () => request.delete('/log/clean')
export const allergyChildren = () => request.get('/child/allergy')
// 幼儿档案 Excel：批量导入与模板下载
export const importChildren = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/child/import', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export const childImportTemplate = () =>
  request.get('/child/import/template', { responseType: 'blob' })
// 幼儿档案导出：参数与列表查询一致，导出的是「当前筛选结果」而不是全表
// 导出比普通请求慢（后端要拼整个 xlsx），单独放宽超时
export const exportChildren = (params) =>
  request.get('/child/export', { params, responseType: 'blob', timeout: 60000 })
// 幼儿档案变更留痕
export const childHistory = (id) => request.get(`/child/${id}/history`)
// 公告状态机：立即发布 / 撤回
export const publishNotice = (id) => request.post(`/notice/${id}/publish`)
export const revokeNotice = (id) => request.post(`/notice/${id}/revoke`)

// ---------------- 看板与统计报表
export const statApi = {
  overview: () => request.get('/stat/overview'),
  trend: () => request.get('/stat/trend'),
  report: () => request.get('/stat/report')
}

// ---------------- 系统设置
export const systemApi = {
  info: () => request.get('/system/info'),
  backup: () => request.post('/system/backup'),
  backupList: () => request.get('/system/backup/list'),
  removeBackup: (name) => request.delete(`/system/backup/${name}`)
}
