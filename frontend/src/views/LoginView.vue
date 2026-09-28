<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { login, register } from '../api/user'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const registering = ref(false)
const submitting = ref(false)
const form = reactive({ username: '', password: '', nickname: '' })

async function submit() {
  if (!/^[a-zA-Z0-9_]{4,32}$/.test(form.username)) {
    showToast('用户名需为 4–32 位字母、数字或下划线')
    return
  }
  if (form.password.length < 8 || form.password.length > 72) {
    showToast('密码长度需为 8–72 位')
    return
  }
  submitting.value = true
  try {
    const session = registering.value
      ? await register(form.username, form.password, form.nickname.trim())
      : await login(form.username, form.password)
    auth.setSession(session)
    showToast(registering.value ? '注册成功，欢迎加入' : '欢迎回来')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '登录暂时不可用')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="page page-narrow">
    <section class="auth-card surface">
      <span class="eyebrow">WELCOME TO CAMPUS TABLE</span>
      <h1>{{ registering ? '创建你的账号' : '欢迎回来' }}</h1>
      <p>
        {{
          registering
            ? '保存口味偏好，让每一餐都更合心意。'
            : '登录后填写问卷，生成属于你的口味画像。'
        }}
      </p>
      <form
        class="form-stack"
        @submit.prevent="submit"
      >
        <div class="field">
          <label for="username">用户名</label><input
            id="username"
            v-model.trim="form.username"
            autocomplete="username"
            minlength="4"
            maxlength="32"
            required
            placeholder="4–32 位字母、数字或下划线"
          >
        </div>
        <div class="field">
          <label for="password">密码</label><input
            id="password"
            v-model="form.password"
            :autocomplete="registering ? 'new-password' : 'current-password'"
            type="password"
            minlength="8"
            maxlength="72"
            required
            placeholder="至少 8 位"
          >
        </div>
        <div
          v-if="registering"
          class="field"
        >
          <label for="nickname">昵称（选填）</label><input
            id="nickname"
            v-model.trim="form.nickname"
            maxlength="32"
            placeholder="怎么称呼你"
          >
        </div>
        <button
          class="button"
          type="submit"
          :disabled="submitting"
        >
          {{ submitting ? '请稍候…' : registering ? '注册并登录' : '登录' }}
        </button>
      </form>
      <div
        class="switch-link"
        @click="registering = !registering"
      >
        {{ registering ? '已有账号？返回登录' : '第一次来？创建一个账号' }}
      </div>
    </section>
  </main>
</template>
