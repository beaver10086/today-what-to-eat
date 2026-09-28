<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { listTags } from '../api/data'
import type { Tag } from '../api/data'
import { getPreference, savePreference } from '../api/user'
import type { TastePreference } from '../api/user'

const router = useRouter()
const tags = ref<Tag[]>([])
const loading = ref(true)
const saving = ref(false)
const form = reactive<TastePreference>({
  maxSpiceLevel: 2,
  budgetMin: undefined,
  budgetMax: undefined,
  likeTagIds: [],
  dislikeTagIds: [],
  dietType: 0,
})
const tagGroups = computed(() =>
  [1, 2, 3, 5]
    .map((type) => ({
      type,
      label: { 1: '口味偏好', 2: '菜系偏好', 3: '喜欢的食材', 5: '用餐特点' }[
        type as 1 | 2 | 3 | 5
      ],
      items: tags.value.filter((tag) => tag.tagType === type),
    }))
    .filter((group) => group.items.length),
)

function toggleTag(id: number, group: 'likeTagIds' | 'dislikeTagIds') {
  const other = group === 'likeTagIds' ? 'dislikeTagIds' : 'likeTagIds'
  form[other] = form[other].filter((tagId) => tagId !== id)
  form[group] = form[group].includes(id)
    ? form[group].filter((tagId) => tagId !== id)
    : [...form[group], id]
}

async function load() {
  loading.value = true
  try {
    const tagPage = await listTags()
    tags.value = tagPage.records
    try {
      const preference = await getPreference()
      Object.assign(form, preference)
    } catch {
      // A missing questionnaire means this is a new user; keep the defaults.
    }
  } catch (error) {
    showToast(error instanceof Error ? error.message : '标签加载失败')
  } finally {
    loading.value = false
  }
}

async function save() {
  if (
    form.budgetMin != null &&
    form.budgetMax != null &&
    Number(form.budgetMin) > Number(form.budgetMax)
  ) {
    showToast('预算下限不能高于上限')
    return
  }
  saving.value = true
  try {
    const result = await savePreference({
      ...form,
      budgetMin:
        form.budgetMin === undefined || form.budgetMin === null
          ? undefined
          : Number(form.budgetMin),
      budgetMax:
        form.budgetMax === undefined || form.budgetMax === null
          ? undefined
          : Number(form.budgetMax),
    })
    Object.assign(form, result)
    showToast('口味问卷已保存')
    await router.push('/profile')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="page page-narrow">
    <header class="page-heading">
      <div>
        <span class="eyebrow">A LITTLE ABOUT YOUR TASTE</span>
        <h1>你的口味，由你做主</h1>
        <p>选一选喜欢和避开的食物，之后还可以随时修改。</p>
      </div>
    </header>
    <div
      v-if="loading"
      class="loading-note"
    >
      正在准备你的问卷…
    </div>
    <template v-else>
      <section class="survey-section surface">
        <h2>能接受多辣？</h2>
        <div class="option-row">
          <button
            v-for="(label, index) in ['不吃辣', '微辣', '中辣', '重辣']"
            :key="label"
            type="button"
            class="choice"
            :class="form.maxSpiceLevel === index ? 'active' : ''"
            @click="form.maxSpiceLevel = index"
          >
            {{ label }}
          </button>
        </div>
      </section>
      <section class="survey-section surface">
        <h2>一餐预算</h2>
        <div class="survey-grid">
          <div class="field">
            <label for="budget-min">最低（元）</label><input
              id="budget-min"
              v-model.number="form.budgetMin"
              type="number"
              min="0"
              step="1"
              placeholder="不限"
            >
          </div>
          <div class="field">
            <label for="budget-max">最高（元）</label><input
              id="budget-max"
              v-model.number="form.budgetMax"
              type="number"
              min="0"
              step="1"
              placeholder="不限"
            >
          </div>
        </div>
      </section>
      <section class="survey-section surface">
        <h2>饮食类型</h2>
        <div class="option-row">
          <button
            v-for="option in [
              { label: '不限', value: 0 },
              { label: '素食', value: 1 },
              { label: '清真', value: 2 },
              { label: '其他要求', value: 3 },
            ]"
            :key="option.value"
            class="choice"
            :class="form.dietType === option.value ? 'active' : ''"
            type="button"
            @click="form.dietType = option.value"
          >
            {{ option.label }}
          </button>
        </div>
      </section>
      <section
        v-for="group in tagGroups"
        :key="group.type"
        class="survey-section surface"
      >
        <h2>{{ group.label }}</h2>
        <div class="option-row">
          <button
            v-for="tag in group.items"
            :key="tag.id"
            class="choice"
            :class="form.likeTagIds.includes(tag.id) ? 'active' : ''"
            type="button"
            @click="toggleTag(tag.id, 'likeTagIds')"
          >
            {{ tag.tagName }}
          </button>
        </div>
      </section>
      <section class="survey-section surface">
        <h2>有什么不吃或需要避开？</h2>
        <p class="help-copy">
          选择忌口标签，菜品筛选和画像会记住你的选择。
        </p>
        <div class="option-row">
          <button
            v-for="tag in tags.filter((item) => item.tagType === 4)"
            :key="tag.id"
            class="choice"
            :class="form.dislikeTagIds.includes(tag.id) ? 'active' : ''"
            type="button"
            @click="toggleTag(tag.id, 'dislikeTagIds')"
          >
            {{ tag.tagName }}
          </button>
        </div>
      </section>
      <div class="survey-submit">
        <button
          class="button"
          type="button"
          :disabled="saving"
          @click="save"
        >
          {{ saving ? '正在保存…' : '保存我的口味' }}
        </button>
      </div>
    </template>
  </main>
</template>
