<template>
  <div class="page-card">
    <h3 class="page-title">系统设置与日志</h3>

    <el-tabs v-model="tab">
      <!-- 系统信息 -->
      <el-tab-pane label="系统信息" name="info">
        <el-descriptions :column="2" border style="margin-top: 8px">
          <el-descriptions-item label="数据库版本">{{ info.dbVersion }}</el-descriptions-item>
          <el-descriptions-item label="数据表数量">{{ info.tableCount }} 张</el-descriptions-item>
          <el-descriptions-item label="Java 版本">{{ info.javaVersion }}</el-descriptions-item>
          <el-descriptions-item label="操作系统">{{ info.osName }}</el-descriptions-item>
          <el-descriptions-item label="JVM 已用内存">{{ info.jvmUsedMb }} MB</el-descriptions-item>
          <el-descriptions-item label="JVM 最大内存">{{ info.jvmMaxMb }} MB</el-descriptions-item>
          <el-descriptions-item label="备份文件数">{{ info.backupCount }} 个</el-descriptions-item>
          <el-descriptions-item label="运行框架">Spring Boot 3.5 + MyBatis-Plus 3.5</el-descriptions-item>
        </el-descriptions>

        <h4 class="sub-title">数据表概览</h4>
        <el-table :data="info.tables || []" size="small" border stripe>
          <el-table-column type="index" label="#" width="55" />
          <el-table-column prop="tableName" label="表名" />
          <el-table-column prop="tableRows" label="数据行数（估算）" width="160" />
        </el-table>
      </el-tab-pane>

      <!-- 数据备份 -->
      <el-tab-pane label="数据备份" name="backup">
        <div class="table-toolbar">
          <el-button type="primary" :icon="FolderAdd" :loading="backing" @click="doBackup">
            立即备份
          </el-button>
          <span class="tip">备份文件保存在后端运行目录的 backups/ 下，格式为 JSON</span>
        </div>
        <el-table :data="backups" size="small" border stripe>
          <el-table-column prop="fileName" label="文件名" />
          <el-table-column prop="sizeKb" label="大小(KB)" width="120" />
          <el-table-column prop="createTime" label="备份时间" width="190" />
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button link type="danger" @click="removeBackup(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 操作日志 -->
      <el-tab-pane label="操作日志" name="log">
        <el-form :inline="true" style="margin-bottom: 8px">
          <el-form-item label="关键字">
            <el-input
              v-model="logQuery.keyword"
              placeholder="操作描述/操作人"
              clearable
              style="width: 200px"
              @keyup.enter="loadLog"
            />
          </el-form-item>
          <el-form-item label="请求方式">
            <el-select v-model="logQuery.method" clearable placeholder="全部" style="width: 130px">
              <el-option label="POST" value="POST" />
              <el-option label="PUT" value="PUT" />
              <el-option label="DELETE" value="DELETE" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="loadLog">查询</el-button>
            <el-button type="danger" :icon="Delete" @click="clearLog">清空日志</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="logLoading" :data="logs" border stripe size="small">
          <el-table-column type="index" label="#" width="52" />
          <el-table-column prop="username" label="操作人" width="100" />
          <el-table-column prop="operation" label="操作描述" min-width="180" />
          <el-table-column prop="method" label="方式" width="90" />
          <el-table-column prop="uri" label="请求地址" min-width="190" show-overflow-tooltip />
          <el-table-column prop="ip" label="IP" width="130" />
          <el-table-column prop="costTime" label="耗时(ms)" width="100" />
          <el-table-column prop="status" label="状态码" width="90" />
          <el-table-column prop="createTime" label="操作时间" width="170" />
        </el-table>

        <div class="pager">
          <el-pagination
            v-model:current-page="logQuery.pageNum"
            v-model:page-size="logQuery.pageSize"
            :total="logTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @size-change="loadLog"
            @current-change="loadLog"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, FolderAdd, Search } from '@element-plus/icons-vue'
import { cleanLog, logApi, systemApi } from '@/api'

const tab = ref('info')
const info = ref({})
const backups = ref([])
const backing = ref(false)

const logs = ref([])
const logTotal = ref(0)
const logLoading = ref(false)
const logQuery = reactive({ pageNum: 1, pageSize: 10, keyword: '', method: '' })

async function loadInfo() {
  const res = await systemApi.info()
  info.value = res.data || {}
}

async function loadBackups() {
  const res = await systemApi.backupList()
  backups.value = res.data || []
}

async function doBackup() {
  backing.value = true
  try {
    const res = await systemApi.backup()
    ElMessage.success(`备份成功：${res.data.fileName}（${res.data.tableCount} 张表）`)
    loadBackups()
    loadInfo()
  } finally {
    backing.value = false
  }
}

function removeBackup(row) {
  ElMessageBox.confirm(`确定删除备份文件「${row.fileName}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await systemApi.removeBackup(row.fileName)
      ElMessage.success('已删除')
      loadBackups()
      loadInfo()
    })
    .catch(() => {})
}

async function loadLog() {
  logLoading.value = true
  try {
    const params = { ...logQuery }
    Object.keys(params).forEach((k) => {
      if (params[k] === '') delete params[k]
    })
    const res = await logApi.page(params)
    logs.value = res.data || []
    logTotal.value = res.total || 0
  } finally {
    logLoading.value = false
  }
}

function clearLog() {
  ElMessageBox.confirm('确定清空全部操作日志吗？该操作不可恢复。', '警告', { type: 'warning' })
    .then(async () => {
      await cleanLog()
      ElMessage.success('日志已清空')
      loadLog()
    })
    .catch(() => {})
}

onMounted(() => {
  loadInfo()
  loadBackups()
  loadLog()
})
</script>

<style scoped>
.sub-title {
  margin: 20px 0 10px;
  font-size: 14px;
  color: #303133;
}

.tip {
  color: #909399;
  font-size: 13px;
}
</style>
