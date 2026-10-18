import { createRouter, createWebHashHistory } from 'vue-router'

/** 各角色默认首页 */
export function homeOf(role) {
  return role === 'parent' ? '/my-child' : '/dashboard'
}

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard.vue'),
        meta: { title: '首页看板', icon: 'DataLine', roles: ['admin', 'teacher'] }
      },
      {
        path: 'my-child',
        name: 'myChild',
        component: () => import('@/views/my-child/index.vue'),
        meta: { title: '我的孩子', icon: 'User', roles: ['parent'] }
      },
      {
        path: 'child',
        name: 'child',
        component: () => import('@/views/child/index.vue'),
        meta: { title: '幼儿档案', icon: 'User', roles: ['admin', 'teacher'] }
      },
      {
        path: 'clazz',
        name: 'clazz',
        component: () => import('@/views/clazz/index.vue'),
        meta: { title: '班级管理', icon: 'School', roles: ['admin', 'teacher'] }
      },
      {
        path: 'attendance',
        name: 'attendance',
        component: () => import('@/views/attendance/index.vue'),
        meta: { title: '考勤管理', icon: 'Calendar', roles: ['admin', 'teacher'] }
      },
      {
        path: 'teacher',
        name: 'teacher',
        component: () => import('@/views/teacher/index.vue'),
        meta: { title: '教职工管理', icon: 'Avatar', roles: ['admin'] }
      },
      {
        path: 'course',
        name: 'course',
        component: () => import('@/views/course/index.vue'),
        meta: { title: '课程活动', icon: 'Notebook', roles: ['admin', 'teacher'] }
      },
      {
        path: 'fee',
        name: 'fee',
        component: () => import('@/views/fee/index.vue'),
        meta: { title: '收费管理', icon: 'Money', roles: ['admin', 'teacher'] }
      },
      {
        path: 'leave',
        name: 'leave',
        component: () => import('@/views/leave/index.vue'),
        meta: { title: '请假审批', icon: 'DocumentChecked', roles: ['admin', 'teacher'] }
      },
      {
        path: 'notice',
        name: 'notice',
        component: () => import('@/views/notice/index.vue'),
        meta: { title: '通知公告', icon: 'Bell', roles: ['admin', 'teacher', 'parent'] }
      },
      {
        path: 'menu',
        name: 'menu',
        component: () => import('@/views/menu/index.vue'),
        meta: { title: '每周食谱', icon: 'Bowl', roles: ['admin', 'teacher', 'parent'] }
      },
      {
        path: 'food',
        name: 'food',
        component: () => import('@/views/food/index.vue'),
        meta: { title: '食材库存', icon: 'Box', roles: ['admin', 'teacher'] }
      },
      {
        path: 'report',
        name: 'report',
        component: () => import('@/views/report/index.vue'),
        meta: { title: '统计报表', icon: 'PieChart', roles: ['admin'] }
      },
      {
        path: 'dict',
        name: 'dict',
        component: () => import('@/views/dict/index.vue'),
        meta: { title: '数据字典', icon: 'Collection', roles: ['admin'] }
      },
      {
        path: 'system',
        name: 'system',
        component: () => import('@/views/system/index.vue'),
        meta: { title: '系统设置与日志', icon: 'Setting', roles: ['admin'] }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('kids_token')
  if (to.meta.public) {
    next()
    return
  }
  if (!token) {
    next('/login')
    return
  }
  const user = JSON.parse(localStorage.getItem('kids_user') || '{}')
  // 角色守卫：越权访问一律回落到该角色首页
  if (to.meta.roles && !to.meta.roles.includes(user.role)) {
    next(homeOf(user.role))
    return
  }
  if (to.path === '/dashboard' && user.role === 'parent') {
    next('/my-child')
    return
  }
  next()
})

export default router
