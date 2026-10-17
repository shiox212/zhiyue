<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import { useAssistantStore } from '@/stores/assistant.store'
import { useUiStore } from '@/stores/ui.store'
import { avatarLetter, displayName } from '@/utils/format'

const assistant = useAssistantStore()
const openAssistant = (event: MouseEvent) => { menuOpen.value = false; if (route.path === "/assistant") document.querySelector<HTMLTextAreaElement>(".assistant-chat textarea")?.focus(); else assistant.show(event.currentTarget as HTMLElement) }
const auth = useAuthStore()
const ui = useUiStore()
const router = useRouter()
const route = useRoute()
const { user, isAuthenticated, isAdmin } = storeToRefs(auth)
const menuOpen = ref(false)

const name = computed(() => displayName(user.value?.nickname || user.value?.username))

watch(() => route.fullPath, () => {
  menuOpen.value = false
})

const logout = async () => {
  await auth.logout()
  ui.notify('已安全退出', 'success')
  await router.push('/books')
}
</script>

<template>
  <header class="site-header">
    <div class="container header-inner">
      <RouterLink to="/books" class="brand" aria-label="知阅首页">
        <span class="brand__mark" aria-hidden="true">
          <svg viewBox="0 0 28 28"><path d="M5 5.5h7.5c2.2 0 4 1.8 4 4V23c0-2.2-1.8-4-4-4H5V5.5Zm18 0h-2.5c-2.2 0-4 1.8-4 4V23c0-2.2 1.8-4 4-4H23V5.5Z" /></svg>
        </span>
        <span><strong>知阅</strong><small>读见更辽阔的自己</small></span>
      </RouterLink>

      <button
        type="button"
        class="menu-toggle"
        :aria-expanded="menuOpen"
        aria-controls="primary-navigation"
        :aria-label="menuOpen ? '收起导航' : '展开导航'"
        @click="menuOpen = !menuOpen"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true"><path :d="menuOpen ? 'm6 6 12 12M18 6 6 18' : 'M4 7h16M4 12h16M4 17h16'" /></svg>
      </button>

      <div id="primary-navigation" class="nav-panel" :class="{ 'nav-panel--open': menuOpen }">
        <nav class="primary-nav" aria-label="主导航">
          <RouterLink to="/books">发现</RouterLink>
          <RouterLink v-if="isAuthenticated" to="/recommendations">推荐</RouterLink>
          <RouterLink v-if="isAuthenticated" to="/shelf">书架</RouterLink>
          <button type="button" class="assistant-nav-entry" @click="openAssistant">AI 阅读助手</button>
        </nav>

        <div class="account-actions">
          <template v-if="isAuthenticated">
            <RouterLink to="/profile" class="profile-link" :aria-label="`进入${name}的个人中心`">
              <span class="avatar" aria-hidden="true">{{ avatarLetter(name) }}</span>
              <span class="profile-link__name">{{ name }}</span>
            </RouterLink>
            <button type="button" class="text-button" @click="logout">退出</button>
          </template>
          <RouterLink v-else to="/login" class="button small">登录</RouterLink>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.site-header { position: sticky; z-index: 100; top: 0; border-bottom: 1px solid rgb(217 221 215 / 85%); background: rgb(244 241 235 / 94%); backdrop-filter: blur(14px); }
.header-inner { display: flex; min-height: 72px; align-items: center; justify-content: space-between; gap: 20px; }
.brand { display: inline-flex; min-height: 48px; align-items: center; gap: 10px; color: var(--color-text); text-decoration: none; }
.brand__mark { display: grid; width: 42px; height: 42px; place-items: center; border-radius: 12px; background: var(--color-primary); color: white; }
.brand__mark svg { width: 28px; fill: none; stroke: currentColor; stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }
.brand strong { display: block; font-family: var(--font-reading); font-size: 1.2rem; letter-spacing: .12em; }
.brand small { display: none; color: var(--color-text-muted); font-size: .7rem; letter-spacing: .04em; }
.menu-toggle { display: grid; width: 48px; height: 48px; place-items: center; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-surface); }
.menu-toggle svg { width: 23px; fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; }
.nav-panel { position: absolute; top: 72px; right: 0; left: 0; display: none; border-bottom: 1px solid var(--color-border); background: var(--color-surface); padding: 12px 16px 20px; box-shadow: var(--shadow-md); }
.nav-panel--open { display: grid; gap: 14px; }
.primary-nav { display: grid; gap: 4px; }
.primary-nav a, .primary-nav .assistant-nav-entry { display: flex; min-height: 44px; align-items: center; border-radius: var(--radius-sm); padding: 8px 12px; color: var(--color-text-soft); font-weight: 700; text-decoration: none; }
.primary-nav .assistant-nav-entry:hover,
.primary-nav a:hover,
.primary-nav a.router-link-active { background: var(--color-primary-soft); color: var(--color-primary-strong); }
.account-actions { display: flex; align-items: center; justify-content: space-between; gap: 8px; border-top: 1px solid var(--color-border); padding-top: 12px; }
.profile-link { display: inline-flex; min-height: 44px; align-items: center; gap: 8px; border-radius: var(--radius-sm); color: var(--color-text); font-weight: 700; text-decoration: none; }
.avatar { display: grid; width: 36px; height: 36px; place-items: center; border-radius: 50%; background: var(--color-accent-soft); color: #75401f; font-weight: 800; }

@media (min-width: 920px) {
  .brand small { display: block; }
  .menu-toggle { display: none; }
  .nav-panel { position: static; display: flex; flex: 1; align-items: center; justify-content: space-between; border: 0; background: transparent; padding: 0; box-shadow: none; }
  .primary-nav { display: flex; align-items: center; gap: 2px; margin-left: 20px; }
  .primary-nav a { padding-inline: 13px; }
  .account-actions { justify-content: flex-end; border: 0; padding: 0; }
  .profile-link { padding-inline: 4px; }
}

@media (min-width: 1080px) {
  .primary-nav { gap: 6px; margin-left: 36px; }
}
.assistant-nav-entry { font: inherit; border: 1px solid var(--color-primary); background: var(--color-primary-soft); cursor: pointer; color: var(--color-primary-strong) !important; }
.assistant-nav-entry:focus-visible { outline: 3px solid var(--color-primary); outline-offset: 2px; }
</style>
