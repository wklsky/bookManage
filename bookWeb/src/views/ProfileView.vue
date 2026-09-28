<script setup lang="ts">
import { computed, reactive, ref, watchEffect } from 'vue'
import { api } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/stores/toast'

const auth = useAuthStore()
const toast = useToast()
const saving = ref(false)
const changingPassword = ref(false)
const profileForm = reactive({ nickname: '', email: '', phone: '', avatarUrl: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const roleLabel = computed(
  () =>
    ({ READER: '读者', LIBRARIAN: '图书管理员', ADMIN: '系统管理员' })[auth.user?.role || 'READER'],
)
const statusLabel = computed(
  () => ({ ACTIVE: '正常', DISABLED: '已停用', LOCKED: '已锁定' })[auth.user?.status || 'ACTIVE'],
)

watchEffect(() => {
  if (!auth.user) return
  Object.assign(profileForm, {
    nickname: auth.user.nickname || '',
    email: auth.user.email || '',
    phone: auth.user.phone || '',
    avatarUrl: auth.user.avatarUrl || '',
  })
})

async function saveProfile() {
  saving.value = true
  try {
    await auth.updateProfile({
      nickname: profileForm.nickname.trim() || undefined,
      email: profileForm.email.trim() || undefined,
      phone: profileForm.phone.trim() || undefined,
      avatarUrl: profileForm.avatarUrl.trim() || undefined,
    })
    toast.success('个人资料已保存')
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function changePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    toast.error('请填写当前密码和新密码')
    return
  }
  if (passwordForm.newPassword.length < 8) {
    toast.error('新密码至少需要 8 位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    toast.error('两次输入的新密码不一致')
    return
  }
  changingPassword.value = true
  try {
    await api.put('/api/users/profile/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
    // 后端在改密成功后会吊销该用户全部刷新令牌，当前会话已无法续期。
    // 必须主动退出，否则用户会在访问令牌过期后被无提示地踢回登录页，且不知道原因。
    auth.reset()
    toast.success('密码已修改，请使用新密码重新登录')
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '密码修改失败')
  } finally {
    changingPassword.value = false
  }
}

async function logout() {
  try {
    await auth.logout()
  } catch {
    auth.reset()
  }
  toast.success('已退出登录')
}
</script>

<template>
  <div class="page-wrap profile-page">
    <header class="page-header">
      <div>
        <p class="eyebrow dark">MY ACCOUNT</p>
        <h1>个人中心</h1>
        <p>维护个人资料与登录密码</p>
      </div>
      <button class="btn btn-danger-ghost" type="button" @click="logout">退出登录</button>
    </header>
    <section class="profile-hero panel">
      <div class="avatar profile-avatar">
        <img v-if="auth.user?.avatarUrl" :src="auth.user.avatarUrl" alt="头像" /><span v-else>{{
          (auth.user?.nickname || auth.user?.username || '用').slice(0, 1)
        }}</span>
      </div>
      <div>
        <h2>{{ auth.user?.nickname || auth.user?.username }}</h2>
        <p>@{{ auth.user?.username }}</p>
        <div>
          <span class="role-pill" :class="`role-${auth.user?.role.toLowerCase()}`">{{
            roleLabel
          }}</span
          ><span class="status-badge badge-success">{{ statusLabel }}</span>
        </div>
      </div>
      <dl>
        <div>
          <dt>用户编号</dt>
          <dd>#{{ auth.user?.id }}</dd>
        </div>
        <div>
          <dt>注册时间</dt>
          <dd>
            {{
              auth.user?.createdAt
                ? new Intl.DateTimeFormat('zh-CN').format(new Date(auth.user.createdAt))
                : '—'
            }}
          </dd>
        </div>
      </dl>
    </section>

    <div class="settings-grid">
      <section class="panel settings-panel">
        <div class="panel-heading">
          <div>
            <h2>基本资料</h2>
            <p>用于通知与借阅联系</p>
          </div>
        </div>
        <form class="form-stack" @submit.prevent="saveProfile">
          <label class="field"
            ><span>用户名</span><input :value="auth.user?.username" disabled /><small
              >用户名不可修改</small
            ></label
          >
          <div class="form-grid two">
            <label class="field"><span>昵称</span><input v-model="profileForm.nickname" /></label
            ><label class="field"><span>手机号</span><input v-model="profileForm.phone" /></label>
          </div>
          <label class="field"
            ><span>邮箱</span><input v-model="profileForm.email" type="email" /></label
          ><label class="field"
            ><span>头像地址</span
            ><input v-model="profileForm.avatarUrl" type="url" placeholder="https://…"
          /></label>
          <div class="form-actions">
            <button class="btn btn-primary" type="submit" :disabled="saving">
              {{ saving ? '保存中…' : '保存资料' }}
            </button>
          </div>
        </form>
      </section>
      <section class="panel settings-panel">
        <div class="panel-heading">
          <div>
            <h2>修改密码</h2>
            <p>建议定期更换登录密码</p>
          </div>
        </div>
        <form class="form-stack" @submit.prevent="changePassword">
          <label class="field"
            ><span>当前密码 <b>*</b></span
            ><input
              v-model="passwordForm.oldPassword"
              type="password"
              autocomplete="current-password" /></label
          ><label class="field"
            ><span>新密码 <b>*</b></span
            ><input
              v-model="passwordForm.newPassword"
              type="password"
              autocomplete="new-password"
            /><small>至少 8 位，建议包含字母和数字</small></label
          ><label class="field"
            ><span>确认新密码 <b>*</b></span
            ><input
              v-model="passwordForm.confirmPassword"
              type="password"
              autocomplete="new-password"
          /></label>
          <div class="form-actions">
            <button class="btn btn-primary" type="submit" :disabled="changingPassword">
              {{ changingPassword ? '修改中…' : '修改密码' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>
