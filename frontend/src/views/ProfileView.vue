<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { showToast } from 'vant'
import { getPreference, getTasteProfile } from '../api/user'
import type { TastePreference, TasteProfileItem } from '../api/user'

const preference = ref<TastePreference | null>(null)
const profile = ref<TasteProfileItem[]>([])
const loading = ref(true)
const error = ref(false)

async function load() {
  loading.value = true
  error.value = false
  try {
    const [preferences, tasteProfile] = await Promise.all([getPreference(), getTasteProfile()])
    preference.value = preferences
    profile.value = tasteProfile
  } catch (reason) {
    error.value = true
    if (reason instanceof Error) showToast(reason.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="page page-narrow">
    <header class="page-heading">
      <div>
        <span class="eyebrow">YOUR FLAVOR PROFILE</span>
        <h1>我的口味画像</h1>
        <p>根据你填写的问卷整理而成，之后可以随时调整。</p>
      </div>
      <RouterLink
        class="button secondary small"
        to="/survey"
      >
        修改问卷
      </RouterLink>
    </header>
    <div
      v-if="loading"
      class="loading-note"
    >
      正在读取你的口味画像…
    </div>
    <section
      v-else-if="error"
      class="surface empty-state"
    >
      <span class="emoji">🌱</span>
      <h2>先告诉我们你的口味吧</h2>
      <p>完成一份简单问卷，就能生成你的口味画像。</p>
      <RouterLink
        class="button small"
        to="/survey"
      >
        填写口味问卷
      </RouterLink>
    </section>
    <template v-else-if="preference">
      <section class="profile-summary surface">
        <span class="eyebrow">TASTE NOTE</span>
        <p>{{ preference.profileSummary || '还没有形成口味摘要。' }}</p>
      </section>
      <section class="survey-section surface">
        <h2>偏好权重</h2>
        <div
          v-if="profile.length"
          class="profile-list"
        >
          <article
            v-for="item in profile"
            :key="item.tagId"
            class="profile-row"
          >
            <div>
              <strong>{{ item.tagName }}</strong><small>{{ item.weight > 0 ? '喜欢' : '忌口' }} ·
                {{ item.source === 1 ? '来自口味问卷' : '来自推荐反馈' }}</small>
            </div>
            <span :class="item.weight > 0 ? 'weight-positive' : 'weight-negative'">{{ item.weight > 0 ? '+' : '' }}{{ item.weight }}</span>
          </article>
        </div>
        <div
          v-else
          class="empty-hint"
        >
          尚未选择口味标签，可以回到问卷补充。
        </div>
      </section>
      <section class="survey-section surface">
        <h2>问卷设置</h2>
        <div class="profile-facts">
          <span>辣度上限
            <strong>{{
              ['不吃辣', '微辣', '中辣', '重辣'][preference.maxSpiceLevel]
            }}</strong></span><span>饮食类型
            <strong>{{ ['不限', '素食', '清真', '其他要求'][preference.dietType] }}</strong></span><span>单餐预算
            <strong>{{ preference.budgetMin ?? 0 }} – {{ preference.budgetMax ?? '不限' }} 元</strong></span>
        </div>
      </section>
    </template>
  </main>
</template>
