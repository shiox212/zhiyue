import { computed, reactive, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { assistantApi, type AssistantAnswer, type AssistantStatus } from '@/api/assistant.api'
import { useAuthStore } from '@/stores/auth.store'
import { getErrorMessage } from '@/utils/format'

export interface AssistantTurn { id: number; question: string; pending: boolean; answer: AssistantAnswer | null; error: string }
export const useAssistantStore = defineStore('assistant', () => {
  const auth = useAuthStore()
  const history = ref<AssistantTurn[]>([])
  const draft = ref('')
  const status = ref<AssistantStatus | null>(null)
  const loading = ref(false)
  const statusError = ref('')
  const pending = ref(false)
  const open = ref(false)
  const expanded = ref(false)
  const fullPage = ref(false)
  const unread = ref(false)
  const revision = ref(0)
  const version = ref(0)
  const visible = computed(() => open.value || fullPage.value)
  let requested = false
  let nextId = 0
  let statusVersion = 0
  let returnFocus: HTMLElement | null = null

  function clear() {
    version.value++
    history.value = []; draft.value = ''; pending.value = false; unread.value = false
    revision.value++
  }
  async function loadStatus(reconnect = false) {
    if (!auth.isAuthenticated || loading.value || (requested && !reconnect)) return
    const stamp = ++statusVersion
    requested = true; loading.value = true; statusError.value = ''
    try { const result = await assistantApi.status(); if (stamp === statusVersion) status.value = result }
    catch (cause) { if (stamp === statusVersion) statusError.value = getErrorMessage(cause, '暂时无法连接助手') }
    finally { if (stamp === statusVersion) loading.value = false }
  }
  function show(trigger?: HTMLElement) {
    returnFocus = trigger || (document.activeElement instanceof HTMLElement ? document.activeElement : null)
    open.value = true; unread.value = false
    void loadStatus()
  }
  function hide(restore = true) {
    open.value = false
    if (restore) queueMicrotask(() => { if (returnFocus?.isConnected) returnFocus.focus({ preventScroll: true }) })
  }
  async function ask() {
    if (!auth.isAuthenticated || pending.value || !status.value?.configured || !draft.value.trim() || draft.value.length > 1000) return false
    const stamp = version.value
    const turn = reactive<AssistantTurn>({ id: ++nextId, question: draft.value.trim(), pending: true, answer: null, error: '' })
    history.value.push(turn); draft.value = ''; pending.value = true; revision.value++
    try {
      const result = await assistantApi.ask(turn.question, 'AUTO')
      if (stamp === version.value) turn.answer = result
    } catch (cause) {
      if (stamp === version.value) turn.error = getErrorMessage(cause, '本次提问失败，请编辑后重发')
    } finally {
      if (stamp === version.value) { turn.pending = false; pending.value = false; revision.value++; if (!visible.value) unread.value = true }
    }
    return true
  }
  watch(() => `${auth.user?.id ?? "guest"}:${auth.isAuthenticated}`, () => {
    clear(); statusVersion++; status.value = null; loading.value = false; statusError.value = ''; requested = false
    if (visible.value && auth.isAuthenticated) void loadStatus()
  }, { flush: 'sync' })
  watch(visible, value => { if (value) { unread.value = false; void loadStatus() } })
  return { history, draft, status, loading, statusError, pending, open, expanded, fullPage, unread, revision, version, visible, clear, loadStatus, show, hide, ask }
})
