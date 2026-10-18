<script setup lang="ts">
import { computed, nextTick, onMounted, ref, useId, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import { useAssistantStore } from '@/stores/assistant.store'
import type { AssistantAnswer } from '@/api/assistant.api'

const props = defineProps<{ compact?: boolean; active?: boolean }>()
const session = useAssistantStore()
const auth = useAuthStore()
const route = useRoute()
const fieldId = useId()
const messages = ref<HTMLElement | null>(null)
const input = ref<HTMLTextAreaElement | null>(null)
const composing = ref(false)
const confirming = ref(false)
const privacy = ref<HTMLDetailsElement | null>(null)
const privacyOpen = ref(false)
const clearButton = ref<HTMLButtonElement | null>(null)
const confirmButton = ref<HTMLButtonElement | null>(null)
const copyNotice = ref('')
const newContent = ref(false)
const atBottom = ref(true)
const expandedSources = ref(new Set<string>())
const examples = ['站内有哪些科幻类图书？', '怎样安排每天的阅读时间？', '如何做一份有用的读书笔记？']
const statusText = computed(() => !auth.isAuthenticated ? '登录后开始对话' : session.loading ? '正在连接…' : session.statusError ? '连接失败' : session.status?.configured ? '可以提问' : '尚未配置')
const validBookId = (value: unknown): value is number => typeof value === 'number' && Number.isSafeInteger(value) && value > 0
function resizeInput() {
  if (!input.value) return
  input.value.style.height = 'auto'
  input.value.style.height = `${Math.min(input.value.scrollHeight, 128)}px`
}
function bottom() {
  if (messages.value) messages.value.scrollTop = messages.value.scrollHeight
  atBottom.value = true; newContent.value = false
}
function onScroll() {
  const el = messages.value
  if (el) atBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight < 48
  if (atBottom.value) newContent.value = false
}
function sourceToggle(event: Event, key: string) {
  const element = event.target as HTMLDetailsElement
  if (element.open) expandedSources.value.add(key); else expandedSources.value.delete(key)
  onScroll()
}
function fill(text: string) { session.draft = text; nextTick(() => input.value?.focus()) }
function submit() {
  if (composing.value || session.pending || !session.status?.configured || !session.draft.trim()) return
  void session.ask(); nextTick(bottom)
}
function keydown(event: KeyboardEvent) {
  if (event.key !== 'Enter' || event.shiftKey || event.isComposing || composing.value || event.keyCode === 229) return
  if (window.matchMedia('(pointer: coarse)').matches || window.innerWidth <= 600) return
  event.preventDefault(); submit()
}
function answerText(answer: AssistantAnswer) {
  return answer.status === 'NO_EVIDENCE' ? '本次未检索到足够相关的站内资料。可补充书名、作者或更具体的问题后再次提问。' : answer.answer
}
async function copy(answer: AssistantAnswer) {
  try {
    await navigator.clipboard.writeText([answerText(answer), answer.generalSupplement ? `通用知识补充\n${answer.generalSupplement}` : ''].filter(Boolean).join('\n\n'))
    copyNotice.value = '回答已复制'
  } catch { copyNotice.value = '复制失败，请选择回答文字手动复制' }
}
function clear() { session.clear(); confirming.value = false; copyNotice.value = ''; expandedSources.value.clear(); nextTick(() => input.value?.focus()) }
function showConfirmation() {
  if (privacy.value) privacy.value.open = false
  privacyOpen.value = false; confirming.value = true
  nextTick(() => confirmButton.value?.focus())
}
function cancelConfirmation() { confirming.value = false; nextTick(() => clearButton.value?.focus()) }
function privacyToggle() {
  privacyOpen.value = privacy.value?.open ?? false
  if (privacyOpen.value) confirming.value = false
}
function closeOverlay(event: KeyboardEvent) {
  // Native details updates `open` before its asynchronous toggle event fires.
  if (event.key !== 'Escape' || (!confirming.value && !privacy.value?.open)) return
  event.preventDefault(); event.stopPropagation()
  if (confirming.value) cancelConfirmation()
  else { if (privacy.value) privacy.value.open = false; privacyOpen.value = false; privacy.value?.querySelector('summary')?.focus() }
}
watch(() => session.draft, () => nextTick(resizeInput))
watch(() => session.revision, async () => {
  const follow = atBottom.value && expandedSources.value.size === 0
  await nextTick()
  if (follow) bottom(); else newContent.value = true
})
watch(() => session.version, () => { confirming.value = false; privacyOpen.value = false; if (privacy.value) privacy.value.open = false; copyNotice.value = ''; expandedSources.value.clear(); newContent.value = false })
watch(() => props.active, value => { if (value) nextTick(resizeInput) })
onMounted(() => { void session.loadStatus(); resizeInput(); bottom() })
</script>

<template>
  <section class="assistant-chat" :class="{ compact }" aria-label="AI 阅读助手对话" @keydown="closeOverlay">
    <div class="assistant-status" role="status"><span :class="{ ready: session.status?.configured }">{{ statusText }}</span><span>优先查找站内图书，也可回答阅读相关问题</span></div>
    <div ref="messages" class="assistant-messages" tabindex="0" aria-label="对话记录" @scroll="onScroll">
      <div v-if="!auth.isAuthenticated" class="assistant-empty">
        <p class="assistant-eyebrow">一起读懂一本书</p><h3>从一个好问题开始</h3>
        <p>登录后可向 AI 阅读助手提问。</p>
        <RouterLink class="button" :to="{ path: '/login', query: { redirect: route.fullPath } }" @click="session.hide(false)">登录后提问</RouterLink>
      </div>
      <template v-else>
        <div v-if="!session.history.length" class="assistant-empty">
          <p class="assistant-eyebrow">问 · 读 · 理解</p><h3>今天想读懂什么？</h3>
          <p>找一本书，或聊聊阅读的方法。</p>
          <div class="assistant-examples" aria-label="示例问题"><button v-for="example in examples" :key="example" type="button" @click="fill(example)">{{ example }} <span aria-hidden="true">↗</span></button></div>
        </div>
        <article v-for="turn in session.history" :key="turn.id" class="conversation-turn">
          <div class="assistant-question"><span>你</span><p>{{ turn.question }}</p></div>
          <div class="assistant-response">
            <strong class="assistant-author">AI 阅读助手</strong>
            <p v-if="turn.pending" role="status">正在准备回答，请稍候。你可以收起面板。</p>
            <div v-else-if="turn.error" class="assistant-error"><p role="alert">本次提问未完成：{{ turn.error }}</p><button type="button" :disabled="session.pending" @click="fill(turn.question)">编辑后重发</button></div>
            <template v-else-if="turn.answer">
              <div class="answer-card">
                <p class="answer-label">{{ turn.answer.status === 'GENERAL_KNOWLEDGE' ? '通用知识 · 非站内资料 · 非实时联网' : turn.answer.status === 'ANSWERED' ? '站内资料' : turn.answer.status === 'UNVERIFIED' ? '回答未通过校验' : '站内资料不足' }}</p>
                <p class="answer-text">{{ answerText(turn.answer) }}</p>
                <section v-if="turn.answer.generalSupplement && turn.answer.status !== 'UNVERIFIED'" class="assistant-supplement"><h4>通用知识补充</h4><p class="answer-text">{{ turn.answer.generalSupplement }}</p><p>此部分来自模型通用知识，未经站内引文验证。</p></section>
              </div>
              <div v-if="turn.answer.relatedBooks?.length" class="assistant-books"><h4>相关馆藏 <span>· {{ turn.answer.relatedBooks.length }} 本</span></h4><p>相关书目不等于已验证引文。</p><div v-for="(book, index) in turn.answer.relatedBooks" :key="index" class="assistant-book"><strong>{{ book.title }}</strong><RouterLink v-if="validBookId(book.bookId)" :to="`/books/${book.bookId}`" @click="session.hide(false)">查看站内详情 →</RouterLink><span v-else>详情暂不可用</span></div></div>
              <div v-if="turn.answer.citations.length" class="source-list"><h4>引用依据 · {{ turn.answer.citations.length }} 条</h4>
                <details v-for="(source, index) in turn.answer.citations" :key="source.sourceId" class="source-card" @toggle="sourceToggle($event, `${turn.id}:${source.sourceId}`)">
                  <summary><span class="source-number">{{ index + 1 }}</span><span>{{ source.title }}<small>{{ source.bookId != null ? '站内图书' : '项目资料' }}</small></span><span class="source-toggle">原文</span></summary>
                  <div class="source-body"><p>资料编号：{{ source.sourceId }}</p><h5>逐字引文</h5><blockquote>{{ source.quote }}</blockquote><h5>资料摘录</h5><p class="source-excerpt">{{ source.excerpt }}</p><RouterLink v-if="validBookId(source.bookId)" class="book-link" :to="`/books/${source.bookId}`" @click="session.hide(false)">查看站内详情：{{ source.title }}</RouterLink><p v-else-if="source.bookId != null">详情暂不可用</p></div>
                </details>
              </div>
              <p class="assistant-review">{{ turn.answer.status === 'UNVERIFIED' ? '未展示未通过核对的模型内容，可编辑问题后再试。' : '模型回答可能有误，请结合原文判断。图书简介不等于全文。' }}</p>
              <div class="assistant-answer-actions"><button type="button" @click="copy(turn.answer)">复制回答</button><button v-if="['UNVERIFIED', 'INSUFFICIENT', 'NO_EVIDENCE'].includes(turn.answer.status)" type="button" :disabled="session.pending" @click="fill(turn.question)">编辑问题</button></div>
            </template>
          </div>
        </article>
      </template>
    </div>
    <div class="assistant-composer">
      <button v-if="(newContent || !atBottom) && !confirming && !privacyOpen" type="button" class="assistant-new" @click="bottom">{{ newContent ? '有新内容 · 回到底部 ↓' : '回到底部 ↓' }}</button>
      <p v-if="copyNotice" class="assistant-feedback" role="status">{{ copyNotice }}</p>
      <template v-if="auth.isAuthenticated">
        <p v-if="session.statusError" class="assistant-error" role="alert">{{ session.statusError }} <button type="button" @click="session.loadStatus(true)">重新连接</button></p>
        <p v-else-if="!session.loading && session.status && !session.status.configured" role="status">助手尚未配置，请联系项目维护者。<button type="button" @click="session.loadStatus(true)">重新连接</button></p>
        <form @submit.prevent="submit">
          <label :for="`${fieldId}-question`">你的问题</label>
          <textarea :id="`${fieldId}-question`" ref="input" v-model="session.draft" rows="1" maxlength="1000" placeholder="输入书名，或聊聊阅读…" @keydown="keydown" @compositionstart="composing = true" @compositionend="composing = false" />
          <div class="assistant-send-row"><span>{{ session.draft.length }}/1000<span class="assistant-key-hint"> · Shift+Enter 换行</span></span><button type="submit" class="button" :disabled="session.loading || session.pending || !session.status?.configured || !session.draft.trim() || session.draft.length > 1000">{{ session.pending ? '回答中…' : '发送' }}</button></div>
        </form>
      </template>
      <div class="assistant-footnote"><details ref="privacy" @toggle="privacyToggle"><summary>隐私与回答边界</summary><p>当前问题和命中的公开资料会发送给 DeepSeek，请勿输入密码、密钥或个人信息。仅本次问题参与生成，不包含历史对话或隐含页面内容。对话只在内存保留，刷新、清空或退出后移除。通用知识非实时联网结果。</p></details><button v-if="session.history.length && !confirming" ref="clearButton" type="button" @click="showConfirmation">清空对话</button></div>
      <div v-if="confirming" class="assistant-confirm" role="group" aria-label="确认清空对话"><p>清空本次对话？在途请求仍可能计费，返回内容将丢弃。</p><button ref="confirmButton" type="button" @click="clear">确认清空</button><button type="button" @click="cancelConfirmation">保留对话</button></div>
    </div>
  </section>
</template>

<style scoped>
.assistant-chat { display: flex; flex-direction: column; height: 100%; min-height: 0; color: var(--color-text); background: var(--color-canvas); font-size: 16px; line-height: 1.65; }
.assistant-chat * { box-sizing: border-box; }
.assistant-chat button, .assistant-chat summary, .assistant-chat a { min-height: 44px; touch-action: manipulation; }
.assistant-chat button:not(.button) { font: inherit; color: var(--color-primary-strong); background: transparent; border: 1px solid var(--color-border); border-radius: 8px; padding: 6px 10px; cursor: pointer; }
.assistant-chat button:disabled { opacity: .65; cursor: default; }
.assistant-chat :is(button, a, summary, textarea, [tabindex]):focus-visible { outline: 3px solid var(--color-primary); outline-offset: 2px; }
.assistant-status { flex: none; display: flex; flex-wrap: wrap; gap: 2px 10px; padding: 8px 18px; border-bottom: 1px solid var(--color-border); color: var(--color-text-soft); font-size: 12px; }
.assistant-status span:first-child { font-weight: 700; }
.ready { color: var(--color-primary-strong); }
.assistant-messages { flex: 1; min-height: 0; overflow-y: auto; overscroll-behavior: contain; overflow-anchor: none; padding: 20px 18px; scrollbar-gutter: stable; }
.assistant-empty h3 { margin: 4px 0; font: 700 1.5rem var(--font-reading); }
.assistant-empty p { margin: 8px 0; }
.assistant-eyebrow, .answer-label { font-size: 13px; color: var(--color-primary-strong); font-weight: 700; }
.assistant-examples { display: grid; gap: 8px; margin-top: 20px; }
.assistant-examples button { display: flex; justify-content: space-between; gap: 8px; text-align: left; }
.assistant-examples button:hover { background: var(--color-primary-soft); }
.conversation-turn { margin-bottom: 28px; overflow-wrap: anywhere; }
.assistant-question { margin: 0 0 18px 28px; padding: 12px 16px; border-radius: 14px 14px 3px 14px; background: var(--color-primary-soft); }
.assistant-question span { font-size: 12px; color: var(--color-primary-strong); }
.assistant-question p { margin: 2px 0; white-space: pre-wrap; }
.assistant-author { font-family: var(--font-reading); }
.answer-label { margin: 8px 0; }
.answer-text { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.85; margin: 0; }
.assistant-supplement { margin-top: 18px; padding-top: 12px; border-top: 1px dashed var(--color-primary); }
.assistant-chat h4 { margin: 8px 0; font-size: 15px; }
.assistant-supplement > p:last-child, .assistant-books > p, .assistant-review { font-size: 12px; color: var(--color-text-soft); }
.assistant-books { margin-top: 18px; }
.assistant-book { padding: 10px 12px; margin: 6px 0; border: 1px solid var(--color-border); border-radius: 8px; background: var(--color-surface); }
.assistant-book strong { display: block; }
.assistant-book a, .book-link { display: flex; align-items: center; color: var(--color-primary-strong); }
.source-list { margin-top: 18px; }
.source-card { margin: 8px 0; border: 1px solid var(--color-border); border-radius: 8px; background: var(--color-surface); }
.source-card summary { display: flex; align-items: center; gap: 10px; padding: 8px 12px; cursor: pointer; }
.source-number { background: var(--color-primary-soft); border-radius: 50%; min-width: 28px; text-align: center; }
.source-card small { display: block; color: var(--color-text-soft); font-size: 12px; }
.source-toggle { margin-left: auto; flex: none; font-size: 12px; }
.source-body { padding: 12px; border-top: 1px solid var(--color-border); font-size: 14px; }
.source-body h5 { margin: 10px 0; font-size: 14px; }
.source-body blockquote { margin: 0; padding: 8px 12px; background: var(--color-primary-soft); border-left: 3px solid var(--color-primary); white-space: pre-wrap; }
.source-excerpt { white-space: pre-wrap; }
.assistant-answer-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.assistant-composer { flex: none; position: relative; padding: 10px 16px max(8px, env(safe-area-inset-bottom)); border-top: 1px solid var(--color-border); background: var(--color-surface); }
.assistant-composer form label { display: block; font-size: 13px; font-weight: 700; }
.assistant-composer textarea { display: block; width: 100%; min-height: 44px; max-height: min(128px, 22dvh); resize: none; border: 1px solid var(--color-border); border-radius: 8px; background: var(--color-canvas); color: var(--color-text); padding: 8px 10px; font: inherit; line-height: 1.5; }
.assistant-composer textarea::placeholder { color: var(--color-text-soft); opacity: 1; }
.assistant-send-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-top: 6px; }
.assistant-send-row > span { color: var(--color-text-soft); font-size: 12px; }
.assistant-send-row .button { min-width: 80px; }
.assistant-footnote { display: flex; gap: 8px; align-items: start; font-size: 12px; color: var(--color-text-soft); }
.assistant-footnote details { flex: 1; min-width: 0; }
.assistant-footnote summary { cursor: pointer; padding-top: 10px; }
.assistant-footnote p { position: absolute; bottom: calc(100% + 4px); left: 12px; right: 12px; max-height: min(160px, 35dvh); overflow-y: auto; margin: 0; padding: 12px; border: 1px solid var(--color-border); border-radius: 8px; background: var(--color-surface); box-shadow: var(--shadow-md); }
.assistant-footnote > button { flex: none; border-color: transparent !important; }
.assistant-new { position: absolute; bottom: calc(100% + 8px); left: 50%; transform: translateX(-50%); white-space: nowrap; background: var(--color-surface) !important; box-shadow: var(--shadow-md); }
.assistant-feedback { margin: 0; font-size: 13px; }
.assistant-error { color: var(--color-danger); overflow-wrap: anywhere; }
.assistant-confirm { position: absolute; bottom: calc(100% + 4px); left: 12px; right: 12px; max-height: min(160px, 35dvh); overflow-y: auto; background: var(--color-surface); box-shadow: var(--shadow-md); padding: 8px; border: 1px solid var(--color-border); border-radius: 8px; font-size: 13px; }
.assistant-confirm p { margin: 0 0 8px; }
.assistant-confirm button + button { margin-left: 8px; }
@media (max-width: 600px), (max-height: 500px) { .assistant-key-hint { display: none; } .assistant-messages { padding: 14px 12px; } .assistant-composer { padding-inline: 12px; } .assistant-status { padding: 5px 12px; } }
@media (prefers-reduced-motion: reduce) { .assistant-chat *, .assistant-chat *::before { scroll-behavior: auto !important; transition: none !important; animation: none !important; } }
</style>
