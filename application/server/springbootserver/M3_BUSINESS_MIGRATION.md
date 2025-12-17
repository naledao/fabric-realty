# M3：业务接口全量迁移（链码调用对齐）

本阶段目标：完成 3 组业务接口（不动产登记机构/交易平台/银行）的 Controller + DTO + Service 实现，路径/入参/响应结构与 Go 版本保持一致；分页查询结构与链码返回保持一致。

> 说明：区块监听与 `/block/list` 在 M4 落地（见 `M4_BLOCK_LISTENER.md`）。

## 1. 接口与组织身份映射（必须保持一致）

- 不动产登记机构：`org1`
- 交易平台：`org3`
- 银行：`org2`

## 2. 已实现为真实链码调用的接口

### 2.1 不动产登记机构（org1）

- `POST /api/realty-agency/realty/create` -> `CreateRealEstate(id, address, area, owner, createTimeRFC3339)`
- `GET  /api/realty-agency/realty/{id}` -> `QueryRealEstate(id)`
- `GET  /api/realty-agency/realty/list?pageSize&bookmark&status` -> `QueryRealEstateList(pageSize, bookmark, status)`

### 2.2 交易平台（org3）

- `POST /api/trading-platform/transaction/create` -> `CreateTransaction(txId, realEstateId, seller, buyer, price, createTimeRFC3339)`
- `GET  /api/trading-platform/realty/{id}` -> `QueryRealEstate(id)`
- `GET  /api/trading-platform/transaction/{txId}` -> `QueryTransaction(txId)`
- `GET  /api/trading-platform/transaction/list?pageSize&bookmark&status` -> `QueryTransactionList(pageSize, bookmark, status)`

### 2.3 银行（org2）

- `POST /api/bank/transaction/complete/{txId}` -> `CompleteTransaction(txId, updateTimeRFC3339)`
- `GET  /api/bank/transaction/{txId}` -> `QueryTransaction(txId)`
- `GET  /api/bank/transaction/list?pageSize&bookmark&status` -> `QueryTransactionList(pageSize, bookmark, status)`

## 3. 分页查询返回结构（必须保持字段名一致）

链码分页查询 `data` 字段结构为：

```json
{
  "records": [],
  "recordsCount": 0,
  "bookmark": "",
  "fetchedRecordsCount": 0
}
```

字段名必须保持：`records / recordsCount / bookmark / fetchedRecordsCount`。

## 4. 联调/验收步骤（前端全流程）

1. 启动 Fabric 网络（参考仓库根目录 `dev.md` / `network/install.sh`）
2. 启动 Spring Boot（需要 Maven）：在 `application/server/springbootserver` 执行 `mvn spring-boot:run`
3. 按以下顺序调用接口（可直接用 `M0_EXAMPLES.http`）：
   - 创建房产（org1）
   - 交易平台创建交易（org3）
   - 银行完成交易（org2）
   - 查询房产/交易/列表确认状态变化

## 5. 相关代码位置

- Controller：`src/main/java/com/togettoyou/fabricrealty/springbootserver/api/`
- Service：`src/main/java/com/togettoyou/fabricrealty/springbootserver/service/`
- Fabric Gateway：`src/main/java/com/togettoyou/fabricrealty/springbootserver/fabric/`
