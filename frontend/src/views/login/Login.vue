<template>
  <div class="login-container">
    <div class="login-bg" :style="{ backgroundImage: `url(${bannerImg})` }" />
    <div class="login-card">
      <h2 class="title">企业资产管理系统</h2>
      <p class="subtitle">Enterprise Asset Management System</p>
      <el-form ref="formRef" :model="form" :rules="rules" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password @keyup.enter="submit">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="submit">登 录</el-button>
      </el-form>
      <p class="tip">演示账号：admin / admin123</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api'
import { useUserStore } from '@/store/user'
import bannerImg from '@/assets/login-banner.png'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login(form)
    userStore.setLoginData(res.data)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  position: relative;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  /* 兜底：图片加载不出来时仍保持青色 */
  background: linear-gradient(135deg, #0e7490 0%, #06b6d4 100%);
}
/* 底层：login-banner.png 全屏铺底 */
.login-bg {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  z-index: 0;
}
/* 青色半透明遮罩，让主色调与图片融合 */
.login-container::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(14, 116, 144, 0.78) 0%, rgba(6, 182, 212, 0.72) 100%);
  z-index: 1;
}
.login-card {
  position: relative;
  z-index: 2;
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.25);
}
.title {
  text-align: center;
  color: #0e7490;
  margin-bottom: 4px;
}
.subtitle {
  text-align: center;
  color: #999;
  font-size: 12px;
  margin-bottom: 28px;
}
.login-btn {
  width: 100%;
}
.tip {
  margin-top: 16px;
  text-align: center;
  color: #0e7490;
  font-size: 12px;
}
</style>
