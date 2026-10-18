<template>
  <div>
    <!-- 统计卡片（带图标，对齐组员最新版） -->
    <el-row :gutter="16" class="stat-cards" v-if="loaded">
      <el-col :span="6" v-for="s in statCards" :key="s.label">
        <el-card shadow="hover" :body-style="{ padding: '18px 20px' }">
          <div class="stat-row">
            <div class="stat-icon" :style="{ background: s.bg, color: s.color }">
              <el-icon :size="22"><component :is="s.icon" /></el-icon>
            </div>
            <div class="stat-main">
              <div class="stat-label">{{ s.label }}</div>
              <div class="stat-value" :style="{ color: s.color }">
                {{ s.value }}<span class="stat-unit">{{ s.unit }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <CrudPage ref="crud" :config="config" :options="{ teacherList }">
      <template #col-remark="{ row }">{{ row.remark || '—' }}</template>
    </CrudPage>

    <!-- 查看幼儿对话框 -->
    <el-dialog v-model="childDialog" :title="`${currentClazzName} - 幼儿名单`" width="760px">
      <el-table v-loading="childLoading" :data="childList" border stripe max-height="480">
        <el-table-column type="index" label="#" width="52" align="center" />
        <el-table-column prop="name" label="姓名" width="110" />
        <el-table-column prop="gender" label="性别" width="70" align="center" />
        <el-table-column prop="guardianName" label="家长" width="110" />
        <el-table-column prop="guardianPhone" label="家长电话" width="150" />
        <el-table-column label="状态" width="140">
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
    </el-dialog>

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
import { OfficeBuilding, UserFilled, DataAnalysis, WarningFilled } from '@element-plus/icons-vue'
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
const stats = reactive({ clazzCount: 0, childCount: 0, fullRate: '0', overload: 0 })
const statCards = computed(() => [
  { label: '班级总数', value: stats.clazzCount, unit: ' 个', color: '#409EFF', bg: '#ecf5ff', icon: OfficeBuilding },
  { label: '在园幼儿', value: stats.childCount, unit: ' 人', color: '#67C23A', bg: '#f0f9eb', icon: UserFilled },
  { label: '整体满员率', value: stats.fullRate, unit: ' %', color: '#E6A23C', bg: '#fdf6ec', icon: DataAnalysis },
  { label: '超载班级', value: stats.overload, unit: ' 个', color: '#F56C6C', bg: '#fef0f0', icon: WarningFilled }
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
  stats.fullRate = capSum ? Math.round((childSum / capSum) * 100) : 0
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
    { prop: 'name', label: '班级名称', width: 110 },
    { prop: 'ageRange', label: '适龄段', width: 90 },
    { prop: 'room', label: '教室', width: 90 },
    { prop: 'capacity', label: '额定人数', width: 95 },
    { prop: 'childCount', label: '在园人数', width: 95 },
    {
      prop: 'fullStatus',
      label: '满员状态',
      width: 95,
      map: { 0: '正常', 1: '满员', 2: '超载' },
      mapTag: (v) => (v === 2 ? 'danger' : v === 1 ? 'warning' : 'success')
    },
    { prop: 'openDate', label: '开班日期', width: 115 },
    { prop: 'headTeacherName', label: '班主任', width: 100 },
    { prop: 'remark', label: '班级特色', minWidth: 180 }
  ],
  fields: [
    { field: 'name', label: '班级名称', required: true },
    { field: 'ageRange', label: '适龄段', placeholder: '如 3-4岁' },
    { field: 'room', label: '教室', placeholder: '如 A101' },
    { field: 'capacity', label: '额定人数', type: 'number', min: 1, max: 60 },
    { field: 'headTeacherId', label: '班主任', type: 'select', options: 'teacherList' },
    { field: 'openDate', label: '开班日期', type: 'date' },
    { field: 'remark', label: '班级特色', type: 'textarea', span: 24 }
  ],
  defaults: { capacity: 25 },
  rowActions: [
    { label: '查看幼儿', type: 'primary', handler: (row) => openChildren(row) }
  ]
}

// 查看幼儿
const childDialog = ref(false)
const childLoading = ref(false)
const childList = ref([])
const currentClazzId = ref(null)
const currentClazzName = ref('')

async function openChildren(row) {
  currentClazzId.value = row.id
  currentClazzName.value = row.name
  childDialog.value = true
  childLoading.value = true
  try {
    const res = await childApi.page({ clazzId: row.id, pageSize: 200 })
    childList.value = (res.data || []).map((c) => ({ ...c, status: c.status ?? 1 }))
  } finally {
    childLoading.value = false
  }
}

/**
 * 在园状态切换：先取完整档案再提交，任何情况下都兜底补 clazzId，
 * 避免后端「请选择所在班级」校验报错
 */
async function onChildStatusChange(row) {
  const target = row.status
  try {
    let full = null
    try {
      full = (await childApi.detail(row.id)).data
    } catch (e) {
      full = null
    }
    if (!full || full.clazzId == null) {
      full = { ...(full || {}), ...row, clazzId: currentClazzId.value }
    }
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
    let full = null
    try {
      full = (await childApi.detail(transferChild.value.id)).data
    } catch (e) {
      full = null
    }
    full = { ...(full || {}), ...transferChild.value, clazzId: transferForm.newClazzId }
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
.stat-row { display: flex; align-items: center; gap: 14px; }
.stat-icon {
  width: 46px; height: 46px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.stat-label { color: #909399; font-size: 13px; }
.stat-value { font-size: 26px; font-weight: 700; margin-top: 4px; }
.stat-unit { font-size: 13px; font-weight: 400; color: #909399; }
</style>
