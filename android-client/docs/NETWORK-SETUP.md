# 网络连通性配置说明（真机 / 模拟器）

本文件说明**如何在不动现有 Web 项目的前提下**让 Android 客户端连上电脑上的 Spring Boot。
以下命令都需要你自己执行——我不会修改 `compose.yaml`、`.env.docker` 或防火墙设置。

---

## 1. 当前部署的实际情况（已核对）

`compose.yaml` 中所有端口都显式绑定了 `127.0.0.1`：

```yaml
backend:
  ports:
    - "127.0.0.1:${BACKEND_PORT:-8080}:8080"
```

`docker compose ps` 实测：

```
fantasy-library-backend-1   127.0.0.1:8080->8080/tcp
fantasy-library-frontend-1  127.0.0.1:5173->80/tcp
fantasy-library-minio-1     127.0.0.1:19000->9000/tcp, 127.0.0.1:19001->9001/tcp
fantasy-library-oracle-1    127.0.0.1:11521->1521/tcp
```

含义：

- **Android 模拟器可以连**：模拟器里的 `10.0.2.2` 就是宿主机的 `127.0.0.1`，
  所以 `http://10.0.2.2:8080` 直接可用；
- **真实手机通过局域网 IP 连不上**：因为服务只监听宿主机回环，`192.168.x.x:8080` 不会被接受。

这不是客户端的问题，是宿主机端口绑定的问题。

---

## 2. 真机方案 A（推荐，零改动）：`adb reverse`

前提：手机用 USB 连电脑，且已开启「USB 调试」。

```powershell
# 电脑端（SDK 路径按你的实际安装）
D:\soft\learn\platform-tools\adb.exe devices           # 确认设备已授权
D:\soft\learn\platform-tools\adb.exe reverse tcp:8080 tcp:8080
D:\soft\learn\platform-tools\adb.exe reverse --list    # 应看到 tcp:8080 tcp:8080
```

然后在 App 的「连接」页填写：

```
协议：http
地址：127.0.0.1
端口：8080
```

此时手机的 `127.0.0.1:8080` 会被 adb 转发到电脑的 `127.0.0.1:8080`，
**不需要改动 Docker、不需要改防火墙、不需要路由器设置**。

注意事项：

- `adb reverse` 在设备重连或 adb 重启后失效，需要重新执行；
- 同一时刻只能有一个 `adb reverse` 规则占用该端口；
- 若 App 内填写的是 `127.0.0.1`，界面上会显示「回环地址」提示——这只是提醒，
  在 adb reverse 场景下是**正确用法**。

## 3. 真机方案 B：让 Docker 监听局域网（需要你自行修改）

如果你希望通过 `http://192.168.1.20:8080` 直接访问，需要：

1. 修改 `compose.yaml`（或加一个 override 文件），把 backend 的端口绑定改成
   `"${BACKEND_PORT:-8080}:8080"`（去掉 `127.0.0.1:` 前缀）；
2. `docker compose --env-file .env.docker up -d backend`；
3. 在 Windows 防火墙放行 TCP 8080 的入站连接；
4. 确认电脑与手机在同一网段，且电脑 IP 未被 DHCP 改变。

> 我**不会**替你执行这些修改：这会改变现有部署的暴露面。
> 如果决定这么做，建议只开放 backend，不要暴露 Oracle（1521）与 MinIO Console（9001）。

## 4. 真机方案 C：局域网内用 `netsh portproxy`

不改 Docker，把宿主机的局域网地址转发到回环端口：

```powershell
# 需要管理员权限
netsh interface portproxy add v4tov4 listenaddress=0.0.0.0 listenport=8080 `
    connectaddress=127.0.0.1 connectport=8080
netsh interface portproxy show all
```

同样需要在防火墙放行 8080。撤销：

```powershell
netsh interface portproxy delete v4tov4 listenaddress=0.0.0.0 listenport=8080
```

---

## 5. 模拟器

```
地址：10.0.2.2
端口：8080
协议：http
```

`10.0.2.2` 是 Android 模拟器为宿主机回环预留的特殊地址。若用 `127.0.0.1`，
那指的是模拟器自己，必然连接失败。

---

## 6. 客户端侧的明文 / HTTPS 策略

| 构建类型 | 明文 HTTP | 说明 |
|---|---|---|
| `debug` | 允许 | `app/src/debug/res/xml/network_security_config.xml` 中 `cleartextTrafficPermitted="true"` |
| `release` | 禁止 | `app/src/main/res/xml/network_security_config.xml` 中 `cleartextTrafficPermitted="false"`，且代码里也会拒绝 `http://` 地址 |

因此：

- 局域网联调用 **debug 包**；
- release 包必须配 HTTPS（例如把 Nginx / Spring Boot 放到反向代理并启用证书），
  否则保存地址时就会提示「Release 构建只允许 HTTPS」。

---

## 7. 快速自检顺序

1. 电脑上 `curl http://127.0.0.1:8080/api/health` 应返回 `{"application":"UP","database":"UP"}`；
2. 模拟器：App 填 `10.0.2.2:8080` → 测试连接 → 应显示 HTTP 200 与数据库 UP；
3. 真机：先 `adb reverse tcp:8080 tcp:8080`，再填 `127.0.0.1:8080`；
4. 首页能看到「动漫 5 部 / 游戏 4 部」这类真实总量，说明 Oracle 已连通；
5. 动漫列表能显示封面图，说明 MinIO → Spring Boot → App 的图片链路正常；
6. 放映页点「检测服务器是否支持 Range」应返回 `206` 与 `Content-Range`。
