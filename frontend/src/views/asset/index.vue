<template>
  <div class="page-banner">
    <img :src="bannerImg" alt="banner" />
  </div>
  <el-card shadow="never">
    <el-form :inline="true" :model="query">
      <el-form-item label="资产名称">
        <el-input v-model="query.assetName" placeholder="资产名称" clearable @keyup.enter="load" />
      </el-form-item>
      <el-form-item label="资产编号">
        <el-input v-model="query.assetCode" placeholder="资产编号" clearable @keyup.enter="load" />
      </el-form-item>
      <el-form-item label="分类">
        <el-tree-select v-model="query.categoryId" :data="categoryTreeData" node-key="id" :props="{ label: 'categoryName' }" check-strictly clearable placeholder="全部分类" style="width: 180px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
          <el-option v-for="(v, k) in statusMap" :key="k" :label="v" :value="k" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button v-if="userStore.hasPerm('asset:add')" type="primary" @click="openAdd">资产登记</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="assetCode" label="资产编号" width="150" />
      <el-table-column prop="assetName" label="资产名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column prop="brand" label="品牌" width="100" />
      <el-table-column prop="model" label="型号" width="140" show-overflow-tooltip />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType[row.status] || 'info'">{{ statusMap[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="price" label="价格" width="100" align="right">
        <template #default="{ row }">{{ row.price ? '¥' + row.price : '-' }}</template>
      </el-table-column>
      <el-table-column prop="custodianName" label="保管人" width="100" />
      <el-table-column prop="location" label="存放位置" width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button v-if="userStore.hasPerm('asset:edit')" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="userStore.hasPerm('asset:repair') && row.status === 'IN_USE'" link type="warning" @click="repair(row)">报修</el-button>
          <el-button v-if="userStore.hasPerm('asset:repair') && row.status === 'REPAIRING'" link type="success" @click="repairDone(row)">维修完成</el-button>
          <el-button v-if="userStore.hasPerm('asset:scrap') && ['IN_STOCK', 'IN_USE', 'REPAIRING'].includes(row.status)" link type="warning" @click="scrap(row)">报废</el-button>
          <el-button v-if="userStore.hasPerm('asset:delete')" link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="load" @current-change="load" />

    <el-dialog v-model="dialog" :title="form.id ? '编辑资产' : '资产登记'" width="560px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="资产名称" prop="assetName"><el-input v-model="form.assetName" /></el-form-item>
        <el-form-item label="资产编号"><el-input v-model="form.assetCode" placeholder="留空自动生成" /></el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-tree-select v-model="form.categoryId" :data="categoryTreeData" node-key="id" :props="{ label: 'categoryName' }" check-strictly placeholder="请选择分类" style="width: 100%" />
        </el-form-item>
        <el-form-item label="品牌"><el-input v-model="form.brand" /></el-form-item>
        <el-form-item label="型号"><el-input v-model="form.model" /></el-form-item>
        <el-form-item label="序列号"><el-input v-model="form.serialNumber" /></el-form-item>
        <el-form-item label="购置价格"><el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="购置日期"><el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="存放位置">
          <el-select v-model="form.location" placeholder="请选择存放位置" clearable filterable style="width: 100%">
            <el-option label="北京" value="北京" />
            <el-option label="上海" value="上海" />
            <el-option label="广州" value="广州" />
            <el-option label="深圳" value="深圳" />
            <el-option label="成都" value="成都" />
            <el-option label="杭州" value="杭州" />
            <el-option label="武汉" value="武汉" />
            <el-option label="南京" value="南京" />
            <el-option label="重庆" value="重庆" />
            <el-option label="西安" value="西安" />
            <el-option label="天津" value="天津" />
            <el-option label="苏州" value="苏州" />
            <el-option label="长沙" value="长沙" />
            <el-option label="郑州" value="郑州" />
            <el-option label="青岛" value="青岛" />
            <el-option label="厦门" value="厦门" />
            <el-option label="合肥" value="合肥" />
            <el-option label="济南" value="济南" />
            <el-option label="福州" value="福州" />
            <el-option label="大连" value="大连" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
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
import { pageAsset, addAsset, updateAsset, deleteAsset, scrapAsset, repairAsset, repairDoneAsset, categoryTree } from '@/api'
import { useUserStore } from '@/store/user'
import bannerImg from '@/assets/asset-banner.png'

const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const categoryTreeData = ref([])

const statusMap = {
  IN_STOCK: '在库',
  APPLIED: '已申请',
  ALLOCATED: '已分配',
  IN_USE: '使用中',
  PENDING_RETURN: '待归还',
  REPAIRING: '维修中',
  SCRAPPED: '已报废'
}
const statusType = {
  IN_STOCK: 'info',
  APPLIED: 'warning',
  ALLOCATED: 'primary',
  IN_USE: 'success',
  PENDING_RETURN: 'warning',
  REPAIRING: 'danger',
  SCRAPPED: 'danger'
}

const query = reactive({ pageNum: 1, pageSize: 10, assetName: '', assetCode: '', categoryId: null, status: '', brand: '' })

const dialog = ref(false)
const formRef = ref()
const form = reactive({ id: null, assetName: '', assetCode: '', categoryId: null, brand: '', model: '', serialNumber: '', price: null, purchaseDate: '', location: '', remark: '' })

const rules = {
  assetName: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

async function load() {
  loading.value = true
  try {
    const res = await pageAsset(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { pageNum: 1, assetName: '', assetCode: '', categoryId: null, status: '', brand: '' })
  load()
}

function openAdd() {
  Object.assign(form, { id: null, assetName: '', assetCode: '', categoryId: null, brand: '', model: '', serialNumber: '', price: null, purchaseDate: '', location: '', remark: '' })
  dialog.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id, assetName: row.assetName, assetCode: row.assetCode, categoryId: row.categoryId,
    brand: row.brand, model: row.model, serialNumber: row.serialNumber, price: row.price,
    purchaseDate: row.purchaseDate, location: row.location, remark: row.remark
  })
  dialog.value = true
}

async function save() {
  await formRef.value.validate()
  if (form.id) {
    await updateAsset(form)
  } else {
    await addAsset(form)
  }
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}

function remove(row) {
  ElMessageBox.confirm(`确认删除资产「${row.assetName}」吗？`, '提示', { type: 'warning' }).then(async () => {
    await deleteAsset(row.id)
    ElMessage.success('删除成功')
    load()
  })
}

function scrap(row) {
  ElMessageBox.confirm(`确认报废资产「${row.assetName}」吗？`, '提示', { type: 'warning' }).then(async () => {
    await scrapAsset(row.id)
    ElMessage.success('已报废')
    load()
  })
}

function repair(row) {
  ElMessageBox.confirm(`确认为「${row.assetName}」办理报修吗？`, '提示', { type: 'warning' }).then(async () => {
    await repairAsset(row.id)
    ElMessage.success('已报修')
    load()
  })
}

function repairDone(row) {
  ElMessageBox.confirm(`确认「${row.assetName}」维修完成？`, '提示', { type: 'info' }).then(async () => {
    await repairDoneAsset(row.id)
    ElMessage.success('已恢复使用')
    load()
  })
}

onMounted(async () => {
  load()
  const res = await categoryTree()
  categoryTreeData.value = res.data
})
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
