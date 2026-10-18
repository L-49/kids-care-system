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

    <!-- 视图切换 + 周导航 -->
    <div class="view-bar no-print">
      <el-radio-group v-model="view">
        <el-radio-button value="week">周视图</el-radio-button>
        <el-radio-button value="list">列表视图</el-radio-button>
      </el-radio-group>

      <template v-if="view === 'week'">
        <el-button-group class="nav">
          <el-button :icon="ArrowLeft" @click="shiftWeek(-1)">上一周</el-button>
          <el-button @click="resetWeek">本周</el-button>
          <el-button @click="shiftWeek(1)">
            下一周
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </el-button-group>
        <span class="range">
          {{ weekRange }}
          <el-tag v-if="isCurrentWeek" size="small" type="success" effect="plain">本周</el-tag>
          <el-tag size="small" type="info" effect="plain">已排 {{ weekData.length }} / 21 餐</el-tag>
        </span>
        <div class="right">
          <el-button v-if="!readonly" type="primary" plain :icon="CopyDocument" @click="handleCopyLast">
            复制上周
          </el-button>
          <el-button :icon="Printer" @click="printWeek">打印</el-button>
        </div>
      </template>
    </div>

    <!-- 周视图 -->
    <div v-show="view === 'week'" class="week-view print-area" v-loading="weekLoading">
      <table class="week-table">
        <thead>
          <tr>
            <th class="meal-th">餐次</th>
            <th v-for="d in days" :key="d.date" :class="{ today: d.today }">
              <div class="day-label">{{ d.label }}</div>
              <div class="day-md">{{ d.md }}</div>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in mealTypes" :key="m">
            <td class="meal-td">
              <el-tag :type="mealTag(m)" effect="light" size="small">{{ m }}</el-tag>
            </td>
            <td v-for="d in days" :key="d.date + m" :class="{ today: d.today }">
              <div v-if="cell(m, d.date)" class="cell">
                <div class="dishes">{{ cell(m, d.date).dishes }}</div>
                <div v-if="cell(m, d.date).note" class="note">{{ cell(m, d.date).note }}</div>
              </div>
              <span v-else class="empty">—</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 列表视图 -->
    <CrudPage v-show="view === 'list'" ref="crud" :config="config" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ArrowRight, CopyDocument, Printer } from '@element-plus/icons-vue'
import CrudPage from '@/components/CrudPage.vue'
import { allergyChildren, copyLastWeekMenu, menuApi, menuWeek } from '@/api'

const crud = ref()
const allergyList = ref([])

const user = JSON.parse(localStorage.getItem('kids_user') || '{}')
const readonly = user.role === 'parent'

const pad = (n) => String(n).padStart(2, '0')
const fmt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
const parse = (s) => {
  const [y, m, d] = s.split('-').map(Number)
  return new Date(y, m - 1, d)
}
const now = new Date()
const today = fmt(now)

/** 取某天所在周的周一 */
function mondayOf(date) {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const wd = d.getDay() || 7
  d.setDate(d.getDate() - (wd - 1))
  return d
}

const weekStart = ref(fmt(mondayOf(new Date())))
const weekData = ref([])
const weekLoading = ref(false)
const view = ref('week')

const mealTypes = ['早餐', '午餐', '午点']
const dayLabels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const days = computed(() =>
  dayLabels.map((label, i) => {
    const d = parse(weekStart.value)
    d.setDate(d.getDate() + i)
    const date = fmt(d)
    return {
      date,
      label,
      md: `${pad(d.getMonth() + 1)}-${pad(d.getDate())}`,
      today: date === today
    }
  })
)

const weekRange = computed(() => {
  const a = days.value[0]
  const b = days.value[6]
  return `${a.md === b.md ? a.date : a.date + ' ~ ' + b.date}`
})

const isCurrentWeek = computed(() => weekStart.value === fmt(mondayOf(new Date())))

/** 日期 + 餐次 -> 食谱 */
const weekMap = computed(() => {
  const map = {}
  weekData.value.forEach((p) => {
    if (!map[p.menuDate]) map[p.menuDate] = {}
    map[p.menuDate][p.mealType] = p
  })
  return map
})

const cell = (meal, date) => weekMap.value[date]?.[meal]

const mealTag = (m) => ({ 早餐: 'success', 午餐: 'warning', 午点: 'info' })[m] || 'info'

async function loadWeek() {
  weekLoading.value = true
  try {
    const res = await menuWeek(weekStart.value)
    weekData.value = res.data || []
  } finally {
    weekLoading.value = false
  }
}

function shiftWeek(n) {
  const d = parse(weekStart.value)
  d.setDate(d.getDate() + n * 7)
  weekStart.value = fmt(d)
}

function resetWeek() {
  weekStart.value = fmt(mondayOf(new Date()))
}

async function handleCopyLast() {
  try {
    await ElMessageBox.confirm(
      '将上一周同星期几的食谱复制到本周，本周已排餐的日期会跳过、不覆盖。是否继续？',
      '复制上周食谱',
      { type: 'info', confirmButtonText: '开始复制', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  const res = await copyLastWeekMenu(weekStart.value)
  const n = res.data || 0
  ElMessage.success(n > 0 ? `已复制 ${n} 条食谱` : '本周餐次已排满，没有可复制的记录')
  loadWeek()
  crud.value?.reload()
}

function printWeek() {
  window.print()
}

watch(weekStart, loadWeek)

const config = {
  title: '食谱',
  nameField: 'dishes',
  api: menuApi,
  readonly,
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
  loadWeek()
  // 过敏名单仅园长/教师可见
  if (readonly) return
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

.view-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.view-bar .range {
  color: #606266;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.view-bar .right {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.week-view {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 6px rgba(0, 21, 41, 0.06);
  overflow-x: auto;
}

.week-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  min-width: 900px;
}

.week-table th,
.week-table td {
  border: 1px solid #ebeef5;
  padding: 8px 10px;
  vertical-align: top;
  text-align: left;
  font-size: 13px;
  color: #303133;
}

.week-table thead th {
  background: #f5f7fa;
  color: #606266;
  font-weight: 600;
  text-align: center;
  padding: 8px 4px;
}

.week-table thead th.today {
  background: #ecf5ff;
  color: #409eff;
}

.week-table th.meal-th {
  width: 78px;
}

.week-table td.meal-td {
  text-align: center;
  vertical-align: middle;
  background: #fafafa;
}

.week-table td.today {
  background: #f5faff;
}

.day-label {
  font-size: 13px;
}

.day-md {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
  margin-top: 2px;
}

.week-table thead th.today .day-md {
  color: #66b1ff;
}

.week-table tbody td {
  height: 96px;
}

.cell .dishes {
  line-height: 1.5;
  color: #303133;
}

.cell .note {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.empty {
  color: #dcdfe6;
}

/* 打印：只保留食谱表格 */
@media print {
  :global(.aside),
  :global(.header),
  :global(.allergy-bar),
  .no-print {
    display: none !important;
  }

  :global(.main) {
    padding: 0 !important;
    overflow: visible !important;
  }

  .week-view {
    box-shadow: none;
    overflow: visible;
  }

  .week-table {
    min-width: 0;
  }
}
</style>
