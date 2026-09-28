/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 15:30
 * @FilePath: bookAdmin/src/types/api.ts
 * @Description: 管理后台使用的接口数据类型，与后端 VO 字段保持一致
 */
export type UserRole = 'READER' | 'LIBRARIAN' | 'ADMIN'
export type UserStatus = 'ACTIVE' | 'DISABLED' | 'LOCKED'
export type CategoryStatus = 'ACTIVE' | 'DISABLED'
export type BookStatus = 'ACTIVE' | 'INACTIVE'
export type OrderStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'BORROWED'
  | 'RETURN_REQUESTED'
  | 'RETURNED'
  | 'CANCELLED'
  | 'OVERDUE'
export type ReturnCondition = 'GOOD' | 'DAMAGED' | 'LOST'

/** 后端 AuditAction 枚举，actionLabel 由后端直接给出中文，前端不再二次映射 */
export type AuditAction =
  | 'FEATURED_CREATE'
  | 'FEATURED_UPDATE'
  | 'FEATURED_DELETE'
  | 'SITE_SETTING_UPDATE'
  | 'USER_ACCESS_UPDATE'
  | 'USER_PASSWORD_RESET'
  | 'BOOK_STATUS_UPDATE'

export type AuditTargetType = 'BOOK' | 'FEATURED' | 'SITE' | 'USER'

/** 站点主题取值受后端 SiteSettingKeys.THEMES 约束 */
export type SiteTheme = 'DEFAULT' | 'DARK' | 'WARM'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageData<T> {
  page: number
  size: number
  total: number
  pages: number
  records: T[]
}

export interface TokenData {
  tokenType: 'Bearer'
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export interface User {
  id: number
  username: string
  nickname?: string
  email?: string
  phone?: string
  avatarUrl?: string
  role: UserRole
  status: UserStatus
  createdAt: string
  updatedAt?: string
}

export interface CategoryBrief {
  id: number
  name: string
}

export interface Category {
  id: number
  name: string
  description?: string
  sortOrder?: number
  status: CategoryStatus
  bookCount: number
  createdAt?: string
  updatedAt?: string
}

export interface Book {
  id: number
  title: string
  author: string
  isbn: string
  publisher?: string
  publishDate?: string
  description?: string
  coverUrl?: string
  category: CategoryBrief
  totalStock: number
  availableStock: number
  status: BookStatus
  createdAt?: string
  updatedAt?: string
}

export interface UserBrief {
  id: number
  username: string
  nickname?: string
}

export interface BookBrief {
  id: number
  title: string
  isbn: string
  coverUrl?: string
}

export interface BorrowOrder {
  id: number
  orderNo: string
  user: UserBrief
  book: BookBrief
  status: OrderStatus
  remark?: string
  auditRemark?: string
  returnCondition?: ReturnCondition
  reservedAt?: string
  approvedAt?: string
  borrowedAt?: string
  dueAt?: string
  returnedAt?: string
  createdAt: string
  updatedAt?: string
}

export interface DashboardSummary {
  bookTitles: number
  totalCopies: number
  availableCopies: number
  activeReaders: number
  pendingOrders: number
  borrowedOrders: number
  overdueOrders: number
}

/** 首页推荐位：冗余了图书快照，列表无需逐条再查图书 */
export interface FeaturedBook {
  id: number
  bookId: number
  title: string
  author: string
  isbn: string
  coverUrl?: string
  bookStatus: BookStatus
  totalStock: number
  availableStock: number
  position: number
  enabled: boolean
  remark?: string
  createdBy: number
  createdAt?: string
  updatedAt?: string
}

export interface SiteSettings {
  siteName: string
  slogan: string
  announcement: string
  announcementEnabled: boolean
  bannerImage: string
  theme: SiteTheme
  updatedAt?: string
}

export interface AuditLog {
  id: number
  operatorId: number
  operatorName: string
  action: AuditAction
  actionLabel: string
  targetType: AuditTargetType
  targetId?: string
  summary?: string
  ip?: string
  createdAt: string
}
