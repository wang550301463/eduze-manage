package com.eduze.platform.media;

import com.eduze.platform.runtime.PlatformException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile({"dev", "test"})
@ConditionalOnProperty(name = "eduze.media.provider", havingValue = "local")
public class LocalUploadController {
    private final LocalObjectStore store;
    private final MediaService service;

    public LocalUploadController(LocalObjectStore store, MediaService service) {
        this.store = store;
        this.service = service;
    }

    @PutMapping("/api/v1/media/public/uploads/{id}")
    public ResponseEntity<Void> upload(
            @PathVariable String id,
            @RequestParam long expires,
            @RequestParam String token,
            HttpServletRequest request)
            throws java.io.IOException {
        store.verify("PUT", id, expires, token);
        var file = service.find(id);
        if (!file.status().equals("PENDING")) {
            throw new PlatformException(409, "文件已确认，不能覆盖");
        }
        store.put(file.objectKey(), file.contentType(), file.size(), request.getInputStream());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/media/public/files/{id}")
    public ResponseEntity<org.springframework.core.io.Resource> read(
            @PathVariable String id, @RequestParam long expires, @RequestParam String token) {
        store.verify("GET", id, expires, token);
        var file = service.find(id);
        if (!file.status().equals("READY")) {
            throw new PlatformException(404, "文件不可用");
        }
        return ResponseEntity.ok()
                .header("Cache-Control", "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(org.springframework.http.MediaType.parseMediaType(file.contentType()))
                .body(store.resource(file.objectKey()));
    }
}
