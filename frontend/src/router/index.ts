import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'dishes',
      component: () => import('../views/DishListView.vue'),
      meta: { title: '菜品发现' },
    },
    {
      path: '/shops/:id',
      name: 'shop-detail',
      component: () => import('../views/ShopDetailView.vue'),
      meta: { title: '档口详情' },
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('../views/AdminView.vue'),
      meta: { title: '数据管理', requiresAdmin: true },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { title: '登录注册' },
    },
    {
      path: '/survey',
      name: 'survey',
      component: () => import('../views/SurveyView.vue'),
      meta: { title: '口味问卷', requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue'),
      meta: { title: '我的口味画像', requiresAuth: true },
    },
    {
      path: '/recommendations',
      name: 'recommendations',
      component: () => import('../views/RecommendationView.vue'),
      meta: { title: '今日推荐', requiresAuth: true },
    },
    {
      path: '/favorites',
      name: 'favorites',
      component: () => import('../views/FavoritesView.vue'),
      meta: { title: '我的收藏', requiresAuth: true },
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAdmin && !auth.isAdmin)
    return { name: 'login', query: { redirect: to.fullPath } }
  if (to.meta.requiresAuth && !auth.isLoggedIn)
    return { name: 'login', query: { redirect: to.fullPath } }
  return true
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title ?? '今天吃什么')} · 今天吃什么`
})

export default router
