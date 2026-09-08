package com.eduze.commerce;

import com.eduze.platform.runtime.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final JdbcTemplate jdbc;
    private final OrderService orders;
    private final PaymentGateway gateway;
    private final InternalClient client;
    private final WeChatPaymentProperties settings;

    public PaymentService(
            JdbcTemplate jdbc,
            OrderService orders,
            PaymentGateway gateway,
            InternalClient client,
            WeChatPaymentProperties settings) {
        this.jdbc = jdbc;
        this.orders = orders;
        this.gateway = gateway;
        this.client = client;
        this.settings = settings;
    }

    @Transactional
    public Map<String, String> prepay(Actor actor, String id) {
        jdbc.queryForList("SELECT id FROM commerce_order WHERE id=? FOR UPDATE", id);
        var order = orders.get(actor, id);
        if (!actor.userId().equals(order.userId()) || !"UNPAID".equals(order.paymentStatus())) {
            throw new PlatformException(409, "订单不支持支付");
        }
        Map<?, ?> binding =
                client.get(
                        "identity",
                        "/internal/identity/users/" + actor.userId() + "/wechat",
                        Map.class);
        if (binding == null
                || !settings.getAppId().equals(binding.get("appId"))
                || !(binding.get("openid") instanceof String openid)) {
            throw new PlatformException(409, "请使用已绑定微信身份支付");
        }
        return gateway.prepay(order, openid);
    }

    @Transactional
    public void paid(JsonNode body) {
        gateway.verifyMerchant(body, true);
        if (!"SUCCESS".equals(body.path("trade_state").asText())
                || !"CNY".equals(body.path("amount").path("currency").asText())) {
            throw new PlatformException(400, "支付结果不是人民币成功交易");
        }
        orders.paid(
                body.path("out_trade_no").asText(),
                body.path("transaction_id").asText(),
                body.path("amount").path("total").asLong(-1));
    }

    @Transactional
    public void cancel(Actor actor, String id) {
        var order = orders.get(actor, id);
        jdbc.queryForList("SELECT id FROM commerce_order WHERE id=? FOR UPDATE", id);
        order = orders.get(actor, id);
        if ("CANCELLED".equals(order.paymentStatus())) {
            return;
        }
        if (!"UNPAID".equals(order.paymentStatus())) {
            throw new PlatformException(409, "已支付订单请申请退款");
        }
        var state = gateway.queryOrder(id);
        if ("SUCCESS".equals(state.path("trade_state").asText())) {
            throw new PlatformException(409, "支付已成功，请先刷新订单");
        }
        if (!"NOT_FOUND".equals(state.path("status").asText())
                && !"CLOSED".equals(state.path("trade_state").asText())) {
            gateway.closeOrder(id);
        }
        jdbc.update(
                "UPDATE commerce_order SET payment_status='CANCELLED',updated_at=CURRENT_TIMESTAMP WHERE id=?",
                id);
        jdbc.update(
                "UPDATE product SET stock=stock+?,version=version+1 WHERE id=?",
                order.quantity(),
                order.productId());
    }
}
