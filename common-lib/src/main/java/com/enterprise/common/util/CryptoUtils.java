package com.enterprise.common.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Small, self-contained crypto helpers:
 * <ul>
 *   <li>AES-GCM symmetric encryption / decryption with random IVs</li>
 *   <li>HMAC-SHA256 signing</li>
 *   <li>URL-safe Base64 encoding / decoding</li>
 * </ul>
 *
 * <p>All methods use {@link java.security.SecureRandom}; keys must be
 * supplied by the caller — this class is deliberately not Spring-managed so
 * it can be reused from plain JVM processes too.</p>
 */
public final class CryptoUtils {

    private static final String AES = "AES";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;
    private static final SecureRandom RNG = new SecureRandom();

    private CryptoUtils() {}

    // ---- AES-GCM ----

    /**
     * Build a {@link SecretKey} from a 16/24/32-byte seed.
     *
     * @throws IllegalArgumentException if the key length is unsupported
     */
    public static SecretKey aesKey(byte[] seed) {
        if (seed == null || !(seed.length == 16 || seed.length == 24 || seed.length == 32)) {
            throw new IllegalArgumentException("AES key must be 16, 24 or 32 bytes");
        }
        return new SecretKeySpec(seed, AES);
    }

    /**
     * AES-GCM encrypt {@code plaintext}. The first 12 bytes of the returned
     * array are the random IV; the remainder is ciphertext + GCM tag.
     */
    public static byte[] encryptAesGcm(byte[] plaintext, SecretKey key) {
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            RNG.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext);
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM encrypt failed", e);
        }
    }

    /** Inverse of {@link #encryptAesGcm(byte[], SecretKey)}. */
    public static byte[] decryptAesGcm(byte[] input, SecretKey key) {
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            System.arraycopy(input, 0, iv, 0, GCM_IV_BYTES);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return cipher.doFinal(input, GCM_IV_BYTES, input.length - GCM_IV_BYTES);
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM decrypt failed", e);
        }
    }

    // ---- HMAC ----

    /** HMAC-SHA256 signature, returned as a lower-case hex string. */
    public static String hmacSha256Hex(String message, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 failed", e);
        }
    }

    // ---- Base64 ----

    public static String b64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static byte[] b64UrlDecode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }
}
