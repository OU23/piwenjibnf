<template>
  <div>
    <div class="page-banner">
      <img :src="bannerImg" alt="banner" />
    </div>
    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-header">
              <span>资产状态分布</span>
              <el-button link type="primary" @click="refresh">刷新</el-button>
            </div>
          </template>
          <div ref="pieRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="panel">
          <template #header>资产分类 TOP 分布</template>
          <div ref="barRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="panel">
      <template #header>近 6 个月资产入库与分配趋势</template>
      <div ref="lineRef" class="chart tall"></div>
    </el-card>

    <el-card shadow="never" class="panel">
      <template #header>资产状态明细</template>
      <el-table :data="statusTable" border stripe>
        <el-table-column prop="name" label="状态" min-width="120" />
        <el-table-column prop="value" label="数量" width="140" align="center" />
        <el-table-column label="占比" min-width="200">
          <template #default="{ row }">
            <el-progress :percentage="row.percent" :stroke-width="14" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { statisticsStatus, statisticsCategory, statisticsTrend } from '@/api'
import bannerImg from '@/assets/statistics-banner.png'

const pieRef = ref()
const barRef = ref()
const lineRef = ref()
const statusData = ref([])
const categoryData = ref([])
const trendData = ref({})

let pieChart, barChart, lineChart

const statusTable = computed(() => {
  const total = statusData.value.reduce((s, d) => s + (Number(d.value) || 0), 0)
  return statusData.value.map(d => ({
    name: d.name,
    value: d.value,
    percent: total ? Math.round((Number(d.value) / total) * 100) : 0
  }))
})

function initPie(data) {
  pieChart = echarts.init(pieRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { type: 'scroll', orient: 'vertical', right: 10, top: 'center' },
    series: [{
      type: 'pie',
      radius: ['35%', '62%'],
      center: ['40%', '50%'],
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      data: data.map(d => ({ name: d.name, value: d.value }))
    }]
  })
}

function initBar(data) {
  barChart = echarts.init(barRef.value)
  barChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 50, right: 20, top: 20, bottom: 80 },
    xAxis: {
      type: 'category',
      data: data.map(d => d.name),
      axisLabel: { interval: 0, rotate: 35 }
    },
    yAxis: { type: 'value' },
    series: [{
      name: '数量',
      type: 'bar',
      barMaxWidth: 32,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#409EFF' },
          { offset: 1, color: '#79bbff' }
        ]),
        borderRadius: [4, 4, 0, 0]
      },
      data: data.map(d => d.value)
    }]
  })
}

function initLine(data) {
  const assetTrend = data.assetTrend || []
  const allocationTrend = data.allocationTrend || []
  const months = [...new Set([
    ...assetTrend.map(d => d.month),
    ...allocationTrend.map(d => d.month)
  ])].sort()
  const assetMap = Object.fromEntries(assetTrend.map(d => [d.month, d.count]))
  const allocMap = Object.fromEntries(allocationTrend.map(d => [d.month, d.count]))

  lineChart = echarts.init(lineRef.value)
  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['入库', '分配'] },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: months, boundaryGap: false },
    yAxis: { type: 'value' },
    series: [
      { name: '入库', type: 'line', smooth: true, areaStyle: { opacity: 0.12 }, data: months.map(m => assetMap[m] || 0) },
      { name: '分配', type: 'line', smooth: true, areaStyle: { opacity: 0.12 }, data: months.map(m => allocMap[m] || 0) }
    ]
  })
}

function resizeAll() {
  pieChart && pieChart.resize()
  barChart && barChart.resize()
  lineChart && lineChart.resize()
}

async function refresh() {
  const [status, category, trend] = await Promise.all([
    statisticsStatus(),
    statisticsCategory(),
    statisticsTrend()
  ])
  statusData.value = status.data
  categoryData.value = category.data
  trendData.value = trend.data
  pieChart.setOption({ series: [{ data: statusData.value.map(d => ({ name: d.name, value: d.value })) }] })
  barChart.setOption({ xAxis: { data: categoryData.value.map(d => d.name) }, series: [{ data: categoryData.value.map(d => d.value) }] })
}

onMounted(async () => {
  const [status, category, trend] = await Promise.all([
    statisticsStatus(),
    statisticsCategory(),
    statisticsTrend()
  ])
  statusData.value = status.data
  categoryData.value = category.data
  trendData.value = trend.data
  initPie(statusData.value)
  initBar(categoryData.value)
  initLine(trendData.value)
  window.addEventListener('resize', resizeAll)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeAll)
  pieChart && pieChart.dispose()
  barChart && barChart.dispose()
  lineChart && lineChart.dispose()
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
.panel {
  margin-bottom: 16px;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chart {
  height: 320px;
  width: 100%;
}
.chart.tall {
  height: 360px;
}
</style>
