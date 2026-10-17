<template>
  <CrudPage ref="crud" :config="config" :options="{ clazzList }" />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import CrudPage from '@/components/CrudPage.vue'
import { childApi, clazzApi } from '@/api'

const crud = ref()
const clazzList = ref([])

const config = {
  title: '幼儿档案',
  nameField: 'name',
  api: childApi,
  search: [
    { field: 'keyword', label: '关键字', placeholder: '姓名/家长/电话' },
    { field: 'clazzId', label: '班级', type: 'select', options: 'clazzList' },
    {
      field: 'status',
      label: '状态',
      type: 'select',
      options: [
        { label: '在园', value: 1 },
        { label: '离园', value: 0 }
      ]
    }
  ],
  columns: [
    { prop: 'name', label: '姓名', width: 100 },
    { prop: 'gender', label: '性别', width: 70 },
    { prop: 'birthday', label: '出生日期', width: 110 },
    { prop: 'clazzName', label: '所在班级', width: 100 },
    { prop: 'guardianName', label: '家长姓名', width: 100 },
    { prop: 'guardianPhone', label: '家长电话', width: 130 },
    { prop: 'allergy', label: '过敏史', minWidth: 120 },
    { prop: 'enrollDate', label: '入园日期', width: 110 },
    {
      prop: 'status',
      label: '状态',
      width: 80,
      map: { 1: '在园', 0: '离园' },
      mapTag: (v) => (v === 1 ? 'success' : 'info')
    }
  ],
  fields: [
    { field: 'name', label: '幼儿姓名', required: true },
    {
      field: 'gender',
      label: '性别',
      type: 'select',
      options: [
        { label: '男', value: '男' },
        { label: '女', value: '女' }
      ]
    },
    { field: 'birthday', label: '出生日期', type: 'date' },
    { field: 'clazzId', label: '所在班级', type: 'select', options: 'clazzList', required: true },
    { field: 'guardianName', label: '家长姓名' },
    { field: 'guardianPhone', label: '家长电话', placeholder: '11 位手机号，用于家长端登录' },
    { field: 'enrollDate', label: '入园日期', type: 'date' },
    {
      field: 'status',
      label: '在园状态',
      type: 'select',
      options: [
        { label: '在园', value: 1 },
        { label: '离园', value: 0 }
      ]
    },
    { field: 'address', label: '家庭住址', span: 24 },
    {
      field: 'allergy',
      label: '过敏史/禁忌',
      type: 'textarea',
      span: 24,
      placeholder: '如：牛奶、花生过敏。填写后食谱页面会自动标注该幼儿'
    }
  ],
  defaults: { gender: '男', status: 1 }
}

onMounted(async () => {
  const res = await clazzApi.all()
  clazzList.value = (res.data || []).map((c) => ({ label: c.name, value: c.id }))
})
</script>
