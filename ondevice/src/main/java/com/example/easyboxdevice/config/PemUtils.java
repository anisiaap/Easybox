//package com.example.easyboxdevice.config;
//
//import java.io.StringReader;
//import java.security.KeyFactory;
//import java.security.PrivateKey;
//import java.security.PublicKey;
//import java.security.interfaces.RSAPublicKey;
//import java.security.spec.*;
//import java.util.Base64;
//import java.util.stream.Collectors;
//
//public class PemUtils {
//
//    public static RSAPublicKey parsePublicKeyFromPem(String pem) throws Exception {
//        String key = stripHeaders(pem);
//        byte[] decoded = Base64.getDecoder().decode(key);
//        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
//        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
//    }
//
//    public static PrivateKey parsePrivateKeyFromPem(String pem) throws Exception {
//        String key = stripHeaders(pem);
//        byte[] decoded = Base64.getDecoder().decode(key);
//        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
//        return KeyFactory.getInstance("RSA").generatePrivate(spec);
//    }
//
//    private static String stripHeaders(String pem) {
//        return pem.lines()
//                .filter(line -> !line.startsWith("-----"))
//                .collect(Collectors.joining());
//    }
//}