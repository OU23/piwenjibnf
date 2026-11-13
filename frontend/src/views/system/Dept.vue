<template>
  <div class="page">
    <div class="page-banner">
      <img :src="bannerImg" alt="部门管理" />
      <div class="banner-stats">
        <div class="stat-item">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">部门总数</div>
        </div>
        <div class="stat-divider" />
        <div class="stat-item">
          <div class="stat-value">{{ stats.enabled }}</div>
          <div class="stat-label">启用中</div>
        </div>
        <div class="stat-divider" />
        <div class="stat-item">
          <div class="stat-value">{{ stats.disabled }}</div>
          <div class="stat-label">已禁用</div>
        </div>
      </div>
    </div>

    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="部门名称">
          <el-input v-model="query.deptName" placeholder="请输入部门名称" clearable style="width: 180px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="query.leader" placeholder="请输入负责人" clearable style="width: 150px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="query.phone" placeholder="请输入联系电话" clearable style="width: 160px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 工具栏 -->
      <div class="toolbar">
        <el-button v-if="userStore.hasPerm('system:dept:add')" type="primary" @click="openAdd">新增部门</el-button>
        <el-button v-if="userStore.hasPerm('system:dept:import')" type="success" plain @click="importDialog = true">批量导入</el-button>
        <el-button v-if="userStore.hasPerm('system:dept:export')" type="warning" plain @click="handleExport">导出</el-button>
        <el-button v-if="!searching" text type="primary" @click="toggleExpand">{{ expanded ? '全部折叠' : '全部展开' }}</el-button>
        <el-tag v-if="searching" type="info" closable @close="reset" style="margin-left: 8px">
          搜索模式：共 {{ total }} 条结果，关闭返回部门树
        </el-tag>
      </div>

      <!-- 树形表格（默认视图） -->
      <el-table
        v-if="!searching"
        :key="tableKey"
        :data="treeData"
        row-key="id"
        :default-expand-all="expanded"
        :tree-props="{ children: 'children' }"
        v-loading="loading"
        border
      >
        <el-table-column prop="deptName" label="部门名称" min-width="220" />
        <el-table-column prop="leader" label="负责人" width="110" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="70" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="userStore.hasPerm('system:dept:add')" link type="primary" @click="openAddChild(row)">新增子部门</el-button>
            <el-button v-if="userStore.hasPerm('system:dept:edit')" link type="primary" @click="openEdit(row)">修改</el-button>
            <el-button v-if="userStore.hasPerm('system:dept:delete')" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页列表（搜索视图） -->
      <template v-else>
        <el-table :data="listData" v-loading="loading" border>
          <el-table-column prop="deptName" label="部门名称" min-width="160" />
          <el-table-column prop="parentName" label="上级部门" min-width="120" />
          <el-table-column prop="leader" label="负责人" width="110" />
          <el-table-column prop="phone" label="联系电话" width="140" />
          <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
          <el-table-column prop="sort" label="排序" width="70" align="center" />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="170" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="userStore.hasPerm('system:dept:edit')" link type="primary" @click="openEdit(row)">修改</el-button>
              <el-button v-if="userStore.hasPerm('system:dept:delete')" link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          class="pager"
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadPage"
          @size-change="loadPage"
        />
      </template>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialog" :title="form.id ? '修改部门' : '新增部门'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            node-key="id"
            value-key="id"
            check-strictly
            default-expand-all
            :render-after-expand="false"
            placeholder="不选则为顶级部门"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="form.deptName" placeholder="请输入部门名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.leader" placeholder="请输入负责人" maxlength="64" />
        </el-form-item>
        <el-form-item label="联系电话">
          <div class="phone-input">
            <el-select v-model="form.countryCode" placeholder="区号" style="width: 110px; flex-shrink: 0">
              <el-option label="+86 中国" value="+86" />
              <el-option label="+852 香港" value="+852" />
              <el-option label="+853 澳门" value="+853" />
              <el-option label="+886 台湾" value="+886" />
              <el-option label="+1 美国" value="+1" />
              <el-option label="+44 英国" value="+44" />
              <el-option label="+81 日本" value="+81" />
              <el-option label="+82 韩国" value="+82" />
            </el-select>
            <el-input v-model="form.phoneNumber" placeholder="请输入联系电话" maxlength="32" style="flex: 1" />
          </div>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <div class="email-input">
            <el-input v-model="form.emailPrefix" placeholder="邮箱账号" style="flex:1" :style="{border:'none'}" />
            <el-select v-model="form.emailSuffix" placeholder="@后缀" style="width:130px;flex-shrink:0" :style="{border:'none',borderLeft:'1px solid #dcdfe6',borderRadius:0}">
              <el-option label="@qq.com" value="@qq.com" />
              <el-option label="@126.com" value="@126.com" />
              <el-option label="@163.com" value="@163.com" />
              <el-option label="@gmail.com" value="@gmail.com" />
              <el-option label="@outlook.com" value="@outlook.com" />
              <el-option label="@sina.com" value="@sina.com" />
              <el-option label="@sohu.com" value="@sohu.com" />
              <el-option label="@hotmail.com" value="@hotmail.com" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog v-model="importDialog" title="批量导入部门" width="520px" destroy-on-close>
      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        <p>1. CSV 文件，UTF-8 编码，首行为表头（可先下载模板）</p>
        <p>2. 列顺序：部门名称,上级部门,负责人,联系电话,邮箱,排序,状态,备注</p>
        <p>3. 上级部门按名称匹配，留空为顶级部门；同名部门自动跳过</p>
      </el-alert>
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".csv"
        :on-change="(file) => (importFile = file.raw)"
        drag
      >
        <el-icon size="40" color="#409eff"><UploadFilled /></el-icon>
        <div class="el-upload__text">将 CSV 文件拖到此处，或<em>点击选择</em></div>
      </el-upload>
      <template #footer>
        <el-button @click="downloadTemplate">下载模板</el-button>
        <el-button @click="importDialog = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="handleImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deptTree, pageDept, addDept, updateDept, deleteDept, exportDept, importDept } from '@/api'
import { useUserStore } from '@/store/user'
import bannerImg from '@/assets/dept-banner.png'

const userStore = useUserStore()
const loading = ref(false)
const treeData = ref([])
const listData = ref([])
const total = ref(0)
const searching = ref(false)
const expanded = ref(false)
const tableKey = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, deptName: '', leader: '', phone: '', status: null })

const stats = reactive({ total: 0, enabled: 0, disabled: 0 })

const dialog = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  parentId: 0,
  deptName: '',
  leader: '',
  phone: '',           // 提交给后端的拼接字符串，如 "+86 0755-88886666"
  countryCode: '+86',   // 单独维护区号，便于回显
  phoneNumber: '',      // 单独维护号码部分
  email: '',
  emailPrefix: '',      // 邮箱前缀（@ 之前）
  emailSuffix: '@163.com', // 邮箱后缀下拉
  sort: 0,
  status: 1,
  remark: ''
})

/** 把后端存的 "phone" 拆成下拉框与号码输入框；空字符串也兼容 */
function splitPhone(stored) {
  if (!stored) return { countryCode: '+86', phoneNumber: '' }
  const m = String(stored).match(/^(\+\d+)\s*(.*)$/)
  if (m && ['+86', '+852', '+853', '+886', '+1', '+44', '+81', '+82'].includes(m[1])) {
    return { countryCode: m[1], phoneNumber: m[2].trim() }
  }
  // 兼容旧数据：纯号码
  return { countryCode: '+86', phoneNumber: stored }
}

/** 合并回后端要的 phone 字段 */
function joinPhone() {
  return form.phoneNumber ? `${form.countryCode} ${form.phoneNumber}`.trim() : ''
}

/** 把后端存的 email 拆成前缀 + 后缀；兼容已有企业域名 */
function splitEmail(stored) {
  if (!stored) return { emailPrefix: '', emailSuffix: '@163.com' }
  const at = String(stored).indexOf('@')
  if (at > 0) return { emailPrefix: stored.slice(0, at), emailSuffix: stored.slice(at) }
  return { emailPrefix: stored, emailSuffix: '@163.com' }
}

/** 合并回后端要的 email 字段 */
function joinEmail() {
  return (form.emailPrefix || '') + (form.emailSuffix || '')
}

const importDialog = ref(false)
const importing = ref(false)
const importFile = ref(null)
const uploadRef = ref()

const rules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

/** 上级部门可选树（编辑时排除自身及子树，防止循环层级） */
const parentOptions = computed(() => {
  if (!form.id) return treeData.value
  const prune = (nodes) => nodes
    .filter((n) => n.id !== form.id)
    .map((n) => ({ ...n, children: n.children ? prune(n.children) : undefined }))
  return prune(treeData.value)
})

async function loadTree() {
  loading.value = true
  try {
    const res = await deptTree()
    treeData.value = res.data
    // 计算统计数据
    const flat = []
    const walk = (nodes) => nodes.forEach((n) => { flat.push(n); n.children && walk(n.children) })
    walk(res.data)
    stats.total = flat.length
    stats.enabled = flat.filter((n) => n.status === 1).length
    stats.disabled = flat.filter((n) => n.status === 0).length
  } finally {
    loading.value = false
  }
}

async function loadPage() {
  loading.value = true
  try {
    const res = await pageDept(query)
    listData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function search() {
  if (!query.deptName && !query.leader && !query.phone && query.status === null) {
    searching.value = false
    loadTree()
    return
  }
  searching.value = true
  query.pageNum = 1
  loadPage()
}

function reset() {
  query.deptName = ''
  query.leader = ''
  query.phone = ''
  query.status = null
  searching.value = false
  loadTree()
}

function toggleExpand() {
  expanded.value = !expanded.value
  tableKey.value++ // 强制重渲染表格使展开状态生效
}

function openAdd() {
  Object.assign(form, {
    id: null, parentId: 0, deptName: '', leader: '', phone: '', countryCode: '+86', phoneNumber: '',
    email: '', emailPrefix: '', emailSuffix: '@163.com', sort: 0, status: 1, remark: ''
  })
  dialog.value = true
}

function openAddChild(row) {
  Object.assign(form, {
    id: null, parentId: row.id, deptName: '', leader: '', phone: '', countryCode: '+86', phoneNumber: '',
    email: '', emailPrefix: '', emailSuffix: '@163.com', sort: 0, status: 1, remark: ''
  })
  dialog.value = true
}

function openEdit(row) {
  const { countryCode, phoneNumber } = splitPhone(row.phone)
  const { emailPrefix, emailSuffix } = splitEmail(row.email)
  Object.assign(form, {
    id: row.id, parentId: row.parentId || 0, deptName: row.deptName, leader: row.leader || '',
    phone: row.phone || '', countryCode, phoneNumber,
    email: row.email || '', emailPrefix, emailSuffix,
    sort: row.sort ?? 0, status: row.status, remark: row.remark || ''
  })
  dialog.value = true
}

async function save() {
  await formRef.value.validate()
  const payload = { ...form, parentId: form.parentId || 0, phone: joinPhone(), email: joinEmail() }
  if (form.id) {
    await updateDept(payload)
  } else {
    await addDept(payload)
  }
  ElMessage.success(form.id ? '修改成功' : '新增成功')
  dialog.value = false
  searching.value ? loadPage() : loadTree()
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除部门「${row.deptName}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteDept(row.id)
      ElMessage.success('删除成功')
      searching.value ? loadPage() : loadTree()
    })
    .catch(() => {})
}

async function handleExport() {
  const res = await exportDept()
  const url = URL.createObjectURL(new Blob([res.data], { type: 'text/csv;charset=UTF-8' }))
  const a = document.createElement('a')
  a.href = url
  a.download = `部门列表-${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

function downloadTemplate() {
  const csv = '\uFEFF部门名称,上级部门,负责人,联系电话,邮箱,排序,状态,备注\r\n研发三组,技术部,张三,0755-88886008,dev3@company.com,3,启用,示例数据\r\n测试部,,李四,0755-88886009,test@company.com,6,启用,顶级部门示例\r\n'
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=UTF-8' }))
  const a = document.createElement('a')
  a.href = url
  a.download = '部门导入模板.csv'
  a.click()
  URL.revokeObjectURL(url)
}

async function handleImport() {
  if (!importFile.value) {
    ElMessage.warning('请先选择 CSV 文件')
    return
  }
  importing.value = true
  try {
    const fd = new FormData()
    fd.append('file', importFile.value)
    const res = await importDept(fd)
    ElMessage.success(res.data.message || '导入完成')
    importDialog.value = false
    importFile.value = null
    searching.value ? loadPage() : loadTree()
  } catch (e) {
    // 错误信息已由拦截器提示
  } finally {
    importing.value = false
  }
}

onMounted(loadTree)
</script>

<style scoped>
.page {
  background: #fff;
  padding: 4px 0 24px;
}
.page-banner {
  position: relative;
  margin-bottom: 18px;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}
.page-banner img {
  width: 100%;
  height: 168px;
  object-fit: cover;
  display: block;
}
.banner-stats {
  position: absolute;
  right: 28px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  gap: 0;
  padding: 14px 22px;
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.92), rgba(67, 56, 202, 0.92));
  border-radius: 10px;
  color: #fff;
  backdrop-filter: blur(6px);
  box-shadow: 0 6px 18px rgba(67, 56, 202, 0.35);
}
.stat-item {
  padding: 0 16px;
  text-align: center;
  min-width: 78px;
}
.stat-value {
  font-size: 26px;
  font-weight: 600;
  line-height: 1.2;
  font-family: 'Helvetica Neue', Arial, sans-serif;
}
.stat-label {
  font-size: 12px;
  opacity: 0.88;
  margin-top: 2px;
}
.stat-divider {
  width: 1px;
  height: 36px;
  background: rgba(255, 255, 255, 0.28);
}
.toolbar {
  margin-bottom: 14px;
}
.pager {
  margin-top: 18px;
  justify-content: flex-end;
}

/* 区号+电话 复合输入框：让下拉与输入框视觉连接（共享边框） */
.phone-input {
  display: flex;
  align-items: center;
  width: 100%;
}
.phone-input :deep(.el-select .el-input__wrapper),
.phone-input :deep(.el-input__wrapper) {
  border-radius: 4px;
}
.phone-input :deep(.el-select) .el-input__wrapper {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
  border-right: none;
}
.phone-input :deep(.el-input:not(.el-select)) .el-input__wrapper {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}

/* 邮箱前缀+后缀 复合输入框：整体外框包裹，内部控件去边框（参照用户管理邮箱框） */
.email-input {
  display: flex;
  gap: 0;
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}
.email-input :deep(.el-input__wrapper),
.email-input :deep(.el-select .el-input__wrapper) {
  box-shadow: none !important;
  border-radius: 0;
}

/* 表格行高更舒展 */
:deep(.el-table__row) {
  font-size: 14px;
}
:deep(.el-table .cell) {
  padding: 0 12px;
}
:deep(.el-table__row td) {
  padding: 10px 0;
}
</style>
