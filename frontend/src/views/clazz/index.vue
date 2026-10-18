<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-cards" v-if="loaded">
      <el-col :span="6" v-for="s in statCards" :key="s.label">
        <el-card shadow="hover" :body-style="{ padding: '18px 20px' }">
          <div class="stat-label">{{ s.label }}</div>
          <div class="stat-value" :style="{ color: s.color }">{{ s.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <CrudPage ref="crud" :config="config" :options="{ teacherList }" />

    <!-- 查看幼儿抽屉 -->
    <el-drawer v-model="childDrawer" :title="drawerTitle" size="48%">
      <el-table v-loading="childLoading" :data="childList" border stripe>
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="70" />
        <el-table-column prop="guardianName" label="家长" width="100" />
        <el-table-column prop="guardianPhone" label="联系电话" width="140" />
        <el-table-column label="在园状态" width="120">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              active-text="在园"
              inactive-text="离园"
              :disabled="readonly"
              @change="onChildStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" v-if="!readonly">
          <template #default="{ row }">
            <el-button link type="primary" @click="openTransfer(row)">调班</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- 调班对话框 -->
    <el-dialog v-model="transferVisible" title="幼儿调班" width="420px" @closed="transferChild = null">
      <el-form label-width="90px">
        <el-form-item label="幼儿">
          <span>{{ transferChild?.name }}（当前班级：{{ currentClazzName }}）</span>
        </el-form-item>
        <el-form-item label="目标班级">
          <el-select v-model="transferForm.newClazzId" placeholder="请选择目标班级" style="width: 100%">
            <el-option v-for="c in transferClazzOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" :loading="transferSaving" @click="submitTransfer">确定调班</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import CrudPage from '@/components/CrudPage.vue'
import { clazzApi, teacherApi, childApi } from '@/api'

const crud = ref()
const teacherList = ref([])
const allClazzes = ref([])

// 教师可查看班级，但只有园长能新增/修改/删除、调班
const user = JSON.parse(localStorage.getItem('kids_user') || '{}')
const readonly = user.role === 'teacher'

// 统计卡片
const loaded = ref(false)
const stats = reactive({ clazzCount: 0, childCount: 0, fullRate: '0%', overload: 0 })
const statCards = computed(() => [
  { label: '班级总数', value: stats.clazzCount, color: '#409EFF' },
  { label: '在园幼儿', value: stats.childCount, color: '#67C23A' },
  { label: '整体满员率', value: stats.fullRate, color: '#E6A23C' },
  { label: '超载班级', value: stats.overload, color: '#F56C6C' }
])

async function loadStats() {
  const res = await clazzApi.all()
  const list = res.data || []
  let childSum = 0, capSum = 0, overload = 0
  list.forEach((c) => {
    childSum += c.childCount || 0
    capSum += c.capacity || 0
    if (c.fullStatus === 2) overload++
  })
  stats.clazzCount = list.length
  stats.childCount = childSum
  stats.fullRate = capSum ? Math.round((childSum / capSum) * 100) + '%' : '0%'
  stats.overload = overload
  loaded.value = true
}

const config = {
  title: '班级',
  nameField: 'name',
  api: clazzApi,
  readonly,
  search: [{ field: 'keyword', label: '班级名称', placeholder: '请输入班级名称' }],
  columns: [
    { prop: 'name', label: '班级名称', width: 120 },
    { prop: 'ageRange', label: '适龄段', width: 100 },
    { prop: 'room', label: '教室', width: 100 },
    { prop: 'capacity', label: '额定人数', width: 100 },
    { prop: 'headTeacherName', label: '班主任', width: 110 },
    { prop: 'openDate', label: '开班日期', width: 120 },
    { prop: 'childCount', label: '在园人数', width: 100 },
    {
      prop: 'fullStatus',
      label: '满员状态',
      width: 110,
      map: { 0: '正常', 1: '满员', 2: '超载' },
      mapTag: (v) => (v === 2 ? 'danger' : v === 1 ? 'warning' : 'success')
    }
  ],
  fields: [
    { field: 'name', label: '班级名称', required: true },
    { field: 'ageRange', label: '适龄段', placeholder: '如 3-4岁' },
    { field: 'room', label: '教室', placeholder: '如 A101' },
    { field: 'capacity', label: '额定人数', type: 'number', min: 1, max: 60 },
    { field: 'headTeacherId', label: '班主任', type: 'select', options: 'teacherList' },
    { field: 'openDate', label: '开班日期', type: 'date' },
    { field: 'remark', label: '班级特色', type: 'textarea' }
  ],
  defaults: { capacity: 25 },
  rowActions: [
    { label: '查看幼儿', type: 'primary', handler: (row) => openChildren(row) }
  ]
}

// 查看幼儿
const childDrawer = ref(false)
const childLoading = ref(false)
const childList = ref([])
const currentClazzId = ref(null)
const currentClazzName = ref('')
const drawerTitle = computed(() => `「${currentClazzName.value}」在园幼儿`)

async function openChildren(row) {
  currentClazzId.value = row.id
  currentClazzName.value = row.name
  childDrawer.value = true
  childLoading.value = true
  try {
    const res = await childApi.page({ clazzId: row.id, pageSize: 200 })
    childList.value = (res.data || []).map((c) => ({ ...c, status: c.status ?? 1 }))
  } finally {
    childLoading.value = false
  }
}

async function onChildStatusChange(row) {
  const target = row.status
  try {
    const full = await childApi.detail(row.id)
    full.status = target
    await childApi.edit(full)
    ElMessage.success(target === 1 ? '已设为在园' : '已设为离园')
    loadStats()
  } catch (e) {
    row.status = target === 1 ? 0 : 1
  }
}

// 调班
const transferVisible = ref(false)
const transferSaving = ref(false)
const transferChild = ref(null)
const transferForm = reactive({ newClazzId: null })
const transferClazzOptions = ref([])

function openTransfer(row) {
  transferChild.value = JSON.parse(JSON.stringify(row))
  transferForm.newClazzId = null
  transferClazzOptions.value = allClazzes.value.filter((c) => c.id !== currentClazzId.value)
  transferVisible.value = true
}

async function submitTransfer() {
  if (!transferForm.newClazzId) {
    ElMessage.warning('请选择目标班级')
    return
  }
  transferSaving.value = true
  try {
    const full = await childApi.detail(transferChild.value.id)
    full.clazzId = transferForm.newClazzId
    await childApi.edit(full)
    ElMessage.success('调班成功')
    transferVisible.value = false
    openChildren({ id: currentClazzId.value, name: currentClazzName.value })
    loadStats()
  } finally {
    transferSaving.value = false
  }
}

onMounted(async () => {
  const tRes = await teacherApi.all()
  teacherList.value = (tRes.data || []).map((t) => ({ label: `${t.name}（${t.role}）`, value: t.id }))
  const cRes = await clazzApi.all()
  allClazzes.value = cRes.data || []
  loadStats()
})
</script>

<style scoped>
.stat-cards { margin-bottom: 16px; }
.stat-label { color: #909399; font-size: 13px; }
.stat-value { font-size: 26px; font-weight: 700; margin-top: 6px; }
</style>
