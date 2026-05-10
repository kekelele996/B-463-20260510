<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <el-icon class="logo-icon"><Monitor /></el-icon>
        <div class="title">企业固定资产管理系统</div>
      </div>
      <el-card class="login-card" shadow="hover">
        <h3 class="login-title">欢迎登录</h3>
        <el-form :model="form" :rules="rules" ref="loginFormRef" size="large">
          <el-form-item prop="username">
            <el-input 
              v-model="form.username" 
              placeholder="请输入用户名" 
              prefix-icon="User"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input 
              v-model="form.password" 
              type="password" 
              placeholder="请输入密码" 
              prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
              登 录
            </el-button>
          </el-form-item>
          <div class="login-footer">
            <el-button link type="primary" @click="$router.push('/register')">注册账号</el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loginFormRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await api.post('/users/login', form)
        if (res.data.code === 200) {
          ElMessage.success('登录成功')
          localStorage.setItem('token', res.data.data.token)
          localStorage.setItem('user', JSON.stringify(res.data.data))
          router.push('/dashboard')
        } else {
          ElMessage.error(res.data.message || '登录失败')
        }
      } catch (err) {
        ElMessage.error(err.response?.data?.message || '登录失败')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #1f4037 0%, #99f2c8 100%);
  background-image: url('https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=1920&q=80');
  background-size: cover;
  background-position: center;
  position: relative;
}

.login-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4); 
}

.login-box {
  position: relative;
  z-index: 1;
  width: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.login-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  color: #fff;
  text-shadow: 0 2px 4px rgba(0,0,0,0.5);
}

.logo-icon {
  font-size: 40px;
  margin-right: 10px;
}

.title {
  font-size: 24px;
  font-weight: bold;
}

.login-card {
  width: 100%;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
}

.login-title {
  text-align: center;
  margin: 0 0 20px;
  color: #303133;
}

.login-btn {
  width: 100%;
  font-weight: bold;
  letter-spacing: 1px;
}

.login-footer {
  display: flex;
  justify-content: space-between;
  margin-top: -10px;
}

.copyright {
  margin-top: 20px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
}
</style>
