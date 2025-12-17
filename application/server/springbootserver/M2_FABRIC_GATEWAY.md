# M2：Fabric Gateway Java 接入说明

本阶段目标：在 Spring Boot 工程中完成 Fabric Gateway Java SDK 接入，支持多组织身份连接、合约缓存，并提供 evaluate/submit 封装与错误翻译，使至少 1 条读/写接口可真实调用链码。

## 1. 关键实现文件

- 多组织连接 + 合约缓存：`src/main/java/com/togettoyou/fabricrealty/springbootserver/fabric/FabricGatewayClient.java`
- evaluate/submit 抽象：`src/main/java/com/togettoyou/fabricrealty/springbootserver/fabric/FabricClient.java`
- 错误翻译（尽量对齐 Go 的 “错误码: Xxx, 消息: ...”）：`src/main/java/com/togettoyou/fabricrealty/springbootserver/fabric/FabricErrorTranslator.java`

## 2. 依赖版本

- Fabric Gateway Java SDK：`org.hyperledger.fabric:fabric-gateway:1.7.0`（见 `pom.xml`）
- 目标链路：对齐仓库现有 Fabric `v2.5.10`（Go 侧 `fabric-gateway v1.7.0`）

## 3. 连接模型（多组织）

启动时按 `fabric.organizations` 遍历，为每个组织创建：

- gRPC TLS Channel（使用 `tlsCertPath` 作为 trust root，并设置 `overrideAuthority=gatewayPeer`）
- Identity（读取 `certPath` 目录下第一个证书文件）
- Signer（读取 `keyPath` 目录下第一个私钥文件）
- Gateway/Network/Contract，并缓存 `Contract` 供业务层调用

超时策略（对齐 Go 配置）：

- Evaluate：5s
- Endorse：15s
- Submit：5s
- CommitStatus：1m

> 若 Fabric 不可用/配置不正确，应用启动会失败（与 Go 版本一致：Fabric 初始化失败即退出）。

## 4. 配置（application.yml）

配置位置：`src/main/resources/application.yml`

```yaml
fabric:
  channelName: mychannel
  chaincodeName: mychaincode
  organizations:
    org1:
      mspID: Org1MSP
      certPath: ...
      keyPath: ...
      tlsCertPath: ...
      peerEndpoint: localhost:7051
      gatewayPeer: peer0.org1.togettoyou.com
```

说明：

- `certPath`/`keyPath` 为“目录”，会读取目录下第一个文件（与 Go 逻辑一致）
- `gatewayPeer` 用于 TLS serverName/authority，需与 peer TLS 证书 SAN 匹配

## 5. 已接入链码调用的接口（最小跑通）

- 写：`POST /api/realty-agency/realty/create` -> `org1` `CreateRealEstate(...)`
- 读：`GET  /api/realty-agency/realty/{id}` -> `org1` `QueryRealEstate(id)`

同时也已将以下接口接入链码（便于后续 M3 直接联调）：

- 房产列表：`GET /api/realty-agency/realty/list` -> `QueryRealEstateList(...)`
- 交易平台：创建/查询交易、查询房产、交易列表
- 银行：完成交易、查询交易、交易列表

区块监听与区块列表在 M4 落地（见 `M4_BLOCK_LISTENER.md`）。

## 6. 联调建议

1. 启动 Fabric 网络（见仓库根目录 `dev.md` 与 `network/install.sh`）
2. 启动 Spring Boot：`mvn spring-boot:run`
3. 用 `M0_EXAMPLES.http` 依次调用创建房产与查询房产验证链路
