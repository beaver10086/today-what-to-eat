<script setup lang="ts">
import { ref } from 'vue'
import AsyncState from '../components/AsyncState.vue'

const state = ref<'loading' | 'empty' | 'error' | 'ready'>('empty')
const retry = () => {
  state.value = 'loading'
  globalThis.setTimeout(() => {
    state.value = 'empty'
  }, 250)
}
</script>

<template>
  <main class="home-page">
    <header class="hero">
      <span class="eyebrow">校园食堂 · 每日灵感</span>
      <h1>今天吃什么？</h1>
      <p>告诉我们你的口味，下一餐交给灵感。</p>
    </header>

    <section
      class="content-card"
      aria-label="今日推荐"
    >
      <div class="section-heading">
        <div>
          <span class="eyebrow">GOOD FOOD, GOOD MOOD</span>
          <h2>今日推荐</h2>
        </div>
        <span
          class="plate"
          aria-hidden="true"
        >🥗</span>
      </div>
      <AsyncState
        v-if="state !== 'ready'"
        :state="state"
        @retry="retry"
      />
      <p v-else>
        推荐内容即将上线
      </p>
    </section>
    <nav
      class="bottom-nav"
      aria-label="主导航"
    >
      <a
        class="active"
        href="/"
      >发现</a>
      <a href="#favorites">收藏</a>
      <a href="#profile">我的</a>
    </nav>
  </main>
</template>
