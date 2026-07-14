<template>
  <div class="order-page">
    <div class="order-main">
      <h2 class="page-title">📝 点餐</h2>

      <el-form :model="form" class="order-form">
        <div class="form-row">
          <label class="form-label">桌号</label>
          <el-select v-model="form.tableId" placeholder="请选择空闲餐桌" class="table-select" @change="onTableChange">
            <el-option
              v-for="t in availableTables"
              :key="t.id"
              :label="`${t.tableNo}（${t.type}）`"
              :value="t.id"
            ></el-option>
          </el-select>
        </div>

        <div class="section-label">选择菜品</div>
        <el-tabs v-model="activeCategory" class="category-tabs">
          <el-tab-pane
            v-for="cat in categories"
            :key="cat.id"
            :label="cat.name"
            :name="cat.id"
          >
            <div class="dish-grid">
              <div
                v-for="dish in getDishesByCategory(cat.id)"
                :key="dish.id"
                class="dish-card"
              >
                <img v-if="dish.imageUrl" :src="dish.imageUrl" class="dish-img" alt="">
                <div class="dish-info">
                  <div class="dish-name">{{ dish.name }}</div>
                  <div class="dish-price">¥{{ dish.price }}</div>
                </div>
                <button
                  v-if="!selectedDishes[dish.id]"
                  class="add-btn"
                  @click="incrementDish(dish.id)"
                >+ 添加</button>
                <span v-else class="added-tag">✔ 已添加</span>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-form>
    </div>

    <div class="cart-sidebar">
      <div class="cart-header">
        <h3>点餐清单</h3>
        <span v-if="totalCount > 0" class="cart-badge">{{ totalCount }}</span>
      </div>

      <div v-if="totalCount > 0" class="cart-items">
        <div v-for="(qty, dishId) in selectedDishes" :key="dishId" class="cart-item">
          <div class="cart-item-info">
            <div class="cart-item-name">{{ getDishName(Number(dishId)) }}</div>
            <div class="cart-item-price">¥{{ getDishSubtotal(Number(dishId), qty) }}</div>
          </div>
          <div class="cart-qty">
            <button class="qty-btn sm" @click="decrementDish(Number(dishId))">−</button>
            <span class="qty-num">{{ qty }}</span>
            <button class="qty-btn sm" @click="incrementDish(Number(dishId))">+</button>
          </div>
        </div>
      </div>

      <div v-else class="cart-empty">
        <div class="empty-icon">🛒</div>
        <p>请选择菜品</p>
      </div>

      <div v-if="totalCount > 0" class="cart-footer">
        <div v-if="baseCharge > 0" class="charge-row">
          <span>桌位费</span>
          <span class="charge-val">¥{{ baseCharge }}</span>
        </div>
        <div class="cart-total-row">
          <span>合计</span>
          <span class="total-price">¥{{ totalPrice }}</span>
        </div>
        <button class="checkout-btn" @click="submitOrder">去结算</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCategories, getDishes, createOrder, getTables } from '../api'

const router = useRouter()

const form = ref({ tableNo: '', tableId: null })
const categories = ref([])
const dishes = ref([])
const activeCategory = ref(null)
const selectedDishes = ref({})

const getDishesByCategory = (categoryId) => {
  return dishes.value.filter(d => d.categoryId === categoryId)
}

const getDishName = (dishId) => {
  const dish = dishes.value.find(d => d.id === dishId)
  return dish ? dish.name : ''
}

const getDishPrice = (dishId) => {
  const dish = dishes.value.find(d => d.id === dishId)
  return dish ? Number(dish.price) : 0
}

const getDishSubtotal = (dishId, qty) => {
  return (getDishPrice(dishId) * qty).toFixed(2)
}

const allTables = ref([])
const selectedTable = ref(null)

const availableTables = computed(() => {
  return allTables.value.filter(t => t.status === 0)
})

const getTableBaseCharge = (area) => {
  if (area === 'B') return 20
  if (area === 'C') return 50
  return 0
}

const onTableChange = (tableId) => {
  const table = allTables.value.find(t => t.id === tableId)
  selectedTable.value = table || null
  form.value.tableNo = table ? table.tableNo : ''
}

const incrementDish = (dishId) => {
  if (!selectedDishes.value[dishId]) {
    selectedDishes.value[dishId] = 1
  } else {
    selectedDishes.value[dishId]++
  }
}

const decrementDish = (dishId) => {
  if (selectedDishes.value[dishId] > 1) {
    selectedDishes.value[dishId]--
  } else {
    delete selectedDishes.value[dishId]
  }
}

const baseCharge = computed(() => {
  return selectedTable.value ? getTableBaseCharge(selectedTable.value.area) : 0
})

const totalPrice = computed(() => {
  const dishTotal = Object.entries(selectedDishes.value)
    .reduce((sum, [dishId, qty]) => sum + getDishPrice(Number(dishId)) * qty, 0)
  return (dishTotal + baseCharge.value).toFixed(2)
})

const totalCount = computed(() => {
  return Object.values(selectedDishes.value).reduce((sum, qty) => sum + qty, 0)
})

const canSubmit = computed(() => {
  return form.value.tableId && form.value.tableNo && totalCount.value > 0
})

const submitOrder = async () => {
  try {
    const dishIds = []
    for (const [dishId, qty] of Object.entries(selectedDishes.value)) {
      for (let i = 0; i < qty; i++) {
        dishIds.push(Number(dishId))
      }
    }
    await createOrder({
      tableId: form.value.tableId,
      tableNo: form.value.tableNo,
      dishIds
    })
    ElMessage.success('订单创建成功')
    form.value.tableNo = ''
    form.value.tableId = null
    selectedTable.value = null
    selectedDishes.value = {}
    allTables.value = await getTables()
    router.push('/kitchen')
  } catch (e) {
    console.error('创建订单失败:', e)
  }
}

const loadData = async () => {
  try {
    const [catData, dishData, tableData] = await Promise.all([getCategories(), getDishes(), getTables()])
    categories.value = catData || []
    dishes.value = dishData || []
    allTables.value = tableData || []
    if (categories.value.length > 0) {
      activeCategory.value = categories.value[0].id
    }
  } catch (e) {
    console.error(e)
  }
}

onMounted(loadData)
</script>

<style scoped>
.order-page {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.order-main {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.page-title {
  font-size: 20px;
  color: #2c2c2c;
  margin-bottom: 20px;
  font-weight: 600;
}

.form-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.form-label {
  font-size: 14px;
  color: #5a5a5a;
  font-weight: 500;
  white-space: nowrap;
}

.table-select {
  width: 240px;
}

.table-select :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.table-select :deep(.el-input__inner) {
  font-size: 14px;
}

.section-label {
  font-size: 15px;
  font-weight: 600;
  color: #2c2c2c;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid #f0ebe4;
}

.category-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  color: #5a5a5a;
  padding: 0 18px;
}

.category-tabs :deep(.el-tabs__item.is-active) {
  color: #c87a4f;
  font-weight: 600;
}

.category-tabs :deep(.el-tabs__active-bar) {
  background-color: #c87a4f;
}

.dish-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 14px;
  padding: 6px 0;
}

.dish-card {
  background: #fff;
  border: 1px solid #f0ebe4;
  border-radius: 10px;
  padding: 0 0 16px;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow: hidden;
}

.dish-card:hover {
  box-shadow: 0 4px 16px rgba(200, 122, 79, 0.1);
  border-color: #e0d5c8;
}

.dish-img {
  width: 100%;
  height: 140px;
  object-fit: cover;
  display: block;
  background: #f5f0ea;
}

.dish-info {
  text-align: center;
  padding: 0 12px;
}

.dish-img {
  width: 100%;
  height: 140px;
  object-fit: cover;
  display: block;
  background: #f5f0ea;
}

.dish-info {
  padding: 0 16px;
}


.dish-card:hover {
  box-shadow: 0 4px 16px rgba(200, 122, 79, 0.1);
  border-color: #e0d5c8;
}

.dish-info {
  text-align: center;
}

.dish-name {
  font-size: 15px;
  font-weight: 600;
  color: #2c2c2c;
  margin-bottom: 6px;
}

.dish-price {
  font-size: 16px;
  font-weight: 700;
  color: #d4814a;
}

.added-tag {
  text-align: center;
  padding: 8px 0;
  border-radius: 20px;
  background: #f0ebe4;
  color: #8c8c8c;
  font-size: 13px;
  font-weight: 600;
  cursor: default;
}

.qty-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #c87a4f;
  background: #fff;
  color: #c87a4f;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  transition: all 0.15s;
}

.qty-btn:hover {
  background: #c87a4f;
  color: #fff;
}

.qty-btn.sm {
  width: 26px;
  height: 26px;
  font-size: 14px;
}

.qty-num {
  font-size: 18px;
  font-weight: 700;
  color: #2c2c2c;
  min-width: 24px;
  text-align: center;
}

.add-btn {
  padding: 8px 0;
  border: none;
  border-radius: 20px;
  background: #c87a4f;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.add-btn:hover {
  background: #b0673a;
}

/* ---- Cart Sidebar ---- */
.cart-sidebar {
  width: 320px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  min-height: 400px;
  position: sticky;
  top: 20px;
}

.cart-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 20px 16px;
  border-bottom: 1px solid #f0ebe4;
}

.cart-header h3 {
  font-size: 16px;
  color: #2c2c2c;
  font-weight: 600;
}

.cart-badge {
  background: #c87a4f;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cart-items {
  flex: 1;
  padding: 12px 20px;
  overflow-y: auto;
}

.cart-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px dashed #f0ebe4;
}

.cart-item:last-child {
  border-bottom: none;
}

.cart-item-info {
  flex: 1;
  min-width: 0;
}

.cart-item-name {
  font-size: 14px;
  color: #2c2c2c;
  font-weight: 500;
  margin-bottom: 2px;
}

.cart-item-price {
  font-size: 13px;
  font-weight: 600;
  color: #d4814a;
}

.cart-qty {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 12px;
}

.cart-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #b8b0a5;
  padding: 40px;
}

.empty-icon {
  font-size: 40px;
  margin-bottom: 8px;
}

.cart-footer {
  padding: 16px 20px 20px;
  border-top: 1px solid #f0ebe4;
}

.cart-total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  font-size: 14px;
  color: #5a5a5a;
}

.total-price {
  font-size: 22px;
  font-weight: 700;
  color: #c87a4f;
}

.checkout-btn {
  width: 100%;
  padding: 12px 0;
  border: none;
  border-radius: 8px;
  background: #c87a4f;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.checkout-btn:hover {
  background: #b0673a;
}

.checkout-btn:disabled {
  background: #d9d1c8;
  cursor: not-allowed;
}

.charge-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 6px;
  padding-bottom: 6px;
  border-bottom: 1px dashed #f0ebe4;
}

.charge-val {
  color: #c87a4f;
  font-weight: 600;
}
</style>
