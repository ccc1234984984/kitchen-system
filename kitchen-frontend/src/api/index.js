import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: ''
})

api.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 401) {
      localStorage.removeItem('adminUser')
      window.location.href = '/login'
      return Promise.reject(new Error('未登录'))
    }
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  error => {
    const message = error.response?.data?.message || error.message || '网络错误'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export const getCategories = () => api.get('/api/categories')
export const getDishes = (categoryId) => api.get('/api/dishes', { params: { categoryId } })
export const createOrder = (data) => api.post('/api/orders', data)
export const getQueue = () => api.get('/api/orders/queue')
export const acceptOrder = (id) => api.post(`/api/orders/${id}/accept`)
export const startCooking = (id) => api.post(`/api/orders/${id}/start`)
export const finishDish = (itemId) => api.post(`/api/orders/items/${itemId}/finish`)
export const completeOrder = (id) => api.post(`/api/orders/${id}/complete`)
export const getOrderItems = (id) => api.get(`/api/orders/${id}/items`)
export const updatePriority = (id, score) => api.post(`/api/orders/${id}/priority`, { score })
export const generateTasks = () => api.post('/api/cooking-tasks/generate')
export const getTaskList = (pageNum = 1, pageSize = 12) =>
  api.get('/api/cooking-tasks', { params: { pageNum, pageSize } })
export const finishTask = (id) => api.post(`/api/cooking-tasks/${id}/finish`)
export const getTables = () => api.get('/api/tables')
export const getTableOrder = (tableId) => api.get(`/api/tables/${tableId}/order`)
export const clearTable = (tableId) => api.post(`/api/tables/${tableId}/clear`)
export const adminLogin = (data) => api.post('/api/admin/login', data)
export const adminLogout = () => api.post('/api/admin/logout')
export const adminMe = () => api.get('/api/admin/me')
export const changePassword = (data) => api.post('/api/admin/change-password', data)
export const getDishesAdmin = (pageNum = 1, pageSize = 20) => api.get('/api/dishes/admin', { params: { pageNum, pageSize } })
export const createDish = (data) => api.post('/api/dishes', data)
export const updateDish = (id, data) => api.put(`/api/dishes/${id}`, data)
export const deleteDish = (id) => api.delete(`/api/dishes/${id}`)
export const toggleDishStatus = (id) => api.put(`/api/dishes/${id}/status`)
export const uploadImage = (file) => {
  const form = new FormData()
  form.append('file', file)
  return api.post('/api/oss/upload', form, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export const generateDishImage = (name) => api.post('/api/ai/generate-dish-image', { name })
