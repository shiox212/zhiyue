<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import { useUiStore } from '@/stores/ui.store'
import { authApi } from '@/api/auth.api'
import { getErrorMessage } from '@/utils/format'

const auth = useAuthStore()
const ui = useUiStore()
const route = useRoute()
const router = useRouter()
const form = reactive({ username: '', password: '' })
const errors = reactive({ username: '', password: '', form: '' })
const showPassword = ref(false)
const registering = ref(false)
const registrationPending = ref(false)
const nickname = ref('')
const confirmPassword = ref('')
const pending = computed(() => auth.submitting || registrationPending.value)
const usernameInput = ref<HTMLInputElement | null>(null)

const redirectTarget = computed(() => {
  const value = typeof route.query.redirect === 'string' ? route.query.redirect : '/books'
  return value.startsWith('/') && !value.startsWith('//') ? value : '/books'
})

onMounted(async () => {
  if (route.query.expired === '1') errors.form = '登录状态已过期，请重新登录后继续。'
  await nextTick()
  usernameInput.value?.focus()
})

const validate = () => {
  errors.username = form.username.trim().length >= 3 && form.username.trim().length <= 20 ? '' : '用户名需要 3 至 20 个字符'
  errors.password = form.password.length >= 6 && form.password.length <= 20 ? '' : '密码需要 6 至 20 个字符'
  return !errors.username && !errors.password
}

const submit = async () => {
  errors.form = ''
  if (!validate()) return
  if (registering.value) {
    if (form.password !== confirmPassword.value) {
      errors.form = '两次输入的密码不一致'
      return
    }
    registrationPending.value = true
    try {
      await authApi.register({ username: form.username.trim(), password: form.password, nickname: nickname.value.trim() || undefined })
      registering.value = false
      form.password = ''
      confirmPassword.value = ''
      ui.notify('注册成功，请登录', 'success')
    } catch (error) {
      errors.form = getErrorMessage(error, '注册失败，请重试')
    } finally {
      registrationPending.value = false
    }
    return
  }
  try {
    await auth.login(form.username.trim(), form.password)
    ui.notify('登录成功，欢迎回来', 'success')
    await router.replace(redirectTarget.value)
  } catch (error) {
    errors.form = getErrorMessage(error, '登录失败，请检查用户名和密码')
  }
}
</script>

<template>
  <main class="login-page">
    <a href="#login-form" class="skip-link">跳到登录表单</a>
    <section class="login-story" aria-labelledby="story-title">
      <RouterLink to="/books" class="login-brand" aria-label="返回知阅图书页">
        <svg viewBox="0 0 28 28" aria-hidden="true"><path d="M5 5.5h7.5c2.2 0 4 1.8 4 4V23c0-2.2-1.8-4-4-4H5V5.5Zm18 0h-2.5c-2.2 0-4 1.8-4 4V23c0-2.2 1.8-4 4-4H23V5.5Z" /></svg>
        <span>知阅</span>
      </RouterLink>
      <div>
        <p class="eyebrow">KNOW · READ · GROW</p>
        <h1 id="story-title">从下一本好书开始，<br />拓宽自己的边界。</h1>
        <p>收藏阅读进度，留下真实评分，在书评中分享自己的发现。</p>
      </div>
      <blockquote>“读过的每一页，都在悄悄重塑看世界的方式。”</blockquote>
    </section>

    <section class="login-workspace" aria-labelledby="login-title">
      <form id="login-form" class="login-card card" novalidate @submit.prevent="submit">
        <div>
          <p class="eyebrow">{{ registering ? '开启阅读之旅' : '欢迎回来' }}</p>
          <h2 id="login-title">{{ registering ? '注册知阅' : '登录知阅' }}</h2>
          <p class="login-intro">{{ registering ? '创建账户，记录自己的阅读生活。' : '继续管理书架和个性化推荐。' }}</p>
        </div>

        <p v-if="errors.form" class="form-alert" role="alert">{{ errors.form }}</p>

        <div class="form-field">
          <label for="username">用户名</label>
          <input
            id="username"
            ref="usernameInput"
            v-model="form.username"
            class="input"
            type="text"
            autocomplete="username"
            :aria-invalid="Boolean(errors.username)"
            :aria-describedby="errors.username ? 'username-error' : undefined"
            @blur="validate"
          />
          <p v-if="errors.username" id="username-error" class="field-error" role="alert">{{ errors.username }}</p>
        </div>

        <div class="form-field">
          <label for="password">密码</label>
          <div class="password-field">
            <input
              id="password"
              v-model="form.password"
              class="input"
              :type="showPassword ? 'text' : 'password'"
              :autocomplete="registering ? 'new-password' : 'current-password'"
              :aria-invalid="Boolean(errors.password)"
              :aria-describedby="errors.password ? 'password-error' : undefined"
              @blur="validate"
            />
            <button type="button" class="password-toggle" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword">
              {{ showPassword ? '隐藏' : '显示' }}
            </button>
          </div>
          <p v-if="errors.password" id="password-error" class="field-error" role="alert">{{ errors.password }}</p>
        </div>

        <template v-if="registering">
          <div class="form-field"><label for="confirm-password">确认密码</label><input id="confirm-password" v-model="confirmPassword" class="input" type="password" autocomplete="new-password" maxlength="20" /></div>
          <div class="form-field"><label for="register-nickname">昵称（可选）</label><input id="register-nickname" v-model="nickname" class="input" autocomplete="nickname" maxlength="20" /></div>
        </template>
        <button class="button login-submit" type="submit" :disabled="pending">
          <span v-if="pending" class="button-spinner" aria-hidden="true"></span>
          {{ pending ? '正在提交…' : registering ? '注册' : '登录' }}
        </button>
        <button class="button secondary" type="button" :disabled="pending" @click="registering = !registering; errors.form = ''; errors.username = ''; errors.password = ''; form.password = ''; confirmPassword = ''">{{ registering ? '已有账户，返回登录' : '还没有账户？注册' }}</button>
        <RouterLink to="/books" class="browse-link">暂不登录，先浏览图书</RouterLink>
      </form>
    </section>
  </main>
</template>

<style scoped>
.login-page { display: grid; min-height: 100dvh; }
.login-story { display: none; }
.login-workspace { display: grid; min-height: 100dvh; place-items: center; padding: 28px 16px; }
.login-card { display: grid; width: min(100%, 440px); gap: 20px; padding: clamp(24px, 6vw, 40px); box-shadow: var(--shadow-md); }
.login-card h2 { margin: 0; font-family: var(--font-reading); font-size: 2rem; }
.login-intro { margin: 6px 0 0; color: var(--color-text-soft); }
.form-alert { margin: 0; border-left: 4px solid var(--color-danger); border-radius: var(--radius-sm); background: var(--color-danger-soft); padding: 12px 14px; color: #6f251f; }
.password-field { position: relative; }
.password-field .input { padding-right: 72px; }
.password-toggle { position: absolute; top: 1px; right: 1px; min-width: 64px; height: 44px; border: 0; border-radius: var(--radius-sm); background: transparent; color: var(--color-primary-strong); font-weight: 700; }
.password-toggle:hover { background: var(--color-primary-soft); }
.login-submit { width: 100%; }
.browse-link { min-height: 44px; padding: 9px; color: var(--color-primary-strong); text-align: center; }
.button-spinner { width: 18px; height: 18px; border: 2px solid rgb(255 255 255 / 40%); border-top-color: white; border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (min-width: 860px) {
  .login-page { grid-template-columns: minmax(360px, .9fr) minmax(480px, 1.1fr); }
  .login-story { position: relative; display: flex; min-height: 100dvh; flex-direction: column; justify-content: space-between; overflow: hidden; padding: 48px clamp(36px, 6vw, 80px); background: var(--color-primary-strong); color: white; }
  .login-story::after { position: absolute; right: -120px; bottom: -160px; width: 430px; height: 430px; border: 1px solid rgb(255 255 255 / 18%); border-radius: 50%; box-shadow: 0 0 0 72px rgb(255 255 255 / 4%), 0 0 0 142px rgb(255 255 255 / 3%); content: ''; }
  .login-brand { display: inline-flex; width: fit-content; min-height: 48px; align-items: center; gap: 12px; color: white; font-family: var(--font-reading); font-size: 1.3rem; font-weight: 800; letter-spacing: .12em; text-decoration: none; }
  .login-brand svg { width: 34px; fill: none; stroke: currentColor; stroke-width: 1.7; }
  .login-story .eyebrow { color: #d8e7dc; }
  .login-story h1 { max-width: 13ch; margin: 12px 0 20px; font-family: var(--font-reading); font-size: clamp(2.5rem, 5vw, 4.6rem); font-weight: 650; line-height: 1.23; }
  .login-story p:not(.eyebrow) { max-width: 47ch; color: #e2ebe5; }
  .login-story blockquote { position: relative; z-index: 1; max-width: 38ch; margin: 0; border-left: 2px solid #bcd2c2; padding-left: 16px; color: #e2ebe5; font-family: var(--font-reading); }
}
</style>
