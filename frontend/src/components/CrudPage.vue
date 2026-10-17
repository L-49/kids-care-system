<template>
  <div class="page-card">
    <h3 class="page-title">{{ config.title }}</h3>

    <!-- 查询条件 -->
    <el-form v-if="config.search && config.search.length" :inline="true" :model="search" class="search-bar">
      <el-form-item v-for="s in config.search" :key="s.field" :label="s.label">
        <el-input
          v-if="!s.type || s.type === 'input'"
          v-model="search[s.field]"
          :placeholder="s.placeholder || '请输入'"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-else-if="s.type === 'select'"
          v-model="search[s.field]"
          :placeholder="s.placeholder || '全部'"
          clearable
          style="width: 160px"
        >
          <el-option
            v-for="o in resolveOptions(s.options)"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
        <el-date-picker
          v-else-if="s.type === 'date'"
          v-model="search[s.field]"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          style="width: 160px"
        />
        <el-date-picker
          v-else-if="s.type === 'month'"
          v-model="search[s.field]"
          type="month"
          value-format="YYYY-MM"
          placeholder="选择月份"
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <div class="table-toolbar">
      <div class="left">
        <el-button type="primary" :icon="Plus" @click="openAdd">新增{{ config.title }}</el-button>
        <slot name="toolbar" />
      </div>
      <span class="total-tip">共 {{ total }} 条记录</span>
    </div>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="rows" border stripe style="width: 100%">
      <el-table-column type="index" label="#" width="52" align="center" />
      <el-table-column
        v-for="c in config.columns"
        :key="c.prop"
        :prop="c.prop"
        :label="c.label"
        :width="c.width"
        :min-width="c.minWidth || 110"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          <el-tag v-if="c.mapTag" :type="tagType(c, row[c.prop])" disable-transitions>
            {{ formatCell(c, row[c.prop]) }}
          </el-tag>
          <span v-else-if="c.money" class="money">¥{{ Number(row[c.prop] || 0).toFixed(2) }}</span>
          <span v-else>{{ formatCell(c, row[c.prop]) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" :width="actionWidth" fixed="right" align="center">
        <template #default="{ row }">
          <el-button
            v-for="a in visibleActions(row)"
            :key="a.label"
            link
            :type="a.type || 'success'"
            @click="a.handler(row, ctx)"
          >
            {{ a.label }}
          </el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="load"
        @current-change="load"
      />
    </div>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="(form.id ? '编辑' : '新增') + config.title"
      width="680px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="16">
          <el-col v-for="f in config.fields" :key="f.field" :span="f.span || 12">
            <el-form-item :label="f.label" :prop="f.field">
              <el-input
                v-if="!f.type || f.type === 'input'"
                v-model="form[f.field]"
                :placeholder="f.placeholder || '请输入' + f.label"
                clearable
              />
              <el-input
                v-else-if="f.type === 'textarea'"
                v-model="form[f.field]"
                type="textarea"
                :rows="3"
                :placeholder="f.placeholder || '请输入' + f.label"
              />
              <el-select
                v-else-if="f.type === 'select'"
                v-model="form[f.field]"
                :placeholder="'请选择' + f.label"
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="o in resolveOptions(f.options)"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
              <el-input-number
                v-else-if="f.type === 'number'"
                v-model="form[f.field]"
                :min="f.min ?? 0"
                :max="f.max ?? 999999"
                :precision="f.precision ?? 0"
                style="width: 100%"
              />
              <el-date-picker
                v-else-if="f.type === 'date'"
                v-model="form[f.field]"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
              <el-date-picker
                v-else-if="f.type === 'month'"
                v-model="form[f.field]"
                type="month"
                value-format="YYYY-MM"
                placeholder="选择月份"
                style="width: 100%"
              />
              <el-radio-group v-else-if="f.type === 'radio'" v-model="form[f.field]">
                <el-radio v-for="o in resolveOptions(f.options)" :key="o.value" :value="o.value">
                  {{ o.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const props = defineProps({
  config: { type: Object, required: true },
  options: { type: Object, default: () => ({}) }
})

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({})

const query = reactive({ pageNum: 1, pageSize: 10 })
const search = reactive({})

const rules = computed(() => {
  const r = {}
  ;(props.config.fields || []).forEach((f) => {
    if (f.required) {
      r[f.field] = [{ required: true, message: '请填写' + f.label, trigger: 'blur' }]
    }
  })
  return r
})

const actionWidth = computed(() => {
  const n = (props.config.rowActions || []).length + 2
  return Math.max(150, n * 62)
})

const ctx = { reload: () => load() }

function resolveOptions(src) {
  if (!src) return []
  if (typeof src === 'string') {
    return props.options[src] || []
  }
  return src
}

function formatCell(col, val) {
  if (val === null || val === undefined || val === '') return '—'
  if (col.map) return col.map[val] ?? val
  return val
}

function tagType(col, val) {
  if (typeof col.mapTag === 'function') return col.mapTag(val)
  return 'info'
}

function visibleActions(row) {
  return (props.config.rowActions || []).filter((a) => !a.show || a.show(row))
}

async function load() {
  loading.value = true
  try {
    const params = { ...query, ...search }
    Object.keys(params).forEach((k) => {
      if (params[k] === '' || params[k] === null) delete params[k]
    })
    const res = await props.config.api.page(params)
    rows.value = res.data || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  load()
}

function handleReset() {
  Object.keys(search).forEach((k) => {
    search[k] = ''
  })
  query.pageNum = 1
  load()
}

function resetForm() {
  Object.keys(form).forEach((k) => delete form[k])
  formRef.value && formRef.value.clearValidate()
}

function openAdd() {
  resetForm()
  Object.assign(form, props.config.defaults || {})
  dialogVisible.value = true
}

function openEdit(row) {
  resetForm()
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = { ...form }
      if (payload.id) {
        await props.config.api.edit(payload)
        ElMessage.success('修改成功')
      } else {
        await props.config.api.add(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      load()
    } finally {
      saving.value = false
    }
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除「${row[props.config.nameField || 'name']}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
    .then(async () => {
      await props.config.api.remove(row.id)
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

onMounted(() => {
  ;(props.config.search || []).forEach((s) => {
    search[s.field] = s.default ?? ''
  })
  if (props.config.defaultSearch) {
    Object.assign(search, props.config.defaultSearch)
  }
  load()
})

defineExpose({ load, reload: load })
</script>

<style scoped>
.search-bar {
  padding: 4px 0 0;
}

.search-bar :deep(.el-form-item) {
  margin-bottom: 12px;
}

.table-toolbar .left {
  display: flex;
  gap: 8px;
  align-items: center;
}

.total-tip {
  color: #909399;
  font-size: 13px;
}

.money {
  color: #f56c6c;
  font-weight: 600;
}
</style>
