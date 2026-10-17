<template>
  <CrudPage ref="crud" :config="config" :options="{ childList }" />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CrudPage from '@/components/CrudPage.vue'
import { approveLeave, childApi, leaveApi } from '@/api'

const crud = ref()
const childList = ref([])

const user = JSON.parse(localStorage.getItem('kids_user') || '{}')

const statusMap = { 0: '待审批', 1: '已通过', 2: '已驳回' }
const tagMap = { 0: 'warning', 1: 'success', 2: 'danger' }

const config = {
  title: '请假申请',
  nameField: 'childName',
  api: leaveApi,
  search: [
    { field: 'keyword', label: '幼儿姓名', placeholder: '请输入姓名' },
    {
      field: 'status',
      label: '审批状态',
      type: 'select',
      options: [
        { label: '待审批', value: 0 },
        { label: '已通过', value: 1 },
        { label: '已驳回', value: 2 }
      ]
    }
  ],
  columns: [
    { prop: 'childName', label: '幼儿', width: 100 },
    { prop: 'clazzName', label: '班级', width: 100 },
    { prop: 'startDate', label: '开始日期', width: 115 },
    { prop: 'endDate', label: '结束日期', width: 115 },
    { prop: 'days', label: '天数', width: 70 },
    { prop: 'reason', label: '请假事由', minWidth: 180 },
    {
      prop: 'status',
      label: '状态',
      width: 100,
      map: statusMap,
      mapTag: (v) => tagMap[v] || 'info'
    },
    { prop: 'approver', label: '审批人', width: 100 },
    { prop: 'approveTime', label: '审批时间', width: 170 }
  ],
  fields: [
    { field: 'childId', label: '幼儿', type: 'select', options: 'childList', required: true },
    { field: 'startDate', label: '开始日期', type: 'date', required: true },
    { field: 'endDate', label: '结束日期', type: 'date', required: true },
    { field: 'reason', label: '请假事由', type: 'textarea', span: 24, required: true }
  ],
  rowActions: [
    {
      label: '通过',
      type: 'success',
      show: (row) => row.status === 0,
      handler: (row, ctx) => handleApprove(row, ctx, true)
    },
    {
      label: '驳回',
      type: 'danger',
      show: (row) => row.status === 0,
      handler: (row, ctx) => handleApprove(row, ctx, false)
    }
  ]
}

function handleApprove(row, ctx, pass) {
  ElMessageBox.confirm(
    `确定${pass ? '通过' : '驳回'}「${row.childName}」的请假申请吗？`,
    '审批确认',
    { type: 'warning' }
  )
    .then(async () => {
      await approveLeave(row.id, pass, user.realName || user.username || '管理员')
      ElMessage.success(pass ? '已通过该请假申请' : '已驳回该请假申请')
      ctx.reload()
    })
    .catch(() => {})
}

onMounted(async () => {
  const res = await childApi.all()
  childList.value = (res.data || []).map((i) => ({
    label: `${i.name}（${i.clazzName || '未分班'}）`,
    value: i.id
  }))
})
</script>
