const BASE_URL = 'http://localhost:8080'

const request = (url, method = 'GET', data = {}) => {
  // 过滤掉 undefined 的参数
  const params = {}
  for (const k in data) {
    if (data[k] !== undefined && data[k] !== null) {
      params[k] = data[k]
    }
  }
  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE_URL + url,
      method,
      data: params,
      header: {
        'content-type': 'application/json',
        'X-App-Source': 'miniapp'
      },
      success: (res) => {
        const body = res.data
        console.log('[API]', method, url, res.statusCode, body)
        if (body.code === 200) {
          resolve(body.data)
        } else {
          const msg = body.message || '请求失败'
          wx.showToast({ title: '[' + url + '] ' + msg, icon: 'none' })
          reject(new Error(msg))
        }
      },
      fail: (err) => {
        wx.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}

module.exports = {
  login: (phone, name) => request('/api/user/login', 'POST', name ? { phone, name } : { phone }),
  updateUserName: (userId, name) => request('/api/user/name', 'PUT', { userId, name }),
  // 菜品相关
  getCategories: () => request('/api/categories'),
  getDishes: (categoryId) => {
    const params = categoryId ? { categoryId } : {}
    return request('/api/dishes', 'GET', params)
  },
  getDishDetail: (id) => request(`/api/dishes/${id}`),

  // 订单相关
  createOrder: (data) => request('/api/orders', 'POST', data),
  getUserOrders: (userId) => request('/api/orders/list?userId=' + userId),
  getUserOrdersPage: (userId, pageNum, pageSize) => request('/api/orders/page?userId=' + userId + '&pageNum=' + pageNum + '&pageSize=' + pageSize),
  getOrderDetail: (id) => request(`/api/orders/${id}`),
  cancelOrder: (id) => request(`/api/orders/${id}/cancel`, 'POST'),
  confirmReceive: (id) => request(`/api/orders/${id}/confirm`, 'POST'),

  // 餐桌相关
  getTables: () => request('/api/tables'),
  getTableOrder: (tableId) => request(`/api/tables/${tableId}/order`),

  // 支付相关
  payOrder: (id) => request(`/api/orders/${id}/pay`, 'POST'),

  // 用户相关
  getUserInfo: () => request('/api/user/info'),
  updateUserInfo: (data) => request('/api/user/info', 'PUT', data),

  // 通用
  getSystemConfig: () => request('/api/config')
}