package com.togettoyou.fabricrealty.springbootserver.block.storage;

import com.togettoyou.fabricrealty.springbootserver.api.dto.BlockQueryResultDto;
import com.togettoyou.fabricrealty.springbootserver.block.model.BlockRecord;

import java.util.OptionalLong;

public interface BlockStorage {
    OptionalLong getLatestBlockNum(String orgName);

    void saveBlock(String orgName, BlockRecord block);

    BlockQueryResultDto queryBlocks(String orgName, int pageSize, int pageNum);
}

