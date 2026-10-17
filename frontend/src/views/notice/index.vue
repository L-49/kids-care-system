<template>
  <CrudPage ref="crud" :config="config" :options="{ typeOptions }" />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import CrudPage from '@/components/CrudPage.vue'
import { noticeApi, dictApi } from '@/api'

const crud = ref()
const user = JSON.parse(localStorage.getItem('kids_user') || '{}')

// 类型选项来自数据字典 notice_type，新增类型只需在字典里加一项，不用改代码
const typeOptions = ref([])

// 字典项的展示颜色（未配置的类型回落到灰色）
const typeColor = { 通知: 'primary', 活动: 'success', 食谱: 'warning', 放假: 'info' }

const config = {
  title: '通知公告',
  nameField: 'title',
  api: noticeApi,
  search: [
    { field: 'keyword', label: '标题', placeholder: '请输入标题关键字' },
    { field: 'type', label: '类型', type: 'select', options: 'typeOptions' }
  ],
  columns: [
    { prop: 'title', label: '标题', minWidth: 220 },
    { prop: 'type', label: '类型', width: 90, mapTag: (v) => typeColor[v] || 'info' },
    { prop: 'publisher', label: '发布人', width: 110 },
    { prop: 'publishTime', label: '发布时间', width: 175 },
    { prop: 'content', label: '内容', minWidth: 260 }
  ],
  fields: [
    { field: 'title', label: '标题', required: true, span: 24 },
    { field: 'type', label: '类型', type: 'select', options: 'typeOptions', required: true },
    { field: 'publisher', label: '发布人' },
    { field: 'content', label: '公告内容', type: 'textarea', span: 24 }
  ],
  defaults: { type: '通知', publisher: user.realName || user.username || '管理员' }
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
})
</script>
