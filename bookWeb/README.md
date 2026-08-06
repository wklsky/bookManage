# 阅界图书管理系统前端

`bookWeb` 是图书管理系统的前端项目，根据项目根目录下的 `swagger.json` 接口定义实现。系统面向读者、图书管理员和系统管理员，覆盖图书检索、预约借阅、借出归还、馆藏维护及用户管理等主要流程。

## 技术栈

- Vue 3
- TypeScript
- Vite
- Pinia
- 原生 Fetch API
- 原生 CSS 响应式布局

项目没有依赖额外的 UI 组件库，便于直接维护和二次开发。

## 功能说明

### 通用功能

- 用户登录、注册和退出登录
- JWT 访问令牌存储及自动刷新
- 登录失效后自动清理认证状态
- 个人资料修改
- 登录密码修改
- 桌面端和移动端响应式布局

### 读者

- 按关键词、分类和库存状态检索图书
- 查看图书详情和当前可借库存
- 提交图书预约
- 查看自己的借阅记录
- 取消待审核预约
- 对借阅中或逾期图书发起还书

### 图书管理员

- 查看馆藏及借阅数据概览
- 新增、编辑、下架图书
- 调整图书库存
- 新增、编辑和删除图书分类
- 审核读者预约
- 确认图书借出
- 验收归还图书
- 查询全部借阅记录

### 系统管理员

除图书管理员功能外，还可以：

- 查询和筛选系统用户
- 修改用户角色
- 修改用户账号状态

## 角色权限

接口定义中的角色如下：

| 角色 | 说明 |
| --- | --- |
| `READER` | 读者，可以检索、预约和归还图书 |
| `LIBRARIAN` | 图书管理员，可以维护馆藏并处理借阅流程 |
| `ADMIN` | 系统管理员，拥有图书管理员权限及用户管理权限 |

前端会根据当前用户资料动态展示可访问的菜单。最终权限仍应由后端接口校验，不能仅依赖前端控制。

## 环境要求

- Node.js `^22.18.0` 或 `>=24.12.0`
- pnpm
- 后端服务默认运行在 `http://localhost:8080`

## 安装依赖

进入前端目录后执行：

```text
cd bookWeb
pnpm install
```

## 本地开发

先启动后端服务，并确保其监听 `8080` 端口，然后启动前端：

```powershell
pnpm run dev
```

开发服务器启动后，根据终端输出访问对应地址，通常为：

```text
http://localhost:5173
```

开发环境中，Vite 会将所有 `/api` 请求代理到：

```text
http://localhost:8080
```

代理配置位于 `vite.config.ts`：

```ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
}
```

## 常用命令

```powershell
# 启动开发服务器
pnpm run dev

# TypeScript 类型检查
pnpmrun type-check

# 代码检查并自动修复
pnpm run lint

# 格式化 src 目录
pnpm run format

# 生产环境构建
pnpm run build

# 本地预览生产构建
pnpm run preview
```

## 目录结构

```text
bookWeb/
├─ public/                  # 公共静态资源
├─ src/
│  ├─ api/
│  │  └─ client.ts         # Fetch 封装、令牌注入和自动刷新
│  ├─ components/          # 布局、弹窗、分页和消息提示组件
│  ├─ layout/
│  │  └─ login/            # 登录和注册页面
│  ├─ stores/
│  │  ├─ auth.ts           # 用户认证状态
│  │  └─ toast.ts          # 全局操作消息
│  ├─ styles/
│  │  └─ main.css          # 全局样式及响应式规则
│  ├─ types/
│  │  └─ api.ts            # 接口数据类型
│  ├─ views/
│  │  ├─ DashboardView.vue # 管理端数据概览
│  │  ├─ BooksView.vue     # 图书检索和管理
│  │  ├─ OrdersView.vue    # 借阅流程管理
│  │  ├─ CategoriesView.vue# 分类管理
│  │  ├─ UsersView.vue     # 用户管理
│  │  └─ ProfileView.vue   # 个人中心
│  ├─ App.vue              # 页面入口及角色导航控制
│  └─ main.ts              # Vue 应用初始化
├─ index.html
├─ package.json
└─ vite.config.ts
```

## 接口与认证

前端按照 `swagger.json` 中的统一响应结构读取数据：

```json
{
  "code": 200,
  "message": "请求成功",
  "data": {}
}
```

登录成功后，访问令牌和刷新令牌会保存在浏览器 `localStorage` 中：

```text
book_access_token
book_refresh_token
```

除注册、登录和刷新令牌接口外，请求会自动携带：

```http
Authorization: Bearer <accessToken>
```

当接口返回 `401` 时，客户端会尝试使用刷新令牌获取新的访问令牌，并自动重试原请求。刷新失败后，用户需要重新登录。

## 生产构建

执行：

```powershell
pnpm run build
```

构建产物生成在：

```text
bookWeb/dist
```

生产环境部署时，需要满足以下条件之一：

1. 将前端和后端部署在同一域名下，并让 `/api` 指向后端服务；
2. 在 Nginx、网关或其他反向代理中，将 `/api` 转发到后端；
3. 修改 API 请求策略，使其指向实际后端地址。

Nginx 反向代理示例：

```nginx
server {
    listen 80;
    server_name example.com;

    root /var/www/bookWeb/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 接口联调注意事项

- 后端分页从第 `1` 页开始。
- 后端默认每页返回 `10` 条数据。
- 图书删除为软删除；存在未完成借阅时，后端可能返回 `409`。
- 分类下存在图书时，分类删除可能被后端拒绝。
- 借阅状态变化由后端进行最终校验，前端只展示当前状态允许的操作。
- 如果前后端不在同一域名，需要由后端正确配置 CORS，或通过反向代理统一域名。

## 代码检查

提交代码前建议执行：

```powershell
pnpm run lint
pnpm run build
```

`build` 命令会同时执行 Vue TypeScript 类型检查和 Vite 生产构建。
