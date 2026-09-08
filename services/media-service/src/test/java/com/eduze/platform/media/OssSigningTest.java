package com.eduze.platform.media;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OssSigningTest {
    @Test
    void vpcStorageSignsClientUrlsWithThePublicEndpoint() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withUserConfiguration(OssObjectStore.class)
                .withPropertyValues(
                        "OSS_ENDPOINT=https://oss-cn-shanghai-internal.aliyuncs.com",
                        "OSS_PUBLIC_ENDPOINT=https://oss-cn-shanghai.aliyuncs.com",
                        "OSS_ACCESS_KEY_ID=test-access-id",
                        "OSS_ACCESS_KEY_SECRET=" + UUID.randomUUID(),
                        "OSS_BUCKET=eduze-test")
                .run(
                        context -> {
                            var store = context.getBean(OssObjectStore.class);
                            Instant expiry = Instant.now().plusSeconds(300);
                            assertEquals(
                                    "eduze-test.oss-cn-shanghai.aliyuncs.com",
                                    URI.create(
                                                    store.upload(
                                                                    "id",
                                                                    "staging/test.png",
                                                                    "image/png",
                                                                    expiry)
                                                            .url())
                                            .getHost());
                            assertEquals(
                                    "eduze-test.oss-cn-shanghai.aliyuncs.com",
                                    URI.create(
                                                    store.readUrl(
                                                            "id", "sealed/test.png", expiry, false))
                                            .getHost());
                        });
    }

    @Test
    void explicitCredentialsSignPrivateUrlsWithoutIniProfileProvider() {
        var store =
                new OssObjectStore(
                        "https://oss-cn-hangzhou.aliyuncs.com",
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
