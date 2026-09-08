package com.eduze.platform.media;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.GetObjectRequest;
import com.eduze.platform.runtime.PlatformException;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eduze.media.provider", havingValue = "oss", matchIfMissing = true)
public class OssObjectStore implements ObjectStore {
    private final OSS client;
    private final OSS signingClient;
    private final String bucket;

    public OssObjectStore(
            @Value("${OSS_ENDPOINT}") String endpoint,
            @Value("${OSS_PUBLIC_ENDPOINT:${OSS_ENDPOINT}}") String publicEndpoint,
            @Value("${OSS_ACCESS_KEY_ID}") String accessId,
            @Value("${OSS_ACCESS_KEY_SECRET}") String accessSecret,
            @Value("${OSS_BUCKET}") String bucket) {
        if (!endpoint.startsWith("https://")
                || !publicEndpoint.startsWith("https://")
                || publicEndpoint.contains("-internal.aliyuncs.com")
                || bucket.isBlank()
                || accessId.isBlank()
                || accessSecret.isBlank()) {
            throw new IllegalStateException("OSS must use HTTPS and configured credentials");
        }
        this.bucket = bucket;
        com.aliyun.oss.ClientBuilderConfiguration config =
                new com.aliyun.oss.ClientBuilderConfiguration();
        config.setConnectionTimeout(3000);
        config.setSocketTimeout(10000);
        config.setMaxErrorRetry(1);
        client = new OSSClientBuilder().build(endpoint, accessId, accessSecret, config);
        signingClient =
                endpoint.equals(publicEndpoint)
                        ? client
                        : new OSSClientBuilder()
                                .build(publicEndpoint, accessId, accessSecret, config);
    }

    public Upload upload(String id, String key, String type, Instant expiresAt) {
        GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest(bucket, key, HttpMethod.PUT);
        request.setExpiration(Date.from(expiresAt));
        request.setContentType(type);
        return new Upload(
                signingClient.generatePresignedUrl(request).toString(),
                "PUT",
                Map.of("Content-Type", type));
    }

    public Metadata inspect(String key) {
        try {
            var metadata = client.getObjectMetadata(bucket, key);
            GetObjectRequest request = new GetObjectRequest(bucket, key);
            request.setRange(0, 511);
            try (var object = client.getObject(request)) {
                return new Metadata(
                        metadata.getContentLength(),
                        metadata.getContentType(),
                        object.getObjectContent().readNBytes(512));
            }
        } catch (Exception error) {
            throw new PlatformException(422, "文件尚未上传完成或无法读取");
        }
    }

    public String readUrl(String id, String key, Instant expiresAt, boolean thumbnail) {
        GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest(bucket, key, HttpMethod.GET);
        request.setExpiration(Date.from(expiresAt));
        if (thumbnail) {
            request.setProcess("image/resize,w_480/auto-orient,1/format,webp");
        }
        return signingClient.generatePresignedUrl(request).toString();
    }

    public void seal(String sourceKey, String sealedKey) {
        client.copyObject(bucket, sourceKey, bucket, sealedKey);
    }

    public void delete(String key) {
        client.deleteObject(bucket, key);
    }

    @PreDestroy
    public void close() {
        if (signingClient != client) {
            signingClient.shutdown();
        }
        client.shutdown();
    }
}
