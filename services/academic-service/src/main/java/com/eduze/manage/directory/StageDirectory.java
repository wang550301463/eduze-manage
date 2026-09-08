package com.eduze.manage.directory;

import com.eduze.platform.runtime.Actors;
import com.eduze.platform.runtime.InternalClient;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StageDirectory {
    private final InternalClient client;

    @SuppressWarnings("unchecked")
    private Map<Long, StageView> cache() {
        var request = Actors.request();
        if (request == null) return new HashMap<>();
        String key = getClass().getName();
        Object value = request.getAttribute(key);
        if (value == null) {
            value = new HashMap<Long, StageView>();
            request.setAttribute(key, value);
        }
        return (Map<Long, StageView>) value;
    }

    public StageView get(Long id) {
        if (id == null) return null;
        Map<Long, StageView> cache = cache();
        if (!cache.containsKey(id)) {
            var rows = batch(List.of(id));
            rows.forEach(u -> cache.put(u.getId(), u));
            cache.putIfAbsent(id, null);
        }
        return cache.get(id);
    }

    public List<StageView> batch(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        var requested = ids.stream().filter(Objects::nonNull).distinct().toList();
        List<StageView> result = new ArrayList<>();
        for (int i = 0; i < requested.size(); i += 200) {
            var part = requested.subList(i, Math.min(i + 200, requested.size()));
            var rows =
                    Arrays.asList(
                            client.post(
                                    "teaching",
                                    "/internal/teaching/stages/batch",
                                    Map.of("ids", part),
                                    StageView[].class));
            var cache = cache();
            part.forEach(id -> cache.put(id, null));
            rows.forEach(u -> cache.put(u.getId(), u));
            result.addAll(rows);
        }
        return result;
    }
}
