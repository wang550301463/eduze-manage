package com.eduze.commerce;

import com.eduze.platform.runtime.PlatformException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.*;
import java.time.*;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

/** APIv3 RSA request/response authentication and AES-GCM notification decryption. */
@Component
public class WeChatPaymentGateway implements PaymentGateway {
    private static final String BASE = "https://api.mch.weixin.qq.com";
    private final WeChatPaymentProperties settings;
    private final ObjectMapper json;
    private final RestClient http;

    public WeChatPaymentGateway(WeChatPaymentProperties settings, ObjectMapper json) {
        this.settings = settings;
        this.json = json;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));
        http = RestClient.builder().requestFactory(factory).build();
    }

    private void configured() {
        if (settings.getAppId().isBlank()
                || settings.getMchId().isBlank()
                || settings.getMerchantSerial().isBlank()
                || settings.getPlatformSerial().isBlank()
                || settings.getPrivateKeyPath().isBlank()
                || settings.getPlatformKeyPath().isBlank()
                || settings.getApiV3Key().getBytes(StandardCharsets.UTF_8).length != 32
                || !settings.getNotifyBaseUrl().startsWith("https://")) {
            throw new PlatformException(503, "微信支付尚未完成正式配置");
        }
    }

    private byte[] pem(String file, String label) throws java.io.IOException {
        return Base64.getDecoder()
                .decode(
                        Files.readString(Path.of(file))
                                .replace("-----BEGIN " + label + "-----", "")
                                .replace("-----END " + label + "-----", "")
                                .replaceAll("\\s", ""));
    }

    private String sign(String message) {
        try {
            var key =
                    KeyFactory.getInstance("RSA")
                            .generatePrivate(
                                    new PKCS8EncodedKeySpec(
                                            pem(settings.getPrivateKeyPath(), "PRIVATE KEY")));
            var signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception error) {
            throw new PlatformException(503, "支付签名配置不可用");
        }
    }

    private void verify(String body, String timestamp, String nonce, String serial, String signed) {
        configured();
        try {
            if (timestamp == null
                    || nonce == null
                    || signed == null
                    || !settings.getPlatformSerial().equals(serial)
                    || Math.abs(Instant.now().getEpochSecond() - Long.parseLong(timestamp)) > 300) {
                throw new GeneralSecurityException("Invalid envelope");
            }
            PublicKey key =
                    KeyFactory.getInstance("RSA")
                            .generatePublic(
                                    new X509EncodedKeySpec(
                                            pem(settings.getPlatformKeyPath(), "PUBLIC KEY")));
            var signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(key);
            signature.update(
                    (timestamp + "\n" + nonce + "\n" + body + "\n")
                            .getBytes(StandardCharsets.UTF_8));
            if (!signature.verify(Base64.getDecoder().decode(signed))) {
                throw new GeneralSecurityException("Invalid signature");
            }
        } catch (Exception error) {
            throw new PlatformException(401, "微信支付消息验签失败");
        }
    }

    private JsonNode request(String method, String path, Object payload) {
        configured();
        try {
            String body = payload == null ? "" : json.writeValueAsString(payload),
                    time = Long.toString(Instant.now().getEpochSecond()),
                    nonce = UUID.randomUUID().toString();
            String signature =
                    sign(method + "\n" + path + "\n" + time + "\n" + nonce + "\n" + body + "\n");
            String auth =
                    "WECHATPAY2-SHA256-RSA2048 mchid=\""
                            + settings.getMchId()
                            + "\",nonce_str=\""
                            + nonce
                            + "\",timestamp=\""
                            + time
                            + "\",serial_no=\""
                            + settings.getMerchantSerial()
                            + "\",signature=\""
                            + signature
                            + "\"";
            var request =
                    http.method(HttpMethod.valueOf(method))
                            .uri(BASE + path)
                            .header("Authorization", auth)
                            .header("Accept", "application/json")
                            .header("Wechatpay-Serial", settings.getPlatformSerial())
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON);
            if (payload != null) {
                request.body(body);
            }
            return request.exchange(
                    (req, res) -> {
                        String response =
                                new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        verify(
                                response,
                                res.getHeaders().getFirst("Wechatpay-Timestamp"),
                                res.getHeaders().getFirst("Wechatpay-Nonce"),
                                res.getHeaders().getFirst("Wechatpay-Serial"),
                                res.getHeaders().getFirst("Wechatpay-Signature"));
                        if (res.getStatusCode().value() == 404) {
                            return json.createObjectNode().put("status", "NOT_FOUND");
                        }
                        if (!res.getStatusCode().is2xxSuccessful()) {
                            throw new PlatformException(503, "微信支付处理结果待核对");
                        }
                        return response.isBlank()
                                ? json.createObjectNode()
                                : json.readTree(response);
                    });
        } catch (PlatformException error) {
            throw error;
        } catch (Exception error) {
            throw new PlatformException(503, "微信支付处理结果待核对");
        }
    }

    @Override
    public Map<String, String> prepay(CommerceModels.Order order, String openid) {
        JsonNode response =
                request(
                        "POST",
                        "/v3/pay/transactions/jsapi",
                        Map.of(
                                "appid",
                                settings.getAppId(),
                                "mchid",
                                settings.getMchId(),
                                "description",
                                order.productTitle(),
                                "out_trade_no",
                                order.id(),
                                "notify_url",
                                settings.getNotifyBaseUrl() + "/api/v1/commerce/callbacks/payment",
                                "amount",
                                Map.of("total", order.totalMinor(), "currency", "CNY"),
                                "payer",
                                Map.of("openid", CatalogService.text(openid, 128))));
        String prepay = response.path("prepay_id").asText();
        if (prepay.isBlank()) {
            throw new PlatformException(503, "微信支付未返回预支付凭证");
        }
        String timestamp = Long.toString(Instant.now().getEpochSecond()),
                nonce = UUID.randomUUID().toString(),
                pack = "prepay_id=" + prepay;
        return Map.of(
                "timeStamp",
                timestamp,
                "nonceStr",
                nonce,
                "package",
                pack,
                "signType",
                "RSA",
                "paySign",
                sign(settings.getAppId() + "\n" + timestamp + "\n" + nonce + "\n" + pack + "\n"));
    }

    @Override
    public JsonNode queryOrder(String orderId) {
        return request(
                "GET",
                "/v3/pay/transactions/out-trade-no/" + orderId + "?mchid=" + settings.getMchId(),
                null);
    }

    @Override
    public void closeOrder(String orderId) {
        request(
                "POST",
                "/v3/pay/transactions/out-trade-no/" + orderId + "/close",
                Map.of("mchid", settings.getMchId()));
    }

    @Override
    public JsonNode refund(String orderId, String refundId, long total, String reason) {
        return request(
                "POST",
                "/v3/refund/domestic/refunds",
                Map.of(
                        "out_trade_no",
                        orderId,
                        "out_refund_no",
                        refundId,
                        "reason",
                        reason,
                        "notify_url",
                        settings.getNotifyBaseUrl() + "/api/v1/commerce/callbacks/refund",
                        "amount",
                        Map.of("refund", total, "total", total, "currency", "CNY")));
    }

    @Override
    public JsonNode queryRefund(String refundId) {
        return request("GET", "/v3/refund/domestic/refunds/" + refundId, null);
    }

    @Override
    public JsonNode callback(String body, Map<String, String> headers) {
        if (body.length() > 65536) {
            throw new PlatformException(400, "支付通知过大");
        }
        var normalized = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        normalized.putAll(headers);
        verify(
                body,
                normalized.get("wechatpay-timestamp"),
                normalized.get("wechatpay-nonce"),
                normalized.get("wechatpay-serial"),
                normalized.get("wechatpay-signature"));
        try {
            var resource = json.readTree(body).path("resource");
            if (!"AEAD_AES_256_GCM".equals(resource.path("algorithm").asText())) {
                throw new GeneralSecurityException("Unsupported algorithm");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(
                            settings.getApiV3Key().getBytes(StandardCharsets.UTF_8), "AES"),
                    new GCMParameterSpec(
                            128, resource.path("nonce").asText().getBytes(StandardCharsets.UTF_8)));
            cipher.updateAAD(
                    resource.path("associated_data").asText().getBytes(StandardCharsets.UTF_8));
            return json.readTree(
                    cipher.doFinal(
                            Base64.getDecoder().decode(resource.path("ciphertext").asText())));
        } catch (Exception error) {
            throw new PlatformException(400, "支付通知解密失败");
        }
    }

    @Override
    public void verifyMerchant(JsonNode body, boolean requireAppId) {
        if (!settings.getMchId().equals(body.path("mchid").asText())
                || (requireAppId && !settings.getAppId().equals(body.path("appid").asText()))) {
            throw new PlatformException(403, "支付商户信息不匹配");
        }
    }
}
