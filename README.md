# 幻想图书馆 · Fantasy Library

[中文](README.md) · [English](README.en.md) · [日本語](README.ja.md)

![license](https://img.shields.io/badge/license-MIT-c8a45c)
![spring](https://img.shields.io/badge/Spring%20Boot-3.5-6db33f)
![vue](https://img.shields.io/badge/Vue-3.5-42b883)
![kotlin](https://img.shields.io/badge/Kotlin-2.2-7f52ff)
![android](https://img.shields.io/badge/Android-minSdk%2026-3ddc84)

一个**完全本地运行**的「中古幻想图书馆」：复古书架与翻书动画呈现的动漫 / 游戏馆藏系统，配套
Spring Boot REST API、Oracle 数据库、MinIO 私有媒体存储，以及一个原生 Android 测试客户端。

所有服务都跑在自己的电脑上（Docker Compose），不依赖任何第三方在线服务；
馆藏数据来自 Oracle，图片与视频经后端从 MinIO 流式提供。

---

## 目录

- [功能概览](#功能概览)
- [技术栈](#技术栈)
- [仓库结构](#仓库结构)
- [快速开始](#快速开始)
- [Android 客户端](#android-客户端)
- [主要 API](#主要-api)
- [测试](#测试)
- [安全与隐私](#安全与隐私)
- [素材与许可](#素材与许可)
- [原有详细文档](#原有详细文档)

## 功能概览

**公开站（阅读端）**

- 复古书架首页，书架 / 分类两种入口，书本展开与翻页动画
- 动漫与游戏馆藏列表、分页、按分类与标签筛选、关键词检索
- 作品详情：简介、标签、插图、制作信息 / 开发商与平台
- **放映室**：Media3 风格的自研播放器，从 MinIO 流式播放并支持 HTTP Range 拖动
- 游戏下载节点选择、下载队列与说明编辑
- 账号体系：注册 / 登录 / 私人书架（追番、愿望单、观看历史）
- 社区手稿：发帖、评论、点赞、举报

**管理端（馆务）**

- 独立的管理端站点与独立会话（与公开站账号隔离）
- 作品、分集、播放源、下载节点、图片、标签的全量管理
- 评论与举报审核、成员管理

**Android 客户端（`android-client/`）**

- Kotlin + Jetpack Compose 原生应用，MVVM 分层
- 服务器地址配置（IP / 端口可改、可保存、可测连通性）
- 真实读取 Oracle 馆藏、加载 MinIO 图片、播放 MinIO 视频（含全屏与进度拖动）
- 真实 Cookie + CSRF 登录，无模拟数据、无硬编码视频地址

## 技术栈

| 层 | 技术 |
| --- | --- |
| 公开站前端 | Vue 3.5、Vue Router 4、Pinia 3、Axios、GSAP（翻书动画）、Vite |
| 管理端前端 | Vue 3.5、Vue Router 4、Pinia 3、Vite |
| 后端 | Java 21、Spring Boot 3.5、Spring Security（Cookie + CSRF 会话）、MyBatis、Jackson |
| 数据库 | Oracle Database Free（`FREEPDB1`） |
| 对象存储 | MinIO（私有桶，后端代理签名访问） |
| 部署 | Docker Compose（frontend / admin-frontend / backend / oracle / minio / minio-init） |
| Android | Kotlin 2.2、AGP 8.13、Jetpack Compose（BOM 2025.09）、Media3 1.4、Retrofit 2.11 + OkHttp 4.12、kotlinx.serialization、Coil 2.7、DataStore、minSdk 26 / compileSdk 36 |

## 仓库结构

```
fantasy-library/
├── frontend/            # 公开站（Vue 3 + Vite），Nginx 反代 /api
├── admin-frontend/      # 管理端（Vue 3 + Vite），独立会话与端口
├── backend/             # Spring Boot REST API + MyBatis + 静态演示素材
│   ├── src/main/java/com/fantasylibrary/api        # Controller
│   ├── src/main/resources/mybatis                 # Mapper XML
│   └── db/oracle/                                 # 建表与迁移脚本
├── docker/minio-init/   # 一次性初始化桶与访问密钥
├── android-client/      # 原生 Android 测试客户端（独立 Gradle 工程）
│   ├── app/src/main/java/com/fantasylibrary/android/{data,ui,di}
│   ├── docs/            # 接口契约 / 网络配置 / 验收记录
│   └── screenshots/     # 真机（模拟器）验收截图
├── compose.yaml         # 全套服务编排
├── LICENSE              # MIT
└── README.md · README.en.md · README.ja.md
```

## 快速开始

### 1. 准备环境

- Docker Desktop（Linux 容器模式）
- JDK 21（只想跑 Docker 可不装）
- Node.js 22（只想跑 Docker 可不装）
- Android Studio + SDK 36（仅 Android 客户端需要）

### 2. 配置环境变量

```bash
cp docker/compose.env.example .env.docker
# 按需填写 Oracle / MinIO 账号密码与端口
```

> `.env.docker` 已被 `.gitignore` 忽略，**任何密钥都不会进入仓库**。

### 3. 启动

```bash
docker compose --env-file .env.docker up -d --wait
docker compose --env-file .env.docker ps -a
```

| 服务 | 地址 |
| --- | --- |
| 公开站 | http://localhost:5173/ |
| 管理端 | http://localhost:5174/ |
| 健康检查 | http://localhost:8080/api/health |
| Oracle | `jdbc:oracle:thin:@//localhost:11521/FREEPDB1` |
| MinIO API / 控制台 | http://127.0.0.1:19000/ · http://127.0.0.1:19001/ |

首次在新机器启动空卷时，先在 `FREEPDB1` 创建应用账号并执行 `backend/db/oracle/install.sql` 初始化示例数据，
再导入 MinIO 素材；已有数据的机器直接挂载原有命名卷即可。

## Android 客户端

`android-client/` 是一个**独立的 Gradle 工程**，不参与 Web 端构建。

```bash
cd android-client
./gradlew :app:assembleDebug          # 产物 app/build/outputs/apk/debug/
```

在 Android Studio 里打开 `android-client/` 目录即可（Gradle JDK 选 17+）。
App 首次启动会停在「连接」页，填写电脑地址后即可读取真实数据：

| 场景 | 填什么 |
| --- | --- |
| Android 模拟器 | `10.0.2.2` + `8080`（`10.0.2.2` 指向宿主机回环） |
| 真机（USB） | 先 `adb reverse tcp:8080 tcp:8080`，再填 `127.0.0.1` + `8080` |
| 真机（局域网） | Docker 端口需绑定到 `0.0.0.0` 并放行防火墙，然后填电脑局域网 IP |

五个页面：**连接配置** → **服务状态** → **动漫资料库** → **游戏资料库** → **放映室**，
另有作品详情与登录页。合法 HTTP 只在 debug 构建放行，release 构建强制 HTTPS。

细节文档：

- [`android-client/docs/API-CONTRACT.md`](android-client/docs/API-CONTRACT.md) —— 逐条核对过的真实接口契约
- [`android-client/docs/NETWORK-SETUP.md`](android-client/docs/NETWORK-SETUP.md) —— 模拟器 / 真机连通方案
- [`android-client/docs/VERIFICATION.md`](android-client/docs/VERIFICATION.md) —— 逐项验收证据（含截图与实测输出）

## 主要 API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/health` | 应用与数据库状态（非统一包装） |
| GET | `/api/anime` | 动漫分页列表（`page` / `size` / `category` / `tag` / `q`） |
| GET | `/api/games` | 游戏分页列表 |
| GET | `/api/works/{id}` | 作品详情（含标签、插图、动漫 / 游戏信息） |
| GET | `/api/works/{id}/episodes` | 分集与播放源 |
| GET | `/api/media/catalog/{path}` | 图片（后端从 MinIO 代理） |
| GET | `/api/media/playback/{file}` | 视频（支持 HTTP Range） |
| GET/POST | `/api/auth/csrf`、`/api/auth/login`、`/api/auth/logout` | Cookie + CSRF 会话 |
| GET | `/api/me/**` | 受保护的私人书架接口 |

业务响应统一为 `{ code, message, data, errors }`；错误码与 HTTP 状态码的对应关系见
`backend/src/main/java/com/fantasylibrary/api/ApiExceptionHandler.java`。

## 测试

```bash
# 后端
cd backend && mvn test

# 公开站 / 管理端
cd frontend && npm run test
cd admin-frontend && npm run test

# Android：41 个纯逻辑测试
cd android-client && ./gradlew :app:testDebugUnitTest
# 追加 6 个真实后端契约测试（需本地服务在运行）
./gradlew :app:testDebugUnitTest -PliveBackend=true
```

Android 侧的契约测试会真实请求 `/api/health`、`/api/anime`、分集、封面字节与
`Range: bytes=0-1`，并验证错误密码确实返回 `401 INVALID_CREDENTIALS`。

## 安全与隐私

- 仓库内**没有任何真实凭据**：`.env.docker`、`*.local`、`uploads/` 全部被忽略
- 浏览器端只与 Nginx 通信，`/api` 由 Nginx 反代到 Spring Boot；Oracle 与 MinIO 不对公网暴露
- 登录态为数据库会话令牌 + HttpOnly Cookie + CSRF 头，密码使用 BCrypt 存储
- Android 客户端不持有数据库或 MinIO 凭据，只保存会话 Cookie 与 CSRF token（加密存储）
- 客户端会拦截 `localhost` / `127.0.0.1` / Docker 内部服务名等手机必然打不开的媒体地址

## 素材与许可

- 源码以 **MIT** 协议开源，见 [`LICENSE`](LICENSE)
- `frontend/src/assets/` 与 `backend/src/main/resources/static/api/media/` 下是**原创演示素材**
- **真实动漫封面与视频不在仓库**：它们由部署者自行导入并存放于 MinIO（`uploads/` 已被忽略）
- 使用本项目部署时，请自行确认所导入作品素材的授权

## 原有详细文档

以下保留项目原有的阶段性开发与运维记录（Docker 用法、数据库迁移、各阶段实现说明等）。

---

## 使用 Docker Desktop 运行

先启动 Docker Desktop，并保持 Linux 容器模式。在项目根目录的 PowerShell 或 IDEA Terminal 启动整套服务。

```powershell
Set-Location 'D:\project\fantasy-library'
docker compose --env-file .env.docker up -d --wait
docker compose --env-file .env.docker ps -a
```

修改项目代码后，执行 `docker compose --env-file .env.docker up -d --build --wait` 重新构建。运行 `docker compose --env-file .env.docker stop` 可停止服务。`minio-init` 是一次性初始化容器，显示 `Exited (0)` 表示私有桶及应用访问密钥初始化成功。

| 服务 | 本机地址 |
| --- | --- |
| 公开站 | http://localhost:5173/ |
| 管理端 | http://localhost:5174/ |
| Spring Boot 健康检查 | http://localhost:8080/api/health |
| Oracle JDBC | `jdbc:oracle:thin:@//localhost:11521/FREEPDB1` |
| MinIO API | http://127.0.0.1:19000/ |
| MinIO 控制台 | http://127.0.0.1:19001/ |

公开站和管理端各自通过 Nginx 转发 `/api/`。前端依赖在构建镜像内安装，运行时无需手动执行 `npm run dev` 或 Maven。测试环境的端口仅绑定本机。普通账号在公开站登录。管理员直接打开管理端登录，使用独立会话。MinIO 控制台使用项目根目录 `.env.docker` 中的 `MINIO_ROOT_USER` 和 `MINIO_ROOT_PASSWORD`，后端使用独立的存储桶访问密钥。

`docker/compose.env.example` 提供环境变量模板，实际 `.env.docker` 由 Git 忽略。`MINIO_LICENSE_FILE` 指向外部 MinIO 许可证。首次在新机器准备空卷时，先运行 `docker compose --env-file .env.docker up -d --wait oracle minio`，再在 `FREEPDB1` 创建对应的应用账号并恢复 Oracle 备份及 MinIO 存储桶；全新开发库可以在应用账号下运行 `backend/db/oracle/install.sql` 初始化示例数据。若修改公开站端口，在模板中填写 `VITE_PUBLIC_SITE_URL` 并重建管理端镜像。

Oracle、MinIO 和历史本地上传使用 Docker 命名卷。迁移保留了原 Windows Oracle 和 `D:\minio-data` 数据。常规停机和重启沿用同一批卷，`docker compose down -v` 会删除 Compose 数据卷。

## 删除作品

在管理端的馆藏列表点击 **删除作品**，核对标题后确认永久删除。Oracle 会同事务移除该作品、封面与插图记录、平台和标签关联、播放或下载节点、用户收藏及观看历史。社区帖子和评论会保留，帖子里的作品关联会解除。其他作品共用的站内文件会保留，独占的 MinIO 文件由清理队列自动删除。外部资源按数据库记录处理。

旧库升级时在应用账号下执行 `backend/db/oracle/16_work_retirement.sql` 后更新 Docker。

番剧评论按作品共享，游客可读，登录后可在评论下回复和点赞。管理端的「评论管理」可按番剧评论或社区评论筛选并删除留言。更新已有数据库时，先按顺序执行 `backend/db/oracle/17_screening_comments.sql` 和 `backend/db/oracle/18_screening_discussions.sql`，再重建后端、用户前端和管理端。迁移只归并符合旧播放器自动标题规则的留言，并保留作者、时间、回复和点赞。

## 管理番剧与下载节点

在管理端作品列表点击 **管理节点**。番剧先创建播放来源，再录入该来源的分集，为每一集上传 MP4/WebM 或填写 HTTPS 地址。同一集可以保留备用视频。公开详情和放映室都有横向来源选项，每个来源显示自己的分集。

游戏先在作品资料保存适用平台，再新增下载节点。节点可选择 HTTPS 地址，或上传并替换一个 MinIO 安装包。单个视频或安装包最多 5 GiB，上传过程中分段写入 MinIO。外部地址保存在 Oracle 中。资源列表中的 **下架并删除** 经确认后会删除 Oracle 的资源记录。MinIO 文件在没有其他引用时清理，暂时无法清理的文件会自动重试。

从旧版本升级时，在容器的 `FREEPDB1` 应用账号下按顺序运行 `backend/db/oracle/13_playback_nodes.sql` 至 `15_seed_playback_lines.sql`，再重建 Docker 中的后端和两个前端。示例作品的馆内和轻量视频会分别整理为两个来源，保留原视频对象。

## 开发环境

- JDK 21 和 Maven 3.6.3 或更新版本。也可以使用 IntelliJ IDEA 内置的 Maven。
- Node.js 20.19+（20.x）或 22.12+，以及 npm。

## 用 IntelliJ IDEA 打开和运行

1. 在 IDEA 中选择 **File → Open**，打开 D:\project\fantasy-library。这样可以在同一个窗口查看 frontend 和 backend。
2. 如果 IDEA 未自动导入后端，在 Project 窗口右键 backend/pom.xml，选择 **Add as Maven Project**。
3. 在 **File → Project Structure → Project** 将 Project SDK 设为 **JDK 21**。如果尚未配置，可使用 **Download JDK**。使用 Maven 面板运行时也可在 **Settings → Build, Execution, Deployment → Build Tools → Maven → Runner** 将 JRE 设为 Project JDK。
4. 打开 backend/src/main/java/com/fantasylibrary/FantasyLibraryApplication.java，通过 main 方法旁的运行按钮启动后端。
5. 在 IDEA 的 Terminal 中切换到 frontend，首次安装依赖时执行 npm ci，然后用 npm run dev 启动前端。访问 http://localhost:5173/ 浏览书架，或打开 http://localhost:5173/status 检查连接状态。只浏览书架时可以直接启动前端。

## 用 PowerShell 启动

在第一个终端启动后端。

    Set-Location 'D:\project\fantasy-library\backend'
    java -version
    mvn -version
    mvn spring-boot:run

在第二个终端启动前端。

    Set-Location 'D:\project\fantasy-library\frontend'
    npm ci
    npm run dev

如果 PowerShell 阻止执行 npm.ps1，可以使用 npm.cmd ci 和 npm.cmd run dev。

书架首页默认位于 http://localhost:5173/，前端连接状态页位于 http://localhost:5173/status，后端健康检查默认位于 http://localhost:8080/api/health。Vite 将 /api 请求转发到本机后端。开发或预览时如果更改后端端口，可以将 frontend/.env.example 复制为 frontend/.env，修改 API_PROXY_TARGET 后重启 Vite。生产环境需由静态站点所在域名的网关将 /api 转发到后端，并保留 /api 路径。

如果系统未将 Maven 加入 PATH，可以直接用 IDEA 的 Maven 面板执行目标，或在终端以完整路径调用 mvn.cmd。

## Oracle 配置与健康检查

本地开发可以将 backend/.env.example 复制为 backend/.env，并填写 Oracle 连接信息。从项目根目录或 backend 目录启动时，后端会按 Java Properties 格式自动读取这份文件。也可以在 IDEA 的 **Run → Edit Configurations → Environment variables** 中设置同名进程环境变量，进程环境变量优先。修改配置后重启后端。

| 环境变量 | 用途 |
| --- | --- |
| SERVER_PORT | 可选，默认监听 8080 |
| ORACLE_JDBC_URL | 真实 Oracle JDBC 地址，格式见示例文件 |
| ORACLE_USERNAME | Oracle 用户名 |
| ORACLE_PASSWORD | Oracle 密码 |

三个 Oracle 变量齐全时，后端通过 MyBatis 运行 SELECT 1 FROM DUAL。没有 Oracle 实例或凭据时可直接启动后端。数据库探测与 API 可用性分开表示。

    GET /api/health

    {"application":"UP","database":"NOT_CONFIGURED"}

| database 字段 | 含义 |
| --- | --- |
| NOT_CONFIGURED | Oracle 环境变量缺失或不完整 |
| DOWN | 已配置，实际探测失败 |
| UP | 真实查询成功 |

这三种数据库状态均可从可用的 API 得到 HTTP 200。API 请求失败时，前端会显示后端 API 无法访问、数据库状态未知。运行页面使用真实 REST API。测试中的替身数据只存在于测试代码。

## 构建和测试

| 目录 | 命令 | 内容 |
| --- | --- | --- |
| frontend | npm ci | 按锁文件安装依赖 |
| frontend | npm run build | Vue TypeScript 类型检查和 Vite 构建，输出到 dist |
| frontend | npm test | Vitest 健康状态与书籍阅读测试 |
| backend | mvn verify | 编译、执行后端测试并生成 target/fantasy-library-backend-0.1.0-SNAPSHOT.jar |

阶段 0 验证结果

- 前端类型检查与生产构建通过，Vitest 5 项通过。
- 后端使用 JDK 21 执行 mvn clean verify，3 项测试通过，已生成可执行 JAR。
- 本机浏览器用桌面与手机视口验证状态页，并联调了 API 离线、Oracle 未配置和实际连接失败。真实 Oracle 查询成功的分支仍需可用实例与凭据验证。

## 阶段 0 文件

    .gitignore
    README.md
    frontend/
      .env.example
      index.html
      package.json
      package-lock.json
      tsconfig.json
      vite.config.ts
      src/
        App.vue
        main.ts
        animations/entrance.ts
        api/client.ts
        api/health.ts
        components/HealthStatus.vue
        pages/StatusPage.vue
        router/index.ts
        stores/health.ts
        styles/main.scss
      tests/health.test.ts
    backend/
      .env.example
      pom.xml
      src/main/java/com/fantasylibrary/
        FantasyLibraryApplication.java
        api/HealthController.java
        api/HealthResponse.java
        config/OracleConnectionProperties.java
        config/OracleDataSourceConfiguration.java
        persistence/mapper/OracleHealthMapper.java
        service/HealthService.java
      src/main/resources/application.yml
      src/test/java/com/fantasylibrary/
        api/HealthEndpointTest.java
        service/HealthServiceTest.java

## 阶段 1 · 复古书架首页

- 首页采用木质书架，展示追番、游戏、社区、推荐、个人书架五册书。窄屏采用两层书架。
- 书目与样式映射来自前端本地配置。书架首页可独立于 API 和 Oracle 运行。
- 点击、触屏或使用键盘激活书脊会设置 Pinia 的 `selectedBookId`。切换路由会保留当前选中项，刷新页面后重新选择。
- 后续展开动画可从 `useShelfStore().selectBook(bookId)` 和书脊上的 `data-book-id` 接入。本阶段的交互范围为选中、高亮和状态提示。

### 阶段 1 文件清单

| 操作 | 文件 |
| --- | --- |
| 新增 | `frontend/src/pages/ShelfPage.vue` |
| 新增 | `frontend/src/components/LibraryHeader.vue` |
| 新增 | `frontend/src/components/shelf/Bookshelf.vue`、`BookSpine.vue`、`BookSigil.vue`、`LibraryAtmosphere.vue` |
| 新增 | `frontend/src/data/shelfBooks.ts`、`frontend/src/stores/shelf.ts` |
| 新增 | `frontend/src/assets/shelf/library-hall.webp`、`walnut-grain.webp`、`leather-grain.webp` |
| 修改 | `frontend/index.html`、`frontend/src/App.vue`、`frontend/src/router/index.ts`、`frontend/src/styles/main.scss`、`frontend/src/pages/StatusPage.vue`、`README.md` |

### 阶段 1 验证

- Vue TypeScript 类型检查、Vite 生产构建和 5 项前端测试通过。
- Java 21 环境下，3 项后端回归测试通过。
- Chrome 验证桌面悬停与键盘操作、390px 与 320px 的手机双层布局和触屏选书，以及从 /status 返回时的选中状态。
- 浏览器检查期间，首页只访问本地资源。状态页路由测试使用受控 API 响应。

<details>
<summary>本地素材及生成提示词</summary>

以下三张图片使用内置 ImageGen 生成，再压缩为项目本地 WebP。

- **library-hall.webp** — Use case: historical-scene. Background photograph for a Chinese-language medieval fantasy library website, landscape 3:2 or 16:9. A photorealistic antique European library gallery at dusk, frontal symmetrical view, richly textured deep walnut, tall shadowy bookcases and dark arched woodwork near the two far edges, a very subtle warm old brass sconce glow on each side. Dark atmospheric center with soft umber and no dominant details because a separate virtual bookshelf and title will be placed there. Closely framed architectural interior, archival atmosphere, premium warm direction of light, faint haze, readable detail at the edges. Natural materials and cinematic photographic optics. No text, no people, no UI, no giant spotlights, no blue or purple, no bright blank area.
- **walnut-grain.webp** — Use case: photorealistic-natural. Website material texture, a dead straight top-down macro photograph of a broad single sheet of old carved-library dark walnut, horizontal grain from left to right, warm brown color with natural fine rich wavy striations and a few very restrained antique scratches. Thin continuous visual detail, medium-dark brown so color is visible, matte oil finish. Flat diffuse lighting, even exposure, no chips, no board seams, no object, no text, no knots in the center, no perspective, no vignette. The entire image is one surface for cropping onto CSS architectural moldings.
- **leather-grain.webp** — Use case: photorealistic-natural. A dead straight top-down macro photograph of finely pebbled aged bookbinding leather, even subtle micro-grain with delicate random natural wrinkles and old compressed patina, charcoal gray neutral color, broad uniform sheet filling the whole frame. Soft flat diffuse light, subtle grain contrast, no patches, no cracks, no seams, no borders, no objects and no text. Neutral texture will be tinted many colors by CSS.

</details>

## 阶段 2 · 书本展开和翻页

在书架首页点击「追番」，即可打开有三组左右页的示例书。每组左页是精选轮播，右页是六条视频推荐的封面。书页使用本地羊皮纸和虚构封面素材，首页和阅读器均可独立于后端运行。

- 「上一页」「下一页」和方向键可翻页。页码以双页范围显示，并在纸叶落定时更新。
- 「合书返回」和 Esc 会合上并归还书籍，然后把键盘焦点还给书脊。再次打开从第一页开始。
- 载入、展开、翻页或归还时会显示进度提示。动画期间锁定操作，两端页码分别禁用越界按钮。
- 手机窄屏和粗指针设备默认使用低动态效果，桌面也会遵循系统的减少动态偏好。可在阅读器工具栏手动切换。
- 阶段 2 的可读书籍为「追番」。其余书籍保留阶段 1 的选中交互。

取书、开合和翻页时序使用 GSAP Timeline。翻页时提前放好底层页，并用一张以中缝为轴的双面 CSS 3D 纸叶遮住即将露出的页。[GSAP Timeline](https://gsap.com/docs/v3/GSAP/Timeline/) · [CSS transform-style](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Properties/transform-style)

### 阶段 2 文件清单

| 操作 | 文件 |
| --- | --- |
| 新增 | `frontend/src/components/reader/BookReader.vue`、`BookObject.vue`、`BookCover.vue`、`BookPage.vue`、`TurningLeaf.vue` |
| 新增 | `frontend/src/animations/readerMotion.ts`、`frontend/src/reader/turnPlan.ts`、`frontend/src/styles/book-reader.scss` |
| 新增 | `frontend/src/data/animeReader.ts`、`frontend/tests/turnPlan.test.ts` |
| 新增 | `frontend/src/assets/reader/parchment.webp`、`mist-harbor.webp`、`star-court.webp`、`bamboo-night.webp` |
| 修改 | `frontend/src/pages/ShelfPage.vue`、`frontend/src/components/shelf/Bookshelf.vue`、`frontend/src/components/shelf/BookSpine.vue`、`README.md` |

### 阶段 2 验证

- `npm run build` 执行 TypeScript 类型检查和 Vite 打包，`npm test` 执行 9 项前端测试。新增回归测试覆盖源坐标读取、两种翻页方向的纸叶内容和越界保护。
- Java 21 下复跑 3 项后端回归测试。
- Chrome 验证取书起点、3D 开合、正反向翻页、鼠标和键盘连点保护、页码、归位焦点，以及 320px 和 390px 的触屏阅读布局与低动态模式。 另验证了封面加载失败、开合期间调整窗口尺寸和系统减少动态偏好。

后续可复用 `ReaderSpread` 数据结构、`planTurn()` 纸叶映射和现有动效层。在扩大馆藏范围前，可先验收取书位置、翻页方向和动效强度。

<details>
<summary>阶段 2 本地素材及生成提示词</summary>

四张素材使用内置 ImageGen 生成，再压缩为项目本地 WebP。

- **parchment.webp** — Use case: photorealistic-natural. Full-frame flat scan of a single finely grained aged parchment folio, subtle random light brown fibers and softly mottled warm pale ivory and biscuit colors. Low contrast, medium-light and evenly lit everywhere so type can be laid on top. Fine book paper, not ragged, completely flat, no curled corners, no folds, no seams, no border, no stains with hard edges, no vignette, no writing, no objects, no gradient, no shadows. This will be an overlay for the pages of a realistic medieval fantasy web book.
- **mist-harbor.webp** — Use case: stylized-concept. A vertical 2:3 illustrated anime cover for an original fantasy story. An adolescent courier in a dark ocean-blue hooded travel coat with a leather letter bag stands on a cobbled old harbor quay, viewed three-quarter rear, a folded cream letter in hand. Warm copper dock lanterns lead into an ancient stone port under curling sea mist, an old teal sailing ship in the distance. A single moon and distant birds, intimate rather than epic. Refined original hand-painted Japanese-animation-inspired cinematic key art, nuanced brushwork, beautiful silhouettes and dusk color, blue-green and amber. Compose the courier in the lower-center third and keep major details within the frame. No lettering, labels, logos, borders, watermark, or existing characters.
- **star-court.webp** — Use case: stylized-concept. Vertical 2:3 anime book cover illustration for an original medieval fantasy story. A young woman apprentice in a dark indigo scholar's cloak and warm muted ochre scarf, small in the lower center, raises one hand toward an intricate antique armillary sphere hanging within a ruined, opened-roof observatory. A field of bright but subtle stars, midnight ink blue, bronze reflections, a hint of pale violet and antique paper in the foreground. A tiny lantern lights an astronomical journal. Original painterly Japanese-animation-inspired fantasy key art with clean readable composition and atmospheric depth, antique optics and architecture. No text, no watermark, no graphic frames, no reference to existing characters.
- **bamboo-night.webp** — Use case: stylized-concept. Vertical 2:3 anime cover art for an original quiet folktale. A young traveling messenger in a dark sage robe carries a single warm paper lantern, pausing on ancient stone steps in a layered green bamboo forest at night. A small white spirit fox waits half-hidden at the foot of an old wooden mountain gate. Beautiful pale-green moonlit bamboo, atmospheric mist, subtle night colors mixed with gold reflected around the lantern. Delicate original hand-painted Japanese-animation-inspired background art, readable silhouettes, cinematic but intimate. Keep the recognizable figures within the center, excellent edges. No text, no logos, no watermark, no borders, no existing characters.

</details>

### 阶段 2 · 近满屏双页与推荐内容

阅读器打开时按视口伸展书本，顶部工具和底部页码保留操作空间。左页提供三张精选横幅，普通动效下每 3.5 秒轮播，支持箭头、分页点及暂停。鼠标停留、键盘聚焦、后台标签、低动态模式或书本翻页期间会停下自动轮播。右页展示六张带标题和时长的本地视频示例封面，当前版面为两列三排。

| 操作 | 文件 |
| --- | --- |
| 新增 | `frontend/src/components/reader/FeaturedCarousel.vue`、`VideoRecommendations.vue`、`ReaderArtwork.vue` |
| 修改 | `frontend/src/components/reader/BookReader.vue`、`BookObject.vue`、`BookPage.vue`、`TurningLeaf.vue` |
| 修改 | `frontend/src/styles/book-reader.scss`、`frontend/src/data/animeReader.ts`、`README.md` |
| 新增 | `frontend/src/assets/reader/banner-harbor.webp`、`banner-stars.webp`、`banner-bamboo.webp`、`video-places.webp`、`video-tales.webp` |

<details>
<summary>推荐横幅与图集的内置 ImageGen 提示词</summary>

- **banner-harbor.webp** — Original animated fantasy website hero, horizontal cinematic 16:9, no text. Warm lanterns reflected in a mysterious old harbor in blue sea mist, a hooded young letter courier stands near center-right, holding one ivory letter, tall sailing ship silhouette and medieval port in the middle ground. Atmospherically rich, expressive original hand-painted anime key art, ink-blue and warm amber contrasts, softly detailed borders. Keep the main subject around center and center-right so a narrow crop works. A slightly darker lower third for a title overlay in the app. No text, logos, UI, or existing characters.
- **banner-stars.webp** — Original animated fantasy website hero, horizontal cinematic 16:9, no text. Apprentice astronomer with dark blue cloak inside an ancient observatory, a huge brass armillary sphere with many intricate rings, high arched vault open to the star-studded sky, candle-lit desk at right. Beautiful original hand-painted anime film key art, midnight indigo with scattered antique brass highlights. Main focal point centered so portrait crop works, darker lower third. No text, no watermark, no existing characters.
- **banner-bamboo.webp** — Original animated fantasy website hero, horizontal cinematic 16:9, no text. A lone young traveling lantern bearer in deep jade robes on mossy stone steps in a vast green bamboo forest, an ivory spirit fox near the lantern, old dark wooden mountain gate in the center distance. Original atmospheric painterly anime adventure key art, layers of blue-green fog and orange light, strong centered composition and darker lower third so portrait crop works. No text, logos, or existing characters.
- **video-places.webp** — Website illustration sprite sheet. Output a horizontal 3:2 or wider raster divided into exactly THREE equal-width columns and TWO equal-height rows, a 3x2 total of SIX independent cinematic original anime fantasy location stills. Perfectly straight equal divisions at 1/3, 2/3 of the width and exactly 1/2 height. Each illustration fills its rectangular cell edge-to-edge. No gutters, no printed dividers, no border, no text, no logo, no other subdivisions. In reading order: top-left misty coastal lighthouse in rain and a ship, top-center copper post office with pigeon loft and shelves of letters, top-right icy celestial tower under aurora, bottom-left mossy arch and medieval underground waterways, bottom-center small mountain inn at sunset, bottom-right a firefly-studded bamboo riverside bridge. Varied colors and distinct individual compositions, expressive painterly original anime background design, strong silhouettes centered independently in each cell.
- **video-tales.webp** — Website illustration sprite sheet. Output a horizontal 3:2 or wider raster divided into exactly THREE equal-width columns and TWO equal-height rows, a 3x2 total of SIX independent cinematic original anime fantasy video stills. Perfectly straight equal divisions at 1/3, 2/3 of the width and at exactly half the height. Each illustration fills its own rectangular cell edge-to-edge. No gutters, no drawn frames, no text, no logo, no other subdivisions. In reading order: top-left close-up candlelight and scattered vintage letters in a medieval stone room, top-center young anime navigator with raincoat crossing rope bridge above a gorge, top-right old bronze star compass and hands on parchment; bottom-left portrait of a white fox with pale green lantern, bottom-center woman in dark violet magic cloak walking through old abbey filled with candles, bottom-right group of three travelers near cooking fire under crimson red autumn trees. Varied colors, separate distinct compositions, painterly original anime, beautiful readable thumbnail art.

</details>

### 阶段 2 · 双页比例与题材书签

桌面双页按 1.67:1 排版，每张单页略偏竖长方形。书本左侧有「全部」「悬疑」「恋爱」「推理」「奇幻」「冒险」书签。切换题材筛选当前右页封面，来回翻页保留选中的题材，左页保持独立轮播。手机端显示书本上方的横向题材栏。再次打开书本默认显示「全部」。

在前端目录运行 npm run dev，访问 http://localhost:5173/ 后点击「追番」。该题材栏使用本地示例数据，可随前端独立运行。

| 操作 | 文件 |
| --- | --- |
| 新增 | frontend/src/components/reader/ReaderGenreRail.vue |
| 修改 | frontend/src/components/reader/BookReader.vue、VideoRecommendations.vue |
| 修改 | frontend/src/styles/book-reader.scss、frontend/src/data/animeReader.ts |
| 修改 | frontend/tests/turnPlan.test.ts、README.md |

类型检查和生产构建通过，10 项前端测试与 3 项后端测试通过。Chrome 验证了 1440×900、1920×1080、390×844 和 320×640 视口，并检查筛选、恢复全部六张封面、轮播位置保留、双向翻页与合书归位。可继续使用现有本地题材字段为后续馆藏补充分类。

### 阶段 2 · 题材扩展与封面调优

「追番」题材栏提供 29 个常见题材与「全部」。桌面用双列标签，手机端可横滑。筛选优先显示当前右页的匹配内容，当前页缺少同类内容时从本地示例目录补充。选择「全部」恢复当前页的六张封面，左页轮播和书本页码维持当前状态。

精选自动轮播间隔为 3.5 秒，横向切换用时 360 毫秒。右页缩小留白和卡间距来放大封面。1440×900 视口下单张封面实测约 173×156 像素。

| 操作 | 文件 |
| --- | --- |
| 修改 | frontend/src/data/animeReader.ts、frontend/src/components/reader/ReaderGenreRail.vue |
| 修改 | frontend/src/components/reader/BookReader.vue、FeaturedCarousel.vue、BookPage.vue、VideoRecommendations.vue |
| 修改 | frontend/src/styles/book-reader.scss、frontend/tests/turnPlan.test.ts、README.md |
| 新增 | frontend/src/assets/reader/genres-modern.webp、genres-classic.webp |

前端类型检查和生产构建通过，11 项前端测试及 3 项后端测试通过。Chrome 验证了 1920×1080、1440×900、1024×700、390×844 和 320×640 的布局，以及实测轮播间隔、悬停暂停、新题材显示、横向滚动、双向翻页和合书。当前内容为本地示例，后续馆藏可复用现有题材字段。

<details>
<summary>题材缩略图的内置 ImageGen 提示词</summary>

- **genres-modern.webp** — A single asset atlas for an original anime fantasy video bookshelf, exactly SIX independent square illustrations in THREE equal-width columns and TWO equal-height rows, horizontal 3:2 overall raster. Hard exact divisions at one third and two thirds width and one half height, no gutter, no outline, no typography or logos. Each fills one full square. In reading order: (1) late-afternoon high-school running track with young relay runners, dynamic perspective and warm sun; (2) vibrant indie student band on a modest stage, silhouettes with guitars and microphone, peach and lilac overhead lights; (3) tiny smiling neighborhood bakery and cat tugging at a baker's apron, warm whimsical daily-life atmosphere; (4) huge pale ivory humanoid mecha with brass trim in a dark industrial hangar with a pilot in foreground; (5) student inventor stepping through a glowing magical clock portal, ancient tower room; (6) quiet orbiting station with a small exploration ship and young pilot silhouetted by starlight. Invented characters and locations, colorful refined hand-painted anime background art, tactile cinematic lighting and centered compositions individually, consistent craftsmanship, varied hues. No watermarks, branding, frames or written symbols.
- **genres-classic.webp** — A single thumbnail asset atlas for a fantasy library with SIX independent square original anime scenes, exactly THREE equal-width columns by TWO equal-height rows, overall landscape 3:2. Exact divisions at one third and two thirds width and at one half height. Square illustrations fill their cells edge-to-edge, no border or gap. No text, watermark or logo. In reading order: (1) young wuxia sword apprentice on a bridge between Chinese mountain peaks and wind-blown pine; (2) xianxia practitioner in flowing jade and cream robes entering an ancient cloud monastery, glowing ethereal blade and pale clouds; (3) fictional Tang-inspired lantern-lit palace street fair, lively merchants and ceremonial musicians, red and brass; (4) empty moonlit school hallway at night, eerie illuminated door and tiny figure in the distance, suspenseful, no gore; (5) two young animation coworkers in a small studio among desks with warm lighting and stacked planning boards (no readable writing); (6) two travelers quietly reuniting on a vintage railroad platform in amber autumn dusk, nostalgic soft light. Expressive painted Japanese-animation-inspired cinematic stills with centered subjects. All characters original; no logos, captions or borders.

</details>

### 阶段 2 · 紧凑封面与视频搜索

右页统一使用两列三排，六张封面填满各自网格。1440×900 下的单张封面约为 273×158 像素，行距约 9 像素。桌面搜索框位于「视频推荐」标题右侧，窄屏移到标题下方。

搜索支持本地示例的视频名和题材，可和左侧题材筛选一起使用，结果最多显示六张。翻页会保留关键词，清空关键词会恢复当前题材的推荐。输入时的左右方向键用于移动光标，按 Esc 可清空非空搜索框。

| 操作 | 文件 |
| --- | --- |
| 新增 | frontend/src/components/reader/ReaderSearch.vue |
| 修改 | frontend/src/components/reader/BookReader.vue、BookObject.vue、BookPage.vue、TurningLeaf.vue、VideoRecommendations.vue |
| 修改 | frontend/src/data/animeReader.ts、frontend/tests/turnPlan.test.ts、README.md |

前端类型检查和生产构建通过，13 项前端测试和 3 项后端测试通过。Chrome 验证了 1920×1080、1440×900、1024×700、390×844 和 320×640 下的封面与搜索布局，并验证了搜索结果、空结果、键盘操作、双向翻页与归位。运行方式仍是在前端目录执行 npm run dev，再打开首页的「追番」。数据为本地示例，可继续通过 filterReaderSpreads 接入扩展后的本地目录。

## 阶段 3 · 多书籍分类

在 IDEA 中打开 `D:\project\fantasy-library`。在 Terminal 切换到 `frontend` 后执行 `npm run dev`。首次安装依赖时先执行 `npm ci`。打开 [馆藏书架](http://localhost:5173/) 可选择任意一本书。此阶段的阅读功能只需启动前端。

| 书籍 | 直达地址 | 双页内容 |
| --- | --- | --- |
| 追番 | [`/books/anime`](http://localhost:5173/books/anime) | 番剧封面、名称和简介轮播，视频推荐、题材和搜索 |
| 游戏 | [`/books/games`](http://localhost:5173/books/games) | 游戏精选与图册，展示封面、类型、平台和简介 |
| 社区 | [`/books/community`](http://localhost:5173/books/community) | 馆内公告、热门讨论、创作和新留言列表 |
| 推荐 | [`/books/recommendations`](http://localhost:5173/books/recommendations) | 馆员轮播和专题书单 |
| 个人书架 | [`/books/personal`](http://localhost:5173/books/personal) | 观看进度、稍后再看、游戏手记和收藏剪报 |

五类书由同一个 `Book` 组件完成取书、展开、双向翻页与归位。通过浏览器导航切换类别时，阅读器会等当前动画结束，合上旧书，再打开新书。合书回到书架后，焦点回到原书脊。阅读页码与轮播位置按书保存在当前页面会话中，追番的题材与搜索也会保留，刷新后会重置。窄屏可纵向滚动，系统减少动态偏好和阅读器的手动开关都可使用。

数据与封面为本地 Mock，社区帖子和个人收藏仅展示。部署前端静态产物时，应将直接访问的 `/books/*` 地址回退到 `index.html`，让 Vue Router 接管地址。后续阶段可从 `readerBooks.ts` 的分类配置与 `bookTypes.ts` 的页型扩展数据来源和布局。

### 阶段 3 文件清单

| 操作 | 文件 |
| --- | --- |
| 重构 | `frontend/src/components/reader/BookReader.vue` 改为 `Book.vue` |
| 新增 | `frontend/src/components/reader/GameCatalogue.vue`、`ReaderNotes.vue`、`ReaderCollections.vue` |
| 修改 | `frontend/src/components/reader/BookCover.vue`、`BookObject.vue`、`BookPage.vue`、`FeaturedCarousel.vue`、`ReaderArtwork.vue` |
| 新增 | `frontend/src/data/readerBooks.ts`、`mockBookPages.ts`、`frontend/src/reader/bookTypes.ts`、`frontend/src/stores/reader.ts` |
| 修改 | `frontend/src/data/animeReader.ts`、`frontend/src/reader/turnPlan.ts`、`frontend/src/pages/ShelfPage.vue`、`frontend/src/router/index.ts`、`frontend/src/styles/book-reader.scss` |
| 新增 | `frontend/src/assets/reader/mock-game-covers.webp`、`frontend/tests/readerBooks.test.ts` |
| 修改 | `README.md` |

<details>
<summary>游戏封面素材</summary>

`frontend/src/assets/reader/mock-game-covers.webp` 由单张三列两行的原创插画图集转换而成。生成描述要求六幅独立的中古幻想动漫游戏封面，分别绘制废城骑士、北境吊桥、晶石地窟、春日温室、浮岛港口和秋日食堂。每格正方形，主体居中，采用绘制感的电影布光，省略文字、商标、边框和水印。

</details>

### 阶段 3 验证

- 前端类型检查、Vite 生产构建和 Vitest 通过，共 16 项前端测试。后端在 JDK 21 下现有 3 项测试通过。
- Chrome 桌面视口 1440×900 验证五类书籍的开合、双向翻页、动画防重入、分类直达、无效分类回到书架、切换中保持页码及焦点恢复。320×568 窄屏验证布局与减少动态模式。

## 阶段 4 · Oracle 馆藏与基础 API

### 数据结构

| 表 | 用途 |
| --- | --- |
| `FL_CATEGORY`、`FL_TAG` | 作品分类与标签字典，使用稳定的 slug |
| `FL_WORK` | 全局作品 ID、类型、标题、简介、发布日期与分类 |
| `FL_ANIME` | 动漫集数、制作公司和播出状态 |
| `FL_GAME`、`FL_PLATFORM`、`FL_GAME_PLATFORM` | 游戏开发商、年龄分级与多平台关联 |
| `FL_WORK_TAG` | 作品和标签的多对多关联 |
| `FL_WORK_IMAGE` | 作品封面、画廊图片、替代文本与排序 |

动漫和游戏扩展表使用作品 ID 与类型的组合外键，扩展记录必须匹配作品类型。同一作品最多一张封面。分页索引覆盖各类型和分类的发布日期排序，关联表增加标签及平台反向查询索引。手工新增作品时，把主表和相应扩展表记录放在同一个事务中。

### 在 IDEA 启动

1. 在 IDEA 中打开 `D:\project\fantasy-library`，使用 JDK 21。
2. 在 IDEA 运行配置中设置 `ORACLE_JDBC_URL`、`ORACLE_USERNAME`、`ORACLE_PASSWORD`，或按 `backend/.env.example` 在本机填写 Git 忽略的 `backend/.env`。使用实际开发库的 PDB 服务名。
3. 运行 [FantasyLibraryApplication.java](backend/src/main/java/com/fantasylibrary/FantasyLibraryApplication.java)。也可在 `backend` 目录执行 `mvn spring-boot:run`。
4. 打开 [动漫列表](http://localhost:8080/api/anime?page=1&size=2) 或 [OpenAPI 3.0.3 文档](http://localhost:8080/api/openapi.yaml)。示例图片路径以 `/api/media/catalog/` 开头，客户端使用当前 API 地址解析这些路径。

### 为新开发库初始化

使用具有建表权限及表空间配额的应用账号，连接到目标 PDB。首次初始化在 SQL*Plus 中依次执行以下命令。登录命令会提示输入密码。

```text
sqlplus /nolog
CONNECT <应用账号>@//<地址>:<端口>/<PDB 服务名>
@D:/project/fantasy-library/backend/db/oracle/install.sql
```

在 PowerShell 中以 UTF-8 运行 SQL*Plus，可在启动前设置 `$env:NLS_LANG = '.AL32UTF8'`。也可在支持 UTF-8 的数据库工具里按顺序执行 `backend/db/oracle/01_schema.sql` 和 `02_seed.sql`。

`install.sql` 先检查同名馆藏对象，再执行建表和种子脚本。同名对象存在时先检查已执行的 DDL。已经建表且想补齐示例时，运行 `02_seed.sql`，脚本会补充缺失记录并保留同标识的已有值。后端常规启动从配置连接 Oracle，迁移通过上述脚本显式执行。

### REST API

| 方法和路径 | 数据 |
| --- | --- |
| `GET /api/categories` | 按 `sortOrder` 返回作品分类 |
| `GET /api/anime` | 动漫分页、集数、制作公司与播出状态 |
| `GET /api/games` | 游戏分页、开发商、年龄分级与平台列表 |
| `GET /api/works/{id}` | 通过全局作品 ID 获取详情、标签、封面和画廊 |

列表参数为 `page`（从 1 开始，默认 1）、`size`（1–100，默认 20），可叠加 `category`、`tag`、`tags` 和 `q`。多个标签用重复的 `tags` 参数传入，例如 `tags=mystery&tags=detective`，作品须同时匹配所选标签。分类及标签使用 slug，关键词匹配标题和简介。结果按发布日期和 ID 降序排列，超出页码范围返回空列表及原有总数。空结果的 `totalPages` 为 0。

成功响应包含 `code: OK`、`message` 与 `data`。分页数据包含 `items`、`page`、`size`、`total` 和 `totalPages`。出错时 HTTP 状态码对应错误类型，响应包含稳定的业务 `code` 和 `message`，字段校验失败可附 `errors`。无效参数为 400，作品不存在为 404，Oracle 不可用或馆藏表未初始化为 503。系统连接状态由 `GET /api/health` 提供。

### 阶段 4 文件清单

| 范围 | 文件 |
| --- | --- |
| Oracle 建表、索引、初始化 | `backend/db/oracle/01_schema.sql`、`02_seed.sql`、`install.sql` |
| REST 与响应 | `backend/src/main/java/com/fantasylibrary/api/CatalogController.java`、`ApiResponse.java`、`ApiExceptionHandler.java` |
| 业务与模型 | `backend/src/main/java/com/fantasylibrary/service/CatalogService.java`、`catalog/model/Catalog.java`、`CatalogProblem.java`、`CatalogQuery.java` |
| MyBatis 与配置 | `backend/src/main/java/com/fantasylibrary/persistence/mapper/CatalogMapper.java`、`backend/src/main/resources/mybatis/CatalogMapper.xml`、`application.yml`、`backend/pom.xml` |
| 接口文档与本地资源 | `backend/src/main/resources/static/api/openapi.yaml`、`media/catalog/` 下的 7 张 WebP |
| 测试 | `CatalogServiceTest.java`、`CatalogEndpointTest.java`、`CatalogMapperTest.java`、`CatalogStub.java`、`HealthEndpointTest.java` |
| 使用说明 | `README.md` |

### 验证和当前范围

- 后端 `mvn verify` 构建可执行 JAR，14 项测试通过。前端现有生产构建及 16 项测试通过。
- 在本机 Oracle 开发库创建了 9 张馆藏表和索引，导入 3 部示例动漫和 3 款示例游戏。用真实 API 验证分类、分页、筛选、详情、7 张图片及 OpenAPI，验证重复执行种子脚本保持作品 ID 与行数。
- 本阶段为公开只读查询。Oracle 中的示例记录和静态资源用于联调，书架仍由阶段 3 的前端示例数据驱动。后续可用同一份 OpenAPI 合同接入 Vue 与 Android。


### 游戏书版式调整

在 IDEA 的 Terminal 中进入 `D:\project\fantasy-library\frontend` 并执行 `npm run dev`，打开 [游戏图鉴](http://localhost:5173/books/games)。

游戏图鉴采用本地 Mock，共 20 款游戏。每张书页最多两幅封面，默认每个跨页展示四款。左侧的 40 个游戏标签在手机上可横向滚动。右页搜索框支持名称、游戏类型、标签别名与平台，能与当前标签组合。更换筛选条件后回到第一页，尾页按命中数量排版。

- 图册数据和分页在 `frontend/src/data/gameReader.ts`，书籍配置涉及 `readerBooks.ts`、`mockBookPages.ts`、`src/reader/bookTypes.ts` 与 `src/stores/reader.ts`。
- 阅读器调整涉及 `Book.vue`、`BookPage.vue`、`GameCatalogue.vue`、`ReaderGenreRail.vue`、`ReaderSearch.vue` 与 `book-reader.scss`。
- 新增本地游戏封面图集 `src/assets/reader/game-genres.webp` 和测试 `tests/gameReader.test.ts`。
- 前端类型检查、生产构建和 19 项测试通过。Chrome 验证了 1440×900、1024×700、390×844 和 320×568 下的双页布局、筛选、搜索、空结果、翻页和合书后的状态恢复。

## 阶段 5 · 书页接入 Oracle

在 IDEA 中用 JDK 21 启动 `FantasyLibraryApplication`（已运行旧版本时需重启）。在 `frontend` 目录执行 `npm run dev`，打开 [追番书](http://localhost:5173/books/anime) 或 [游戏书](http://localhost:5173/books/games)。开发端口与 Oracle 设置见本文开头。

追番和游戏通过 Spring Boot 从 Oracle 读取馆藏。书页展示封面、名称、简介、分类、标签和作品专属信息。点击左页番剧轮播或任一作品封面可进入 `/works/{id}` 查看详细资料和已收录的插图。同一浏览会话内从详情返回时会恢复该书的查询条件和页码。推荐、社区与个人书架沿用本地 Mock。

- 右页可搜索名称或简介。左侧标签可多选，作品必须包含所有选中的标签。再次点选可取消，点「全部」会清除所选标签并保留关键词。标签来自 `GET /api/tags?type=ANIME` 或 `GAME`，列表通过重复的 `tags` 参数按标签筛选。
- 一次分页查询固定取四部作品。追番书保留左页大轮播，右页四部推荐按两行两列排列。游戏书左右各两款。筛选或末页不足四条时显示实际匹配项。
- 等待下一页时仍可查看当前页、更改搜索或合书。书页取得数据后开始翻动，页码随动画结束更新。超时和服务错误会显示重试入口，当前条件没有匹配时显示空页。数据库沿用阶段 4 的表和种子，馆藏扩充脚本见下方。
- 验收命令为前端 `npm run build`、`npm test`，后端 `mvn verify`。本次通过 24 项前端测试与 14 项后端测试。在本机 Chrome 以真实 Oracle 数据验证桌面与手机书页、详情与查询，并对翻页请求注入延迟和临时失败验证过期结果、页码提交和重试。
- 下一阶段可复用作品 ID、资料页路由、分页与标签接口。更多可浏览的内容取决于已入库作品和图片资源。

### 阶段 5 文件清单

- 前端接口、阅读数据与状态：`frontend/src/api/catalog.ts`、`frontend/src/reader/catalogSpread.ts`、`frontend/src/reader/useCatalogBook.ts`、`frontend/src/reader/bookTypes.ts`、`frontend/src/data/readerBooks.ts`、`frontend/src/stores/reader.ts`、`frontend/src/reader/turnPlan.ts`、`frontend/src/reader/prepareCovers.ts`。
- 书页、详情与路由：`frontend/src/components/reader/CatalogFilters.vue`、`frontend/src/components/reader/CatalogEntries.vue`、`frontend/src/components/reader/BookPage.vue`、`frontend/src/components/reader/FeaturedCarousel.vue`、`frontend/src/components/reader/Book.vue`、`frontend/src/styles/book-reader.scss`、`frontend/src/router/index.ts`、`frontend/src/pages/WorkDetailPage.vue`。
- 前端测试：`frontend/tests/readerBooks.test.ts`、`frontend/tests/catalogBook.test.ts`。
- 后端接口、业务与数据映射：`backend/src/main/java/com/fantasylibrary/catalog/model/Catalog.java`、`backend/src/main/java/com/fantasylibrary/persistence/mapper/CatalogMapper.java`、`backend/src/main/resources/mybatis/CatalogMapper.xml`、`backend/src/main/java/com/fantasylibrary/service/CatalogService.java`、`backend/src/main/java/com/fantasylibrary/api/CatalogController.java`。
- 接口文档与后端测试：`backend/src/test/java/com/fantasylibrary/support/CatalogStub.java`、`backend/src/main/resources/static/api/openapi.yaml`、`backend/src/test/java/com/fantasylibrary/api/CatalogEndpointTest.java`。
- 使用说明：`README.md`。

### 阶段 5 验收修正 · 四部作品与主流标签

已完成阶段 4 初始化的开发库只需在原应用账号连接中执行 `backend/db/oracle/03_reader_editions.sql`。脚本按标识增补示例作品、标签关联、平台和本地封面，可重复执行。全新库使用原 `install.sql` 入口会一并执行这份脚本。

示例馆藏共 16 部番剧、20 款游戏。左栏标签来自 Oracle 中对应类型的作品，搜索和多选标签可叠加。开发启动方式同上，IDEA 中通过 `FantasyLibraryApplication` 使用最新后端资源，前端目录执行 `npm run dev`。验收可从两本书的第一页翻到最后一页，或点选「恋爱」「推理」「角色扮演」「策略」检查筛选。后续可按同一套作品与标签结构继续录入馆藏。

本机 Oracle 中追番有 31 个可筛选标签，游戏有 43 个。逐跨页核验追番 4 个跨页与游戏 5 个跨页，前端 26 项、后端 14 项自动测试通过。

本次变更包括 Oracle 的 `03_reader_editions.sql`、`install.sql` 和 `backend/src/main/resources/static/api/media/catalog/editions/` 下的封面。前端涉及 `Book.vue`、`CatalogFilters.vue`、`CatalogEntries.vue`、`book-reader.scss`、`catalogSpread.ts`、`useCatalogBook.ts`、`stores/reader.ts` 及 `catalogBook.test.ts`、`readerBooks.test.ts`。

### 阶段 5 标签多选

追番和游戏书的左栏可同时选中多个标签，点「全部」会清除所选标签。更新后请用 IDEA 重新运行 `FantasyLibraryApplication`，前端继续在 `frontend` 目录执行 `npm run dev`。数据库无需迁移。

前端涉及 `Book.vue`、`CatalogFilters.vue`、`ReaderGenreRail.vue`、`reader.ts`、`useCatalogBook.ts`、`catalog.ts`、`catalogBook.test.ts`、`readerBooks.test.ts`。后端涉及 `CatalogQuery.java`、`CatalogMapper.xml`、`openapi.yaml`、`CatalogEndpointTest.java`、`CatalogMapperTest.java`、`CatalogServiceTest.java`、`CatalogStub.java`。

## 阶段 6 · 账号与私人藏书

在 IDEA 中启动 `FantasyLibraryApplication`。前端在 `frontend` 目录执行 `npm run dev`，通过顶部「登录」创建账号。注册后用同一账号名和密码登录，「个人资料」可修改昵称与简介，也可以退出。

打开公开的作品详情即可加入私人书架。番剧提供「想看」「在看」「已看」「搁置」与已看集数。游戏可分别设为收藏、愿望单，也可以同时开启。详情页上的「移出书架」清除该作品的个人记录。书架上的「个人书架」会展开私人藏书室，左页显示追番，右页显示游戏收藏和愿望单。跨设备登录同一账号后，打开书架或点「刷新」即可读取 Oracle 中最新保存的记录。

### 数据库与会话配置

- 本机开发库已执行 `backend/db/oracle/04_accounts.sql`，新增账号、令牌和收藏表。已有馆藏的其他环境在应用账号下执行这份脚本一次。空库继续使用 `backend/db/oracle/install.sql`。
- 密码使用 BCrypt。会话令牌由安全随机数生成，Oracle 只保存令牌的 SHA-256 摘要，并检查七天有效期。浏览器通过 HttpOnly、SameSite Cookie 携带令牌。退出撤销当前会话。写操作通过 Spring Security 校验 CSRF。
- HTTPS 部署设置 `AUTH_SECURE_COOKIE=true`，前端与 API 通过同源入口访问。请求中的身份来自登录令牌，资料和收藏操作使用服务端识别出的用户编号。

| 路径 | 用途 |
| --- | --- |
| `GET /api/auth/csrf` | 获取写请求校验值 |
| `POST /api/auth/register`、`/login`、`/logout` | 注册、登录和退出 |
| `GET /api/me/profile`、`PUT /api/me/profile` | 读取与修改自己的资料 |
| `GET /api/me/collection` | 读取私人书架 |
| `GET`、`PUT`、`DELETE /api/me/collection/{workId}` | 读取、保存和移除自己的作品记录 |

浏览器验收覆盖注册、登录、资料修改、追番记录、游戏愿望单和退出。使用两个账号验证写入与删除均按当前用户隔离，并在桌面和手机会话之间验证同步。之后的登录需求可复用账号会话和 `/api/me/**` 权限校验。当前跨设备同步通过打开书架或刷新触发。

### 阶段 6 文件清单

- 前端页面与路由。`frontend/src/components/LibraryHeader.vue`、`frontend/src/components/reader/Book.vue`、`frontend/src/components/reader/BookPage.vue`、`frontend/src/components/reader/PersonalEntries.vue`、`frontend/src/components/reader/WorkCollectionPanel.vue`、`frontend/src/pages/AccountPage.vue`、`frontend/src/pages/WorkDetailPage.vue`、`frontend/src/router/index.ts`。
- 前端状态与数据。`frontend/src/api/account.ts`、`frontend/src/data/readerBooks.ts`、`frontend/src/reader/bookTypes.ts`、`frontend/src/reader/usePersonalBook.ts`、`frontend/src/stores/account.ts`。
- Oracle、环境配置与依赖。`backend/.env.example`、`backend/db/oracle/04_accounts.sql`、`backend/db/oracle/install.sql`、`backend/pom.xml`、`backend/src/main/resources/application.yml`。
- 后端接口、安全与数据访问。`backend/src/main/java/com/fantasylibrary/account/AccountModels.java`、`backend/src/main/java/com/fantasylibrary/account/AccountProblem.java`、`backend/src/main/java/com/fantasylibrary/api/AccountController.java`、`backend/src/main/java/com/fantasylibrary/api/ApiExceptionHandler.java`、`backend/src/main/java/com/fantasylibrary/api/ApiResponse.java`、`backend/src/main/java/com/fantasylibrary/api/CollectionController.java`、`backend/src/main/java/com/fantasylibrary/persistence/mapper/AccountMapper.java`、`backend/src/main/java/com/fantasylibrary/persistence/mapper/CollectionMapper.java`、`backend/src/main/java/com/fantasylibrary/security/LibrarySecurity.java`、`backend/src/main/java/com/fantasylibrary/security/SessionCookie.java`、`backend/src/main/java/com/fantasylibrary/security/SessionTokenFilter.java`、`backend/src/main/java/com/fantasylibrary/service/AccountService.java`、`backend/src/main/java/com/fantasylibrary/service/CollectionService.java`、`backend/src/main/resources/mybatis/CollectionMapper.xml`、`backend/src/main/resources/static/api/openapi.yaml`。
- 测试。`backend/src/test/java/com/fantasylibrary/api/AccountSecurityTest.java`、`backend/src/test/java/com/fantasylibrary/api/CatalogEndpointTest.java`、`frontend/tests/readerBooks.test.ts`。
- 运行和验收说明记录在 `README.md`。

## 阶段 7 · 社区手稿与举报审核

### 启动和体验

1. 用 IDEA 打开项目，使用完整 JDK 21 启动 `FantasyLibraryApplication`。本机验收使用位于 `D:\project\tools\community-jdk21\jdk-21.0.13+9` 的 Temurin。若旧的项目 SDK 缺少图片处理组件，可把 IDEA 的 Project SDK 和后端运行配置指向此目录。
2. 本机 Oracle 开发库已执行 `backend/db/oracle/05_community.sql`。已有阶段 6 表的其他数据库用原应用账号执行该脚本一次，空库用 `install.sql`。
3. 在 `frontend` 中运行 `npm run dev`。从书架选择「社区」。左页浏览公告和话题，右页翻阅讨论档案。作品详情的「查看作品讨论」可打开该作品的档案，从这里撰写手稿会预填关联作品。

访客可阅读公开帖子和评论。登录后可发布手稿、回复、点赞以及举报。作者可编辑和删除自己的帖子。普通账号无法调用审核接口。管理员可在社区左页打开「举报审核」，隐藏或删除被举报的内容，也可驳回举报。隐藏和删除会让公众页面与媒体链接停止提供该内容。

### 开通审核权限

在 Oracle 应用账号下更新指定用户，账号已经注册后再执行。替换示例账号名后运行。

    UPDATE fl_account SET account_role = 'ADMIN'
    WHERE username = LOWER('your_registered_username');
    COMMIT;

刷新应用后，指定用户可以从社区书进入审核。服务器每次从登录会话取得实际身份和角色。

### 文件存储与内容处理

- 每帖最多 4 张 JPEG 或 PNG，每张最多 5 MB、单边最多 4096 像素、总计最多 1200 万像素。上传先解码再重新编码，生成独立文件名。
- `COMMUNITY_UPLOAD_DIR` 配置文件目录，默认是相对于后端工作目录的 `./uploads/community`。Oracle 保存帖子、评论、审核记录和文件标识。部署、备份或迁移时需同时保留 Oracle 数据和图片目录。
- 标题、正文、回复和举报理由以纯文本渲染。服务端校验篇幅、作品关联、回复所属帖子、作者和管理员权限。写入沿用阶段 6 的 CSRF 校验。
- 隐藏的图片由公开接口拒绝读取。删除帖子或编辑时移除附图会清理对应文件，审核状态保留在 Oracle。审核当前由管理员手动处理。

### 社区接口

| 范围 | 路径 |
| --- | --- |
| 公开档案和详情 | `GET /api/community/posts`、`GET /api/community/posts/{id}` |
| 公开评论、图片 | `GET /api/community/posts/{id}/comments`、`GET /api/community/media/{key}` |
| 发帖、编辑和删除 | `POST /api/me/community/posts`、`PUT /api/me/community/posts/{id}`、`DELETE /api/me/community/posts/{id}` |
| 批注、点赞和举报 | `/api/me/community/posts/{id}/comments`、`/api/me/community/posts/{id}/like`、`/api/me/community/reports` |
| 举报审核 | `GET /api/admin/community/reports`、`PUT /api/admin/community/reports/{id}` |

帖子、评论和举报都支持分页。发布和编辑用 multipart 请求，`post` 部分为 JSON，`images` 为图片文件。完整参数和数据类型记录在 `/api/openapi.yaml`。后续可沿用此契约扩展访问端。

### 阶段 7 文件清单

- 数据库、配置与存储目录。`.gitignore`、`backend/.env.example`、`backend/db/oracle/05_community.sql`、`backend/db/oracle/install.sql`、`backend/src/main/resources/application.yml`。
- 后端接口、权限与数据访问。`backend/src/main/java/com/fantasylibrary/account/AccountModels.java`、`backend/src/main/java/com/fantasylibrary/api/ApiExceptionHandler.java`、`backend/src/main/java/com/fantasylibrary/api/ApiResponse.java`、`backend/src/main/java/com/fantasylibrary/api/CommunityController.java`、`backend/src/main/java/com/fantasylibrary/community/CommunityImages.java`、`backend/src/main/java/com/fantasylibrary/community/CommunityModels.java`、`backend/src/main/java/com/fantasylibrary/community/CommunityProblem.java`、`backend/src/main/java/com/fantasylibrary/persistence/mapper/AccountMapper.java`、`backend/src/main/java/com/fantasylibrary/persistence/mapper/CommunityMapper.java`、`backend/src/main/java/com/fantasylibrary/security/LibrarySecurity.java`、`backend/src/main/java/com/fantasylibrary/security/SessionTokenFilter.java`、`backend/src/main/java/com/fantasylibrary/service/AccountService.java`、`backend/src/main/java/com/fantasylibrary/service/CommunityService.java`、`backend/src/main/resources/mybatis/CommunityMapper.xml`、`backend/src/main/resources/static/api/openapi.yaml`。
- 社区界面与阅读器。`frontend/src/components/community/ForumRoom.vue`、`frontend/src/components/community/PostComments.vue`、`frontend/src/components/community/ReportContent.vue`、`frontend/src/components/reader/Book.vue`、`frontend/src/components/reader/BookPage.vue`、`frontend/src/components/reader/CommunityBookPage.vue`、`frontend/src/data/readerBooks.ts`、`frontend/src/pages/CommunityEditPage.vue`、`frontend/src/pages/CommunityPostPage.vue`、`frontend/src/pages/CommunityReviewPage.vue`、`frontend/src/pages/WorkDetailPage.vue`、`frontend/src/reader/bookTypes.ts`、`frontend/src/reader/useCommunityBook.ts`、`frontend/src/router/index.ts`、`frontend/src/styles/community.scss`。
- 浏览器 API 接入。`frontend/src/api/account.ts`、`frontend/src/api/community.ts`。
- 自动测试。`backend/src/test/java/com/fantasylibrary/api/CatalogEndpointTest.java`、`backend/src/test/java/com/fantasylibrary/api/CommunitySecurityTest.java`、`backend/src/test/java/com/fantasylibrary/community/CommunityImagesTest.java`、`frontend/tests/readerBooks.test.ts`。
- 升级、配置和验收说明见 `README.md`。

### 阶段 7 调整 · 热门手稿、浏览数和个人头像

社区书的左页在话题下显示最多 3 篇热门手稿，沿用当前话题和作品筛选。排序依次看评论数、点赞数、浏览数，再按时间。右页讨论档案继续按时间分页展示，预览包含摘要、作者姓名与头像、浏览数、评论数和点赞数。有附图的手稿可显示首张缩图。

打开公开帖子详情记一次浏览。列表、编辑读取和图片读取不累计浏览。旧帖的浏览数从更新后开始计算。此处统计的是详情打开次数，重复打开会计入。

在顶部「个人资料」中选择头像，预览后点「保存头像」。接受 JPEG 或 PNG，最多 2 MB。服务器重新编码并将最长边压缩到 384 像素以内。未设置头像时显示罗盘占位图，移除头像也会恢复占位。头像文件与社区附图共用 `COMMUNITY_UPLOAD_DIR`，数据库保存文件标识。

本机已执行 `backend/db/oracle/06_community_views.sql` 与 `07_account_avatar.sql`。已有社区表的其他环境按顺序执行两份脚本，再使用完整 JDK 21 重启后端。前端启动方式仍为 `npm run dev`。

涉及文件

- `backend/db/oracle/06_community_views.sql`
- `backend/db/oracle/07_account_avatar.sql`
- `backend/db/oracle/install.sql`
- `backend/src/main/java/com/fantasylibrary/account/AccountModels.java`
- `backend/src/main/java/com/fantasylibrary/api/AvatarController.java`
- `backend/src/main/java/com/fantasylibrary/api/CommunityController.java`
- `backend/src/main/java/com/fantasylibrary/community/CommunityImages.java`
- `backend/src/main/java/com/fantasylibrary/community/CommunityModels.java`
- `backend/src/main/java/com/fantasylibrary/persistence/mapper/AccountMapper.java`
- `backend/src/main/java/com/fantasylibrary/persistence/mapper/CommunityMapper.java`
- `backend/src/main/java/com/fantasylibrary/service/AvatarService.java`
- `backend/src/main/java/com/fantasylibrary/service/CommunityService.java`
- `backend/src/main/resources/mybatis/CommunityMapper.xml`
- `backend/src/main/resources/static/api/openapi.yaml`
- `backend/src/test/java/com/fantasylibrary/api/CatalogEndpointTest.java`
- `frontend/src/api/account.ts`
- `frontend/src/api/community.ts`
- `frontend/src/components/account/ProfileAvatarEditor.vue`
- `frontend/src/components/community/AuthorAvatar.vue`
- `frontend/src/components/community/PostComments.vue`
- `frontend/src/components/community/PostPreview.vue`
- `frontend/src/components/reader/CommunityBookPage.vue`
- `frontend/src/pages/AccountPage.vue`
- `frontend/src/pages/CommunityPostPage.vue`
- `frontend/src/reader/useCommunityBook.ts`
- `frontend/src/stores/account.ts`


## 阶段 8 · 馆员荐读与推荐榜单

书架中的「推荐」打开四个书页。第一跨页显示馆员荐读与热门榜，第二跨页显示新入馆作品和近期活跃讨论。顶部可以切换「全部」「追番」「游戏」。作品和帖子可以打开详情并返回推荐书。已有书架及翻页组件同时用于桌面和手机。

### 数据与排序

- 热门榜只收录有互动的作品，热度 = 收藏数 × 4 + 愿望单数 × 2 + 关联帖可见评论数 × 3 + 点赞数 × 2 + 最多 100 分的关联帖浏览数。同分按作品 ID 降序。隐藏或删除的帖子及隐藏评论不贡献热度。
- 新入馆按 Oracle 的 `fl_work.added_at` 倒序。迁移前的作品若没有收录时间，在新增记录之后按首发日期排列。作品资料页的首发日期与收录时间各自独立。
- 近期活跃讨论取近 14 天内发布或收到可见回复的公开帖子，按最新发帖或回复时间排序。选择类型时包含关联该类型作品的帖子，以及未关联作品但同类型的话题。
- Java 给收藏历史加权。想看 2 分，在看 4 分（有观看进度为 5 分），已看 5 分，搁置 0 分；游戏收藏 5 分、愿望单 2 分，可以叠加。候选匹配的每个标签贡献偏好权重的四倍，作品类型也贡献相应的偏好权重。理由引用实际贡献分值的标签和收藏记录。个性推荐排除自己书架里已有的作品。
- 游客、新账号和没有可用匹配的账号优先看到热门作品。分类榜无可用作品时，从 Oracle 中的 `editorial_rank` 选出编辑精选。现有库中四部本地示例作品预置了精选顺序。缺少真实互动时榜单显示空状态。

公开接口为 `GET /api/recommendations`，个性推荐为 `GET /api/me/recommendations`。两者均支持 `type=ANIME` 或 `type=GAME`。后者使用当前登录会话，响应不缓存。接口结构已记入 `/api/openapi.yaml`。

### 启动与验收

本机应用库已执行 `backend/db/oracle/08_recommendations.sql`。其他已有阶段 7 数据库请先在应用账号下执行这份脚本。新库按 `backend/db/oracle/install.sql` 初始化。用 IDEA 的 JDK 21 重新运行 `FantasyLibraryApplication`，在 IDEA 终端切换到 `frontend` 后运行 `npm run dev`，从书架进入「推荐」。

当前热门榜每次查询实时聚合互动；只展示有互动的前 5 部作品。荐读、新入馆、活跃讨论分别最多显示 4 项。收藏及公开互动少时，榜单会较短。

前端构建和 30 项测试通过，后端 Maven 构建和 26 项测试通过。用本机 Oracle 验证了榜单计分、类型筛选、旧帖收到新回复、账号隔离、收藏状态变化、桌面和手机翻页以及网络错误重试。验收临时记录已经清理。新接口文档可供后续客户端对接。

### 阶段 8 文件清单

- `backend/db/oracle/08_recommendations.sql`
- `backend/db/oracle/install.sql`
- `backend/src/main/java/com/fantasylibrary/api/RecommendationController.java`
- `backend/src/main/java/com/fantasylibrary/persistence/mapper/CatalogMapper.java`
- `backend/src/main/java/com/fantasylibrary/persistence/mapper/CommunityMapper.java`
- `backend/src/main/java/com/fantasylibrary/persistence/mapper/RecommendationMapper.java`
- `backend/src/main/java/com/fantasylibrary/recommendation/PreferenceScoring.java`
- `backend/src/main/java/com/fantasylibrary/recommendation/RecommendationModels.java`
- `backend/src/main/java/com/fantasylibrary/service/CatalogService.java`
- `backend/src/main/java/com/fantasylibrary/service/RecommendationService.java`
- `backend/src/main/resources/mybatis/CatalogMapper.xml`
- `backend/src/main/resources/mybatis/CommunityMapper.xml`
- `backend/src/main/resources/mybatis/RecommendationMapper.xml`
- `backend/src/main/resources/static/api/openapi.yaml`
- `backend/src/test/java/com/fantasylibrary/api/CatalogEndpointTest.java`
- `backend/src/test/java/com/fantasylibrary/api/RecommendationEndpointTest.java`
- `backend/src/test/java/com/fantasylibrary/recommendation/PreferenceScoringTest.java`
- `backend/src/test/java/com/fantasylibrary/support/CatalogStub.java`
- `frontend/src/api/recommendations.ts`
- `frontend/src/components/community/ForumRoom.vue`
- `frontend/src/components/community/PostPreview.vue`
- `frontend/src/components/reader/Book.vue`
- `frontend/src/components/reader/BookPage.vue`
- `frontend/src/components/reader/RecommendationBookPage.vue`
- `frontend/src/components/reader/RecommendationFilter.vue`
- `frontend/src/components/reader/RecommendationWorkCard.vue`
- `frontend/src/data/readerBooks.ts`
- `frontend/src/pages/CommunityPostPage.vue`
- `frontend/src/pages/WorkDetailPage.vue`
- `frontend/src/reader/bookTypes.ts`
- `frontend/src/reader/useRecommendationBook.ts`
- `frontend/src/stores/reader.ts`
- `frontend/tests/readerBooks.test.ts`
- `README.md`



## 个人书架 · 追番、愿望单、观看历史与消息

在书架打开「个人书架」，顶部书签可直接切换栏目。追番把在看、想看与已看、搁置分到左右页。游戏把愿望单和已收藏游戏分到左右页。同一部游戏可同时出现在两页。点击收藏或历史记录可回到作品资料页修改状态和进度。

观看历史按保存时间排序，可翻页。进入「在看」或「已看」，以及已有观看集数时变更集数或状态，都会留下记录。重复保存相同状态和集数会保留原记录，移出收藏后仍可查看已保存的观看历史。迁移时从已有追番收藏导入符合条件的最近一次进度，时间沿用原收藏更新时间。

消息收录收到的帖子评论、评论回复与新点赞。可以打开原帖、逐条标为已读或全部标为已读。自己的操作不产生给自己的消息。同一点赞取消再恢复会沿用原来的已读状态。帖子或关联评论隐藏或删除后，相应消息不会继续展示。迁移时纳入已有的可见评论和回复。早期点赞缺少时间，因此从更新后的点赞开始产生消息。

本机 Oracle 已执行 `backend/db/oracle/09_personal_library.sql`。其他已有项目数据库先应用该脚本，新库使用 `backend/db/oracle/install.sql`。在 IDEA 使用 JDK 21 重启 `FantasyLibraryApplication`，前端在 `frontend` 运行 `npm run dev`。账号权限、分页接口和已读操作已记入 OpenAPI。

前端构建和 30 项测试通过，后端 Maven 构建和 27 项测试通过。使用本机 Oracle 和两个临时账号验证了游戏愿望单、并发更新进度、历史分页、消息归属、已读、详情返回及 320 像素手机布局。临时记录已清理。

### 个人书架改动文件

- `README.md`
- `backend/db/oracle/09_personal_library.sql`
- `backend/db/oracle/install.sql`
- `backend/src/main/java/com/fantasylibrary/api/PersonalController.java`
- `backend/src/main/java/com/fantasylibrary/persistence/mapper/PersonalMapper.java`
- `backend/src/main/java/com/fantasylibrary/personal/PersonalModels.java`
- `backend/src/main/java/com/fantasylibrary/service/CollectionService.java`
- `backend/src/main/java/com/fantasylibrary/service/CommunityService.java`
- `backend/src/main/java/com/fantasylibrary/service/PersonalService.java`
- `backend/src/main/resources/mybatis/PersonalMapper.xml`
- `backend/src/main/resources/static/api/openapi.yaml`
- `backend/src/test/java/com/fantasylibrary/api/AccountSecurityTest.java`
- `backend/src/test/java/com/fantasylibrary/api/CatalogEndpointTest.java`
- `frontend/src/api/personal.ts`
- `frontend/src/components/reader/Book.vue`
- `frontend/src/components/reader/BookObject.vue`
- `frontend/src/components/reader/BookPage.vue`
- `frontend/src/components/reader/PersonalBookPage.vue`
- `frontend/src/components/reader/PersonalEntries.vue`
- `frontend/src/components/reader/PersonalInbox.vue`
- `frontend/src/components/reader/PersonalTabs.vue`
- `frontend/src/components/reader/PersonalWatchHistory.vue`
- `frontend/src/pages/CommunityPostPage.vue`
- `frontend/src/reader/bookTypes.ts`
- `frontend/src/reader/usePersonalBook.ts`
- `frontend/src/stores/reader.ts`


## 阶段 9 · 视觉与阅读体验

木纹、皮革、黄铜、羊皮纸及文字配色由 `frontend/src/styles/main.scss` 的共享变量提供。书架和系统状态沿用深色材质，书页、作品资料、账号表单、社区手稿及审核页共享纸张、棕色墨迹、黄铜边框和墨绿色操作按钮。书封保留各自的皮革配色。目录、推荐与历史记录使用统一的封面边框和焦点反馈。

桌面在 46rem 以上使用双页。取书和归还主要用位移与缩放插值，开合衔接保留封面和中缝，翻页保留双面纸张与随翻动变化的阴影。书页翻完才提交异步取得的内容。窄屏显示一页，翻过同一跨页的另一侧时复用现有数据。手机的搜索框固定在阅读器顶部，标签可展开。

阅读器右上角「设置」可选跟随设备、标准翻书或简化过渡，也可开启轻微翻页音效。设置保存在当前浏览器，音效默认关闭。系统要求减少动态时和窄屏都会使用淡入淡出，自动模式在低内存、较少处理器核心或省流量设备上也使用简化过渡。用 Tab 移动焦点，Enter 打开书籍，← / → 翻页，Esc 关闭设置或合书。搜索框内的方向键保留文字编辑行为，合书后焦点回到原书脊。

书架首屏按需加载阅读器和动画库。列表的 29 张轻量封面相较对应原图的合计体积减少约 49%，作品详情沿用原图，新增封面可使用原图作为回退。手机登录页插画从 293.6 KB 降到 39.6 KB。轮播只渲染当前与过渡中的画面，桌面跨页时仅短暂预解码最多两张轻量封面。

### 启动和验证

在 IDEA 保持现有后端与 Oracle 运行，在 `frontend` 运行 `npm run dev`。使用 `npm run build` 后可通过 `npm run preview` 预览生产构建。阶段 9 沿用现有 REST API 和数据库。

前端构建与 32 项测试通过，27 项原有后端测试通过。使用真实本地 Oracle 和 Chromium 的 1440 × 900 桌面、390 × 844 手机视口验收了书架、五本书、作品详情、系统状态、注册、登录回跳、个人资料、社区发帖与编辑以及管理员入口。另模拟低性能桌面、系统减少动态和手机 4 倍 CPU 降速，检查了缩略图请求、音效开关、键盘焦点和分页。另外在 320 像素视口复测了个人书架、观看记录与消息已读。使用临时账号的测试资料已清理。

Vite 生产构建中，首屏 JavaScript 的 gzip 体积约为 69.5 KB，之前约为 114.6 KB。一次本地浏览器验收中，桌面跨页约 930 ms（包含请求和动画），手机在 4 倍 CPU 降速时，同一跨页内切换单页约 350 ms。该结果来自本机模拟，实际耗时随设备、浏览器和网络变化。

### 阶段 9 文件清单

- `frontend/src/animations/readerMotion.ts`
- `frontend/src/components/HealthStatus.vue`
- `frontend/src/components/LibraryHeader.vue`
- `frontend/src/components/community/PostPreview.vue`
- `frontend/src/components/reader/Book.vue`
- `frontend/src/components/reader/BookCover.vue`
- `frontend/src/components/reader/BookObject.vue`
- `frontend/src/components/reader/BookPage.vue`
- `frontend/src/components/reader/CatalogEntries.vue`
- `frontend/src/components/reader/FeaturedCarousel.vue`
- `frontend/src/components/reader/PersonalEntries.vue`
- `frontend/src/components/reader/PersonalWatchHistory.vue`
- `frontend/src/components/reader/ReaderArtwork.vue`
- `frontend/src/components/reader/ReaderGenreRail.vue`
- `frontend/src/components/reader/ReaderSettings.vue`
- `frontend/src/components/reader/RecommendationBookPage.vue`
- `frontend/src/components/reader/RecommendationWorkCard.vue`
- `frontend/src/components/shelf/BookSpine.vue`
- `frontend/src/pages/CommunityPostPage.vue`
- `frontend/src/pages/ShelfPage.vue`
- `frontend/src/pages/StatusPage.vue`
- `frontend/src/pages/WorkDetailPage.vue`
- `frontend/src/reader/bookGeometry.ts`
- `frontend/src/reader/coverPreview.ts`
- `frontend/src/reader/pagePosition.ts`
- `frontend/src/reader/pageSound.ts`
- `frontend/src/reader/preferences.ts`
- `frontend/src/reader/prepareCovers.ts`
- `frontend/src/stores/reader.ts`
- `frontend/src/styles/account-page.scss`
- `frontend/src/styles/book-reader.scss`
- `frontend/src/styles/community.scss`
- `frontend/src/styles/main.scss`
- `frontend/tests/readerPreferences.test.ts`
- `frontend/tests/turnPlan.test.ts`
- `frontend/src/assets/catalog/` 内 29 张轻量 WebP 封面
- `frontend/src/assets/account/ledger-small.webp`
- `README.md`


## 作品详情与放映室

追番书进入作品详情后，首屏的「开始观看」会打开独立的放映室。详情页以插画和黄金按钮呈现首屏，下面保留作品档案、原图、收藏和讨论。游戏详情继续提供收藏与愿望单。

放映室提供分集、播放节点、上下一段、自动接播、进度拖动、倍速、静音、音量、全屏及浏览器支持的小窗。播放器聚焦时可用空格播放或暂停，左右方向键前后跳 5 秒，F 切换全屏。切换同一段视频的来源时沿用播放秒数及播放或暂停状态。进度按作品和分集保存在当前浏览器。「私人书架」沿用账户收藏，馆友留言会进入该作品的现有社区讨论档案。

本机 Oracle 已应用 `backend/db/oracle/10_playback.sql`，登记《雾港来信》的两段原创插画演示预告，各有 720p 馆内节点和 360p 轻量节点。演示使用现有馆藏插画和合成氛围音。尚未登记媒体的作品会显示空状态。已有其他环境应用同一脚本，新库使用 `backend/db/oracle/install.sql`。

日后新增资源时，在 `fl_episode` 按作品登记分集或预告，再在 `fl_episode_source` 为同一分集登记一条或多条有效节点，按 `sort_order` 排序。支持 MP4 与 WebM，可引用 `/api/media/playback/` 下的项目静态文件，或有使用权限的 HTTPS 媒体直链。源文件由浏览器直接读取，拖动进度依赖浏览器支持的编码及媒体服务的字节范围读取能力。`GET /api/works/{id}/episodes` 与数据格式已写入 OpenAPI。

在 IDEA 用 JDK 21 重启 `FantasyLibraryApplication`，前端在 `frontend` 目录运行 `npm run dev`。从追番书打开《雾港来信》，点「开始观看」进入。

前端构建与 34 项测试通过，后端构建与 30 项测试通过。使用真实 Oracle 和 Chromium 的桌面及手机视口验证媒体 Range 响应、两种清晰度、拖动进度、切节点续播、暂停状态、刷新续播、自动接播、全屏、失败恢复、分享、收藏及讨论。临时验收账号和记录已清理。

### 放映室改动文件

- `README.md`
- `backend/db/oracle/10_playback.sql`
- `backend/db/oracle/install.sql`
- `backend/src/main/java/com/fantasylibrary/api/PlaybackController.java`
- `backend/src/main/java/com/fantasylibrary/persistence/mapper/PlaybackMapper.java`
- `backend/src/main/java/com/fantasylibrary/playback/PlaybackModels.java`
- `backend/src/main/java/com/fantasylibrary/service/PlaybackService.java`
- `backend/src/main/resources/static/api/openapi.yaml`
- `backend/src/test/java/com/fantasylibrary/api/CatalogEndpointTest.java`
- `backend/src/test/java/com/fantasylibrary/api/RecommendationEndpointTest.java`
- `backend/src/test/java/com/fantasylibrary/service/PlaybackServiceTest.java`
- `frontend/src/api/playback.ts`
- `frontend/src/components/player/LibraryVideo.vue`
- `frontend/src/components/player/RelatedScreenings.vue`
- `frontend/src/components/player/ScreeningDiscussion.vue`
- `frontend/src/components/player/ScreeningProgram.vue`
- `frontend/src/pages/WatchPage.vue`
- `frontend/src/pages/WorkDetailPage.vue`
- `frontend/src/player/resume.ts`
- `frontend/src/player/useLibraryVideo.ts`
- `frontend/src/player/useWorkScreen.ts`
- `frontend/src/router/index.ts`
- `frontend/src/styles/screening-room.scss`
- `frontend/src/styles/work-dossier.scss`
- `frontend/tests/playback.test.ts`
- `backend/src/main/resources/static/api/media/playback/` 内四个 WebM 文件


## 游戏详情 · 下载与节点选择

游戏详情的首屏提供「下载游戏」与「下载节点」。下拉按平台分组，切换后按钮指向该节点。本地文件以附件下载，外部 HTTPS 节点在新标签页打开。未登记资源时，按钮和下拉显示为不可用。接口请求出错时可重试。

### 登记下载资源

本机 Oracle 已应用 `backend/db/oracle/11_game_downloads.sql`。其他已有环境执行该脚本，新库沿用 `backend/db/oracle/install.sql`。节点需使用作品已有的平台，可在同一平台登记多条。

- 分发本地附件时，把有分发权限的游戏包放入 `GAME_DOWNLOAD_DIR`，在 `fl_game_download.file_name` 填纯文件名。目录默认 `./uploads/game-downloads`，相对后端工作目录。文件名支持英文字母、数字、点、下划线和短横线，并以字母或数字开头。
- 使用外部来源时，填写 `external_url` 为 HTTPS 下载链接或官方发行页面。每条节点填文件名或外部链接之一。节点显示名填 `label`，同平台顺序由 `sort_order` 决定。

```sql
INSERT INTO fl_game_download (work_id, platform_id, label, file_name, external_url, sort_order)
SELECT w.id, p.id, :label, :file_name, :external_url, :sort_order
FROM fl_work w
JOIN fl_game_platform gp ON gp.work_id = w.id
JOIN fl_platform p ON p.id = gp.platform_id
WHERE w.slug = :game_slug AND p.slug = :platform_slug;
```

在 IDEA 中用 JDK 21 重启 `FantasyLibraryApplication`，前端在 `frontend` 运行 `npm run dev`。`GET /api/works/{id}/downloads` 和本地附件路径已写入 OpenAPI。

前后端构建通过，34 项前端测试和 32 项后端测试通过。用本机 Oracle、桌面和 320 像素手机视口验证了下拉切换、实际 ZIP 下载、字节范围读取、外部节点、空状态、失败重试及番剧播放入口。临时测试节点和文件已清理。

### 改动文件

- `README.md`
- `backend/.env.example`
- `backend/db/oracle/11_game_downloads.sql`
- `backend/db/oracle/install.sql`
- `backend/src/main/java/com/fantasylibrary/api/ApiExceptionHandler.java`
- `backend/src/main/java/com/fantasylibrary/api/ApiResponse.java`
- `backend/src/main/java/com/fantasylibrary/api/GameDownloadController.java`
- `backend/src/main/java/com/fantasylibrary/catalog/model/CatalogProblem.java`
- `backend/src/main/java/com/fantasylibrary/download/GameDownload.java`
- `backend/src/main/java/com/fantasylibrary/persistence/mapper/GameDownloadMapper.java`
- `backend/src/main/java/com/fantasylibrary/service/GameDownloadService.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/static/api/openapi.yaml`
- `backend/src/test/java/com/fantasylibrary/api/CatalogEndpointTest.java`
- `backend/src/test/java/com/fantasylibrary/service/GameDownloadServiceTest.java`
- `frontend/src/api/downloads.ts`
- `frontend/src/components/catalog/GameDownloadActions.vue`
- `frontend/src/pages/WorkDetailPage.vue`
- `frontend/src/styles/work-dossier.scss`


## 独立管理端

用 IntelliJ IDEA 打开 `D:\project\fantasy-library`，在同一项目里可以看到三个独立目录。

| 目录 | 用途 | 默认地址 |
| --- | --- | --- |
| `frontend` | 公开图书馆 | `http://localhost:5173` |
| `admin-frontend` | 独立馆务管理 | `http://localhost:5174` |
| `backend` | Spring Boot 与 Oracle | `http://127.0.0.1:8080` |

在 IDEA 用 JDK 21 启动 `FantasyLibraryApplication`。两个前端各自在自己的目录运行 `npm ci` 和 `npm run dev`，各自也可运行 `npm run build` 打包。浏览器直接打开 5174，使用原管理员账号登录。公开站旧的 `/admin` 和 `/community/moderation` 会跳转到独立管理端。

两个前端通过各自的 `/api` 代理访问后端。公开站拒绝管理接口；管理员直接使用独立管理站登录，管理会话和普通会话各用独立 Cookie 与防伪校验。管理端用 `VITE_PUBLIC_SITE_URL` 指定公开站地址。`API_PROXY_TARGET` 可指定后端地址。部署时分别托管两个构建目录，配置各自的 SPA 回退和同源 `/api` 转发。

### 馆藏编辑

- 番剧和游戏各有列表，可按标题、作品标识和上下架状态查找。新增作品先存为待上架，可在编辑页预览封面和确认资料。
- 共用字段包括标题、作品标识、简介、发行或首播日期、分类及标签。番剧另有制作公司、播出状态和集数。游戏另有开发商、年龄分级和平台。
- 封面与详情插图记录来源地址及图片说明，支持预览、换图、移除和调整插图顺序。地址支持本站馆藏图片路径或 HTTPS。图片地址写入 Oracle 原有的 `fl_work_image`，媒体文件不写入 Oracle；将来接 MinIO 时可把图片和视频放在 MinIO，数据库保存对象来源。本次图片维护通过地址完成。
- 已登记的播放与下载节点可单独上下架。作品下架后，本站会停止公开该作品及相应资源；第三方链接的有效性由来源方控制。有下载节点的平台会保留，避免编辑平台时误删节点。
- 论坛总开关、帖子上下架、评论删除、举报审核、普通账号新增和注销都在管理端。写操作沿用现有会话、管理员校验和 CSRF 校验。

### 改动范围

- `admin-frontend/` 新增独立 Vue、TypeScript、Vite 项目，含依赖锁定、环境变量示例、登录、作品编辑、封面/插图、节点与审核组件。
- `frontend/src/router/index.ts`、`router/adminUrl.ts`、`pages/AccountPage.vue`、`components/reader/CommunityBookPage.vue` 和 `api/community.ts` 调整管理跳转，管理页面、操作组件及接口调用迁入独立项目。
- `backend/src/main/java/com/fantasylibrary/` 新增 `WorkEditorialController`、`WorkEditorialService`、`WorkEditorialMapper` 和表单模型，并扩充现有馆藏列表。
- 更新 `LibraryAdminMapper.xml`、`openapi.yaml` 与接口权限测试。作品编辑沿用现有 Oracle 表，无需执行新迁移。

### 验证

公开站与管理端构建完成。公开站和后端各通过 34 项自动测试；在本机 Oracle 中验证了管理员跨端跳转、独立登录、普通用户权限、番剧与游戏上下架、封面与插图更新、重复标识、下载节点保留，以及 320 像素宽屏幕的编辑布局。验收用的作品和账号已清理。

## MinIO 私有媒体存储

本机 MinIO 控制台使用 `http://127.0.0.1:9001`，后端通过 `http://127.0.0.1:9000` 访问 `fantasy-library-media`。保持存储桶的匿名访问关闭。

|MinIO 对象前缀|用途|
|---|---|
|`catalog/`|番剧和游戏封面、插图，新增图片在 `catalog/uploads/`|
|`playback/`|站内视频文件，Oracle 关联具体分集和节点|
|`community/`|论坛帖图|
|`avatars/`|用户头像|
|`site/`|登录页、阅读页、书架等网站自带配图|

Oracle 保留原有媒体地址和业务元数据。站内地址由 Spring Boot 解析为私有桶对象，播放时沿用作品和节点的发布状态校验。HTTPS 外链由浏览器直接使用，Oracle 保存原地址。

### 初次授权和迁移

1. 保持 MinIO 运行并创建 `fantasy-library-media` 桶。将官方客户端放在 `D:\minio\mc.exe`。
2. 在 PowerShell 7 中到 `backend` 运行 `.\tools\authorize-local-minio.ps1`。在提示时输入 MinIO 控制台的密码。脚本会创建仅能访问该桶的专用密钥，保存在 Git 忽略的 `backend/.env`。
3. 先运行 `.\tools\migrate-local-media.ps1` 查看 Oracle 引用和本地文件清单。暂时停止旧后端写入后，加 `-Apply` 执行复制。脚本核对读取回来的 SHA-256，保留已有同名文件和本地原件。遇到同名且内容不同的对象会停止。脚本可重复执行。IDEA 的 JDK 21 也可以用 `-Java` 参数指定。
4. 迁移校验成功后，在 `backend/.env` 设置 `MEDIA_STORAGE_ENABLED=true`，在 `frontend/.env.local` 设置 `VITE_SITE_MEDIA_FROM_MINIO=true`，然后重启后端和公开站。管理端还是在 `admin-frontend` 中用 `npm run dev`，默认地址为 `http://localhost:5174`。

MinIO 关闭会影响启用后的媒体访问。把两处配置改回 `false` 可以让已保留的旧素材继续由原位置提供。切换后新增的文件在 MinIO，回退本地模式前应先处理这部分文件。

### 管理端录入

- **封面和插图**在作品编辑页的「封面与插图」中选择上传。上传完成会自动填入站内地址，填写说明后点「保存资料」。每张最多 5 MiB。已有 HTTPS 图片可直接粘贴到地址栏。
- **视频**在番剧编辑页的「播放资源」里新增或选择分集，再点击「新增节点」。选择本地 MP4 / WebM 上传，或填入可直接播放的 HTTPS 视频地址。保存节点后按需上架，作品也需要处于上架状态。每段本地视频最多 5 GiB。
- **论坛图片和头像**沿用原有上传入口。启用 MinIO 后，新文件会保存到对应的私有桶前缀。现有游戏模块管理封面、插图和下载，论坛支持图片发帖。

### 本次改动

- `backend/src/main/java/com/fantasylibrary/media/`、`api/AdminMediaController.java`、`api/PlaybackEditorialController.java` 等新增 MinIO 通道、上传与播放节点录入。现有馆藏、播放、论坛和头像读取路径沿用。
- `backend/tools/` 新增本机授权、Oracle 路径盘点及校验迁移脚本。现有 Oracle 表结构可直接使用。
- `admin-frontend/src/` 新增图片和番剧视频上传入口及播放节点表单。
- `frontend/src/` 新增从站内媒体 API 获取自带配图的开关。环境变量示例和 OpenAPI 已更新。

### 本机迁移验收

共转存 81 个对象，包括 29 张馆藏图片、4 段视频、1 张论坛图片和 47 个网站静态素材，均已回读并核对 SHA-256。本地 Oracle 联调通过了管理端图片和视频上传、外部地址原样保存、视频范围请求与下架权限。项目已启用 MinIO，原文件作为迁移备份保留。
