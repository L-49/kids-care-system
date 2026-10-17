<template>
  <CrudPage ref="crud" :config="config" :options="{ childList }" />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CrudPage from '@/components/CrudPage.vue'
import { childApi, feeApi, payFee } from '@/api'

const crud = ref()
const childList = ref([])

const now = new Date()
const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`

const feeTypes = [
  { label: '保教费', value: '保教费' },
  { label: '餐费', value: '餐费' },
  { label: '校车费', value: '校车费' },
  { label: '其他', value: '其他' }
]

const config = {
  title: '收费记录',
  nameField: 'childName',
  api: feeApi,
  search: [
    { field: 'keyword', label: '幼儿姓名', placeholder: '请输入姓名' },
    { field: 'feeMonth', label: '所属月份', type: 'month' },
    {
      field: 'payStatus',
      label: '缴费状态',
      type: 'select',
      options: [
        { label: '未缴', value: 0 },
        { label: '已缴', value: 1 }
      ]
    }
  ],
  columns: [
    { prop: 'childName', label: '幼儿', width: 100 },
    { prop: 'feeMonth', label: '所属月份', width: 110 },
    { prop: 'feeType', label: '费用类型', width: 110 },
    { prop: 'amount', label: '金额', width: 120, money: true },
    {
      prop: 'payStatus',
      label: '缴费状态',
      width: 100,
      map: { 1: '已缴', 0: '未缴' },
      mapTag: (v) => (v === 1 ? 'success' : 'warning')
    },
    { prop: 'payTime', label: '缴费时间', width: 170 },
    { prop: 'remark', label: '备注', minWidth: 120 }
  ],
  fields: [
    { field: 'childId', label: '幼儿', type: 'select', options: 'childList', required: true },
    { field: 'feeType', label: '费用类型', type: 'select', options: feeTypes, required: true },
    { field: 'amount', label: '金额（元）', type: 'number', min: 0, max: 99999, precision: 2 },
    { field: 'feeMonth', label: '所属月份', type: 'month', required: true },
    { field: 'remark', label: '备注', type: 'textarea', span: 24 }
  ],
  defaults: { feeMonth: currentMonth, feeType: '保教费', amount: 1800 },
  rowActions: [
    {
      label: '缴费登记',
      type: 'success',
      show: (row) => row.payStatus === 0,
      handler: (row, ctx) => {
        ElMessageBox.confirm(
          `确认为「${row.childName}」缴纳 ${row.feeMonth} ${row.feeType} ¥${Number(row.amount).toFixed(2)} 吗？`,
          '缴费登记',
          { type: 'warning' }
        )
          .then(async () => {
            await payFee(row.id)
            ElMessage.success('缴费成功')
            ctx.reload()
          })
          .catch(() => {})
      }
    }
  ]
}

onMounted(async () => {
  const res = await childApi.all()
  childList.value = (res.data || []).map((i) => ({
    label: `${i.name}（${i.clazzName || '未分班'}）`,
    value: i.id
  }))
})
</script>
