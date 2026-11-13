<template>
  <div class="page-banner">
    <img :src="bannerImg" alt="banner" />
  </div>
  <el-card shadow="never">
    <el-form :inline="true" :model="query">
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
          <el-option label="待审批" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
          <el-option label="已撤销" value="CANCELLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-checkbox v-model="onlyMine" @change="load">只看我的申请</el-checkbox>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button v-if="userStore.hasPerm('asset:application:submit')" type="primary" @click="openSubmit">发起申请</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="applicationNo" label="申请单号" width="170" />
      <el-table-column prop="applicantName" label="申请人" width="100" />
      <el-table-column prop="categoryName" label="申请分类" width="120" />
      <el-table-column label="资产" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.assetName || ('数量: ' + row.quantity) }}</template>
      </el-table-column>
      <el-table-column prop="quantity" label="数量" width="70" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="appStatusType[row.status] || 'info'">{{ appStatusMap[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="申请事由" min-width="160" show-overflow-tooltip />
      <el-table-column prop="applyTime" label="申请时间" width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING' && userStore.hasPerm('asset:application:approve')" link type="success" @click="openApprove(row)">审批</el-button>
          <el-button v-if="['PENDING', 'APPROVED'].includes(row.status) && row.applicantId === userStore.userInfo.userId" link type="warning" @click="cancel(row)">撤销</el-button>
          <el-button link type="primary" @click="viewRecords(row)">流程</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="load" @current-change="load" />

    <!-- 发起申请 -->
    <el-dialog v-model="submitDialog" title="发起资产申请" width="520px" :close-on-click-modal="false">
      <el-form ref="submitRef" :model="submitForm" :rules="submitRules" label-width="100px">
        <el-form-item label="申请分类" prop="categoryId">
          <el-tree-select v-model="submitForm.categoryId" :data="categoryTreeData" node-key="id" :props="{ label: 'categoryName' }" check-strictly placeholder="请选择分类" style="width: 100%" @change="onCategoryChange" />
        </el-form-item>
        <el-form-item label="申请数量">
          <el-input-number v-model="submitForm.quantity" :min="1" :disabled="submitForm.specific" />
        </el-form-item>
        <el-form-item>
          <el-switch v-model="submitForm.specific" active-text="指定具体资产" />
        </el-form-item>
        <el-form-item v-if="submitForm.specific" label="具体资产">
          <el-select v-model="submitForm.assetId" filterable placeholder="请选择资产" style="width: 100%">
            <el-option v-for="a in availableAssets" :key="a.id" :label="`${a.assetCode} - ${a.assetName}`" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请事由" prop="reason"><el-input v-model="submitForm.reason" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitDialog = false">取消</el-button>
        <el-button type="primary" @click="doSubmit">提交</el-button>
      </template>
    </el-dialog>

    <!-- 审批 -->
    <el-dialog v-model="approveDialog" title="审批申请" width="480px">
      <el-form label-width="90px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="approveForm.result">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="0">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见"><el-input v-model="approveForm.comment" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialog = false">取消</el-button>
        <el-button type="primary" @click="doApprove">确定</el-button>
      </template>
    </el-dialog>

    <!-- 审批流程 -->
    <el-dialog v-model="recordsDialog" title="审批流程" width="560px">
      <el-timeline>
        <el-timeline-item v-for="r in records" :key="r.id" :timestamp="r.approveTime" :type="r.result === 1 ? 'success' : 'danger'">
          <div><b>{{ r.approverName }}</b> {{ r.result === 1 ? '通过' : '驳回' }}</div>
          <div v-if="r.comment" style="color:#999">意见：{{ r.comment }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageApplication, submitApplication, approveApplication, cancelApplication, applicationRecords, categoryTree, pageAsset } from '@/api'
import { useUserStore } from '@/store/user'
import bannerImg from '@/assets/application-banner.png'

const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const onlyMine = ref(false)
const categoryTreeData = ref([])
const availableAssets = ref([])

const appStatusMap = { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回', CANCELLED: '已撤销' }
const appStatusType = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }

const query = reactive({ pageNum: 1, pageSize: 10, status: '' })

const submitDialog = ref(false)
const submitRef = ref()
const submitForm = reactive({ categoryId: null, quantity: 1, specific: false, assetId: null, reason: '' })
const submitRules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  reason: [{ required: true, message: '请填写申请事由', trigger: 'blur' }]
}

const approveDialog = ref(false)
const approveId = ref(null)
const approveForm = reactive({ result: 1, comment: '' })

const recordsDialog = ref(false)
const records = ref([])

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (onlyMine.value) params.applicantId = userStore.userInfo.userId
    const res = await pageApplication(params)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openSubmit() {
  Object.assign(submitForm, { categoryId: null, quantity: 1, specific: false, assetId: null, reason: '' })
  submitDialog.value = true
}

async function onCategoryChange(categoryId) {
  availableAssets.value = []
  submitForm.assetId = null
  if (categoryId && submitForm.specific) {
    const res = await pageAsset({ pageNum: 1, pageSize: 500, status: 'IN_STOCK', categoryId })
    availableAssets.value = res.data.records
  }
}

async function doSubmit() {
  await submitRef.value.validate()
  await submitApplication({ ...submitForm })
  ElMessage.success('申请已提交')
  submitDialog.value = false
  load()
}

function openApprove(row) {
  approveId.value = row.id
  approveForm.result = 1
  approveForm.comment = ''
  approveDialog.value = true
}

async function doApprove() {
  await approveApplication(approveId.value, { businessId: approveId.value, result: approveForm.result, comment: approveForm.comment })
  ElMessage.success('审批完成')
  approveDialog.value = false
  load()
}

function cancel(row) {
  ElMessageBox.confirm('确认撤销该申请吗？', '提示', { type: 'warning' }).then(async () => {
    await cancelApplication(row.id)
    ElMessage.success('已撤销')
    load()
  })
}

async function viewRecords(row) {
  const res = await applicationRecords(row.id)
  records.value = res.data
  recordsDialog.value = true
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
