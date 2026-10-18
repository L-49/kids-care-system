<template>
  <div class="food-page">
    <!-- 库存概览 -->
    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-label">食材品类</div>
        <div class="stat-value">{{ overview.kinds || 0 }}<span class="unit">种</span></div>
      </div>
      <div class="stat-card" :class="{ danger: overview.warnCount > 0 }">
        <div class="stat-label">低于预警线</div>
        <div class="stat-value">{{ overview.warnCount || 0 }}<span class="unit">种</span></div>
      </div>
      <div class="stat-card">
        <div class="stat-label">本月入库</div>
        <div class="stat-value">{{ overview.monthInTimes || 0 }}<span class="unit">笔</span></div>
        <div class="stat-sub">合计 {{ fmtQty(overview.monthInQty) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">本月出库</div>
        <div class="stat-value">{{ overview.monthOutTimes || 0 }}<span class="unit">笔</span></div>
        <div class="stat-sub">合计 {{ fmtQty(overview.monthOutQty) }}</div>
      </div>
    </div>

    <CrudPage ref="crud" :config="config">
      <template #toolbar>
        <el-button type="success" plain :icon="Download" @click="openStock('IN')">入库</el-button>
        <el-button type="warning" plain :icon="Upload" @click="openStock('OUT')">出库</el-button>
        <el-button :icon="Tickets" @click="openRecords()">出入库流水</el-button>
      </template>
    </CrudPage>

    <!-- 出入库弹窗 -->
    <el-dialog
      v-model="stockVisible"
      :title="stockType === 'IN' ? '食材入库' : '食材出库'"
      width="520px"
      destroy-on-close
    >
      <el-form :model="stockForm" label-width="90px">
        <el-form-item label="食材" required>
          <el-select
            v-model="stockForm.foodId"
            filterable
            placeholder="请选择食材"
            style="width: 100%"
          >
            <el-option v-for="f in foodList" :key="f.id" :value="f.id" :label="f.name">
              <span>{{ f.name }}</span>
              <span class="opt-qty">{{ fmtQty(f.quantity) }}{{ f.unit }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="当前库存">
          <span v-if="currentFood" class="cur">
            {{ fmtQty(currentFood.quantity) }} {{ currentFood.unit }}
            <el-tag v-if="currentFood.warn" type="danger" size="small" effect="plain">低于预警线</el-tag>
          </span>
          <span v-else class="muted">请先选择食材</span>
        </el-form-item>
        <el-form-item :label="stockType === 'IN' ? '入库数量' : '出库数量'" required>
          <el-input-number
            v-model="stockForm.quantity"
            :min="0.01"
            :precision="2"
            :step="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="操作后">
          <span class="after" :class="{ short: afterValue < 0 }">{{ afterText }}</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="stockForm.remark"
            :placeholder="stockType === 'IN' ? '如：采购入库 / 供应商送货' : '如：厨房领用 / 午餐制作'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockVisible = false">取 消</el-button>
        <el-button type="primary" :loading="stockSaving" @click="submitStock">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 出入库流水 -->
    <el-dialog v-model="recordVisible" title="出入库流水" width="920px" destroy-on-close>
      <div class="record-filter">
        <el-select
          v-model="recordQuery.type"
          placeholder="全部类型"
          clearable
          style="width: 130px"
          @change="handleRecordSearch"
        >
          <el-option label="入库" value="IN" />
          <el-option label="出库" value="OUT" />
        </el-select>
        <el-date-picker
          v-model="recordRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
          @change="handleRecordSearch"
        />
        <el-tag
          v-if="recordQuery.foodId"
          closable
          type="info"
          effect="plain"
          @close="clearRecordFood"
        >
          仅看：{{ recordFoodName }}
        </el-tag>
        <span class="tip">共 {{ recordTotal }} 条</span>
      </div>
      <el-table :data="records" border stripe size="small" max-height="400">
        <el-table-column prop="createTime" label="时间" width="160" />
        <el-table-column prop="foodName" label="食材" width="100" />
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === 'IN' ? 'success' : 'warning'" size="small">
              {{ row.typeText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="110" align="right">
          <template #default="{ row }">
            <span :class="row.type === 'IN' ? 'in' : 'out'">
              {{ row.type === 'IN' ? '+' : '-' }}{{ fmtQty(row.quantity) }}{{ row.unit }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="库存变化" min-width="150" align="center">
          <template #default="{ row }">
            {{ fmtQty(row.beforeQty) }} → {{ fmtQty(row.afterQty) }}
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="90" />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      </el-table>
      <el-pagination
        v-model:current-page="recordQuery.pageNum"
        v-model:page-size="recordQuery.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="recordTotal"
        layout="total, sizes, prev, pager, next"
        background
        class="record-pager"
        @size-change="loadRecords"
        @current-change="loadRecords"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Tickets, Upload } from '@element-plus/icons-vue'
import CrudPage from '@/components/CrudPage.vue'
import { foodApi, foodOverview, stockChange, stockRecords } from '@/api'

const crud = ref()
const overview = ref({})
const foodList = ref([])

const categories = [
  { label: '主食', value: '主食' },
  { label: '蔬菜', value: '蔬菜' },
  { label: '肉类', value: '肉类' },
  { label: '水果', value: '水果' },
  { label: '其他', value: '其他' }
]

/** 数量统一保留两位小数，去掉多余的 .00 */
function fmtQty(v) {
  const n = Number(v || 0)
  return Number.isInteger(n) ? String(n) : n.toFixed(2)
}

// ---------------- 出入库
const stockVisible = ref(false)
const stockSaving = ref(false)
const stockType = ref('IN')
const stockForm = reactive({ foodId: null, quantity: 1, remark: '' })

const currentFood = computed(() => foodList.value.find((f) => f.id === stockForm.foodId) || null)

const afterValue = computed(() => {
  if (!currentFood.value) return 0
  const base = Number(currentFood.value.quantity || 0)
  const n = Number(stockForm.quantity || 0)
  return stockType.value === 'IN' ? base + n : base - n
})

const afterText = computed(() => {
  if (!currentFood.value) return '—'
  const v = afterValue.value
  return `${v.toFixed(2)} ${currentFood.value.unit}` + (v < 0 ? '  （库存不足）' : '')
})

async function loadFoods() {
  const res = await foodApi.all()
  foodList.value = res.data || []
}

async function loadOverview() {
  const res = await foodOverview()
  overview.value = res.data || {}
}

function openStock(type, row) {
  stockType.value = type
  stockForm.foodId = row ? row.id : null
  stockForm.quantity = 1
  stockForm.remark = ''
  stockVisible.value = true
  loadFoods()
}

async function submitStock() {
  if (!stockForm.foodId) {
    ElMessage.warning('请先选择食材')
    return
  }
  if (!stockForm.quantity || stockForm.quantity <= 0) {
    ElMessage.warning('数量必须大于 0')
    return
  }
  if (stockType.value === 'OUT' && afterValue.value < 0) {
    ElMessage.warning('出库数量超过当前库存')
    return
  }
  stockSaving.value = true
  try {
    const res = await stockChange(stockForm.foodId, {
      type: stockType.value,
      quantity: stockForm.quantity,
      remark: stockForm.remark
    })
    const r = res.data || {}
    ElMessage.success(
      `${stockType.value === 'IN' ? '入库' : '出库'}成功，${r.foodName} 当前库存 ${fmtQty(r.afterQty)}${r.unit || ''}`
    )
    stockVisible.value = false
    loadOverview()
    loadFoods()
    crud.value?.reload()
  } finally {
    stockSaving.value = false
  }
}

// ---------------- 流水
const recordVisible = ref(false)
const records = ref([])
const recordTotal = ref(0)
const recordRange = ref([])
const recordQuery = reactive({ pageNum: 1, pageSize: 10, type: '', foodId: null })

const recordFoodName = computed(
  () => foodList.value.find((f) => f.id === recordQuery.foodId)?.name || ''
)

async function loadRecords() {
  const params = { pageNum: recordQuery.pageNum, pageSize: recordQuery.pageSize }
  if (recordQuery.type) params.type = recordQuery.type
  if (recordQuery.foodId) params.foodId = recordQuery.foodId
  if (recordRange.value && recordRange.value.length === 2) {
    params.startDate = recordRange.value[0]
    params.endDate = recordRange.value[1]
  }
  const res = await stockRecords(params)
  records.value = res.data || []
  recordTotal.value = res.total || 0
}

function handleRecordSearch() {
  recordQuery.pageNum = 1
  loadRecords()
}

function clearRecordFood() {
  recordQuery.foodId = null
  handleRecordSearch()
}

function openRecords(foodId) {
  recordVisible.value = true
  recordQuery.pageNum = 1
  recordQuery.type = ''
  recordQuery.foodId = foodId || null
  recordRange.value = []
  loadRecords()
}

// ---------------- 表格配置
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
  rowActions: [
    { label: '入库', type: 'success', handler: (row) => openStock('IN', row) },
    { label: '出库', type: 'warning', handler: (row) => openStock('OUT', row) },
    { label: '流水', type: 'primary', handler: (row) => openRecords(row.id) }
  ],
  defaults: { category: '蔬菜', unit: 'kg', quantity: 0, warnLine: 5 }
}

onMounted(() => {
  loadOverview()
  loadFoods()
})
</script>

<style scoped>
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px 18px;
  box-shadow: 0 1px 6px rgba(0, 21, 41, 0.06);
  border-left: 3px solid #409eff;
}

.stat-card.danger {
  border-left-color: #f56c6c;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-value {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}

.stat-value .unit {
  font-size: 13px;
  font-weight: 400;
  color: #909399;
  margin-left: 4px;
}

.stat-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #c0c4cc;
}

.opt-qty {
  float: right;
  color: #909399;
  font-size: 12px;
}

.cur {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #303133;
}

.muted {
  color: #c0c4cc;
}

.after {
  color: #67c23a;
  font-weight: 600;
}

.after.short {
  color: #f56c6c;
}

.record-filter {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.record-filter .tip {
  margin-left: auto;
  color: #909399;
  font-size: 13px;
}

.record-pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.in {
  color: #67c23a;
}

.out {
  color: #e6a23c;
}
</style>
