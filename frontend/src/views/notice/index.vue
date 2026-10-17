<template>
  <CrudPage
    ref="crud"
    :config="config"
    :options="{ typeOptions, statusOptions, searchStatusOptions, targetOptions, clazzList }"
  />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CrudPage from '@/components/CrudPage.vue'
import { noticeApi, dictApi, publishNotice, revokeNotice, clazzApi } from '@/api'

const crud = ref()
const user = JSON.parse(localStorage.getItem('kids_user') || '{}')

// 类型选项来自数据字典 notice_type，新增类型只需在字典里加一项，不用改代码
const typeOptions = ref([])
// 班级选项（定向可见用）
const clazzList = ref([])

// 可见范围：只做两档，教师维度需要先补 sys_user 与 teacher 的绑定
const targetOptions = [
  { label: '全园', value: 'all' },
  { label: '指定班级', value: 'class' }
]

// 字典项的展示颜色（未配置的类型回落到灰色）
const typeColor = { 通知: 'primary', 活动: 'success', 食谱: 'warning', 放假: 'info' }

// 公告状态：与后端 Notice.STATUS_* 常量一一对应
const STATUS_TEXT = { 0: '草稿', 1: '定时待发', 2: '已发布', 3: '已撤回' }
const STATUS_TAG = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
const STATUS_DRAFT = 0
const STATUS_SCHEDULED = 1
const STATUS_PUBLISHED = 2

const statusOptions = [
  { label: '草稿', value: STATUS_DRAFT },
  { label: '定时待发', value: STATUS_SCHEDULED },
  { label: '已发布', value: STATUS_PUBLISHED }
]

// 查询用的状态（含「已撤回」，管理端可按需查看）
const searchStatusOptions = [
  ...statusOptions,
  { label: '已撤回', value: 3 }
]

const config = {
  title: '通知公告',
  nameField: 'title',
  api: noticeApi,
  search: [
    { field: 'keyword', label: '标题', placeholder: '请输入标题关键字' },
    { field: 'type', label: '类型', type: 'select', options: 'typeOptions' },
    { field: 'status', label: '状态', type: 'select', options: 'searchStatusOptions' }
  ],
  columns: [
    { prop: 'title', label: '标题', minWidth: 200 },
    { prop: 'type', label: '类型', width: 90, mapTag: (v) => typeColor[v] || 'info' },
    { prop: 'status', label: '状态', width: 105, map: STATUS_TEXT, mapTag: (v) => STATUS_TAG[v] || 'info' },
    { prop: 'targetLabel', label: '可见范围', width: 100 },
    { prop: 'publisher', label: '发布人', width: 100 },
    { prop: 'publishTime', label: '发布时间', width: 175 },
    { prop: 'content', label: '内容', minWidth: 200 }
  ],
  fields: [
    { field: 'title', label: '标题', required: true, span: 24 },
    { field: 'type', label: '类型', type: 'select', options: 'typeOptions', required: true },
    { field: 'publisher', label: '发布人' },
    { field: 'status', label: '发布方式', type: 'select', options: 'statusOptions' },
    { field: 'targetType', label: '可见范围', type: 'select', options: 'targetOptions' },
    { field: 'targetId', label: '指定班级', type: 'select', options: 'clazzList' },
    { field: 'publishTime', label: '发布时间', type: 'datetime' },
    { field: 'content', label: '公告内容', type: 'textarea', span: 24 }
  ],
  rowActions: [
    {
      label: '立即发布',
      type: 'primary',
      show: (row) => row.status === STATUS_DRAFT || row.status === STATUS_SCHEDULED,
      handler: (row, ctx) => {
        ElMessageBox.confirm(`确定立即发布「${row.title}」吗？发布后家长端即可看到。`, '提示', {
          type: 'info',
          confirmButtonText: '立即发布',
          cancelButtonText: '取消'
        })
          .then(async () => {
            await publishNotice(row.id)
            ElMessage.success('已发布')
            ctx.reload()
          })
          .catch(() => {})
      }
    },
    {
      label: '撤回',
      type: 'danger',
      show: (row) => row.status === STATUS_PUBLISHED,
      handler: (row, ctx) => {
        ElMessageBox.confirm(
          `确定撤回「${row.title}」吗？撤回后不再对外展示，但记录会保留，可追溯。`,
          '提示',
          { type: 'warning', confirmButtonText: '确定撤回', cancelButtonText: '取消' }
        )
          .then(async () => {
            await revokeNotice(row.id)
            ElMessage.success('已撤回')
            ctx.reload()
          })
          .catch(() => {})
      }
    }
  ],
  defaults: {
    type: '通知',
    status: STATUS_PUBLISHED,
    targetType: 'all',
    publisher: user.realName || user.username || '管理员'
  }
}

onMounted(async () => {
  try {
    const res = await dictApi.type('notice_type')
    typeOptions.value = (res.data || []).map((d) => ({ label: d.dictLabel, value: d.dictValue }))
  } catch (e) {
    // 字典接口异常时兜底，保证页面可用
    typeOptions.value = [
      { label: '通知', value: '通知' },
      { label: '活动', value: '活动' },
      { label: '食谱', value: '食谱' },
      { label: '放假', value: '放假' }
    ]
  }
  try {
    const clazz = await clazzApi.all()
    clazzList.value = (clazz.data || []).map((c) => ({ label: c.name, value: c.id }))
  } catch (e) {
    clazzList.value = []
  }
})
</script>
