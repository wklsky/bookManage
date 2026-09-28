# 阅界 · 图书管理系统 开发指导书

本文是后端（及少量前端增强）**继续开发**的实施指南。阅读前请先浏览根目录 [`README.md`](../README.md) 了解项目现状。

契约的唯一事实来源是根目录 [`swagger.json`](../swagger.json)：**字段名、状态码、业务规则以它为准**，前端 `bookWeb/src/types/api.ts` 是它的 TS 映射，二者冲突时以 `swagger.json` 为准并同步修改前端类型。

---

## 0. 目标与验收标准

### 总目标

把 `bookManage` 从「启动类 + CORS」补齐为完整可运行的后端，使前端 6 个业务页面无需改动即可跑通全部流程。

在此之上另有一个独立的管理后台 `bookAdmin`（见 [第 13 节](#13-管理后台-bookadmin2026-09-28)），它在契约之外新增了推荐位、站点展示配置、用户管理增强与操作审计四类运营能力，后端为此新增 3 张表与 11 个接口。

### 当前状态（2026-09-28 更新）

P0–P6 已全部落地，P8（管理后台）主体完成，工程可编译、Spring 上下文可正常启动。代码层面的验收项如下：

| 验收项 | 状态 | 说明 |
| --- | --- | --- |
| 31 个接口全部实现 | ✅ 代码完成 | 6 个 Controller，路径/方法/响应与 `swagger.json` 对齐 |
| 管理后台 `bookAdmin` | ✅ 代码完成 | 独立 SPA，8 个页面；`vue-tsc` 类型检查与 `vite build` 均通过 |
| 管理后台专属接口 | ✅ 代码完成 | 11 个接口，路径统一 `/api/admin` 前缀，权限按 LIBRARIAN / ADMIN 划分 |
| 统一响应与错误结构 | ✅ 代码完成 | `R<T>` + `GlobalExceptionHandler`，错误带 `errors / requestId / timestamp` |
| JWT 双令牌 | ✅ 代码完成 | 刷新令牌落库可撤销，轮换式续期 |
| 角色鉴权 | ✅ 代码完成 | URL 层 `hasRole` + Service 层数据归属二次校验 |
| 借阅全流程 | ✅ 代码完成 | 状态机 8 态，`OVERDUE` 为派生状态 |
| 库存防超卖 | ✅ 代码完成 | `SELECT ... FOR UPDATE` + 条件更新 |
| 端到端联调 | ⏳ 待验证 | 开发机无法连接目标数据库（凭据已失效），尚未跑通真实数据 |

> ⏳ 表示代码已具备条件、但未经运行时验证。**联调前务必按 [第 10 节](#10-联调与自测) 走一遍手工验收路径**，尤其是并发预约与库存回补两条链路。

---

## 1. 阶段划分

| 阶段 | 内容 | 前置 | 状态 |
| --- | --- | --- | --- |
| P0 | 依赖补齐、配置整改、安全基线 | — | ✅ 完成 |
| P1 | 基础框架：统一响应、全局异常、ORM、分页、Security + JWT | P0 | ✅ 完成 |
| P2 | Auth 模块（注册 / 登录 / 刷新 / 登出） | P1 | ✅ 完成 |
| P3 | User 模块 + Category 模块 | P2 | ✅ 完成 |
| P4 | Book 模块（含库存调整与流水） | P3 | ✅ 完成 |
| P5 | Order 模块（状态机，核心难点） | P4 | ✅ 完成 |
| P6 | Dashboard 统计 | P5 | ✅ 完成 |
| P7 | 联调、加固与前端增强（可选） | P6 | ⏳ 待进行 |
| P8 | 独立管理后台 `bookAdmin` + 后端运营接口 | P6 | ✅ 完成 |

下文各节保留完整的实施要点，作为**维护与排错时的参考**：业务规则、并发要求、易错点仍然有效，改动代码前请先对照。

---

## 2. P0：依赖与配置整改

### 2.1 补齐 `pom.xml` 依赖

| 依赖 | 用途 | 当时现状 | 最终决策 |
| --- | --- | --- | --- |
| `spring-boot-starter-test` | 单元测试（JUnit5 / AssertJ） | 未显式声明 | ✅ 已补充 |
| MyBatis-Plus Starter | ORM（`application.yml` 已配置） | 缺失，配置悬空 | ❌ **不引入**，改用官方 `mybatis-spring-boot-starter` |
| `spring-boot-starter-data-redis` | Redis（`application.yml` 已配置） | 缺失，配置悬空 | ❌ **不引入**，刷新令牌改由表承载，并移除 Redis 配置 |
| JWT 库 | 签发与解析令牌 | 缺失 | ✅ 引入 JJWT `0.12.5` |
| `springdoc-openapi`（可选） | 在线接口文档 | 缺失 | ❌ 不引入，`swagger.json` 已足够 |

> 决策理由：MyBatis-Plus 对 Spring Boot 4.1.0 的兼容需要实测，而本项目 SQL 并不复杂（无复杂联表动态条件、无自动填充需求），用官方 starter + 注解式 Mapper + 手写分页即可覆盖，少一层依赖少一层升级风险。同理，Redis 只被规划用于"令牌黑名单"，用一张 `sys_refresh_token` 表同样能解决，不值得为它引入运维成本。

### 2.2 配置整改（安全红线，优先于业务开发）

1. **凭据外置**：`application.yml` 中的数据库地址、账号、密码改为占位符，从环境变量或本地 profile 注入：

   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:book_system}?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
       username: ${DB_USER}
       password: ${DB_PASSWORD}
   ```

   同时**轮换已提交到仓库的凭据**，并新增 `application-local.yml`（加入 `.gitignore`）。

2. **ORM 配置**：采用官方 `mybatis-spring-boot-starter`，最终落地如下（无 MyBatis-Plus，故不配逻辑删除全局策略）：

   ```yaml
   mybatis:
     mapper-locations: classpath*:/mapper/**/*.xml   # 当前 Mapper 全为注解式，保留以便后续加 XML
     type-aliases-package: com.example.bookmanage.entity
     configuration:
       map-underscore-to-camel-case: true             # is_deleted -> isDeleted，实体字段名必须与之一致
   ```

   > 软删除不借助框架的逻辑删除能力，而是在每条查询 SQL 中显式写 `is_deleted = 0`，避免"某处忘记拼接"导致已下架图书重新出现。

3. **新增自定义配置**：JWT 密钥与有效期、借阅期限天数（供 `checkout` 默认 `dueAt` 使用）。

   ```yaml
   book:
     jwt:
       secret: ${JWT_SECRET}
       access-token-ttl: 30m
       refresh-token-ttl: 7d
     borrow:
       default-days: 30      # 业务要求：借出未指定 dueAt 时按系统借阅期限计算
   ```

4. **CORS 收敛**：`WebMvcConfig` 的 `allowedOriginPattern("*")` 改为读取配置的白名单。

5. **补充根级 `.gitignore`**：忽略 `target/`、`node_modules/`、`dist/`、`.env*`。

---

## 3. P1：基础框架

### 3.1 推荐包结构

```text
com.example.bookmanage
├─ common/
│  ├─ R.java                 # 统一响应包装
│  ├─ PageResult.java        # 分页结果 {page,size,total,pages,records}
│  └─ ErrorCode.java         # 错误码枚举
├─ config/                   # WebMvcConfig、SecurityConfig、MybatisPlusConfig、JacksonConfig
├─ security/
│  ├─ JwtTokenProvider.java  # 签发 / 解析 / 校验
│  ├─ JwtAuthenticationFilter.java
│  ├─ LoginUser.java         # 当前登录主体（id / username / role / status）
│  └─ annotation/            # 自定义权限注解（可选）
├─ exception/
│  ├─ BizException.java
│  └─ GlobalExceptionHandler.java
├─ entity/                   # SysUser / BookCategory / Book / BorrowOrder / BookStockLog
├─ mapper/                   # *Mapper.java（resources/mapper/*.xml）
├─ service/ + service/impl/
├─ controller/
├─ dto/request/ + dto/response/
└─ util/
```

启动类需补 `@MapperScan("com.example.bookmanage.mapper")`。

### 3.2 统一响应

成功：`{ "code": 200, "message": "success", "data": ... }`，`data` 为 `null` 时不要省略字段（前端直接读 `result.data`）。

失败：`{ "code": 409, "message": "...", "errors": [{ "field": "isbn", "message": "..." }], "requestId": "...", "timestamp": "..." }`，`code` 取 HTTP 状态码。

### 3.3 全局异常处理

必须覆盖并映射到的状态码：

| 异常 | 状态码 |
| --- | --- |
| 参数校验失败（`MethodArgumentNotValidException`） | 400，并把字段错误填充到 `errors` |
| 未认证 / 令牌失效 | 401 |
| 权限不足（`AccessDeniedException`） | 403 |
| 资源不存在 | 404 |
| 业务冲突（重复预约、状态不允许、有未完成借阅、分类下有书） | 409 |
| 业务规则不满足（如库存不足） | 422 |
| 其他 | 500（日志打全，响应不暴露堆栈） |

`requestId` 用 UUID 生成并写入日志，便于前后端对照排查。

### 3.4 Security + JWT

要点：

- `SecurityConfig` 中 `csrf` 关闭（前后端分离 + 无 Cookie 会话）、`sessionManagement` 设为 `STATELESS`。
- 放行：`POST /api/auth/register`、`/api/auth/login`、`/api/auth/refresh`。
- `JwtAuthenticationFilter` 解析 `Authorization: Bearer`，装载 `LoginUser` 到 `SecurityContext`。
- 角色映射为 `ROLE_READER` / `ROLE_LIBRARIAN` / `ROLE_ADMIN`，管理端接口用 `hasAnyRole("LIBRARIAN","ADMIN")`，用户管理用 `hasRole("ADMIN")`。
- **不要把角色判断写死在 Controller 里**，用配置式鉴权或统一注解，便于后续审计。
- 用户 `status` 为 `DISABLED` / `LOCKED` 时，即使令牌有效也必须拒绝（返回 401 或 403）。

> 若 Boot 4 的 Security DSL 与示例写法有差异，以实际依赖版本为准，但上述语义不变。

### 3.5 字段约定

- 时间字段统一 `created_at` / `updated_at`，开启 `map-underscore-to-camel-case` 后 Java 侧为驼峰。
- 前端类型定义为驼峰（`availableStock`、`dueAt`），后端响应必须一致。
- 时间序列化统一 ISO-8601 字符串，时区 `Asia/Shanghai`。

---

## 4. P2：Auth 模块

| 接口 | 实现要点 |
| --- | --- |
| `POST /api/auth/register` | 用户名与邮箱唯一（冲突 409）；密码加密存储（BCrypt）；默认角色 `READER`、状态 `ACTIVE`；返回 **201** |
| `POST /api/auth/login` | `account` 需同时支持**用户名或邮箱**匹配；校验状态（禁用/锁定返回 403）；签发 accessToken + refreshToken + `expiresIn`，`tokenType` 固定 `"Bearer"` |
| `POST /api/auth/refresh` | 校验 refreshToken，签发新令牌对；失败返回 401（前端会清空令牌并跳登录） |
| `POST /api/auth/logout` | 使 refreshToken 失效（Redis 黑名单或删除记录）；令牌非法时也应幂等返回成功，避免前端报错 |

密码规则与前端保持一致：注册要求 **不少于 8 位**，修改密码同样校验。

> 前端 `client.ts` 在收到 401 时会用 `refreshPromise` 单例刷新并**重放一次**原请求，失败则派发 `auth-expired` 事件。因此 refresh 接口必须能在并发下稳定工作。

---

## 5. P3：User 与 Category 模块

### User

| 接口 | 实现要点 |
| --- | --- |
| `GET /api/users/profile` | 返回当前登录用户 |
| `PUT /api/users/profile` | 仅允许改 `nickname / email / phone / avatarUrl`；**用户名、角色、状态不可由本人修改** |
| `PUT /api/users/profile/password` | 校验原密码 + 新密码 ≥ 8 位 |
| `GET /api/users` | 分页 + 关键词 + 角色 / 状态筛选（仅 `ADMIN`） |
| `GET /api/users/{id}` | 详情 |
| `PUT /api/users/{id}/status` | 修改角色 / 状态；需记录 `reason`（禁用、锁定、调整角色原因）；**禁止降级自己**，禁止把最后一个 `ADMIN` 改掉 |

### Category

| 接口 | 实现要点 |
| --- | --- |
| `GET /api/categories` | 支持 `keyword`、`includeDisabled`；返回含 `bookCount` |
| `POST` / `PUT /api/categories/{id}` | 名称必填，`sortOrder` 控制展示顺序 |
| `DELETE /api/categories/{id}` | 分类下存在图书时拒绝（409），前端已提示「该分类下可能仍有图书」 |
| `GET /api/categories/{id}` | 详情（前端暂未调用，但契约要求实现） |

---

## 6. P4：Book 模块

| 接口 | 实现要点 |
| --- | --- |
| `GET /api/books` | 分页 + 关键词（书名/作者/ISBN）+ `categoryId` + `availableOnly` + `status` 筛选；默认过滤 `is_deleted = 1` |
| `POST /api/books` | ISBN 唯一（409）；`categoryId` 必须存在（404）；初始化 `total_stock` 与 `available_stock` 相等 |
| `GET /api/books/{id}` | 详情，含关联的 `category`（`{id, name}`） |
| `PUT /api/books/{id}` | **只改书目信息，不改库存**（库存走专门接口） |
| `DELETE /api/books/{id}` | 软删除；存在未完成借阅单时返回 409 |
| `PATCH /api/books/{id}/stock` | 见下 |

### 库存调整规则（易错点）

1. 通过**正负变更量** `changeAmount` 调整总库存；
2. 调整后 `total_stock` **不得小于当前已借出数量**（`total_stock - available_stock`），否则返回 422；
3. 同步维护 `available_stock`；
4. 必须写入 `b_book_stock_log`（`book_id`、`operator_id`、`change_amount`、`reason`），`reason` 必填。

> 库存是并发热点，更新请使用条件更新（`WHERE id = ? AND available_stock >= ?`）或悲观锁，不要「先查后写」。

---

## 7. P5：Order 模块（核心）

### 7.1 状态机

```text
reserve      → PENDING
PENDING      --audit(通过)--> APPROVED
PENDING      --audit(拒绝)--> REJECTED          拒绝必须填 auditRemark，并释放库存
PENDING/APPROVED --cancel--> CANCELLED         仅所有者可取消，释放库存
APPROVED     --checkout--> BORROWED            未传 dueAt 时按 book.borrow.default-days 计算
BORROWED/OVERDUE --return--> RETURN_REQUESTED  仅所有者
RETURN_REQUESTED --confirm-return--> RETURNED  LOST 不恢复库存
BORROWED 且 due_at < now → OVERDUE            查询时动态推导
```

非法流转一律返回 **409**。

### 7.2 各接口要点

| 接口 | 规则 |
| --- | --- |
| `POST /api/orders/reserve` | 创建 `PENDING` 单并**锁定 1 本库存**（`available_stock - 1`，不足返回 422）；同一用户对同一本书不能存在重复未完成单（409）；生成唯一 `order_no` |
| `GET /api/orders` | 管理端全量，支持 `status` 等筛选 |
| `GET /api/orders/mine` | 仅当前用户 |
| `GET /api/orders/{id}` | 读者仅能看自己的（越权 403） |
| `PUT /api/orders/{id}/cancel` | 仅所有者，`PENDING`/`APPROVED` 可取消，释放库存 |
| `PUT /api/orders/{id}/audit` | 仅 `PENDING`；拒绝必须填备注并释放库存 |
| `PUT /api/orders/{id}/checkout` | `APPROVED → BORROWED`，写 `borrowed_at` 与 `due_at` |
| `PUT /api/orders/{id}/return` | 所有者将 `BORROWED`/`OVERDUE` 置为 `RETURN_REQUESTED`，可带 `remark` |
| `PUT /api/orders/{id}/confirm-return` | 验收，`GOOD`/`DAMAGED` 恢复可用库存，`LOST` **不恢复** |

### 7.3 事务与并发

- 每个状态流转方法加 `@Transactional`，库存变更与订单状态更新必须在同一事务内。
- 库存扣减用条件更新并校验影响行数，影响行数为 0 即视为库存不足。
- 建议对 `b_book` 行加悲观锁（`SELECT ... FOR UPDATE`）或使用 Redis 分布式锁；至少保证单机下的正确。
- `OVERDUE` 建议**查询时按 `due_at` 动态计算**返回，避免定时任务未执行导致状态滞后；如需落库则补定时任务并同步 `DBInitial.sql` 的状态说明。

### 7.4 需同步修改的建表脚本

`DBInitial.sql` 中 `b_borrow_order.status` 的注释只列了 5 个状态（`PENDING, APPROVED, BORROWED, RETURNED, REJECTED`），与契约的 8 态不一致。请补全注释为：

```sql
`status` varchar(20) NOT NULL DEFAULT 'PENDING'
  COMMENT '订单状态: PENDING, APPROVED, REJECTED, BORROWED, RETURN_REQUESTED, RETURNED, CANCELLED, OVERDUE',
```

---

## 8. P6：Dashboard

`GET /api/dashboard/summary`（`LIBRARIAN`/`ADMIN`）返回：

| 字段 | 口径 |
| --- | --- |
| `bookTitles` | 未删除图书的**书目数** |
| `totalCopies` | `SUM(total_stock)` |
| `availableCopies` | `SUM(available_stock)` |
| `activeReaders` | 有借阅记录的用户数（去重） |
| `pendingOrders` | `status = PENDING` 数量 |
| `borrowedOrders` | `status = BORROWED` 数量 |
| `overdueOrders` | `BORROWED` 且 `due_at < now()` 数量 |

口径写进注释，避免后续统计口径漂移。

---

## 9. 编码规范（后端）

- **严格分层**：Controller 只做参数校验与转发，业务逻辑全部在 Service；禁止在 Controller 写 SQL。
- **禁止裸写 Map**：请求用 `XxxRequest` DTO，响应用实体或 `XxxResponse`，与 `swagger.json` 的 definitions 一一对应。
- **参数校验**：用 `jakarta.validation` 注解，并在 `GlobalExceptionHandler` 中把字段错误映射到 `errors`。
- **事务边界**：状态流转 + 库存变更必须在同一事务；查询方法加 `readOnly = true`。
- **常量化**：状态、角色、借阅期限等禁止出现魔法字符串/数字，统一枚举或常量类。
- **日志**：关键状态流转与异常必须打日志并带 `requestId`；禁止把密码、令牌写进日志。
- **不要改前端字段名**：若确需变更，必须同时修改 `swagger.json`、`bookWeb/src/types/api.ts` 与相关视图。

---

## 10. 联调与自测

### 10.1 启动顺序

```powershell
# 1. 后端
cd bookManage
.\mvnw.cmd spring-boot:run        # 监听 8080

# 2. 前台
cd bookWeb
pnpm install
pnpm run dev                      # 监听 5173，/api 代理到 8080

# 3. 管理后台（可选，与前台端口错开）
cd bookAdmin
pnpm install
pnpm run dev                      # 监听 5174，/api 代理到 8080
```

> 后台的 `/api` 走 Vite 代理，但后端 CORS 白名单仍须包含 `http://localhost:5174`（已在 `application.yml` 与 `BookProperties` 默认值中补齐），否则浏览器会拦跨域请求。

### 10.2 手工验收路径

1. 注册新账号 → 自动登录（默认 `READER`）
2. 检索图书 → 预约 → 「我的借阅」可见 `PENDING` → 取消 → 库存回补
3. 用管理员账号登录 → 数据概览有数据 → 审核预约 → 借出 → 读者侧发起还书 → 管理员验收
4. 分类管理：新增 → 编辑 → 删除（有书时报错）
5. 图书管理：新增 → 编辑 → 库存调整 → 删除（有未完成借阅时报错）
6. 用户管理：改角色 / 禁用 → 被禁用用户请求返回 401/403
7. 等待 accessToken 过期（或手动清空 `book_access_token`）→ 触发任意请求 → 观察是否自动刷新成功
8. 后台用 `LIBRARIAN` 登录 → 添加推荐位 → 前台图书检索页顶部出现「编辑推荐」横滑区
9. 后台把某本推荐图书**下架** → 前台该条目应立刻消失（推荐位要求「已启用 + 图书已上架」）
10. 后台用 `ADMIN` 登录 → 改站点名称与公告 → 刷新前台，标题与公告条同步变化
11. 后台重置某读者密码 → 该读者在另一浏览器访问令牌过期后应被登出，而**不能**继续续期
12. 后台查看操作审计日志 → 上述推荐位、上下架、站点配置、重置密码四类操作均应有记录
13. 用 `READER` 账号登录后台 → 应被拦在「没有后台访问权限」提示页

> 第 8–13 条依赖新增的 3 张表（`b_featured_book` / `sys_site_setting` / `sys_audit_log`），**必须先重新执行 `DBInitial.sql`**，否则相关接口会报表不存在。

### 10.3 前端质量命令

两个前端工程命令一致：

```powershell
pnpm run type-check
pnpm run lint
pnpm run build
```

`bookAdmin` 当前只提供 `type-check` 与 `build`（未配置 ESLint）。

---

## 11. 一致性问题处理情况

| # | 问题 | 位置 | 状态 |
| --- | --- | --- | --- |
| 1 | 数据库凭据明文且为公网地址 | `application.yml`、`generationData.py` | ✅ 已外置为环境变量；⚠️ **历史提交中的凭据仍需轮换** |
| 2 | `b_borrow_order.status` 仅 5 态，缺 `RETURN_REQUESTED` / `CANCELLED` / `OVERDUE` | `DBInitial.sql` | ✅ 已补全注释 |
| 3 | 造数脚本库名 `library`，与 `book_system` 不一致 | `generationData.py` | ⏳ 待统一 |
| 4 | `pom.xml` 缺 ORM / JWT 依赖，且 `application.yml` 存在 Redis、MyBatis-Plus 悬空配置 | `pom.xml`、`application.yml` | ✅ 已补齐 ORM/JWT，并移除悬空配置（不引入 Redis） |
| 5 | CORS 全放行且允许携带凭证 | `WebMvcConfig` | ✅ 已收敛为配置项白名单 |
| 6 | 根目录缺少 `.gitignore` | 仓库根目录 | ✅ 已新增 |
| 7 | `bookWeb/README.md` 命令缺空格、目录树与实际文件不符 | `bookWeb/README.md` | ✅ 已修正 |
| 8 | 契约未定义刷新令牌的存储载体，而退出/改密要求令牌可主动失效 | `swagger.json` | ✅ 新增 `sys_refresh_token` 表承接 |
| 9 | 读者发起归还时的说明无字段承载 | `swagger.json`、`DBInitial.sql` | ✅ 新增 `b_borrow_order.return_remark` |
| 10 | 管理后台需编排前台首页展示位，但图书表没有承载字段 | `DBInitial.sql` | ✅ 新增 `b_featured_book`（独立表，避免污染 `b_book` 业务字段） |
| 11 | 站点公告 / 主题等展示项无处配置，且数量会随运营增加 | `DBInitial.sql` | ✅ 新增 `sys_site_setting` 键值表，键名由 `SiteSettingKeys` 白名单收敛 |
| 12 | 管理员关键操作无留痕，无法追溯责任 | `DBInitial.sql` | ✅ 新增 `sys_audit_log`，刻意不设外键并冗余 `operator_name` |
| 13 | 新增 3 张表未纳入造数脚本，演示环境无推荐位 / 站点配置数据 | `generationData.py` | ⏳ 待补 |
| 14 | `bookAdmin` 依赖版本（Vite 6 / Pinia 3 / TS 5.7）低于 `bookWeb`（Vite 8 / Pinia 4 / TS 6） | `bookAdmin/package.json` | ⏳ 待统一，二者 API 用法一致、不影响运行 |

---

## 12. 代码审查与修复记录（2026-09-28）

对后端全量代码做了一次审查，以下为发现并已修复的问题。**改动相关代码前请先读一遍，避免把已修的坑重新踩回去。**

### 12.1 业务逻辑缺陷

| # | 严重度 | 问题 | 修复 |
| --- | --- | --- | --- |
| 1 | 严重 | **逾期图书无法还书**。`requireOrder()` 会把 `BORROWED` + 逾期推导为 `OVERDUE`，而 `requestReturn()` 只放行 `BORROWED`，于是逾期订单发起还书时被 409 拒绝——这与状态机图中 `OVERDUE ──return──→` 的约定直接矛盾 | `OrderService.requestReturn()` 同时放行 `BORROWED` 与 `OVERDUE` |
| 2 | 严重 | **"最后一个管理员"保护误伤**。原逻辑用 `countActiveAdmin() <= 1` 判断，会把被调整的目标账号自己算进去：管理员被停用后再想改回管理员，会因自己不在"启用管理员"中而永远被拦截 | 新增 `countActiveAdminExcluding(id)`；仅当目标**当前**是启用管理员、且本次调整会取消该身份时才校验 |

### 12.2 健壮性与可维护性

| # | 问题 | 修复 |
| --- | --- | --- |
| 3 | 参数校验失败与请求体解析失败均不记日志，只有一句笼统提示，联调时无法定位是哪个字段出错 | `GlobalExceptionHandler` 补 `log.warn`，输出 `requestId` 与字段错误明细 |
| 4 | 三个分页接口返回 `R<Object>`，丢弃了泛型信息 | 改为 `R<PageResult<UserVO>>` / `R<PageResult<BookVO>>` / `R<PageResult<OrderVO>>` |
| 5 | `JwtAuthenticationFilter` 手工拼装 `LoginUser`，与 `LoginUser.from(SysUser)` 重复 | 复用 `LoginUser.from()`，消除两处构造逻辑不一致的风险 |
| 6 | `BookService.adjustStock()` 存在未使用的局部变量 | 改为显式的存在性校验调用，并说明它承担的职责 |
| 7 | `countByIsbn()` 传入 `null` 时未声明 `jdbcType`，MyBatis 按 `OTHER` 传空值，部分驱动会拒绝 | 补 `#{excludeId,jdbcType=BIGINT}` |
| 8 | `RefreshTokenMapper.deleteExpiredBefore()` 无调用方，属死代码 | 移除方法，改为类注释 TODO，说明清理任务待补 |
| 9 | `AuthService.login()` 会写入刷新令牌却没有事务，与 `register` / `refresh` 不一致 | 补 `@Transactional` |

### 12.3 Spring Boot 4 / Security 7 适配坑

这几个类的包路径在 Spring Boot 4 时代发生了迁移，**照抄 Spring Boot 2/3 的写法会直接编译失败**：

| 类 | 旧包名（Boot 2/3） | 本项目的包名 |
| --- | --- | --- |
| `OncePerRequestFilter` | `org.springframework.security.web.filter` | `org.springframework.web.filter` |
| `AuthenticationEntryPoint` | `org.springframework.security.web` | `org.springframework.security.web`（未变） |
| `ObjectMapper` | `com.fasterxml.jackson.databind` | `tools.jackson.databind`（**Jackson 3**） |

另外两点：

- **Jackson 3 的注解坐标不可用**（`tools.jackson.core:jackson-annotations` 在中央仓库不存在）。因此统一响应的"空字段不序列化"改为在 `application.yml` 配 `spring.jackson.default-property-inclusion: non_null`，而不是在 `R<T>` 上加 `@JsonInclude`。
- **MyBatis 注解里 `<script>` 块内外对 `<`、`>` 的处理不同**：`<script>` 内必须写成 `&lt;` / `&gt;`（会被当 XML 解析），而普通 `@Select` 字符串里要直接写 `<` / `>`（不会被转义）。混用会产生语法错误或直接报错。

---

### 12.4 前端修复

前端虽已按契约实现，但审查中发现了若干真实缺陷，均已修复：

| # | 严重度 | 问题 | 修复 |
| --- | --- | --- | --- |
| 1 | 严重 | **借阅日期筛选必然失败**。`OrdersView` 用 `<input type="date">`，取值是 `YYYY-MM-DD`，而契约里 `from` / `to` 是 `format: date-time`。后端 `OrderService.parseDateTime()` 先试 `LocalDateTime.parse` 再试 `OffsetDateTime.parse`，两者都无法解析纯日期，最终返回 400 | `OrdersView` 补齐时间部分：起点补 `T00:00:00`，终点补 `T23:59:59`，保证闭区间覆盖整天 |
| 2 | 中 | **删除后分页越界**。删掉末页最后一条后总页数减少，前端仍用旧页码请求，列表永久空白且 PaginationBar 不会纠正 | 图书、借阅、用户三个列表页在加载后判断 `page > pages`，自动回退到最后一页 |
| 3 | 中 | **改密后无提示被踢出**。后端改密成功会吊销该用户全部刷新令牌，但前端仍保留本地会话，用户会在访问令牌过期后被静默踢回登录页 | `ProfileView` 改密成功后主动 `auth.reset()`，并提示"请使用新密码重新登录" |
| 4 | 中 | 令牌续期失败时 `App.vue` 只调用 `auth.reset()`，界面莫名跳回登录页 | `handleExpired` 增加 toast 提示"登录状态已失效" |
| 5 | 低 | 库存调整数量未校验整数。`v-model.number` 在输入 `1.5` 时写入小数，原判断仅挡住 0 与空值，小数会提交并触发 400 | `BooksView` 改用 `Number.isInteger` 拦截，并把"数量"与"原因"的提示分开 |
| 6 | 低 | 新增图书未校验 `totalStock`，契约为 0~100000 的整数 | `BooksView` 提交前拦截 |
| 7 | 低 | 详情弹窗与操作弹窗共用同一个 `<form>`，回车提交会走到 `submitAction()`，在 `detail` 态下不发请求却静默关闭、误报成功 | `OrdersView` 对 `detail` 态显式拦截 |

---

## 13. 管理后台 bookAdmin（2026-09-28）

后台独立成应用 `bookAdmin`，与前台 `bookWeb` 分离：独立端口（5174）、独立登录、独立令牌键（`book_admin_*`）。

### 13.1 新增数据表

| 表 | 用途 | 关键设计 |
| --- | --- | --- |
| `b_featured_book` | 前台首页推荐位 | `uk_book_id` 保证一本书只占一个位；`position` 越小越靠前 |
| `sys_site_setting` | 站点展示配置 | 键值结构，新增展示项不必改表；键名由 `SiteSettingKeys` 白名单收敛 |
| `sys_audit_log` | 操作审计日志 | **不设外键**：操作员被删时级联删除会抹掉其历史记录，与审计目的冲突；冗余 `operator_name` 以便账号改名后仍可追溯 |

图书软删除时会同步清理其推荐位——`ON DELETE CASCADE` 对软删除不生效，不清会留下指向已下架图书的展示位。

### 13.2 新增接口

见 [README 管理后台接口表](../README.md#管理后台专属接口契约外供-bookadmin-使用)。

几条权限划分的取舍：

- 推荐位与馆藏归 `LIBRARIAN`：属于日常馆藏运营。
- 站点文案、主题、审计日志归 `ADMIN`：前者影响整站观感，后者可回溯他人操作。
- 重置他人密码归 `ADMIN`：会让对方全部设备下线，风险高；且管理员无从得知他人旧密码，安全边界完全由接口权限兜底，因此**必须**同时吊销目标用户的刷新令牌。

路径统一挂 `/api/admin/users/{id}/password` 而不是 `/api/users/{id}/password`：后者会与 `/api/users/profile/password`（本人改密）产生模式歧义。

### 13.3 审计的写入时机

`AuditLogService.record()` 刻意使用**默认事务传播**而不是 `REQUIRES_NEW`：审计应与业务结果的成败保持一致，若业务回滚而审计单独提交，就会留下一条"发生过但没生效"的假记录。

写入异常被吞掉并记录 error 日志——审计是旁路能力，它失败不该把正常的业务操作一起拖垮。

### 13.4 已接入审计的动作

推荐位增删改、站点配置修改、用户角色/状态调整、重置他人密码、图书上下架与删除。

### 13.5 前台如何消费

| 后台操作 | 前台表现 |
| --- | --- |
| 编排推荐位 | `bookWeb` 图书检索页顶部「编辑推荐」横滑区（`GET /api/featured-books`） |
| 站点配置 | 站点标题、公告条、主题换肤（`GET /api/site-settings`） |

推荐位有两个硬性过滤：**已启用** 且 图书本身 **已上架**，缺一不露出。

### 13.6 新增 / 改动的后端文件

新增：

| 层 | 文件 |
| --- | --- |
| 实体 | `FeaturedBook`、`SiteSetting`、`AuditLog` |
| 枚举 | `AuditAction`、`AuditTargetType` |
| 常量 | `SiteSettingKeys`（配置键白名单，防止任意键写入） |
| Mapper | `FeaturedBookMapper`、`SiteSettingMapper`、`AuditLogMapper` |
| Service | `FeaturedBookService`、`SiteSettingService`、`AuditLogService` |
| Controller | `FeaturedBookController`、`SiteSettingController`（前台只读）<br>`AdminFeaturedBooksController`、`AdminSiteSettingsController`、`AdminAuditLogsController`、`AdminUserController` |
| DTO | `AdminRequest`（内部 record 集合）、`FeaturedBookVO`、`SiteSettingsVO`、`AuditLogVO` |
| 请求体 | `UserRequest.PasswordReset` |

改动：

- `UserService`：新增 `resetPassword()`，并把原来的「只打日志」改为写审计。
- `OrderService`：新增 `listByUser()`，与管理端列表共用查询，只是把 `userId` 固定。
- `BookService`：上下架与删除写审计；删除时主动清理推荐位。
- `SecurityConfig`：新增 12 条鉴权规则。
- `application.yml` 与 `BookProperties`：CORS 白名单放行 5174。

### 13.7 遗留事项

- 新增 3 张表尚未纳入 `generationData.py` 造数脚本，演示环境没有推荐位 / 站点配置数据。
- 站点配置的 `bannerImage`（首页横幅）目前只落库，前台尚未渲染横幅区块。
- 审计日志无归档/清理策略，长期运行需评估表体积。
- `bookAdmin` 未配置 ESLint，只有 `vue-tsc` 类型检查与 `vite build` 两道保障。

---

## 14. 前端可选增强（P7，非阻塞）

- 引入 `vue-router` 替换两个前端里 `App.vue` 的 hash 手工切换，补齐 404 页与路由守卫。
- 清理 `bookWeb/package.json` 中未启用的 `unplugin-auto-import` / `unplugin-vue-components`。
- 补充 `src/utils/` 与组合式函数，把 `OrdersView.vue`、`BooksView.vue` 中超过 100 行的业务逻辑抽离为 `useOrders.ts`、`useBooks.ts`。
- 图书封面改为可配置上传（当前仅 `coverUrl` 字符串）。
- 逾期自动提醒 / 定时任务（`OVERDUE` 落库后可实现）。
- 统一 `bookAdmin` 与 `bookWeb` 的依赖版本（当前后台固定在 Vite 6 / Pinia 3 / TS 5.7）。

---

## 15. 前后端契约对账（2026-09-28）

对「后端 12 个 Controller」与「两个前端的全部调用点」做了一次逐项比对，确认路径、方法、请求参数、响应字段是否一致。

### 15.1 结论

路径与方法、请求体字段、响应字段**全部对齐**，未发现字段名或类型错配。逐类核对结果：

| 检查项 | 结论 |
| --- | --- |
| 路径与方法 | ✅ 前端 55 处调用全部命中后端 42 个端点，无未定义路径 |
| 分页结构 | ✅ 后端 `PageResult{page,size,total,pages,records}` 与前端 `PageData` 完全一致 |
| 响应 VO vs TS 类型 | ✅ `UserVO`/`BookVO`/`OrderVO`/`CategoryVO`/`DashboardVO`/`FeaturedBookVO`/`SiteSettingsVO`/`AuditLogVO` 字段逐一对应 |
| 请求 DTO | ✅ 必填约束与前端表单校验一致，无非空字段漏传 |
| 枚举取值 | ✅ `decision` 用 `APPROVE`/`REJECT`（注意不是 `APPROVED`）；`condition` 用 `GOOD`/`DAMAGED`/`LOST`；主题 `DEFAULT`/`DARK`/`WARM` 与 `SiteSettingKeys.THEMES` 一致 |
| 筛选空值 | ✅ 全部筛选项用空串 `''` 作「不限」，被 `toQuery` 丢弃，不会把 `ALL` 之类的非法值传给枚举参数 |
| 日期格式 | ✅ `from`/`to` 补 `T00:00:00` / `T23:59:59`；`dueAt` 用 `toISOString()`；`publishDate` 为 `YYYY-MM-DD` |
| 令牌与跨域 | ✅ 前后台令牌键分离（`book_*` / `book_admin_*`）；CORS 白名单含 5173 与 5174 |

### 15.2 对账中发现并修复的数据同步问题

| # | 严重度 | 问题 | 修复 |
| --- | --- | --- | --- |
| 1 | 中 | **改书后推荐位不刷新**。`bookWeb/BooksView` 只在 `onMounted` 拉一次推荐位，此后保存、库存调整、预约、下架都只刷列表。推荐位冗余了书名与库存快照，下架后顶部推荐区仍展示该书，读者点击即 404 | 新增 `refreshBooks(page)`，四个写操作后同时刷新列表与推荐位 |
| 2 | 中 | **登录后站点配置不生效**。`App.vue` 仅在 `onMounted` 且已有令牌时才 `site.load()`。首次访问是未登录状态，登录成功后不再补拉，站点名称、公告条与主题会一直停在默认值 | 改为 `watch(auth.authenticated)`，登录成功后触发加载 |

### 15.3 已知但不修的行为

- 后台修改站点配置后，**前台需刷新页面才会生效**（配置在登录时拉取一次，未做轮询或推送）。
- `OrderVO` 不返回读者的还书说明（`b_borrow_order.return_remark`）：契约未定义该出参，后端只落库不回显，属于有意为之，后续若要展示需同步改契约与 VO。
