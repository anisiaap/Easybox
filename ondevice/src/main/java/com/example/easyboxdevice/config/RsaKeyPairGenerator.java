//package com.example.easyboxdevice.config;
//
//import java.io.FileWriter;
//import java.security.KeyPair;
//import java.security.KeyPairGenerator;
//import java.security.PrivateKey;
//import java.security.PublicKey;
//import java.util.Base64;
//
//public class RsaKeyPairGenerator {
//    public static void main(String[] args) throws Exception {
//        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
//        generator.initialize(2048); // strong enough for JWT
//
//        KeyPair pair = generator.generateKeyPair();
//        PrivateKey privateKey = pair.getPrivate();
//        PublicKey publicKey = pair.getPublic();
//
//        String privatePem = encodeToPem("PRIVATE KEY", privateKey.getEncoded());
//        String publicPem = encodeToPem("PUBLIC KEY", publicKey.getEncoded());
//
//        try (FileWriter privOut = new FileWriter("device-private.pem");
//             FileWriter pubOut = new FileWriter("device-public.pem")) {
//            privOut.write(privatePem);
//            pubOut.write(publicPem);
//        }
//
//        System.out.println("✅ Keys written to device-private.pem and device-public.pem");
//    }
//
//    private static String encodeToPem(String type, byte[] data) {
//        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(data);
//        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
//    }
//}