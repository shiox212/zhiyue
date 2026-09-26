<script setup lang="ts">
defineProps<{
  title: string
  description?: string
  tone?: 'loading' | 'empty' | 'error'
}>()
</script>

<template>
  <section class="status card" :role="tone === 'error' ? 'alert' : 'status'" :aria-busy="tone === 'loading'">
    <span v-if="tone === 'loading'" class="status__spinner" aria-hidden="true"></span>
    <svg v-else class="status__icon" viewBox="0 0 24 24" aria-hidden="true">
      <path v-if="tone === 'error'" d="M12 9v4m0 4h.01M4.9 19h14.2a2 2 0 0 0 1.73-3L13.73 3.7a2 2 0 0 0-3.46 0L3.17 16A2 2 0 0 0 4.9 19Z" />
      <path v-else d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20V3H6.5A2.5 2.5 0 0 0 4 5.5v14Zm0 0V5.5" />
    </svg>
    <div>
      <h2>{{ title }}</h2>
      <p v-if="description">{{ description }}</p>
    </div>
    <div v-if="$slots.default" class="status__action"><slot /></div>
  </section>
</template>

<style scoped>
.status { display: grid; justify-items: center; gap: 12px; padding: 48px 24px; text-align: center; }
.status h2 { margin: 0; font-family: var(--font-reading); font-size: 1.3rem; }
.status p { max-width: 48ch; margin: 6px 0 0; color: var(--color-text-soft); }
.status__icon { width: 44px; height: 44px; fill: none; stroke: var(--color-primary); stroke-width: 1.6; stroke-linecap: round; stroke-linejoin: round; }
.status__spinner { width: 38px; height: 38px; border: 3px solid var(--color-primary-soft); border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
.status__action { margin-top: 4px; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
