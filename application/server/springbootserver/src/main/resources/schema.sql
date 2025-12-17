CREATE TABLE IF NOT EXISTS blocks (
    org_name VARCHAR(64) NOT NULL,
    block_num BIGINT NOT NULL,
    block_hash VARCHAR(128) NOT NULL,
    data_hash VARCHAR(128) NOT NULL,
    prev_hash VARCHAR(128) NOT NULL,
    tx_count INT NOT NULL,
    save_time TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (org_name, block_num)
);

CREATE INDEX IF NOT EXISTS idx_blocks_org_blocknum ON blocks (org_name, block_num);

CREATE TABLE IF NOT EXISTS latest_blocks (
    org_name VARCHAR(64) PRIMARY KEY,
    block_num BIGINT NOT NULL,
    save_time TIMESTAMP WITH TIME ZONE NOT NULL
);
