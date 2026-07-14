<template>
  <div class="dish-manage">
    <div class="page-header">
      <h2 class="page-title">菜品管理</h2>
      <el-button type="primary" class="add-btn" @click="openAddDialog">添加菜品</el-button>
    </div>

    <el-table :data="dishes" stripe style="width: 100%" v-loading="loading">
      <el-table-column label="图片" width="100">
        <template #default="{ row }">
          <img v-if="row.imageUrl" :src="row.imageUrl" class="table-img">
          <span v-else class="no-img">无图</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="菜品名称" width="150" />
      <el-table-column label="分类" width="100">
        <template #default="{ row }">{{ getCategoryName(row.categoryId) }}</template>
      </el-table-column>
      <el-table-column prop="price" label="价格" width="100">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="estimatedTime" label="制作时长(分)" width="130" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="220">
        <template #default="{ row }">
          <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button size="small" @click="handleToggleStatus(row)">
            {{ row.status === 1 ? '下架' : '上架' }}
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      @change="handlePageChange"
      @size-change="handleSizeChange"
    />

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑菜品' : '添加菜品'" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" label-width="100px" :rules="rules">
        <el-form-item label="菜品名称" prop="name">
          <el-input v-model="form.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="制作时长(分)" prop="estimatedTime">
          <el-input-number v-model="form.estimatedTime" :min="1" :max="999" style="width:100%" />
        </el-form-item>
        <el-form-item label="菜品图片">
          <div class="upload-area">
            <img v-if="form.imageUrl" :src="form.imageUrl" class="upload-preview">
            <div class="upload-actions">
              <div class="upload-btns">
                <el-button @click="triggerUpload" :loading="uploading" :disabled="generating">选择图片</el-button>
                <el-button type="primary" plain @click="handleAiGenerate" :loading="generating" :disabled="uploading">
                  AI 生成
                </el-button>
              </div>
              <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="handleFileChange">
              <span class="upload-hint">AI 生成会根据菜品名称自动作图，约需 10~30 秒</span>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDishesAdmin, createDish, updateDish, deleteDish, toggleDishStatus, getCategories, uploadImage, generateDishImage } from '../api'
import Pagination from '../components/Pagination.vue'

const loading = ref(false)
const dishes = ref([])
const categories = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const uploading = ref(false)
const generating = ref(false)
const formRef = ref(null)
const fileInput = ref(null)

const initForm = () => ({
  id: null,
  name: '',
  categoryId: null,
  price: 0,
  estimatedTime: 10,
  imageUrl: ''
})

const form = reactive(initForm())

const rules = {
  name: [{ required: true, message: '请输入菜品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  estimatedTime: [{ required: true, message: '请输入制作时长', trigger: 'blur' }]
}

const getCategoryName = (id) => {
  const c = categories.value.find(c => c.id === id)
  return c ? c.name : ''
}

const loadData = async () => {
  loading.value = true
  try {
    const [data, c] = await Promise.all([getDishesAdmin(currentPage.value, pageSize.value), getCategories()])
    dishes.value = data.records || []
    total.value = data.total || 0
    categories.value = c || []
  } catch (e) {
    console.error(e)
  }
  loading.value = false
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadData()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadData()
}

const resetForm = () => {
  Object.assign(form, initForm())
  isEdit.value = false
}

const openAddDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  form.id = row.id
  form.name = row.name
  form.categoryId = row.categoryId
  form.price = row.price
  form.estimatedTime = row.estimatedTime
  form.imageUrl = row.imageUrl || ''
  dialogVisible.value = true
}

const triggerUpload = () => {
  fileInput.value?.click()
}

const handleFileChange = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  uploading.value = true
  try {
    const url = await uploadImage(file)
    form.imageUrl = url
    ElMessage.success('图片上传成功')
  } catch (err) {
    console.error(err)
  }
  uploading.value = false
  fileInput.value.value = ''
}

const handleAiGenerate = async () => {
  const name = (form.name || '').trim()
  if (!name) {
    ElMessage.warning('请先填写菜品名称')
    return
  }
  generating.value = true
  try {
    const url = await generateDishImage(name)
    form.imageUrl = url
    ElMessage.success('AI 图片生成成功')
  } catch (err) {
    console.error(err)
  }
  generating.value = false
}

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) {
      await updateDish(form.id, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createDish({ ...form })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await loadData()
  } catch (e) {
    console.error(e)
  }
  saving.value = false
}

const handleToggleStatus = async (row) => {
  try {
    await toggleDishStatus(row.id)
    ElMessage.success(row.status === 1 ? '已下架' : '已上架')
    await loadData()
  } catch (e) {
    console.error(e)
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '删除确认')
    await deleteDish(row.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

onMounted(loadData)
</script>

<style scoped>
.dish-manage {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  color: #2c2c2c;
  font-weight: 600;
}

.add-btn {
  background: #c87a4f;
  border-color: #c87a4f;
}

.add-btn:hover {
  background: #b0673a;
  border-color: #b0673a;
}

.table-img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
  display: block;
  background: #f5f0ea;
}

.no-img {
  color: #b8b0a5;
  font-size: 12px;
}

.upload-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.upload-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.upload-btns {
  display: flex;
  gap: 8px;
}

.upload-preview {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #f0ebe4;
  background: #f5f0ea;
}

.upload-hint {
  color: #b8b0a5;
  font-size: 12px;
}
</style>
