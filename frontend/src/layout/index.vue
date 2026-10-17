<template>
  <el-container class="layout">
    <el-aside width="216px" class="aside">
      <div class="logo">
        <span class="dot">🌱</span>
        <span class="txt">阳光幼儿园</span>
      </div>
      <el-menu :default-active="activePath" router class="menu" background-color="#1f2d3d"
               text-color="#bfcbd9" active-text-color="#ffd04b">
        <el-menu-item-group v-for="g in menus" :key="g.title" :title="g.title">
          <el-menu-item v-for="m in g.items" :key="m.path" :index="m.path">
            <el-icon><component :is="m.icon" /></el-icon>
            <span>{{ m.title }}</span>
          </el-menu-item>
        </el-menu-item-group>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="crumb">
          <span class="sys">幼儿园 / 托育机构管理系统</span>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <el-dropdown @command="handleCommand">
          <span class="user">
            <el-avatar :size="30" class="avatar">{{ shortName }}</el-avatar>
            <span class="name">{{ user.realName || user.username }}</span>
            <el-tag size="small" type="success">{{ roleText }}</el-tag>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { authApi } from '@/api'

const route = useRoute()
const router = useRouter()

const user = JSON.parse(localStorage.getItem('kids_user') || '{}')

const menus = [
  {
    title: '基础档案',
    items: [
      { path: '/dashboard', title: '首页看板', icon: 'DataLine' },
      { path: '/child', title: '幼儿档案', icon: 'User' },
      { path: '/clazz', title: '班级管理', icon: 'School' },
      { path: '/attendance', title: '考勤管理', icon: 'Calendar' },
      { path: '/teacher', title: '教职工管理', icon: 'Avatar' }
    ]
  },
  {
    title: '保教业务',
    items: [{ path: '/course', title: '课程活动', icon: 'Notebook' }]
  },
  {
    title: '膳食与收费',
    items: [
      { path: '/menu', title: '每周食谱', icon: 'Bowl' },
      { path: '/food', title: '食材库存', icon: 'Box' },
      { path: '/fee', title: '收费管理', icon: 'Money' }
    ]
  },
  {
    title: '家园沟通',
    items: [
      { path: '/leave', title: '请假审批', icon: 'DocumentChecked' },
      { path: '/notice', title: '通知公告', icon: 'Bell' }
    ]
  },
  {
    title: '统计与系统',
    items: [
      { path: '/report', title: '统计报表', icon: 'PieChart' },
      { path: '/dict', title: '数据字典', icon: 'Collection' },
      { path: '/system', title: '系统设置与日志', icon: 'Setting' }
    ]
  }
]

const activePath = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')
const shortName = computed(() => (user.realName || user.username || '?').slice(-2))
const roleText = computed(
  () => ({ admin: '园长', teacher: '教师', parent: '家长' })[user.role] || '用户'
)

function handleCommand(cmd) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
      .then(async () => {
        try {
          await authApi.logout()
        } catch (e) {
          /* 忽略退出接口异常 */
        }
        localStorage.removeItem('kids_token')
        localStorage.removeItem('kids_user')
        ElMessage.success('已退出登录')
        router.push('/login')
      })
      .catch(() => {})
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.aside {
  background: #1f2d3d;
  overflow-x: hidden;
}

.logo {
  height: 58px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 18px;
  color: #fff;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 1px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo .dot {
  font-size: 20px;
}

.menu {
  border-right: none;
}

.menu :deep(.el-menu-item-group__title) {
  color: #6b7b8d;
  font-size: 12px;
  padding-left: 18px !important;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 10;
}

.crumb {
  display: flex;
  align-items: center;
  gap: 16px;
}

.sys {
  font-weight: 600;
  color: #303133;
}

.user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.avatar {
  background: #409eff;
  font-size: 12px;
}

.name {
  color: #303133;
}

.main {
  background: #f5f7fa;
  padding: 16px;
  overflow-y: auto;
}
</style>
