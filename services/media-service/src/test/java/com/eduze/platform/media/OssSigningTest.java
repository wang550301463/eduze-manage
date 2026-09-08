package com.eduze.platform.media;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OssSigningTest {
    @Test
    void explicitCredentialsSignPrivateUrlsWithoutIniProfileProvider() {
        var store =
                new OssObjectStore(
                        "https://oss-cn-hangzhou.aliyuncs.com",
                        "test-access-id",
                        UUID.randomUUID().toString(),
                        "eduze-test");
        try {
            Instant expiry = Instant.now().plusSeconds(300);
            var upload = store.upload("id", "staging/test.png", "image/png", expiry);
            assertEquals("PUT", upload.method());
            assertEquals("image/png", upload.headers().get("Content-Type"));
            URI signed = URI.create(store.readUrl("id", "sealed/test.png", expiry, true));
            assertEquals("https", signed.getScheme());
            assertTrue(signed.getQuery().contains("Expires=" + expiry.getEpochSecond()));
            assertTrue(signed.getQuery().contains("Signature="));
            assertTrue(signed.getQuery().contains("x-oss-process="));
        } finally {
            store.close();
        }
    }
}
