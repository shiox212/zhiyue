<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import AssistantPanel from '@/components/assistant/AssistantPanel.vue'
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

const assistantOpen = ref(false)
const assistantMounted = ref(false)
const assistantButton = ref<HTMLButtonElement | null>(null)
const assistantRegion = ref<HTMLElement | null>(null)
const openAssistant = async () => {
  if (assistantOpen.value) { closeAssistant(); return }
  assistantOpen.value = true
  assistantMounted.value = true
  await nextTick()
  assistantRegion.value?.querySelector<HTMLButtonElement>('button')?.focus()
}
const closeAssistant = () => { assistantOpen.value = false; nextTick(() => assistantButton.value?.focus()) }
const onKeydown = (event: KeyboardEvent) => { if (event.key === 'Escape' && assistantOpen.value) closeAssistant() }
onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <a href="#main-content" class="skip-link">跳到主要内容</a>
  <AppHeader />
  <main id="main-content" tabindex="-1">
    <RouterView />
  </main>
  <button ref="assistantButton" class="assistant-fab" type="button" :aria-expanded="assistantOpen" aria-controls="floating-assistant" aria-label="打开图书知识助手" @click="openAssistant">
    <span aria-hidden="true">问</span>
  </button>
  <aside v-show="assistantOpen" ref="assistantRegion" id="floating-assistant" class="floating-assistant" aria-label="图书知识助手">
    <div class="floating-bar"><strong>图书知识助手</strong><button type="button" class="icon-button" aria-label="关闭图书知识助手" @click="closeAssistant">×</button></div>
    <AssistantPanel v-if="assistantMounted" compact />
  </aside>
  <footer class="site-footer">
    <div class="container footer-inner">
      <p><strong>知阅</strong> · 让阅读、记录与发现自然发生。</p>
      <p>课程实践升级版</p>
    </div>
  </footer>
</template>

<style scoped>
.site-footer { border-top: 1px solid var(--color-border); background: var(--color-canvas-soft); color: var(--color-text-soft); }
.footer-inner { display: flex; min-height: 78px; flex-direction: column; justify-content: center; gap: 2px; padding-block: 16px; }
.footer-inner p { margin: 0; }
.footer-inner strong { color: var(--color-text); font-family: var(--font-reading); letter-spacing: .08em; }
.assistant-fab { position: fixed; right: 20px; bottom: 22px; z-index: 40; width: 56px; height: 56px; border: 0; border-radius: 50%; background: var(--color-primary); color: white; font: 700 1.1rem var(--font-reading); box-shadow: 0 8px 24px rgba(25, 44, 66, .24); cursor: pointer; }
.assistant-fab:focus-visible, .icon-button:focus-visible { outline: 3px solid var(--color-accent); outline-offset: 3px; }
.floating-assistant { position: fixed; right: 20px; bottom: 90px; z-index: 39; width: min(520px, calc(100vw - 32px)); max-height: min(760px, calc(100dvh - 112px)); overflow: auto; border: 1px solid var(--color-border); border-radius: 8px; background: var(--color-canvas); box-shadow: 0 18px 50px rgba(25, 44, 66, .2); }
.floating-bar { position: sticky; top: 0; z-index: 1; display: flex; align-items: center; justify-content: space-between; gap: 12px; min-height: 52px; padding: 8px 14px; border-bottom: 1px solid var(--color-border); background: var(--color-canvas); }
.icon-button { width: 44px; height: 44px; border: 0; background: transparent; color: var(--color-text-soft); font-size: 1.5rem; cursor: pointer; }
@media (max-width: 560px) { .assistant-fab { right: 12px; bottom: max(12px, env(safe-area-inset-bottom)); } .floating-assistant { right: 8px; bottom: calc(76px + env(safe-area-inset-bottom)); width: calc(100vw - 16px); max-height: calc(100dvh - 92px - env(safe-area-inset-bottom)); } }
@media (min-width: 640px) { .footer-inner { flex-direction: row; align-items: center; justify-content: space-between; } }
</style>
