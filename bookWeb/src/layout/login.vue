<script setup lang="ts">
defineOptions({ name: 'LoginView' })
import { computed, reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/stores/toast'

const auth = useAuthStore()
const toast = useToast()
const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const showPassword = ref(false)
const form = reactive({
  account: '',
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  nickname: '',
  phone: '',
})

const title = computed(() => (mode.value === 'login' ? '欢迎回来' : '创建读者账号'))

function switchMode(next: 'login' | 'register') {
  mode.value = next
  showPassword.value = false
}

async function submit() {
  if (loading.value) return
  if (mode.value === 'login' && (!form.account.trim() || !form.password)) {
    toast.error('请输入账号和密码')
    return
  }
  if (mode.value === 'register') {
    if (!form.username.trim() || !form.email.trim() || !form.password) {
      toast.error('请填写用户名、邮箱和密码')
      return
    }
    if (form.password.length < 8) {
      toast.error('密码至少需要 8 位')
      return
    }
    if (form.password !== form.confirmPassword) {
      toast.error('两次输入的密码不一致')
      return
    }
  }

  loading.value = true
  try {
    if (mode.value === 'login') {
      await auth.login(form.account.trim(), form.password)
      toast.success('登录成功')
    } else {
      await auth.register({
        username: form.username.trim(),
        password: form.password,
        email: form.email.trim(),
        nickname: form.nickname.trim() || undefined,
        phone: form.phone.trim() || undefined,
      })
      toast.success('注册成功，已为你登录')
    }
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '操作失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-story">
      <div class="auth-brand">
        <div class="brand-mark large" aria-hidden="true"><i></i><i></i><i></i></div>
        <span>阅界 · 图书管理系统</span>
      </div>
      <div class="story-copy">
        <p class="eyebrow">READ · BORROW · RETURN</p>
        <h1>让每一本书，<br />都找到下一位读者。</h1>
        <p>检索馆藏、预约借阅、跟踪归还进度；管理人员也可在同一处完成馆藏和借阅流程。</p>
      </div>
      <div class="book-shelf-art" aria-hidden="true">
        <span style="--h: 62%; --c: #5475b8"></span><span style="--h: 83%; --c: #e0a348"></span>
        <span style="--h: 72%; --c: #3f7c70"></span><span style="--h: 94%; --c: #8d5d73"></span>
        <span style="--h: 68%; --c: #bf624f"></span><span style="--h: 78%; --c: #31537b"></span>
        <span style="--h: 55%; --c: #89a65d"></span><span style="--h: 88%; --c: #d18a42"></span>
      </div>
    </section>

    <section class="auth-panel">
      <div class="auth-card">
        <div class="auth-tabs" role="tablist">
          <button type="button" :class="{ active: mode === 'login' }" @click="switchMode('login')">
            登录
          </button>
          <button
            type="button"
            :class="{ active: mode === 'register' }"
            @click="switchMode('register')"
          >
            注册
          </button>
        </div>
        <div class="auth-heading">
          <h2>{{ title }}</h2>
          <p>
            {{ mode === 'login' ? '使用用户名或邮箱进入系统' : '注册后即可浏览并预约馆藏图书' }}
          </p>
        </div>

        <form class="form-stack" @submit.prevent="submit">
          <template v-if="mode === 'login'">
            <label class="field">
              <span>账号</span>
              <input v-model="form.account" autocomplete="username" placeholder="用户名或邮箱" />
            </label>
          </template>
          <template v-else>
            <div class="form-grid two">
              <label class="field">
                <span>用户名 <b>*</b></span>
                <input v-model="form.username" autocomplete="username" placeholder="4–32 位字符" />
              </label>
              <label class="field">
                <span>昵称</span>
                <input v-model="form.nickname" placeholder="选填" />
              </label>
            </div>
            <label class="field">
              <span>邮箱 <b>*</b></span>
              <input
                v-model="form.email"
                type="email"
                autocomplete="email"
                placeholder="name@example.com"
              />
            </label>
            <label class="field">
              <span>手机号</span>
              <input v-model="form.phone" autocomplete="tel" placeholder="选填" />
            </label>
          </template>

          <label class="field">
            <span>密码 <b>*</b></span>
            <span class="password-input">
              <input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="current-password"
                placeholder="至少 8 位"
              />
              <button type="button" @click="showPassword = !showPassword">
                {{ showPassword ? '隐藏' : '显示' }}
              </button>
            </span>
          </label>
          <label v-if="mode === 'register'" class="field">
            <span>确认密码 <b>*</b></span>
            <input
              v-model="form.confirmPassword"
              type="password"
              autocomplete="new-password"
              placeholder="再次输入密码"
            />
          </label>

          <button class="btn btn-primary btn-block btn-lg" type="submit" :disabled="loading">
            <span v-if="loading" class="spinner"></span
            >{{ loading ? '正在处理…' : mode === 'login' ? '进入系统' : '注册并登录' }}
          </button>
        </form>
        <p class="auth-note">登录即表示你同意遵守图书借阅管理规定。</p>
      </div>
    </section>
  </main>
</template>
