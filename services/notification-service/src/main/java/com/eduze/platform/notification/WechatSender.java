package com.eduze.platform.notification;

import com.eduze.platform.runtime.PlatformException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class WechatSender {
    private final NotificationProperties properties;
    private final RestClient client;
    private String accessToken;
    private Instant tokenExpires = Instant.EPOCH;

    public WechatSender(NotificationProperties properties) {
        this.properties = properties;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));
        client =
                RestClient.builder()
                        .requestFactory(factory)
                        .baseUrl("https://api.weixin.qq.com")
                        .build();
    }

    private synchronized String token() {
        if (Instant.now().isBefore(tokenExpires)) {
            return accessToken;
        }
        JsonNode reply =
                client.get()
                        .uri(
                                builder ->
                                        builder.path("/cgi-bin/token")
                                                .queryParam("grant_type", "client_credential")
                                                .queryParam("appid", properties.getAppId())
                                                .queryParam("secret", properties.getAppSecret())
                                                .build())
                        .retrieve()
                        .body(JsonNode.class);
        if (reply == null || !reply.hasNonNull("access_token")) {
            throw new PlatformException(503, "微信接入凭证不可用");
        }
        accessToken = reply.get("access_token").asText();
        tokenExpires =
                Instant.now().plusSeconds(Math.max(1, reply.path("expires_in").asInt(7200) - 120));
        return accessToken;
    }

    public String send(String openId, String key, String path, Map<String, String> values) {
        NotificationProperties.Template template = properties.getTemplates().get(key);
        if (properties.getAppId() == null
                || properties.getAppSecret() == null
                || template == null
                || template.getTemplateId() == null) {
            return "SKIPPED";
        }
        Map<String, Object> data = new HashMap<>();
        template.getFields()
                .forEach(
                        (field, source) ->
                                data.put(field, Map.of("value", values.getOrDefault(source, ""))));
        if (data.isEmpty()) {
            return "SKIPPED";
        }
        String token = token();
        JsonNode reply =
                client.post()
                        .uri(
                                builder ->
                                        builder.path("/cgi-bin/message/subscribe/send")
                                                .queryParam("access_token", token)
                                                .build())
                        .body(
                                Map.of(
                                        "touser",
                                        openId,
                                        "template_id",
                                        template.getTemplateId(),
                                        "page",
                                        path,
                                        "data",
                                        data,
                                        "miniprogram_state",
                                        "formal",
                                        "lang",
                                        "zh_CN"))
                        .retrieve()
                        .body(JsonNode.class);
        if (reply == null || !reply.hasNonNull("errcode")) {
            throw new IllegalStateException("通知结果未知");
        }
        int code = reply.path("errcode").asInt(-2);
        if (code == 40001 || code == 42001) {
            tokenExpires = Instant.EPOCH;
        }
        return NotificationPolicy.providerOutcome(code);
    }
}
