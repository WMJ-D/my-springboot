package com.example.test.security;

import org.bouncycastle.crypto.generators.SCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 密码哈希工具，与 Express 侧 utils/password.js 完全兼容：
 * 格式 scrypt$&lt;salt_hex&gt;$&lt;hash_hex&gt;，
 * Node 的 crypto.scrypt 默认参数 N=16384, r=8, p=1，派生长度 64 字节
 */
public final class PasswordUtil {

    private static final int N = 16384;
    private static final int R = 8;
    private static final int P = 1;
    private static final int KEY_LENGTH = 64;
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        byte[] derived = scrypt(password.getBytes(StandardCharsets.UTF_8), salt, KEY_LENGTH);
        return "scrypt$" + HexFormat.of().formatHex(salt) + "$" + HexFormat.of().formatHex(derived);
    }

    public static boolean verify(String password, String encoded) {
        if (password == null || encoded == null) {
            return false;
        }
        String[] parts = encoded.split("\\$");
        if (parts.length != 3 || !"scrypt".equals(parts[0]) || parts[1].isEmpty() || parts[2].isEmpty()) {
            return false;
        }
        byte[] salt;
        byte[] expected;
        try {
            salt = HexFormat.of().parseHex(parts[1]);
            expected = HexFormat.of().parseHex(parts[2]);
        } catch (IllegalArgumentException error) {
            return false;
        }
        byte[] actual = scrypt(password.getBytes(StandardCharsets.UTF_8), salt, expected.length);
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] scrypt(byte[] password, byte[] salt, int keyLength) {
        try {
            return SCrypt.generate(password, salt, N, R, P, keyLength);
        } catch (IllegalArgumentException | IllegalStateException error) {
            throw new IllegalStateException("密码哈希计算失败", error);
        }
    }

    static {
        // 注册 BouncyCastle 提供者（SCrypt.generate 为静态工具，不依赖注册，但保留以防后续使用其算法）
        java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }
}
