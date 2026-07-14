App({
  globalData: {
    cart: [],
    user: null
  },

  // 添加商品到购物车
  addToCart(dish, quantity = 1) {
    const cart = this.globalData.cart
    const existing = cart.find(item => item.id === dish.id)
    
    if (existing) {
      existing.quantity += quantity
    } else {
      cart.push({
        ...dish,
        quantity: quantity
      })
    }
    
    this.globalData.cart = cart
    this.saveCart()
  },

  // 从购物车移除商品
  removeFromCart(dishId) {
    const cart = this.globalData.cart
    const index = cart.findIndex(item => item.id === dishId)
    
    if (index > -1) {
      cart.splice(index, 1)
      this.globalData.cart = cart
      this.saveCart()
    }
  },

  // 更新购物车商品数量
  updateCartQuantity(dishId, quantity) {
    const cart = this.globalData.cart
    const item = cart.find(item => item.id === dishId)
    
    if (item) {
      if (quantity <= 0) {
        this.removeFromCart(dishId)
      } else {
        item.quantity = quantity
        this.globalData.cart = cart
        this.saveCart()
      }
    }
  },

  // 清空购物车
  clearCart() {
    this.globalData.cart = []
    this.saveCart()
  },

  // 保存购物车到本地存储
  saveCart() {
    wx.setStorageSync('cart', this.globalData.cart)
  },

  // 从本地存储加载购物车
  loadCart() {
    const cart = wx.getStorageSync('cart')
    if (cart) {
      this.globalData.cart = cart
    }
  },

  // 获取购物车总数量
  getCartCount() {
    return this.globalData.cart.reduce((total, item) => total + item.quantity, 0)
  },

  // 获取购物车总价
  getCartTotal() {
    return this.globalData.cart.reduce((total, item) => total + (item.price * item.quantity), 0)
  },

  onLaunch() {
    this.loadCart()
    const user = wx.getStorageSync('user')
    if (user) {
      this.globalData.user = user
    }
  }
})