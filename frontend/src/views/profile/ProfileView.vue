<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import StatusPanel from '@/components/base/StatusPanel.vue'
import { profileApi } from '@/api/profile.api'
import { useAuthStore } from '@/stores/auth.store'
import { useUiStore } from '@/stores/ui.store'
import type { UserStats } from '@/types/domain'
import { avatarLetter, displayName, formatDate, getErrorMessage } from '@/utils/format'

const auth = useAuthStore()
const ui = useUiStore()
const router = useRouter()
const stats = ref<UserStats>({ shelfCount: 0, ratingCount: 0, reviewCount: 0 })
const statsLoading = ref(true)
const nickname = ref(auth.user?.nickname || '')
const profilePending = ref(false)
const passwordPending = ref(false)
const password = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordError = ref('')

const loadStats = async () => {
  statsLoading.value = true
  try {
    stats.value = await profileApi.stats(auth.user!.id)
  } catch (cause) {
    ui.notify(getErrorMessage(cause, '阅读统计暂时无法加载'), 'error')
  } finally {
    statsLoading.value = false
  }
}

const saveProfile = async () => {
  if (!nickname.value.trim()) return
  profilePending.value = true
  try {
    const updated = await profileApi.update(auth.user!.id, nickname.value.trim())
    auth.updateUser(updated)
    ui.notify('个人资料已保存', 'success')
  } catch (cause) {
    ui.notify(getErrorMessage(cause), 'error')
  } finally {
    profilePending.value = false
  }
}

const changePassword = async () => {
  passwordError.value = ''
  if (password.newPassword.length < 6) passwordError.value = '新密码至少需要 6 个字符'
  else if (password.newPassword !== password.confirmPassword) passwordError.value = '两次输入的新密码不一致'
  else if (password.oldPassword === password.newPassword) passwordError.value = '新密码不能与当前密码相同'
  if (passwordError.value) return

  passwordPending.value = true
  try {
    await profileApi.changePassword(auth.user!.id, password.oldPassword, password.newPassword)
    auth.clearSession()
    ui.notify('密码已修改，请使用新密码重新登录', 'success')
    await router.replace('/login')
  } catch (cause) {
    passwordError.value = getErrorMessage(cause, '密码修改失败')
  } finally {
    passwordPending.value = false
  }
}

onMounted(loadStats)
</script>

<template>
  <div class="page-shell container profile-page">
    <header class="page-heading"><div><p class="eyebrow">ACCOUNT & READING</p><h1>个人中心</h1><p class="lede">管理账户信息，并回顾自己的阅读参与。</p></div></header>

    <section class="profile-summary card">
      <div class="profile-avatar" aria-hidden="true">{{ avatarLetter(auth.user?.nickname || auth.user?.username) }}</div>
      <div><h2>{{ displayName(auth.user?.nickname || auth.user?.username) }}</h2><p>@{{ auth.user?.username }} · {{ auth.isAdmin ? '管理员' : '普通用户' }}</p><p v-if="auth.user?.createTime" class="join-date">{{ formatDate(auth.user.createTime) }} 加入</p></div>
    </section>

    <section aria-labelledby="stats-title">
      <h2 id="stats-title" class="section-title">阅读概览</h2>
      <StatusPanel v-if="statsLoading" tone="loading" title="正在读取统计" />
      <div v-else class="stats-grid">
        <div class="stat-card card"><strong>{{ stats.shelfCount }}</strong><span>书架藏书</span></div>
        <div class="stat-card card"><strong>{{ stats.ratingCount }}</strong><span>评分次数</span></div>
        <div class="stat-card card"><strong>{{ stats.reviewCount }}</strong><span>书评数量</span></div>
      </div>
    </section>

    <div class="profile-forms">
      <form class="card panel stack" aria-labelledby="profile-form-title" @submit.prevent="saveProfile">
        <div><p class="eyebrow">PROFILE</p><h2 id="profile-form-title" class="section-title">基本资料</h2></div>
        <div class="form-field"><label for="profile-username">用户名</label><input id="profile-username" class="input" :value="auth.user?.username" disabled /><p class="field-help">用户名不可修改。</p></div>
        <div class="form-field"><label for="nickname">昵称</label><input id="nickname" v-model="nickname" class="input" maxlength="20" autocomplete="nickname" /><p class="field-help">最多 20 个字符。</p></div>
        <button class="button mobile-wide" type="submit" :disabled="profilePending || !nickname.trim()">{{ profilePending ? '保存中…' : '保存资料' }}</button>
      </form>

      <form class="card panel stack" aria-labelledby="password-form-title" @submit.prevent="changePassword">
        <div><p class="eyebrow">SECURITY</p><h2 id="password-form-title" class="section-title">修改密码</h2></div>
        <p v-if="passwordError" class="password-alert" role="alert">{{ passwordError }}</p>
        <div class="form-field"><label for="old-password">当前密码</label><input id="old-password" v-model="password.oldPassword" class="input" type="password" autocomplete="current-password" required /></div>
        <div class="form-field"><label for="new-password">新密码</label><input id="new-password" v-model="password.newPassword" class="input" type="password" autocomplete="new-password" minlength="6" required /></div>
        <div class="form-field"><label for="confirm-password">确认新密码</label><input id="confirm-password" v-model="password.confirmPassword" class="input" type="password" autocomplete="new-password" minlength="6" required /></div>
        <button class="button mobile-wide" type="submit" :disabled="passwordPending">{{ passwordPending ? '修改中…' : '修改密码' }}</button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.profile-page { display: grid; gap: 30px; }
.profile-summary { display: flex; align-items: center; gap: 18px; padding: clamp(20px, 4vw, 30px); }
.profile-avatar { display: grid; width: 72px; height: 72px; flex: 0 0 auto; place-items: center; border-radius: 50%; background: var(--color-primary); color: white; font-family: var(--font-reading); font-size: 1.8rem; font-weight: 800; }
.profile-summary h2 { margin: 0; font-family: var(--font-reading); font-size: 1.45rem; }
.profile-summary p { margin: 3px 0 0; color: var(--color-text-soft); }
.join-date { font-size: .85rem; }
.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-top: 16px; }
.stat-card { display: grid; gap: 4px; padding: 18px 12px; text-align: center; }
.stat-card strong { color: var(--color-primary-strong); font-size: clamp(1.5rem, 5vw, 2.1rem); font-variant-numeric: tabular-nums; }
.stat-card span { color: var(--color-text-soft); font-size: .83rem; }
.profile-forms { display: grid; gap: 20px; }
.panel .button { justify-self: start; }
.password-alert { margin: 0; border-left: 4px solid var(--color-danger); background: var(--color-danger-soft); padding: 12px; color: #6f251f; }
@media (min-width: 840px) { .profile-forms { grid-template-columns: 1fr 1fr; align-items: start; } }
@media (max-width: 460px) { .profile-summary { align-items: flex-start; } .profile-avatar { width: 56px; height: 56px; font-size: 1.35rem; } }
</style>
