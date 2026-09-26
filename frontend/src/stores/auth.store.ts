import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/auth.api'
import type { User } from '@/types/domain'
export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const submitting = ref(false)
  const isAuthenticated = computed(() => Boolean(user.value))
  const isAdmin = computed(() => user.value?.role === 1)
  const updateUser = (value: User) => { user.value = value }
  const clearSession = () => { user.value = null }
  const restore = async () => {
    try { user.value = await authApi.me() } catch { clearSession() }
  }
  const login = async (username: string, password: string) => {
    submitting.value = true
    try { user.value = await authApi.login({ username, password }); return user.value }
    finally { submitting.value = false }
  }
  const logout = async () => { await authApi.logout(); clearSession() }
  return { user, submitting, isAuthenticated, isAdmin, updateUser, clearSession, restore, login, logout }
})
