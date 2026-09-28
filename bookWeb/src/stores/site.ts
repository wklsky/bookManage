import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/api/client'
import type { SiteSettings } from '@/types/api'

/**
 * 站点前台展示配置，由管理后台维护。
 *
 * <p>展示配置是增强项而非核心链路：读取失败时保持默认值继续渲染，
 * 不能因为后台没配过就把整个前台页面挡在门外。
 */
export const useSiteStore = defineStore('site', () => {
  const settings = ref<SiteSettings | null>(null)

  const siteName = computed(() => settings.value?.siteName?.trim() || '阅界')
  const slogan = computed(() => settings.value?.slogan?.trim() || '')
  const bannerImage = computed(() => settings.value?.bannerImage?.trim() || '')
  /** 公告需同时满足「有内容」与「后台已启用」两个条件，否则前台不露出 */
  const announcement = computed(() =>
    settings.value?.announcementEnabled ? settings.value.announcement.trim() : '',
  )

  async function load() {
    try {
      settings.value = await api.get<SiteSettings>('/api/site-settings')
      applyTheme(settings.value.theme)
      document.title = `${siteName.value} · 图书管理系统`
    } catch {
      // 忽略：沿用 siteName 等默认值即可，前台功能不受影响
    }
  }

  return { settings, siteName, slogan, bannerImage, announcement, load }
})

/** 主题落到 <html> 上，由 main.css 中的变量覆盖实现整站换肤 */
function applyTheme(theme?: string) {
  const root = document.documentElement
  root.classList.remove('theme-dark', 'theme-warm')
  if (theme === 'DARK') root.classList.add('theme-dark')
  if (theme === 'WARM') root.classList.add('theme-warm')
}
