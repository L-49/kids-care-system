<template>
  <div class="my-child" v-loading="loading">
    <el-row :gutter="16">
      <el-col :xs="24" :md="9">
        <div class="page-card profile">
          <el-avatar :size="62" class="avatar">{{ (child.name || '?').slice(-1) }}</el-avatar>
          <div class="info">
            <h3>
              {{ child.name || '—' }}
              <el-tag size="small" :type="child.gender === '女' ? 'danger' : 'primary'" effect="plain">
                {{ child.gender }}
              </el-tag>
            </h3>
            <p>班级：{{ child.clazzName || '未分班' }}</p>
            <p>家长：{{ child.guardianName || '—' }} · {{ child.guardianPhone || '—' }}</p>
            <p>入园日期：{{ child.enrollDate || '—' }}</p>
            <p v-if="child.allergy" class="allergy">过敏/禁忌：{{ child.allergy }}</p>
          </div>
        </div>

        <div class="page-card">
          <h3 class="page-title">今日考勤</h3>
          <div v-if="todayAttendance" class="today">
            <el-tag :type="statusType(todayAttendance.status)" size="large">
              {{ todayAttendance.status }}
            </el-tag>
            <span v-if="todayAttendance.checkInTime" class="time">
              入园 {{ todayAttendance.checkInTime }}
            </span>
            <el-tag v-else type="info" size="small" effect="plain">未打卡</el-tag>
            <p v-if="todayAttendance.remark" class="remark">{{ todayAttendance.remark }}</p>
          </div>
          <el-empty v-else description="今天还没有考勤记录" :image-size="56" />
        </div>
      </el-col>

      <el-col :xs="24" :md="15">
        <div class="page-card">
          <h3 class="page-title">近一周考勤</h3>
          <div v-if="attendanceDays.length" class="days">
            <div v-for="d in attendanceDays" :key="d.day" class="day-item">
              <span class="day">{{ d.day }}</span>
              <el-tag :type="statusType(d.status)" size="small">{{ d.status }}</el-tag>
              <span class="clock">{{ d.checkInTime || '—' }}</span>
            </div>
          </div>
          <el-empty v-else description="近一周暂无考勤记录" :image-size="56" />
        </div>

        <div class="page-card">
          <div class="head">
            <h3 class="page-title">缴费记录</h3>
            <span v-if="Number(unpaidAmount) > 0" class="unpaid">
              待缴 ¥{{ Number(unpaidAmount).toFixed(2) }}
            </span>
            <span v-else class="paid">已全部结清</span>
          </div>
          <el-table :data="fees" size="small" border stripe>
            <el-table-column prop="feeMonth" label="所属月份" width="100" />
            <el-table-column prop="feeType" label="费用类型" width="100" />
            <el-table-column label="金额" width="110">
              <template #default="{ row }">¥{{ Number(row.amount || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.payStatus === 1 ? 'success' : 'warning'" size="small">
                  {{ row.payStatus === 1 ? '已缴' : '待缴' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="payTime" label="缴费时间" min-width="150" />
          </el-table>
        </div>
      </el-col>
    </el-row>

    <div class="page-card">
      <div class="head">
        <h3 class="page-title">请假记录</h3>
        <el-button type="primary" :icon="Plus" @click="openLeave">申请请假</el-button>
      </div>
      <el-table :data="leaves" size="small" border stripe>
        <el-table-column prop="startDate" label="开始日期" width="115" />
        <el-table-column prop="endDate" label="结束日期" width="115" />
        <el-table-column prop="days" label="天数" width="70" />
        <el-table-column prop="reason" label="请假事由" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="leaveTag(row.status)" size="small">{{ leaveText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="approver" label="审批人" width="100" />
        <el-table-column prop="approveTime" label="审批时间" width="170" />
      </el-table>
    </div>

    <el-dialog v-model="leaveVisible" title="申请请假" width="480px" destroy-on-close>
      <el-form :model="leaveForm" label-width="90px">
        <el-form-item label="开始日期" required>
          <el-date-picker
            v-model="leaveForm.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束日期" required>
          <el-date-picker
            v-model="leaveForm.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="请假事由" required>
          <el-input v-model="leaveForm.reason" type="textarea" :rows="3" placeholder="请填写请假原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="leaveVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitLeave">提 交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { leaveApi, myChildApi } from '@/api'

const loading = ref(false)
const child = ref({})
const todayAttendance = ref(null)
const attendanceDays = ref([])
const fees = ref([])
const unpaidAmount = ref(0)
const leaves = ref([])

const leaveVisible = ref(false)
const submitting = ref(false)
const leaveForm = reactive({ startDate: '', endDate: '', reason: '' })

function statusType(status) {
  return { 出勤: 'success', 迟到: 'warning', 请假: 'info', 缺勤: 'danger' }[status] || 'info'
}

function leaveText(s) {
  return { 0: '待审批', 1: '已通过', 2: '已驳回' }[s] || '—'
}

function leaveTag(s) {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await myChildApi()
    const data = res.data || {}
    child.value = data.child || {}
    todayAttendance.value = data.todayAttendance || null
    attendanceDays.value = data.attendanceDays || []
    fees.value = data.fees || []
    unpaidAmount.value = data.unpaidAmount || 0
    leaves.value = data.leaves || []
  } finally {
    loading.value = false
  }
}

function openLeave() {
  leaveForm.startDate = ''
  leaveForm.endDate = ''
  leaveForm.reason = ''
  leaveVisible.value = true
}

async function submitLeave() {
  if (!leaveForm.startDate || !leaveForm.endDate || !leaveForm.reason.trim()) {
    ElMessage.warning('请填写完整的请假信息')
    return
  }
  submitting.value = true
  try {
    await leaveApi.add({
      startDate: leaveForm.startDate,
      endDate: leaveForm.endDate,
      reason: leaveForm.reason.trim()
    })
    ElMessage.success('请假申请已提交，等待老师审批')
    leaveVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.profile {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.avatar {
  background: #409eff;
  font-size: 22px;
  flex-shrink: 0;
}

.info h3 {
  margin: 0 0 8px;
  font-size: 19px;
  color: #303133;
}

.info p {
  margin: 4px 0;
  color: #606266;
  font-size: 13px;
}

.info .allergy {
  color: #f56c6c;
  font-weight: 600;
}

.today {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.today .time {
  color: #606266;
  font-size: 13px;
}

.today .remark {
  width: 100%;
  margin: 4px 0 0;
  color: #909399;
  font-size: 12px;
}

.days {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.day-item {
  min-width: 88px;
  padding: 10px 8px;
  background: #f7f9fc;
  border-radius: 8px;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: center;
}

.day-item .day {
  color: #303133;
  font-weight: 600;
  font-size: 13px;
}

.day-item .clock {
  color: #909399;
  font-size: 12px;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.unpaid {
  color: #f56c6c;
  font-weight: 600;
}

.paid {
  color: #67c23a;
  font-size: 13px;
}
</style>
