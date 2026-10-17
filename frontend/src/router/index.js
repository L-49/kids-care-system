import { createRouter, createWebHashHistory } from 'vue-router'

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
        meta: { title: '首页看板', icon: 'DataLine' }
      },
      {
        path: 'child',
        name: 'child',
        component: () => import('@/views/child/index.vue'),
        meta: { title: '幼儿档案', icon: 'User' }
      },
      {
        path: 'clazz',
        name: 'clazz',
        component: () => import('@/views/clazz/index.vue'),
        meta: { title: '班级管理', icon: 'School' }
      },
      {
        path: 'attendance',
        name: 'attendance',
        component: () => import('@/views/attendance/index.vue'),
        meta: { title: '考勤管理', icon: 'Calendar' }
      },
      {
        path: 'teacher',
        name: 'teacher',
        component: () => import('@/views/teacher/index.vue'),
        meta: { title: '教职工管理', icon: 'Avatar' }
      },
      {
        path: 'course',
        name: 'course',
        component: () => import('@/views/course/index.vue'),
        meta: { title: '课程活动', icon: 'Notebook' }
      },
      {
        path: 'fee',
        name: 'fee',
        component: () => import('@/views/fee/index.vue'),
        meta: { title: '收费管理', icon: 'Money' }
      },
      {
        path: 'leave',
        name: 'leave',
        component: () => import('@/views/leave/index.vue'),
        meta: { title: '请假审批', icon: 'DocumentChecked' }
      },
      {
        path: 'notice',
        name: 'notice',
        component: () => import('@/views/notice/index.vue'),
        meta: { title: '通知公告', icon: 'Bell' }
      },
      {
        path: 'menu',
        name: 'menu',
        component: () => import('@/views/menu/index.vue'),
        meta: { title: '每周食谱', icon: 'Bowl' }
      },
      {
        path: 'food',
        name: 'food',
        component: () => import('@/views/food/index.vue'),
        meta: { title: '食材库存', icon: 'Box' }
      },
      {
        path: 'report',
        name: 'report',
        component: () => import('@/views/report/index.vue'),
        meta: { title: '统计报表', icon: 'PieChart' }
      },
      {
        path: 'dict',
        name: 'dict',
        component: () => import('@/views/dict/index.vue'),
        meta: { title: '数据字典', icon: 'Collection' }
      },
      {
        path: 'system',
        name: 'system',
        component: () => import('@/views/system/index.vue'),
        meta: { title: '系统设置与日志', icon: 'Setting' }
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
  next()
})

export default router
