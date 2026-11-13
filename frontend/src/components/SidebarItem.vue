<template>
  <el-sub-menu v-if="item.children && item.children.length" :index="fullPath">
    <template #title>
      <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
      <span>{{ item.menuName }}</span>
    </template>
    <SidebarItem v-for="child in item.children" :key="child.id" :item="child" :base-path="fullPath" />
  </el-sub-menu>

  <el-menu-item v-else :index="fullPath">
    <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
    <template #title>{{ item.menuName }}</template>
  </el-menu-item>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  item: { type: Object, required: true },
  basePath: { type: String, default: '' }
})

// 计算完整路由路径
const fullPath = computed(() => {
  if (!props.item.path) return props.basePath
  if (props.item.path.startsWith('/')) return props.item.path
  return props.basePath + '/' + props.item.path
})
</script>
