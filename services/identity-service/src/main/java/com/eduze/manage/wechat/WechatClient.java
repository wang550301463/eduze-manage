package com.eduze.manage.wechat;

import com.eduze.platform.runtime.PlatformException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Calls WeChat code2Session. Secrets and session_key are never returned or logged. */
@Component
public class WechatClient {
    private final Environment env;
    private final ObjectMapper json;
    private final HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public WechatClient(Environment env, ObjectMapper json) {
        this.env = env;
        this.json = json;
    }

    public String appId() {
        return required("eduze.wechat.app-id");
    }

    public String exchange(String code) {
        if (code == null || code.isBlank() || code.length() > 256) {
            throw new PlatformException(400, "微信登录码无效");
        }
        String uri =
                "https://api.weixin.qq.com/sns/jscode2session?appid="
                        + encode(appId())
                        + "&secret="
                        + encode(required("eduze.wechat.secret"))
                        + "&js_code="
                        + encode(code)
                        + "&grant_type=authorization_code";
        try {
            var request =
                    HttpRequest.newBuilder(URI.create(uri))
                            .timeout(Duration.ofSeconds(8))
                            .GET()
                            .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new PlatformException(503, "微信登录暂不可用");
            }
            JsonNode body = json.readTree(response.body());
            String openId = body.path("openid").asText("");
            if (body.path("errcode").asInt(0) != 0 || openId.isBlank()) {
                throw new PlatformException(401, "微信登录码失效，请重新登录");
            }
            return openId;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new PlatformException(503, "微信登录中断");
        } catch (java.io.IOException ex) {
            throw new PlatformException(503, "微信登录暂不可用");
        }
    }

    private String required(String key) {
        String value = env.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new PlatformException(503, "微信登录尚未配置");
        }
        return value;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
