<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <el-icon class="logo-icon"><Monitor /></el-icon>
        <div class="title">企业固定资产管理系统</div>
      </div>
      <el-card class="register-card" shadow="hover">
        <h3 class="register-title">注册新账号</h3>
        <el-form :model="form" :rules="rules" ref="registerFormRef" size="large">
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
            />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input 
              v-model="form.confirmPassword" 
              type="password" 
              placeholder="请再次输入密码" 
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="register-btn" :loading="loading" @click="handleRegister">
              立即注册
            </el-button>
          </el-form-item>
          <div class="register-footer">
            <span class="text-muted">已有账号？</span>
            <el-button link type="primary" @click="$router.push('/login')">去登录</el-button>
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
const registerFormRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  role: 'user'
})

const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [{ validator: validatePass2, trigger: 'blur' }]
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const { confirmPassword, ...registerData } = form
        const res = await api.post('/users/register', registerData)
        if (res.data.code === 200) {
          ElMessage.success('注册成功，请登录')
          router.push('/login')
        } else {
          ElMessage.error(res.data.message || '注册失败')
        }
      } catch (err) {
        ElMessage.error(err.response?.data?.message || '注册失败')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.register-container {
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

.register-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4); 
}

.register-box {
  position: relative;
  z-index: 1;
  width: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.register-header {
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

.register-card {
  width: 100%;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
}

.register-title {
  text-align: center;
  margin: 0 0 20px;
  color: #303133;
}

.register-btn {
  width: 100%;
  font-weight: bold;
  letter-spacing: 1px;
}

.register-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: -10px;
  font-size: 14px;
}

.text-muted {
  color: #909399;
  margin-right: 5px;
}

.copyright {
  margin-top: 20px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
}
</style>
