<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { showToast } from 'vant'
import { useRouter } from 'vue-router'
import { createRecommendation, getNextRecommendation, sendRecommendationFeedback } from '../api/recommendation'
import type { RecommendationResult } from '../api/recommendation'

const router = useRouter()
const mealType = ref(guessMeal())
const result = ref<RecommendationResult | null>(null)
const loading = ref(false)
const busyDishId = ref<number | null>(null)
const mealOptions = [
  { value: 1, label: '早餐' },
  { value: 2, label: '午餐' },
  { value: 3, label: '晚餐' },
  { value: 4, label: '夜宵' },
]
const mealName = computed(() => mealOptions.find((item) => item.value === mealType.value)?.label)

function guessMeal() {
  const hour = new Date().getHours()
  if (hour < 10) return 1
  if (hour < 15) return 2
  if (hour < 21) return 3
  return 4
}

async function loadRecommendation() {
  loading.value = true
  try {
    result.value = await createRecommendation({ mealType: mealType.value, limit: 5 })
  } catch (error) {
    const message = error instanceof Error ? error.message : '推荐暂时不可用'
    showToast(message)
    if (message.includes('口味问卷')) await router.push('/survey')
  } finally {
    loading.value = false
  }
}

async function feedback(dishId: number, feedbackType: 1 | 2 | 4) {
  if (!result.value) return
  busyDishId.value = dishId
  try {
    await sendRecommendationFeedback(result.value.id, { dishId, feedbackType })
    showToast(feedbackType === 1 ? '已记录喜欢，画像已更新' : feedbackType === 2 ? '已记录偏好' : '已记为吃过')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '操作失败')
  } finally {
    busyDishId.value = null
  }
}

async function nextGroup(dishId: number) {
  if (!result.value) return
  loading.value = true
  try {
    result.value = await getNextRecommendation(result.value.id, dishId)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '暂时没有更多菜品')
  } finally {
    loading.value = false
  }
}

onMounted(loadRecommendation)
</script>

<template>
  <main class="recommendation-page">
    <section class="recommendation-hero">
      <div>
        <p class="eyebrow">
          PERSONAL PICKS · 今日灵感
        </p>
        <h1>这一餐，交给好胃口</h1>
        <p>根据你的预算、口味和饮食偏好，为你挑出合适的校园菜品。</p>
      </div>
      <div class="meal-picker">
        <label for="meal-type">用餐时间</label>
        <select
          id="meal-type"
          v-model.number="mealType"
        >
          <option
            v-for="option in mealOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>
        <button
          class="primary-button"
          :disabled="loading"
          @click="loadRecommendation"
        >
          {{ loading ? '正在挑选…' : '重新推荐' }}
        </button>
      </div>
    </section>

    <section
      v-if="result"
      class="recommendation-results"
    >
      <header class="results-heading">
        <div>
          <span class="eyebrow">{{ mealName }} · 为你精选</span>
          <h2>今天可以试试这些</h2>
        </div>
        <span class="result-count">{{ result.items.length }} 道推荐</span>
      </header>
      <div
        v-if="result.items.length"
        class="recommendation-grid"
      >
        <article
          v-for="(item, index) in result.items"
          :key="item.dish.id"
          class="recommendation-card"
        >
          <div
            class="recommendation-image"
            :class="`tone-${index % 4}`"
          >
            <img
              v-if="item.dish.imageUrl"
              :src="item.dish.imageUrl"
              :alt="item.dish.dishName"
            >
            <span v-else>{{ item.dish.dishName.slice(0, 1) }}</span>
            <b>NO. {{ String(index + 1).padStart(2, '0') }}</b>
          </div>
          <div class="recommendation-body">
            <div class="dish-title-row">
              <h3>{{ item.dish.dishName }}</h3>
              <strong>¥{{ Number(item.dish.price).toFixed(2) }}</strong>
            </div>
            <p class="recommendation-location">
              {{ item.dish.canteenName }} · {{ item.dish.shopName }}
            </p>
            <p class="recommendation-reason">
              {{ item.reason }}
            </p>
            <div class="recommendation-meta">
              <span>★ {{ Number(item.dish.rating || 0).toFixed(1) }}</span>
              <span>{{ item.dish.spiceLevel === 0 ? '不辣' : `辣度 ${item.dish.spiceLevel}` }}</span>
              <span>符合你的口味设置</span>
            </div>
            <div class="recommendation-actions">
              <button
                :disabled="busyDishId === item.dish.id"
                @click="feedback(item.dish.id, 1)"
              >
                喜欢
              </button>
              <button
                :disabled="busyDishId === item.dish.id"
                @click="feedback(item.dish.id, 2)"
              >
                不合口味
              </button>
              <button
                :disabled="loading"
                @click="nextGroup(item.dish.id)"
              >
                换一组
              </button>
            </div>
          </div>
        </article>
      </div>
      <div
        v-else
        class="recommendation-empty"
      >
        <h3>这次没找到合适的菜</h3>
        <p>可以放宽预算、辣度或饮食限制后重新推荐。</p>
        <RouterLink to="/survey">
          调整口味问卷
        </RouterLink>
      </div>
    </section>
    <section
      v-else-if="loading"
      class="recommendation-loading"
    >
      正在结合你的口味挑选菜品…
    </section>
  </main>
</template>
