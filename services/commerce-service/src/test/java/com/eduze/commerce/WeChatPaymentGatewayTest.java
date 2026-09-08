package com.eduze.commerce;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.Instant;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class WeChatPaymentGatewayTest {
    @TempDir Path folder;
    private final ObjectMapper json = new ObjectMapper();
    private WeChatPaymentProperties settings;
    private WeChatPaymentGateway gateway;
    private KeyPair pair;

    @BeforeEach
    void setup() throws Exception {
        pair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        Path publicKey = folder.resolve("public.pem"), privateKey = folder.resolve("private.pem");
        Files.writeString(
                publicKey,
                "-----BEGIN PUBLIC KEY-----\n"
                        + Base64.getEncoder().encodeToString(pair.getPublic().getEncoded())
                        + "\n-----END PUBLIC KEY-----");
        Files.writeString(
                privateKey,
                "-----BEGIN PRIVATE KEY-----\n" // gitleaks:allow -- generated at runtime; no stored
                        // credential
                        + Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded())
                        + "\n-----END PRIVATE KEY-----");
        settings = new WeChatPaymentProperties();
        settings.setAppId("app");
        settings.setMchId("merchant");
        settings.setMerchantSerial("serial");
        settings.setPrivateKeyPath(privateKey.toString());
        settings.setPlatformKeyPath(publicKey.toString());
        settings.setPlatformSerial("PUB_KEY_ID_1");
        settings.setApiV3Key("12345678901234567890123456789012");
        settings.setNotifyBaseUrl("https://example.com");
        gateway = new WeChatPaymentGateway(settings, json);
    }

    private String body() throws Exception {
        String nonce = "123456789012", aad = "transaction";
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(settings.getApiV3Key().getBytes(StandardCharsets.UTF_8), "AES"),
                new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
        cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
        return json.writeValueAsString(
                Map.of(
                        "resource",
                        Map.of(
                                "algorithm",
                                "AEAD_AES_256_GCM",
                                "nonce",
                                nonce,
                                "associated_data",
                                aad,
                                "ciphertext",
                                Base64.getEncoder()
                                        .encodeToString(
                                                cipher.doFinal(
                                                        "{\"mchid\":\"merchant\",\"appid\":\"app\"}"
                                                                .getBytes(
                                                                        StandardCharsets
                                                                                .UTF_8))))));
    }

    private Map<String, String> headers(String body, String timestamp) throws Exception {
        String nonce = "signed-nonce";
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initSign(pair.getPrivate());
        s.update((timestamp + "\n" + nonce + "\n" + body + "\n").getBytes(StandardCharsets.UTF_8));
        return Map.of(
                "wechatpay-timestamp",
                timestamp,
                "wechatpay-nonce",
                nonce,
                "wechatpay-serial",
                "PUB_KEY_ID_1",
                "wechatpay-signature",
                Base64.getEncoder().encodeToString(s.sign()));
    }

    @Test
    void verifiesAndDecryptsAuthenticCallbackButRejectsTamperingAndReplay() throws Exception {
        String body = body();
        var headers = headers(body, Long.toString(Instant.now().getEpochSecond()));
        assertThat(gateway.callback(body, headers).path("mchid").asText()).isEqualTo("merchant");
        var upper = new HashMap<String, String>();
        headers.forEach((k, v) -> upper.put(k.toUpperCase(java.util.Locale.ROOT), v));
        assertThat(gateway.callback(body, upper).path("mchid").asText()).isEqualTo("merchant");
        assertThatThrownBy(() -> gateway.callback(body + " ", headers)).hasMessageContaining("验签");
        var stale = headers(body, Long.toString(Instant.now().minusSeconds(600).getEpochSecond()));
        assertThatThrownBy(() -> gateway.callback(body, stale)).hasMessageContaining("验签");
        assertThatThrownBy(
                        () ->
                                gateway.verifyMerchant(
                                        json.createObjectNode()
                                                .put("mchid", "other")
                                                .put("appid", "app"),
                                        true))
                .hasMessageContaining("商户");
    }

    @Test
    void absentProductionCredentialsNeverPretendToPay() {
        var disabled = new WeChatPaymentGateway(new WeChatPaymentProperties(), json);
        assertThatThrownBy(() -> disabled.queryOrder("order")).hasMessageContaining("正式配置");
    }
}
