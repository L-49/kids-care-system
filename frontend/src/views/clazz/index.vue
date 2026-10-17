<template>
  <CrudPage ref="crud" :config="config" :options="{ teacherList }" />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import CrudPage from '@/components/CrudPage.vue'
import { clazzApi, teacherApi } from '@/api'

const crud = ref()
const teacherList = ref([])

const config = {
  title: '班级',
  nameField: 'name',
  api: clazzApi,
  search: [{ field: 'keyword', label: '班级名称', placeholder: '请输入班级名称' }],
  columns: [
    { prop: 'name', label: '班级名称', width: 120 },
    { prop: 'ageRange', label: '适龄段', width: 100 },
    { prop: 'room', label: '教室', width: 100 },
    { prop: 'capacity', label: '额定人数', width: 100 },
    { prop: 'headTeacherName', label: '班主任', width: 110 },
    { prop: 'childCount', label: '在园人数', width: 100 }
  ],
  fields: [
    { field: 'name', label: '班级名称', required: true },
    { field: 'ageRange', label: '适龄段', placeholder: '如 3-4岁' },
    { field: 'room', label: '教室', placeholder: '如 A101' },
    { field: 'capacity', label: '额定人数', type: 'number', min: 1, max: 60 },
    { field: 'headTeacherId', label: '班主任', type: 'select', options: 'teacherList' }
  ],
  defaults: { capacity: 25 }
}

onMounted(async () => {
  const res = await teacherApi.all()
  teacherList.value = (res.data || []).map((t) => ({ label: `${t.name}（${t.role}）`, value: t.id }))
})
</script>
