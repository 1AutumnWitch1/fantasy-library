# 幻想図書館 · Fantasy Library

[中文](README.md) · [English](README.en.md) · **日本語**

![license](https://img.shields.io/badge/license-MIT-c8a45c)
![spring](https://img.shields.io/badge/Spring%20Boot-3.5-6db33f)
![vue](https://img.shields.io/badge/Vue-3.5-42b883)
![kotlin](https://img.shields.io/badge/Kotlin-2.2-7f52ff)
![android](https://img.shields.io/badge/Android-minSdk%2026-3ddc84)

**完全にローカルで動く**「中世幻想図書館」── 古い本棚とページめくりの演出でアニメ・ゲームの
コレクションを閲覧するサイトです。Spring Boot の REST API、Oracle データベース、プライベートな
MinIO オブジェクトストレージ、そしてネイティブ Android テストクライアントで構成されています。

すべてのサービスは Docker Compose で自分の PC 上だけで動作し、外部のオンラインサービスには
依存しません。館蔵データは Oracle、表紙画像と動画はバックエンドが MinIO からストリーミングします。

---

## 目次

- [機能](#機能)
- [技術スタック](#技術スタック)
- [リポジトリ構成](#リポジトリ構成)
- [クイックスタート](#クイックスタート)
- [Android クライアント](#android-クライアント)
- [主な API](#主な-api)
- [テスト](#テスト)
- [セキュリティとプライバシー](#セキュリティとプライバシー)
- [素材とライセンス](#素材とライセンス)
- [詳細ドキュメント](#詳細ドキュメント)

## 機能

**公開サイト（閲覧）**

- 本棚ホーム画面、本棚／カテゴリの 2 つの入口、本を開く・ページをめくる演出
- アニメ・ゲームの館蔵一覧、ページ送り、カテゴリ／タグ絞り込み、キーワード検索
- 作品詳細：あらすじ、タグ、挿絵、制作会社／開発元／対応プラットフォーム
- **上映室**：MinIO からストリーミングする自作プレイヤー（HTTP Range によるシーク対応）
- ゲームのダウンロードノード選択、ダウンロード待ち行列、解説の編集
- アカウント：登録／ログイン／マイ本棚（視聴中・ウィッシュリスト・視聴履歴）
- コミュニティ原稿：投稿、コメント、いいね、通報

**管理サイト（司書）**

- 公開サイトとは分離されたセッションを持つ独立の管理サイト
- 作品・エピソード・再生ソース・ダウンロードノード・画像・タグの一括管理
- コメントと通報の審査、メンバー管理

**Android クライアント（`android-client/`）**

- Kotlin + Jetpack Compose のネイティブアプリ、MVVM 構成
- サーバーアドレスの設定（IP／ポートを変更・保存・実接続テスト）
- Oracle の実データ読込、MinIO 画像の表示、MinIO 動画の再生（全画面・シーク対応）
- 実際の Cookie + CSRF ログイン。モックデータなし、動画 URL のハードコードなし

## 技術スタック

| レイヤー | 技術 |
| --- | --- |
| 公開フロントエンド | Vue 3.5、Vue Router 4、Pinia 3、Axios、GSAP（ページめくり）、Vite |
| 管理フロントエンド | Vue 3.5、Vue Router 4、Pinia 3、Vite |
| バックエンド | Java 21、Spring Boot 3.5、Spring Security（Cookie + CSRF セッション）、MyBatis、Jackson |
| データベース | Oracle Database Free（`FREEPDB1`） |
| オブジェクトストレージ | MinIO（プライベートバケット、バックエンド経由で配信） |
| デプロイ | Docker Compose（frontend / admin-frontend / backend / oracle / minio / minio-init） |
| Android | Kotlin 2.2、AGP 8.13、Jetpack Compose（BOM 2025.09）、Media3 1.4、Retrofit 2.11 + OkHttp 4.12、kotlinx.serialization、Coil 2.7、DataStore、minSdk 26 / compileSdk 36 |

## リポジトリ構成

```
fantasy-library/
├── frontend/            # 公開サイト（Vue 3 + Vite）、Nginx が /api を転送
├── admin-frontend/      # 管理サイト（Vue 3 + Vite）、独立セッションとポート
├── backend/             # Spring Boot REST API + MyBatis + デモ用静的素材
│   ├── src/main/java/com/fantasylibrary/api        # コントローラ
│   ├── src/main/resources/mybatis                 # マッパー XML
│   └── db/oracle/                                 # スキーマとマイグレーション
├── docker/minio-init/   # バケットとアクセスキーの初期化（1 回だけ）
├── android-client/      # ネイティブ Android テストクライアント（独立 Gradle プロジェクト）
│   ├── app/src/main/java/com/fantasylibrary/android/{data,ui,di}
│   ├── docs/            # API 契約 / ネットワーク設定 / 検証記録
│   └── screenshots/     # 実機（エミュレータ）検証スクリーンショット
├── compose.yaml         # 全サービスのオーケストレーション
├── LICENSE              # MIT
└── README.md · README.en.md · README.ja.md
```

## クイックスタート

### 1. 前提環境

- Docker Desktop（Linux コンテナモード）
- JDK 21（Docker だけ使う場合は不要）
- Node.js 22（Docker だけ使う場合は不要）
- Android Studio + SDK 36（Android クライアントのみ）

### 2. 環境変数の設定

```bash
cp docker/compose.env.example .env.docker
# Oracle / MinIO の認証情報とポートを記入
```

> `.env.docker` は git 管理外です。**秘密情報がリポジトリに入ることはありません**。

### 3. 起動

```bash
docker compose --env-file .env.docker up -d --wait
docker compose --env-file .env.docker ps -a
```

| サービス | URL |
| --- | --- |
| 公開サイト | http://localhost:5173/ |
| 管理サイト | http://localhost:5174/ |
| ヘルスチェック | http://localhost:8080/api/health |
| Oracle | `jdbc:oracle:thin:@//localhost:11521/FREEPDB1` |
| MinIO API / コンソール | http://127.0.0.1:19000/ · http://127.0.0.1:19001/ |

空のボリュームで初めて起動する場合は、`FREEPDB1` にアプリケーション用アカウントを作成し、
`backend/db/oracle/install.sql` を実行してサンプルデータを投入してから MinIO に素材を
インポートしてください。既存環境は既存の名前付きボリュームをそのまま使います。

## Android クライアント

`android-client/` は**独立した Gradle プロジェクト**で、Web 側のビルドには含まれません。

```bash
cd android-client
./gradlew :app:assembleDebug          # 出力: app/build/outputs/apk/debug/
```

Android Studio で `android-client/` を開きます（Gradle JDK は 17 以上）。
初回起動時は「接続」画面で止まるので、PC のアドレスを入力すると実データを読み込みます。

| 環境 | 入力する値 |
| --- | --- |
| Android エミュレータ | `10.0.2.2` + `8080`（`10.0.2.2` はホストのループバック） |
| 実機（USB） | `adb reverse tcp:8080 tcp:8080` の後 `127.0.0.1` + `8080` |
| 実機（LAN） | Docker のポートを `0.0.0.0` に公開しファイアウォールを許可、PC の LAN IP を入力 |

5 つの画面：**接続設定** → **サービス状態** → **アニメ資料庫** → **ゲーム資料庫** → **上映室**。
ほかに作品詳細とログイン画面があります。平文 HTTP は debug ビルドのみ許可、release は HTTPS 必須です。

関連ドキュメント：

- [`android-client/docs/API-CONTRACT.md`](android-client/docs/API-CONTRACT.md) — ソースと実機で裏付けた API 契約
- [`android-client/docs/NETWORK-SETUP.md`](android-client/docs/NETWORK-SETUP.md) — エミュレータ／実機の接続方法
- [`android-client/docs/VERIFICATION.md`](android-client/docs/VERIFICATION.md) — スクリーンショット付きの受け入れ記録

## 主な API

| メソッド | パス | 説明 |
| --- | --- | --- |
| GET | `/api/health` | アプリとデータベースの状態（ラップなし） |
| GET | `/api/anime` | アニメ一覧（`page`、`size`、`category`、`tag`、`q`） |
| GET | `/api/games` | ゲーム一覧 |
| GET | `/api/works/{id}` | 作品詳細（タグ、画像、アニメ／ゲーム情報） |
| GET | `/api/works/{id}/episodes` | エピソードと再生ソース |
| GET | `/api/media/catalog/{path}` | MinIO から配信する画像 |
| GET | `/api/media/playback/{file}` | HTTP Range 対応の動画ストリーム |
| GET/POST | `/api/auth/csrf`、`/api/auth/login`、`/api/auth/logout` | Cookie + CSRF セッション |
| GET | `/api/me/**` | 保護されたマイ本棚 API |

業務レスポンスは常に `{ code, message, data, errors }` 形式です。エラーコードと HTTP ステータスの
対応は `backend/src/main/java/com/fantasylibrary/api/ApiExceptionHandler.java` を参照してください。

## テスト

```bash
# バックエンド
cd backend && mvn test

# 公開サイト / 管理サイト
cd frontend && npm run test
cd admin-frontend && npm run test

# Android：ロジックのみ 41 件
cd android-client && ./gradlew :app:testDebugUnitTest
# さらに実バックエンド契約テスト 6 件（ローカルでサービス起動が必要）
./gradlew :app:testDebugUnitTest -PliveBackend=true
```

Android の契約テストは実際に `/api/health`、`/api/anime`、エピソード、表紙のバイト列、
`Range: bytes=0-1` を要求し、誤ったパスワードが `401 INVALID_CREDENTIALS` を返すことまで検証します。

## セキュリティとプライバシー

- リポジトリに実認証情報はありません：`.env.docker`、`*.local`、`uploads/` はすべて無視されます
- ブラウザは Nginx とのみ通信し、`/api` は Spring Boot へ転送されます。Oracle と MinIO は非公開です
- セッションは DB 保存トークン + HttpOnly Cookie + CSRF ヘッダー、パスワードは BCrypt で保存
- Android クライアントは DB や MinIO の認証情報を持たず、セッション Cookie と CSRF トークンのみを暗号化保存します
- クライアントは `localhost`、`127.0.0.1`、Docker 内部ホスト名など端末から到達できない URL を拒否します

## 素材とライセンス

- ソースコードは **MIT** ライセンスです — [`LICENSE`](LICENSE) を参照
- `frontend/src/assets/` と `backend/src/main/resources/static/api/media/` の画像は**オリジナルのデモ素材**です
- **実際のアニメ表紙や動画はリポジトリに含まれません**：運用者が MinIO にインポートします（`uploads/` は無視）
- 自分のデータで運用する場合は、取り込む素材とメタデータの権利をご自身で確認してください

## 詳細ドキュメント

段階ごとの開発・運用記録（Docker の使い方、データベース移行、各段階の実装メモ）は
中国語版 [`README.md`](README.md) にまとめてあります。
