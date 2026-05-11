import { createRouter, createWebHistory } from 'vue-router'
import Login from './views/Login.vue'
import Register from './views/Register.vue'
import Dashboard from './views/Dashboard.vue'
import UserManage from './views/UserManage.vue'
import EquipmentManage from './views/EquipmentManage.vue'
import VehicleManage from './views/VehicleManage.vue'
import BorrowRequest from './views/BorrowRequest.vue'
import MainLayout from './layout/MainLayout.vue'

const routes = [
  { path: '/login', component: Login },
  { path: '/register', component: Register },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: Dashboard },
      { path: 'users', component: UserManage },
      { path: 'equipments', component: EquipmentManage },
      { path: 'vehicles', component: VehicleManage },
      { path: 'borrows', component: BorrowRequest }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation Guard
router.beforeEach((to, from, next) => {
  const user = localStorage.getItem('user')
  if (to.path !== '/login' && to.path !== '/register' && !user) {
    next('/login')
  } else {
    next()
  }
})

export default router
