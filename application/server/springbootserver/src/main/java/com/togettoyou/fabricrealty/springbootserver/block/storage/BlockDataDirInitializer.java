package com.togettoyou.fabricrealty.springbootserver.block.storage;

import com.togettoyou.fabricrealty.springbootserver.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class BlockDataDirInitializer {
    private static final Logger log = LoggerFactory.getLogger(BlockDataDirInitializer.class);

    private final AppProperties appProperties;

    public BlockDataDirInitializer(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void init() {
        try {
            Path blocksDir = Path.of(appProperties.dataDir(), "blocks");
            Files.createDirectories(blocksDir);
            log.info("Block storage dir ready: {}", blocksDir.toAbsolutePath());
        } catch (Exception e) {
            throw new IllegalStateException("初始化区块数据目录失败: " + e.getMessage(), e);
        }
    }
}

