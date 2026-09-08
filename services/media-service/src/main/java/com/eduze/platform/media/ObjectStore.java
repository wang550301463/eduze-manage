package com.eduze.platform.media;

import java.time.Instant;
import java.util.Map;

public interface ObjectStore {
    record Upload(String url, String method, Map<String, String> headers) {}

    record Metadata(long size, String contentType, byte[] signature) {}

    Upload upload(String id, String key, String contentType, Instant expiresAt);

    Metadata inspect(String key);

    String readUrl(String id, String key, Instant expiresAt, boolean thumbnail);

    void seal(String sourceKey, String sealedKey);

    void delete(String key);
}
