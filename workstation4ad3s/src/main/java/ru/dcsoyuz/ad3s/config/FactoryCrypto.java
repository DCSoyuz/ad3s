package ru.dcsoyuz.ad3s.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Validates the factory activation key supplied via the {@code --factory=<key>}
 * command-line argument. The key is checked against a stored SHA-256 hash, so the
 * plaintext key never appears in source.
 *
 * NOTE: this no longer encrypts/decrypts anything. Factory register descriptions
 * live as plaintext in the git-ignored factory source set (src/factory/java),
 * which is never published, so on-disk encryption was redundant for an
 * internal-only build and has been removed.
 */
public class FactoryCrypto {

    private static final String KEY_HASH = "3d7a4759b743cf6fee2059eddb5febf4c822be9f6abc93f6d103fb0f30f6558a";

    public static boolean isValidKey(String input) {
        if (input == null || input.isEmpty()) return false;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return KEY_HASH.equals(sb.toString());
        } catch (Exception e) {
            return false;
        }
    }
}
