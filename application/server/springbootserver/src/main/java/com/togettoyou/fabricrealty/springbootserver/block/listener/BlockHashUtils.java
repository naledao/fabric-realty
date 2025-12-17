package com.togettoyou.fabricrealty.springbootserver.block.listener;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class BlockHashUtils {
    private static final HexFormat HEX = HexFormat.of().withLowerCase();

    private BlockHashUtils() {
    }

    public static String sha256BlockHeaderHash(long blockNum, byte[] previousHash, byte[] dataHash) {
        byte[] header = encodeHeader(blockNum, previousHash == null ? new byte[0] : previousHash, dataHash == null ? new byte[0] : dataHash);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HEX.formatHex(digest.digest(header));
        } catch (Exception e) {
            throw new IllegalStateException("计算区块哈希失败: " + e.getMessage(), e);
        }
    }

    public static String toHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        return HEX.formatHex(bytes);
    }

    private static byte[] encodeHeader(long blockNum, byte[] previousHash, byte[] dataHash) {
        byte[] integer = encodeInteger(BigInteger.valueOf(blockNum));
        byte[] prev = encodeOctetString(previousHash);
        byte[] data = encodeOctetString(dataHash);

        int length = integer.length + prev.length + data.length;
        byte[] seqLen = encodeLength(length);

        byte[] result = new byte[1 + seqLen.length + length];
        int offset = 0;
        result[offset++] = 0x30;
        System.arraycopy(seqLen, 0, result, offset, seqLen.length);
        offset += seqLen.length;
        System.arraycopy(integer, 0, result, offset, integer.length);
        offset += integer.length;
        System.arraycopy(prev, 0, result, offset, prev.length);
        offset += prev.length;
        System.arraycopy(data, 0, result, offset, data.length);
        return result;
    }

    private static byte[] encodeInteger(BigInteger value) {
        byte[] bytes = value.toByteArray();
        byte[] len = encodeLength(bytes.length);

        byte[] result = new byte[1 + len.length + bytes.length];
        int offset = 0;
        result[offset++] = 0x02;
        System.arraycopy(len, 0, result, offset, len.length);
        offset += len.length;
        System.arraycopy(bytes, 0, result, offset, bytes.length);
        return result;
    }

    private static byte[] encodeOctetString(byte[] bytes) {
        byte[] len = encodeLength(bytes.length);
        byte[] result = new byte[1 + len.length + bytes.length];
        int offset = 0;
        result[offset++] = 0x04;
        System.arraycopy(len, 0, result, offset, len.length);
        offset += len.length;
        System.arraycopy(bytes, 0, result, offset, bytes.length);
        return result;
    }

    private static byte[] encodeLength(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length < 0");
        }
        if (length < 128) {
            return new byte[]{(byte) length};
        }
        int temp = length;
        int numBytes = 0;
        while (temp > 0) {
            numBytes++;
            temp >>= 8;
        }
        byte[] result = new byte[1 + numBytes];
        result[0] = (byte) (0x80 | numBytes);
        for (int i = numBytes; i > 0; i--) {
            result[i] = (byte) (length & 0xFF);
            length >>= 8;
        }
        return result;
    }
}

