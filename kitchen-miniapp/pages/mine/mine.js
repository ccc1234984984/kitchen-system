const api = require('../../utils/api')
const app = getApp()

Page({
  data: {
    orders: [],
    loading: false,
    pageNum: 1,
    pageSize: 10,
    total: 0,
    hasMore: true,
    statusMap: { 0: '待接单', 1: '已接单', 2: '制作中', 3: '已出餐', 4: '已完成' }
  },

  paying(item) {
    return item.paymentStatus === 0
  },

  getStatusText(item) {
    if (item.paymentStatus === 0) return '待支付'
    return this.data.statusMap[item.status] || '未知'
  },

  onShow() {
    const user = app.globalData.user
    if (!user) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.setData({ userPhone: user.name || user.phone, orders: [], pageNum: 1, hasMore: true })
    this.loadOrders(user.id, 1, true)
  },

  async loadOrders(userId, pageNum, reset) {
    this.setData({ loading: true })
    try {
      const res = await api.getUserOrdersPage(userId, pageNum, this.data.pageSize)
      const records = res.records || []
      const orders = reset ? records : this.data.orders.concat(records)
      this.setData({
        orders,
        total: res.total || 0,
        pageNum: pageNum,
        hasMore: pageNum * this.data.pageSize < (res.total || 0),
        loading: false
      })
    } catch (e) {
      console.error('加载订单失败', e)
      this.setData({ loading: false })
    }
  },

  onLoadMore() {
    const user = app.globalData.user
    if (user) {
      this.loadOrders(user.id, this.data.pageNum + 1, false)
    }
  },

  onOrderTap(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/order-detail/order-detail?id=' + id
    })
  },

  onPayTap(e) {
    const id = e.currentTarget.dataset.id
    wx.showLoading({ title: '支付中...' })
    api.payOrder(id).then(() => {
      wx.hideLoading()
      wx.showToast({ title: '支付成功', icon: 'success' })
      const user = app.globalData.user
      this.setData({ orders: [], pageNum: 1, hasMore: true })
      this.loadOrders(user.id, 1, true)
    }).catch(() => {
      wx.hideLoading()
    })
  },

  onEditName() {
    const user = app.globalData.user
    wx.showModal({
      title: '修改姓名',
      editable: true,
      placeholderText: '请输入姓名',
      content: user.name || '',
      success: (res) => {
        if (res.confirm && res.content && res.content.trim()) {
          const name = res.content.trim()
          wx.showLoading({ title: '保存中...' })
          api.updateUserName(user.id, name).then(updated => {
            app.globalData.user = updated
            wx.setStorageSync('user', updated)
            this.setData({ userPhone: updated.name })
            wx.hideLoading()
            wx.showToast({ title: '已修改', icon: 'success' })
          }).catch(() => {
            wx.hideLoading()
          })
        }
      }
    })
  }
})