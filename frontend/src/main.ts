import { createApp } from 'vue'
import App from './App.vue'
import router from '@/router'
import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/auth.store'
import '@/styles/tokens.css'
import '@/styles/global.css'

const app = createApp(App)
app.use(pinia)

window.addEventListener('zhiyue:auth-expired', async () => {
  useAuthStore(pinia).clearSession()
  if (router.currentRoute.value.meta.requiresAuth) {
    await router.replace({ name: 'login', query: { redirect: router.currentRoute.value.fullPath, expired: '1' } })
  }
})

useAuthStore(pinia).restore().finally(() => {
  app.use(router)
  app.mount('#app')
})
