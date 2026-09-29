<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getCanteen, listDishes, listShops } from '../api/data'
import type { Canteen, Dish, Shop } from '../api/data'

const route = useRoute()
const canteen = ref<Canteen | null>(null)
const shops = ref<Shop[]>([])
const dishes = ref<Dish[]>([])
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const id = Number(route.params.id)
    const [place, shopPage, dishPage] = await Promise.all([
      getCanteen(id),
      listShops(id),
      listDishes({ canteenId: id, size: 100 }),
    ])
    canteen.value = place
    shops.value = shopPage.records.filter((shop) => shop.canteenId === id)
    const pages = Math.ceil(dishPage.total / dishPage.size)
    const next = await Promise.all(
      Array.from({ length: Math.max(0, pages - 1) }, (_, i) =>
        listDishes({ canteenId: id, page: i + 2, size: dishPage.size }),
      ),
    )
    dishes.value = [...dishPage.records, ...next.flatMap((page) => page.records)]
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '食堂菜单暂时无法加载'
  } finally {
    loading.value = false
  }
}

function shopDishes(shopId: number) {
  return dishes.value.filter((dish) => dish.shopId === shopId)
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
      正在整理食堂菜单…
    </div>
    <section
      v-else-if="error"
      class="surface empty-state"
    >
      <h1>食堂菜单暂时无法加载</h1>
      <p>{{ error }}</p>
    </section>
    <template v-else-if="canteen">
      <RouterLink
        class="back-link"
        to="/"
      >
        ← 返回全部菜品
      </RouterLink>
      <header class="shop-hero surface canteen-hero">
        <div
          class="shop-illustration"
          aria-hidden="true"
        >
          🏫
        </div>
        <div>
          <span class="eyebrow">{{ canteen.campus }} · 校园食堂</span>
          <h1>{{ canteen.canteenName }}</h1>
          <p>{{ canteen.description || '按门店浏览本食堂在售菜品。' }}</p>
          <div class="dish-meta">
            <span
              v-if="canteen.location"
              class="pill"
            >{{ canteen.location }}</span>
            <span class="pill">{{ canteen.status === 1 ? '营业中' : '暂停营业' }}</span>
            <span
              v-if="canteen.openTime && canteen.closeTime"
              class="pill"
            >{{ canteen.openTime }}–{{ canteen.closeTime }}</span>
          </div>
        </div>
      </header>
      <div class="result-line">
        <span>门店与菜单</span><span>{{ shops.length }} 家门店 · {{ dishes.length }} 道菜品</span>
      </div>
      <section
        v-if="shops.length"
        class="canteen-shop-list"
      >
        <article
          v-for="shop in shops"
          :key="shop.id"
          class="canteen-shop surface"
        >
          <div class="canteen-shop-heading">
            <div>
              <span class="eyebrow">{{ shop.cuisine || '校园餐饮' }} · {{ shop.locationDesc || '门店' }}</span>
              <h2>
                <RouterLink :to="`/shops/${shop.id}`">
                  {{ shop.shopName }} <span aria-hidden="true">↗</span>
                </RouterLink>
              </h2>
              <p>{{ shop.description || '查看门店介绍和完整菜品。' }}</p>
            </div>
            <RouterLink
              class="button secondary small"
              :to="`/shops/${shop.id}`"
            >
              查看门店
            </RouterLink>
          </div>
          <div
            v-if="shopDishes(shop.id).length"
            class="dish-preview-list"
          >
            <RouterLink
              v-for="dish in shopDishes(shop.id).slice(0, 4)"
              :key="dish.id"
              :to="`/dishes/${dish.id}`"
            >
              <span>{{ dish.dishName }}</span><strong>¥{{ Number(dish.price).toFixed(2) }}</strong>
            </RouterLink>
            <RouterLink
              v-if="shopDishes(shop.id).length > 4"
              class="more-dishes"
              :to="`/shops/${shop.id}`"
            >
              查看其余 {{ shopDishes(shop.id).length - 4 }} 道菜品 →
            </RouterLink>
          </div>
          <p
            v-else
            class="muted-note"
          >
            这家门店暂无在售菜品。
          </p>
        </article>
      </section>
      <section
        v-else
        class="surface empty-state"
      >
        <h2>暂时没有门店信息</h2>
      </section>
    </template>
  </main>
</template>
