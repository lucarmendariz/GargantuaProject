import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import InventarioView from '../views/InventarioView.vue'
import HistorialGlobalView from '../views/HistorialGlobalView.vue'
import ProduccionView from '../views/ProduccionView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'login', component: LoginView },
    { path: '/dashboard', name: 'dashboard', component: DashboardView },
    { path: '/inventario', name: 'inventario', component: InventarioView },
    { path: '/historial-global', name: 'historial', component: HistorialGlobalView },
    { path: '/produccion', name: 'produccion', component: ProduccionView }
  ]
})

export default router