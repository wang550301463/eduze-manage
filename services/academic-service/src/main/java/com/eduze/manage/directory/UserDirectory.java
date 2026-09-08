package com.eduze.manage.directory;

import com.eduze.platform.runtime.Actors;
import com.eduze.platform.runtime.InternalClient;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDirectory {
    private final InternalClient client;

    @SuppressWarnings("unchecked")
    private Map<Long, UserView> cache() {
        var request = Actors.request();
        if (request == null) return new HashMap<>();
        String key = getClass().getName();
        Object value = request.getAttribute(key);
        if (value == null) {
            value = new HashMap<Long, UserView>();
            request.setAttribute(key, value);
        }
        return (Map<Long, UserView>) value;
    }

    public UserView get(Long id) {
        if (id == null) return null;
        Map<Long, UserView> cache = cache();
        if (!cache.containsKey(id)) {
            var rows = batch(List.of(id));
            rows.forEach(u -> cache.put(u.getId(), u));
            cache.putIfAbsent(id, null);
        }
        return cache.get(id);
    }

    public List<UserView> batch(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        var requested = ids.stream().filter(Objects::nonNull).distinct().toList();
        List<UserView> result = new ArrayList<>();
        for (int i = 0; i < requested.size(); i += 200) {
            var part = requested.subList(i, Math.min(i + 200, requested.size()));
            var rows =
                    Arrays.asList(
                            client.post(
                                    "identity",
                                    "/internal/identity/users/batch",
                                    Map.of("ids", part),
                                    UserView[].class));
            var cache = cache();
            part.forEach(id -> cache.put(id, null));
            rows.forEach(u -> cache.put(u.getId(), u));
            result.addAll(rows);
        }
        return result;
    }

    public List<UserView> teachers(Long branchId) {
        var rows =
                Arrays.asList(
                        client.get(
                                "identity",
                                "/internal/identity/teachers"
                                        + (branchId == null ? "" : "?branchId=" + branchId),
                                UserView[].class));
        rows.forEach(u -> cache().put(u.getId(), u));
        return rows;
    }
}
