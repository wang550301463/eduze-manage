package com.eduze.manage.identity;

import com.eduze.manage.identity.IdentityDirectoryModels.*;
import com.eduze.platform.runtime.Actor;
import com.eduze.platform.runtime.InternalCredentials;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/identity")
@RequiredArgsConstructor
public class InternalIdentityController {
    private final SessionIntrospection sessions;
    private final InternalCredentials credentials;
    private final IdentityDirectoryService directory;

    public record TokenRequest(String token) {}

    public record Batch(List<Long> ids) {}

    public record RecipientsRequest(List<String> userIds) {}

    @PostMapping("/introspect")
    public Actor introspect(@RequestBody TokenRequest input, HttpServletRequest req) {
        credentials.require(req);
        return sessions.introspect(input.token());
    }

    @GetMapping("/users/{id}")
    public UserView user(@PathVariable Long id, HttpServletRequest req) {
        credentials.require(req);
        return directory.user(id);
    }

    @PostMapping("/users/batch")
    public List<UserView> batch(@RequestBody Batch input, HttpServletRequest req) {
        credentials.require(req);
        return directory.batch(input.ids());
    }

    @GetMapping("/teachers")
    public List<UserView> teachers(
            @RequestParam(required = false) Long branchId, HttpServletRequest req) {
        credentials.require(req);
        return directory.teachers(branchId);
    }

    @GetMapping("/branches")
    public List<BranchView> branches(HttpServletRequest req) {
        credentials.require(req);
        return directory.branches();
    }

    @GetMapping("/branches/{id}")
    public BranchView branch(@PathVariable Long id, HttpServletRequest req) {
        credentials.require(req);
        return directory.branch(id);
    }

    @PostMapping("/branches/batch")
    public List<BranchView> branchesBatch(@RequestBody Batch input, HttpServletRequest req) {
        credentials.require(req);
        return directory.branchesBatch(input.ids());
    }

    @PostMapping("/wechat/recipients")
    public List<Map<String, String>> recipients(
            @RequestBody RecipientsRequest input, HttpServletRequest req) {
        credentials.requireCaller(req, "notification");
        return directory.recipients(input.userIds());
    }

    @GetMapping("/users/{id}/wechat")
    public Map<String, String> paymentIdentity(@PathVariable Long id, HttpServletRequest req) {
        credentials.requireCaller(req, "commerce");
        return directory.paymentIdentity(id);
    }
}
