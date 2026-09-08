package com.eduze.platform.media;

import com.eduze.platform.runtime.PlatformException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
@ConditionalOnProperty(name = "eduze.media.provider", havingValue = "local")
public class LocalObjectStore implements ObjectStore {
    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();
    private final Path root;
    private final String base;
    private final byte[] secret;

    public LocalObjectStore(
            @Value("${eduze.media.local-root:./storage}") String root,
            @Value("${eduze.media.public-base:http://localhost:18080}") String base) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.base = base;
        secret = new byte[32];
        RANDOM.nextBytes(secret);
    }

    private Path path(String key) {
        Path result = root.resolve(key).normalize();
        if (!result.startsWith(root)) {
            throw new PlatformException(400, "文件路径无效");
        }
        return result;
    }

    public String token(String operation, String id, long expiry) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(
                            mac.doFinal(
                                    (operation + ":" + id + ":" + expiry)
                                            .getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException error) {
            throw new IllegalStateException(error);
        }
    }

    public void verify(String operation, String id, long expiry, String token) {
        if (expiry < Instant.now().getEpochSecond()
                || token == null
                || !MessageDigest.isEqual(
                        token(operation, id, expiry).getBytes(StandardCharsets.UTF_8),
                        token.getBytes(StandardCharsets.UTF_8))) {
            throw new PlatformException(403, "上传或读取凭证已失效");
        }
    }

    public Upload upload(String id, String key, String type, Instant expiry) {
        long time = expiry.getEpochSecond();
        return new Upload(
                base
                        + "/api/v1/media/public/uploads/"
                        + id
                        + "?expires="
                        + time
                        + "&token="
                        + token("PUT", id, time),
                "PUT",
                Map.of("Content-Type", type));
    }

    public void put(String key, String type, long expectedSize, InputStream stream) {
        Path file = path(key);
        try {
            Files.createDirectories(file.getParent());
            Path temporary = Files.createTempFile(file.getParent(), "upload-", ".part");
            try {
                try (var output = Files.newOutputStream(temporary)) {
                    byte[] buffer = new byte[8192];
                    long count = 0;
                    int read;
                    while ((read = stream.read(buffer)) != -1) {
                        count += read;
                        if (count > expectedSize) {
                            throw new PlatformException(400, "文件超过声明大小");
                        }
                        output.write(buffer, 0, read);
                    }
                    if (count != expectedSize) {
                        throw new PlatformException(400, "文件未完整上传");
                    }
                }
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                Files.writeString(path(key + ".type"), type);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (java.io.IOException error) {
            throw new PlatformException(503, "本地开发存储不可用");
        }
    }

    public Metadata inspect(String key) {
        try (var stream = Files.newInputStream(path(key))) {
            return new Metadata(
                    Files.size(path(key)),
                    Files.readString(path(key + ".type")),
                    stream.readNBytes(512));
        } catch (java.io.IOException error) {
            throw new PlatformException(422, "文件尚未上传完成");
        }
    }

    public String readUrl(String id, String key, Instant expiry, boolean thumbnail) {
        long time = expiry.getEpochSecond();
        return base
                + "/api/v1/media/public/files/"
                + id
                + "?expires="
                + time
                + "&token="
                + token("GET", id, time);
    }

    public org.springframework.core.io.Resource resource(String key) {
        return new org.springframework.core.io.FileSystemResource(path(key));
    }

    public void seal(String sourceKey, String sealedKey) {
        try {
            Files.createDirectories(path(sealedKey).getParent());
            Files.copy(path(sourceKey), path(sealedKey));
            Files.copy(path(sourceKey + ".type"), path(sealedKey + ".type"));
        } catch (java.io.IOException error) {
            throw new PlatformException(503, "文件归档失败");
        }
    }

    public void delete(String key) {
        try {
            Files.deleteIfExists(path(key));
            Files.deleteIfExists(path(key + ".type"));
        } catch (java.io.IOException error) {
            throw new PlatformException(503, "文件清理失败");
        }
    }
}
