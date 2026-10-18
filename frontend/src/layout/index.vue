<template>
  <el-container class="layout">
    <el-aside width="216px" class="aside">
      <div class="logo">
        <span class="dot">🌱</span>
        <span class="txt">阳光幼儿园</span>
      </div>
      <el-menu
        ref="menuRef"
        :default-active="activePath"
        :default-openeds="openedKeys"
        router
        unique-opened
        class="menu"
        background-color="#1f2d3d"
        text-color="#bfcbd9"
        active-text-color="#ffd04b"
      >
        <template v-for="m in menus" :key="m.key">
          <!-- 只有一个可用子项时直接渲染为一级菜单，避免出现"一级里套一个二级" -->
          <el-menu-item v-if="!m.children" :index="m.path">
            <el-icon><component :is="m.icon" /></el-icon>
            <span>{{ m.title }}</span>
          </el-menu-item>

          <el-sub-menu v-else :index="m.key">
            <template #title>
              <el-icon><component :is="m.icon" /></el-icon>
              <span>{{ m.title }}</span>
            </template>
            <el-menu-item v-for="c in m.children" :key="c.path" :index="c.path">
              <el-icon><component :is="c.icon" /></el-icon>
              <span>{{ c.title }}</span>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="crumb">
          <span class="sys">幼儿园 / 托育机构管理系统</span>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: homePath }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentGroup">{{ currentGroup }}</el-breadcrumb-item>
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
              <el-dropdown-item disabled>{{ user.username }}</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
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
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { authApi } from '@/api'
import { homeOf } from '@/router'

const route = useRoute()
const router = useRouter()
const menuRef = ref()

const user = JSON.parse(localStorage.getItem('kids_user') || '{}')
const ADMIN = 'admin'
const TEACHER = 'teacher'
const PARENT = 'parent'

// 一级菜单 + 二级菜单；每个叶子声明可见角色，渲染时按当前角色过滤（与后端 @RequireRole 一致）
const allMenus = [
  {
    key: 'home',
    title: '首页',
    icon: 'HomeFilled',
    children: [
      { path: '/dashboard', title: '首页看板', icon: 'DataLine', roles: [ADMIN, TEACHER] },
      { path: '/my-child', title: '我的孩子', icon: 'User', roles: [PARENT] }
    ]
  },
  {
    key: 'base',
    title: '基础档案',
    icon: 'FolderOpened',
    children: [
      { path: '/child', title: '幼儿档案', icon: 'User', roles: [ADMIN, TEACHER] },
      { path: '/clazz', title: '班级管理', icon: 'School', roles: [ADMIN, TEACHER] },
      { path: '/teacher', title: '教职工管理', icon: 'Avatar', roles: [ADMIN, TEACHER] }
    ]
  },
  {
    key: 'nursing',
    title: '保教业务',
    icon: 'Reading',
    children: [
      { path: '/attendance', title: '考勤管理', icon: 'Calendar', roles: [ADMIN, TEACHER] },
      { path: '/course', title: '课程活动', icon: 'Notebook', roles: [ADMIN, TEACHER] },
      { path: '/menu', title: '每周食谱', icon: 'Bowl', roles: [ADMIN, TEACHER, PARENT] },
      { path: '/food', title: '食材库存', icon: 'Box', roles: [ADMIN, TEACHER] }
    ]
  },
  {
    key: 'contact',
    title: '家园沟通',
    icon: 'ChatDotRound',
    children: [
      { path: '/leave', title: '请假审批', icon: 'DocumentChecked', roles: [ADMIN, TEACHER] },
      { path: '/notice', title: '通知公告', icon: 'Bell', roles: [ADMIN, TEACHER, PARENT] }
    ]
  },
  {
    key: 'finance',
    title: '财务与统计',
    icon: 'Coin',
    children: [
      { path: '/fee', title: '收费管理', icon: 'Money', roles: [ADMIN] },
      { path: '/report', title: '统计报表', icon: 'PieChart', roles: [ADMIN] }
    ]
  },
  {
    key: 'system',
    title: '系统管理',
    icon: 'Setting',
    children: [
      { path: '/dict', title: '数据字典', icon: 'Collection', roles: [ADMIN] },
      { path: '/system', title: '系统设置与日志', icon: 'Setting', roles: [ADMIN] }
    ]
  }
]

// 按角色过滤；过滤后只剩一个子项的组，直接摊平为一级菜单
const menus = computed(() =>
  allMenus
    .map((g) => {
      const children = g.children.filter((c) => c.roles.includes(user.role))
      if (children.length === 0) return null
      if (children.length === 1) {
        const only = children[0]
        return { key: only.path, path: only.path, title: only.title, icon: only.icon }
      }
      return { key: g.key, title: g.title, icon: g.icon, children }
    })
    .filter(Boolean)
)

const activePath = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')

// 当前页面所属的一级菜单（面包屑用）
const currentGroup = computed(() => {
  const hit = menus.value.find((m) => m.children && m.children.some((c) => c.path === route.path))
  return hit ? hit.title : ''
})

// 只展开当前页面所属的一级菜单，切换路由时保持同步
const openedKeys = computed(() => {
  const hit = menus.value.find((m) => m.children && m.children.some((c) => c.path === route.path))
  return hit ? [hit.key] : []
})

watch(
  () => route.path,
  () => {
    const key = openedKeys.value[0]
    if (key && typeof menuRef.value?.open === 'function') menuRef.value.open(key)
  }
)

const homePath = computed(() => homeOf(user.role))
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
  display: flex;
  flex-direction: column;
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
  flex-shrink: 0;
}

.logo .dot {
  font-size: 20px;
}

.menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 一级菜单标题 */
.menu :deep(.el-sub-menu__title) {
  height: 50px;
  line-height: 50px;
  font-size: 14px;
}

.menu :deep(.el-sub-menu__title:hover) {
  background-color: #182430;
}

/* 二级菜单项：缩进 + 略深背景，层级一眼可辨 */
.menu :deep(.el-sub-menu .el-menu-item) {
  height: 44px;
  line-height: 44px;
  min-width: 0;
  padding-left: 48px !important;
  font-size: 13px;
  background-color: #182430;
}

.menu :deep(.el-sub-menu .el-menu-item:hover) {
  background-color: #141f2a;
}

.menu :deep(.el-sub-menu .el-menu-item.is-active) {
  background-color: #10171f;
  border-right: 3px solid #ffd04b;
}

/* 无子项的一级菜单 */
.menu > :deep(.el-menu-item) {
  height: 50px;
  line-height: 50px;
  font-size: 14px;
}

.menu :deep(.el-menu-item .el-icon),
.menu :deep(.el-sub-menu__title .el-icon) {
  margin-right: 8px;
  width: 18px;
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
