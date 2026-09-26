<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui.store'

const ui = useUiStore()
const { toasts } = storeToRefs(ui)
</script>

<template>
  <div class="toast-region" aria-live="polite" aria-atomic="false">
    <div v-for="toast in toasts" :key="toast.id" class="toast" :class="`toast--${toast.tone}`">
      <span class="toast__mark" aria-hidden="true"></span>
      <span>{{ toast.message }}</span>
      <button type="button" class="toast__close" aria-label="关闭提示" @click="ui.dismiss(toast.id)">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 6 12 12M18 6 6 18" /></svg>
      </button>
    </div>
  </div>
</template>

<style scoped>
.toast-region { position: fixed; z-index: 900; top: 80px; right: 16px; display: grid; gap: 10px; width: min(380px, calc(100vw - 32px)); }
.toast { display: grid; grid-template-columns: 8px 1fr 44px; align-items: center; gap: 10px; min-height: 56px; padding: 6px 6px 6px 14px; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-surface-raised); box-shadow: var(--shadow-md); }
.toast__mark { width: 8px; height: 8px; border-radius: 50%; background: var(--color-primary); }
.toast--error .toast__mark { background: var(--color-danger); }
.toast--success .toast__mark { background: var(--color-success); }
.toast__close { width: 44px; height: 44px; border: 0; border-radius: var(--radius-sm); background: transparent; color: var(--color-text-soft); }
.toast__close:hover { background: var(--color-canvas-soft); }
.toast__close svg { width: 20px; fill: none; stroke: currentColor; stroke-width: 2; }
</style>
