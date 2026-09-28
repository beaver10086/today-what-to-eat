<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { showToast } from 'vant'
import { listFavorites, removeFavorite } from '../api/favorites'
import type { FavoriteItem } from '../api/favorites'

const selectedType = ref<0 | 1 | 2>(0)
const items = ref<FavoriteItem[]>([])
const total = ref(0)
const loading = ref(true)
const removingId = ref<number | null>(null)
const filteredItems = computed(() => items.value)

async function load() {
  loading.value = true
  try {
    const page = await listFavorites({
      ...(selectedType.value ? { targetType: selectedType.value } : {}),
      page: 1,
      size: 100,
    })
    items.value = page.records
    total.value = page.total
  } catch (error) {
    showToast(error instanceof Error ? error.message : '收藏列表加载失败')
  } finally {
    loading.value = false
  }
}

async function remove(item: FavoriteItem) {
  removingId.value = item.favoriteId
  try {
    await removeFavorite(item.targetType, item.targetId)
    items.value = items.value.filter((saved) => saved.favoriteId !== item.favoriteId)
    total.value = Math.max(0, total.value - 1)
    showToast('已取消收藏')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '取消收藏失败')
  } finally {
    removingId.value = null
  }
}

watch(selectedType, load)
onMounted(load)
</script>

<template>
  <main class="page favorites-page">
    <header class="page-heading">
      <div>
        <span class="eyebrow">YOUR SAVED MENU</span>
        <h1>我的收藏</h1>
        <p>把喜欢的菜品和档口收好，下次想吃时更快找到。</p>
      </div>
      <span class="favorites-total">{{ total }} 项收藏</span>
    </header>

    <nav
      class="favorite-tabs"
      aria-label="收藏类型"
    >
      <button
        :class="{ active: selectedType === 0 }"
        @click="selectedType = 0"
      >
        全部
      </button>
      <button
        :class="{ active: selectedType === 1 }"
        @click="selectedType = 1"
      >
        菜品
      </button>
      <button
        :class="{ active: selectedType === 2 }"
        @click="selectedType = 2"
      >
        档口
      </button>
    </nav>

    <div
      v-if="loading"
      class="loading-note"
    >
      正在整理你的收藏…
    </div>
    <section
      v-else-if="!filteredItems.length"
      class="surface empty-state"
    >
      <span class="emoji">♡</span>
      <h2>这里还空着</h2>
      <p>在菜品列表、推荐或档口页面点一下收藏，就会保存在这里。</p>
      <RouterLink
        class="button small"
        to="/"
      >
        去发现菜品
      </RouterLink>
    </section>
    <section
      v-else
      class="favorites-grid"
      aria-label="收藏列表"
    >
      <article
        v-for="item in filteredItems"
        :key="item.favoriteId"
        class="favorite-card"
      >
        <RouterLink
          class="favorite-card-main"
          :to="item.shopId ? `/shops/${item.shopId}` : '/'"
        >
          <div class="favorite-thumb">
            <img
              v-if="item.imageUrl"
              :src="item.imageUrl"
              :alt="item.targetName"
            >
            <span v-else>{{ item.targetType === 1 ? '🍽️' : '🏪' }}</span>
          </div>
          <div class="favorite-copy">
            <span class="favorite-kind">{{ item.targetType === 1 ? '菜品' : '档口' }}</span>
            <h2>{{ item.targetName }}</h2>
            <p>{{ item.subtitle }}</p>
            <strong v-if="item.price != null">{{ item.targetType === 1 ? '¥' : '人均 ¥' }}{{ Number(item.price).toFixed(2) }}</strong>
          </div>
        </RouterLink>
        <button
          class="favorite-remove"
          :disabled="removingId === item.favoriteId"
          @click="remove(item)"
        >
          取消收藏
        </button>
      </article>
    </section>
  </main>
</template>
