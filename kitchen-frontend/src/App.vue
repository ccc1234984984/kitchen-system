<template>
  <div class="app-container">
    <template v-if="$route.path !== '/login'">
      <div class="nav-bar">
        <router-link to="/kitchen" class="nav-link" active-class="nav-active">🍳 后厨看板</router-link>
        <router-link to="/tables" class="nav-link" active-class="nav-active">🪑 餐桌管理</router-link>
        <router-link to="/dishes" class="nav-link" active-class="nav-active">🍽️ 菜品管理</router-link>
        <router-link to="/order" class="nav-link" active-class="nav-active">📝 点餐前台</router-link>
        <div class="nav-right">
          <span class="nav-user">{{ adminName }}</span>
          <el-button size="small" text @click="showPwdDialog = true">修改密码</el-button>
          <el-button size="small" text @click="handleLogout">退出</el-button>
        </div>
      </div>
    </template>
    <router-view />

    <el-dialog v-model="showPwdDialog" title="修改密码" width="400px">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password></el-input>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password></el-input>
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPwdDialog = false">取消</el-button>
        <el-button type="primary" @click="handleChangePwd" :loading="pwdLoading">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { changePassword } from './api'

const router = useRouter()
const adminName = ref('')
const showPwdDialog = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref(null)

const user = localStorage.getItem('adminUser')
if (user) {
  try {
    adminName.value = JSON.parse(user).username || '管理员'
  } catch (e) {}
}

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const handleChangePwd = async () => {
  if (!pwdFormRef.value) return
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  pwdLoading.value = true
  try {
    await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
      confirmPassword: pwdForm.confirmPassword
    })
    ElMessage.success('密码修改成功')
    showPwdDialog.value = false
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (e) {
    console.error(e)
  }
  pwdLoading.value = false
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定退出登录吗？', '退出')
    localStorage.removeItem('adminUser')
    ElMessage.success('已退出')
    router.push('/login')
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Microsoft YaHei', sans-serif;
  background: #f5f0ea;
}

.app-container {
  padding: 20px;
  min-height: 100vh;
}

.nav-bar {
  display: flex;
  align-items: center;
  gap: 0;
  margin-bottom: 20px;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.nav-link {
  display: inline-block;
  padding: 14px 32px;
  font-size: 16px;
  font-weight: bold;
  color: #5a5a5a;
  text-decoration: none;
  transition: all 0.2s;
  border-bottom: 3px solid transparent;
}

.nav-link:hover {
  color: #c87a4f;
  background: #faf6f0;
}

.nav-link:active {
  color: #a05a2f;
  background: #f0e8df;
}

.nav-active {
  color: #c87a4f;
  border-bottom-color: #c87a4f;
  background: #fdf9f4;
}

.nav-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
}

.nav-user {
  font-size: 13px;
  color: #8c8c8c;
}
</style>
