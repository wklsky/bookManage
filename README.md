# 阅界 · 图书管理系统

面向读者、图书管理员与系统管理员的图书管理系统，覆盖图书检索、预约借阅、审核借出、归还验收、馆藏维护与用户管理等完整流程。

仓库采用前后端分离结构：接口契约由根目录 [`swagger.json`](./swagger.json) 统一定义，前端已按契约完整实现，后端已按契约完成全部接口实现。

---

## 一、当前进度

| 模块 | 状态 | 说明 |
| --- | --- | --- |
| 接口契约 `swagger.json` | ✅ 已完成 | 6 个 Tag、24 个路径、31 个操作，含全量请求/响应定义 |
| 数据库脚本 | ✅ 已完成 | `DBInitial.sql`（6 张表）+ `generationData.py`（造数） |
| 前端 `bookWeb` | ✅ 已完成 | 6 个业务页面、认证体系、请求层、类型定义齐备 |
| 后端 `bookManage` | ✅ 已完成 | 31 个接口全部实现：认证、用户、分类、图书、借阅流程、统计概览 |

> 一句话结论：**前后端已可联调**。后端采用 Spring Boot 4.1 + MyBatis 注解 + JWT 无状态认证，31 个接口与 `swagger.json` 逐项对齐。实施细节与遗留事项见 [开发指导书](./docs/development.md)。
>
> ⚠️ 联调前请先完成数据库建表并配置环境变量，见 [快速开始](#四快速开始)。

---

## 二、目录结构

```text
bookManage/                  # 仓库根目录
├─ swagger.json                 # 前后端共同遵循的接口契约（Swagger 2.0 手写设计稿）
├─ README.md                    # 本文档
├─ .gitignore                   # 根级忽略规则（含 application-local.yml、*.log）
├─ docs/
│  └─ development.md            # 后端继续开发的实施指南
├─ bookManage/                  # 后端：Spring Boot 工程
│  ├─ pom.xml
│  ├─ mvnw / mvnw.cmd           # Maven Wrapper（3.9.16，无需本地安装 Maven）
│  └─ src/
│     ├─ main/java/com/example/bookmanage/
│     │  ├─ BookManageApplication.java    # 启动类（@MapperScan）
│     │  ├─ common/                       # R / PageResult / Paging / SqlSort / ErrorItem
│     │  ├─ config/                       # SecurityConfig / WebMvcConfig / BookProperties
│     │  ├─ controller/                   # 6 个 REST 控制器
│     │  ├─ dto/{request,response}/       # 请求参数（record）与视图对象（record）
│     │  ├─ entity/                       # 6 张表对应的实体
│     │  ├─ enums/                        # 角色 / 状态 / 借阅状态等 6 个枚举
│     │  ├─ exception/                    # BizException / GlobalExceptionHandler
│     │  ├─ mapper/                       # 6 个 MyBatis 注解式 Mapper
│     │  ├─ security/                     # JWT 签发解析、认证过滤器、登录主体
│     │  ├─ service/                      # 5 个业务服务
│     │  └─ support/ViewAssembler.java    # 实体 → 视图对象的集中转换
│     ├─ main/resources/
│     │  ├─ application.yml               # 端口 / 数据源 / JWT / CORS / MyBatis
│     │  ├─ application-local.yml         # 本地数据库凭据（已被 gitignore）
│     │  ├─ DBInitial.sql                 # 建表脚本（6 张表）
│     │  └─ generationData.py             # Python 造数脚本
│     └─ test/java/.../BookManageApplicationTests.java
└─ bookWeb/                     # 前端：Vue 3 工程
   ├─ index.html                # 标题：阅界 · 图书管理系统
   ├─ vite.config.ts            # 别名 @ -> src，/api 代理到 8080
   ├─ package.json
   └─ src/
      ├─ main.ts / App.vue      # 入口与页面容器（hash 路由）
      ├─ api/client.ts          # fetch 封装、Bearer 注入、401 自动刷新
      ├─ stores/{auth,toast}.ts # 认证状态 / 全局消息
      ├─ types/api.ts           # 与 swagger 对齐的 TS 类型
      ├─ components/            # AppShell / BaseModal / PaginationBar / ToastStack
      ├─ layout/login.vue       # 登录 + 注册
      ├─ views/                 # Dashboard / Books / Orders / Categories / Users / Profile
      ├─ styles/main.css        # 原生 CSS 响应式样式
      └─ assets/svg/            # 10 个手绘图标
```

---

## 三、技术栈

### 后端

| 项 | 版本 / 选型 |
| --- | --- |
| 语言 | Java 17 |
| 框架 | Spring Boot `4.1.0`（`spring-boot-starter-parent`） |
| 构建 | Maven（提供 Wrapper 3.9.16） |
| Web / 安全 | `spring-boot-starter-web`、`-security`、`-validation` |
| ORM | `mybatis-spring-boot-starter` `4.0.1`（MyBatis `3.5.19`），Mapper 全部注解式 |
| 认证 | JJWT `0.12.5`（HS256）；访问令牌 30 分钟，刷新令牌 7 天且落库可撤销 |
| 数据库 | MySQL 5.7+ / 8.0+，库名 `book_system` |
| 工具 | Lombok `1.18.46` |

> 两点选型说明：
>
> 1. **未引入 Redis 与 MyBatis-Plus**。刷新令牌的"可主动失效"由 `sys_refresh_token` 表承担，分页为手写 `LIMIT` + `COUNT` 查询，避免为两个非必需能力引入额外依赖与运维成本。
> 2. **Spring Boot 4 内置的是 Jackson 3**（包名为 `tools.jackson.*`，不是 `com.fasterxml.jackson.*`）。自定义序列化或引入 Jackson 注解时需特别注意坐标，否则会编译失败。

### 前端

| 项 | 版本 / 选型 |
| --- | --- |
| 框架 | Vue 3.5（`<script setup>` + TypeScript） |
| 构建 | Vite 8 |
| 状态 | Pinia 4 |
| 网络 | 原生 `fetch` 自封装（无 axios） |
| UI | 无组件库，原生 CSS 响应式布局 |
| 包管理 | pnpm |
| Node | `^22.18.0` 或 `>=24.12.0` |

> 前端**未安装 vue-router**，页面切换由 `App.vue` 基于 `location.hash` + 组件映射实现（`#/books`、`#/orders` 等）。

---

## 四、快速开始

### 1. 前置条件

- JDK 17+
- Node.js `^22.18.0` 或 `>=24.12.0`，以及 pnpm
- MySQL 5.7+ / 8.0+

### 2. 初始化数据库

```text
# 在 MySQL 中创建库后执行建表脚本（脚本内为 DROP + CREATE，重复执行会重建表）
mysql -u <user> -p book_system < bookManage/src/main/resources/DBInitial.sql
```

需要造演示数据可运行（依赖 `pymysql`、`faker`）：

```powershell
pip install pymysql faker
python bookManage/src/main/resources/generationData.py
```

> ⚠️ 脚本默认每表写入 200 条，且**先清表后插入**，请勿对生产库执行。

### 3. 配置数据库连接

后端不再把凭据写进仓库，通过环境变量注入：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_HOST` | `localhost` | 数据库地址 |
| `DB_PORT` | `3306` | 数据库端口 |
| `DB_NAME` | `book_system` | 库名 |
| `DB_USER` | `root` | 用户名 |
| `DB_PASSWORD` | 空 | 密码 |
| `JWT_SECRET` | 内置占位值 | **生产必须覆盖**，HS256 要求不少于 32 字节 |

本地开发更推荐复制一份 `application-local.yml`（已被 `.gitignore` 排除），在里面直接写连接信息，无需每次设置环境变量。

### 4. 启动后端

```powershell
cd bookManage
.\mvnw.cmd spring-boot:run
```

默认监听 `http://localhost:8080`，激活 `local` profile。启动成功日志形如 `Started BookManageApplication`，且**不会**在启动时建连失败——数据源为懒加载，首次请求才会真正连接数据库，因此请先确认库与表已就绪。

### 5. 启动前端

```powershell
cd bookWeb
pnpm install
pnpm run dev
```

默认 `http://localhost:5173`，`/api` 由 Vite 代理到 `http://localhost:8080`。

---

## 五、接口契约

`swagger.json` 是手写的设计契约（非运行时导出），前后端必须共同遵守：

| 约定 | 内容 |
| --- | --- |
| 路径前缀 | 全部为 `/api` |
| 认证 | `Authorization: Bearer <accessToken>`；注册、登录、刷新令牌除外 |
| 角色 | `READER`、`LIBRARIAN`、`ADMIN` |
| 分页 | `page` 从 1 开始默认 1；`size` 默认 10，最大 100 |
| 成功响应 | `{ "code": 200, "message": "success", "data": {} }`，无数据时 `data` 为 `null` |
| 失败响应 | `{ "code", "message", "errors": [{field, message}], "requestId", "timestamp" }` |
| 复用响应码 | 400 / 401 / 403 / 404 / 409 / 422 / 429 / 500 |

### 接口清单（24 路径 / 31 操作）

| Tag | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| Auth | POST | `/api/auth/register` | 用户注册（201） |
| Auth | POST | `/api/auth/login` | 用户登录（account 可为用户名或邮箱） |
| Auth | POST | `/api/auth/refresh` | 刷新访问令牌 |
| Auth | POST | `/api/auth/logout` | 退出登录 |
| User | GET | `/api/users/profile` | 当前用户资料 |
| User | PUT | `/api/users/profile` | 更新资料（用户名/角色/状态不可改） |
| User | PUT | `/api/users/profile/password` | 修改密码 |
| User | GET | `/api/users` | 用户分页列表 |
| User | GET | `/api/users/{id}` | 用户详情 |
| User | PUT | `/api/users/{id}/status` | 修改角色或状态 |
| Category | GET | `/api/categories` | 分类列表 |
| Category | POST | `/api/categories` | 新增分类 |
| Category | GET | `/api/categories/{id}` | 分类详情 |
| Category | PUT | `/api/categories/{id}` | 更新分类 |
| Category | DELETE | `/api/categories/{id}` | 删除分类（有书则拒绝） |
| Book | GET | `/api/books` | 图书分页检索 |
| Book | POST | `/api/books` | 新增图书 |
| Book | GET | `/api/books/{id}` | 图书详情 |
| Book | PUT | `/api/books/{id}` | 更新书目（库存走专门接口） |
| Book | DELETE | `/api/books/{id}` | 软删除（有未完成借阅返回 409） |
| Book | PATCH | `/api/books/{id}/stock` | 调整库存并记流水 |
| Order | GET | `/api/orders` | 全部借阅单（管理端） |
| Order | GET | `/api/orders/mine` | 我的借阅记录 |
| Order | POST | `/api/orders/reserve` | 预约借阅 |
| Order | GET | `/api/orders/{id}` | 借阅单详情 |
| Order | PUT | `/api/orders/{id}/cancel` | 取消预约 |
| Order | PUT | `/api/orders/{id}/audit` | 审核预约 |
| Order | PUT | `/api/orders/{id}/checkout` | 确认借出 |
| Order | PUT | `/api/orders/{id}/return` | 发起还书 |
| Order | PUT | `/api/orders/{id}/confirm-return` | 验收归还 |
| Dashboard | GET | `/api/dashboard/summary` | 管理端统计概览 |

---

## 六、角色与权限

| 角色 | 能力 |
| --- | --- |
| `READER` | 检索图书、预约、查看/取消自己的借阅单、发起还书、维护个人资料 |
| `LIBRARIAN` | 馆藏与分类维护、库存调整、审核预约、确认借出、验收归还、查看全部借阅单、数据概览 |
| `ADMIN` | `LIBRARIAN` 全部能力 + 用户查询、角色与状态管理 |

契约中通过 `x-roles` 标注了受限接口（如用户管理、分类/图书写操作、审核借出、验收归还、数据概览等）。

> 前端会按角色隐藏菜单与操作按钮，但**权限必须在后端接口层强制校验**，不能依赖前端控制。

---

## 七、借阅状态机

```text
  reserve
     │
     ▼
  PENDING ──audit 拒绝（必填备注，释放库存）──→ REJECTED
     │
     ├──audit 通过──→ APPROVED ──checkout──→ BORROWED ──return──→ RETURN_REQUESTED ──confirm-return──→ RETURNED
     │                                            │                                                      
     │                                       dueAt 过期                                                  
     │                                            ▼                                                      
     │                                         OVERDUE ──return───────────────────────────────────────────┘
     │
     └──cancel（读者，仅 PENDING / APPROVED）──→ CANCELLED（释放库存）
```

| 状态 | 含义 | 触发 |
| --- | --- | --- |
| `PENDING` | 待审核 | `POST /api/orders/reserve`，锁定 1 本库存 |
| `APPROVED` | 审核通过 | `PUT /api/orders/{id}/audit` |
| `REJECTED` | 审核拒绝 | 拒绝必须填写备注，并释放库存 |
| `BORROWED` | 借阅中 | `PUT /api/orders/{id}/checkout`，未传 `dueAt` 按系统期限计算 |
| `OVERDUE` | 已逾期 | 由 `due_at` 过期推导 |
| `RETURN_REQUESTED` | 待验收 | `PUT /api/orders/{id}/return`（读者，仅 `BORROWED`/`OVERDUE`） |
| `RETURNED` | 已归还 | `PUT /api/orders/{id}/confirm-return`；`LOST` 不恢复库存 |
| `CANCELLED` | 已取消 | `PUT /api/orders/{id}/cancel`，释放库存 |

---

## 八、数据模型

`DBInitial.sql` 共 6 张表：

| 表 | 说明 | 关键字段 |
| --- | --- | --- |
| `sys_user` | 系统用户 | `username`/`email` 唯一，`role`、`status` |
| `b_category` | 图书分类 | `sort_order`、`status` |
| `b_book` | 图书 | `isbn` 唯一，`category_id` 外键，`total_stock`、`available_stock`、`is_deleted` |
| `b_borrow_order` | 借阅单 | `order_no` 唯一，`status`、各环节时间戳、`return_condition`、`return_remark` |
| `b_book_stock_log` | 库存流水 | `change_amount`、`reason`、`operator_id` |
| `sys_refresh_token` | 刷新令牌 | `token_id` 唯一，`revoked`、`expires_at` |

> 相对初始脚本的两处变更：
>
> 1. 新增 `sys_refresh_token`：JWT 本身无法主动失效，退出登录与改密码需要服务端吊销能力，故落库保存。
> 2. `b_borrow_order` 新增 `return_remark`：记录读者发起归还时的说明，与管理员的 `audit_remark` 区分。
> 3. `b_borrow_order.status` 注释补全为 8 个状态（含 `RETURN_REQUESTED`、`CANCELLED`、`OVERDUE`）。其中 `OVERDUE` 不落库，由 `BORROWED` + `due_at` 过期推导。

---

## 九、安全提示

已修复：

1. ✅ **凭据外置**：`application.yml` 中的数据库地址与账号改为 `${DB_HOST}` 等环境变量占位，原硬编码内容迁移到已被 `.gitignore` 排除的 `application-local.yml`。
2. ✅ **CORS 收敛**：改为配置项白名单（`book.cors.allowed-origins`），不再使用通配来源。
3. ✅ **配置悬空消除**：移除了 `spring.data.redis` 与 `mybatis-plus` 配置，与 `pom.xml` 实际依赖保持一致。
4. ✅ **根级 `.gitignore`**：已补充，覆盖 `target/`、`node_modules/`、`application-local.yml`、`*.log`。

仍需你处理：

5. ⚠️ **历史提交中的凭据必须轮换**。公网库账号密码曾在 `application.yml`、`generationData.py` 中明文入库（见 `git log`），仅修改当前文件不会让历史记录消失，请立即在数据库侧轮换账号密码。
6. ⚠️ **`JWT_SECRET` 生产环境必须注入**。当前回退值 `change-me-please-change-me-in-production-env` 仅保证 HS256 长度合规，任何拿到它的人都能伪造令牌。
7. ⚠️ **刷新令牌表需要定期清理**。已撤销与已过期的记录目前只标记不删除（`RefreshTokenMapper` 留有 TODO），上线前应补充定时清理任务。

---

## 十、文档索引

| 文档 | 位置 |
| --- | --- |
| 接口契约 | [`swagger.json`](./swagger.json) |
| 后端开发指导书 | [`docs/development.md`](./docs/development.md) |
| 前端说明 | [`bookWeb/README.md`](./bookWeb/README.md) |
| 建表脚本 | `bookManage/src/main/resources/DBInitial.sql` |
