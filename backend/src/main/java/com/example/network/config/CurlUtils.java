//package com.example.network.config;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//
//public class CurlUtils {
//
//    /**
//     * Executes a curl command to fetch JSON from Nominatim
//     *
//     * @param address the address you want to geocode
//     * @return the raw JSON response as a String
//     * @throws Exception if something goes wrong (process fails, etc.)
//     */
//    public static String getCoordinatesJsonViaCurl(String address) throws Exception {
//        // Build the EXACT same curl command that works in your terminal
//        String encodedAddress = java.net.URLEncoder.encode(address, java.nio.charset.StandardCharsets.UTF_8);
//        // Adjust user-agent to match your working terminal command
//        ProcessBuilder processBuilder = new ProcessBuilder(
//                "curl",
//                "-s", // silent
//                "-S", // show errors
//                "-H", "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.5481.100 Safari/537.36",
//                "-H", "Accept: application/json",
//                "-H", "Referer: https://www.openstreetmap.org",
//                "-H", "Accept-Language: en-US,en;q=0.9",
//                "-H", "Connection: keep-alive",
//                "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" + encodedAddress + "&countrycodes=ro"
//        );
//
//        processBuilder.redirectErrorStream(true);
//        Process process = processBuilder.start();
//
//        // Read output
//        StringBuilder output = new StringBuilder();
//        try (BufferedReader reader = new BufferedReader(
//                new InputStreamReader(process.getInputStream()))) {
//            String line;
//            while ((line = reader.readLine()) != null) {
//                output.append(line);
//            }
//        }
//
//        // Wait for curl to finish
//        int exitCode = process.waitFor();
//        if (exitCode != 0) {
//            throw new RuntimeException("curl process failed with exit code " + exitCode);
//        }
//
//        return output.toString();
//    }
//}
