<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getCanteen, getShop, listDishes } from '../api/data'
import type { Canteen, Dish, Shop } from '../api/data'

const route = useRoute()
const shop = ref<Shop | null>(null)
const canteen = ref<Canteen | null>(null)
const dishes = ref<Dish[]>([])
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const id = Number(route.params.id)
    shop.value = await getShop(id)
    canteen.value = await getCanteen(shop.value.canteenId)
    const menu = await listDishes({ shopId: id, size: 100 })
    dishes.value = menu.records
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '档口信息暂时无法加载'
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => route.params.id, load)
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
          <span class="eyebrow">{{ canteen?.campus }} · {{ canteen?.canteenName }}</span>
          <h1>{{ shop.shopName }}</h1>
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
              <h2>{{ dish.dishName }}</h2>
              <span class="price">¥{{ Number(dish.price).toFixed(2) }}</span>
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
