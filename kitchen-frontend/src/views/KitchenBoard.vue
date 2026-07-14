<template>
  <div class="kitchen-board">
    <div class="header">
      <h2>🍳 后厨制作任务</h2>
      <el-button type="primary" @click="loadTasks" :loading="loading">刷新</el-button>
    </div>

    <div class="task-list" v-if="tasks.length > 0">
      <div
        v-for="(task, idx) in tasks"
        :key="task.id"
        class="task-card"
        :class="getStatusClass(task, idx)"
      >
        <div class="task-header">
          <div class="task-title">
            <span class="dish-name">{{ task.dishName }}</span>
            <span class="quantity">× {{ task.quantity }}</span>
          </div>
          <el-tag v-if="idx === 0 && task.status !== 2" type="warning" size="small" effect="dark">制作中</el-tag>
        </div>

        <div class="task-info">
          <span class="table-list">桌号: {{ task.tableNos.join(', ') }}</span>
          <span class="wait-time" :class="{ urgent: task.avgWaitMinutes > 15 }">
             平均等待 {{ task.avgWaitMinutes }} 分钟
          </span>
        </div>

        <div class="task-actions">
          <el-button
            :type="idx === 0 ? 'success' : 'default'"
            size="small"
            :class="{ 'btn-gray': idx > 0 }"
            @click="handleFinish(task.id)"
          >
            完成制作
          </el-button>
        </div>
      </div>
    </div>

    <el-empty v-else description="暂无制作任务"></el-empty>

    <Pagination
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      @change="handlePageChange"
      @size-change="handleSizeChange"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { generateTasks, getTaskList, finishTask } from '../api'
import Pagination from '../components/Pagination.vue'

const tasks = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
let timer = null

const loadTasks = async () => {
  loading.value = true
  try {
    await generateTasks()
    const data = await getTaskList(currentPage.value, pageSize.value)
    tasks.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    console.error('loadTasks 错误:', e)
  }
  loading.value = false
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadTasks()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadTasks()
}

const handleFinish = async (id) => {
  try {
    await finishTask(id)
    ElMessage.success('制作完成')
    loadTasks()
  } catch (e) {
    console.error(e)
    ElMessage.error('操作失败')
  }
}

const getStatusClass = (task, idx) => {
  if (task.status === 2) return 'status-done'
  if (idx === 0 && task.status === 1) return 'status-cooking'
  return 'status-pending'
}

onMounted(() => {
  loadTasks()
  timer = setInterval(loadTasks, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.kitchen-board {
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  color: #2c2c2c;
  font-size: 20px;
}

.task-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.task-card {
  background: white;
  border-radius: 10px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid #e8e3dc;
  border-left: 4px solid #409eff;
}

.task-card.status-pending {
  border-left-color: #b8b0a5;
  opacity: 0.85;
}

.task-card.status-cooking {
  border-left-color: #e6a23c;
  border-color: #e6a23c;
  box-shadow: 0 4px 16px rgba(230, 162, 60, 0.15);
}

.task-card.status-done {
  border-left-color: #67c23a;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.task-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dish-name {
  font-size: 18px;
  font-weight: bold;
  color: #2c2c2c;
}

.quantity {
  color: #8c8c8c;
  font-size: 14px;
}

.task-info {
  display: flex;
  gap: 20px;
  margin-bottom: 12px;
}

.table-list {
  color: #5a5a5a;
}

.wait-time {
  color: #5a5a5a;
}

.wait-time.urgent {
  color: #f56c6c;
  font-weight: bold;
}

.task-actions {
  display: flex;
  gap: 8px;
}

.btn-gray {
  background: #f0ebe4;
  border-color: #e0d5c8;
  color: #8c8c8c;
}

.btn-gray:hover {
  background: #e8e3dc;
  border-color: #d4c8b8;
  color: #5a5a5a;
}
</style>
