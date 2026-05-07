package uk.gov.hmcts.payment.api.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class HmacUtil {

    public static String hmacSha256(String key, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey =
            new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        mac.init(secretKey);
        byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

        return bytesToHex(rawHmac);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    public static void main(String[] args) throws Exception {
        String key = "my-secret-key";
        String message = "AAAAA";

        String hash = hmacSha256(key, message);
        System.out.println(hash);
    }
}

