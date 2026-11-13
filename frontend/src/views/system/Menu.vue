<template>
  <div class="page-banner">
    <img :src="bannerImg" alt="banner" />
  </div>
  <el-card shadow="never">
    <el-alert title="菜单与权限由数据库初始化脚本维护，此处展示当前菜单/权限树结构。" type="info" :closable="false" style="margin-bottom: 16px" />
    <el-table :data="tree" row-key="id" border default-expand-all :tree-props="{ children: 'children' }">
      <el-table-column prop="menuName" label="菜单名称" min-width="200" />
      <el-table-column label="类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.menuType === 'M'" type="warning">目录</el-tag>
          <el-tag v-else-if="row.menuType === 'C'" type="success">菜单</el-tag>
          <el-tag v-else type="info">按钮</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路由地址" min-width="140" />
      <el-table-column prop="component" label="组件路径" min-width="140" />
      <el-table-column prop="perms" label="权限标识" min-width="180" />
      <el-table-column prop="icon" label="图标" width="120" />
      <el-table-column prop="sort" label="排序" width="70" align="center" />
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { menuTree } from '@/api'
import bannerImg from '@/assets/menu-banner.png'

const tree = ref([])

onMounted(async () => {
  const res = await menuTree()
  tree.value = res.data
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
</style>
