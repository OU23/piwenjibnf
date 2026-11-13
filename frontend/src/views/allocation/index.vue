<template>
  <div class="page-banner">
    <img :src="bannerImg" alt="banner" />
  </div>
  <el-card shadow="never">
    <el-form :inline="true" :model="query">
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
          <el-option label="待领用" value="ALLOCATED" />
          <el-option label="已领用" value="IN_USE" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button v-if="userStore.hasPerm('asset:allocation:allocate')" type="primary" @click="openAllocate">手动分配</el-button>
      <el-button v-if="userStore.hasPerm('asset:allocation:allocate')" type="success" @click="openAuto">自动分配</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="allocationNo" label="分配单号" width="170" />
      <el-table-column prop="assetName" label="资产" min-width="150" show-overflow-tooltip />
      <el-table-column prop="assetCode" label="资产编号" width="150" />
      <el-table-column prop="fromUserName" label="原保管人" width="100">
        <template #default="{ row }">{{ row.fromUserName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="toUserName" label="分配对象" width="100" />
      <el-table-column label="分配方式" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.allocationType === 'AUTO' ? 'success' : 'primary'">{{ row.allocationType === 'AUTO' ? '自动' : '手动' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'IN_USE' ? 'success' : 'warning'">{{ row.status === 'IN_USE' ? '已领用' : '待领用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="allocateTime" label="分配时间" width="170" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'ALLOCATED' && row.toUserId === userStore.userInfo.userId" link type="primary" @click="confirm(row)">确认领用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="load" @current-change="load" />

    <!-- 手动分配 -->
    <el-dialog v-model="allocDialog" title="手动分配资产" width="560px" :close-on-click-modal="false">
      <el-form ref="allocRef" :model="allocForm" :rules="allocRules" label-width="100px">
        <el-form-item label="分配对象" prop="toUserId">
          <el-select v-model="allocForm.toUserId" filterable placeholder="请选择用户" style="width: 100%">
            <el-option v-for="u in users" :key="u.id" :label="`${u.realName || u.username} (${u.department || '-'})`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择资产" prop="assetIds">
          <el-select v-model="allocForm.assetIds" multiple filterable placeholder="请选择在库/已申请资产" style="width: 100%">
            <el-option v-for="a in availableAssets" :key="a.id" :label="`${a.assetCode} - ${a.assetName}`" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="allocForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="allocDialog = false">取消</el-button>
        <el-button type="primary" @click="doAllocate">分配</el-button>
      </template>
    </el-dialog>

    <!-- 自动分配 -->
    <el-dialog v-model="autoDialog" title="自动分配资产" width="480px" :close-on-click-modal="false">
      <el-form ref="autoRef" :model="autoForm" :rules="autoRules" label-width="100px">
        <el-form-item label="分类" prop="categoryId">
          <el-tree-select v-model="autoForm.categoryId" :data="categoryTreeData" node-key="id" :props="{ label: 'categoryName' }" check-strictly placeholder="请选择分类" style="width: 100%" />
        </el-form-item>
        <el-form-item label="数量" prop="quantity"><el-input-number v-model="autoForm.quantity" :min="1" /></el-form-item>
        <el-form-item label="分配对象" prop="toUserId">
          <el-select v-model="autoForm.toUserId" filterable placeholder="请选择用户" style="width: 100%">
            <el-option v-for="u in users" :key="u.id" :label="`${u.realName || u.username} (${u.department || '-'})`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="autoForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="autoDialog = false">取消</el-button>
        <el-button type="primary" @click="doAuto">自动分配</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageAllocation, allocate, autoAllocate, confirmAllocation, listUser, categoryTree, pageAsset } from '@/api'
import { useUserStore } from '@/store/user'
import bannerImg from '@/assets/allocation-banner.png'

const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const users = ref([])
const categoryTreeData = ref([])
const availableAssets = ref([])

const query = reactive({ pageNum: 1, pageSize: 10, status: '' })

const allocDialog = ref(false)
const allocRef = ref()
const allocForm = reactive({ toUserId: null, assetIds: [], remark: '' })
const allocRules = {
  toUserId: [{ required: true, message: '请选择分配对象', trigger: 'change' }],
  assetIds: [{ required: true, type: 'array', min: 1, message: '请选择资产', trigger: 'change' }]
}

const autoDialog = ref(false)
const autoRef = ref()
const autoForm = reactive({ categoryId: null, quantity: 1, toUserId: null, remark: '' })
const autoRules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  toUserId: [{ required: true, message: '请选择分配对象', trigger: 'change' }]
}

async function load() {
  loading.value = true
  try {
    const res = await pageAllocation(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadAvailableAssets() {
  // 加载可分配资产：在库 + 已申请
  const [inStock, applied] = await Promise.all([
    pageAsset({ pageNum: 1, pageSize: 500, status: 'IN_STOCK' }),
    pageAsset({ pageNum: 1, pageSize: 500, status: 'APPLIED' })
  ])
  availableAssets.value = [...inStock.data.records, ...applied.data.records]
}

async function openAllocate() {
  Object.assign(allocForm, { toUserId: null, assetIds: [], remark: '' })
  await loadAvailableAssets()
  allocDialog.value = true
}

async function doAllocate() {
  await allocRef.value.validate()
  await allocate({ ...allocForm, allocationType: 'MANUAL' })
  ElMessage.success('分配成功')
  allocDialog.value = false
  load()
}

function openAuto() {
  Object.assign(autoForm, { categoryId: null, quantity: 1, toUserId: null, remark: '' })
  autoDialog.value = true
}

async function doAuto() {
  await autoRef.value.validate()
  const count = await autoAllocate({ ...autoForm })
  ElMessage.success(`自动分配完成，共分配 ${count.data} 件资产`)
  autoDialog.value = false
  load()
}

function confirm(row) {
  ElMessageBox.confirm('确认领用该资产吗？', '提示', { type: 'info' }).then(async () => {
    await confirmAllocation(row.id)
    ElMessage.success('已领用')
    load()
  })
}

onMounted(async () => {
  load()
  const [u, c] = await Promise.all([listUser(), categoryTree()])
  users.value = u.data
  categoryTreeData.value = c.data
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
