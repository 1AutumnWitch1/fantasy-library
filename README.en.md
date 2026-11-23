# Fantasy Library

[中文](README.md) · **English** · [日本語](README.ja.md)

![license](https://img.shields.io/badge/license-MIT-c8a45c)
![spring](https://img.shields.io/badge/Spring%20Boot-3.5-6db33f)
![vue](https://img.shields.io/badge/Vue-3.5-42b883)
![kotlin](https://img.shields.io/badge/Kotlin-2.2-7f52ff)
![android](https://img.shields.io/badge/Android-minSdk%2026-3ddc84)

A **fully local** "mediaeval fantasy library": an anime & game collection site rendered as a
reverent old bookshelf (with book-opening and page-flip animations), backed by a Spring Boot REST
API, an Oracle database, a private MinIO object store — plus a native Android test client.

Everything runs on your own machine via Docker Compose. No third-party online services are
involved: catalog data lives in Oracle, and cover images / videos are streamed by the backend
from MinIO.

---

## Table of contents

- [Features](#features)
- [Tech stack](#tech-stack)
- [Repository layout](#repository-layout)
- [Quick start](#quick-start)
- [Android client](#android-client)
- [API overview](#api-overview)
- [Testing](#testing)
- [Security & privacy](#security--privacy)
- [Assets & license](#assets--license)
- [Detailed documentation](#detailed-documentation)

## Features

**Public site (reader)**

- Bookshelf home page with two entry points (shelf / category), book-opening and page-flip animation
- Anime and game catalogs with pagination, category & tag filtering, keyword search
- Work detail pages: summary, tags, illustrations, studio / developer / platform metadata
- **Screening room**: custom player streaming from MinIO with HTTP Range seeking
- Game download nodes, download queue and editorial notes
- Accounts: sign-up / sign-in / private shelf (watching, wishlist, history)
- Community manuscripts: posts, comments, likes, reports

**Admin site (librarian)**

- Separate admin site with its own session, isolated from public accounts
- Full management of works, episodes, playback sources, download nodes, images and tags
- Comment & report moderation, member management

**Android client (`android-client/`)**

- Native Kotlin + Jetpack Compose app, MVVM layering
- Configurable server address (IP / port, persisted, with a real connectivity test)
- Real Oracle catalog data, MinIO images, MinIO video playback (fullscreen + seeking)
- Real cookie + CSRF login — no mock data, no hard-coded media URLs

## Tech stack

| Layer | Technology |
| --- | --- |
| Public frontend | Vue 3.5, Vue Router 4, Pinia 3, Axios, GSAP (page-flip animation), Vite |
| Admin frontend | Vue 3.5, Vue Router 4, Pinia 3, Vite |
| Backend | Java 21, Spring Boot 3.5, Spring Security (cookie + CSRF sessions), MyBatis, Jackson |
| Database | Oracle Database Free (`FREEPDB1`) |
| Object storage | MinIO (private bucket, proxied & signed by the backend) |
| Deployment | Docker Compose (frontend / admin-frontend / backend / oracle / minio / minio-init) |
| Android | Kotlin 2.2, AGP 8.13, Jetpack Compose (BOM 2025.09), Media3 1.4, Retrofit 2.11 + OkHttp 4.12, kotlinx.serialization, Coil 2.7, DataStore, minSdk 26 / compileSdk 36 |

## Repository layout

```
fantasy-library/
├── frontend/            # public Vue 3 + Vite site, Nginx proxies /api
├── admin-frontend/      # admin Vue 3 + Vite site, own session and port
├── backend/             # Spring Boot REST API + MyBatis + demo static assets
│   ├── src/main/java/com/fantasylibrary/api        # controllers
│   ├── src/main/resources/mybatis                 # mapper XML
│   └── db/oracle/                                 # schema and migrations
├── docker/minio-init/   # one-shot bucket & access-key bootstrap
├── android-client/      # native Android test client (standalone Gradle project)
│   ├── app/src/main/java/com/fantasylibrary/android/{data,ui,di}
│   ├── docs/            # API contract / network setup / verification log
│   └── screenshots/     # device verification screenshots
├── compose.yaml         # full service orchestration
├── LICENSE              # MIT
└── README.md · README.en.md · README.ja.md
```

## Quick start

### 1. Prerequisites

- Docker Desktop (Linux containers)
- JDK 21 (not needed if you only use Docker)
- Node.js 22 (not needed if you only use Docker)
- Android Studio + SDK 36 (only for the Android client)

### 2. Configure environment

```bash
cp docker/compose.env.example .env.docker
# fill in Oracle / MinIO credentials and ports
```

> `.env.docker` is git-ignored — **no secret ever enters this repository**.

### 3. Run

```bash
docker compose --env-file .env.docker up -d --wait
docker compose --env-file .env.docker ps -a
```

| Service | URL |
| --- | --- |
| Public site | http://localhost:5173/ |
| Admin site | http://localhost:5174/ |
| Health check | http://localhost:8080/api/health |
| Oracle | `jdbc:oracle:thin:@//localhost:11521/FREEPDB1` |
| MinIO API / console | http://127.0.0.1:19000/ · http://127.0.0.1:19001/ |

On a brand-new machine with empty volumes, create the application account in `FREEPDB1`, execute
`backend/db/oracle/install.sql` to seed sample data, then import your media into MinIO. Existing
deployments simply mount their existing named volumes.

## Android client

`android-client/` is a **standalone Gradle project**; it is not part of the web build.

```bash
cd android-client
./gradlew :app:assembleDebug          # output: app/build/outputs/apk/debug/
```

Open `android-client/` in Android Studio (Gradle JDK 17+). On first launch the app stops on the
"Connection" page; enter your server address to read real data:

| Scenario | What to enter |
| --- | --- |
| Android emulator | `10.0.2.2` + `8080` (`10.0.2.2` maps to the host loopback) |
| Physical device (USB) | `adb reverse tcp:8080 tcp:8080`, then `127.0.0.1` + `8080` |
| Physical device (LAN) | bind the Docker ports to `0.0.0.0`, allow the port in the firewall, then use the host LAN IP |

Five screens: **Connection** → **Status** → **Anime catalog** → **Game catalog** → **Screening room**,
plus work detail and sign-in. Cleartext HTTP is allowed in debug builds only; release builds require HTTPS.

Further reading:

- [`android-client/docs/API-CONTRACT.md`](android-client/docs/API-CONTRACT.md) — endpoint-by-endpoint contract verified against the source and live service
- [`android-client/docs/NETWORK-SETUP.md`](android-client/docs/NETWORK-SETUP.md) — emulator / physical device connectivity
- [`android-client/docs/VERIFICATION.md`](android-client/docs/VERIFICATION.md) — acceptance evidence with screenshots and raw output

## API overview

| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/health` | application & database status (not wrapped) |
| GET | `/api/anime` | paginated anime (`page`, `size`, `category`, `tag`, `q`) |
| GET | `/api/games` | paginated games |
| GET | `/api/works/{id}` | work detail (tags, images, anime / game info) |
| GET | `/api/works/{id}/episodes` | episodes and playback sources |
| GET | `/api/media/catalog/{path}` | images proxied from MinIO |
| GET | `/api/media/playback/{file}` | video stream with HTTP Range support |
| GET/POST | `/api/auth/csrf`, `/api/auth/login`, `/api/auth/logout` | cookie + CSRF sessions |
| GET | `/api/me/**` | protected personal-shelf endpoints |

Business responses always use `{ code, message, data, errors }`; the error-code to HTTP-status
mapping lives in `backend/src/main/java/com/fantasylibrary/api/ApiExceptionHandler.java`.

## Testing

```bash
# backend
cd backend && mvn test

# public site / admin site
cd frontend && npm run test
cd admin-frontend && npm run test

# Android: 41 pure-logic unit tests
cd android-client && ./gradlew :app:testDebugUnitTest
# plus 6 live backend contract tests (a local backend must be running)
./gradlew :app:testDebugUnitTest -PliveBackend=true
```

The Android contract tests really request `/api/health`, `/api/anime`, episodes, cover bytes and
`Range: bytes=0-1`, and assert that a wrong password returns `401 INVALID_CREDENTIALS`.

## Security & privacy

- No real credentials in the repository: `.env.docker`, `*.local` and `uploads/` are ignored
- The browser only talks to Nginx; `/api` is reverse-proxied to Spring Boot, while Oracle and MinIO stay private
- Sessions use database-backed tokens + HttpOnly cookies + CSRF headers; passwords are BCrypt-hashed
- The Android client holds no database or MinIO credentials — only the session cookie and CSRF token, stored encrypted
- The client rejects media URLs pointing at `localhost`, `127.0.0.1` or Docker-internal hostnames

## Assets & license

- Source code is released under the **MIT** license — see [`LICENSE`](LICENSE)
- Images under `frontend/src/assets/` and `backend/src/main/resources/static/api/media/` are **original demo artwork**
- **Real anime covers and videos are not in this repository**: operators import them into MinIO (`uploads/` is ignored)
- When deploying with your own data, make sure you hold the rights to the imported artwork and metadata

## Detailed documentation

The exhaustive, phase-by-phase development and operations notes (Docker usage, database migrations,
per-stage implementation details) are kept in the Chinese [`README.md`](README.md).
