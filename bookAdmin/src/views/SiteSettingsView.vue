<script setup lang="ts">
/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 15:30
 * @FilePath: bookAdmin/src/views/SiteSettingsView.vue
 * @Description: 站点前台展示配置：站点名称、标语、公告、横幅与主题
 */
import { onMounted, reactive, ref } from 'vue'
import { api } from '@/api/client'
import { useToastStore } from '@/stores/toast'
import type { SiteSettings, SiteTheme } from '@/types/api'
import { formatDateTime } from '@/utils/datetime'

const toast = useToastStore()
const loading = ref(false)
const saving = ref(false)
const form = reactive({
  siteName: '',
  slogan: '',
  announcement: '',
  announcementEnabled: false,
  bannerImage: '',
  theme: 'DEFAULT' as SiteTheme,
})
const updatedAt = ref<string | undefined>()

/** 主题取值必须与后端 SiteSettingKeys.THEMES 一致，否则会被拒绝 */
const THEME_OPTIONS: Array<{ value: SiteTheme; label: string }> = [
  { value: 'DEFAULT', label: '默认（靛蓝）' },
  { value: 'DARK', label: '深色' },
  { value: 'WARM', label: '暖色' },
]

async function load() {
  loading.value = true
  try {
    const data = await api.get<SiteSettings>('/api/admin/site-settings')
    Object.assign(form, {
      siteName: data.siteName ?? '',
      slogan: data.slogan ?? '',
      announcement: data.announcement ?? '',
      announcementEnabled: Boolean(data.announcementEnabled),
      bannerImage: data.bannerImage ?? '',
      theme: data.theme ?? 'DEFAULT',
    })
    updatedAt.value = data.updatedAt
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '站点配置加载失败')
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.siteName.trim()) {
    toast.error('请填写站点名称')
    return
  }
  saving.value = true
  try {
    const data = await api.put<SiteSettings>('/api/admin/site-settings', {
      siteName: form.siteName.trim(),
      slogan: form.slogan.trim(),
      announcement: form.announcement.trim(),
      announcementEnabled: form.announcementEnabled,
      bannerImage: form.bannerImage.trim(),
      theme: form.theme,
    })
    updatedAt.value = data.updatedAt
    toast.success('站点展示配置已保存')
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="panel">
    <div class="panel-heading">
      <div>
        <h3>展示配置</h3>
        <p>这些配置直接作用于前台站点首页，改动后立即对读者生效</p>
      </div>
      <span v-if="updatedAt" style="font-size: 12px; color: var(--ink-mute)">
        最近更新：{{ formatDateTime(updatedAt) }}
      </span>
    </div>

    <form v-if="!loading" @submit.prevent="save">
      <div class="form-grid">
        <label class="field">
          <span>站点名称</span>
          <input v-model="form.siteName" maxlength="50" />
        </label>
        <label class="field">
          <span>站点主题</span>
          <select v-model="form.theme">
            <option v-for="item in THEME_OPTIONS" :key="item.value" :value="item.value">
              {{ item.label }}
            </option>
          </select>
        </label>
      </div>

      <label class="field">
        <span>站点标语</span>
        <input v-model="form.slogan" maxlength="100" />
      </label>

      <label class="field">
        <span>首页横幅图片地址</span>
        <input v-model="form.bannerImage" maxlength="500" placeholder="https://…" />
      </label>

      <label class="field">
        <span>首页公告</span>
        <textarea v-model="form.announcement" maxlength="500" placeholder="留空表示不展示公告" />
      </label>

      <label class="field" style="display: flex; align-items: center; gap: 8px">
        <input v-model="form.announcementEnabled" type="checkbox" style="width: auto" />
        <span>在前台启用公告</span>
      </label>

      <div class="modal-foot">
        <button class="btn" type="button" @click="load">重新载入</button>
        <button class="btn btn-primary" type="submit" :disabled="saving">
          {{ saving ? '保存中…' : '保存配置' }}
        </button>
      </div>
    </form>
    <p v-else class="empty">加载中…</p>
  </section>
</template>
