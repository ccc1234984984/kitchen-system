<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="login-icon">🍳</div>
        <h2>后厨管理系统</h2>
      </div>

      <el-form :model="form" @keyup.enter="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large"></el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="login-btn" @click="handleLogin" :loading="loading">
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span>默认账号：admin / 123456</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminLogin } from '../api'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '' })

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await adminLogin(form)
    localStorage.setItem('adminUser', JSON.stringify(res))
    ElMessage.success('登录成功')
    router.push('/kitchen')
  } catch (e) {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f0ea 0%, #ede4d8 100%);
}

.login-card {
  width: 400px;
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.login-header h2 {
  font-size: 22px;
  color: #2c2c2c;
  margin-bottom: 4px;
}

.login-header p {
  font-size: 14px;
  color: #8c8c8c;
}

.login-btn {
  width: 100%;
  font-size: 16px;
  padding: 12px;
  background: #c87a4f;
  border-color: #c87a4f;
}

.login-btn:hover {
  background: #b0673a;
  border-color: #b0673a;
}

.login-footer {
  text-align: center;
  font-size: 12px;
  color: #b8b0a5;
  margin-top: 20px;
}
</style>
