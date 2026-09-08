package com.eduze.platform.media;

import com.eduze.platform.runtime.PlatformException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class MediaRules {
    private static final Map<String, String> TYPES =
            Map.of(
                    "image/jpeg",
                    "jpg",
                    "image/png",
                    "png",
                    "image/webp",
                    "webp",
                    "application/pdf",
                    "pdf",
                    "video/mp4",
                    "mp4",
                    "audio/mpeg",
                    "mp3",
                    "audio/wav",
                    "wav",
                    "audio/mp4",
                    "m4a",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "pptx");

    private MediaRules() {}

    public static String extension(String type) {
        String extension = TYPES.get(type);
        if (extension == null) {
            throw new PlatformException(400, "不支持的文件类型");
        }
        return extension;
    }

    public static void validateUpload(String type, long size) {
        extension(type);
        long limit = type.startsWith("image/") ? 20_000_000L : 200_000_000L;
        if (size <= 0 || size > limit) {
            throw new PlatformException(400, "文件大小超过限制");
        }
    }

    public static void validateContent(String type, byte[] bytes) {
        String start = new String(bytes, StandardCharsets.ISO_8859_1);
        boolean valid =
                switch (extension(type)) {
                    case "jpg" ->
                            bytes.length >= 3
                                    && (bytes[0] & 255) == 255
                                    && (bytes[1] & 255) == 216
                                    && (bytes[2] & 255) == 255;
                    case "png" -> start.startsWith("\u0089PNG\r\n\u001a\n");
                    case "pdf" -> start.startsWith("%PDF-");
                    case "webp" ->
                            start.startsWith("RIFF")
                                    && start.length() >= 12
                                    && start.substring(8, 12).equals("WEBP");
                    case "wav" ->
                            start.startsWith("RIFF")
                                    && start.length() >= 12
                                    && start.substring(8, 12).equals("WAVE");
                    case "mp4", "m4a" ->
                            start.length() >= 12 && start.substring(4, 8).equals("ftyp");
                    case "mp3" ->
                            start.startsWith("ID3")
                                    || (bytes.length > 1
                                            && (bytes[0] & 255) == 255
                                            && (bytes[1] & 224) == 224);
                    case "pptx" -> start.startsWith("PK\u0003\u0004");
                    default -> false;
                };
        if (!valid) {
            throw new PlatformException(400, "实际文件格式与声明不一致");
        }
    }
}
