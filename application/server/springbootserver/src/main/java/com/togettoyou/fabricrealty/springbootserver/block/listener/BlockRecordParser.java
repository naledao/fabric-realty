package com.togettoyou.fabricrealty.springbootserver.block.listener;

import com.togettoyou.fabricrealty.springbootserver.block.model.BlockRecord;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public final class BlockRecordParser {
    private BlockRecordParser() {
    }

    public static BlockRecord fromEvent(Object event) {
        Object block = extractBlock(event);
        Object header = invoke(block, "getHeader");
        long blockNum = asLong(invoke(header, "getNumber"));

        byte[] previousHash = asBytes(invoke(header, "getPreviousHash"));
        byte[] dataHash = asBytes(invoke(header, "getDataHash"));

        int txCount = extractTxCount(block);

        String blockHash = BlockHashUtils.sha256BlockHeaderHash(blockNum, previousHash, dataHash);
        String dataHashHex = BlockHashUtils.toHex(dataHash);
        String prevHashHex = BlockHashUtils.toHex(previousHash);

        return new BlockRecord(blockNum, blockHash, dataHashHex, prevHashHex, txCount, OffsetDateTime.now(ZoneOffset.UTC));
    }

    private static Object extractBlock(Object event) {
        Object block = tryInvoke(event, "getBlock");
        return block != null ? block : event;
    }

    private static int extractTxCount(Object block) {
        Object data = tryInvoke(block, "getData");
        if (data == null) {
            return 0;
        }
        Object count = tryInvoke(data, "getDataCount");
        if (count instanceof Number number) {
            return number.intValue();
        }
        Object list = tryInvoke(data, "getDataList");
        if (list instanceof List<?> l) {
            return l.size();
        }
        return 0;
    }

    private static long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof java.math.BigInteger bi) {
            return bi.longValue();
        }
        if (value instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static byte[] asBytes(Object value) {
        if (value == null) {
            return new byte[0];
        }
        if (value instanceof byte[] bytes) {
            return bytes;
        }
        Method toByteArray = findMethod(value.getClass(), "toByteArray");
        if (toByteArray != null) {
            try {
                Object result = toByteArray.invoke(value);
                if (result instanceof byte[] bytes) {
                    return bytes;
                }
            } catch (Exception ignored) {
            }
        }
        return new byte[0];
    }

    private static Object invoke(Object target, String name) {
        Method method = findMethod(target.getClass(), name);
        if (method == null) {
            throw new IllegalStateException("无法读取字段: " + target.getClass().getName() + "." + name);
        }
        try {
            return method.invoke(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object tryInvoke(Object target, String name) {
        Method method = findMethod(target.getClass(), name);
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}

