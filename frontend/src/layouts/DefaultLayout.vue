<script setup lang="ts">
import { RouterLink, RouterView, useRoute } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import AssistantPanel from '@/components/assistant/AssistantPanel.vue'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useAssistantStore } from '@/stores/assistant.store'

const session = useAssistantStore()
const route = useRoute()
const onPage = computed(() => route.path === '/assistant')
const visible = computed(() => session.open && !onPage.value)
const small = ref(false)
const region = ref<HTMLElement | null>(null)
const background = ref<HTMLElement | null>(null)
const closeButton = ref<HTMLButtonElement | null>(null)
const viewport = ref({ height: '', top: '0px' })
let oldOverflow: string | null = null
let toastRegion: HTMLElement | null = null
let toastWasInert = false
function updateViewport() {
  small.value = window.innerWidth <= 600 || window.innerHeight <= 500
  const vv = window.visualViewport
  viewport.value = { height: `${vv?.height ?? window.innerHeight}px`, top: `${vv?.offsetTop ?? 0}px` }
}
function releaseModal() {
  if (toastRegion) { toastRegion.inert = toastWasInert; toastRegion = null }
  if (background.value) background.value.inert = false
  if (oldOverflow !== null) { document.body.style.overflow = oldOverflow; oldOverflow = null }
}
async function applyMode() {
  releaseModal()
  if (visible.value && small.value) {
    oldOverflow = document.body.style.overflow; document.body.style.overflow = 'hidden'
    if (background.value) background.value.inert = true
    toastRegion = document.querySelector<HTMLElement>(".toast-region")
    if (toastRegion) { toastWasInert = toastRegion.inert; toastRegion.inert = true }
  }
  if (visible.value) { await nextTick(); if (!region.value?.contains(document.activeElement)) closeButton.value?.focus({ preventScroll: true }) }
}
function onFocus(event: FocusEvent) {
  if (visible.value && small.value && event.target instanceof Node && !region.value?.contains(event.target)) closeButton.value?.focus({ preventScroll: true })
}
function onKeydown(event: KeyboardEvent) {
  if (!visible.value) return
  if (event.key === 'Escape') { event.preventDefault(); session.hide(); return }
  if (event.key !== 'Tab' || !small.value) return
  const items = Array.from(region.value?.querySelectorAll<HTMLElement>('button:not(:disabled), a[href], textarea:not(:disabled), summary, [tabindex="0"]') || []).filter(el => el.getClientRects().length > 0)
  const first = items[0]; const last = items[items.length - 1]
  if (!first) { event.preventDefault(); return }
  if (event.shiftKey && (document.activeElement === first || !region.value?.contains(document.activeElement))) { event.preventDefault(); last?.focus() }
  else if (!event.shiftKey && (document.activeElement === last || !region.value?.contains(document.activeElement))) { event.preventDefault(); first.focus() }
}
watch([visible, small], applyMode, { flush: 'post' })
watch(() => route.fullPath, () => { if (visible.value && small.value) nextTick(() => closeButton.value?.focus()) })
watch(onPage, value => { session.fullPage = value; if (value) session.hide(false) }, { immediate: true })
onMounted(() => {
  updateViewport(); window.addEventListener('resize', updateViewport); window.addEventListener('keydown', onKeydown); document.addEventListener('focusin', onFocus)
  window.visualViewport?.addEventListener('resize', updateViewport); window.visualViewport?.addEventListener('scroll', updateViewport)
})
onBeforeUnmount(() => {
  releaseModal(); session.fullPage = false
  window.removeEventListener('resize', updateViewport); window.removeEventListener('keydown', onKeydown); document.removeEventListener('focusin', onFocus)
  window.visualViewport?.removeEventListener('resize', updateViewport); window.visualViewport?.removeEventListener('scroll', updateViewport)
})
</script>

<template>
  <div ref="background">
    <a href="#main-content" class="skip-link">跳到主要内容</a>
    <AppHeader />
    <main id="main-content" tabindex="-1"><RouterView /></main>
    <footer class="site-footer"><div class="container footer-inner"><p><strong>知阅</strong> · 让阅读、记录与发现自然发生。</p><p>课程实践升级版</p></div></footer>
    <button v-if="!onPage" class="assistant-fab" type="button" :aria-expanded="visible" aria-controls="floating-assistant" aria-label="打开 AI 阅读助手" @click="visible ? session.hide() : session.show($event.currentTarget as HTMLElement)">
      <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 4h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-8l-5 3v-3H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z M8 9h8 M8 13h5" /></svg><span>{{ session.unread ? '有新回答 · 问问 AI' : '问问 AI' }}</span>
    </button>
  </div>
  <section v-if="visible" id="floating-assistant" ref="region" class="floating-assistant" :class="{ 'assistant-expanded': session.expanded, 'assistant-modal': small }" :style="small ? { height: viewport.height, top: viewport.top } : {}" :role="small ? 'dialog' : 'region'" :aria-modal="small ? true : undefined" aria-labelledby="assistant-window-title">
    <header class="floating-bar"><div><strong id="assistant-window-title">AI 阅读助手</strong><small>知阅 · 陪你读懂每一页</small></div><div class="assistant-window-actions"><button v-if="!small" type="button" :aria-pressed="session.expanded" @click="session.expanded = !session.expanded">{{ session.expanded ? '还原' : '展开' }}</button><RouterLink v-if="!small" to="/assistant" @click="session.hide(false)">全页</RouterLink><button ref="closeButton" type="button" aria-label="收起 AI 阅读助手" @click="session.hide()">收起 <span aria-hidden="true">×</span></button></div></header>
    <AssistantPanel compact :active="visible" />
  </section>
</template>

<style scoped>
.site-footer { border-top: 1px solid var(--color-border); background: var(--color-canvas-soft); color: var(--color-text-soft); }
.footer-inner { display: flex; min-height: 78px; flex-direction: column; justify-content: center; gap: 2px; padding-block: 16px; }
.footer-inner p { margin: 0; }
.footer-inner strong { color: var(--color-text); font-family: var(--font-reading); letter-spacing: .08em; }
/* Layer contract: header 100, assistant entry 110, panel 120, existing toast 900, mobile modal 910, skip link 1000. */
.assistant-fab { position: fixed; right: 20px; bottom: max(20px, env(safe-area-inset-bottom)); z-index: 110; display: flex; align-items: center; gap: 9px; min-height: 48px; padding: 10px 18px; border: 1px solid var(--color-primary-strong); border-radius: 28px; background: var(--color-primary); color: white; font: 700 16px var(--font-reading); box-shadow: 0 6px 22px rgb(32 40 34 / 22%); cursor: pointer; }
.assistant-fab svg { width: 22px; height: 22px; fill: none; stroke: currentColor; stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }
.floating-assistant { position: fixed; right: 20px; bottom: 82px; z-index: 120; width: min(460px, calc(100vw - 40px)); height: min(700px, calc(100dvh - 106px)); display: flex; flex-direction: column; overflow: hidden; border: 1px solid var(--color-border); border-radius: 16px; background: var(--color-canvas); box-shadow: 0 18px 50px rgb(32 40 34 / 22%); }
.assistant-expanded { width: min(840px, calc(100vw - 40px)); height: min(850px, calc(100dvh - 106px)); }
.floating-bar { flex: none; display: flex; align-items: center; justify-content: space-between; gap: 8px; min-height: 66px; padding: 8px 12px 8px 18px; border-bottom: 1px solid var(--color-border); background: var(--color-primary-soft); }
.floating-bar strong { font-family: var(--font-reading); font-size: 17px; }
.floating-bar small { display: block; font-size: 12px; color: var(--color-text-soft); }
.assistant-window-actions { display: flex; gap: 4px; flex: none; }
.assistant-window-actions :is(a,button) { display: flex; align-items: center; justify-content: center; min-height: 44px; min-width: 44px; padding: 6px; font: inherit; font-size: 14px; background: transparent; border: 0; border-radius: 6px; color: var(--color-primary-strong); cursor: pointer; text-decoration: none; }
.assistant-window-actions :is(a,button):hover { background: var(--color-surface); }
.assistant-fab:focus-visible, .assistant-window-actions :focus-visible { outline: 3px solid var(--color-primary); outline-offset: 3px; }
.floating-assistant > .assistant-chat { flex: 1; }
.assistant-modal { z-index: 910; left: 0; right: 0; bottom: auto; width: 100%; max-width: 100%; height: 100dvh; border-radius: 0; padding-top: env(safe-area-inset-top); }
@media (max-width: 600px) { .assistant-fab { right: 12px; bottom: max(12px, env(safe-area-inset-bottom)); } }
@media (min-width: 640px) { .footer-inner { flex-direction: row; align-items: center; justify-content: space-between; } }
@media (prefers-reduced-motion: reduce) { .floating-assistant, .assistant-fab { animation: none; transition: none; } }
</style>
