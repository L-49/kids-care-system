<template>
  <CrudPage ref="crud" :config="config" :options="{ clazzList, teacherList }" />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import CrudPage from '@/components/CrudPage.vue'
import { clazzApi, courseApi, teacherApi } from '@/api'

const crud = ref()
const clazzList = ref([])
const teacherList = ref([])

const config = {
  title: '课程活动',
  nameField: 'title',
  api: courseApi,
  search: [
    { field: 'keyword', label: '课程名称', placeholder: '请输入课程名称' },
    { field: 'clazzId', label: '班级', type: 'select', options: 'clazzList' },
    { field: 'courseDate', label: '上课日期', type: 'date' }
  ],
  columns: [
    { prop: 'courseDate', label: '日期', width: 115 },
    { prop: 'startTime', label: '开始', width: 80 },
    { prop: 'endTime', label: '结束', width: 80 },
    { prop: 'title', label: '课程名称', minWidth: 130 },
    { prop: 'clazzName', label: '班级', width: 100 },
    { prop: 'teacherName', label: '授课教师', width: 100 },
    { prop: 'content', label: '活动内容', minWidth: 200 },
    {
      prop: 'status',
      label: '状态',
      width: 80,
      map: { 1: '正常', 0: '取消' },
      mapTag: (v) => (v === 1 ? 'success' : 'info')
    }
  ],
  fields: [
    { field: 'title', label: '课程名称', required: true },
    { field: 'clazzId', label: '班级', type: 'select', options: 'clazzList', required: true },
    { field: 'teacherId', label: '授课教师', type: 'select', options: 'teacherList' },
    { field: 'courseDate', label: '上课日期', type: 'date', required: true },
    { field: 'startTime', label: '开始时间', placeholder: '如 09:00' },
    { field: 'endTime', label: '结束时间', placeholder: '如 09:40' },
    {
      field: 'status',
      label: '状态',
      type: 'select',
      options: [
        { label: '正常', value: 1 },
        { label: '取消', value: 0 }
      ]
    },
    { field: 'content', label: '活动内容', type: 'textarea', span: 24 }
  ],
  defaults: { status: 1 }
}

onMounted(async () => {
  const [k, t] = await Promise.all([clazzApi.all(), teacherApi.all()])
  clazzList.value = (k.data || []).map((i) => ({ label: i.name, value: i.id }))
  teacherList.value = (t.data || []).map((i) => ({ label: i.name, value: i.id }))
})
</script>
