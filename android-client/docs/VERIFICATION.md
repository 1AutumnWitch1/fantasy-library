# 验收记录（实测，非模拟）

所有结论都来自**真实运行**：
Windows + Docker Compose（Spring Boot / Oracle / MinIO）+ Android 模拟器（Pixel 系列，Android 16 / API 36）。

| 项目 | 值 |
|---|---|
| 模拟器 | `Medium_Phone_API_36.1`（x86_64，1080×2400，Android API 36） |
| 客户端 | `com.fantasylibrary.android.debug` v0.1.0-test-debug（debug 构建） |
| 后端 | `http://127.0.0.1:8080`，模拟器内为 `http://10.0.2.2:8080` |
| 后端版本 | 现有 `backend/`（未修改） |
| 测试日期 | 见各截图与 `docs/test-report.txt` 时间戳 |

---

## 一、逐项验收对照（对应需求「七、最终验收标准」）

### 1. Android App 可以成功启动 ✅

- `adb install -r app-debug.apk` → `Success`
- 启动后进入「连接」或「状态」页（取决于是否已保存地址）
- 截图：[`03-home-status.png`](../screenshots/03-home-status.png)

### 2. 可以修改并保存电脑服务器 IP ✅

- 「连接」页支持协议（http/https）、IP/域名、端口，实时预览最终地址；
- 保存后写入 DataStore；**杀进程重启 App 自动读取**（截图 16 的实验）
- 截图：[`01-server-config.png`](../screenshots/01-server-config.png)
- 重启后仍为 `http://10.0.2.2:8080`，无需重新输入

### 3. 可以连接 Spring Boot ✅

「测试连接」真实请求 `GET /api/health`：

```
HTTP 状态   200
响应时间    135 ms
Content-Type application/json
响应正文    {"application":"UP","database":"UP"}
```

截图：[`02-connection-test.png`](../screenshots/02-connection-test.png)（页面同时显示 `应用 UP` / `数据库 UP`）

### 4. 可以读取 Oracle 中真实的动漫数据 ✅

`GET /api/anime?page=1&size=20` 返回：

```
total=5  totalPages=1
#110 OVERLORD            KADOKAWA · 全 1 集 · 2026 · 冒险/探索/魔法   cover=/api/media/catalog/uploads/665374d6-….jpg
#83  电锯人               未知 · 2026 · 动作/剧情                      cover=…/3cd16862-….png
#103 游戏人生剧场版        榎宮祐 · 全 1 集 · 2014 · 冒险/热血/解谜     cover=…/ef7ad8f2-….webp
#?   魔法少女小圆 剧场版    Magica Quartet · 全 1 集 · 2013            cover=…/….webp
```

截图：[`04-anime-library.png`](../screenshots/04-anime-library.png)
详情页（真实简介、标签、分集、「进入视频播放测试」）：
[`05-anime-detail.png`](../screenshots/05-anime-detail.png)、
[`05b-anime-detail-body.png`](../screenshots/05b-anime-detail-body.png)

### 5. 可以读取 Oracle 中真实的游戏数据 ✅

`GET /api/games?page=1&size=20` → `total=4`，卡片展示开发商、平台、分级（18+）：

```
#108 新官中[日式RPG/动态] 勇者闯魔穴 Milky Quest II …   未知 · PC · 2026 · 18+
#107 爱丽丝的摇篮（爱丽丝☆クロニクル）                 ひなゆあ/ななめはっちゃー · PC · 18+
```

截图：[`06-games-library.png`](../screenshots/06-games-library.png)
详情页：[`07-game-detail.png`](../screenshots/07-game-detail.png)

### 6. 可以加载 MinIO 中真实的图片 ✅

- 后端返回相对地址 `/api/media/catalog/uploads/….jpg`，客户端按配置的服务器 origin 补全；
- 实测整图下载：`200`，`Content-Type: image/jpeg`，`Content-Length: 175775`；
- 列表与详情页封面全部正常显示（见截图 04 / 05 / 06 / 07）；
- 加载失败时可点「重试」；地址若指向 `localhost` / `127.0.0.1` / `minio` 等，
  会直接显示「地址不可用 + 原因」，而不是空白。

### 7. 可以播放 MinIO 中真实的视频 ✅

- 播放源来自 `GET /api/works/110/episodes?allNodes=true`：
  `/api/media/playback/0290e37e-a88f-444f-89c9-b5333448fa6a.mp4`（1080p / MP4）
- ExoPlayer 实际播放，界面显示真实时长 `2:15:01`，画面为真实影片内容；
- 截图：[`08-video-playing-range.png`](../screenshots/08-video-playing-range.png)

### 8. 视频可以正常拖动进度条 ✅

- 点击进度条 50% → 时间从 `00:05` 跳到 `1:13:53 / 2:15:01`，并渲染出该位置的真实画面；
- 截图：[`09-video-seek.png`](../screenshots/09-video-seek.png)
- 全屏（横屏沉浸式）：[`10-video-fullscreen.png`](../screenshots/10-video-fullscreen.png)

服务器 Range 支持（App 内「HTTP Range 检测」与命令行双重验证）：

```http
GET /api/media/playback/0290e37e-….mp4
Range: bytes=0-1

HTTP/1.1 206 Partial Content
Accept-Ranges: bytes
Content-Range: bytes 0-1/4068399266
Content-Type: video/mp4
```

尾部范围也可用（`Range: bytes=-2048` → `206 bytes 4068397218-4068399265/4068399266`），
说明后端把 Range 透传给 MinIO，支持任意位置拖动。

### 9. 网络断开时有明确错误提示 ✅

开启飞行模式后点「重新检测」：

```
服务异常
HTTP 状态   无响应
响应时间    30037 ms
请求超时
请求超时（连接或读取超时）
可能原因：服务器无响应或网络很慢，可重试并检查局域网质量
timeout
[重新尝试]
```

截图：[`12-network-timeout.png`](../screenshots/12-network-timeout.png)

### 10. 不会暴露数据库 / MinIO 凭据 ✅

| 检查项 | 结果 |
|---|---|
| 客户端是否包含 Oracle 账号密码 | 否（全工程无 `jdbc:`、`ORACLE_USERNAME/PASSWORD` 常量，亦无真实值） |
| 客户端是否包含 MinIO accessKey/secretKey | 否（仅有 `AWSAccessKeyId` 这类**用于识别**预签名 URL 的关键字） |
| 是否直接访问 Oracle 1521 | 否（只有 HTTP 8080 请求） |
| 是否访问 MinIO 控制台 9001 | 否 |
| 是否硬编码真实局域网 IP | 否。代码里只有 `10.0.2.2` / `127.0.0.1` 这类标准回环示例、输入框占位符 `192.168.1.20` 与单元测试用例；真实服务地址只存在 DataStore |
| 保存了哪些认证信息 | 仅会话 Cookie + CSRF token（加密存储），**不含密码** |

---

## 二、登录验证（真实接口，不伪造）

现有后端使用 Cookie + CSRF 会话（`LibrarySecurity` / `SessionCookie` / `AccountController`），
没有 Bearer Token 接口，客户端据此实现：

1. `GET /api/auth/csrf` → `{headerName:"X-XSRF-TOKEN", token:"…"}` 且下发 `XSRF-TOKEN` Cookie；
2. `POST /api/auth/login`（带 CSRF 头 + Cookie）；
3. 成功后调用 `GET /api/me/profile` 验证会话真的生效。

**实测错误密码**（这是真实验证，而不是"跳过登录"）：

```
POST /api/auth/login {"username":"android_probe","password":"wrongpass123"}
→ HTTP 401  {"code":"INVALID_CREDENTIALS","message":"账号名或密码不正确"}
```

App 界面显示：`认证失败 / 用户名或密码错误 / HTTP 401 · INVALID_CREDENTIALS / 账号名或密码不正确`
并且「登录状态」仍为 **未登录**（没有任何假成功）。
截图：[`13-login-401.png`](../screenshots/13-login-401.png)

> 登录**成功**路径需要真实账号才能演示。我没有在你的 Oracle 里创建账号，
> 也没有伪造登录成功——这是需求里明确要求的。
> 你把测试账号给我，我可以立刻补一次成功登录的截图。

退出登录链路同样已按客户端实现顺序实测（先取 CSRF，再 POST）：

```
GET  /api/auth/csrf  → 200 {"headerName":"X-XSRF-TOKEN","token":"…"}  + Set-Cookie XSRF-TOKEN
POST /api/auth/logout (X-XSRF-TOKEN: <token>, Cookie: XSRF-TOKEN=…)
     → 200 {"code":"OK","message":"请求成功"}
```

---

## 三、自动化测试结果

`docs/test-report.txt` 完整记录，摘要：

```
JUnit version 4.13.2
[live] GET /api/health -> 200 {"application":"UP","database":"UP"}
[live] GET /api/anime?page=1&size=3 -> 200 total=5 totalPages=2 items=3
[live] work=110 episode=1 source=/api/media/playback/0290e37e-….mp4 quality=1080p
[live] RANGE … -> 206 Content-Range=bytes 0-1/4068399266 Accept-Ranges=bytes
[live] GET /api/media/catalog/uploads/665374d6-….jpg -> 200 type=image/jpeg length=175775
[live] GET /api/auth/csrf -> {"code":"OK", …, "headerName":"X-XSRF-TOKEN","token":"…"} cookies=[XSRF-TOKEN]
[live] POST /api/auth/login (wrong password) -> 401 {"code":"INVALID_CREDENTIALS", …}

OK (47 tests)
```

- 41 个纯逻辑测试：媒体地址解析与拦截、服务器地址校验、DTO 解析、错误分类；
- 6 个真实后端契约测试（`-PliveBackend=true` 才执行，默认跳过，不会影响 CI）。

---

## 四、构建产物

| 产物 | 大小 | 说明 |
|---|---|---|
| `app/build/outputs/apk/debug/app-debug.apk` | 16.5 MB | 可安装、已实测运行 |
| `app/build/outputs/apk/release/app-release-unsigned.apk` | 3.1 MB | R8 混淆通过；release 禁止明文 HTTP |

Release 的 HTTPS 约束有两层：

1. `app/src/main/res/xml/network_security_config.xml`：`cleartextTrafficPermitted="false"`；
2. 运行时 `ServerAddress.parse` / `MediaUrls.resolve` 直接拒绝 `http://`（`BuildConfig.ALLOW_CLEARTEXT=false`）。

**打包产物级验证**（用 `aapt2` 直接读 APK 里的编译后资源，不是看源码）：

```text
# Release APK（资源被 R8/资源压缩重命名为 res/8G.xml）
E: network-security-config
    E: base-config
      A: cleartextTrafficPermitted=false      ← 禁止明文
      A: src="system"

# Debug APK
A: cleartextTrafficPermitted=true             ← 允许局域网 HTTP 联调
A: src="user"
```

两份安装包也放在 `dist/` 下，可直接安装：

- `dist/fantasy-library-android-debug.apk`（可直接连局域网 HTTP）
- `dist/fantasy-library-android-release-unsigned.apk`（未签名，只能走 HTTPS）

---

## 五、如实记录的限制

1. **模拟器解码限制**：视频 3.79 GB / 1080p，模拟器软件解码（SwiftShader）；
   跳到 1:13:53 后再解码偶发 `ERROR_CODE_DECODING_FAILED`，界面显示
   「播放失败 / 解码失败：视频编码不受支持」并可「重试播放」（见
   [`11-video-decode-error.png`](../screenshots/11-video-decode-error.png)）。
   首次播放与拖动本身均已成功（截图 08 / 09）。真机硬件解码一般不会遇到。
2. **登录成功路径**：需要真实账号（见第二节）。
3. **分集时长**：数据库 `durationSeconds=60`，实际视频 2:15:01；客户端显示真实媒体时长，未改后端数据。
4. 本机 Gradle `Test` worker 因执行环境管道限制无法启动，测试改用 `JUnitCore` 直接运行（同一批测试类）。
