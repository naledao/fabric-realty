package com.togettoyou.fabricrealty.springbootserver.block.listener;

import com.togettoyou.fabricrealty.springbootserver.block.model.BlockRecord;
import com.togettoyou.fabricrealty.springbootserver.block.storage.BlockStorage;
import com.togettoyou.fabricrealty.springbootserver.config.AppProperties;
import com.togettoyou.fabricrealty.springbootserver.config.FabricProperties;
import com.togettoyou.fabricrealty.springbootserver.fabric.FabricErrorTranslator;
import com.togettoyou.fabricrealty.springbootserver.fabric.FabricNetworkProvider;
import org.hyperledger.fabric.client.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class BlockListenerManager {
    private static final Logger log = LoggerFactory.getLogger(BlockListenerManager.class);

    private final FabricProperties fabricProperties;
    private final FabricNetworkProvider networkProvider;
    private final BlockStorage blockStorage;
    private final Duration retryInterval;

    private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread t = new Thread(runnable);
        t.setName("block-listener-" + t.getId());
        t.setDaemon(true);
        return t;
    });

    private final Map<String, Future<?>> tasks = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    public BlockListenerManager(
            FabricProperties fabricProperties,
            FabricNetworkProvider networkProvider,
            BlockStorage blockStorage,
            AppProperties appProperties
    ) {
        this.fabricProperties = fabricProperties;
        this.networkProvider = networkProvider;
        this.blockStorage = blockStorage;
        int seconds = Math.max(1, appProperties.blockRetrySeconds());
        this.retryInterval = Duration.ofSeconds(seconds);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        if (fabricProperties.organizations() == null || fabricProperties.organizations().isEmpty()) {
            log.warn("fabric.organizations 为空，跳过区块监听启动");
            return;
        }

        for (String orgName : fabricProperties.organizations().keySet()) {
            tasks.put(orgName, executor.submit(() -> listenForever(orgName)));
        }
        log.info("Block listener started for orgs={}", fabricProperties.organizations().keySet());
    }

    private void listenForever(String orgName) {
        while (running.get()) {
            OptionalLong latest = blockStorage.getLatestBlockNum(orgName);
            long startBlock = latest.isPresent() ? latest.getAsLong() + 1 : 0;

            Network network;
            try {
                network = networkProvider.getNetwork(orgName);
            } catch (Exception e) {
                log.error("Block listener init failed: org={}, error={}", orgName, e.getMessage());
                sleepQuietly(retryInterval);
                continue;
            }

            log.info("Block listener connecting: org={}, startBlock={}", orgName, startBlock);

            try (BlockEventStream stream = BlockEventsAccessor.open(network, startBlock)) {
                var iterator = stream.iterator();
                while (running.get() && iterator.hasNext()) {
                    Object event = iterator.next();
                    BlockRecord record = BlockRecordParser.fromEvent(event);
                    blockStorage.saveBlock(orgName, record);
                    log.debug("Saved block: org={}, blockNum={}", orgName, record.blockNum());
                }
                if (running.get()) {
                    log.warn("Block events stream ended: org={}, will retry after {}s", orgName, retryInterval.toSeconds());
                }
            } catch (Exception e) {
                String msg = FabricErrorTranslator.toUserMessage(e);
                log.warn("Block listener error: org={}, startBlock={}, error={}", orgName, startBlock, msg);
            }

            sleepQuietly(retryInterval);
        }
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        for (Future<?> task : tasks.values()) {
            task.cancel(true);
        }
        tasks.clear();
        executor.shutdownNow();
    }

    private static void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}

