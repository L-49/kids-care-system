<template>
  <div class="dashboard">
    <el-row :gutter="16">
      <el-col v-for="c in cards" :key="c.label" :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="icon" :style="{ background: c.color + '1a', color: c.color }">
            <el-icon :size="22"><component :is="c.icon" /></el-icon>
          </div>
          <div class="txt">
            <div class="label">{{ c.label }}</div>
            <div class="value">
              {{ c.value }}<span class="unit">{{ c.unit }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 4px">
      <el-col :xs="24" :md="16">
        <div class="page-card chart-card">
          <h3 class="page-title">近 7 天出勤趋势</h3>
          <div ref="trendRef" class="chart"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="8">
        <div class="page-card chart-card">
          <h3 class="page-title">在园人数分布</h3>
          <div ref="pieRef" class="chart"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 4px">
      <el-col :xs="24" :md="14">
        <div class="page-card">
          <h3 class="page-title">待缴费提醒</h3>
          <el-table :data="data.unpaidFeeTop || []" size="small" border stripe>
            <el-table-column prop="childName" label="幼儿" width="100" />
            <el-table-column prop="feeMonth" label="所属月份" width="110" />
            <el-table-column prop="feeType" label="费用类型" width="110" />
            <el-table-column label="金额">
              <template #default="{ row }">
                <span style="color: #f56c6c; font-weight: 600">
                  ¥{{ Number(row.amount || 0).toFixed(2) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
      <el-col :xs="24" :md="10">
        <div class="page-card">
          <h3 class="page-title">今日考勤概况</h3>
          <div class="att-grid">
            <div v-for="(v, k) in data.attendanceToday || {}" :key="k" class="att-item">
              <div class="num" :class="'c' + statusIndex(k)">{{ v }}</div>
              <div class="name">{{ k }}</div>
            </div>
          </div>
          <el-divider />
          <div class="quick">
            <div class="quick-item">
              <span>本月已收</span>
              <b class="income">¥{{ Number(data.monthIncome || 0).toFixed(2) }}</b>
            </div>
            <div class="quick-item">
              <span>待收金额</span>
              <b class="unpaid">¥{{ Number(data.unpaidAmount || 0).toFixed(2) }}</b>
            </div>
            <div class="quick-item">
              <span>未缴笔数</span>
              <b>{{ data.unpaidCount || 0 }} 笔</b>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { statApi } from '@/api'

const data = ref({})
const trendRef = ref()
const pieRef = ref()
let trendChart = null
let pieChart = null

const cards = computed(() => [
  { label: '在园幼儿', value: data.value.childCount ?? 0, unit: ' 人', icon: 'User', color: '#409eff' },
  { label: '在岗教职工', value: data.value.teacherCount ?? 0, unit: ' 人', icon: 'Avatar', color: '#67c23a' },
  { label: '班级数量', value: data.value.clazzCount ?? 0, unit: ' 个', icon: 'School', color: '#e6a23c' },
  { label: '待审批请假', value: data.value.pendingLeave ?? 0, unit: ' 条', icon: 'DocumentChecked', color: '#f56c6c' }
])

function statusIndex(k) {
  return { 出勤: 0, 迟到: 1, 请假: 2, 缺勤: 3 }[k] ?? 0
}

function renderTrend() {
  const list = data.value.attendanceTrend || []
  trendChart = echarts.init(trendRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['出勤人数', '记录总数'], right: 10 },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: list.map((i) => i.day) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '出勤人数',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: list.map((i) => Number(i.present || 0)),
        itemStyle: { color: '#409eff' },
        areaStyle: { opacity: 0.15 }
      },
      {
        name: '记录总数',
        type: 'bar',
        barWidth: 16,
        data: list.map((i) => Number(i.total || 0)),
        itemStyle: { color: '#c6e2ff' }
      }
    ]
  })
}

function renderPie() {
  const list = data.value.clazzDistribution || []
  pieChart = echarts.init(pieRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}：{c} 人（{d}%）' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['42%', '66%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{c}人' },
        data: list.map((i) => ({ name: i.name, value: Number(i.value || 0) }))
      }
    ]
  })
}

function resize() {
  trendChart && trendChart.resize()
  pieChart && pieChart.resize()
}

onMounted(async () => {
  const res = await statApi.overview()
  data.value = res.data || {}
  await nextTick()
  renderTrend()
  renderPie()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  trendChart && trendChart.dispose()
  pieChart && pieChart.dispose()
})
</script>

<style scoped>
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
}

.stat-card .icon {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-card .label {
  color: #909399;
  font-size: 13px;
}

.stat-card .value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  line-height: 1.3;
}

.stat-card .unit {
  font-size: 13px;
  font-weight: 400;
  color: #909399;
  margin-left: 2px;
}

.chart-card {
  padding-bottom: 16px;
}

.chart {
  height: 300px;
}

.att-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.att-item {
  background: #f7f9fc;
  border-radius: 8px;
  padding: 12px;
  text-align: center;
}

.att-item .num {
  font-size: 22px;
  font-weight: 600;
}

.att-item .num.c0 {
  color: #67c23a;
}
.att-item .num.c1 {
  color: #e6a23c;
}
.att-item .num.c2 {
  color: #409eff;
}
.att-item .num.c3 {
  color: #f56c6c;
}

.att-item .name {
  color: #909399;
  font-size: 13px;
}

.quick {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #606266;
  padding-bottom: 12px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.quick-item b {
  font-size: 16px;
}

.income {
  color: #67c23a;
}

.unpaid {
  color: #f56c6c;
}
</style>
