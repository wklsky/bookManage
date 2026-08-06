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

export interface BookForm {
  title: string
  author: string
  isbn: string
  publisher: string
  publishDate: string
  description: string
  coverUrl: string
  categoryId: number | null
  totalStock: number
  status: BookStatus
}
