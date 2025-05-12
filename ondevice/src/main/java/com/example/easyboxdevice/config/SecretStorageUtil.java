package com.example.easyboxdevice.config;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public class SecretStorageUtil {
    private static final String FILE = "device-secret.enc";
    private static final String ENCRYPTION_PASSWORD = "strong-password";

    public static void storeSecret(String secret) throws Exception {
        SecretKeySpec key = getKey();
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(secret.getBytes("UTF-8"));
        Files.write(getFilePath(), Base64.getEncoder().encode(encrypted));
    }

    public static String loadSecret() throws Exception {
        SecretKeySpec key = getKey();
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] encrypted = Base64.getDecoder().decode(Files.readAllBytes(getFilePath()));
        return new String(cipher.doFinal(encrypted), "UTF-8");
    }

    public static boolean exists() {
        return Files.exists(getFilePath());
    }

    public static void deleteSecret() throws Exception {
        Path path = getFilePath();
        if (Files.exists(path)) {
            Files.delete(path);
            System.out.println("🗑️ Deleted stored secret: " + path.toAbsolutePath());
        }
    }

    private static Path getFilePath() {
        return Paths.get(FILE);
    }

    private static SecretKeySpec getKey() throws Exception {
        byte[] key = ENCRYPTION_PASSWORD.getBytes("UTF-8");
        key = MessageDigest.getInstance("SHA-256").digest(key);
        return new SecretKeySpec(Arrays.copyOf(key, 16), "AES");
    }
}