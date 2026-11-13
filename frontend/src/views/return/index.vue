<template>
  <div class="page-banner">
    <img :src="bannerImg" alt="banner" />
  </div>
  <el-card shadow="never">
    <el-form :inline="true" :model="query">
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已归还" value="COMPLETED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button v-if="userStore.hasPerm('asset:return:submit')" type="primary" @click="openSubmit">归还登记</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="returnNo" label="归还单号" width="170" />
      <el-table-column prop="assetName" label="资产" min-width="150" show-overflow-tooltip />
      <el-table-column prop="assetCode" label="资产编号" width="150" />
      <el-table-column prop="userName" label="归还人" width="100" />
      <el-table-column prop="reason" label="归还原因" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="returnStatusType[row.status] || 'info'">{{ returnStatusMap[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="returnTime" label="归还时间" width="170" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING' && userStore.hasPerm('asset:return:audit')" link type="success" @click="openAudit(row)">审核</el-button>
          <el-button link type="primary" @click="viewRecords(row)">流程</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="load" @current-change="load" />

    <!-- 归还登记 -->
    <el-dialog v-model="submitDialog" title="归还登记" width="520px" :close-on-click-modal="false">
      <el-form ref="submitRef" :model="submitForm" :rules="submitRules" label-width="100px">
        <el-form-item label="归还资产" prop="assetId">
          <el-select v-model="submitForm.assetId" filterable placeholder="请选择自己名下使用中的资产" style="width: 100%">
            <el-option v-for="a in myAssets" :key="a.id" :label="`${a.assetCode} - ${a.assetName}`" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="归还原因"><el-input v-model="submitForm.reason" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitDialog = false">取消</el-button>
        <el-button type="primary" @click="doSubmit">提交</el-button>
      </template>
    </el-dialog>

    <!-- 审核 -->
    <el-dialog v-model="auditDialog" title="归还审核" width="480px">
      <el-form label-width="90px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.result">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="0">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见"><el-input v-model="auditForm.comment" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialog = false">取消</el-button>
        <el-button type="primary" @click="doAudit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 审批流程 -->
    <el-dialog v-model="recordsDialog" title="审核流程" width="560px">
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
import { ElMessage } from 'element-plus'
import { pageReturn, submitReturn, auditReturn, returnRecords, pageAsset } from '@/api'
import { useUserStore } from '@/store/user'
import bannerImg from '@/assets/return-banner.png'

const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const myAssets = ref([])

const returnStatusMap = { PENDING: '待审核', COMPLETED: '已归还', REJECTED: '已驳回' }
const returnStatusType = { PENDING: 'warning', COMPLETED: 'success', REJECTED: 'danger' }

const query = reactive({ pageNum: 1, pageSize: 10, status: '' })

const submitDialog = ref(false)
const submitRef = ref()
const submitForm = reactive({ assetId: null, reason: '' })
const submitRules = { assetId: [{ required: true, message: '请选择资产', trigger: 'change' }] }

const auditDialog = ref(false)
const auditId = ref(null)
const auditForm = reactive({ result: 1, comment: '' })

const recordsDialog = ref(false)
const records = ref([])

async function load() {
  loading.value = true
  try {
    const res = await pageReturn(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function openSubmit() {
  Object.assign(submitForm, { assetId: null, reason: '' })
  const res = await pageAsset({ pageNum: 1, pageSize: 500, status: 'IN_USE', custodianId: userStore.userInfo.userId })
  myAssets.value = res.data.records
  submitDialog.value = true
}

async function doSubmit() {
  await submitRef.value.validate()
  await submitReturn({ ...submitForm })
  ElMessage.success('归还已提交')
  submitDialog.value = false
  load()
}

function openAudit(row) {
  auditId.value = row.id
  auditForm.result = 1
  auditForm.comment = ''
  auditDialog.value = true
}

async function doAudit() {
  await auditReturn({ returnId: auditId.value, ...auditForm })
  ElMessage.success('审核完成')
  auditDialog.value = false
  load()
}

async function viewRecords(row) {
  const res = await returnRecords(row.id)
  records.value = res.data
  recordsDialog.value = true
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
