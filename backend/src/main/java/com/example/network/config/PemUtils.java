package com.example.network.config;

import java.io.BufferedReader;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.stream.Collectors;

public class PemUtils {

    public static PrivateKey parsePrivateKeyFromPem(String pem) throws Exception {
        String base64 = stripPemHeader(pem, "PRIVATE KEY");
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    public static PublicKey parsePublicKeyFromPem(String pem) throws Exception {
        String base64 = stripPemHeader(pem, "PUBLIC KEY");
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private static String stripPemHeader(String pem, String keyType) throws Exception {
        try (BufferedReader reader = new BufferedReader(new StringReader(pem))) {
            return reader.lines()
                    .filter(line -> !line.contains("BEGIN " + keyType))
                    .filter(line -> !line.contains("END " + keyType))
                    .collect(Collectors.joining());
        }
    }
}