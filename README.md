# 循物二手交易平台 v3.0.0 · Java + MySQL 课程版

在上传的 v2.0 源码上续做，并依据两份交接文档补齐 v2.1 的商品练习要求，再增加 MySQL 持久化和注册登录。前端仍为 HTML/CSS/原生 JavaScript，后端仍为 Spring Boot 分层 REST。

## 快速运行（Windows / IDEA）

需要 **JDK 17 或 21、MySQL 8.0.16 以上（推荐 MySQL 8.4）**。交付 JAR 可直接运行；仅运行项目不需要 Node、全局 Gradle 或 Docker。

1. 解压到新目录，避免与旧版混合。保留 `release/xunwu-demo-3.0.0.jar`。
2. 在 MySQL Workbench / IDEA Database / MySQL 命令行连接本机 MySQL。修改 `database/create-database.sql` 中的示例密码，然后执行此文件，创建 `xunwu` 库及应用账号。应用自动创建十二张表。
3. 复制 `.env.example` 为 `.env`，填写实际 `DB_USER`、`DB_PASSWORD`、`DB_URL`，修改管理员和示例用户初始密码。`.env` 不提交、不传给别人。
4. 在项目目录的 PowerShell 执行 `powershell -ExecutionPolicy Bypass -File .\start.ps1`。该参数只针对这次脚本进程，不修改系统执行策略。Linux/macOS 执行 `./start.sh`。
5. 等待 `Started DemoApplication`，浏览器访问 **http://localhost:8080**。不要双击 HTML。

### 登录账号

- 管理员：登录页点“管理员”，账号 `admin01`，密码为**首次启动时**的 `XUNWU_ADMIN_PASSWORD`。
- 示例用户：首次设置 `XUNWU_SEED_DEMO=true` 时，手机号 `19900000001` 至 `19900000005`，密码为首次的 `XUNWU_DEMO_PASSWORD`。
- 新用户：点击“注册账号”，填写昵称、手机号和 8—72 位密码。买家卖家统一为普通用户。
- `XUNWU_SEED_DEMO=false` 可从空市场开始。管理员仍会创建，用户自行注册。
- 重启不会重置数据或密码；修改 `.env` 的初始密码不会修改已存在账号。不要为了切换账号删除数据库。
- 数据库密码、用户登录密码、管理员登录密码是三种不同的密码。

### 在 IntelliJ IDEA 修改和运行

打开包含 `build.gradle` 的项目根目录，Project SDK 和 Gradle JVM 选 JDK 17/21，使用项目 Gradle Wrapper 同步。运行 `com.example.demo.DemoApplication`。

**IDEA 直接运行 Java 不会自动读取 `.env`**：在 Run Configuration 的 Environment variables 填入 `.env` 的键值（无需注释），或使用支持 env 文件的 IDE 配置功能。启动脚本会自动读取 `.env`。Gradle 首次同步需要网络。

构建 JAR：`./gradlew bootJar`（Windows 使用 `gradlew.bat`），产物在 `build/libs/`。替换 `release/` 中同名 JAR 前先停止运行中的程序，再启动；不要一边覆盖 JAR 一边继续访问旧进程。

## 已完成

- 注册、登录、退出、用户/管理员分权；PBKDF2 随机盐密码哈希、HttpOnly 会话 Cookie、CSRF 校验；旧 `X-Demo-User/Admin` 请求头不再有效。
- 商品查询、分类、关键词、market/all/mine 范围、新增、编辑、审核、删除；Builder、ProductRepository 六方法及构造注入。
- 多图（最多 4 张，每张约 1 MB；存储在 MySQL 的图片文本字段）、收藏、个人地址。
- 快递/自提订单、模拟付款、取消、发货、收货、整单退款、双方文字评价、会话消息、举报处理。
- MySQL 十二张表，外键、唯一约束、CHECK、事务回滚与数据库行锁。服务器重启后业务数据保留。
- 桌面和手机布局，中文界面；Java 应用集合处理使用普通循环，事务使用自定义 Operation 与匿名内部类。

支付、物流、退款到账仍为课程状态模拟；聊天为请求刷新而非 WebSocket 推送。未实现找回密码、线上支付或互联网生产部署。

## 数据库与报告边界

本轮只有源码与两份交接文档，没有收到最终实验报告原件、ER 图或旧 SQL。实现依据现有 12 个模型和交接约束；**不声称已与原报告逐字段核对**。

保留十二张业务表，不新增成色、评分、管理员姓名或会话商品关联表。为登录新增 `users.password_hash`、`admins.password_hash`；为数据库防止重复有效订单新增 `orders.active_product_id` 生成列与唯一索引。字段细节见 `docs/数据库设计.md`。

## 测试

自动测试会清空并重建夹具，**必须使用独立数据库**，名称以 `_test` 结尾。先建立 `xunwu_test`，授予测试账号该库权限，然后设置 `TEST_DB_USER`、`TEST_DB_PASSWORD`；如有需要设置 `TEST_DB_URL`。默认测试地址为本机 3306 的 `xunwu_test`，不会使用 `.env` 的业务库。

PowerShell：

```powershell
$env:TEST_DB_PASSWORD = '你的测试库密码'
.\gradlew.bat test bootJar
```

Linux/macOS：`TEST_DB_PASSWORD='你的测试库密码' ./gradlew test bootJar`。

测试结果和实际验收范围见 `docs/验收记录.md`。IDEA 请求演示见 `requests/product-crud.http`、`requests/platform.http`，现在先登录获取会话和 CSRF token，不再用演示身份头。

## 文件导航

- `src/main/java/.../controller`：HTTP 入口、状态码、认证接口。
- `service`：交易规则、权限与 DTO；`repository`：JDBC 和事务；`model`：十二类业务数据；`dto`：请求/响应结构。
- `security`：密码哈希、会话与 CSRF 验证。
- `src/main/resources/db/schema.sql`：应用建表 SQL，`database/schema.sql` 是同内容的教学副本。
- `docs/逐文件修改说明.md`：本轮改哪里、改什么、怎么验证。
- `docs/学习与答辩指南.md`：C++ 基础到 Java / Spring / 数据库的阅读路线。
- `练习要求对应说明.md`：v2.1 教学要求的逐项核对。

## 常见问题

- `Access denied`：核对数据库账号/密码及授权；不是网站账号。
- `Unknown database`：先执行建库 SQL。
- `Communications link failure`：MySQL 未启动、端口不符或 URL 错误。
- 首次启动提示密码长度：在启动环境中设置管理员初始密码；选择示例数据时还要设置用户初始密码。
- 8080 被占用：`.env` 设置 `PORT=8081`，访问对应端口。
- 登录后 403 页面凭据失效：刷新页面重新登录；不同标签页退出会使原会话失效。
- `Unsupported class file` / Gradle JVM 不兼容：选 JDK 17/21，避免用 JDK 25/26 运行该 Gradle 8 Wrapper。
- 数据仍是旧数据：这版重启保留 MySQL 数据，初始种子不会覆盖已有库。需重新演示时创建另一个空库，不要删除要保留的库。

此交付不包含 `.idea`、`.gradle`、绝对 JDK 路径、测试数据库文件或个人凭据。
