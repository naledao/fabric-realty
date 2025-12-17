package com.togettoyou.fabricrealty.springbootserver.block.listener;

import java.util.Iterator;

public final class BlockEventStream implements AutoCloseable {
    private final Iterator<?> iterator;
    private final AutoCloseable closeable;

    public BlockEventStream(Iterator<?> iterator, AutoCloseable closeable) {
        this.iterator = iterator;
        this.closeable = closeable;
    }

    public Iterator<?> iterator() {
        return iterator;
    }

    @Override
    public void close() {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}

