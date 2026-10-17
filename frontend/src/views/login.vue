<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="brand">
        <div class="icon">🌱</div>
        <h2>阳光幼儿园管理系统</h2>
        <p>幼儿档案 · 考勤 · 课程 · 收费 · 家园沟通</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
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

      <div class="tips">
        <span>演示账号：</span>
        <el-tag v-for="t in accounts" :key="t.username" class="tag" size="small" @click="fill(t)">
          {{ t.label }}：{{ t.username }} / 123456
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { authApi } from '@/api'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: 'admin', password: '123456' })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const accounts = [
  { label: '园长', username: 'admin' },
  { label: '教师', username: 'teacher' },
  { label: '家长', username: 'parent' }
]

function fill(t) {
  form.username = t.username
  form.password = '123456'
}

function handleLogin() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await authApi.login({ username: form.username, password: form.password })
      localStorage.setItem('kids_token', res.data.token)
      localStorage.setItem('kids_user', JSON.stringify(res.data.user))
      ElMessage.success('登录成功，欢迎回来')
      router.push('/dashboard')
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
  width: 420px;
  padding: 34px 36px 26px;
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

.btn {
  width: 100%;
  margin-top: 4px;
}

.tips {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px dashed #e4e7ed;
  font-size: 12px;
  color: #909399;
}

.tag {
  margin: 4px 6px 0 0;
  cursor: pointer;
}
</style>
