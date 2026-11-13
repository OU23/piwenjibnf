<template>
  <div class="page">
    <div class="page-banner">
      <img :src="bannerImg" alt="banner" />
    </div>
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="query">
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="用户名" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="query.realName" placeholder="姓名" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button v-if="userStore.hasPerm('system:user:add')" type="primary" @click="openAdd">新增用户</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="department" label="部门" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-if="userStore.hasPerm('system:user:edit')" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="userStore.hasPerm('system:user:edit')" link type="warning" @click="openReset(row)">重置密码</el-button>
            <el-button v-if="userStore.hasPerm('system:user:delete')" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="load"
        @current-change="load"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialog" :title="form.id ? '编辑用户' : '新增用户'" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password :placeholder="form.id ? '留空则不修改' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.department" placeholder="请选择部门" clearable filterable style="width: 100%">
            <el-option v-for="d in departments" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <div style="display:flex;gap:0;width:100%;border:1px solid #dcdfe6;border-radius:4px;overflow:hidden">
            <el-input v-model="form.emailPrefix" placeholder="邮箱账号或手机号码" style="flex:1" :style="{border:'none'}" />
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
        <el-form-item label="手机号">
          <div style="display:flex;gap:8px;width:100%">
            <el-select v-model="form.countryCode" placeholder="区号" style="width:100px;flex-shrink:0">
              <el-option label="+86 中国" value="+86" />
              <el-option label="+852 香港" value="+852" />
              <el-option label="+853 澳门" value="+853" />
              <el-option label="+886 台湾" value="+886" />
              <el-option label="+1 美国" value="+1" />
              <el-option label="+44 英国" value="+44" />
              <el-option label="+81 日本" value="+81" />
              <el-option label="+82 韩国" value="+82" />
            </el-select>
            <el-input v-model="form.phone" placeholder="请输入手机号" style="flex:1" />
          </div>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="resetDialog" title="重置密码" width="400px">
      <el-form label-width="90px">
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDialog = false">取消</el-button>
        <el-button type="primary" @click="doReset">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageUser, addUser, updateUser, deleteUser, getUserRoles, resetPassword, listRole } from '@/api'
import { useUserStore } from '@/store/user'
import bannerImg from '@/assets/user-banner.png'

const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const roles = ref([])
const departments = ['人事部', '技术部', '财务部', '市场部', '销售部', '行政部', '采购部', '运维部', '研发部', '总经理办公室']
const query = reactive({ pageNum: 1, pageSize: 10, username: '', realName: '', status: null })

const dialog = ref(false)
const formRef = ref()
const form = reactive({ id: null, username: '', password: '', realName: '', department: '', emailPrefix: '', emailSuffix: '@163.com', email: '', countryCode: '+86', phone: '', roleIds: [], status: 1, remark: '' })

const resetDialog = ref(false)
const resetId = ref(null)
const newPassword = ref('')

function buildEmail() {
  form.email = (form.emailPrefix || '') + (form.emailSuffix || '')
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const res = await pageUser(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.username = ''
  query.realName = ''
  query.status = null
  query.pageNum = 1
  load()
}

function openAdd() {
  Object.assign(form, { id: null, username: '', password: '', realName: '', department: '', emailPrefix: '', emailSuffix: '@163.com', email: '', phone: '', roleIds: [], status: 1, remark: '' })
  dialog.value = true
}

async function openEdit(row) {
  const res = await getUserRoles(row.id)
  const at = (row.email || '').indexOf('@')
  const prefix = at > 0 ? row.email.slice(0, at) : (row.email || '')
  const suffix = at > 0 ? row.email.slice(at) : '@163.com'
  Object.assign(form, {
    id: row.id, username: row.username, password: '', realName: row.realName,
    department: row.department, emailPrefix: prefix, emailSuffix: suffix, email: row.email,
    phone: row.phone, roleIds: res.data, status: row.status, remark: row.remark
  })
  dialog.value = true
}

async function save() {
  await formRef.value.validate()
  buildEmail()
  if (form.id) {
    await updateUser(form)
  } else {
    await addUser(form)
  }
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}

function remove(row) {
  ElMessageBox.confirm(`确认删除用户「${row.username}」吗？`, '提示', { type: 'warning' }).then(async () => {
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    load()
  })
}

function openReset(row) {
  resetId.value = row.id
  newPassword.value = ''
  resetDialog.value = true
}

async function doReset() {
  if (!newPassword.value || newPassword.value.length < 6) {
    ElMessage.warning('密码长度至少 6 位')
    return
  }
  await resetPassword(resetId.value, newPassword.value)
  ElMessage.success('密码已重置')
  resetDialog.value = false
}

onMounted(async () => {
  load()
  const res = await listRole()
  roles.value = res.data
})
</script>

<style scoped>
.page {
  background: #fff;
}
.page-banner {
  margin-bottom: 16px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.06);
}
.page-banner img {
  width: 100%;
  height: 160px;
  object-fit: cover;
  display: block;
}
.toolbar {
  margin-bottom: 12px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
