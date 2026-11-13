<template>
  <div class="page">
    <div class="page-banner">
      <img :src="bannerImg" alt="banner" />
    </div>
    <el-card shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="角色名称">
          <el-input v-model="query.roleName" placeholder="角色名称" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="query.roleName = ''; load()">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button v-if="userStore.hasPerm('system:role:add')" type="primary" @click="openAdd">新增角色</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="roleCode" label="角色编码" width="150" />
        <el-table-column prop="description" label="描述" min-width="180" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="userStore.hasPerm('system:role:edit')" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="userStore.hasPerm('system:role:edit')" link type="warning" @click="openAssign(row)">分配权限</el-button>
            <el-button v-if="userStore.hasPerm('system:role:delete')" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pager" v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </el-card>

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialog" :title="form.id ? '编辑角色' : '新增角色'" width="480px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="角色编码" prop="roleCode"><el-input v-model="form.roleCode" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限 -->
    <el-dialog v-model="assignDialog" title="分配权限" width="480px">
      <el-tree ref="treeRef" :data="menuData" show-checkbox node-key="id" :props="{ label: 'menuName', children: 'children' }" default-expand-all />
      <template #footer>
        <el-button @click="assignDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageRole, addRole, updateRole, deleteRole, getRoleMenus, assignRoleMenus, menuTree } from '@/api'
import { useUserStore } from '@/store/user'
import bannerImg from '@/assets/role-banner.png'

const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, roleName: '' })

const dialog = ref(false)
const formRef = ref()
const form = reactive({ id: null, roleName: '', roleCode: '', description: '', sort: 0, status: 1 })

const assignDialog = ref(false)
const assignRoleId = ref(null)
const treeRef = ref()
const menuData = ref([])

const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const res = await pageRole(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openAdd() {
  Object.assign(form, { id: null, roleName: '', roleCode: '', description: '', sort: 0, status: 1 })
  dialog.value = true
}

function openEdit(row) {
  Object.assign(form, { id: row.id, roleName: row.roleName, roleCode: row.roleCode, description: row.description, sort: row.sort, status: row.status })
  dialog.value = true
}

async function save() {
  await formRef.value.validate()
  if (form.id) {
    await updateRole({ ...form })
  } else {
    await addRole({ ...form })
  }
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}

function remove(row) {
  ElMessageBox.confirm(`确认删除角色「${row.roleName}」吗？`, '提示', { type: 'warning' }).then(async () => {
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    load()
  })
}

async function openAssign(row) {
  assignRoleId.value = row.id
  const [menuRes, roleMenuRes] = await Promise.all([menuTree(), getRoleMenus(row.id)])
  menuData.value = menuRes.data
  assignDialog.value = true
  // 回显已勾选
  setTimeout(() => {
    roleMenuRes.data.forEach((id) => treeRef.value.setChecked(id, true, false))
  }, 50)
}

async function saveAssign() {
  const menuIds = treeRef.value.getCheckedKeys().concat(treeRef.value.getHalfCheckedKeys())
  await assignRoleMenus(assignRoleId.value, menuIds)
  ElMessage.success('权限分配成功')
  assignDialog.value = false
}

onMounted(load)
</script>

<style scoped>
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
