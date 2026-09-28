import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { AuthResult } from '../api/user'

const TOKEN_KEY = 'what-to-eat-token'
const USER_KEY = 'what-to-eat-user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  let savedUser: AuthResult | null = null
  try {
    savedUser = JSON.parse(localStorage.getItem(USER_KEY) || 'null') as AuthResult | null
  } catch {
    localStorage.removeItem(USER_KEY)
  }
  const user = ref<AuthResult | null>(savedUser)
  const isLoggedIn = computed(() => Boolean(token.value && user.value))
  const isAdmin = computed(() => user.value?.role === 2)

  function setSession(session: AuthResult) {
    token.value = session.token
    user.value = session
    localStorage.setItem(TOKEN_KEY, session.token)
    localStorage.setItem(USER_KEY, JSON.stringify(session))
  }

  function clearSession() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, user, isLoggedIn, isAdmin, setSession, clearSession }
})
