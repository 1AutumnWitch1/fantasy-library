# 奇幻图书馆 · Android 联动测试客户端

一个**独立的 Android 原生测试客户端**，用来验证「Android 手机 ↔ 电脑上的 Spring Boot / Oracle / MinIO」整条链路。
它复用现有 Web 项目的后端接口与数据，**没有**新建后端、没有改数据库结构、没有改动任何 Web 代码。

> 位置：`D:\project\fantasy-library\android-client`
> 现有 `backend/`、`frontend/`、`admin-frontend/`、`compose.yaml`、`.env.docker` 全程**只读**，未做任何修改。

---

## 1. 它验证了什么

| 验收项 | 结果 | 证据 |
|---|---|---|
| App 能启动 | ✅ | `screenshots/03-home-status.png` |
| 可修改并保存服务器 IP | ✅ | `screenshots/01-server-config.png`、`02-connection-test.png` |
| 能连接 Spring Boot | ✅ HTTP 200 / 135ms / `{"application":"UP","database":"UP"}` | `02-connection-test.png` |
| 能读 Oracle 真实动漫数据 | ✅ 共 5 部，OVERLORD / 电锯人 … | `04-anime-library.png` |
| 能读 Oracle 真实游戏数据 | ✅ 共 4 部，含平台 / 开发商 / 分级 | `06-games-library.png` |
| 能加载 MinIO 真实图片 | ✅ 封面全部来自 `/api/media/catalog/...` | `04-anime-library.png`、`06-games-library.png` |
| 能播放 MinIO 真实视频 | ✅ ExoPlayer 播放 2:15:01 真实影片 | `08-video-playing-range.png` |
| 进度条可拖动 | ✅ 跳到 1:13:53 并渲染新画面 | `09-video-seek.png` |
| 断网有明确错误提示 | ✅ 「请求超时 / 30037ms / 可能原因…」 | `12-network-timeout.png` |
| 不暴露数据库 / MinIO 凭据 | ✅ 客户端只保存会话 Cookie 与 CSRF token | 见 `docs/VERIFICATION.md` |

完整记录与逐项证据：**[docs/VERIFICATION.md](docs/VERIFICATION.md)**

---

## 2. 页面

底部 5 个入口，均为触屏优先（Compose）：

1. **连接**（`馆门钥匙`）— 协议 / IP / 端口可改，实时预览完整地址，保存 + 测试连接，显示 HTTP 状态码、响应时间、`application` / `database` 与失败分类。
2. **状态**（`馆内状态`）— `/api/health` 结果、Oracle 馆藏总量、登录状态、测试入口。
3. **动漫**（`动漫资料库`）— `/api/anime` 真实数据，封面 / 名称 / 分类 / 标签 / 制作公司 / 集数 / 年份，支持刷新与分页「加载更多」，点击进入详情。
4. **游戏**（`游戏资料库`）— `/api/games`，统一卡片风格，额外展示开发商 / 分级 / 平台。
5. **放映**（`放映室`）— 选动漫 → 选分集 → 选播放源，ExoPlayer 播放，支持播放/暂停、拖动进度、时间显示、全屏（横屏沉浸式）、HTTP Range 检测、地址安全检查。

附加页面：**作品详情**（封面横幅 / 简介 / 标签 / 图片 / 分集 / 进入放映）、**读者登录**（真实 Cookie+CSRF 登录）。

视觉沿用中古幻想图书馆风格：深胡桃木底纹、墨绿晕染、羊皮纸卡片、黄铜角饰与衬线标题；
装饰全部由 Compose Canvas 绘制，不引入图片资源或图标库。

---

## 3. 技术栈与版本（稳定且互相兼容）

| 组件 | 版本 | 说明 |
|---|---|---|
| Gradle | 8.13 | 与本机已缓存发行版一致，未升级 |
| Android Gradle Plugin | 8.13.0 | 支持 `compileSdk 36` |
| Kotlin | 2.2.0 | Compose Compiler 由 Kotlin 自己发布（`org.jetbrains.kotlin.plugin.compose`） |
| compileSdk / targetSdk | 36 | minSdk **26**（Android 8.0） |
| Jetpack Compose | BOM 2025.09.00 | material3 + foundation + navigation-compose 2.9.8 |
| Retrofit | 2.11.0 | + `retrofit2-kotlinx-serialization-converter` 1.0.0 |
| OkHttp | 4.12.0 | 自定义 CookieJar + CSRF 拦截器 + 超时 |
| Kotlin Serialization | 1.8.1 | JSON 解析 |
| Coil | 2.7.0 | 图片加载（复用同一个 OkHttpClient） |
| AndroidX Media3 | 1.4.1 | ExoPlayer + PlayerView |
| DataStore Preferences | 1.1.7 | 保存服务器地址 |
| androidx.security-crypto | 1.1.0 | 加密保存会话 Cookie（失败自动降级并提示） |
| Navigation Compose | 2.9.8 | 页面路由 |

`gradle/libs.versions.toml` 是唯一的版本来源，方便整体调整。
**没有为了追新而升级本机现有的 Gradle / JDK（JDK 17）/ SDK**。

---

## 4. 架构

```
app/src/main/java/com/fantasylibrary/android/
├── MainActivity.kt             # 入口：edge-to-edge、读取已保存地址、决定起始页
├── FantasyLibraryApp.kt        # Application：持有 AppContainer、设置 Coil ImageLoader
├── di/AppContainer.kt          # 手写依赖容器（无需 Hilt）
├── data/
│   ├── network/                # 契约层（与后端 1:1 对应）
│   │   ├── ApiDtos.kt          #   DTO：ApiEnvelope / Work / Episode / Profile …
│   │   ├── FantasyApi.kt       #   Retrofit 接口：/api/anime, /api/games, /api/works/{id}/episodes …
│   │   ├── ApiClient.kt        #   OkHttp + Retrofit 工厂、CsrfInterceptor
│   │   ├── Diagnostics.kt      #   健康检查 / Range 探测（直接读 Response 拿状态码与耗时）
│   │   ├── Failures.kt         #   失败分类（DNS/超时/404/401/5xx/数据库/媒体…）
│   │   ├── MediaUrls.kt        #   媒体地址解析 + localhost/Docker 内部名拦截（纯 Kotlin）
│   │   └── ServerAddress.kt    #   服务器地址解析与校验（纯 Kotlin）
│   ├── auth/                   # SecureStore（加密存储）、PersistentCookieJar
│   ├── repository/             # CatalogRepository / DiagnosticsRepository / AuthRepository
│   └── settings/               # DataStore：保存服务器地址与上次检测摘要
└── ui/
    ├── theme/                  # 深胡桃木 / 墨绿 / 羊皮纸 / 黄铜 配色与衬线排版
    ├── components/             # PaperCard / LeatherPanel / StatusPill / RemoteImage / 手绘符号 …
    ├── screens/{config,home,catalog,detail,video,login}/
    └── AppNavHost.kt           # 底部导航 + NavHost
```

分层清晰：**UI（Compose）→ ViewModel（StateFlow）→ Repository → Network/Storage**。
ViewModel 只依赖 Repository，UI 不直接碰 Retrofit / OkHttp。

---

## 5. 构建与运行

### 5.1 环境（本机已具备）

- JDK 17：`D:\jdk17\jdk-17.0.12`
- Android SDK：`D:\soft\learn`（已在 `local.properties` 中配置）
- Gradle 8.13（wrapper 指向官方地址，本机已缓存）

### 5.2 命令行构建

```powershell
$env:JAVA_HOME = "D:\jdk17\jdk-17.0.12"
$env:ANDROID_HOME = "D:\soft\learn"
cd D:\project\fantasy-library\android-client

.\gradlew.bat :app:assembleDebug          # 产物：app/build/outputs/apk/debug/app-debug.apk
.\gradlew.bat :app:assembleRelease        # 未签名 release（R8 已通过）
```

### 5.3 安装到模拟器

```powershell
$adb = "D:\soft\learn\platform-tools\adb.exe"
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
& $adb shell am start -n com.fantasylibrary.android.debug/com.fantasylibrary.android.MainActivity
```

首次进入若没有保存地址，会停在「连接」页；填 `10.0.2.2` + `8080` 即可（模拟器专用地址）。

**Debug 构建支持免手动输入地址**（仅调试用，Release 不生效）：

```powershell
& $adb shell am start -n com.fantasylibrary.android.debug/com.fantasylibrary.android.MainActivity `
    -e base_url "http://10.0.2.2:8080"
```

### 5.4 Android Studio

直接 `Open` 本目录即可（`settings.gradle.kts` + version catalog 标准结构）。
Gradle JDK 选 17（`Settings → Build Tools → Gradle → Gradle JDK`）。

---

## 6. 测试

### 6.1 单元测试（纯 JVM）

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

包含 41 个纯逻辑测试：媒体地址解析（回环地址 / Docker 内部名 / 预签名 / Release 明文拒绝）、
服务器地址校验、真实响应 JSON 的 DTO 解析、错误分类。

### 6.2 真实后端契约测试（需要后端在跑）

```powershell
.\gradlew.bat :app:testDebugUnitTest -PliveBackend=true
```

会真实请求 `127.0.0.1:8080`，验证：健康检查、分页、详情、分集、封面图片字节、
`Range: bytes=0-1` 是否返回 206 + `Content-Range`、以及错误密码是否老实返回 401 `INVALID_CREDENTIALS`。

> 本机注意：这台机器的 Gradle `Test` worker 在当前执行环境下无法建立管道 stdio
> （`ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain`），
> 因此我用 `JUnitCore` 直接运行同一批测试类拿到了结果：**47 tests OK**，
> 记录见 [`docs/test-report.txt`](docs/test-report.txt)。
> 在 Android Studio 里正常运行 `testDebugUnitTest` 不受此限制。

---

## 7. 网络与安全

- **客户端不持有任何数据库 / MinIO 凭据**，全部业务数据经由 Spring Boot API。
- 不访问 Oracle 端口，不访问 MinIO 管理控制台（`:9001`）。
- 图片 / 视频地址全部来自后端响应；`MediaUrls` 会拦截 `localhost` / `127.0.0.1` / `minio` / `oracle` 等
  在手机上必然打不开的地址，并在界面上说明原因。
- 预签名 URL 原样使用（保留 `X-Amz-Signature` 等 query），不做任何重写。
- 登录态：只保存服务端下发的会话 Cookie + CSRF token，**不保存密码**；
  优先 `EncryptedSharedPreferences`（Keystore），设备不支持时降级并在界面明示。
- 明文 HTTP：**debug 允许**（`src/debug/res/xml/network_security_config.xml`），
  **release 禁止**（`src/main/...` 严格策略 + 代码级拒绝）。
- 不硬编码任何真实服务器 IP；地址只存在 DataStore 里，可随时「清空」。

真机联调（本机 Docker 只监听 `127.0.0.1`）见 **[docs/NETWORK-SETUP.md](docs/NETWORK-SETUP.md)**。

---

## 8. 与现有后端的关系

- 接口路径、字段、分页、错误码全部以 `backend/` 源码 + 运行中的服务为准，
  逐项记录在 **[docs/API-CONTRACT.md](docs/API-CONTRACT.md)**。
- 现有后端的登录**不是** Bearer Token，而是「数据库会话令牌 + HttpOnly Cookie + CSRF」，
  客户端据此实现（`CsrfInterceptor` + `PersistentCookieJar`）。
- 发现但不擅自修改的缺口（例如 `/api/health` 未写进 `openapi.yaml`、没有移动端 Token 接口）
  已在契约文档「现有项目的缺口」一节列出。

---

## 9. 已知限制

1. **登录成功路径未实测**：需要你的真实账号（我没有创建账号，也没有伪造成功）。
   错误密码已实测得到 `HTTP 401 / INVALID_CREDENTIALS`，说明链路是通的。
2. **模拟器上大文件拖动可能解码失败**：当前视频约 3.79 GB / 1080p，
   模拟器使用软件解码（SwiftShader），在跳到 1:13:53 后再解码偶发
   `ERROR_CODE_DECODING_FAILED`，客户端会显示「播放失败 / 解码失败」并可重试；
   真机硬件解码一般不会出现。第一次播放与拖动本身已验证成功（见截图）。
3. 数据库里该分集的 `durationSeconds` 是 60，但视频实际时长 2:15:01——
   这是后端数据，客户端按真实媒体时长显示。
4. 本机 Gradle Test worker 的管道限制见 6.2 说明。

---

## 10. 目录速览

```
android-client/
├── app/                      # 应用模块（编译产物在 app/build，已 gitignore）
├── dist/                     # 可直接安装的 APK（debug / release-unsigned）
├── docs/
│   ├── API-CONTRACT.md       # 真实接口契约（源码 + 实测）
│   ├── NETWORK-SETUP.md      # 真机 / 模拟器网络配置
│   ├── VERIFICATION.md       # 逐项验收证据
│   └── test-report.txt       # 47 项测试运行记录
├── screenshots/              # 真机（模拟器）截图证据
├── tools/ui.ps1              # 联调辅助脚本（按文字点击 / 截图）
├── gradle/libs.versions.toml # 版本目录
└── README.md
```

现成安装包：

- `dist/fantasy-library-android-debug.apk` — 可直连 `http://` 局域网地址（联调用）
- `dist/fantasy-library-android-release-unsigned.apk` — 未签名，只允许 HTTPS
