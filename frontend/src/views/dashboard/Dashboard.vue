<template>
  <div>
    <div class="dashboard-banner">
      <img :src="bannerImg" alt="dashboard banner" />
    </div>

    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :xs="12" :sm="8" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <div class="section-header">
      <span class="section-title">✨ 精选资产</span>
      <span class="section-sub">价值最高的精选好物</span>
    </div>
    <el-row :gutter="16" class="featured-row">
      <el-col v-for="item in featuredAssets" :key="item.id" :xs="12" :sm="8" :md="4">
        <div class="featured-card" :style="{ background: item.gradient }">
          <div class="featured-info">
            <div class="featured-name">{{ item.assetName }}</div>
            <div class="featured-meta">{{ item.categoryName || '未分类' }} · {{ item.statusText }}</div>
            <div class="featured-price">¥{{ formatPrice(item.price) }}</div>
          </div>
          <div class="like-badge" :class="{ liked: !!likedSet[item.id] }" @click.stop="toggleLike(item)">
            <span class="heart">{{ likedSet[item.id] ? '💖' : '🤍' }}</span>
            <span>{{ likeCount(item) }}</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :md="8">
        <el-card shadow="never">
          <template #header>资产状态分布</template>
          <div ref="pieRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="16">
        <el-card shadow="never">
          <template #header>资产分类分布</template>
          <div ref="barRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="chart-row">
      <template #header>近 6 个月资产入库与分配趋势</template>
      <div ref="lineRef" class="chart tall"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { statisticsDashboard, pageAsset } from '@/api'
import bannerImg from '@/assets/dashboard-banner.png'

const cards = ref([])
const featuredAssets = ref([])
const likedSet = ref(JSON.parse(localStorage.getItem('featuredLikes') || '{}'))

const GRADIENTS = [
  'linear-gradient(180deg, #3ED6B5 0%, #4B2E83 100%)',
  'linear-gradient(180deg, #33CCF0 0%, #2D6CDF 100%)',
  'linear-gradient(180deg, #6A8EF0 0%, #8E4BE0 100%)',
  'linear-gradient(180deg, #2BD2FF 0%, #6A5CFF 100%)',
  'linear-gradient(180deg, #38E2C6 0%, #5B3FD6 100%)',
  'linear-gradient(180deg, #4AC0E8 0%, #7B2FF7 100%)'
]

const STATUS_TEXT = {
  IN_STOCK: '在库',
  IN_USE: '使用中',
  REPAIRING: '维修中',
  SCRAPPED: '已报废'
}

function formatPrice(price) {
  const n = Number(price ?? 0)
  return n.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

// 根据资产 ID 生成稳定的初始点赞数，点击后保存在本地
function baseLikes(id) {
  return 50 + ((id * 37) % 400)
}

function likeCount(item) {
  return baseLikes(item.id) + (likedSet.value[item.id] ? 1 : 0)
}

function toggleLike(item) {
  likedSet.value[item.id] = !likedSet.value[item.id]
  localStorage.setItem('featuredLikes', JSON.stringify(likedSet.value))
}

async function loadFeatured() {
  try {
    const res = await pageAsset({ pageNum: 1, pageSize: 100 })
    const records = (res.data.records || []).slice()
      .sort((a, b) => Number(b.price ?? 0) - Number(a.price ?? 0))
      .slice(0, 6)
    featuredAssets.value = records.map((a, i) => ({
      ...a,
      statusText: STATUS_TEXT[a.status] || a.status,
      gradient: GRADIENTS[i % GRADIENTS.length]
    }))
  } catch (e) {
    // 精选板块加载失败不影响首页其他内容
  }
}
const pieRef = ref()
const barRef = ref()
const lineRef = ref()

let pieChart, barChart, lineChart

function initPie(data) {
  pieChart = echarts.init(pieRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      data: data.map(d => ({ name: d.name, value: d.value }))
    }]
  })
}

function initBar(data) {
  barChart = echarts.init(barRef.value)
  barChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 50, right: 20, top: 20, bottom: 60 },
    xAxis: {
      type: 'category',
      data: data.map(d => d.name),
      axisLabel: { interval: 0, rotate: 30 }
    },
    yAxis: { type: 'value' },
    series: [{
      name: '资产数量',
      type: 'bar',
      barMaxWidth: 36,
      itemStyle: { color: '#409EFF', borderRadius: [4, 4, 0, 0] },
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

onMounted(async () => {
  loadFeatured()
  const dash = await statisticsDashboard()
  const d = dash.data
  cards.value = [
    { label: '资产总数', value: d.totalAssets, color: '#409EFF' },
    { label: '资产总价值', value: '¥' + (d.totalValue ?? 0), color: '#E6A23C' },
    { label: '在库资产', value: d.inStockCount, color: '#909399' },
    { label: '使用中', value: d.inUseCount, color: '#67C23A' },
    { label: '维修中', value: d.repairingCount, color: '#F56C6C' },
    { label: '已报废', value: d.scrappedCount, color: '#C0C4CC' },
    { label: '待审批申请', value: d.pendingApplications, color: '#E6A23C' },
    { label: '待归还', value: d.pendingReturns, color: '#E6A23C' }
  ]
  initPie(d.statusDistribution)
  initBar(d.categoryDistribution)
  initLine(d.trend)
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
.stat-card {
  text-align: center;
  margin-bottom: 16px;
}
.stat-value {
  font-size: 28px;
  font-weight: 600;
  line-height: 1.4;
}
.stat-label {
  color: #909399;
  font-size: 13px;
}
.chart-row {
  margin-bottom: 16px;
}
.chart {
  height: 300px;
  width: 100%;
}
.chart.tall {
  height: 340px;
}
.dashboard-banner {
  margin-bottom: 16px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.06);
}
.dashboard-banner img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  display: block;
}
.section-header {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}
.section-title {
  font-size: 17px;
  font-weight: 600;
}
.section-sub {
  color: #909399;
  font-size: 12px;
}
.featured-row {
  margin-bottom: 20px;
}
.featured-card {
  position: relative;
  height: 170px;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  cursor: pointer;
  overflow: hidden;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.featured-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.18);
}
.featured-name {
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.featured-meta {
  font-size: 12px;
  opacity: 0.85;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.featured-price {
  font-size: 18px;
  font-weight: 700;
  margin-top: 6px;
}
.like-badge {
  position: absolute;
  left: 10px;
  top: 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 14px;
  background: rgba(0, 0, 0, 0.28);
  font-size: 12px;
  user-select: none;
  transition: background 0.2s ease, transform 0.15s ease;
}
.like-badge:active {
  transform: scale(0.9);
}
.like-badge.liked {
  background: rgba(255, 255, 255, 0.3);
}
.heart {
  font-size: 12px;
}
</style>
