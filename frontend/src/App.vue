<script setup lang="ts">
import { useRouter } from 'vue-router'
import { onBeforeUnmount, onMounted } from 'vue'
import { showToast } from 'vant'
import { useAuthStore } from './stores/auth'
import { logout } from './api/user'

const auth = useAuthStore()
const router = useRouter()

function handleExpiredSession() {
  const hadSession = auth.isLoggedIn
  auth.clearSession()
  if (hadSession) {
    showToast('登录已失效，请重新登录')
    void router.replace({ name: 'login' })
  }
}

onMounted(() => globalThis.addEventListener('session-expired', handleExpiredSession))
onBeforeUnmount(() => globalThis.removeEventListener('session-expired', handleExpiredSession))

async function signOut() {
  try {
    await logout()
  } catch {
    // The local session is still cleared if the token has expired.
  }
  auth.clearSession()
  showToast('已退出登录')
  await router.push('/')
}
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <RouterLink
        class="brand"
        to="/"
      >
        <span class="brand-mark">食</span>
        <span>今天吃什么<small>校园餐饮指南</small></span>
      </RouterLink>
      <nav
        class="main-nav"
        aria-label="主导航"
      >
        <RouterLink to="/">
          发现菜品
        </RouterLink>
        <RouterLink
          v-if="auth.isLoggedIn"
          to="/recommendations"
        >
          今日推荐
        </RouterLink>
        <RouterLink
          v-if="auth.isLoggedIn"
          to="/favorites"
        >
          我的收藏
        </RouterLink>
        <RouterLink
          v-if="auth.isLoggedIn"
          to="/survey"
        >
          口味问卷
        </RouterLink>
        <RouterLink
          v-if="auth.isLoggedIn"
          to="/profile"
        >
          我的画像
        </RouterLink>
        <RouterLink
          v-if="auth.isAdmin"
          to="/admin"
        >
          数据管理
        </RouterLink>
      </nav>
      <div class="account-actions">
        <template v-if="auth.isLoggedIn">
          <span class="user-name">{{ auth.user?.nickname }}</span>
          <button
            class="text-button"
            type="button"
            @click="signOut"
          >
            退出
          </button>
        </template>
        <RouterLink
          v-else
          class="login-link"
          to="/login"
        >
          登录 / 注册
        </RouterLink>
      </div>
    </header>
    <RouterView />
    <footer class="site-footer">
      用一顿好饭，照顾今天的自己。
    </footer>
  </div>
</template>
