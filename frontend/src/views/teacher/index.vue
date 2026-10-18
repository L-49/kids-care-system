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

    <CrudPage ref="crud" :config="config" />

    <!-- 任教班级抽屉 -->
    <el-drawer v-model="classDrawer" :title="`${currentTeacherName} · 任教班级`" size="56%">
      <div style="margin-bottom: 12px" v-if="!readonly">
        <el-button type="primary" :icon="Plus" @click="openSetHead">设为班主任</el-button>
      </div>
      <el-table :data="clazzRows" border stripe>
        <el-table-column type="index" label="#" width="52" align="center" />
        <el-table-column prop="name" label="班级" width="110" />
        <el-table-column label="来源" width="130">
          <template #default="{ row }">
            <el-tag :type="row.isHead ? 'success' : 'info'" disable-transitions>
              {{ row.isHead ? '担任班主任' : '授课教师' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="room" label="教室" width="100" />
        <el-table-column prop="capacity" label="额定人数" width="100" align="center" />
        <el-table-column prop="childCount" label="在园人数" width="100" align="center" />
        <el-table-column label="操作" v-if="!readonly">
          <template #default="{ row }">
            <el-button v-if="row.isHead" link type="danger" @click="cancelHead(row)">取消班主任</el-button>
            <span v-else style="color: #c0c4cc">—</span>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- 设为班主任对话框 -->
    <el-dialog v-model="setHeadVisible" title="设为班主任" width="420px">
      <el-form label-width="90px">
        <el-form-item label="选择班级">
          <el-select v-model="setHeadClazzId" placeholder="选择无班主任的班级" style="width: 100%">
            <el-option v-for="c in candidateClazzes" :key="c.id" :label="`${c.name}（${c.room || '未设教室'}）`" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="setHeadVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!setHeadClazzId" @click="setHeadTeacher">确定</el-button>
      </template>
    </el-dialog>

    <!-- 任教课程抽屉 -->
    <el-drawer v-model="courseDrawer" :title="`${currentTeacherName} · 任教课程`" size="62%">
      <div style="margin-bottom: 12px" v-if="!readonly">
        <el-button type="primary" :icon="Plus" @click="openCourseAdd">新增课程</el-button>
      </div>
      <el-table v-loading="courseLoading" :data="courseList" border stripe>
        <el-table-column prop="title" label="课程" width="140" />
        <el-table-column prop="clazzName" label="班级" width="110" />
        <el-table-column prop="courseDate" label="日期" width="120" />
        <el-table-column label="时间" width="140">
          <template #default="{ row }">{{ row.startTime || '--' }} ~ {{ row.endTime || '--' }}</template>
        </el-table-column>
        <el-table-column prop="content" label="内容" show-overflow-tooltip>
          <template #default="{ row }">{{ row.content || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" v-if="!readonly">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCourseEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="removeCourse(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- 课程编辑对话框 -->
    <el-dialog
      v-model="courseVisible"
      :title="(courseForm.id ? '编辑' : '新增') + '课程'"
      width="540px"
      @closed="resetCourseForm"
    >
      <el-form ref="courseRef" :model="courseForm" :rules="courseRules" label-width="90px">
        <el-form-item label="课程名称" prop="title">
          <el-input v-model="courseForm.title" placeholder="如 绘本阅读" />
        </el-form-item>
        <el-form-item label="班级" prop="clazzId">
          <el-select v-model="courseForm.clazzId" placeholder="请选择班级" style="width: 100%">
            <el-option v-for="c in allClazzes" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="上课日期" prop="courseDate">
          <el-date-picker
            v-model="courseForm.courseDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="时间">
          <el-time-picker
            v-model="courseForm.startTime"
            format="HH:mm"
            value-format="HH:mm"
            placeholder="开始"
            style="width: 46%"
          />
          <span style="margin: 0 6px">~</span>
          <el-time-picker
            v-model="courseForm.endTime"
            format="HH:mm"
            value-format="HH:mm"
            placeholder="结束"
            style="width: 46%"
          />
        </el-form-item>
        <el-form-item label="活动内容">
          <el-input v-model="courseForm.content" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="courseVisible = false">取消</el-button>
        <el-button type="primary" :loading="courseSaving" @click="submitCourse">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, UserFilled, CircleCheckFilled, CircleCloseFilled, School } from '@element-plus/icons-vue'
import CrudPage from '@/components/CrudPage.vue'
import { teacherApi, clazzApi, courseApi } from '@/api'

const crud = ref()

// 教师只能查看通讯录，仅园长可维护
const user = JSON.parse(localStorage.getItem('kids_user') || '{}')
const readonly = user.role === 'teacher'

// 统计卡片
const loaded = ref(false)
const stats = reactive({ total: 0, onJob: 0, offJob: 0, headCount: 0 })
const statCards = computed(() => [
  { label: '教职工总数', value: stats.total, unit: ' 人', color: '#409EFF', bg: '#ecf5ff', icon: UserFilled },
  { label: '在职人数', value: stats.onJob, unit: ' 人', color: '#67C23A', bg: '#f0f9eb', icon: CircleCheckFilled },
  { label: '离职人数', value: stats.offJob, unit: ' 人', color: '#909399', bg: '#f4f4f5', icon: CircleCloseFilled },
  { label: '担任班主任', value: stats.headCount, unit: ' 人', color: '#E6A23C', bg: '#fdf6ec', icon: School }
])

const allClazzes = ref([])

async function loadStats() {
  const [tRes, cRes] = await Promise.all([teacherApi.all(), clazzApi.all()])
  const teachers = tRes.data || []
  const clazzes = cRes.data || []
  allClazzes.value = clazzes
  stats.total = teachers.length
  stats.onJob = teachers.filter((t) => t.status === 1).length
  stats.offJob = teachers.filter((t) => t.status === 0).length
  stats.headCount = clazzes.filter((c) => c.headTeacherId != null).length
  loaded.value = true
}

const config = {
  title: '教职工',
  nameField: 'name',
  api: teacherApi,
  readonly,
  search: [
    { field: 'keyword', label: '关键字', placeholder: '姓名/电话' },
    {
      field: 'role',
      label: '岗位',
      type: 'select',
      options: [
        { label: '园长', value: '园长' },
        { label: '教师', value: '教师' },
        { label: '保育员', value: '保育员' },
        { label: '保健医', value: '保健医' }
      ]
    },
    {
      field: 'status',
      label: '状态',
      type: 'select',
      options: [
        { label: '在职', value: 1 },
        { label: '离职', value: 0 }
      ]
    }
  ],
  columns: [
    { prop: 'name', label: '姓名', width: 90 },
    { prop: 'gender', label: '性别', width: 65 },
    { prop: 'phone', label: '联系电话', width: 130 },
    { prop: 'role', label: '岗位', width: 90 },
    { prop: 'title', label: '职称', width: 100 },
    { prop: 'education', label: '学历', width: 80 },
    { prop: 'idCard', label: '身份证号', width: 170 },
    { prop: 'entryDate', label: '入职日期', width: 110 },
    { prop: 'emergencyContact', label: '紧急联系人', width: 105 },
    { prop: 'emergencyPhone', label: '紧急电话', width: 130 },
    {
      prop: 'status',
      label: '状态',
      width: 80,
      map: { 1: '在职', 0: '离职' },
      mapTag: (v) => (v === 1 ? 'success' : 'danger')
    }
  ],
  fields: [
    { field: 'name', label: '姓名', required: true },
    {
      field: 'gender',
      label: '性别',
      type: 'select',
      options: [
        { label: '男', value: '男' },
        { label: '女', value: '女' }
      ]
    },
    { field: 'phone', label: '联系电话', required: true },
    {
      field: 'role',
      label: '岗位',
      type: 'select',
      options: [
        { label: '园长', value: '园长' },
        { label: '教师', value: '教师' },
        { label: '保育员', value: '保育员' },
        { label: '保健医', value: '保健医' }
      ]
    },
    { field: 'title', label: '职称' },
    { field: 'education', label: '学历' },
    { field: 'idCard', label: '身份证号' },
    { field: 'entryDate', label: '入职日期', type: 'date' },
    {
      field: 'status',
      label: '在职状态',
      type: 'select',
      options: [
        { label: '在职', value: 1 },
        { label: '离职', value: 0 }
      ]
    },
    { field: 'address', label: '家庭住址', span: 24 },
    { field: 'emergencyContact', label: '紧急联系人' },
    { field: 'emergencyPhone', label: '紧急联系电话' }
  ],
  defaults: { gender: '女', role: '教师', status: 1 },
  rowActions: readonly
    ? []
    : [
        { label: '任教班级', type: 'primary', handler: (row) => openClasses(row) },
        { label: '任教课程', type: 'success', handler: (row) => openCourses(row) }
      ]
}

// 任教班级：班主任 + 授课班级合并展示
const classDrawer = ref(false)
const currentTeacherId = ref(null)
const currentTeacherName = ref('')
const clazzRows = ref([])
const candidateClazzes = ref([])
const setHeadVisible = ref(false)
const setHeadClazzId = ref(null)

async function openClasses(row) {
  currentTeacherId.value = row.id
  currentTeacherName.value = row.name
  classDrawer.value = true
  await refreshClazzRows()
}

async function refreshClazzRows() {
  const cRes = await clazzApi.all()
  const clazzes = cRes.data || []
  const coRes = await courseApi.page({ teacherId: currentTeacherId.value, pageSize: 999 })
  const courses = coRes.data || []
  const taughtIds = [...new Set(courses.map((c) => c.clazzId).filter(Boolean))]
  clazzRows.value = clazzes
    .filter((c) => c.headTeacherId === currentTeacherId.value || taughtIds.includes(c.id))
    .map((c) => ({ ...c, isHead: c.headTeacherId === currentTeacherId.value }))
  candidateClazzes.value = clazzes.filter((c) => c.headTeacherId == null)
  setHeadClazzId.value = null
}

function openSetHead() {
  setHeadClazzId.value = null
  setHeadVisible.value = true
}

async function setHeadTeacher() {
  if (!setHeadClazzId.value) return
  const full = (await clazzApi.detail(setHeadClazzId.value)).data
  full.headTeacherId = currentTeacherId.value
  await clazzApi.edit(full)
  ElMessage.success('已设为班主任')
  setHeadVisible.value = false
  refreshClazzRows()
  loadStats()
}

async function cancelHead(row) {
  await ElMessageBox.confirm(`取消「${row.name}」的班主任设置？`, '提示', { type: 'warning' })
  const full = (await clazzApi.detail(row.id)).data
  full.headTeacherId = null
  await clazzApi.edit(full)
  ElMessage.success('已取消班主任')
  refreshClazzRows()
  loadStats()
}

// 任教课程
const courseDrawer = ref(false)
const courseLoading = ref(false)
const courseList = ref([])

async function openCourses(row) {
  currentTeacherId.value = row.id
  currentTeacherName.value = row.name
  courseDrawer.value = true
  await loadCourses()
}

async function loadCourses() {
  courseLoading.value = true
  try {
    const res = await courseApi.page({ teacherId: currentTeacherId.value, pageSize: 999 })
    courseList.value = res.data || []
  } finally {
    courseLoading.value = false
  }
}

const courseVisible = ref(false)
const courseSaving = ref(false)
const courseRef = ref()
const courseForm = reactive({
  id: null,
  title: '',
  clazzId: null,
  courseDate: '',
  startTime: '',
  endTime: '',
  content: ''
})
const courseRules = {
  title: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  clazzId: [{ required: true, message: '请选择班级', trigger: 'change' }]
}

function resetCourseForm() {
  Object.keys(courseForm).forEach((k) => {
    courseForm[k] = k === 'id' ? null : ''
  })
}

function openCourseAdd() {
  resetCourseForm()
  courseVisible.value = true
}

function openCourseEdit(row) {
  resetCourseForm()
  Object.assign(courseForm, JSON.parse(JSON.stringify(row)))
  courseVisible.value = true
}

async function submitCourse() {
  await courseRef.value.validate(async (valid) => {
    if (!valid) return
    courseSaving.value = true
    try {
      const payload = { ...courseForm, teacherId: currentTeacherId.value }
      if (payload.id) {
        await courseApi.edit(payload)
      } else {
        await courseApi.add(payload)
      }
      ElMessage.success('保存成功')
      courseVisible.value = false
      loadCourses()
    } finally {
      courseSaving.value = false
    }
  })
}

async function removeCourse(row) {
  await ElMessageBox.confirm(`删除课程「${row.title}」？`, '提示', { type: 'warning' })
  await courseApi.remove(row.id)
  ElMessage.success('已删除')
  loadCourses()
}

onMounted(() => {
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
