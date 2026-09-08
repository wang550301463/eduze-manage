package com.eduze.commerce;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/** Only verified provider responses cross this boundary. */
public interface PaymentGateway {
    Map<String, String> prepay(CommerceModels.Order order, String openid);

    JsonNode queryOrder(String orderId);

    JsonNode refund(String orderId, String refundId, long total, String reason);

    JsonNode queryRefund(String refundId);

    void closeOrder(String orderId);

    JsonNode callback(String body, Map<String, String> headers);

    void verifyMerchant(JsonNode body, boolean requireAppId);
}
