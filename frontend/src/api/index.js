import request from './request'

// ---------- 认证 ----------
export const login = (data) => request.post('/auth/login', data)
export const getInfo = () => request.get('/auth/info')
export const changePassword = (data) => request.put('/auth/password', data)

// ---------- 用户 ----------
export const pageUser = (params) => request.get('/user/page', { params })
export const listUser = () => request.get('/user/list')
export const getUser = (id) => request.get(`/user/${id}`)
export const getUserRoles = (id) => request.get(`/user/${id}/roles`)
export const addUser = (data) => request.post('/user', data)
export const updateUser = (data) => request.put('/user', data)
export const deleteUser = (id) => request.delete(`/user/${id}`)
export const resetPassword = (id, newPassword) => request.put(`/user/${id}/password`, null, { params: { newPassword } })

// ---------- 角色 ----------
export const pageRole = (params) => request.get('/role/page', { params })
export const listRole = () => request.get('/role/list')
export const getRoleMenus = (id) => request.get(`/role/${id}/menus`)
export const assignRoleMenus = (id, menuIds) => request.put(`/role/${id}/menus`, menuIds)
export const addRole = (data) => request.post('/role', data)
export const updateRole = (data) => request.put('/role', data)
export const deleteRole = (id) => request.delete(`/role/${id}`)

// ---------- 菜单 ----------
export const menuTree = () => request.get('/menu/tree')

// ---------- 部门 ----------
export const deptTree = () => request.get('/dept/tree')
export const pageDept = (params) => request.get('/dept/page', { params })
export const addDept = (data) => request.post('/dept', data)
export const updateDept = (data) => request.put('/dept', data)
export const deleteDept = (id) => request.delete(`/dept/${id}`)
export const exportDept = () => request.get('/dept/export', { responseType: 'blob' })
export const importDept = (formData) => request.post('/dept/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })

// ---------- 分类 ----------
export const categoryTree = () => request.get('/category/tree')
export const listCategory = () => request.get('/category/list')
export const addCategory = (data) => request.post('/category', data)
export const updateCategory = (data) => request.put('/category', data)
export const deleteCategory = (id) => request.delete(`/category/${id}`)

// ---------- 资产 ----------
export const pageAsset = (params) => request.get('/asset/page', { params })
export const getAsset = (id) => request.get(`/asset/${id}`)
export const addAsset = (data) => request.post('/asset', data)
export const updateAsset = (data) => request.put('/asset', data)
export const deleteAsset = (id) => request.delete(`/asset/${id}`)
export const scrapAsset = (id) => request.put(`/asset/${id}/scrap`)
export const repairAsset = (id) => request.put(`/asset/${id}/repair`)
export const repairDoneAsset = (id) => request.put(`/asset/${id}/repairDone`)

// ---------- 资产申请 ----------
export const submitApplication = (data) => request.post('/application', data)
export const pageApplication = (params) => request.get('/application/page', { params })
export const approveApplication = (id, data) => request.put(`/application/${id}/approve`, data)
export const cancelApplication = (id) => request.put(`/application/${id}/cancel`)
export const applicationRecords = (id) => request.get(`/application/${id}/records`)

// ---------- 资产分配 ----------
export const allocate = (data) => request.post('/allocation', data)
export const autoAllocate = (data) => request.post('/allocation/auto', data)
export const confirmAllocation = (id) => request.put(`/allocation/${id}/confirm`)
export const pageAllocation = (params) => request.get('/allocation/page', { params })
export const myPendingAllocation = () => request.get('/allocation/my/pending')

// ---------- 资产归还 ----------
export const submitReturn = (data) => request.post('/return', data)
export const auditReturn = (data) => request.put('/return/audit', data)
export const pageReturn = (params) => request.get('/return/page', { params })
export const returnRecords = (id) => request.get(`/return/${id}/records`)

// ---------- 统计 ----------
export const statisticsDashboard = () => request.get('/statistics/dashboard')
export const statisticsStatus = () => request.get('/statistics/status')
export const statisticsCategory = () => request.get('/statistics/category')
export const statisticsTrend = () => request.get('/statistics/trend')

// ---------- 操作日志 ----------
export const pageLog = (params) => request.get('/log/page', { params })

// ---------- 文件存储 ----------
// 存储后端（本地磁盘 / 对象存储）由后端配置决定，前端调用方式完全一致
/**
 * 上传文件
 * @param {FormData} formData 含 file 字段
 * @param {object} options { bizType 业务分类, bizId 业务主键, scope 归属范围 }
 */
export const uploadFile = (formData, { bizType = 'common', bizId = null, scope = 'public' } = {}) =>
  request.post('/file/upload', formData, {
    params: { bizType, bizId, scope },
    headers: { 'Content-Type': 'multipart/form-data' }
  })

/** 秒传校验：先算好文件 MD5 探一下，命中就不用重复上传 */
export const checkFile = (md5, size) => request.post('/file/check', null, { params: { md5, size } })

/** 分页查询文件 */
export const pageFile = (params) => request.get('/file/page', { params })

/** 查询某个业务对象名下的全部文件 */
export const listFileByBiz = (bizType, bizId) => request.get('/file/biz', { params: { bizType, bizId } })

/** 删除文件（按记录 ID，引用计数归零才删物理文件） */
export const deleteFile = (id) => request.delete(`/file/${id}`)

/** 当前生效的存储类型与渠道 */
export const getStorageInfo = () => request.get('/file/storage')

/** 按存储后端统计文件数量与体积（用于观察迁移进度） */
export const fileStatistic = () => request.get('/file/statistic')

/**
 * 根据对象键拼出可直接访问的地址（预览 / 下载统一入口）
 * @param {string} objectKey 上传接口返回的 objectKey
 * @param {boolean} download 是否作为附件下载
 */
export const fileUrl = (objectKey, download = false) => {
  if (!objectKey) return ''
  // 已是完整 URL（对象存储直链或预签名地址）时直接返回
  if (/^https?:\/\//i.test(objectKey)) return objectKey
  return `/api/file/preview/${objectKey}${download ? '?download=true' : ''}`
}
