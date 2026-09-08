package com.eduze.platform.runtime;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventController {
    private final Inbox inbox;
    private final InternalCredentials credentials;

    public EventController(Inbox inbox, InternalCredentials credentials) {
        this.inbox = inbox;
        this.credentials = credentials;
    }

    @PostMapping("/internal/events")
    public Map<String, Boolean> receive(
            @Valid @RequestBody EventEnvelope event, HttpServletRequest request) {
        String caller = credentials.require(request);
        if (!event.type().startsWith(caller + ".")) {
            throw new PlatformException(403, "事件来源不匹配");
        }
        return Map.of("processed", inbox.receive(event));
    }
}
