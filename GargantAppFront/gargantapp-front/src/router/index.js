import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import InventarioView from '../views/InventarioView.vue'
import HistorialGlobalView from '../views/HistorialGlobalView.vue'
import ProduccionView from '../views/ProduccionView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { 
      path: '/', 
      name: 'login', 
      component: LoginView 
    },
    { 
      path: '/dashboard', 
      name: 'dashboard', 
      component: DashboardView,
      meta: { requiresAuth: true } // Cualquiera logueado puede entrar
    },
    { 
      path: '/inventario', 
      name: 'inventario', 
      component: InventarioView,
      meta: { requiresAuth: true, roles: ['ADMIN', 'ENCARGADO'] } // Restringido
    },
    { 
      path: '/historial-global', 
      name: 'historial', 
      component: HistorialGlobalView,
      meta: { requiresAuth: true, roles: ['ADMIN'] } // Solo el Administrador supremo
    },
    { 
      path: '/produccion', 
      name: 'produccion', 
      component: ProduccionView,
      meta: { requiresAuth: true, roles: ['ADMIN', 'OPERARIO'] } // Operarios y admin
    }
  ]
})

// 🔐 Guardián de Seguridad Global (Navigation Guard)
router.beforeEach((to, from, next) => {
  // 1. Recuperamos los datos de sesión almacenados al loguearse
  const token = localStorage.getItem('token');
  const savedUser = localStorage.getItem('user'); 
  const user = savedUser ? JSON.parse(savedUser) : null;

  // 2. Comprobar si la pestaña exige estar logueado
  if (to.meta.requiresAuth && !token) {
    // Si no hay token, patada hacia el login
    return next({ name: 'login' });
  }

  // 3. Comprobar restricciones de Rol (Si la ruta tiene limitaciones)
  if (to.meta.roles) {
    const hasRolePermission = user && to.meta.roles.includes(user.role);
    
    if (!hasRolePermission) {
      // Si está logueado pero no tiene el rol necesario, le mandamos al Panel Central
      alert('⚠️ Acceso denegado: Tu perfil de operario no tiene permisos para esta sección.');
      return next({ name: 'dashboard' });
    }
  }

  // Si pasa todos los controles, se le permite continuar el viaje
  next();
})

export default router