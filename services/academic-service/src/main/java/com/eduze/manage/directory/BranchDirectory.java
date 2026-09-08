package com.eduze.manage.directory;

import com.eduze.platform.runtime.InternalClient;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BranchDirectory {
    private final InternalClient client;

    public BranchView get(Long id) {
        if (id == null) return null;
        return client.get("identity", "/internal/identity/branches/" + id, BranchView.class);
    }

    public List<BranchView> batch(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return Arrays.asList(
                client.post(
                        "identity",
                        "/internal/identity/branches/batch",
                        Map.of("ids", ids),
                        BranchView[].class));
    }

    public List<BranchView> all() {
        return Arrays.asList(
                client.get("identity", "/internal/identity/branches", BranchView[].class));
    }
}
