const api = require('../../utils/api')
const app = getApp()

Page({
  data: {
    categories: [],
    dishes: [],
    filteredDishes: [],
    activeCategory: null,
    searchKeyword: '',
    loading: true,
    cartCount: 0,
    cartTotal: '0.00',
    cartItems: [],
    showCart: false,
    tables: [],
    tableOptions: [],
    selectedTableIndex: -1,
    selectedTable: null,
    submitting: false
  },

  onLoad() {
    this.loadData()
  },

  onShow() {
    const user = app.globalData.user
    if (!user) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.updateCartInfo()
  },

  onPullDownRefresh() {
    this.loadData().then(() => {
      wx.stopPullDownRefresh()
    })
  },

  async loadData() {
    this.setData({ loading: true })
    wx.showLoading({ title: '加载中...' })

    try {
      const [categories, dishes, tables] = await Promise.all([
        api.getCategories(),
        api.getDishes(),
        api.getTables().catch(() => [])
      ])

      const dishesWithQuantity = (dishes || []).map(dish => ({
        ...dish,
        quantity: this.getCartQuantity(dish.id)
      }))

      const tableOptions = (tables || []).filter(t => t.status === 0).map(t => ({
        label: `${t.tableNo}（${t.area}区）`,
        value: t.id,
        area: t.area,
        tableNo: t.tableNo
      }))

      this.setData({
        categories: categories || [],
        dishes: dishesWithQuantity,
        tables: tables || [],
        tableOptions,
        activeCategory: categories && categories[0] ? categories[0].id : null,
        filteredDishes: this.filterDishes(dishesWithQuantity, categories && categories[0] ? categories[0].id : null, ''),
        loading: false
      })
    } catch (e) {
      console.error('加载失败', e)
      wx.showToast({ title: '加载失败', icon: 'none' })
    }

    wx.hideLoading()
  },

  getCartQuantity(dishId) {
    const cartItem = app.globalData.cart.find(item => item.id === dishId)
    return cartItem ? cartItem.quantity : 0
  },

  filterDishes(dishes, categoryId, keyword) {
    let filtered = dishes
    if (categoryId) {
      filtered = filtered.filter(dish => dish.categoryId === categoryId)
    }
    if (keyword) {
      const lowerKeyword = keyword.toLowerCase()
      filtered = filtered.filter(dish =>
        dish.name.toLowerCase().includes(lowerKeyword) ||
        (dish.description && dish.description.toLowerCase().includes(lowerKeyword))
      )
    }
    return filtered
  },

  onCategoryTap(e) {
    const categoryId = Number(e.currentTarget.dataset.id)
    this.setData({
      activeCategory: categoryId,
      filteredDishes: this.filterDishes(this.data.dishes, categoryId, this.data.searchKeyword)
    })
  },

  onSearchInput(e) {
    const keyword = e.detail.value
    this.setData({
      searchKeyword: keyword,
      filteredDishes: this.filterDishes(this.data.dishes, this.data.activeCategory, keyword)
    })
  },

  onDishTap(e) {
    const dishId = Number(e.currentTarget.dataset.id)
    const dish = this.data.dishes.find(d => d.id === dishId)
    if (dish) {
      wx.showModal({
        title: dish.name,
        content: `价格：¥${dish.price}\n预计制作时间：${dish.estimatedTime || 10}分钟`,
        showCancel: false,
        confirmText: '知道了'
      })
    }
  },

  onPlusTap(e) {
    const dishId = Number(e.currentTarget.dataset.id)
    const dish = this.data.dishes.find(d => d.id === dishId)
    if (dish) {
      app.addToCart(dish, 1)
      this.updateDishQuantity(dishId, 1)
      this.updateCartInfo()
    }
  },

  onMinusTap(e) {
    const dishId = Number(e.currentTarget.dataset.id)
    const dish = this.data.dishes.find(d => d.id === dishId)
    if (dish) {
      const currentQuantity = this.getCartQuantity(dishId)
      if (currentQuantity > 0) {
        app.updateCartQuantity(dishId, currentQuantity - 1)
        this.updateDishQuantity(dishId, -1)
        this.updateCartInfo()
      }
    }
  },

  updateDishQuantity(dishId, change) {
    const dishes = this.data.dishes.map(dish => {
      if (dish.id === dishId) {
        return { ...dish, quantity: Math.max(0, dish.quantity + change) }
      }
      return dish
    })
    const filteredDishes = this.data.filteredDishes.map(dish => {
      if (dish.id === dishId) {
        return { ...dish, quantity: Math.max(0, dish.quantity + change) }
      }
      return dish
    })
    this.setData({ dishes, filteredDishes })
  },

  updateCartInfo() {
    const cartCount = app.getCartCount()
    const cartTotal = app.getCartTotal()
    const cart = app.globalData.cart || []
    this.setData({
      cartCount,
      cartTotal: cartTotal.toFixed(2),
      cartItems: cart.map(item => ({ ...item }))
    })
  },

  async loadTables() {
    try {
      const tables = await api.getTables()
      const tableOptions = (tables || []).filter(t => t.status === 0).map(t => ({
        label: `${t.tableNo}（${t.area}区）`,
        value: t.id,
        area: t.area,
        tableNo: t.tableNo
      }))
      this.setData({ tables: tables || [], tableOptions })
    } catch (e) {
      console.error('加载餐桌失败', e)
    }
  },

  showCartPopup() {
    this.loadTables()
    this.updateCartInfo()
    this.setData({ showCart: true, selectedTable: null, selectedTableIndex: -1 })
  },

  hideCartPopup() {
    this.setData({ showCart: false })
  },

  onClearCart() {
    wx.showModal({
      title: '提示',
      content: '确定要清空购物车吗？',
      success: (res) => {
        if (res.confirm) {
          app.clearCart()
          this.updateCartInfo()
          this.setData({ showCart: false })
          this.resetDishQuantities()
          wx.showToast({ title: '已清空', icon: 'success' })
        }
      }
    })
  },

  resetDishQuantities() {
    const dishes = this.data.dishes.map(d => ({ ...d, quantity: 0 }))
    const filteredDishes = this.data.filteredDishes.map(d => ({ ...d, quantity: 0 }))
    this.setData({ dishes, filteredDishes })
  },

  onTableChange(e) {
    const index = Number(e.detail.value)
    const table = this.data.tableOptions[index]
    if (table) {
      this.setData({ selectedTableIndex: index, selectedTable: table })
    }
  },

  async onSubmitOrder() {
    if (this.data.cartItems.length === 0) {
      wx.showToast({ title: '购物车是空的', icon: 'none' })
      return
    }

    if (this.data.tableOptions.length > 0 && !this.data.selectedTable) {
      wx.showToast({ title: '请选择餐桌', icon: 'none' })
      return
    }

    this.setData({ submitting: true })
    wx.showLoading({ title: '提交中...' })

    try {
      const dishIds = []
      this.data.cartItems.forEach(item => {
        for (let i = 0; i < item.quantity; i++) {
          dishIds.push(item.id)
        }
      })

      const table = this.data.selectedTable
      const user = app.globalData.user
      const orderData = {
        tableId: table ? table.value : 0,
        tableNo: table ? table.tableNo : 'A01',
        dishIds,
        userId: user ? user.id : null
      }

      await api.createOrder(orderData)
      app.clearCart()
      this.setData({ showCart: false, cartCount: 0, cartTotal: '0.00', cartItems: [], selectedTable: null, selectedTableIndex: -1 })
      this.resetDishQuantities()
      this.loadTables()

      wx.hideLoading()
      wx.showToast({ title: '下单成功', icon: 'success' })
    } catch (e) {
      wx.hideLoading()
      console.error('下单失败', e)
      wx.showToast({ title: '下单失败', icon: 'none' })
    }

    this.setData({ submitting: false })
  }
})
