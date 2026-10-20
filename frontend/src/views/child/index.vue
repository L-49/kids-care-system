<template>
  <CrudPage ref="crud" :config="config" :options="{ clazzList }">
    <template #col-missingFields="{ row }">
      <el-tag v-if="row.missingFields" type="warning" size="small">{{ row.missingFields }}</el-tag>
      <el-tag v-else type="success" size="small">已完整</el-tag>
    </template>

    <template #toolbar>
      <el-tooltip content="导出当前筛选结果（不是全表）" placement="bottom">
        <el-button type="primary" plain :icon="Download" :loading="exporting" @click="handleExport">
          导出 Excel
        </el-button>
      </el-tooltip>
      <el-button :icon="Document" @click="downloadTemplate">下载导入模板</el-button>
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

  <el-drawer v-model="historyVisible" :title="`变更记录 · ${historyChild}`" size="640px">
    <el-timeline v-if="historyList.length">
      <el-timeline-item
        v-for="h in historyList"
        :key="h.id"
        :timestamp="h.createTime"
        placement="top"
      >
        <div class="hist-item">
          <div class="hist-line">
            <el-tag size="small" :type="typeTag(h.changeType)">{{ h.changeType }}</el-tag>
            <template v-if="h.fieldLabel && h.fieldLabel !== '—'">
              <span class="hist-field">{{ h.fieldLabel }}</span>
              <span class="hist-old">{{ h.oldValue || '(空)' }}</span>
              <span class="hist-arrow">→</span>
              <span class="hist-new">{{ h.newValue || '(空)' }}</span>
            </template>
            <span v-else class="hist-field">{{ h.newValue }}</span>
          </div>
          <div class="hist-meta">
            操作人：{{ h.operator || '系统' }}
            <span v-if="h.batchId">　批次：{{ h.batchId }}</span>
          </div>
        </div>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-else description="暂无变更记录" />
  </el-drawer>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Document, Upload } from '@element-plus/icons-vue'
import CrudPage from '@/components/CrudPage.vue'
import {
  childApi,
  clazzApi,
  exportChildren,
  importChildren,
  childImportTemplate,
  childHistory
} from '@/api'

const crud = ref()
const clazzList = ref([])

// ==================== 日期与入园年龄校验（与后端 validateDateRules 同口径） ====================

function todayStr() {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

/** 入园时的月龄：满 36 个月且未满 84 个月即 3~6 岁 */
function monthsBetween(birthday, date) {
  const b = new Date(birthday)
  const d = new Date(date)
  let months = (d.getFullYear() - b.getFullYear()) * 12 + (d.getMonth() - b.getMonth())
  if (d.getDate() < b.getDate()) months -= 1
  return months
}

function ageText(birthday, date) {
  const m = monthsBetween(birthday, date)
  const years = Math.floor(m / 12)
  const months = m % 12
  return months > 0 ? `${years} 岁 ${months} 个月` : `${years} 岁`
}

function validateBirthday(value, form, callback) {
  if (!value) return callback(new Error('请选择出生日期'))
  if (value > todayStr()) return callback(new Error('出生日期不能晚于今天'))
  if (form.enrollDate && value >= form.enrollDate) {
    return callback(new Error('出生日期必须早于入园日期'))
  }
  callback()
}

function validateEnrollDate(value, form, callback) {
  if (!value) return callback(new Error('请选择入园日期'))
  if (!form.birthday) return callback()
  if (value <= form.birthday) return callback(new Error('入园日期必须晚于出生日期'))
  const months = monthsBetween(form.birthday, value)
  if (months < 36) {
    return callback(new Error(`按入园日期计算年龄为${ageText(form.birthday, value)}，不符合 3~6 岁入园要求（须满 3 周岁）`))
  }
  if (months >= 84) {
    return callback(new Error(`按入园日期计算年龄为${ageText(form.birthday, value)}，不符合 3~6 岁入园要求（须未满 7 周岁）`))
  }
  callback()
}

// 变更留痕
const historyVisible = ref(false)
const historyList = ref([])
const historyChild = ref('')

async function openHistory(row) {
  historyChild.value = row.name
  historyList.value = []
  historyVisible.value = true
  const res = await childHistory(row.id)
  historyList.value = res.data || []
}

function typeTag(t) {
  if (t === '新增') return 'success'
  if (t === '导入') return 'warning'
  return 'primary'
}

// Excel 导入
const importing = ref(false)
const resultVisible = ref(false)
const importResult = ref({ total: 0, successCount: 0, failCount: 0, errors: [] })
const summaryText = computed(() => {
  const r = importResult.value
  return `共 ${r.total || 0} 行，成功 ${r.successCount || 0} 条，失败 ${r.failCount || 0} 条`
})

/** 触发浏览器下载：模板与导出共用 */
function saveBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

function fileStamp() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}${p(d.getMonth() + 1)}${p(d.getDate())}`
}

// Excel 导出：跟随列表当前筛选条件
const exporting = ref(false)

async function handleExport() {
  exporting.value = true
  try {
    const params = (crud.value && crud.value.getQuery()) || {}
    const blob = await exportChildren(params)
    saveBlob(blob, `幼儿档案_${fileStamp()}.xlsx`)
    ElMessage.success('导出成功')
  } catch (e) {
    // 错误提示由请求拦截器统一弹出
  } finally {
    exporting.value = false
  }
}

async function downloadTemplate() {
  try {
    const blob = await childImportTemplate()
    saveBlob(blob, '幼儿档案导入模板.xlsx')
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
    {
      field: 'birthday',
      label: '出生日期',
      type: 'date',
      required: true,
      disabledFuture: true,
      validator: validateBirthday
    },
    { field: 'clazzId', label: '所在班级', type: 'select', options: 'clazzList', required: true },
    { field: 'guardianName', label: '家长姓名' },
    { field: 'guardianPhone', label: '家长电话', placeholder: '11 位手机号，用于家长端登录' },
    {
      field: 'enrollDate',
      label: '入园日期',
      type: 'date',
      required: true,
      validator: validateEnrollDate,
      help: '可填未来日期表示预约入园；须满 3 周岁、未满 7 周岁'
    },
    {
      field: 'status',
      label: '在园状态',
      type: 'select',
      disabled: true,
      placeholder: '保存后按入园日期自动判定',
      options: [
        { label: '在园', value: 1 },
        { label: '离园', value: 0 }
      ],
      help: '不可手工指定：入园日期晚于今天为「离园」（预约），当天及以前为「在园」'
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
  rowActions: [
    { label: '变更记录', type: 'primary', handler: (row) => openHistory(row) }
  ],
  defaults: { gender: '男' }
}

onMounted(async () => {
  const res = await clazzApi.all()
  clazzList.value = (res.data || []).map((c) => ({ label: c.name, value: c.id }))
})
</script>

<style scoped>
.hist-item {
  font-size: 13px;
}

.hist-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.hist-field {
  color: #303133;
  font-weight: 500;
}

.hist-old {
  color: #909399;
  text-decoration: line-through;
}

.hist-arrow {
  color: #c0c4cc;
}

.hist-new {
  color: #303133;
}

.hist-meta {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
}
</style>
