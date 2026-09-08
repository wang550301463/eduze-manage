package com.eduze.platform.runtime;

import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class InternalClient {
    private final RuntimeProperties properties;
    private final RestClient client;

    public InternalClient(RuntimeProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));
        client = RestClient.builder().requestFactory(factory).build();
    }

    public <T> T get(String service, String path, Class<T> type) {
        return exchange(service, path, null, type, false);
    }

    public <T> T post(String service, String path, Object body, Class<T> type) {
        return exchange(service, path, body, type, true);
    }

    private <T> T exchange(String service, String path, Object body, Class<T> type, boolean post) {
        String base = properties.getServices().get(service);
        if (base == null || !path.startsWith("/") || path.startsWith("//")) {
            throw new PlatformException(503, "服务尚未配置");
        }
        if (properties.getServiceToken() == null || properties.getServiceToken().length() < 24) {
            throw new PlatformException(503, "服务凭证尚未配置");
        }
        try {
            RestClient.RequestBodySpec request =
                    client.method(
                                    post
                                            ? org.springframework.http.HttpMethod.POST
                                            : org.springframework.http.HttpMethod.GET)
                            .uri(base + path);
            request.header("X-Service-Name", properties.getServiceName());
            request.header("X-Service-Token", properties.getServiceToken());
            if (Actors.request() != null) {
                String bearer = Actors.request().getHeader(HttpHeaders.AUTHORIZATION);
                if (bearer != null) {
                    request.header(HttpHeaders.AUTHORIZATION, bearer);
                }
            }
            String trace = org.slf4j.MDC.get("traceId");
            if (trace != null) {
                request.header("X-Trace-Id", trace);
            }
            if (body != null) {
                request.contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(body);
            }
            return request.retrieve().body(type);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 401 || status == 403 || status == 404 || status == 409 || status == 400) {
                throw new PlatformException(status, "关联资源不可访问或状态已变化");
            }
            throw new PlatformException(503, "关联服务暂不可用");
        } catch (RestClientException exception) {
            throw new PlatformException(503, "关联服务暂不可用");
        }
    }
}
