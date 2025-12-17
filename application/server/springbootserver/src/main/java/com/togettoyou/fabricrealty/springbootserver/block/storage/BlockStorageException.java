package com.togettoyou.fabricrealty.springbootserver.block.storage;

public class BlockStorageException extends RuntimeException {
    public BlockStorageException(String message) {
        super(message);
    }

    public BlockStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

