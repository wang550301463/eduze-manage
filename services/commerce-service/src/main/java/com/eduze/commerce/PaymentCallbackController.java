package com.eduze.commerce;

import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/commerce/callbacks")
public class PaymentCallbackController {
    private final PaymentGateway gateway;
    private final PaymentService payment;
    private final RefundService refunds;

    public PaymentCallbackController(
            PaymentGateway gateway, PaymentService payment, RefundService refunds) {
        this.gateway = gateway;
        this.payment = payment;
        this.refunds = refunds;
    }

    @PostMapping("/payment")
    public Map<String, String> payment(
            @RequestBody String body, @RequestHeader Map<String, String> headers) {
        payment.paid(gateway.callback(body, headers));
        return Map.of("code", "SUCCESS", "message", "成功");
    }

    @PostMapping("/refund")
    public Map<String, String> refund(
            @RequestBody String body, @RequestHeader Map<String, String> headers) {
        var payload = gateway.callback(body, headers);
        refunds.providerResult(payload, true);
        return Map.of("code", "SUCCESS", "message", "成功");
    }
}
