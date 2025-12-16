# Spring Boot 改造企划（fabric-realty / application/server）

> 目标：将当前 `application/server` 的 Go(Gin) 后端改造为 Spring Boot（Java）项目，同时保持前端与链码/网络基本不受影响，做到“接口不改、能力等价、可回滚”。

## 1. 现状盘点（As-Is）

### 1.1 后端现状

- 语言/框架：Go 1.23 + Gin
- Fabric 交互：`fabric-gateway`（Go）分别以 3 个组织身份连接 Fabric Gateway
- 配置：`application/server/config/config.yaml`（本地）与 `application/server/config/config-docker.yaml`（容器）
- 数据落盘：区块监听器将区块信息持久化在 `data/blocks/blocks.db`（BBolt）
- 统一响应：`{"code":httpStatus,"message":"...","data":...}`（前端 axios 拦截依赖该结构）

### 1.2 现有接口（必须保持兼容）

接口前缀：`/api`

- 不动产登记机构（Org1）
  - `POST /api/realty-agency/realty/create`
  - `GET  /api/realty-agency/realty/:id`
  - `GET  /api/realty-agency/realty/list?pageSize&bookmark&status`
  - `GET  /api/realty-agency/block/list?pageSize&pageNum`
- 交易平台（Org3）
  - `POST /api/trading-platform/transaction/create`
  - `GET  /api/trading-platform/realty/:id`
  - `GET  /api/trading-platform/transaction/:txId`
  - `GET  /api/trading-platform/transaction/list?pageSize&bookmark&status`
  - `GET  /api/trading-platform/block/list?pageSize&pageNum`
- 银行（Org2）
  - `POST /api/bank/transaction/complete/:txId`
  - `GET  /api/bank/transaction/:txId`
  - `GET  /api/bank/transaction/list?pageSize&bookmark&status`
  - `GET  /api/bank/block/list?pageSize&pageNum`

### 1.3 与链码的关键契约（必须保持行为一致）

当前后端调用链码函数（方法名/参数顺序）：

- `CreateRealEstate(id, address, area, owner, createTimeRFC3339)`
- `QueryRealEstate(id)`
- `QueryRealEstateList(pageSize, bookmark, status)`
- `CreateTransaction(txId, realEstateId, seller, buyer, price, createTimeRFC3339)`
- `QueryTransaction(txId)`
- `QueryTransactionList(pageSize, bookmark, status)`
- `CompleteTransaction(txId, updateTimeRFC3339)`

链码分页查询返回结构（前端依赖字段名）：

- `records` / `recordsCount` / `bookmark` / `fetchedRecordsCount`

### 1.4 容器与联调现状

- 前端 Nginx 通过 `location /api { proxy_pass http://fabric-realty.server:8888; }` 代理到后端
- `application/docker-compose.yml` 中后端服务名：`fabric-realty.server`（前端依赖此 DNS）
- 容器内配置文件路径：`/app/config/config.yaml`
- 容器内挂载：
  - `./../network/crypto-config -> /network/crypto-config`
  - `./../network/data/application -> /app/data`（区块数据库持久化依赖此目录）

## 2. 改造目标（To-Be）

### 2.1 功能目标

- Spring Boot 后端提供与当前 Go 服务 **完全一致的 API 路径/入参/响应结构**
- Fabric 交互能力等价：按组织身份完成链码调用与查询
- 区块监听与区块列表查询能力等价：断点续听、失败重连、持久化落盘
- 保持现有 `docker-compose` 联调方式（前端无需改动或仅最小改动）

### 2.2 工程目标

- 代码分层清晰：Controller / Service / FabricClient / BlockListener / Storage
- 配置外置化：支持本地与 Docker 两套配置（profile 或外置 config 文件）
- 可观测性：基础健康检查（`/actuator/health`）与日志规范化
- 可回滚：迁移阶段可并行运行 Go 与 Spring Boot 两套后端

## 3. 改造范围（Scope）

### 3.1 包含

- `application/server` 后端改造为 Spring Boot（建议新建目录并逐步切换）
- Dockerfile/Compose、开发文档（`dev.md`）适配 Java 运行方式
- （可选）补齐 OpenAPI 文档与接口契约测试

### 3.2 不包含（默认不动）

- `chaincode/` 链码逻辑与 `network/` Fabric 网络部署脚本
- `application/web` 前端逻辑（除非需要调整代理/端口/错误提示）

## 4. 推荐落地方式（两种方案择一）

### 方案 A：并行新建（推荐，风险最低）

- 保留现有 Go 服务源码（作为对照与回滚手段）
- 新建 `application/server-springboot/`（或 `application/server-java/`）承载 Spring Boot 代码
- 完成联调后，再决定是否删除/归档 Go 版本

优点：可对比验证、可灰度、可快速回滚；缺点：目录略多。

### 方案 B：原地替换（交付形态更“干净”）

- 直接将 `application/server` 改造成 Maven/Gradle 的 Spring Boot 工程结构

优点：结构简单；缺点：迁移中断风险更大、回滚成本更高。

## 5. 目标工程结构建议（以方案 A 为例）

```
application/
  server/                     # legacy Go（保留到切换完成）
  server-springboot/          # new Spring Boot
    Dockerfile
    pom.xml (or build.gradle)
    src/main/java/com/.../
      Application.java
      api/ (controllers)
      service/
      fabric/ (gateway连接、合约获取、错误解析)
      block/ (区块监听、存储、查询)
      common/ (统一响应、异常、DTO)
    src/main/resources/
      application.yml
      application-docker.yml (可选)
    config/ (外置配置，容器运行时覆盖)
      application.yml
```

> 若采用“外置配置”模式：Spring Boot 运行时会自动加载 `./config/application.yml`（相对工作目录）。

## 6. 关键技术方案

### 6.1 技术栈建议

- Java：17（Spring Boot 3.x 推荐）
- Spring Boot：3.2+ / 3.3+
- 构建：Maven（或 Gradle）
- API：`spring-boot-starter-web` + `spring-boot-starter-validation`
- 文档（可选）：`springdoc-openapi-starter-webmvc-ui`
- 可观测（可选）：`spring-boot-starter-actuator`
- Fabric：优先使用 **Fabric Gateway Java SDK**（与现 Go 版本同一代 Gateway API）

### 6.2 配置改造

将现有 YAML 配置迁移为 Spring Boot `application.yml`（保留字段语义），推荐结构：

```yaml
server:
  port: 8888

app:
  dataDir: ./data          # 用于持久化 blocks.db / h2 等
  blockRetrySeconds: 30

fabric:
  channelName: mychannel
  chaincodeName: mychaincode
  organizations:
    org1:
      mspId: Org1MSP
      certPath: ...
      keyPath: ...
      tlsCertPath: ...
      peerEndpoint: localhost:7051
      gatewayPeer: peer0.org1.togettoyou.com
```

说明：

- 字段命名建议从 `mspID` 统一为 `mspId`（Spring 绑定更直观）；如不想改配置，可利用 Spring 的 relaxed binding 做兼容映射
- Docker 环境使用独立 profile（`application-docker.yml`）或外置 `./config/application.yml` 覆盖路径（`/network/...`）

### 6.3 Fabric Gateway Java 连接模型（多组织）

目标：启动时为 `org1/org2/org3` 分别建立连接并缓存 `Contract`，供业务调用。

实现要点：

- 证书/私钥读取：与 Go 一致，读取目录下第一个文件（`certPath`、`keyPath`）
- gRPC TLS：
  - `tlsCertPath` 加载到信任根
  - `peerEndpoint` 可能是 `localhost:7051`（本地）或 `peer0.orgX:7051`（容器）
  - 需要设置 TLS 的 serverName/authority 为 `gatewayPeer`（确保与证书 SAN 匹配）
- 超时策略：对齐 Go 版本（Evaluate/Endorse/Submit/CommitStatus）
- 生命周期：Spring 容器启动时初始化；`@PreDestroy` 关闭 gRPC Channel/Gateway

### 6.4 业务分层与“组织身份”约束

保持现有约束方式（由后端决定使用哪个组织身份发起链码调用）：

- RealtyAgencyService 固定使用 `org1`
- BankService 固定使用 `org2`
- TradingPlatformService 固定使用 `org3`

建议抽象：

- `FabricClient`：`Contract getContract(String org)`、`byte[] submit(...)`、`byte[] evaluate(...)`
- `ErrorTranslator`：将 Fabric/gRPC 异常转换为前端可读的 message

### 6.5 区块监听与持久化（等价替换 Go 的 BBolt）

目标：保持 Go 行为：

- 每组织独立监听 block events
- 支持断点续听：从 `latestBlockNum + 1` 开始
- 监听中断自动重连（默认 30s 重试）
- 提供分页查询：按区块号倒序返回

存储方案建议（二选一）：

1) **H2 file + Spring Data/JDBC（推荐落地快）**
   - 数据文件存放：`${app.dataDir}/blocks/blocks`（容器挂载到 `/app/data`）
   - 表设计：
     - `blocks(org_name, block_num, block_hash, data_hash, prev_hash, tx_count, save_time)`
     - `latest_blocks(org_name, block_num, save_time)`

2) **RocksDB/LevelDB（更贴近 KV，但 Docker 依赖更复杂）**

区块 hash 计算：对齐 Go 版本（`asn1(header{number,previousHash,dataHash})` 后 `sha256`），避免前端展示不一致。

### 6.6 API 层与统一响应/异常处理

保持前端契约：

- 成功：HTTP 200，`code=200`，`message` 可为“成功”或业务提示
- 参数错误：HTTP 400，`code=400`
- 服务端错误：HTTP 500，`code=500`

建议实现：

- `ApiResponse<T>`：统一响应结构
- `@RestControllerAdvice`：
  - 参数校验异常 -> 400
  - Fabric/gRPC 异常 -> 500（提取核心 message）
  - 兜底异常 -> 500

### 6.7 Docker 化与 Compose 接入

目标：前端不改代理目标（仍指向 `fabric-realty.server:8888`），因此后端容器需：

- 监听 `8888`
- 工作目录 `/app`
- 配置在 `/app/config/application.yml`（或 `/app/config/config.yaml` + 自定义加载）
- 数据目录 `/app/data`（持久化区块数据库）

交付物：

- `application/server-springboot/Dockerfile`（多阶段构建：Maven 构建 -> JRE 运行）
- 更新 `application/docker-compose.yml` 的 `fabric-realty.server` 镜像/构建方式（可先用 `build:` 本地构建）

## 7. 迁移步骤与里程碑（Milestones）

### M0：需求冻结与对照基线（0.5~1 天）

- 产出：
  - 接口清单 + 示例请求/响应（可直接从现代码/前端调用整理）
  - 统一响应与错误处理规则确认
- 验收：基线文档完成，前端可用例跑通（以 Go 服务为准）

### M1：Spring Boot 工程骨架（0.5~1 天）

- 产出：
  - Spring Boot 工程初始化（分层目录、配置读取、统一响应、健康检查）
  - 与前端联调的最小可运行版本（可先提供一个 mock 接口）
- 验收：`/actuator/health` 正常；`/api/...` 路由可返回统一响应结构

### M2：Fabric Gateway Java 接入（1~2 天）

- 产出：
  - 多组织连接初始化 + 合约缓存
  - `evaluate/submit` 封装 + 错误翻译
  - 跑通至少 1 条读链码与 1 条写链码
- 验收：本地网络环境下，能成功 `QueryRealEstate` 与 `CreateRealEstate`

### M3：业务接口全量迁移（1~2 天）

- 产出：
  - 3 组 Controller + DTO + Service 完整实现（路径/参数/响应对齐）
  - 分页查询接口返回结构与 Go 一致
- 验收：前端页面全流程（登记->交易->银行确认->查询）跑通

### M4：区块监听与区块列表（1~2 天）

- 产出：
  - BlockListener：断点续听、重连、持久化
  - `/block/list` 查询：分页倒序、字段对齐
- 验收：刷新/重启后能继续接收新区块；前端“区块链浏览”正常展示

### M5：容器化、文档与回滚开关（0.5~1 天）

- 产出：
  - Dockerfile + Compose 改造
  - `dev.md` 更新：Java 开发与运行方式
  - 回滚策略：一键切回 Go 镜像（保留旧 compose 或保留镜像 tag）
- 验收：`./install.sh` 场景（或本地 compose up）可启动并访问 `http://localhost:8000`

## 8. 测试与验收标准（Definition of Done）

- 接口兼容：所有既有路径/方法/参数/响应字段名保持一致
- 功能回归：前端三组织操作流程全部通过
- 稳定性：
  - Fabric 连接失败可快速定位（日志包含 orgName/peerEndpoint）
  - 区块监听中断可自动重连，不产生数据错乱
- 数据持久化：容器重启后区块数据不丢失（依赖 `/app/data` 挂载）
- 性能基线：与 Go 版本相比无明显不可接受的退化（可用简单压测对比）

## 9. 风险清单与对策

- Fabric Gateway Java SDK 版本/API 与 Go 不一致
  - 对策：优先选用与当前 Fabric 2.5.x 兼容的 Gateway Java；若 block events 支持不完整，准备降级方案（使用 Fabric Java SDK 的 block listener 或 QSCC 查询）
- TLS/证书路径差异导致无法连通
  - 对策：提供本地与 Docker 两套 profile；在启动日志打印“证书路径是否存在/peerEndpoint/gatewayPeer”
- 区块监听高频写入导致 IO 压力
  - 对策：批量写入/异步队列（必要时），或数据库写入优化与索引
- 前端依赖错误消息格式
  - 对策：统一异常翻译，保留 message 语义；避免返回非 JSON 的 HTML 错误页

## 10. 回滚策略

- 迁移期保留 Go 服务镜像/源码
- Compose 通过切换镜像 tag 或服务指向实现快速回滚
- 回滚验收：切回后前端全流程可再次跑通

## 11. 工作量预估（粗略）

- 总体：约 5~9 个工作日（取决于 Fabric Java SDK 选型与 block events 落地复杂度）
- 关键不确定性：Java 侧 block events API 可用性与 TLS 细节调通时间

