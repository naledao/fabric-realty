package com.togettoyou.fabricrealty.springbootserver.block.storage;

import com.togettoyou.fabricrealty.springbootserver.api.dto.BlockDataDto;
import com.togettoyou.fabricrealty.springbootserver.api.dto.BlockQueryResultDto;
import com.togettoyou.fabricrealty.springbootserver.block.model.BlockRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.OptionalLong;

@Repository
public class JdbcBlockStorage implements BlockStorage {
    private final JdbcTemplate jdbcTemplate;

    public JdbcBlockStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public OptionalLong getLatestBlockNum(String orgName) {
        List<Long> rows = jdbcTemplate.query(
                "SELECT block_num FROM latest_blocks WHERE org_name = ?",
                (rs, rowNum) -> rs.getLong("block_num"),
                orgName
        );
        if (rows.isEmpty()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(rows.get(0));
    }

    @Override
    @Transactional
    public void saveBlock(String orgName, BlockRecord block) {
        jdbcTemplate.update(
                "MERGE INTO blocks (org_name, block_num, block_hash, data_hash, prev_hash, tx_count, save_time) " +
                        "KEY (org_name, block_num) VALUES (?, ?, ?, ?, ?, ?, ?)",
                orgName,
                block.blockNum(),
                block.blockHash(),
                block.dataHash(),
                block.prevHash(),
                block.txCount(),
                block.saveTime()
        );

        jdbcTemplate.update(
                "MERGE INTO latest_blocks (org_name, block_num, save_time) KEY (org_name) VALUES (?, ?, ?)",
                orgName,
                block.blockNum(),
                block.saveTime()
        );
    }

    @Override
    public BlockQueryResultDto queryBlocks(String orgName, int pageSize, int pageNum) {
        int normalizedPageSize = pageSize > 0 ? pageSize : 10;
        int normalizedPageNum = pageNum > 0 ? pageNum : 1;

        long latestBlockNum = getLatestBlockNum(orgName)
                .orElseThrow(() -> new BlockStorageException("组织数据不存在"));

        long totalLong = latestBlockNum + 1;
        int total = totalLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalLong;

        long startIdx = totalLong - (long) normalizedPageNum * normalizedPageSize;
        long endIdx = startIdx + normalizedPageSize;
        if (startIdx < 0) {
            startIdx = 0;
        }
        if (endIdx > totalLong) {
            endIdx = totalLong;
        }

        boolean hasMore = startIdx > 0;

        List<BlockDataDto> blocks = jdbcTemplate.query(
                "SELECT block_num, block_hash, data_hash, prev_hash, tx_count, save_time " +
                        "FROM blocks " +
                        "WHERE org_name = ? AND block_num >= ? AND block_num < ? " +
                        "ORDER BY block_num DESC",
                (rs, rowNum) -> new BlockDataDto(
                        rs.getLong("block_num"),
                        rs.getString("block_hash"),
                        rs.getString("data_hash"),
                        rs.getString("prev_hash"),
                        rs.getInt("tx_count"),
                        readOffsetDateTimeUtc(rs.getObject("save_time"))
                ),
                orgName,
                startIdx,
                endIdx
        );

        return new BlockQueryResultDto(blocks, total, normalizedPageSize, normalizedPageNum, hasMore);
    }

    private static OffsetDateTime readOffsetDateTimeUtc(Object raw) {
        if (raw == null) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
        if (raw instanceof OffsetDateTime odt) {
            return odt.withOffsetSameInstant(ZoneOffset.UTC);
        }
        if (raw instanceof java.sql.Timestamp ts) {
            return ts.toInstant().atOffset(ZoneOffset.UTC);
        }
        if (raw instanceof java.time.LocalDateTime ldt) {
            return ldt.atOffset(ZoneOffset.UTC);
        }
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
