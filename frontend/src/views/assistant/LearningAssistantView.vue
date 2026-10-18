<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'
import AssistantPanel from '@/components/assistant/AssistantPanel.vue'
import { useAssistantStore } from '@/stores/assistant.store'
const session = useAssistantStore()
onMounted(() => { session.fullPage = true; session.hide(false) })
onBeforeUnmount(() => { session.fullPage = false })
</script>
<template><div class="assistant-page-shell container"><header><h1>AI 阅读助手</h1><RouterLink to="/books">返回图书发现</RouterLink></header><AssistantPanel active /></div></template>
<style scoped>
.assistant-page-shell { height: calc(100dvh - 88px); min-height: 320px; max-width: 1040px; display: flex; flex-direction: column; padding-block: 12px; }
.assistant-page-shell > header { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex: none; }
h1 { font: 700 24px var(--font-reading); margin: 8px 0; }
header a { min-height: 44px; display: flex; align-items: center; color: var(--color-primary-strong); }
.assistant-page-shell > .assistant-chat { flex: 1; border: 1px solid var(--color-border); border-radius: 12px; overflow: hidden; }
@media (max-width: 600px) { .assistant-page-shell { padding: 8px; height: calc(100dvh - 72px); } h1 { font-size: 20px; } }
</style>
