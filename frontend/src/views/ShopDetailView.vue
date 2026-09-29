<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCanteen, getShop, listDishes } from '../api/data'
import type { Canteen, Dish, Shop } from '../api/data'
import { useAuthStore } from '../stores/auth'
import { useFavorites } from '../composables/useFavorites'

const route = useRoute()
const shop = ref<Shop | null>(null)
const canteen = ref<Canteen | null>(null)
const dishes = ref<Dish[]>([])
const loading = ref(true)
const error = ref('')
const auth = useAuthStore()
const router = useRouter()
const shopFavorites = useFavorites(2, auth, router)
const dishFavorites = useFavorites(1, auth, router)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const id = Number(route.params.id)
    shop.value = await getShop(id)
    canteen.value = await getCanteen(shop.value.canteenId)
    const firstPage = await listDishes({ shopId: id, size: 100 })
    const pageCount = Math.ceil(firstPage.total / firstPage.size)
    const remainingPages = await Promise.all(
      Array.from({ length: Math.max(0, pageCount - 1) }, (_, index) =>
        listDishes({ shopId: id, page: index + 2, size: firstPage.size }),
      ),
    )
    dishes.value = [...firstPage.records, ...remainingPages.flatMap((page) => page.records)]
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '档口信息暂时无法加载'
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => route.params.id, load)
watch(
  () => auth.isLoggedIn,
  () => {
    void shopFavorites.refresh()
    void dishFavorites.refresh()
  },
  { immediate: true },
)
</script>

<template>
  <main class="page">
    <div
      v-if="loading"
      class="loading-note"
    >
      正在打开档口菜单…
    </div>
    <section
      v-else-if="error"
      class="surface empty-state"
    >
      <span class="emoji">🥲</span>
      <h2>档口信息暂时无法加载</h2>
      <p>{{ error }}</p>
      <RouterLink
        class="button small"
        to="/"
      >
        返回菜品列表
      </RouterLink>
    </section>
    <template v-else-if="shop">
      <RouterLink
        class="back-link"
        to="/"
      >
        ← 返回菜品发现
      </RouterLink>
      <header class="shop-hero surface">
        <div
          class="shop-illustration"
          aria-hidden="true"
        >
          🍳
        </div>
        <div>
          <RouterLink
            v-if="canteen"
            class="eyebrow canteen-breadcrumb"
            :to="`/canteens/${canteen.id}`"
          >
            {{ canteen.campus }} · {{ canteen.canteenName }} · 查看食堂
          </RouterLink>
          <h1>{{ shop.shopName }}</h1>
          <button
            class="button secondary small shop-favorite-button"
            :class="{ 'is-favorited': shopFavorites.isSaved(shop.id) }"
            :aria-pressed="shopFavorites.isSaved(shop.id)"
            :disabled="shopFavorites.isBusy(shop.id)"
            type="button"
            @click="shopFavorites.toggle(shop.id)"
          >
            {{ shopFavorites.isSaved(shop.id) ? '♥ 已收藏档口' : '♡ 收藏档口' }}
          </button>
          <p>{{ shop.description || shop.cuisine || '校内食堂档口' }}</p>
          <div class="dish-meta">
            <span
              v-if="shop.locationDesc"
              class="pill"
            >{{ shop.locationDesc }}</span><span
              v-if="shop.avgPrice"
              class="pill"
            >人均 ¥{{ Number(shop.avgPrice).toFixed(0) }}</span><span class="pill">{{ shop.status === 1 ? '营业中' : '暂停营业' }}</span>
          </div>
        </div>
      </header>
      <div class="result-line">
        <span>档口菜单</span><span>{{ dishes.length }} 道菜品</span>
      </div>
      <section
        v-if="dishes.length"
        class="dish-grid"
      >
        <article
          v-for="dish in dishes"
          :key="dish.id"
          class="dish-card"
        >
          <div class="dish-visual">
            <img
              v-if="dish.imageUrl"
              :src="dish.imageUrl"
              :alt="dish.dishName"
            ><span v-else>🍲</span>
          </div>
          <div class="dish-body">
            <div class="dish-topline">
              <h2>
                <RouterLink :to="`/dishes/${dish.id}`">
                  {{ dish.dishName }}
                </RouterLink>
              </h2>
              <div class="dish-card-actions">
                <span class="price">¥{{ Number(dish.price).toFixed(2) }}</span>
                <button
                  class="favorite-icon-button"
                  :class="{ saved: dishFavorites.isSaved(dish.id) }"
                  :aria-label="dishFavorites.isSaved(dish.id) ? '取消收藏' : '收藏菜品'"
                  :aria-pressed="dishFavorites.isSaved(dish.id)"
                  :disabled="dishFavorites.isBusy(dish.id)"
                  type="button"
                  @click.stop="dishFavorites.toggle(dish.id)"
                >
                  {{ dishFavorites.isSaved(dish.id) ? '♥' : '♡' }}
                </button>
              </div>
            </div>
            <p class="dish-description">
              {{ dish.description || '档口每日供应' }}
            </p>
          </div>
        </article>
      </section>
      <section
        v-else
        class="surface empty-state"
      >
        <span class="emoji">🍽️</span>
        <h2>菜单还在整理中</h2>
        <p>稍后再来看看这个档口。</p>
      </section>
    </template>
  </main>
</template>
