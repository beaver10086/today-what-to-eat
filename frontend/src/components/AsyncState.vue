<script setup lang="ts">
withDefaults(
  defineProps<{
    state: 'loading' | 'empty' | 'error'
    message?: string
  }>(),
  { message: '' },
)

const emit = defineEmits<{ retry: [] }>()
</script>

<template>
  <div
    class="async-state"
    role="status"
    aria-live="polite"
  >
    <van-loading
      v-if="state === 'loading'"
      vertical
    >
      加载中
    </van-loading>
    <template v-else-if="state === 'empty'">
      <div
        class="state-icon"
        aria-hidden="true"
      >
        🍽️
      </div>
      <p>{{ message || '暂时还没有内容' }}</p>
    </template>
    <template v-else>
      <div
        class="state-icon"
        aria-hidden="true"
      >
        🥲
      </div>
      <p>{{ message || '加载失败，请稍后重试' }}</p>
      <van-button
        size="small"
        type="primary"
        @click="emit('retry')"
      >
        重试
      </van-button>
    </template>
  </div>
</template>
