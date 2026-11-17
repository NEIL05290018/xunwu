# 循物二手交易平台 v4.0.0 · Spring Boot + MyBatis + MySQL

从 v3.0 JdbcTemplate 版升级为 **Mapper 接口 + Mapper XML + Service 事务**。JDK 17、Gradle Wrapper 8.14.3、Spring Boot 3.5.16、MyBatis Starter 3.0.4、MySQL 8.0.16+（推荐 8.4）。前端继续使用原生 HTML/CSS/JavaScript。

**先看完整交接：[`二手交易平台-v4.0-完整交接文档.md`](二手交易平台-v4.0-完整交接文档.md)**。

## 1. 拉取代码

停止正在运行的旧版程序，在项目根目录执行：

```powershell
git status
git pull --ff-only origin main
```

有本地修改时先检查、提交或暂存，不要直接强制覆盖。IDEA 中也可以使用 Git → Pull。Gradle JVM 和 Project SDK 都选 JDK 17，分发选 Wrapper，重新加载 Gradle。

GitHub 只同步代码，不同步 MySQL 业务数据和 `.env`。首次克隆地址：`https://github.com/NEIL05290018/xunwu.git`。

## 2. 本机创建 MySQL 数据库

用 MySQL Workbench、IDEA Database 或命令行连接自己的 MySQL 服务，先执行 `SELECT VERSION();`。

### 从空数据库开始（推荐首次部署）

打开 `database/create-database.sql`，把内容复制到 SQL 查询窗口，在窗口中修改数据库账号示例密码，再执行。不要把真实密码写回仓库中的 SQL。该脚本创建 `xunwu` 数据库与应用账号；应用首次启动自动执行 `src/main/resources/db/schema.sql`，创建十二张表。

如果电脑已经有旧 `xunwu` 库且不需要复用，可**新建另一个库**（例如 `xunwu_v4`），相应修改建库/授权 SQL 和 `.env` 中的 DB_URL。不要直接删除旧库。

### 继续使用 v3.0 已有数据

停止旧程序，先备份数据库，选中 v3.0 的 MySQL 数据库，执行一次 `database/migrate-v3-to-v4.sql`。脚本保留业务记录和密码，调整会话唯一键及补充报告中的约束。DDL 会自动提交，若中途报错需按已执行步骤处理，不能盲目整份重跑。

此脚本**不适用于本次上传的 SQLite 十三表旧 SQL**。v4 启动会检查会话新结构，未升级会明确报错，不会自动重建或清空旧库。

## 3. 配置 .env

把根目录 `.env.example` 复制为 `.env`（不是 `.env.txt`），填写：

```properties
DB_URL=jdbc:mysql://localhost:3306/xunwu?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8
DB_USER=xunwu
DB_PASSWORD=你为数据库账号设置的密码
PORT=8080
XUNWU_ADMIN_PASSWORD=你设置的管理员初始密码
XUNWU_SEED_DEMO=true
XUNWU_DEMO_PASSWORD=你设置的示例用户初始密码
```

网站密码需要 8—72 个字符。数据库密码、网站管理员密码、普通用户密码是不同用途。

**v4 新增：IDEA 从项目根目录直接运行主类，会自动读取 `.env`。** 检查运行配置 Working directory 为包含 build.gradle 的根目录。系统/运行配置里已有的同名环境变量优先于 `.env`；旧配置有错误时需同步修改或移除。`.env` 已被 Git 忽略。

上述 URL 关闭 TLS 仅用于本机课程开发。远程数据库需要按服务端要求配置 TLS，不能直接沿用本机连接参数。

## 4. 启动

### IDEA 运行源码（学习、修改时推荐）

运行 `src/main/java/com/example/demo/DemoApplication.java`，等待 `Started DemoApplication`，访问 http://localhost:8080。首次 Gradle 同步需要联网。

### 使用附带 JAR（无需重新编译）

Windows PowerShell 在根目录执行：

```powershell
java -version
$env:JAVA_HOME
powershell -ExecutionPolicy Bypass -File .\start.ps1
```

确认终端及 JAVA_HOME 指向 JDK 17。Linux/macOS：`./start.sh`。

脚本运行 `release/xunwu-demo-4.0.0.jar`。修改源码后它不会自动更新；重新构建用 `gradlew.bat bootJar`，停止程序后把 `build/libs/` 的新 JAR 替换到 `release/`。

### 登录

- 普通用户：手机号 `19900000001` 至 `19900000005`，密码为首次自动初始化时的 `XUNWU_DEMO_PASSWORD`。
- 管理员：登录页选“管理员”，账号 `admin01`（示例中另有 admin02），密码为首次的 `XUNWU_ADMIN_PASSWORD`。
- 新用户自行注册；同一账号既能买也能卖。
- 设置 `XUNWU_SEED_DEMO=false` 可从空市场开始，管理员仍创建。
- 重启不重置数据或密码，修改初始密码配置不改变已存在账号。

## 5. SQL 示例数据与报告核对

上传的实验报告关系模型是十二表；上传的 SQL 是 SQLite 十三表旧版，ER 图也保留评分、会话商品联系等旧设计。本版以报告正文为准，移除旧版字段，并保持原有商品/订单/用户示例关系。

`database/insert-demo.sql` 是转换后的 **MySQL 可执行示例数据**：七件原始商品加六件展示商品，成色并入描述，图片替换为项目实际资源，密码改为有效 PBKDF2 哈希。

推荐用应用自动初始化，自选初始密码。若老师要求手动执行 SQL，可在**空库**先执行 `database/schema.sql` 再执行 `database/insert-demo.sql`；此时固定演示密码为 `DemoAdmin_2026` / `DemoUser_2026`，应用识别已有数据后不会用 `.env` 覆盖它们。两种初始化方式选一种，不重复插入。

详见 `docs/报告与SQL差异核对.md`。原始报告含个人信息，没有原样发布到公开 GitHub。

## 6. 功能与实现

- 注册、登录、退出、会话鉴权、CSRF、用户/管理员权限隔离。
- 商品完整 CRUD、动态查询、Builder、Repository 六方法、图片、审核、收藏。
- 快递/自提下单、取消、模拟付款、发货/收货、退款、文字评价、聊天、举报。
- 十二业务表的 MyBatis 接口/XML；`#{}` 参数绑定、显式 resultMap、自增键回填。
- Service 的 `@Transactional` 与订单/商品行锁；MySQL 外键、唯一及 CHECK 约束。
- 仅页面组合响应读取十二表后过滤；写操作直接执行对应 Mapper SQL，不再做全量副本差异写回。

支付、物流、退款到账是状态演示。图片以 Data URL/本地路径存数据库，聊天通过请求刷新，不含真实支付或 WebSocket。没有改成学生系统。

## 7. 测试与学习

测试需独立 MySQL 库，默认 `xunwu_v4_test`。创建该库并授予测试账号权限，然后设置 `TEST_DB_PASSWORD`；可选 `TEST_DB_URL` / `TEST_DB_USER`。测试会重置夹具，数据库名必须以 `_test` 结尾，不要指向业务库。

```powershell
$env:TEST_DB_PASSWORD = '你的测试库密码'
.\gradlew.bat test bootJar
```

- `docs/验收记录.md`：实际验证结果与边界。
- `docs/学习与答辩指南.md`：从一次商品查询开始学习。
- `docs/逐文件修改说明.md`：修改位置与验证方法。
- `requests/`：IDEA HTTP 请求示例，先取 CSRF 并登录。
- `database/verification.sql`：关系与业务一致性查询。

## 常见问题

- Git 提示连不上 127.0.0.1:7892：检查本机代理端口；已验证使用 7890 的电脑可配置 `git config --global http.proxy http://127.0.0.1:7890`，前提是代理软件运行。此 Git 设置不会自动配置 Gradle。
- JDK 26 不兼容 Gradle：项目 SDK、Gradle JVM 选 17；不需要升级项目 Gradle 或卸载 26。
- Access denied：检查 DB_USER/DB_PASSWORD 和 MySQL 授权，不是网站密码。
- Unknown database：先建库。Communications link failure：检查 MySQL 服务、端口和 DB_URL。
- 旧数据库结构提示：备份后执行 v3→v4 脚本，或配置新空库。
- 登录失败：区分自动种子与手动 SQL 的初始密码；已有库不会重置密码。
- 本地修改与 Pull 冲突：保留现场，先查看 git status，不要使用 reset --hard。
