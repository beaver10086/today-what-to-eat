<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { showToast } from 'vant'
import { useRouter } from 'vue-router'
import {
  getRandomDish,
  listCanteens,
  listDishes,
  listShops,
  listTags,
  searchByNaturalLanguage,
} from '../api/data'
import type { Canteen, Dish, DishFilters, SearchIntent, Shop, Tag } from '../api/data'
import { useAuthStore } from '../stores/auth'
import { useFavorites } from '../composables/useFavorites'

const categories = ['主食', '荤菜', '素菜', '汤羹', '小吃', '饮品', '甜点']
const canteens = ref<Canteen[]>([])
const shops = ref<Shop[]>([])
const tags = ref<Tag[]>([])
const auth = useAuthStore()
const router = useRouter()
const favorites = useFavorites(1, auth, router)
const dishes = ref<Dish[]>([])
const total = ref(0)
const pickingRandom = ref(false)
const state = ref<'loading' | 'ready' | 'empty' | 'error'>('loading')
const naturalQuery = ref('')
const naturalIntent = ref<SearchIntent | null>(null)
const naturalNotice = ref('')
const naturalLoading = ref(false)
const filters = reactive({
  canteenId: '',
  shopId: '',
  category: '',
  spiceLevel: '',
  mealType: '',
  tagId: '',
  minPrice: '',
  maxPrice: '',
})
const page = ref(1)
const size = 12
const availableShops = computed(() =>
  filters.canteenId
    ? shops.value.filter((shop) => shop.canteenId === Number(filters.canteenId))
    : shops.value,
)

async function loadOptions() {
  try {
    const [canteenResult, shopResult, tagResult] = await Promise.all([
      listCanteens(),
      listShops(),
      listTags(),
    ])
    canteens.value = canteenResult.records
    shops.value = shopResult.records
    tags.value = tagResult.records
  } catch {
    showToast('筛选项加载失败，请稍后重试')
  }
}

async function search(reset = true) {
  if (reset) {
    page.value = 1
    naturalIntent.value = null
    naturalNotice.value = ''
  }
  state.value = 'loading'
  const params: DishFilters = { page: page.value, size }
  if (filters.canteenId) params.canteenId = Number(filters.canteenId)
  if (filters.shopId) params.shopId = Number(filters.shopId)
  if (filters.category) params.category = Number(filters.category)
  if (filters.spiceLevel) params.spiceLevel = Number(filters.spiceLevel)
  if (filters.mealType) params.mealType = Number(filters.mealType)
  if (filters.tagId) params.tagId = Number(filters.tagId)
  if (filters.minPrice) params.minPrice = Number(filters.minPrice)
  if (filters.maxPrice) params.maxPrice = Number(filters.maxPrice)
  try {
    const result = await listDishes(params)
    dishes.value = result.records || []
    total.value = result.total
    state.value = dishes.value.length ? 'ready' : 'empty'
  } catch (error) {
    state.value = 'error'
    showToast(error instanceof Error ? error.message : '菜品加载失败')
  }
}

async function searchNaturally(reset = true) {
  if (!naturalQuery.value.trim()) return
  if (reset) page.value = 1
  naturalLoading.value = true
  state.value = 'loading'
  try {
    const result = await searchByNaturalLanguage(naturalQuery.value.trim(), page.value)
    naturalIntent.value = result.conditions
    naturalNotice.value = result.notice
    dishes.value = result.result.records
    total.value = result.result.total
    state.value = dishes.value.length ? 'ready' : 'empty'
  } catch {
    state.value = 'error'
    naturalNotice.value = '自然语言筛选暂时不可用，请检查网络或重试。'
  } finally {
    naturalLoading.value = false
  }
}

async function pickRandomDish() {
  pickingRandom.value = true
  try {
    const params: DishFilters = {}
    if (filters.canteenId) params.canteenId = Number(filters.canteenId)
    if (filters.shopId) params.shopId = Number(filters.shopId)
    if (filters.category) params.category = Number(filters.category)
    if (filters.spiceLevel) params.spiceLevel = Number(filters.spiceLevel)
    if (filters.mealType) params.mealType = Number(filters.mealType)
    if (filters.tagId) params.tagId = Number(filters.tagId)
    if (filters.minPrice) params.minPrice = Number(filters.minPrice)
    if (filters.maxPrice) params.maxPrice = Number(filters.maxPrice)
    const dish = await getRandomDish(params)
    await router.push(`/dishes/${dish.id}`)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '暂时没有符合条件的菜品')
  } finally {
    pickingRandom.value = false
  }
}

function clearFilters() {
  naturalQuery.value = ''
  naturalIntent.value = null
  naturalNotice.value = ''
  Object.assign(filters, {
    canteenId: '',
    shopId: '',
    category: '',
    spiceLevel: '',
    mealType: '',
    tagId: '',
    minPrice: '',
    maxPrice: '',
  })
  void search()
}

function spiceLabel(level: number) {
  return ['不辣', '微辣', '中辣', '重辣'][level] ?? '不辣'
}

function mealLabel(mask: number) {
  const meals = [
    [1, '早餐'],
    [2, '午餐'],
    [4, '晚餐'],
    [8, '夜宵'],
  ] as const
  return meals
    .filter(([flag]) => (mask & flag) > 0)
    .map(([, label]) => label)
    .join(' · ')
}

function previousPage() {
  if (page.value > 1) {
    page.value -= 1
    void (naturalIntent.value ? searchNaturally(false) : search(false))
  }
}

function nextPage() {
  if (page.value < Math.ceil(total.value / size)) {
    page.value += 1
    void (naturalIntent.value ? searchNaturally(false) : search(false))
  }
}

onMounted(() => {
  void loadOptions()
  void search()
})
watch(() => auth.isLoggedIn, favorites.refresh, { immediate: true })
</script>

<template>
  <main class="page">
    <header class="page-heading">
      <div>
        <span class="eyebrow">A GOOD MEAL STARTS HERE</span>
        <h1>找一份合口味的</h1>
        <p>从校内食堂和档口中，筛出今天想吃的那一餐。</p>
      </div>
      <div
        class="plate-badge"
        aria-hidden="true"
      >
        🥢
      </div>
    </header>

    <nav class="discovery-nav" aria-label="菜品功能导航">
      <span class="discovery-nav-label">快速查看</span>
      <a href="#natural-search">一句话找菜</a>
      <a href="#locations">按地点查看</a>
      <a href="#filters">筛选菜品</a>
      <a href="#menu">全部菜品</a>
      <button type="button" @click="pickRandomDish">🎲 随机推荐</button>
      <RouterLink class="discovery-favorites-link" to="/favorites">我的收藏</RouterLink>
      <RouterLink v-if="auth.isAdmin" to="/admin">后台管理</RouterLink>
    </nav>

    <section
      class="natural-search surface"
      id="natural-search"
      aria-label="自然语言找菜"
    >
      <div>
        <span class="eyebrow">不用逐项筛选</span>
        <h2>说说你现在想吃什么</h2>
        <p>例如：20 块以内、不要辣、离宿舍近的。</p>
      </div>
      <form
        class="natural-search-form"
        @submit.prevent="searchNaturally()"
      >
        <input
          v-model="naturalQuery"
          aria-label="用一句话描述筛选条件"
          maxlength="500"
          placeholder="输入预算、口味、食堂或菜名…"
        >
        <button
          class="button"
          type="submit"
          :disabled="naturalLoading"
        >
          {{ naturalLoading ? '正在理解…' : '帮我找菜' }}
        </button>
      </form>
      <p
        v-if="naturalNotice"
        class="natural-notice"
        aria-live="polite"
      >
        {{ naturalNotice }}
      </p>
      <div
        v-if="naturalIntent"
        class="intent-chips"
        aria-label="识别到的条件"
      >
        <span
          v-if="naturalIntent.budgetMax != null"
          class="pill"
        >预算 ≤ ¥{{ naturalIntent.budgetMax }}</span>
        <span
          v-if="naturalIntent.maxSpiceLevel != null"
          class="pill"
        >辣度 ≤ {{ ['不辣', '微辣', '中辣', '重辣'][naturalIntent.maxSpiceLevel] }}</span>
        <span
          v-if="naturalIntent.canteenKeyword"
          class="pill"
        >{{
          naturalIntent.canteenKeyword
        }}</span>
        <span
          v-if="naturalIntent.shopKeyword"
          class="pill"
        >{{ naturalIntent.shopKeyword }}</span>
        <span
          v-if="naturalIntent.mealType"
          class="pill"
        >{{
          { 1: '早餐', 2: '午餐', 4: '晚餐', 8: '夜宵' }[naturalIntent.mealType]
        }}</span>
        <span
          v-if="naturalIntent.keyword"
          class="pill"
        >关键词：{{ naturalIntent.keyword }}</span>
        <span
          v-if="naturalIntent.nearby"
          class="pill"
        >优先附近</span>
      </div>
    </section>

    <section
      v-if="canteens.length"
      class="canteen-strip"
      id="locations"
      aria-label="按食堂浏览"
    >
      <div class="canteen-strip-heading">
        <span class="eyebrow">按地点探索</span><span>食堂 · 门店 · 菜品</span>
      </div>
      <div class="canteen-strip-list">
        <RouterLink
          v-for="canteen in canteens"
          :key="canteen.id"
          :to="`/canteens/${canteen.id}`"
          class="canteen-chip"
        >
          <span aria-hidden="true">🏫</span><span><strong>{{ canteen.canteenName }}</strong><small>{{ canteen.campus }}</small></span><span aria-hidden="true">→</span>
        </RouterLink>
      </div>
    </section>

    <section
      class="filter-panel surface"
      id="filters"
      aria-label="菜品筛选"
    >
      <div class="field">
        <label for="canteen-filter">食堂</label>
        <select
          id="canteen-filter"
          v-model="filters.canteenId"
          @change="filters.shopId = ''"
        >
          <option value="">
            全部食堂
          </option>
          <option
            v-for="canteen in canteens"
            :key="canteen.id"
            :value="String(canteen.id)"
          >
            {{ canteen.canteenName }}
          </option>
        </select>
      </div>
      <div class="field">
        <label for="shop-filter">档口</label>
        <select
          id="shop-filter"
          v-model="filters.shopId"
        >
          <option value="">
            全部档口
          </option>
          <option
            v-for="shop in availableShops"
            :key="shop.id"
            :value="String(shop.id)"
          >
            {{ shop.shopName }}
          </option>
        </select>
      </div>
      <div class="field">
        <label for="category-filter">菜品分类</label>
        <select
          id="category-filter"
          v-model="filters.category"
        >
          <option value="">
            全部分类
          </option>
          <option
            v-for="(name, index) in categories"
            :key="name"
            :value="String(index + 1)"
          >
            {{ name }}
          </option>
        </select>
      </div>
      <div class="field">
        <label for="spice-filter">辣度上限</label>
        <select
          id="spice-filter"
          v-model="filters.spiceLevel"
        >
          <option value="">
            不限辣度
          </option>
          <option value="0">
            不辣
          </option>
          <option value="1">
            微辣及以下
          </option>
          <option value="2">
            中辣及以下
          </option>
          <option value="3">
            重辣及以下
          </option>
        </select>
      </div>
      <div class="field">
        <label for="meal-filter">用餐时段</label>
        <select
          id="meal-filter"
          v-model="filters.mealType"
        >
          <option value="">
            全部时段
          </option>
          <option value="1">
            早餐
          </option>
          <option value="2">
            午餐
          </option>
          <option value="4">
            晚餐
          </option>
          <option value="8">
            夜宵
          </option>
        </select>
      </div>
      <div class="field">
        <label for="tag-filter">口味标签</label>
        <select
          id="tag-filter"
          v-model="filters.tagId"
        >
          <option value="">
            全部标签
          </option>
          <option
            v-for="tag in tags"
            :key="tag.id"
            :value="String(tag.id)"
          >
            {{ tag.tagName }}
          </option>
        </select>
      </div>
      <div class="field">
        <label for="min-price">最低价格</label>
        <input
          id="min-price"
          v-model="filters.minPrice"
          type="number"
          min="0"
          step="0.5"
          placeholder="不限"
        >
      </div>
      <div class="field">
        <label for="max-price">最高价格</label>
        <input
          id="max-price"
          v-model="filters.maxPrice"
          type="number"
          min="0"
          step="0.5"
          placeholder="不限"
        >
      </div>
      <div class="filter-actions">
        <button
          class="button"
          type="button"
          @click="search()"
        >
          筛选菜品
        </button>
        <button
          class="button secondary"
          type="button"
          @click="clearFilters"
        >
          重置
        </button>
        <button
          class="button random-dish-button"
          :disabled="pickingRandom"
          type="button"
          @click="pickRandomDish"
        >
          {{ pickingRandom ? '正在挑选…' : '🎲 帮我随机挑一份' }}
        </button>
      </div>
    </section>

    <div class="result-line">
      <span id="menu">全部菜品 · 今日菜单</span><span>共 {{ total }} 道</span>
    </div>
    <div
      v-if="state === 'loading'"
      class="loading-note"
    >
      正在寻找合适的菜品…
    </div>
    <section
      v-else-if="state === 'error'"
      class="surface empty-state"
    >
      <span class="emoji">🥲</span>
      <h2>菜单暂时没有连上</h2>
      <p>检查网络后再试一次。</p>
      <button
        class="button small"
        type="button"
        @click="search(false)"
      >
        重新加载
      </button>
    </section>
    <section
      v-else-if="state === 'empty'"
      class="surface empty-state"
    >
      <span class="emoji">🍽️</span>
      <h2>没有找到匹配的菜品</h2>
      <p>放宽价格或口味条件，再看看吧。</p>
      <button
        class="button secondary small"
        type="button"
        @click="clearFilters"
      >
        清除筛选
      </button>
    </section>
    <section
      v-else
      class="dish-grid"
      aria-label="菜品列表"
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
          >
          <span
            v-else
            aria-hidden="true"
          >{{
            ['🍜', '🥘', '🥗', '🍲', '🥟', '🧋', '🍮'][dish.category - 1] || '🍽️'
          }}</span>
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
                :class="{ saved: favorites.isSaved(dish.id) }"
                :aria-label="favorites.isSaved(dish.id) ? '取消收藏' : '收藏菜品'"
                :aria-pressed="favorites.isSaved(dish.id)"
                :disabled="favorites.isBusy(dish.id)"
                type="button"
                @click.stop="favorites.toggle(dish.id)"
              >
                {{ favorites.isSaved(dish.id) ? '♥' : '♡' }}
              </button>
            </div>
          </div>
          <div class="dish-meta">
            <span class="pill">{{ categories[dish.category - 1] }}</span>
            <span class="pill">{{ spiceLabel(dish.spiceLevel) }}</span>
            <span
              v-if="dish.isSignature"
              class="pill"
            >招牌</span>
          </div>
          <p
            v-if="dish.description"
            class="dish-description"
          >
            {{ dish.description }}
          </p>
          <div class="dish-location">
            <RouterLink :to="`/shops/${dish.shopId}`">
              {{ dish.shopName }}
            </RouterLink>
            <RouterLink :to="`/canteens/${dish.canteenId}`">
              {{ dish.canteenName }}
            </RouterLink>
            <span>{{ mealLabel(dish.mealType) }}</span>
          </div>
        </div>
      </article>
    </section>
    <div
      v-if="state === 'ready' && total > size"
      class="pagination"
    >
      <button
        class="button secondary small"
        :disabled="page <= 1"
        type="button"
        @click="previousPage"
      >
        上一页
      </button>
      <span>第 {{ page }} 页 · 共 {{ Math.ceil(total / size) }} 页</span>
      <button
        class="button secondary small"
        :disabled="page >= Math.ceil(total / size)"
        type="button"
        @click="nextPage"
      >
        下一页
      </button>
    </div>
  </main>
</template>
