package com.eduze.platform.runtime;

import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class Inbox {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final List<EventHandler> handlers;

    public Inbox(JdbcTemplate jdbc, TransactionTemplate transaction, List<EventHandler> handlers) {
        this.jdbc = jdbc;
        this.transaction = transaction;
        this.handlers = handlers;
    }

    public boolean receive(EventEnvelope event) {
        if (event.version() != 1) {
            throw new PlatformException(422, "不支持的事件版本");
        }
        List<EventHandler> matching =
                handlers.stream().filter(handler -> handler.supports(event.type())).toList();
        if (matching.isEmpty()) {
            throw new PlatformException(422, "不支持的事件类型");
        }
        return Boolean.TRUE.equals(
                transaction.execute(
                        status -> {
                            try {
                                jdbc.update(
                                        "INSERT INTO platform_inbox(event_id,event_type,processed_at) VALUES(?,?,CURRENT_TIMESTAMP)",
                                        event.eventId(),
                                        event.type());
                            } catch (DuplicateKeyException duplicate) {
                                return false;
                            }
                            matching.forEach(handler -> handler.handle(event));
                            return true;
                        }));
    }
}
