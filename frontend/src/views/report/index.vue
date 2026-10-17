<template>
  <div>
    <el-row :gutter="16">
      <el-col v-for="c in cards" :key="c.label" :xs="12" :md="6">
        <div class="stat-card">
          <div class="label">{{ c.label }}</div>
          <div class="value" :style="{ color: c.color }">{{ c.value }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :md="14">
        <div class="page-card chart-card">
          <h3 class="page-title">近 6 个月收费统计</h3>
          <div ref="feeRef" class="chart"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="10">
        <div class="page-card chart-card">
          <h3 class="page-title">各班出勤率</h3>
          <div ref="attRef" class="chart"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :md="14">
        <div class="page-card">
          <h3 class="page-title">班级出勤明细</h3>
          <el-table :data="data.attendanceByClazz || []" size="small" border stripe>
            <el-table-column prop="clazzName" label="班级" width="120" />
            <el-table-column prop="present" label="出勤人次" width="110" />
            <el-table-column prop="total" label="记录总数" width="110" />
            <el-table-column label="出勤率">
              <template #default="{ row }">
                <el-progress
                  :percentage="rate(row)"
                  :stroke-width="12"
                  :color="rateColor(rate(row))"
                />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
      <el-col :xs="24" :md="10">
        <div class="page-card chart-card">
          <h3 class="page-title">今日考勤构成</h3>
          <div ref="pieRef" class="chart"></div>
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
const feeRef = ref()
const attRef = ref()
const pieRef = ref()
let feeChart = null
let attChart = null
let pieChart = null

const cards = computed(() => [
  {
    label: '本月已收（元）',
    value: Number(data.value.monthIncome || 0).toFixed(2),
    color: '#67c23a'
  },
  {
    label: '待收金额（元）',
    value: Number(data.value.unpaidAmount || 0).toFixed(2),
    color: '#f56c6c'
  },
  {
    label: '今日考勤记录',
    value: (data.value.todayTotal || 0) + ' 条',
    color: '#409eff'
  },
  {
    label: '食材预警',
    value: (data.value.stockWarn || 0) + ' 项',
    color: '#e6a23c'
  }
])

function rate(row) {
  const total = Number(row.total || 0)
  if (!total) return 0
  return Math.round((Number(row.present || 0) / total) * 100)
}

function rateColor(v) {
  if (v >= 95) return '#67c23a'
  if (v >= 85) return '#e6a23c'
  return '#f56c6c'
}

function renderFee() {
  const list = [...(data.value.feeTrend || [])].reverse()
  feeChart = echarts.init(feeRef.value)
  feeChart.setOption({
    tooltip: { trigger: 'axis', valueFormatter: (v) => '¥' + Number(v).toFixed(2) },
    legend: { data: ['实收金额', '应收金额'], right: 10 },
    grid: { left: 60, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: list.map((i) => i.month) },
    yAxis: { type: 'value', name: '元' },
    series: [
      {
        name: '实收金额',
        type: 'bar',
        barWidth: 26,
        data: list.map((i) => Number(i.paid || 0)),
        itemStyle: { color: '#67c23a', borderRadius: [4, 4, 0, 0] }
      },
      {
        name: '应收金额',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: list.map((i) => Number(i.total || 0)),
        itemStyle: { color: '#409eff' }
      }
    ]
  })
}

function renderAtt() {
  const list = data.value.attendanceByClazz || []
  attChart = echarts.init(attRef.value)
  attChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 60, right: 40, top: 20, bottom: 30 },
    xAxis: { type: 'value', max: 100, name: '%' },
    yAxis: { type: 'category', data: list.map((i) => i.clazzName) },
    series: [
      {
        type: 'bar',
        barWidth: 18,
        data: list.map((i) => rate(i)),
        itemStyle: { color: '#409eff', borderRadius: [0, 6, 6, 0] },
        label: { show: true, position: 'right', formatter: '{c}%' }
      }
    ]
  })
}

function renderPie() {
  const today = data.value.attendanceToday || []
  pieChart = echarts.init(pieRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}：{c} 人（{d}%）' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '45%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{c}人' },
        data: today.map((i) => ({ name: i.status, value: Number(i.num || 0) }))
      }
    ]
  })
}

function resize() {
  feeChart && feeChart.resize()
  attChart && attChart.resize()
  pieChart && pieChart.resize()
}

onMounted(async () => {
  const res = await statApi.report()
  data.value = res.data || {}
  await nextTick()
  renderFee()
  renderAtt()
  renderPie()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  feeChart && feeChart.dispose()
  attChart && attChart.dispose()
  pieChart && pieChart.dispose()
})
</script>

<style scoped>
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 18px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
}

.stat-card .label {
  color: #909399;
  font-size: 13px;
}

.stat-card .value {
  font-size: 24px;
  font-weight: 600;
  margin-top: 6px;
}

.chart-card {
  padding-bottom: 16px;
}

.chart {
  height: 300px;
}
</style>
