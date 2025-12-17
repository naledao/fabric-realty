# springbootserver（M1 工程骨架）

本目录为 `SPRINGBOOT_MIGRATION_PLAN.md` 的 M1/M2 产物：

- M1：Spring Boot 工程骨架（配置绑定、统一响应、异常处理、健康检查、API 路由）
- M2：Fabric Gateway Java 接入（多组织连接、合约缓存、evaluate/submit 封装、错误翻译）

## 运行要求

- JDK 17（已验证：`java -version` 为 17.x）
- Maven 3.9+（本环境未内置，可自行安装）

## 启动

在 `application/server/springbootserver` 下执行：

```bash
mvn spring-boot:run
```

默认端口：`8888`（见 `src/main/resources/application.yml`）。

## Docker（M5）

后端容器期望：

- 工作目录：`/app`
- 配置：`/app/config/application.yml`（已内置，见 `config/application.yml`）
- 数据目录：`/app/data`（需通过 compose 挂载持久化）

使用 compose 覆盖文件切换到 Spring Boot 后端（不修改 `application/docker-compose.yml`）：

```bash
docker compose -f application/docker-compose.yml -f application/server/springbootserver/docker-compose.springboot.override.yml up -d --build
```

一键切换/回滚脚本（需要 Docker Compose）：

```bash
# 切到 Spring Boot
bash application/server/springbootserver/switch-backend.sh springboot

# 切回 Go（使用原镜像）
bash application/server/springbootserver/switch-backend.sh go
```

## 验收点（M1）

- `GET /actuator/health` 返回 UP
- `/api/...` 路由存在且返回统一结构：`{"code":200,"message":"成功","data":...}`

## 验收点（M2）

先启动 Fabric 网络（见仓库根目录 `dev.md` / `network/install.sh`），再启动本服务：

- 至少跑通 1 条写链码：`POST /api/realty-agency/realty/create`
- 至少跑通 1 条读链码：`GET /api/realty-agency/realty/{id}`

可直接使用：`M0_EXAMPLES.http` 里的请求样例。

## 验收点（M3）

前端全流程（登记 -> 交易 -> 银行确认 -> 查询）跑通，依赖以下接口均为真实链码调用（非 mock）：

- 房产：创建/查询/列表
- 交易：创建/查询/列表
- 银行：完成交易/查询/列表

详细对照与联调步骤：`M3_BUSINESS_MIGRATION.md`。

## 验收点（M4）

- 启动后自动监听区块（每组织独立监听、断点续听、重连、落盘）
- `/api/*/block/list` 从本地持久化分页查询（按区块号倒序，字段名对齐 Go）

实现与数据库说明：`M4_BLOCK_LISTENER.md`。

## 说明

- 区块监听与区块列表已在 M4 落地，首次启动可能需要等待监听落盘后再访问 `/block/list`。
- 本环境未内置 Maven（`mvn`），需要自行安装后再运行。
