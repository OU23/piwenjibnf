<template>
  <div class="page-banner">
    <img :src="bannerImg" alt="banner" />
  </div>
  <el-card shadow="never">
    <el-form :inline="true" :model="query">
      <el-form-item label="操作人">
        <el-input v-model="query.username" placeholder="用户名" clearable @keyup.enter="load" />
      </el-form-item>
      <el-form-item label="模块">
        <el-input v-model="query.module" placeholder="模块" clearable @keyup.enter="load" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="query.username = ''; query.module = ''; load()">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="操作人" width="110" />
      <el-table-column prop="module" label="模块" width="120" />
      <el-table-column prop="operationType" label="操作类型" width="90" align="center" />
      <el-table-column prop="requestMethod" label="请求方式" width="90" align="center" />
      <el-table-column prop="requestUrl" label="请求地址" min-width="160" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP" width="130" />
      <el-table-column prop="duration" label="耗时(ms)" width="90" align="center" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="操作时间" width="170" />
    </el-table>

    <el-pagination class="pager" v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="load" @current-change="load" />
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { pageLog } from '@/api'
import bannerImg from '@/assets/log-banner.png'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, username: '', module: '' })

async function load() {
  loading.value = true
  try {
    const res = await pageLog(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
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
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
