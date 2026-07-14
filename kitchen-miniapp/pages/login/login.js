const api = require('../../utils/api')
const app = getApp()

Page({
  data: {
    phone: '',
    loading: false
  },

  onLoad() {
    const user = wx.getStorageSync('user')
    if (user && user.name) {
      app.globalData.user = user
      wx.reLaunch({ url: '/pages/menu/menu' })
    }
  },

  onPhoneInput(e) {
    this.setData({ phone: e.detail.value })
  },

  onLogin() {
    const phone = this.data.phone
    if (!/^\d{11}$/.test(phone)) {
      wx.showToast({ title: '请输入11位手机号', icon: 'none' })
      return
    }

    this.setData({ loading: true })
    api.login(phone).then(user => {
      app.globalData.user = user
      wx.setStorageSync('user', user)
      wx.reLaunch({ url: '/pages/menu/menu' })
    }).catch(() => {
      this.setData({ loading: false })
    })
  }
})