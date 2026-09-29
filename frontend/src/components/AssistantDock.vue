<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { askAssistant } from '../api/data'
import type { AssistantAnswer } from '../api/data'

const open = ref(false)
const text = ref('')
const loading = ref(false)
const error = ref('')
const conversation = ref<
  Array<{ role: 'user' | 'assistant'; text: string; result?: AssistantAnswer }>
>([])
const input = ref<{ focus: () => void } | null>(null)

async function submit() {
  const message = text.value.trim()
  if (!message || loading.value) return
  conversation.value.push({ role: 'user', text: message })
  text.value = ''
  error.value = ''
  loading.value = true
  await nextTick()
  try {
    const result = await askAssistant(message)
    conversation.value.push({ role: 'assistant', text: result.answer, result })
  } catch (reason) {
    text.value = message
    error.value = reason instanceof Error
      ? `${reason.message} 已保留你的问题，可以检查连接后重试。`
      : '助手暂时不可用，已保留你的问题，可以稍后重试。'
  } finally {
    loading.value = false
    await nextTick()
    const log = globalThis.document.querySelector('.assistant-messages')
    if (log) log.scrollTop = log.scrollHeight
    input.value?.focus()
  }
}

function toggle() {
  open.value = !open.value
  if (open.value) nextTick(() => input.value?.focus())
}
</script>

<template>
  <div class="assistant-dock">
    <section
      v-if="open"
      class="assistant-panel surface"
      aria-label="智能点餐助手"
    >
      <header class="assistant-heading">
        <div>
          <span class="eyebrow">DINING ASSISTANT</span>
          <h2>问问今天吃什么</h2>
        </div>
        <button
          class="assistant-close"
          type="button"
          aria-label="关闭助手"
          @click="toggle"
        >
          ×
        </button>
      </header>
      <div
        class="assistant-messages"
        aria-live="polite"
      >
        <p
          v-if="!conversation.length"
          class="assistant-welcome"
        >
          可以问我：二食堂有什么不辣的？黄焖鸡多少钱？推荐个适合聚餐的。
        </p>
        <article
          v-for="(message, index) in conversation"
          :key="index"
          class="assistant-message"
          :class="message.role"
        >
          <span>{{ message.text }}</span>
          <RouterLink
            v-for="dish in message.result?.dishes.slice(0, 4) || []"
            :key="dish.id"
            class="assistant-dish"
            :to="`/dishes/${dish.id}`"
            @click="open = false"
          >
            <span><strong>{{ dish.dishName }}</strong><small>{{ dish.canteenName }} · {{ dish.shopName }}</small></span>
            <b>¥{{ Number(dish.price).toFixed(2) }} →</b>
          </RouterLink>
        </article>
        <p
          v-if="loading"
          class="assistant-thinking"
        >
          正在查菜单…
        </p>
        <p
          v-if="error"
          class="assistant-error"
          role="alert"
        >
          {{ error }}
        </p>
      </div>
      <form
        class="assistant-form"
        @submit.prevent="submit"
      >
        <input
          ref="input"
          v-model="text"
          maxlength="500"
          aria-label="向点餐助手提问"
          placeholder="问菜品、价格或推荐…"
        >
        <button
          class="button"
          type="submit"
          :disabled="loading || !text.trim()"
        >
          发送
        </button>
      </form>
      <small class="assistant-footnote">回答来自当前菜品目录；价格和供应情况以门店为准。</small>
    </section>
    <button
      class="assistant-launcher"
      type="button"
      :aria-expanded="open"
      @click="toggle"
    >
      <span aria-hidden="true">{{ open ? '×' : '✦' }}</span>{{ open ? '收起' : '问助手' }}
    </button>
  </div>
</template>
