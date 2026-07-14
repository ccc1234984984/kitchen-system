const api = require('../../utils/api')

Page({
  data: {
    orderId: null,
    order: null,
    loading: true,
    statusText: '',
    statusIcon: '',
    statusDesc: '',
    statusMap: { 0: '待接单', 1: '已接单', 2: '制作中', 3: '已出餐', 4: '已完成' },
    iconMap: { 0: '⏳', 1: '👨‍🍳', 2: '🔥', 3: '✅', 4: '✅' },
    descMap: {
      0: '商家正在确认您的订单',
      1: '商家已接单，准备制作',
      2: '厨师正在精心制作中',
      3: '所有菜品已上齐，请慢用',
      4: '订单已完成'
    }
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ orderId: options.id })
      this.loadOrder()
    } else {
      this.setData({ loading: false })
    }
  },

  async loadOrder() {
    this.setData({ loading: true })
    wx.showLoading({ title: '加载中...' })

    try {
      const order = await api.getOrderDetail(this.data.orderId)
      if (order) {
        const status = order.paymentStatus === 0 ? -1 : order.status
        const items = (order.items || []).map(i => ({
          dishName: i.dishName,
          status: i.status,
          id: i.id
        }))
        order.items = items
        this.setData({
          order,
          loading: false,
          statusText: order.paymentStatus === 0 ? '待支付' : (this.data.statusMap[order.status] || '未知'),
          statusIcon: order.paymentStatus === 0 ? '💳' : (this.data.iconMap[order.status] || '❓'),
          statusDesc: order.paymentStatus === 0 ? '请完成支付后，商家开始制作' : (this.data.descMap[order.status] || '')
        })
      } else {
        this.setData({ loading: false })
      }
    } catch (e) {
      console.error('加载订单失败', e)
      wx.showToast({ title: '加载失败', icon: 'none' })
      this.setData({ loading: false })
    }

    wx.hideLoading()
  },

  async onPay() {
    wx.showLoading({ title: '支付中...' })
    api.payOrder(this.data.orderId).then(() => {
      wx.hideLoading()
      wx.showToast({ title: '支付成功', icon: 'success' })
      this.loadOrder()
    }).catch(() => {
      wx.hideLoading()
    })
  },

  onBack() {
    wx.navigateBack({
      fail: () => {
        wx.switchTab({ url: '/pages/mine/mine' })
      }
    })
  }
})
