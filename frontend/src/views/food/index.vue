<template>
  <CrudPage ref="crud" :config="config" />
</template>

<script setup>
import { ref } from 'vue'
import CrudPage from '@/components/CrudPage.vue'
import { foodApi } from '@/api'

const crud = ref()

const categories = [
  { label: '主食', value: '主食' },
  { label: '蔬菜', value: '蔬菜' },
  { label: '肉类', value: '肉类' },
  { label: '水果', value: '水果' },
  { label: '其他', value: '其他' }
]

const config = {
  title: '食材',
  nameField: 'name',
  api: foodApi,
  search: [
    { field: 'keyword', label: '食材名称', placeholder: '请输入名称' },
    { field: 'category', label: '分类', type: 'select', options: categories },
    {
      field: 'warnOnly',
      label: '库存状态',
      type: 'select',
      options: [{ label: '仅看预警', value: true }]
    }
  ],
  columns: [
    { prop: 'name', label: '食材名称', width: 130 },
    { prop: 'category', label: '分类', width: 90 },
    { prop: 'quantity', label: '库存数量', width: 110 },
    { prop: 'unit', label: '单位', width: 80 },
    { prop: 'warnLine', label: '预警线', width: 100 },
    {
      prop: 'warn',
      label: '库存状态',
      width: 120,
      map: { true: '低于预警线', false: '库存正常' },
      mapTag: (v) => (v ? 'danger' : 'success')
    },
    { prop: 'supplier', label: '供应商', minWidth: 130 },
    { prop: 'updateTime', label: '更新时间', width: 170 }
  ],
  fields: [
    { field: 'name', label: '食材名称', required: true },
    { field: 'category', label: '分类', type: 'select', options: categories, required: true },
    { field: 'quantity', label: '库存数量', type: 'number', precision: 2, min: 0 },
    { field: 'unit', label: '单位', placeholder: 'kg / L / 袋' },
    { field: 'warnLine', label: '预警线', type: 'number', precision: 2, min: 0 },
    { field: 'supplier', label: '供应商' }
  ],
  defaults: { category: '蔬菜', unit: 'kg', quantity: 0, warnLine: 5 }
}
</script>
