<template>
  <CrudPage ref="crud" :config="config" :options="{ clazzList }">
    <template #col-missingFields="{ row }">
      <el-tag v-if="row.missingFields" type="warning" size="small">{{ row.missingFields }}</el-tag>
      <el-tag v-else type="success" size="small">已完整</el-tag>
    </template>

    <template #toolbar>
      <el-button :icon="Download" @click="downloadTemplate">下载导入模板</el-button>
      <el-upload
        :show-file-list="false"
        :auto-upload="false"
        accept=".xlsx,.xls"
        :on-change="handleFileChange"
      >
        <el-button type="success" :icon="Upload" :loading="importing">Excel 批量导入</el-button>
      </el-upload>
    </template>
  </CrudPage>

  <el-dialog v-model="resultVisible" title="导入结果" width="660px">
    <el-alert
      :title="summaryText"
      :type="importResult.failCount ? 'warning' : 'success'"
      :closable="false"
      show-icon
    />
    <el-table
      v-if="importResult.errors && importResult.errors.length"
      :data="importResult.errors"
      size="small"
      border
      max-height="320"
      style="margin-top: 12px"
    >
      <el-table-column prop="rowNum" label="Excel 行号" width="100" align="center" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="message" label="失败原因" min-width="260" />
    </el-table>
    <template #footer>
      <el-button type="primary" @click="resultVisible = false">知道了</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Upload } from '@element-plus/icons-vue'
import CrudPage from '@/components/CrudPage.vue'
import { childApi, clazzApi, importChildren, childImportTemplate } from '@/api'

const crud = ref()
const clazzList = ref([])

// Excel 导入
const importing = ref(false)
const resultVisible = ref(false)
const importResult = ref({ total: 0, successCount: 0, failCount: 0, errors: [] })
const summaryText = computed(() => {
  const r = importResult.value
  return `共 ${r.total || 0} 行，成功 ${r.successCount || 0} 条，失败 ${r.failCount || 0} 条`
})

async function downloadTemplate() {
  try {
    const blob = await childImportTemplate()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '幼儿档案导入模板.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    // 错误提示由请求拦截器统一弹出
  }
}

function handleFileChange(uploadFile) {
  const file = uploadFile && uploadFile.raw
  if (!file) return
  const name = (file.name || '').toLowerCase()
  if (!name.endsWith('.xlsx') && !name.endsWith('.xls')) {
    ElMessage.error('只支持 .xlsx 或 .xls 格式')
    return
  }
  doImport(file)
}

async function doImport(file) {
  importing.value = true
  try {
    const res = await importChildren(file)
    importResult.value = res.data || { total: 0, successCount: 0, failCount: 0, errors: [] }
    resultVisible.value = true
    if (importResult.value.successCount > 0) {
      crud.value && crud.value.reload()
    }
  } catch (e) {
    // 错误提示由请求拦截器统一弹出
  } finally {
    importing.value = false
  }
}

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
    },
    {
      field: 'incompleteOnly',
      label: '档案完整度',
      type: 'select',
      placeholder: '全部',
      options: [{ label: '仅看待补全', value: true }]
    }
  ],
  columns: [
    { prop: 'name', label: '姓名', width: 100 },
    { prop: 'gender', label: '性别', width: 70 },
    { prop: 'birthday', label: '出生日期', width: 110 },
    { prop: 'clazzName', label: '所在班级', width: 100 },
    { prop: 'guardianName', label: '家长姓名', width: 100 },
    { prop: 'guardianPhone', label: '家长电话', width: 130 },
    { prop: 'allergy', label: '过敏史', minWidth: 110 },
    { prop: 'enrollDate', label: '入园日期', width: 110 },
    { prop: 'completeness', label: '完整度', width: 150, progress: true },
    { prop: 'missingFields', label: '待补全项', minWidth: 150 },
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
