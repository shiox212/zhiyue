import { nextTick } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/auth.store'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior: (_to, _from, savedPosition) => savedPosition || { top: 0 },
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { guestOnly: true, title: '登录' },
    },
    {
      path: '/',
      component: () => import('@/layouts/DefaultLayout.vue'),
      children: [
        { path: '', redirect: '/books' },
        { path: 'books', name: 'books', component: () => import('@/views/books/BookListView.vue'), meta: { title: '发现图书' } },
        { path: 'books/:id', name: 'book-detail', component: () => import('@/views/books/BookDetailView.vue'), meta: { title: '图书详情' } },
        { path: 'recommendations', name: 'recommendations', component: () => import('@/views/books/RecommendationsView.vue'), meta: { requiresAuth: true, title: '为你推荐' } },
        { path: 'shelf', name: 'shelf', component: () => import('@/views/shelf/MyShelfView.vue'), meta: { requiresAuth: true, title: '我的书架' } },
        { path: 'profile', name: 'profile', component: () => import('@/views/profile/ProfileView.vue'), meta: { requiresAuth: true, title: '个人中心' } },
        { path: 'assistant', name: 'assistant', component: () => import('@/views/assistant/LearningAssistantView.vue'), meta: { requiresAuth: true, title: '学习助手' } },
        { path: '403', name: 'forbidden', component: () => import('@/views/system/ForbiddenView.vue'), meta: { title: '无权访问' } },
        { path: ':pathMatch(.*)*', name: 'not-found', component: () => import('@/views/system/NotFoundView.vue'), meta: { title: '页面未找到' } },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore(pinia)

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.roles?.includes('ADMIN') && !auth.isAdmin) return { name: 'forbidden' }
  if (to.meta.guestOnly && auth.isAuthenticated) return { name: 'books' }

  document.title = `${to.meta.title || '阅读'}｜知阅`
  return true
})

router.afterEach(async () => {
  await nextTick()
  document.querySelector<HTMLElement>('#main-content')?.focus({ preventScroll: true })
})

export default router
