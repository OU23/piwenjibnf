import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/Dashboard.vue'), meta: { title: '首页' } },
      { path: 'system/user', name: 'User', component: () => import('@/views/system/User.vue'), meta: { title: '用户管理' } },
      { path: 'system/role', name: 'Role', component: () => import('@/views/system/Role.vue'), meta: { title: '角色管理' } },
      { path: 'system/menu', name: 'Menu', component: () => import('@/views/system/Menu.vue'), meta: { title: '菜单管理' } },
      { path: 'system/dept', name: 'Dept', component: () => import('@/views/system/Dept.vue'), meta: { title: '部门管理' } },
      { path: 'system/log', name: 'Log', component: () => import('@/views/system/Log.vue'), meta: { title: '操作日志' } },
      { path: 'asset/category', name: 'Category', component: () => import('@/views/category/index.vue'), meta: { title: '资产分类' } },
      { path: 'asset/asset', name: 'Asset', component: () => import('@/views/asset/index.vue'), meta: { title: '资产台账' } },
      { path: 'business/application', name: 'Application', component: () => import('@/views/application/index.vue'), meta: { title: '资产申请' } },
      { path: 'business/allocation', name: 'Allocation', component: () => import('@/views/allocation/index.vue'), meta: { title: '资产分配' } },
      { path: 'business/return', name: 'AssetReturn', component: () => import('@/views/return/index.vue'), meta: { title: '资产归还' } },
      { path: 'statistics', name: 'Statistics', component: () => import('@/views/statistics/index.vue'), meta: { title: '统计分析' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  document.title = to.meta.title ? `${to.meta.title} - 企业资产管理系统` : '企业资产管理系统'
  if (to.path === '/login') {
    next()
  } else if (!token) {
    next('/login')
  } else {
    next()
  }
})

export default router
