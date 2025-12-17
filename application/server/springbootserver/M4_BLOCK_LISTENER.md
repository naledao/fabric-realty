# M4：区块监听与区块列表（落盘 + 分页查询）

本阶段目标：

- 每组织独立监听 block events
- 支持断点续听（从 `latestBlockNum + 1` 开始）
- 监听中断自动重连（默认 30s 重试，配置项 `app.blockRetrySeconds`）
- 区块数据落盘持久化
- `/api/*/block/list` 支持分页倒序查询（字段名对齐 Go 版本）

## 1. 存储方案（H2 file）

使用 H2 文件库 + Spring JDBC（落地快）：

- DB 文件：`${app.dataDir}/blocks/blocks`（实际生成如 `blocks.mv.db`）
- 建表脚本：`src/main/resources/schema.sql`
- 启动时确保目录存在：`src/main/java/com/togettoyou/fabricrealty/springbootserver/block/storage/BlockDataDirInitializer.java`

表结构：

- `blocks(org_name, block_num, block_hash, data_hash, prev_hash, tx_count, save_time)`
- `latest_blocks(org_name, block_num, save_time)`

## 2. 区块监听（多组织）

实现位置：

- `src/main/java/com/togettoyou/fabricrealty/springbootserver/block/listener/BlockListenerManager.java`

行为：

- 应用就绪后为每个 `fabric.organizations.*` 启动一个后台监听线程
- 读取 `latest_blocks` 作为断点；无记录则从 0 开始
- 监听中断/异常后按 `app.blockRetrySeconds` 重试

> 事件订阅使用 Fabric Gateway Java SDK 的 block events API；为了兼容不同 SDK 版本，内部通过反射尝试 `getBlockEvents(startBlock)` / `newBlockEventsRequest()`。

## 3. 区块 hash 计算（对齐 Go）

对齐 Go 的算法：

- ASN.1(DER) 编码 `SEQUENCE{ INTEGER(number), OCTET STRING(previousHash), OCTET STRING(dataHash) }`
- 对编码结果做 `sha256`
- 输出小写 hex

实现：`src/main/java/com/togettoyou/fabricrealty/springbootserver/block/listener/BlockHashUtils.java`

## 4. `/block/list` 分页查询（对齐 Go）

存储查询实现：

- `src/main/java/com/togettoyou/fabricrealty/springbootserver/block/storage/JdbcBlockStorage.java`

返回结构对齐 Go：

```json
{
  "blocks": [],
  "total": 1,
  "page_size": 10,
  "page_num": 1,
  "has_more": false
}
```

字段名保持：

- 顶层：`blocks / total / page_size / page_num / has_more`
- block：`block_num / block_hash / data_hash / prev_hash / tx_count / save_time`

## 5. 验收建议

1. 启动 Fabric 网络与本服务
2. 等待日志出现区块保存（或手动触发交易产生新区块）
3. 调用任一组织接口验证分页：
   - `GET /api/realty-agency/block/list?pageSize=10&pageNum=1`
   - `GET /api/trading-platform/block/list?pageSize=10&pageNum=1`
   - `GET /api/bank/block/list?pageSize=10&pageNum=1`

