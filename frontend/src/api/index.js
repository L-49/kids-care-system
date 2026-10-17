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
