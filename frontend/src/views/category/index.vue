<template>
  <div class="page-banner">
    <img :src="bannerImg" alt="banner" />
  </div>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button v-if="userStore.hasPerm('asset:category:add')" type="primary" @click="openAdd(null)">新增顶级分类</el-button>
    </div>

    <el-table :data="tree" row-key="id" border default-expand-all :tree-props="{ children: 'children' }" v-loading="loading">
      <el-table-column prop="categoryName" label="分类名称" min-width="200" />
      <el-table-column prop="categoryCode" label="分类编码" width="160" />
      <el-table-column prop="level" label="层级" width="70" align="center" />
      <el-table-column prop="sort" label="排序" width="70" align="center" />
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button v-if="userStore.hasPerm('asset:category:add')" link type="primary" @click="openAdd(row)">新增子类</el-button>
          <el-button v-if="userStore.hasPerm('asset:category:edit')" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="userStore.hasPerm('asset:category:delete')" link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑分类' : '新增分类'" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级分类">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            node-key="id"
            :props="{ label: 'categoryName' }"
            check-strictly
            placeholder="不选则为顶级分类"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="分类名称" prop="categoryName"><el-input v-model="form.categoryName" /></el-form-item>
        <el-form-item label="分类编码" prop="categoryCode"><el-input v-model="form.categoryCode" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="属性配置">
          <el-input v-model="form.attributes" type="textarea" placeholder='JSON 数组，如 [{"name":"CPU型号","type":"text"}]' />
        </el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryTree, addCategory, updateCategory, deleteCategory } from '@/api'
import { useUserStore } from '@/store/user'
import bannerImg from '@/assets/category-banner.png'

const userStore = useUserStore()
const loading = ref(false)
const tree = ref([])
const parentOptions = ref([])

const dialog = ref(false)
const formRef = ref()
const form = reactive({ id: null, parentId: null, categoryName: '', categoryCode: '', sort: 0, description: '', attributes: '', status: 1 })

const rules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  categoryCode: [{ required: true, message: '请输入分类编码', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const res = await categoryTree()
    tree.value = res.data
    parentOptions.value = [{ id: 0, categoryName: '顶级分类', children: res.data }]
  } finally {
    loading.value = false
  }
}

function openAdd(parent) {
  Object.assign(form, { id: null, parentId: parent ? parent.id : null, categoryName: '', categoryCode: '', sort: 0, description: '', attributes: '', status: 1 })
  dialog.value = true
}

function openEdit(row) {
  Object.assign(form, { id: row.id, parentId: row.parentId, categoryName: row.categoryName, categoryCode: row.categoryCode, sort: row.sort, description: row.description, attributes: row.attributes, status: row.status })
  dialog.value = true
}

async function save() {
  await formRef.value.validate()
  const data = { ...form }
  if (!data.parentId) data.parentId = 0
  if (data.id) {
    await updateCategory(data)
  } else {
    await addCategory(data)
  }
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}

function remove(row) {
  ElMessageBox.confirm(`确认删除分类「${row.categoryName}」吗？`, '提示', { type: 'warning' }).then(async () => {
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    load()
  })
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
</style>
