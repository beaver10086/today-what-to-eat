<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDish, getDishTags, listTags } from '../api/data'
import type { Dish, Tag } from '../api/data'
import { useAuthStore } from '../stores/auth'
import { useFavorites } from '../composables/useFavorites'

const route = useRoute()
const router = useRouter()
const dish = ref<Dish | null>(null)
const dishTagIds = ref<number[]>([])
const tags = ref<Tag[]>([])
const loading = ref(true)
const error = ref('')
const auth = useAuthStore()
const favorite = useFavorites(1, auth, router)
const categoryNames = ['主食', '荤菜', '素菜', '汤羹', '小吃', '饮品', '甜点']
const dishTags = computed(() => {
  const ids = new Set(dishTagIds.value)
  return tags.value.filter((tag) => ids.has(tag.id))
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const id = Number(route.params.id)
    const [item, tagIds, allTags] = await Promise.all([getDish(id), getDishTags(id), listTags()])
    dish.value = item
    dishTagIds.value = tagIds
    tags.value = allTags.records
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '菜品详情暂时无法加载'
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => route.params.id, load)
watch(() => auth.isLoggedIn, favorite.refresh, { immediate: true })
</script>

<template>
  <main class="page detail-page">
    <div
      v-if="loading"
      class="loading-note"
    >
      正在加载菜品详情…
    </div>
    <section
      v-else-if="error"
      class="surface empty-state"
    >
      <span class="emoji">🥲</span>
      <h1>菜品详情暂时无法加载</h1>
      <p>{{ error }}</p>
      <RouterLink
        class="button small"
        to="/"
      >
        返回菜品列表
      </RouterLink>
    </section>
    <template v-else-if="dish">
      <nav
        class="breadcrumbs"
        aria-label="当前位置"
      >
        <RouterLink to="/">
          全部菜品
        </RouterLink><span>›</span>
        <RouterLink :to="`/canteens/${dish.canteenId}`">
          {{ dish.canteenName }}
        </RouterLink><span>›</span>
        <RouterLink :to="`/shops/${dish.shopId}`">
          {{ dish.shopName }}
        </RouterLink>
      </nav>
      <article class="detail-card surface">
        <div class="detail-visual">
          <img
            v-if="dish.imageUrl"
            :src="dish.imageUrl"
            :alt="dish.dishName"
          >
          <span
            v-else
            aria-hidden="true"
          >{{
            ['🍜', '🥘', '🥗', '🍲', '🥟', '🧋', '🍮'][dish.category - 1] || '🍽️'
          }}</span>
        </div>
        <div class="detail-content">
          <span class="eyebrow">{{ dish.canteenName }} · {{ dish.shopName }}</span>
          <h1>{{ dish.dishName }}</h1>
          <strong class="detail-price">¥{{ Number(dish.price).toFixed(2) }}</strong>
          <p class="detail-description">
            {{ dish.description || '这道菜还没有添加详细介绍。' }}
          </p>
          <div class="dish-meta">
            <span class="pill">{{ categoryNames[dish.category - 1] }}</span>
            <span class="pill">{{
              ['不辣', '微辣', '中辣', '重辣'][dish.spiceLevel] || '不辣'
            }}</span>
            <span
              v-if="dish.calorie"
              class="pill"
            >约 {{ dish.calorie }} 千卡</span>
            <span
              v-if="dish.isSignature"
              class="pill"
            >招牌菜</span>
            <span
              v-for="tag in dishTags"
              :key="tag.id"
              class="pill"
            >{{ tag.tagName }}</span>
          </div>
          <div class="detail-actions">
            <button
              class="button"
              :class="{ secondary: favorite.isSaved(dish.id) }"
              type="button"
              :disabled="favorite.isBusy(dish.id)"
              @click="favorite.toggle(dish.id)"
            >
              {{ favorite.isSaved(dish.id) ? '♥ 已收藏' : '♡ 收藏这道菜' }}
            </button>
            <RouterLink
              class="button secondary"
              :to="`/shops/${dish.shopId}`"
            >
              查看档口菜单
            </RouterLink>
          </div>
        </div>
      </article>
    </template>
  </main>
</template>
