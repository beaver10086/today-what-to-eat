import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
      meta: { title: '今天吃什么' },
    },
  ],
})

router.afterEach((to) => {
  document.title = String(to.meta.title ?? '今天吃什么')
})

export default router
