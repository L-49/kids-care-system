<template>
  <CrudPage ref="crud" :config="config" />
</template>

<script setup>
import { ref } from 'vue'
import CrudPage from '@/components/CrudPage.vue'
import { dictApi } from '@/api'

const crud = ref()

const dictTypes = [
  { label: '餐次 meal_type', value: 'meal_type' },
  { label: '食材分类 food_category', value: 'food_category' },
  { label: '收费类型 fee_type', value: 'fee_type' },
  { label: '考勤状态 att_status', value: 'att_status' }
]

const config = {
  title: '字典项',
  nameField: 'dictLabel',
  api: dictApi,
  search: [
    { field: 'keyword', label: '标签/值', placeholder: '请输入关键字' },
    { field: 'dictType', label: '字典类型', type: 'select', options: dictTypes }
  ],
  columns: [
    { prop: 'dictType', label: '字典类型', width: 160 },
    { prop: 'dictLabel', label: '标签', width: 130 },
    { prop: 'dictValue', label: '值', width: 130 },
    { prop: 'sort', label: '排序', width: 80 },
    {
      prop: 'status',
      label: '状态',
      width: 90,
      map: { 1: '启用', 0: '停用' },
      mapTag: (v) => (v === 1 ? 'success' : 'info')
    },
    { prop: 'remark', label: '备注', minWidth: 160 }
  ],
  fields: [
    {
      field: 'dictType',
      label: '字典类型',
      type: 'select',
      options: dictTypes,
      required: true
    },
    { field: 'dictLabel', label: '字典标签', required: true },
    { field: 'dictValue', label: '字典值', required: true },
    { field: 'sort', label: '排序', type: 'number', min: 0, max: 999 },
    {
      field: 'status',
      label: '状态',
      type: 'select',
      options: [
        { label: '启用', value: 1 },
        { label: '停用', value: 0 }
      ]
    },
    { field: 'remark', label: '备注', type: 'textarea', span: 24 }
  ],
  defaults: { dictType: 'meal_type', sort: 1, status: 1 }
}
</script>
