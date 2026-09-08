package com.eduze.commerce;

import com.eduze.platform.runtime.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/commerce")
public class CommerceOperationsController {
    private final CommerceOperations operations;

    public CommerceOperationsController(CommerceOperations operations) {
        this.operations = operations;
    }

    @GetMapping("/reconciliations")
    public ApiResponse<?> reconciliations(@RequestParam String branchId) {
        return ApiResponse.ok(operations.reconciliations(Actors.current(), branchId));
    }

    @PostMapping("/orders/{id}/reconcile")
    public ApiResponse<?> reconcile(@PathVariable String id) {
        return ApiResponse.ok(operations.reconcile(Actors.current(), id));
    }

    @GetMapping("/refunds/pending")
    public ApiResponse<?> pending(@RequestParam String branchId) {
        return ApiResponse.ok(operations.pendingRefunds(Actors.current(), branchId));
    }

    @PostMapping("/orders/{id}/refunds/{refundId}/retry")
    public ApiResponse<?> retry(@PathVariable String id, @PathVariable String refundId) {
        return ApiResponse.ok(operations.retryRefund(Actors.current(), id, refundId));
    }
}
