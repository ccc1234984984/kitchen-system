<template>
  <div class="tables-page">
    <h2 class="page-title">🪑 餐桌管理</h2>

    <div v-for="area in areas" :key="area" class="area-section">
      <div class="area-label">{{ area }}区</div>
      <div class="table-grid">
        <div
          v-for="table in getTablesByArea(area)"
          :key="table.id"
          class="table-card"
          :class="statusClass(table.status)"
          @click="showDetail(table)"
        >
          <div class="card-header">
            <span class="table-no">{{ table.tableNo }}</span>
            <button
              class="card-action"
              title="清台"
              @click.stop="confirmClear(table)"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
              </svg>
            </button>
          </div>
          <div class="card-body">
            <div v-if="table.status === 0" class="status-text idle">未开台</div>
            <div v-else-if="table.status === 1" class="status-content dining">
              <div class="dining-price">¥{{ table.totalPrice }}</div>
              <div class="dining-label">就餐中</div>
              <div class="dining-duration">{{ table.duration }}分钟</div>
            </div>
            <div v-else class="status-text cleanup">待清理</div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="detailVisible"
      :title="`${detailTable?.tableNo} - 订单详情`"
      width="500px"
      class="detail-dialog"
    >
      <div v-if="orderDetail" class="order-detail">
        <div class="detail-header">
          <div class="detail-row">
            <span class="label">订单编号</span>
            <span class="value">#{{ orderDetail.orderId }}</span>
          </div>
          <div class="detail-row">
            <span class="label">桌号</span>
            <span class="value">{{ orderDetail.tableNo }}</span>
          </div>
          <div class="detail-row">
            <span class="label">就餐时长</span>
            <span class="value">{{ orderDetail.duration }}分钟</span>
          </div>
          <div class="detail-row">
            <span class="label">订单状态</span>
            <span class="value">{{ statusText(orderDetail.status) }}</span>
          </div>
          <div v-if="orderDetail.userName" class="detail-row">
            <span class="label">顾客</span>
            <span class="value">{{ orderDetail.userName }}（{{ orderDetail.userPhone }}）</span>
          </div>
        </div>

          <div class="detail-items">
          <div class="items-title">菜品明细</div>
          <div v-for="(g, i) in groupedItems" :key="i" class="item-row">
            <span class="item-name">{{ g.dishName }} × {{ g.qty }}</span>
            <span class="item-status" :class="{ done: g.allDone, cooking: g.cooking }">
              {{ g.allDone ? '已完成' : g.cooking ? '制作中' : '待制作' }}
            </span>
          </div>
        </div>

        <div v-if="groupedItems.length > 0" class="detail-progress">
          出餐进度：{{ completedTypes }}/{{ groupedItems.length }}
        </div>

        <div class="detail-footer">
          <span class="label">合计（含桌位费）</span>
          <span class="total-price">¥{{ orderDetail.totalPrice }}</span>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTables, getTableOrder, clearTable, getTaskList } from '../api'

const areas = ['A', 'B', 'C']
const tables = ref([])
const detailVisible = ref(false)
const detailTable = ref(null)
const orderDetail = ref(null)

const getTablesByArea = (area) => {
  return tables.value.filter(t => t.area === area)
}

const statusClass = (status) => {
  if (status === 0) return 'status-idle'
  if (status === 1) return 'status-dining'
  return 'status-cleanup'
}

const statusText = (status) => {
  const map = { 0: '待接单', 1: '已接单', 2: '制作中', 3: '已出餐', 4: '已完成' }
  return map[status] || '未知'
}

const itemStatusText = (status) => {
  const map = { 0: '待制作', 1: '制作中', 2: '已完成' }
  return map[status] || '未知'
}

const cookingDishIds = ref([])

const groupedItems = computed(() => {
  if (!orderDetail.value?.items) return []
  const map = {}
  for (const item of orderDetail.value.items) {
    const key = item.dishId || item.dishName
    if (!map[key]) {
      map[key] = { dishName: item.dishName, qty: 0, allDone: true, dishId: item.dishId }
    }
    map[key].qty++
    if (item.status !== 2) map[key].allDone = false
  }
  return Object.values(map).map(g => ({
    ...g,
    cooking: cookingDishIds.value.includes(g.dishId)
  }))
})

const completedTypes = computed(() => {
  return groupedItems.value.filter(g => g.allDone).length
})

const showDetail = async (table) => {
  if (table.status !== 1) return
  detailTable.value = table
  try {
    const [res, taskData] = await Promise.all([
      getTableOrder(table.id),
      getTaskList(1, 50)
    ])
    orderDetail.value = res
    // 找出当前制作中的菜品ID
    const ids = new Set()
    if (taskData?.records) {
      for (const t of taskData.records) {
        if (t.status === 1) ids.add(t.dishId)
      }
    }
    cookingDishIds.value = [...ids]
    detailVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

const confirmClear = async (table) => {
  const labels = { 0: '空闲', 1: '就餐中', 2: '待清理' }
  const label = labels[table.status] || '未知'
  try {
    await ElMessageBox.confirm(
      `确定清台 ${table.tableNo}（${label}）？<br>清台后该餐桌将恢复为空闲状态。`,
      '确认清台',
      { dangerouslyUseHTMLString: true, confirmButtonText: '确认清台', cancelButtonText: '取消' }
    )
    await clearTable(table.id)
    ElMessage.success(`${table.tableNo} 已清台`)
    await loadTables()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const loadTables = async () => {
  try {
    const res = await getTables()
    tables.value = res || []
  } catch (e) {
    console.error(e)
  }
}

onMounted(loadTables)
</script>

<style scoped>
.tables-page {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.page-title {
  font-size: 20px;
  color: #2c2c2c;
  font-weight: 600;
  margin-bottom: 24px;
}

.area-section {
  margin-bottom: 28px;
}

.area-section:last-child {
  margin-bottom: 0;
}

.area-label {
  font-size: 14px;
  font-weight: 600;
  color: #8c8c8c;
  margin-bottom: 12px;
  padding-left: 4px;
}

.table-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(155px, 1fr));
  gap: 14px;
}

.table-card {
  border: 1px solid #e8e3dc;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}

.table-card:hover {
  box-shadow: 0 4px 16px rgba(200, 122, 79, 0.12);
  border-color: #d4c8b8;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #5a5a5a;
}

.table-no {
  font-size: 16px;
  font-weight: 700;
  color: #c87a4f;
}

.card-action {
  background: none;
  border: none;
  color: #b0b0b0;
  cursor: pointer;
  padding: 2px;
  display: flex;
  align-items: center;
  border-radius: 4px;
  transition: color 0.15s;
}

.card-action:hover {
  color: #e05a5a;
}

.card-body {
  padding: 20px 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100px;
}

/* 空闲 */
.status-idle .card-body {
  background: #fff;
}

.status-text {
  font-size: 15px;
  font-weight: 500;
}

.status-text.idle {
  color: #8c8c8c;
}

/* 就餐中 */
.status-dining .card-body {
  background: #f0f7ee;
}

.status-content.dining {
  text-align: center;
}

.dining-price {
  font-size: 20px;
  font-weight: 700;
  color: #c87a4f;
  margin-bottom: 4px;
}

.dining-label {
  font-size: 13px;
  color: #5a8a5a;
  font-weight: 500;
  margin-bottom: 2px;
}

.dining-duration {
  font-size: 12px;
  color: #8c8c8c;
}

/* 待清理 */
.status-cleanup .card-body {
  background: #fff8e1;
}

.status-text.cleanup {
  color: #b8860b;
}

/* ---- 弹窗 ---- */
.order-detail {
  font-size: 14px;
}

.detail-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0ebe4;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
}

.detail-row .label {
  color: #8c8c8c;
}

.detail-row .value {
  color: #2c2c2c;
  font-weight: 500;
}

.items-title {
  font-weight: 600;
  color: #2c2c2c;
  margin-bottom: 8px;
}

.item-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px dashed #f0ebe4;
}

.item-row:last-child {
  border-bottom: none;
}

.item-name {
  color: #2c2c2c;
}

.item-status {
  color: #8c8c8c;
  font-size: 13px;
}

.item-status.done {
  color: #5a8a5a;
  font-weight: 500;
}

.item-status.cooking {
  color: #e6a23c;
  font-weight: 500;
}

.detail-progress {
  text-align: center;
  padding: 8px 0;
  font-size: 13px;
  color: #8c8c8c;
  border-bottom: 1px solid #f0ebe4;
}

.detail-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 2px solid #f0ebe4;
}

.detail-footer .label {
  color: #2c2c2c;
  font-weight: 600;
}

.total-price {
  font-size: 22px;
  font-weight: 700;
  color: #c87a4f;
}
</style>
