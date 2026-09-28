<script setup lang="ts">
/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 15:30
 * @FilePath: bookAdmin/src/layout/login.vue
 * @Description: 后台登录页，登录后会校验账号是否具备管理权限
 */
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const account = ref('')
const password = ref('')
const submitting = ref(false)
const errorMessage = ref('')

async function submit() {
  if (!account.value.trim() || !password.value) {
    errorMessage.value = '请填写账号和密码'
    return
  }
  submitting.value = true
  errorMessage.value = ''
  try {
    await auth.login(account.value.trim(), password.value)
    // 接口只校验身份，不校验角色：读者账号能登录成功但无权进入后台，
    // 这里的提示由 App.vue 的无权限分支呈现，故不再额外报错
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-wrap">
    <form class="login-card" @submit.prevent="submit">
      <h1>阅界管理后台</h1>
      <p>请使用图书管理员或系统管理员账号登录</p>

      <div v-if="errorMessage" class="login-error">{{ errorMessage }}</div>

      <label class="field">
        <span>账号</span>
        <input v-model="account" autocomplete="username" placeholder="用户名或邮箱" />
      </label>
      <label class="field">
        <span>密码</span>
        <input v-model="password" type="password" autocomplete="current-password" />
      </label>

      <button class="btn btn-primary" type="submit" :disabled="submitting" style="width: 100%">
        {{ submitting ? '登录中…' : '登录' }}
      </button>

      <p class="login-hint">后台仅处理管理事务，读者借阅读书请前往前台站点。</p>
    </form>
  </div>
</template>
