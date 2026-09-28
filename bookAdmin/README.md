# 阅界管理后台

`bookAdmin` 是图书管理系统的**独立管理后台**，与面向读者的前台 `bookWeb` 彻底分离：各自独立登录、独立端口、独立会话存储。

后台负责四类运营工作：**前台展示控制**、**馆藏维护**、**借阅流转**、**用户与权限**，并把关键操作写入审计日志。

## 为什么要独立成应用

前台与后台的使用人群、操作密度和界面形态差异很大。混在一个应用里靠角色切菜单，会让读者端背上后台的代码体积，也会让"谁能改"和"谁能看"的权限规则纠缠在一起。拆开之后：

- 前台只关心检索与借阅；后台只关心运营。
- 令牌键带 `admin_` 前缀，同一浏览器里前后台会话互不顶掉。
- 后台可单独部署、单独做访问控制（例如只允许内网访问）。

## 技术栈

- Vue 3（`<script setup>`）
- TypeScript 严格模式
- Vite（端口 `5174`）
- Pinia
- 原生 Fetch API
- 原生 CSS

## 功能模块

| 菜单 | 权限 | 能力 |
| --- | --- | --- |
| 数据概览 | LIBRARIAN / ADMIN | 馆藏册数、可借册数、借阅人数、待审核 / 借阅中 / 逾期 |
| 首页推荐位 | LIBRARIAN / ADMIN | 编排前台首页展示哪些书、权重与显隐 |
| 馆藏管理 | LIBRARIAN / ADMIN | 新增 / 编辑图书、上下架、库存调整并留痕 |
| 分类管理 | LIBRARIAN / ADMIN | 维护前台分类导航 |
| 借阅管理 | LIBRARIAN / ADMIN | 审核预约、确认借出、验收归还 |
| 用户管理 | ADMIN | 角色与账号状态、重置他人密码、查看其借阅记录 |
| 站点展示配置 | ADMIN | 站点名称、标语、公告、横幅、主题 |
| 操作审计日志 | ADMIN | 按操作人 / 操作类型 / 关键词回溯 |

> 读者账号能登录成功，但会被拦在"没有后台访问权限"提示页。

## 环境要求

- Node.js `^20.19.0` 或 `>=22.12.0`
- pnpm
- 后端服务运行在 `http://localhost:8080`

## 启动

```powershell
cd bookAdmin
pnpm install
pnpm run dev
```

访问 `http://localhost:5174`。`/api` 由 Vite 代理到后端：

```ts
server: {
  port: 5174,
  proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } },
}
```

> 若 pnpm 提示 `Ignored build scripts: esbuild`，执行 `pnpm approve-builds` 勾选 esbuild 后重新安装即可。

## 常用命令

```powershell
pnpm run dev         # 启动开发服务器
pnpm run type-check  # TypeScript 类型检查
pnpm run build       # 类型检查 + 生产构建
pnpm run preview     # 预览生产构建
```

## 目录结构

```text
bookAdmin/
├─ src/
│  ├─ api/client.ts        # Fetch 封装、Bearer 注入、401 自动续期
│  ├─ stores/auth.ts       # 后台登录态（仅管理员可进入）
│  ├─ stores/toast.ts      # 全局操作消息
│  ├─ types/api.ts         # 接口数据类型
│  ├─ utils/datetime.ts    # 统一的时间展示
│  ├─ components/          # AppShell / BaseModal / PaginationBar / ToastStack
│  ├─ layout/login.vue     # 管理员登录
│  ├─ views/               # 8 个管理页面
│  ├─ styles/main.css      # 后台布局样式
│  ├─ App.vue              # 入口、hash 路由与角色守卫
│  └─ main.ts
├─ index.html
├─ vite.config.ts
└─ package.json
```

## 前后台如何联动

后台并不直接渲染前台，而是通过两份"前台可读"的配置生效：

| 后台操作 | 前台效果 | 读取接口 |
| --- | --- | --- |
| 编排推荐位 | 图书检索页顶部出现「编辑推荐」横滑区 | `GET /api/featured-books` |
| 图书上下架 | 读者检索不到已下架图书 | `GET /api/books` |
| 修改站点配置 | 站点标题、公告条、主题换肤 | `GET /api/site-settings` |
| 重置用户密码 | 该用户所有设备被强制下线 | — |

推荐位有两个硬性过滤：只有**已启用**且图书本身**处于上架状态**的条目才会出现在前台，避免读者点进一个 404 的条目。

## 联调注意事项

- 后端 CORS 白名单需包含 `http://localhost:5174`，否则浏览器会拦跨域请求。
- 后台所有写操作都会写审计日志；日志只追加，不提供修改或删除接口。
- 管理员不能修改自己账号的角色与状态（后端会返回 409），界面已提前禁用该入口。
- 站点主题的可选值由后端 `SiteSettingKeys.THEMES` 约束，前端下拉与之一致。
