<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import {
  createCanteen,
  createDish,
  createShop,
  deleteCanteen,
  deleteDish,
  deleteShop,
  listCanteens,
  listAdminDishes,
  listShops,
  listTags,
  setDishTags,
  updateCanteen,
  updateDish,
  updateShop,
  createTag,
  deleteTag,
  getDishTags,
  importDishesCsv,
  updateTag,
} from '../api/data'
import type { Canteen, Dish, Shop, Tag } from '../api/data'

type Kind = 'canteen' | 'shop' | 'dish' | 'tag'
type Entry = Canteen | Shop | Dish | Tag
interface Row {
  id: number
  title: string
  detail: string
  status: string
  entry: Entry
}

const kinds: { key: Kind; label: string }[] = [
  { key: 'canteen', label: '食堂' },
  { key: 'shop', label: '档口' },
  { key: 'dish', label: '菜品' },
  { key: 'tag', label: '标签' },
]
const activeKind = ref<Kind>('canteen')
const canteens = ref<Canteen[]>([])
const shops = ref<Shop[]>([])
const dishes = ref<Dish[]>([])
const tags = ref<Tag[]>([])
const loading = ref(false)
const importing = ref(false)
const csvInput = ref<globalThis.HTMLInputElement | null>(null)
const editorOpen = ref(false)
const editingId = ref<number | null>(null)
const selectedTags = ref<number[]>([])
const form = reactive<Record<string, string | number>>({})
const rows = computed<Row[]>(() => {
  if (activeKind.value === 'canteen')
    return canteens.value.map((item) => ({
      id: item.id,
      title: item.canteenName,
      detail: `${item.campus} · ${item.location || '位置待补充'}`,
      status: item.status === 1 ? '营业中' : '暂停营业',
      entry: item,
    }))
  if (activeKind.value === 'shop')
    return shops.value.map((item) => ({
      id: item.id,
      title: item.shopName,
      detail: `${canteens.value.find((canteen) => canteen.id === item.canteenId)?.canteenName || '未关联食堂'} · ${item.locationDesc || item.cuisine || '档口信息'}`,
      status: item.status === 1 ? '营业中' : '暂停营业',
      entry: item,
    }))
  if (activeKind.value === 'dish')
    return dishes.value.map((item) => ({
      id: item.id,
      title: item.dishName,
      detail: `${item.shopName} · ¥${Number(item.price).toFixed(2)} · ${item.isSignature ? '招牌' : '普通'}`,
      status: item.isAvailable === 1 ? '已上架' : '已下架',
      entry: item,
    }))
  return tags.value.map((item) => ({
    id: item.id,
    title: item.tagName,
    detail: `标签类型 ${item.tagType}`,
    status: '启用',
    entry: item,
  }))
})

async function loadAll() {
  loading.value = true
  try {
    const [canteenPage, shopPage, dishPage, tagPage] = await Promise.all([
      listCanteens(),
      listShops(),
      listAdminDishes(1),
      listTags(),
    ])
    canteens.value = canteenPage.records
    shops.value = shopPage.records
    const remainingDishPages = await Promise.all(
      Array.from({ length: Math.max(0, Math.ceil(dishPage.total / 100) - 1) }, (_, index) =>
        listAdminDishes(index + 2),
      ),
    )
    dishes.value = [
      ...dishPage.records,
      ...remainingDishPages.flatMap((page) => page.records),
    ]
    tags.value = tagPage.records
  } catch (error) {
    showToast(error instanceof Error ? error.message : '数据加载失败')
  } finally {
    loading.value = false
  }
}

function switchKind(kind: Kind) {
  activeKind.value = kind
  closeEditor()
}

function openCreate() {
  editingId.value = null
  selectedTags.value = []
  Object.keys(form).forEach((key) => delete form[key])
  if (activeKind.value === 'canteen')
    Object.assign(form, {
      canteenName: '',
      campus: '',
      location: '',
      openTime: '',
      closeTime: '',
      description: '',
      coverUrl: '',
      sortOrder: 0,
      status: 1,
    })
  if (activeKind.value === 'shop')
    Object.assign(form, {
      canteenId: canteens.value[0]?.id || '',
      shopName: '',
      locationDesc: '',
      cuisine: '',
      openTime: '',
      closeTime: '',
      avgPrice: '',
      queueHeat: '',
      coverUrl: '',
      description: '',
      sortOrder: 0,
      status: 1,
    })
  if (activeKind.value === 'dish')
    Object.assign(form, {
      shopId: shops.value[0]?.id || '',
      dishName: '',
      price: '',
      category: 1,
      mealType: 15,
      spiceLevel: 0,
      takeoutSuitability: '',
      dataSource: '',
      calorie: '',
      description: '',
      imageUrl: '',
      isSignature: 0,
      isAvailable: 1,
    })
  if (activeKind.value === 'tag') Object.assign(form, { tagName: '', tagType: 1, sortOrder: 0 })
  editorOpen.value = true
}

async function openEdit(row: Row) {
  editingId.value = row.id
  selectedTags.value = activeKind.value === 'dish' ? await getDishTags(row.id) : []
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, { ...row.entry })
  editorOpen.value = true
}

function closeEditor() {
  editorOpen.value = false
}

async function save() {
  try {
    const id = editingId.value
    if (activeKind.value === 'canteen') {
      const body = {
        canteenName: String(form.canteenName || '').trim(),
        campus: String(form.campus || '').trim(),
        location: String(form.location || ''),
        openTime: form.openTime ? String(form.openTime) : null,
        closeTime: form.closeTime ? String(form.closeTime) : null,
        description: String(form.description || ''),
        coverUrl: String(form.coverUrl || ''),
        sortOrder: Number(form.sortOrder || 0),
        status: Number(form.status || 0),
      }
      if (!body.canteenName || !body.campus) throw new Error('食堂名称和校区不能为空')
      if (id) await updateCanteen(id, body)
      else await createCanteen(body)
    } else if (activeKind.value === 'shop') {
      const body = {
        canteenId: Number(form.canteenId),
        shopName: String(form.shopName || '').trim(),
        locationDesc: String(form.locationDesc || ''),
        cuisine: String(form.cuisine || ''),
        openTime: form.openTime ? String(form.openTime) : null,
        closeTime: form.closeTime ? String(form.closeTime) : null,
        avgPrice: form.avgPrice === '' ? null : Number(form.avgPrice),
        queueHeat: form.queueHeat === '' || form.queueHeat == null ? null : Number(form.queueHeat),
        coverUrl: String(form.coverUrl || ''),
        description: String(form.description || ''),
        sortOrder: Number(form.sortOrder || 0),
        status: Number(form.status || 0),
      }
      if (!body.canteenId || !body.shopName) throw new Error('请选择食堂并填写档口名称')
      if (id) await updateShop(id, body)
      else await createShop(body)
    } else if (activeKind.value === 'dish') {
      const body = {
        shopId: Number(form.shopId),
        dishName: String(form.dishName || '').trim(),
        price: Number(form.price),
        category: Number(form.category),
        mealType: Number(form.mealType),
        spiceLevel: Number(form.spiceLevel),
        takeoutSuitability:
          form.takeoutSuitability === '' || form.takeoutSuitability == null
            ? null
            : Number(form.takeoutSuitability),
        dataSource: String(form.dataSource || '').trim(),
        calorie: form.calorie === '' ? null : Number(form.calorie),
        description: String(form.description || ''),
        imageUrl: String(form.imageUrl || ''),
        isSignature: Number(form.isSignature),
        isAvailable: Number(form.isAvailable),
      }
      if (!body.shopId || !body.dishName || body.price <= 0)
        throw new Error('请填写档口、菜品名称和有效价格')
      let dishId = id
      if (dishId) await updateDish(dishId, body)
      else {
        const created = (await createDish(body)) as Dish
        dishId = created.id
      }
      await setDishTags(dishId!, selectedTags.value)
    } else {
      const body = {
        tagName: String(form.tagName || '').trim(),
        tagType: Number(form.tagType),
        sortOrder: Number(form.sortOrder || 0),
      }
      if (!body.tagName) throw new Error('标签名称不能为空')
      if (id) await updateTag(id, body)
      else await createTag(body)
    }
    closeEditor()
    showToast('保存成功')
    await loadAll()
  } catch (error) {
    showToast(error instanceof Error ? error.message : '保存失败')
  }
}

async function remove(row: Row) {
  try {
    await showConfirmDialog({ title: '确认删除', message: `确定删除“${row.title}”吗？` })
    if (activeKind.value === 'canteen') await deleteCanteen(row.id)
    else if (activeKind.value === 'shop') await deleteShop(row.id)
    else if (activeKind.value === 'dish') await deleteDish(row.id)
    else await deleteTag(row.id)
    showToast('已删除')
    await loadAll()
  } catch (error) {
    if (error !== 'cancel') showToast(error instanceof Error ? error.message : '删除失败')
  }
}

async function importCsv(event: globalThis.Event) {
  const input = event.target as globalThis.HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  importing.value = true
  try {
    const result = await importDishesCsv(file)
    showToast(`成功导入 ${result.imported} 道菜品`)
    await loadAll()
  } catch (error) {
    showToast(error instanceof Error ? error.message : '导入失败')
  } finally {
    input.value = ''
    importing.value = false
  }
}

function downloadCsvTemplate() {
  const header =
    'shopId,dishName,price,category,mealType,spiceLevel,takeoutSuitability,dataSource,calorie,description,imageUrl,isAvailable\n'
  const link = globalThis.document.createElement('a')
  link.href = globalThis.URL.createObjectURL(
    new globalThis.Blob(['\uFEFF', header], { type: 'text/csv;charset=utf-8' }),
  )
  link.download = '菜品导入模板.csv'
  link.click()
  globalThis.URL.revokeObjectURL(link.href)
}

onMounted(loadAll)
</script>

<template>
  <main class="page">
    <header class="page-heading">
      <div>
        <span class="eyebrow">CAMPUS DATA DESK</span>
        <h1>数据管理</h1>
        <p>维护食堂、档口、菜品和筛选标签，变更会留下操作记录。</p>
      </div>
    </header>
    <div class="admin-tabs">
      <button
        v-for="kind in kinds"
        :key="kind.key"
        class="button small"
        :class="activeKind === kind.key ? '' : 'secondary'"
        type="button"
        @click="switchKind(kind.key)"
      >
        {{ kind.label }}
      </button>
    </div>
    <section
      class="surface"
      style="padding: 18px"
    >
      <div class="admin-toolbar">
        <span class="muted-copy">{{ rows.length }} 条记录</span>
        <button
          v-if="activeKind === 'dish'"
          class="button secondary small"
          type="button"
          :disabled="importing"
          @click="csvInput?.click()"
        >
          {{ importing ? '正在导入…' : '导入菜品 CSV' }}
        </button>
        <input ref="csvInput" type="file" accept=".csv,text/csv" hidden @change="importCsv">
        <button
          v-if="activeKind === 'dish'"
          class="button secondary small"
          type="button"
          @click="downloadCsvTemplate"
        >
          下载 CSV 模板
        </button>
        <button
          class="button small"
          type="button"
          @click="openCreate"
        >
          ＋ 新增{{ kinds.find((kind) => kind.key === activeKind)?.label }}
        </button>
      </div>
      <div
        v-if="loading"
        class="loading-note"
      >
        正在加载数据…
      </div>
      <div
        v-else-if="rows.length"
        class="admin-list"
      >
        <article
          v-for="row in rows"
          :key="row.id"
          class="admin-row surface"
        >
          <div>
            <strong>{{ row.title }}</strong><small>{{ row.detail }} · {{ row.status }}</small>
          </div>
          <div class="row-actions">
            <button
              class="button secondary small"
              type="button"
              @click="openEdit(row)"
            >
              编辑
            </button><button
              class="button danger small"
              type="button"
              @click="remove(row)"
            >
              删除
            </button>
          </div>
        </article>
      </div>
      <div
        v-else
        class="empty-state"
      >
        <span class="emoji">🗂️</span>
        <h2>这里还没有记录</h2>
        <p>新增一条数据，列表就会显示在这里。</p>
      </div>
    </section>

    <div
      v-if="editorOpen"
      class="modal-backdrop"
      @click.self="closeEditor"
    >
      <section
        class="editor-modal surface"
        role="dialog"
        aria-modal="true"
        :aria-label="`${editingId ? '编辑' : '新增'}数据`"
      >
        <div class="modal-heading">
          <div>
            <span class="eyebrow">{{ editingId ? 'EDIT RECORD' : 'NEW RECORD' }}</span>
            <h2>
              {{ editingId ? '编辑' : '新增'
              }}{{ kinds.find((kind) => kind.key === activeKind)?.label }}
            </h2>
          </div>
          <button
            class="icon-button"
            type="button"
            aria-label="关闭"
            @click="closeEditor"
          >
            ×
          </button>
        </div>
        <div class="form-stack">
          <template v-if="activeKind === 'canteen'">
            <div class="field">
              <label>食堂名称</label><input v-model="form.canteenName">
            </div>
            <div class="field">
              <label>所属校区</label><input v-model="form.campus">
            </div>
            <div class="field">
              <label>位置描述</label><input v-model="form.location">
            </div>
            <div class="survey-grid">
              <div class="field">
                <label>开门时间</label><input
                  v-model="form.openTime"
                  type="time"
                >
              </div>
              <div class="field">
                <label>关门时间</label><input
                  v-model="form.closeTime"
                  type="time"
                >
              </div>
            </div>
            <div class="field">
              <label>食堂简介</label><input v-model="form.description">
            </div>
            <div class="field">
              <label>封面图片地址</label><input
                v-model="form.coverUrl"
                type="url"
              >
            </div>
            <div class="field">
              <label>营业状态</label><select v-model="form.status">
                <option :value="1">
                  营业中
                </option>
                <option :value="0">
                  暂停营业
                </option>
              </select>
            </div>
          </template>
          <template v-else-if="activeKind === 'shop'">
            <div class="field">
              <label>所属食堂</label><select v-model="form.canteenId">
                <option
                  v-for="canteen in canteens"
                  :key="canteen.id"
                  :value="canteen.id"
                >
                  {{ canteen.canteenName }}
                </option>
              </select>
            </div>
            <div class="field">
              <label>档口名称</label><input v-model="form.shopName">
            </div>
            <div class="field">
              <label>楼层 / 窗口</label><input v-model="form.locationDesc">
            </div>
            <div class="field">
              <label>主营菜系</label><input v-model="form.cuisine">
            </div>
            <div class="survey-grid">
              <div class="field">
                <label>开门时间</label><input
                  v-model="form.openTime"
                  type="time"
                >
              </div>
              <div class="field">
                <label>关门时间</label><input
                  v-model="form.closeTime"
                  type="time"
                >
              </div>
            </div>
            <div class="survey-grid">
              <div class="field">
                <label>人均价格</label><input
                  v-model="form.avgPrice"
                  type="number"
                  min="0"
                  step="1"
                >
              </div>
              <div class="field">
                <label>排序值</label><input
                  v-model="form.sortOrder"
                  type="number"
                  step="1"
                >
              </div>
            </div>
            <div class="field">
              <label>排队热度</label><select v-model="form.queueHeat">
                <option value="">未核实</option>
                <option :value="0">无需排队</option>
                <option :value="1">较短</option>
                <option :value="2">较长</option>
                <option :value="3">很长</option>
              </select>
            </div>
            <div class="field">
              <label>档口简介</label><input v-model="form.description">
            </div>
            <div class="field">
              <label>封面图片地址</label><input
                v-model="form.coverUrl"
                type="url"
              >
            </div>
            <div class="field">
              <label>营业状态</label><select v-model="form.status">
                <option :value="1">
                  营业中
                </option>
                <option :value="0">
                  暂停营业
                </option>
              </select>
            </div>
          </template>
          <template v-else-if="activeKind === 'dish'">
            <div class="field">
              <label>所属档口</label><select v-model="form.shopId">
                <option
                  v-for="shop in shops"
                  :key="shop.id"
                  :value="shop.id"
                >
                  {{ shop.shopName }}
                </option>
              </select>
            </div>
            <div class="field">
              <label>菜品名称</label><input v-model="form.dishName">
            </div>
            <div class="survey-grid">
              <div class="field">
                <label>价格（元）</label><input
                  v-model="form.price"
                  type="number"
                  min="0.01"
                  step="0.5"
                >
              </div>
              <div class="field">
                <label>菜品分类</label><select v-model="form.category">
                  <option
                    v-for="(name, index) in [
                      '主食',
                      '荤菜',
                      '素菜',
                      '汤羹',
                      '小吃',
                      '饮品',
                      '甜点',
                    ]"
                    :key="name"
                    :value="index + 1"
                  >
                    {{ name }}
                  </option>
                </select>
              </div>
            </div>
            <div class="survey-grid">
              <div class="field">
                <label>辣度</label><select v-model="form.spiceLevel">
                  <option :value="0">
                    不辣
                  </option>
                  <option :value="1">
                    微辣
                  </option>
                  <option :value="2">
                    中辣
                  </option>
                  <option :value="3">
                    重辣
                  </option>
                </select>
              </div>
              <div class="field">
                <label>用餐时段</label><select v-model="form.mealType">
                  <option :value="15">
                    全部时段
                  </option>
                  <option :value="1">
                    早餐
                  </option>
                  <option :value="2">
                    午餐
                  </option>
                  <option :value="4">
                    晚餐
                  </option>
                  <option :value="8">
                    夜宵
                  </option>
                </select>
              </div>
            </div>
            <div class="survey-grid">
              <div class="field">
                <label>热量（千卡）</label><input
                  v-model="form.calorie"
                  type="number"
                  min="0"
                  step="1"
                >
              </div>
              <div class="field">
                <label>招牌菜</label><select v-model="form.isSignature">
                  <option :value="0">
                    普通菜品
                  </option>
                  <option :value="1">
                    招牌菜
                  </option>
                </select>
              </div>
            </div>
            <div class="survey-grid">
              <div class="field">
                <label>打包适配度</label><select v-model="form.takeoutSuitability">
                  <option value="">未核实</option>
                  <option :value="0">不适合</option>
                  <option :value="1">一般</option>
                  <option :value="2">适合</option>
                </select>
              </div>
              <div class="field">
                <label>信息来源</label><input v-model="form.dataSource" maxlength="100">
              </div>
            </div>
            <div class="survey-grid">
              <div class="field">
                <label>上架状态</label><select v-model="form.isAvailable">
                  <option :value="1">
                    上架
                  </option>
                  <option :value="0">
                    下架
                  </option>
                </select>
              </div>
            </div>
            <div class="field">
              <label>菜品简介</label><input v-model="form.description">
            </div>
            <div class="field">
              <label>菜品图片地址</label><input
                v-model="form.imageUrl"
                type="url"
              >
            </div>
            <div class="field">
              <label>口味标签</label>
              <div class="tag-checks">
                <button
                  v-for="tag in tags"
                  :key="tag.id"
                  class="choice"
                  :class="selectedTags.includes(tag.id) ? 'active' : ''"
                  type="button"
                  @click="
                    selectedTags.includes(tag.id)
                      ? (selectedTags = selectedTags.filter((id) => id !== tag.id))
                      : selectedTags.push(tag.id)
                  "
                >
                  {{ tag.tagName }}
                </button>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="field">
              <label>标签名称</label><input v-model="form.tagName">
            </div>
            <div class="field">
              <label>标签类型</label><select v-model="form.tagType">
                <option :value="1">
                  口味
                </option>
                <option :value="2">
                  菜系
                </option>
                <option :value="3">
                  食材
                </option>
                <option :value="4">
                  忌口
                </option>
                <option :value="5">
                  场景 / 特征
                </option>
              </select>
            </div>
          </template>
          <div class="modal-actions">
            <button
              class="button secondary"
              type="button"
              @click="closeEditor"
            >
              取消
            </button><button
              class="button"
              type="button"
              @click="save"
            >
              保存记录
            </button>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>
