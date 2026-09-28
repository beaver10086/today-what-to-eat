import { ref } from 'vue'
import { showToast } from 'vant'
import type { Router } from 'vue-router'
import { addFavorite, getFavoriteIds, removeFavorite } from '../api/favorites'
import type { useAuthStore } from '../stores/auth'

type AuthStore = ReturnType<typeof useAuthStore>

export function useFavorites(targetType: 1 | 2, auth: AuthStore, router: Router) {
  const ids = ref(new Set<number>())
  const busyIds = ref(new Set<number>())

  async function refresh() {
    if (!auth.isLoggedIn) {
      ids.value = new Set()
      return
    }
    try {
      ids.value = new Set(await getFavoriteIds(targetType))
    } catch (error) {
      showToast(error instanceof Error ? error.message : '收藏状态加载失败')
    }
  }

  async function toggle(targetId: number) {
    if (!auth.isLoggedIn) {
      showToast('登录后即可收藏')
      await router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
      return
    }
    if (busyIds.value.has(targetId)) return
    busyIds.value = new Set(busyIds.value).add(targetId)
    try {
      if (ids.value.has(targetId)) {
        await removeFavorite(targetType, targetId)
        const nextIds = new Set(ids.value)
        nextIds.delete(targetId)
        ids.value = nextIds
        showToast('已取消收藏')
      } else {
        await addFavorite(targetType, targetId)
        ids.value = new Set(ids.value).add(targetId)
        showToast('已加入收藏')
      }
    } catch (error) {
      showToast(error instanceof Error ? error.message : '收藏操作失败')
    } finally {
      const nextBusy = new Set(busyIds.value)
      nextBusy.delete(targetId)
      busyIds.value = nextBusy
    }
  }

  return {
    ids,
    busyIds,
    refresh,
    toggle,
    isSaved: (targetId: number) => ids.value.has(targetId),
    isBusy: (targetId: number) => busyIds.value.has(targetId),
  }
}
