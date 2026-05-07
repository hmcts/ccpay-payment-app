package uk.gov.hmcts.payment.api.util;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HmacUtilTest {

    private static String computeExpected(String key, String message) throws Exception {
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

    @Test
    void hmacMatchesStandardMacImplementation() throws Exception {
        String key = "my-secret-key";
        String message = "AAAAA";

        String expected = computeExpected(key, message);
        String actual = HmacUtil.hmacSha256(key, message);

        assertEquals(expected, actual);
    }

    @Test
    void emptyMessageProducesValidHash() throws Exception {
        String key = "secret";
        String message = "";

        String hash = HmacUtil.hmacSha256(key, message);

        assertNotNull(hash);
        assertTrue(hash.matches("^[0-9a-f]{64}$"));
        assertEquals(computeExpected(key, message), hash);
    }

    @Test
    void nullKeyThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> HmacUtil.hmacSha256(null, "msg"));
    }

    @Test
    void nullMessageThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> HmacUtil.hmacSha256("key", null));
    }

    @Test
    void hexIsLowercaseAndCorrectLength() throws Exception {
        String hash = HmacUtil.hmacSha256("k", "m");
        assertEquals(64, hash.length());
        assertTrue(hash.matches("^[0-9a-f]{64}$"));
    }

    @Test
    void concurrentCallsProduceSameResult() throws Exception {
        String key = "concurrent-key";
        String message = "concurrent-message";
        String expected = computeExpected(key, message);

        int threads = 10;
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(exec.submit(() -> HmacUtil.hmacSha256(key, message)));
        }

        for (Future<String> f : futures) {
            assertEquals(expected, f.get(5, TimeUnit.SECONDS));
        }

        exec.shutdownNow();
    }
}
