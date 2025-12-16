# M0：接口/响应基线（以 Go 版本为准）

目标：为 Spring Boot 迁移提供“接口不改、结构不改”的对照基线；本文档从当前 `application/server`（Go + Gin）与链码数据结构整理而来。

## 1. 统一响应结构（前端依赖）

所有接口返回 JSON，统一结构：

```json
{ "code": 200, "message": "成功", "data": {} }
```

- 成功：HTTP `200`，`code=200`，`message` 为“成功”或业务成功提示，`data` 为返回数据
- 失败：HTTP `400/500`，`code` 与 HTTP 状态码一致，`message` 为错误原因，`data` 字段省略
- 注意：当 `data=nil` 时（例如创建成功但无返回数据），Go 版本会省略 `data` 字段（`omitempty`）

## 2. 组织身份与链码调用映射（必须保持一致）

后端固定使用组织身份发起链码调用（非前端传参决定）：

- 不动产登记机构：`org1`
- 银行：`org2`
- 交易平台：`org3`

链码函数（方法名/参数顺序）与后端对应关系：

- `POST /api/realty-agency/realty/create` -> `org1` Submit `CreateRealEstate(id, address, area, owner, createTimeRFC3339)`
- `GET  /api/realty-agency/realty/:id` -> `org1` Evaluate `QueryRealEstate(id)`
- `GET  /api/realty-agency/realty/list` -> `org1` Evaluate `QueryRealEstateList(pageSize, bookmark, status)`
- `POST /api/trading-platform/transaction/create` -> `org3` Submit `CreateTransaction(txId, realEstateId, seller, buyer, price, createTimeRFC3339)`
- `GET  /api/trading-platform/realty/:id` -> `org3` Evaluate `QueryRealEstate(id)`
- `GET  /api/trading-platform/transaction/:txId` -> `org3` Evaluate `QueryTransaction(txId)`
- `GET  /api/trading-platform/transaction/list` -> `org3` Evaluate `QueryTransactionList(pageSize, bookmark, status)`
- `POST /api/bank/transaction/complete/:txId` -> `org2` Submit `CompleteTransaction(txId, updateTimeRFC3339)`
- `GET  /api/bank/transaction/:txId` -> `org2` Evaluate `QueryTransaction(txId)`
- `GET  /api/bank/transaction/list` -> `org2` Evaluate `QueryTransactionList(pageSize, bookmark, status)`

区块列表接口不调用链码，来自后端本地持久化的区块监听数据（按组织隔离）：

- `GET /api/*/block/list` -> `blocks.db` 查询（按区块号倒序分页）

## 3. 数据结构基线（data 字段）

### 3.1 房产 RealEstate（链码返回）

```json
{
  "id": "RE001",
  "propertyAddress": "上海市浦东新区XX路1号",
  "area": 120.5,
  "currentOwner": "Alice",
  "status": "NORMAL",
  "createTime": "2025-12-16T12:00:00Z",
  "updateTime": "2025-12-16T12:00:00Z"
}
```

- `status` 枚举：`NORMAL` / `IN_TRANSACTION`
- `createTime/updateTime`：RFC3339（JSON `date-time`）

### 3.2 交易 Transaction（链码返回）

```json
{
  "id": "TX001",
  "realEstateId": "RE001",
  "seller": "Alice",
  "buyer": "Bob",
  "price": 1000000,
  "status": "PENDING",
  "createTime": "2025-12-16T12:05:00Z",
  "updateTime": "2025-12-16T12:05:00Z"
}
```

- `status` 枚举：`PENDING` / `COMPLETED`

### 3.3 链码分页查询结果 QueryResult（房产/交易列表共用结构）

```json
{
  "records": [],
  "recordsCount": 0,
  "bookmark": "",
  "fetchedRecordsCount": 0
}
```

> `records` 内元素为 `RealEstate` 或 `Transaction`；字段名必须保持：`records / recordsCount / bookmark / fetchedRecordsCount`。

### 3.4 区块列表 BlockQueryResult（后端本地数据）

```json
{
  "blocks": [
    {
      "block_num": 3,
      "block_hash": "abcd...",
      "data_hash": "abcd...",
      "prev_hash": "abcd...",
      "tx_count": 1,
      "save_time": "2025-12-16T12:10:00Z"
    }
  ],
  "total": 4,
  "page_size": 10,
  "page_num": 1,
  "has_more": false
}
```

字段名必须保持：`blocks / total / page_size / page_num / has_more`；区块字段名必须保持：`block_num / block_hash / data_hash / prev_hash / tx_count / save_time`。

## 4. 接口清单 + 示例请求/响应（按现有 Go 行为整理）

Base URL：`http://localhost:8888`，接口前缀：`/api`

### 4.1 不动产登记机构（org1）

#### 4.1.1 创建房产

- `POST /api/realty-agency/realty/create`
- Request Body（JSON）：

```json
{ "id": "RE001", "address": "上海市浦东新区XX路1号", "area": 120.5, "owner": "Alice" }
```

- Success（200）：

```json
{ "code": 200, "message": "房产信息创建成功" }
```

- BadRequest（400，JSON 解析失败时）：

```json
{ "code": 400, "message": "房产信息格式错误" }
```

#### 4.1.2 查询房产

- `GET /api/realty-agency/realty/:id`
- Success（200）：

```json
{ "code": 200, "message": "成功", "data": { "id": "RE001", "propertyAddress": "...", "area": 120.5, "currentOwner": "Alice", "status": "NORMAL", "createTime": "2025-12-16T12:00:00Z", "updateTime": "2025-12-16T12:00:00Z" } }
```

#### 4.1.3 分页查询房产列表（链码分页）

- `GET /api/realty-agency/realty/list?pageSize&bookmark&status`
- Query：
  - `pageSize`：默认 `10`（注意：Go 版本对非数字入参不做 400 校验，可能导致链码侧报错 -> 500）
  - `bookmark`：默认 `""`
  - `status`：默认 `""`；可选 `NORMAL` / `IN_TRANSACTION`
- Success（200）：

```json
{ "code": 200, "message": "成功", "data": { "records": [], "recordsCount": 0, "bookmark": "", "fetchedRecordsCount": 0 } }
```

#### 4.1.4 分页查询区块列表（后端本地）

- `GET /api/realty-agency/block/list?pageSize&pageNum`
- Query：
  - `pageSize`：默认 `10`（<=0 或非数字时会回退到 10）
  - `pageNum`：默认 `1`（<=0 或非数字时会回退到 1）
- Success（200）：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "blocks": [
      {
        "block_num": 0,
        "block_hash": "abcd...",
        "data_hash": "abcd...",
        "prev_hash": "",
        "tx_count": 0,
        "save_time": "2025-12-16T12:10:00Z"
      }
    ],
    "total": 1,
    "page_size": 10,
    "page_num": 1,
    "has_more": false
  }
}
```

### 4.2 交易平台（org3）

#### 4.2.1 生成交易

- `POST /api/trading-platform/transaction/create`
- Request Body（JSON）：

```json
{ "txId": "TX001", "realEstateId": "RE001", "seller": "Alice", "buyer": "Bob", "price": 1000000 }
```

- Success（200）：

```json
{ "code": 200, "message": "交易创建成功" }
```

- BadRequest（400，JSON 解析失败时）：

```json
{ "code": 400, "message": "交易信息格式错误" }
```

#### 4.2.2 查询房产

- `GET /api/trading-platform/realty/:id`
- Success（200）：同 `RealEstate`

#### 4.2.3 查询交易

- `GET /api/trading-platform/transaction/:txId`
- Success（200）：同 `Transaction`

#### 4.2.4 分页查询交易列表（链码分页）

- `GET /api/trading-platform/transaction/list?pageSize&bookmark&status`
- Query：
  - `pageSize`：默认 `10`
  - `bookmark`：默认 `""`
  - `status`：默认 `""`；可选 `PENDING` / `COMPLETED`
- Success（200）：`data` 为 `QueryResult`

#### 4.2.5 分页查询区块列表（后端本地）

- `GET /api/trading-platform/block/list?pageSize&pageNum`
- Success（200）：`data` 为 `BlockQueryResult`

### 4.3 银行（org2）

#### 4.3.1 完成交易

- `POST /api/bank/transaction/complete/:txId`
- Success（200）：

```json
{ "code": 200, "message": "交易完成" }
```

#### 4.3.2 查询交易

- `GET /api/bank/transaction/:txId`
- Success（200）：同 `Transaction`

#### 4.3.3 分页查询交易列表（链码分页）

- `GET /api/bank/transaction/list?pageSize&bookmark&status`
- Success（200）：`data` 为 `QueryResult`

#### 4.3.4 分页查询区块列表（后端本地）

- `GET /api/bank/block/list?pageSize&pageNum`
- Success（200）：`data` 为 `BlockQueryResult`

## 5. 备注：错误消息现状（用于对照）

- 500 类错误均走统一结构，但 `message` 可能包含链码/gRPC 细节，例如：`错误码: Unknown, 消息: ...`
- 个别接口的错误消息存在重复前缀（Go 版本 handler 与 service 都拼接了“xx失败：”），Spring Boot 若要“完全行为一致”需保留该现象
