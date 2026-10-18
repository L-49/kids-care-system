<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="brand">
        <div class="icon">🌱</div>
        <h2>阳光幼儿园管理系统</h2>
        <p>幼儿档案 · 考勤 · 课程 · 收费 · 家园沟通</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="role">
          <div class="role-pick">
            <div
              v-for="r in roleOptions"
              :key="r.value"
              class="role-item"
              :class="{ active: form.role === r.value }"
              @click="pickRole(r)"
            >
              <el-icon :size="20"><component :is="r.icon" /></el-icon>
              <span>{{ r.label }}</span>
            </div>
          </div>
          <div class="role-hint">点击上方身份，账号密码自动填入</div>
        </el-form-item>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-button type="primary" class="btn" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Avatar, Lock, OfficeBuilding, User, UserFilled } from '@element-plus/icons-vue'
import { authApi } from '@/api'
import { homeOf } from '@/router'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

// 每个身份绑定一套演示账号，点击身份卡片即自动填充，无需手工输入
const roleOptions = [
  { value: 'admin', label: '园长', icon: OfficeBuilding, username: 'admin', password: '123456' },
  { value: 'teacher', label: '教师', icon: Avatar, username: 'teacher', password: '123456' },
  { value: 'parent', label: '家长', icon: UserFilled, username: 'parent', password: '123456' }
]

const form = reactive({
  role: roleOptions[0].value,
  username: roleOptions[0].username,
  password: roleOptions[0].password
})

function pickRole(r) {
  form.role = r.value
  form.username = r.username
  form.password = r.password
  formRef.value?.clearValidate(['username', 'password'])
}

const rules = {
  role: [{ required: true, message: '请选择登录身份', trigger: 'change' }],
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function handleLogin() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await authApi.login({
        username: form.username,
        password: form.password,
        role: form.role
      })
      const user = res.data.user || {}
      localStorage.setItem('kids_token', res.data.token)
      localStorage.setItem('kids_user', JSON.stringify(user))
      ElMessage.success(`登录成功，欢迎 ${user.realName || user.username}`)
      router.push(homeOf(user.role))
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-wrap {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #a8d8ff 0%, #c3f0e0 100%);
}

.login-card {
  width: 440px;
  padding: 34px 36px 30px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 12px 34px rgba(31, 45, 61, 0.16);
}

.brand {
  text-align: center;
  margin-bottom: 22px;
}

.brand .icon {
  font-size: 38px;
}

.brand h2 {
  margin: 8px 0 6px;
  font-size: 21px;
  color: #1f2d3d;
}

.brand p {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.role-pick {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  width: 100%;
}

.role-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 0;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  color: #606266;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.role-item:hover {
  border-color: #a0cfff;
  color: #409eff;
}

.role-item.active {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
  font-weight: 600;
}

.role-hint {
  width: 100%;
  margin-top: 6px;
  text-align: center;
  font-size: 12px;
  color: #a8abb2;
  line-height: 1.4;
}

.btn {
  width: 100%;
  margin-top: 4px;
}
</style>
