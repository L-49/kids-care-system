<template>
  <CrudPage ref="crud" :config="config" />
</template>

<script setup>
import { ref } from 'vue'
import CrudPage from '@/components/CrudPage.vue'
import { teacherApi } from '@/api'

const crud = ref()

const config = {
  title: '教职工',
  nameField: 'name',
  api: teacherApi,
  search: [
    { field: 'keyword', label: '关键字', placeholder: '姓名/电话' },
    {
      field: 'role',
      label: '岗位',
      type: 'select',
      options: [
        { label: '园长', value: '园长' },
        { label: '教师', value: '教师' },
        { label: '保育员', value: '保育员' },
        { label: '保健医', value: '保健医' }
      ]
    },
    {
      field: 'status',
      label: '状态',
      type: 'select',
      options: [
        { label: '在职', value: 1 },
        { label: '离职', value: 0 }
      ]
    }
  ],
  columns: [
    { prop: 'name', label: '姓名', width: 110 },
    { prop: 'gender', label: '性别', width: 70 },
    { prop: 'phone', label: '联系电话', width: 140 },
    { prop: 'role', label: '岗位', width: 100 },
    { prop: 'entryDate', label: '入职日期', width: 120 },
    {
      prop: 'status',
      label: '状态',
      width: 80,
      map: { 1: '在职', 0: '离职' },
      mapTag: (v) => (v === 1 ? 'success' : 'danger')
    }
  ],
  fields: [
    { field: 'name', label: '姓名', required: true },
    {
      field: 'gender',
      label: '性别',
      type: 'select',
      options: [
        { label: '男', value: '男' },
        { label: '女', value: '女' }
      ]
    },
    { field: 'phone', label: '联系电话', required: true },
    {
      field: 'role',
      label: '岗位',
      type: 'select',
      options: [
        { label: '园长', value: '园长' },
        { label: '教师', value: '教师' },
        { label: '保育员', value: '保育员' },
        { label: '保健医', value: '保健医' }
      ]
    },
    { field: 'entryDate', label: '入职日期', type: 'date' },
    {
      field: 'status',
      label: '在职状态',
      type: 'select',
      options: [
        { label: '在职', value: 1 },
        { label: '离职', value: 0 }
      ]
    }
  ],
  defaults: { gender: '女', role: '教师', status: 1 }
}
</script>
