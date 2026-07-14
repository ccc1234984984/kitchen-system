import { createRouter, createWebHistory } from 'vue-router'
import AdminLogin from '../views/AdminLogin.vue'
import KitchenBoard from '../views/KitchenBoard.vue'
import OrderForm from '../views/OrderForm.vue'
import TablesView from '../views/TablesView.vue'
import DishManage from '../views/DishManage.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'login', component: AdminLogin },
  { path: '/kitchen', name: 'kitchen', component: KitchenBoard, meta: { requiresAuth: true } },
  { path: '/tables', name: 'tables', component: TablesView, meta: { requiresAuth: true } },
  { path: '/order', name: 'order', component: OrderForm, meta: { requiresAuth: true } },
  { path: '/dishes', name: 'dishes', component: DishManage, meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth) {
    const user = localStorage.getItem('adminUser')
    if (!user) {
      next('/login')
      return
    }
  }
  next()
})

export default router
