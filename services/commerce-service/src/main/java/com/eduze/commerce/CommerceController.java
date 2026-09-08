package com.eduze.commerce;

import static com.eduze.commerce.CommerceModels.*;

import com.eduze.platform.runtime.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/commerce")
public class CommerceController {
    private final CatalogService catalog;
    private final OrderService orders;
    private final PaymentService payment;
    private final RefundService refunds;

    public CommerceController(
            CatalogService catalog,
            OrderService orders,
            PaymentService payment,
            RefundService refunds) {
        this.catalog = catalog;
        this.orders = orders;
        this.payment = payment;
        this.refunds = refunds;
    }

    @GetMapping("/public/products")
    public ApiResponse<java.util.List<Product>> publicProducts(@RequestParam String branchId) {
        return ApiResponse.ok(catalog.list(null, branchId, true));
    }

    @GetMapping("/products")
    public ApiResponse<java.util.List<Product>> products(@RequestParam String branchId) {
        return ApiResponse.ok(catalog.list(Actors.current(), branchId, false));
    }

    @PostMapping("/products")
    public ApiResponse<Product> saveProduct(
            @jakarta.validation.Valid @RequestBody ProductInput input) {
        return ApiResponse.ok(catalog.save(Actors.current(), input));
    }

    @GetMapping("/orders")
    public ApiResponse<java.util.List<Order>> orders(
            @RequestParam(required = false) String branchId) {
        return ApiResponse.ok(orders.list(Actors.current(), branchId));
    }

    @PostMapping("/orders")
    public ApiResponse<Order> create(@jakarta.validation.Valid @RequestBody OrderInput input) {
        return ApiResponse.ok(orders.create(Actors.current(), input));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<Order> order(@PathVariable String id) {
        return ApiResponse.ok(orders.get(Actors.current(), id));
    }

    @PostMapping("/orders/{id}/payment")
    public ApiResponse<Map<String, String>> pay(@PathVariable String id) {
        return ApiResponse.ok(payment.prepay(Actors.current(), id));
    }

    @PostMapping("/orders/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable String id) {
        payment.cancel(Actors.current(), id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/orders/{id}/refunds")
    public ApiResponse<Refund> refund(
            @PathVariable String id, @jakarta.validation.Valid @RequestBody RefundInput input) {
        return ApiResponse.ok(refunds.request(Actors.current(), id, input));
    }

    @GetMapping("/orders/{id}/refunds")
    public ApiResponse<java.util.List<Refund>> refunds(@PathVariable String id) {
        return ApiResponse.ok(refunds.list(Actors.current(), id));
    }

    @PostMapping("/orders/{id}/fulfillment")
    public ApiResponse<Void> fulfillment(
            @PathVariable String id,
            @jakarta.validation.Valid @RequestBody FulfillmentInput input) {
        orders.fulfillment(Actors.current(), id, input);
        return ApiResponse.ok(null);
    }

    @GetMapping("/orders/{id}/aftersales")
    public ApiResponse<java.util.List<Map<String, Object>>> aftersales(@PathVariable String id) {
        return ApiResponse.ok(orders.aftersales(Actors.current(), id));
    }

    @PostMapping("/orders/{id}/aftersales")
    public ApiResponse<Map<String, String>> aftersale(
            @PathVariable String id, @jakarta.validation.Valid @RequestBody AftersaleInput input) {
        return ApiResponse.ok(Map.of("id", orders.aftersale(Actors.current(), id, input)));
    }

    @PostMapping("/orders/{id}/aftersales/{requestId}/resolve")
    public ApiResponse<Void> resolve(
            @PathVariable String id,
            @PathVariable String requestId,
            @jakarta.validation.Valid @RequestBody AftersaleResolution input) {
        orders.resolve(Actors.current(), id, requestId, input);
        return ApiResponse.ok(null);
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard(@RequestParam String branchId) {
        return ApiResponse.ok(orders.dashboard(Actors.current(), branchId));
    }
}
