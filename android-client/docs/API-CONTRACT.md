# 现有 Spring Boot 接口契约（Android 端已按此实现）

本文档**只记录事实**：每一条都来自 `backend/src/main/java` 的真实 Controller / Service 源码，
并用运行中的服务（Windows + Docker Compose）实测确认。没有猜测的路径或字段。

> 管理端 `/api/admin/**` 与本测试客户端无关，Android 端**不调用**任何管理接口。

---

## 1. 通用约定

### 1.1 成功响应包装

除 `/api/health` 与媒体二进制接口外，业务接口统一返回：

```json
{ "code": "OK", "message": "请求成功", "data": { }, "errors": [] }
```

- `data` 在无内容时被省略（`@JsonInclude(NON_NULL)`）；
- `errors` 为空数组时被省略；
- `code` 枚举见 `ApiResponse.Code`。

### 1.2 失败响应与 HTTP 状态码

来自 `ApiExceptionHandler`：

| HTTP | code 示例 | 含义 |
|---|---|---|
| 400 | `INVALID_ARGUMENT` | 参数校验失败，`errors[]` 给出字段级信息 |
| 401 | `LOGIN_REQUIRED` / `INVALID_CREDENTIALS` | 未登录 / 账号密码错误 |
| 403 | `FORBIDDEN` | CSRF 校验失败或权限不足 |
| 404 | `ROUTE_NOT_FOUND` / `WORK_NOT_FOUND` / `PLAYBACK_NOT_FOUND` / `MEDIA_NOT_FOUND` | 路径或资源不存在 |
| 405 | `METHOD_NOT_ALLOWED` | 方法不支持 |
| 503 | `DATABASE_UNAVAILABLE` / `CATALOG_NOT_INITIALIZED` / `MEDIA_STORAGE_UNAVAILABLE` | Oracle 或 MinIO 不可用 |
| 500 | `INTERNAL_ERROR` | 未预期错误 |

---

## 2. 健康检查

### `GET /api/health`

- Controller：`HealthController`（`@RequestMapping("/api")` + `@GetMapping("/health")`）
- **不套 `ApiResponse`**，直接返回对象：

```json
{ "application": "UP", "database": "UP" }
```

- `database` 取值：`UP` / `DOWN` / `NOT_CONFIGURED`（`HealthService.DatabaseStatus`）
- 响应头：`Cache-Control: no-store`
- 实测：`200`，`{"application":"UP","database":"UP"}`

> 注意：该接口**没有**出现在 `backend/src/main/resources/static/api/openapi.yaml` 中，
> 但源码与运行时均确认存在。Android 端直接使用真实接口，不做任何伪造。

---

## 3. 馆藏（动漫 / 游戏）

### 3.1 `GET /api/anime`

查询参数（`CatalogQuery`）：

| 参数 | 约束 | 说明 |
|---|---|---|
| `page` | ≥ 1，默认 1 | 页码 |
| `size` | 1–100，默认 20 | 每页数量 |
| `category` | slug，正则 `[a-z0-9]+(-[a-z0-9]+)*` | 分类 |
| `tag` | 同上 | 单标签 |
| `tags` | 同上（可重复） | 多标签 |
| `q` | ≤ 100 字符 | 关键词 |

返回：

```json
{
  "code": "OK",
  "message": "请求成功",
  "data": {
    "items": [
      {
        "work": {
          "id": 110,
          "type": "ANIME",
          "slug": "overlord",
          "title": "OVERLORD",
          "summary": "...",
          "releaseDate": "2026-11-05",
          "category": { "id": 16, "slug": "story", "name": "叙事冒险", "sortOrder": 110 },
          "cover": {
            "id": 130, "kind": "COVER",
            "url": "/api/media/catalog/uploads/665374d6-....jpg",
            "altText": "1", "sortOrder": 0
          }
        },
        "anime": { "episodeCount": 1, "studio": "KADOKAWA", "airingStatus": "COMPLETED" },
        "tags": [ { "id": 2, "slug": "adventure", "name": "冒险" } ]
      }
    ],
    "page": 1, "size": 3, "total": 5, "totalPages": 2
  }
}
```

要点：

- **分页是真实支持的**（`total` / `totalPages`），不需要客户端自己截断；
- `cover.url` 是**站内相对路径**（`/api/media/catalog/...`），必须拼到服务器地址上；
- `anime.episodeCount` 可为 `null`（实测 `#83 电锯人`）；
- `airingStatus`：`UPCOMING` / `ONGOING` / `COMPLETED`。

### 3.2 `GET /api/games`

参数与分页结构同上，`data.items[]` 为：

```json
{
  "work": { "...": "同 AnimeItem.work" },
  "game": {
    "developer": "未知",
    "ageRating": "18+",
    "platforms": [ { "id": 2, "slug": "pc", "name": "PC" } ]
  },
  "tags": [ { "id": 2, "slug": "adventure", "name": "冒险" } ]
}
```

### 3.3 `GET /api/categories` / `GET /api/tags?type=ANIME|GAME`

`ApiResponse<List<Category>>` 与 `ApiResponse<List<Tag>>`。本客户端仅用于诊断展示，
列表页直接使用 `/api/anime`、`/api/games`。

### 3.4 `GET /api/works/{id}`

返回 `WorkDetail`：

```json
{
  "work": { "...": "同 Work" },
  "tags": [ ... ],
  "images": [ { "id": 130, "kind": "COVER", "url": "/api/media/catalog/...", "sortOrder": 0 } ],
  "anime": { "episodeCount": 1, "studio": "KADOKAWA", "airingStatus": "COMPLETED" }
}
```

- 动漫作品返回 `anime`，游戏作品返回 `game`，另一个字段被 `@JsonInclude(NON_NULL)` 省略；
- `images` 至少包含封面（实测 `#110` 只有 1 张）。

---

## 4. 播放（视频）

### 4.1 `GET /api/works/{id}/episodes?allNodes=true`

```json
{
  "code": "OK",
  "data": [
    {
      "id": 114, "edition": "EPISODE", "number": 1, "title": "1", "durationSeconds": 60,
      "sources": [
        {
          "id": 116, "label": "1", "quality": "1080p",
          "url": "/api/media/playback/0290e37e-a88f-444f-89c9-b5333448fa6a.mp4",
          "format": "MP4"
        }
      ],
      "nodeId": 104, "nodeLabel": "本地"
    }
  ]
}
```

- 只有 `ANIME` 作品有分集；非动漫作品返回 `WORK_NOT_FOUND`；
- `allNodes=false`（默认）只返回第一个有源的节点；客户端使用 `allNodes=true` 以便展示全部节点；
- 播放地址同样是**站内相对路径**。

### 4.2 `GET /api/media/playback/{fileName}`

- 由 `PlaybackService.media()` 从 MinIO 读取 `playback/{fileName}`；
- 只服务数据库中已登记（`mapper.listedMedia(url) > 0`）的资源，否则 `PLAYBACK_NOT_FOUND`；
- 文件名白名单：`[A-Za-z0-9_-]+\.(mp4|webm)`；
- **支持 HTTP Range**，实测：

```http
GET /api/media/playback/0290e37e-....mp4
Range: bytes=0-1

HTTP/1.1 206
Accept-Ranges: bytes
Content-Range: bytes 0-1023/4068399266
Content-Type: video/mp4
Content-Length: 1024
```

> 视频总长约 3.79 GB，206 + `Content-Range` 说明可以拖动进度条、断点续播。

### 4.3 预签名 URL

`PlaybackService.permitted(url)` 允许两种形态：

1. 站内相对路径 `/api/media/playback/xxx.mp4`；
2. 绝对 `https://` 地址（`userInfo` 必须为空）。

也就是说：若数据库里存的是 MinIO 预签名 URL，后端会原样返回，客户端需要**原样使用**并保留 query
签名参数。Android 端的 `MediaUrls.resolve` 对这类地址返回 `PRESIGNED`，不做任何重写。

---

## 5. 图片

### `GET /api/media/catalog/{suffix}` 与 `GET /api/media/site/{suffix}`

- `CatalogMediaController` 从 MinIO 读取 `catalog/...`、`site/...` 对象；
- 实测封面：`200`，`Content-Type: image/jpeg`，`Content-Length: 175775`，`Accept-Ranges: bytes`；
- 地址只允许 `[A-Za-z0-9_-]+\.(webp|jpe?g|png|avif|gif)`，防止路径穿越。

**客户端绝不持有 MinIO 的 accessKey / secretKey**，也不访问 `:9000`/`:9001` 管理端口。

---

## 6. 登录与会话（重要）

现有后端**没有** Bearer Token 接口，使用的是「数据库会话令牌 + HttpOnly Cookie + CSRF 头」：

| 步骤 | 请求 | 说明 |
|---|---|---|
| 1 | `GET /api/auth/csrf` | 返回 `{ "headerName": "X-XSRF-TOKEN", "token": "..." }`，同时下发 `XSRF-TOKEN` Cookie |
| 2 | `POST /api/auth/login` | Body `{ "username": "...", "password": "..." }`；必须带 CSRF 头 |
| 3 | 服务端下发 | `Set-Cookie: fantasy_library_session=...; Path=/api; HttpOnly; SameSite=Lax` |
| 4 | `GET /api/me/profile` | 受保护接口，靠 Cookie 识别身份 |
| 5 | `POST /api/auth/logout` | 需 CSRF 头与 Cookie，成功后清除会话 |

关键事实（来自 `LibrarySecurity` / `SessionCookie` / `AccountController`）：

- 会话是 `SessionCreationPolicy.STATELESS` + 自定义 `SessionTokenFilter`，
  令牌摘要存在 Oracle 表 `fl_login_token`；
- Cookie 为 `HttpOnly`、`SameSite=Lax`，`Path=/api`；
- `Secure` 标志由 `AUTH_SECURE_COOKIE` 或 `request.isSecure()` 决定，
  局域网 HTTP 部署时为 `false`，因此明文环境下也能正常保持登录；
- 密码错误返回 `401` + `INVALID_CREDENTIALS`（"账号名或密码不正确"）；
- 没有面向移动端的「刷新令牌 / Bearer Token」接口。

因此 Android 端：

- 自己实现 `CookieJar` 持久化 Cookie（`PersistentCookieJar`）；
- 用 `CsrfInterceptor` 自动给非 GET 请求附加 CSRF 头；
- 登录成功后立刻调用 `/api/me/profile` 验证会话真的生效；
- 只保存 Cookie 与 CSRF token（加密存储），**不保存密码**；
- 退出登录时先刷新 CSRF 再 POST logout，并清空本地 Cookie。

---

## 7. 现有项目的缺口（未擅自修改）

| 缺口 | 影响 | 客户端处理 |
|---|---|---|
| 无 Bearer Token 登录接口 | 不适合无 Cookie 的原生客户端习惯 | 已用 Cookie 持久化方案实现真实登录 |
| `/api/health` 未写入 `openapi.yaml` | 文档与实现不一致 | 按源码 + 实测使用真实接口 |
| 无「视频列表」独立接口 | 只能经由 作品 → 分集 → 源 获取 | 播放页按该路径逐级选择 |
| 无 `MEDIA_BASE_URL` 之类的绝对地址配置 | 依赖相对路径拼接 | `MediaUrls` 以用户配置的地址为 origin 拼接 |
| 单页 `size` 上限 100 | 数据量大时需翻页 | 列表页实现 `加载更多` |

> 以上缺口都**没有**通过修改 Web 项目来"补上"：`backend/`、`frontend/`、`admin-frontend/`、
> `compose.yaml`、`.env.docker` 均保持原样（只读）。
