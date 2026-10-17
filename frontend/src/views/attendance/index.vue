<template>
  <CrudPage ref="crud" :config="config" :options="{ childList, clazzList }" />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import CrudPage from '@/components/CrudPage.vue'
import { attendanceApi, childApi, clazzApi } from '@/api'

const crud = ref()
const childList = ref([])
const clazzList = ref([])

const statusOptions = [
  { label: '出勤', value: '出勤' },
  { label: '迟到', value: '迟到' },
  { label: '请假', value: '请假' },
  { label: '缺勤', value: '缺勤' }
]

const colorMap = { 出勤: 'success', 迟到: 'warning', 请假: 'info', 缺勤: 'danger' }

const config = {
  title: '考勤记录',
  nameField: 'childName',
  api: attendanceApi,
  search: [
    { field: 'keyword', label: '幼儿姓名', placeholder: '请输入姓名' },
    { field: 'clazzId', label: '班级', type: 'select', options: 'clazzList' },
    { field: 'status', label: '状态', type: 'select', options: statusOptions },
    { field: 'attDate', label: '考勤日期', type: 'date' }
  ],
  columns: [
    { prop: 'attDate', label: '考勤日期', width: 120 },
    { prop: 'childName', label: '幼儿', width: 100 },
    { prop: 'clazzName', label: '班级', width: 100 },
    {
      prop: 'status',
      label: '出勤状态',
      width: 100,
      mapTag: (v) => colorMap[v] || 'info'
    },
    { prop: 'checkInTime', label: '入园时间', width: 100 },
    { prop: 'remark', label: '备注', minWidth: 160 }
  ],
  fields: [
    { field: 'childId', label: '幼儿', type: 'select', options: 'childList', required: true },
    { field: 'attDate', label: '考勤日期', type: 'date', required: true },
    { field: 'status', label: '出勤状态', type: 'select', options: statusOptions, required: true },
    { field: 'checkInTime', label: '入园时间', placeholder: '如 08:05' },
    { field: 'remark', label: '备注', type: 'textarea', span: 24 }
  ],
  defaults: { status: '出勤' }
}

onMounted(async () => {
  const [c, k] = await Promise.all([childApi.all(), clazzApi.all()])
  childList.value = (c.data || []).map((i) => ({
    label: `${i.name}（${i.clazzName || '未分班'}）`,
    value: i.id
  }))
  clazzList.value = (k.data || []).map((i) => ({ label: i.name, value: i.id }))
})
</script>
