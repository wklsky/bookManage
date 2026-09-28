/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 15:30
 * @FilePath: bookAdmin/src/utils/datetime.ts
 * @Description: 后台统一的日期时间展示
 */

/**
 * 后端返回的是 ISO 8601 字符串，直接渲染会带时区后缀且不易读。
 * 解析失败时返回占位符而不是抛错——表格里个别脏数据不该让整页崩掉。
 */
export function formatDateTime(value?: string) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}
