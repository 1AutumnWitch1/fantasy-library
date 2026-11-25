# 幻想图书馆 · Android 卷轴版

这是幻想图书馆的原生 Android 用户端，使用 Kotlin 与 Jetpack Compose。它沿用网站的深胡桃木、皮革、黄铜和羊皮纸材质，以自上而下排列的五根横向卷轴进入追番、游戏、社区、推荐和个人书架。

## 打开应用

1. 在 Android Studio 打开本目录，选中 `app` 后运行到模拟器或 Android 设备。项目可使用 JDK 17。
2. 在任一未连接的馆藏卷轴打开「连接设置」，填写能从设备访问的图书馆地址。本机标准 Android Studio 模拟器可在 Debug 版使用 HTTP、主机 `10.0.2.2`、端口 `8080`。
3. 使用「检查连接」确认服务器就绪，再返回首页。真机使用能从手机访问的服务器地址。Release 版使用 HTTPS。

Debug APK 的构建输出为 `app/build/outputs/apk/debug/app-debug.apk`。

## 浏览路径

- 追番和游戏使用服务端关键词搜索和标签过滤。热门游戏按服务端热度显示。
- 社区支持按话题和热度浏览。登录后可发图文手稿、点赞和回复。作品档案可以查看相关讨论。
- 推荐使用真实榜单和推荐理由，包含新增馆藏及活跃讨论。
- 个人书架使用同一网站账号，显示追番进度、游戏收藏和愿望单、观看记录、馆内消息。
- 作品档案保留放映室和游戏下载入口。放映室顶部固定使用 AndroidX Media3 播放，下方可切换推荐、番剧评论，播放源按节点和分集组织。

首页头像进入独立个人资料，未登录时先打开登录页。个人书架依然从第五根卷轴进入。从首页主动打开卷轴会播放展开动画。从详情返回会恢复之前的页面。可用 `fantasy-library://scroll/anime` 等直达地址，路径对应 `anime`、`games`、`community`、`recommendations`、`personal`。

## 构建和测试

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:testDebugUnitTest -PliveBackend=true
```

真实契约测试默认连接 `http://127.0.0.1:8080`，可以用 `-PliveBackendUrl` 指定测试地址。现有 Gradle 测试进程在此机器的命令行环境可能无法启动，项目中的同一批测试可以通过 JUnitCore 执行。

## 结构

- `ui/AppNavHost.kt` 管理卷轴进入、详情返回及直达地址。
- `ui/screens` 包含首页、内容卷轴、详情、放映室、登录和连接设置。
- `ui/components/ScrollComponents.kt` 提供通用阅读页、控件、封面与状态样式。
- `data/network` 和 `data/repository` 包含 Retrofit 接口、媒体 URL 处理及业务数据访问。会话继续使用 Cookie 和 CSRF，图片经 Coil 加载。

`docs/VERIFICATION.md` 和 `docs/test-report.txt` 是初版联调客户端的历史验证记录。
