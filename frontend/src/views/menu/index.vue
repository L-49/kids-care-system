<template>
  <div class="menu-page">
    <el-alert v-if="allergyList.length" type="warning" :closable="false" class="allergy-bar">
      <template #title>
        <span class="allergy-title">过敏儿童标注（{{ allergyList.length }} 人，排餐时请避开）</span>
        <el-tag
          v-for="c in allergyList"
          :key="c.id"
          type="danger"
          size="small"
          effect="plain"
          class="allergy-tag"
        >
          {{ c.name }}·{{ c.clazzName }}：{{ c.allergy }}
        </el-tag>
      </template>
    </el-alert>

    <CrudPage ref="crud" :config="config" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import CrudPage from '@/components/CrudPage.vue'
import { allergyChildren, menuApi } from '@/api'

const crud = ref()
const allergyList = ref([])

const today = new Date().toISOString().slice(0, 10)

const config = {
  title: '食谱',
  nameField: 'dishes',
  api: menuApi,
  search: [
    { field: 'keyword', label: '菜品', placeholder: '请输入菜品关键字' },
    {
      field: 'mealType',
      label: '餐次',
      type: 'select',
      options: [
        { label: '早餐', value: '早餐' },
        { label: '午餐', value: '午餐' },
        { label: '午点', value: '午点' }
      ]
    },
    { field: 'menuDate', label: '日期', type: 'date' }
  ],
  columns: [
    { prop: 'menuDate', label: '日期', width: 120 },
    {
      prop: 'mealType',
      label: '餐次',
      width: 90,
      mapTag: (v) => ({ 早餐: 'success', 午餐: 'warning', 午点: 'info' })[v] || 'info'
    },
    { prop: 'dishes', label: '菜品', minWidth: 260 },
    { prop: 'note', label: '营养说明', minWidth: 200 },
    { prop: 'publisher', label: '发布人', width: 100 }
  ],
  fields: [
    { field: 'menuDate', label: '日期', type: 'date', required: true },
    {
      field: 'mealType',
      label: '餐次',
      type: 'select',
      required: true,
      options: [
        { label: '早餐', value: '早餐' },
        { label: '午餐', value: '午餐' },
        { label: '午点', value: '午点' }
      ]
    },
    { field: 'publisher', label: '发布人' },
    { field: 'dishes', label: '菜品', required: true, span: 24 },
    { field: 'note', label: '营养说明', type: 'textarea', span: 24 }
  ],
  defaults: { menuDate: today, mealType: '午餐', publisher: '刘芳' }
}

onMounted(async () => {
  const res = await allergyChildren()
  allergyList.value = res.data || []
})
</script>

<style scoped>
.allergy-bar {
  margin-bottom: 12px;
}

.allergy-title {
  font-weight: 600;
  margin-right: 8px;
}

.allergy-tag {
  margin-right: 6px;
}
</style>
