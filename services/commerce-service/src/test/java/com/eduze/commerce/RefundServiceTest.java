package com.eduze.commerce;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eduze.platform.runtime.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

class RefundServiceTest {
    private JdbcTemplate jdbc;
    private RefundService refunds;
    private OrderService orders;
    private TransactionTemplate tx;
    private InternalClient client;
    private PaymentGateway gateway;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final Actor parent = new Actor("p", "1", Set.of(), Set.of("PARENT"), Set.of());
    private CommerceModels.Order order;

    @BeforeEach
    void setup() {
        var ds =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:refund" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "sa",
                        "");
        new ResourceDatabasePopulator(
                        new ClassPathResource("db/migration/V1__commerce.sql"),
                        new ClassPathResource("db/runtime/V0__runtime.sql"))
                .execute(ds);
        jdbc = new JdbcTemplate(ds);
        var manager = new DataSourceTransactionManager(ds);
        tx = new TransactionTemplate(manager);
        client = mock(InternalClient.class);
        gateway = mock(PaymentGateway.class);
        when(client.get(eq("academic"), endsWith("/access"), eq(Map.class)))
                .thenReturn(Map.of("allowed", true, "branchId", "b"));
        when(client.post(eq("academic"), endsWith("/freeze"), any(), eq(Map.class)))
                .thenReturn(Map.of("status", "FROZEN"));
        when(client.post(eq("academic"), endsWith("/complete"), any(), eq(Map.class)))
                .thenReturn(Map.of("status", "COMPLETED"));
        when(client.post(eq("academic"), endsWith("/release"), any(), eq(Map.class)))
                .thenReturn(Map.of("status", "RELEASED"));
        var staff = new Actor("s", "1", Set.of("b"), Set.of("PRINCIPAL"), Set.of("commerce:write"));
        var product =
                new CatalogService(jdbc)
                        .save(
                                staff,
                                new CommerceModels.ProductInput(
                                        null, "b", "COURSE", "主题课", "课程", 5000, 5, "course", 3,
                                        true));
        orders = new OrderService(jdbc, client, new Outbox(jdbc, json));
        refunds = new RefundService(jdbc, orders, gateway, client, manager);
        order =
                tx.execute(
                        s ->
                                orders.create(
                                        parent,
                                        new CommerceModels.OrderInput(
                                                "b", product.id(), "student", 1, "order-key")));
        tx.execute(
                s -> {
                    orders.paid(order.id(), "wx-transaction", 5000);
                    return null;
                });
    }

    private JsonNode result(String refund, String status, boolean callback) {
        var body =
                json.createObjectNode()
                        .put("out_refund_no", refund)
                        .put("out_trade_no", order.id())
                        .put("transaction_id", "wx-transaction")
                        .put("refund_id", "wx-refund")
                        .put(callback ? "refund_status" : "status", status);
        var amount = body.putObject("amount").put("total", 5000).put("refund", 5000);
        if (!callback) {
            amount.put("currency", "CNY");
        }
        return body;
    }

    @Test
    void ambiguousProviderSubmissionIsQueriedBeforeRetryAndRestoresStockOnce() {
        when(gateway.queryRefund(anyString()))
                .thenReturn(json.createObjectNode().put("status", "NOT_FOUND"));
        when(gateway.refund(anyString(), anyString(), anyLong(), anyString()))
                .thenThrow(new PlatformException(503, "unknown"));
        var productBeforeRefund = new CatalogService(jdbc).get(order.productId());
        var input = new CommerceModels.RefundInput("refund-key", "未使用退款");
        var refund = refunds.request(parent, order.id(), input);
        assertThat(refund.status()).isEqualTo("SUBMIT_UNKNOWN");
        when(gateway.queryRefund(refund.id())).thenReturn(result(refund.id(), "SUCCESS", false));
        refunds.advance(refund.id());
        refunds.advance(refund.id());
        assertThat(refunds.get(refund.id()).status()).isEqualTo("COMPLETED");
        verify(gateway, times(1)).refund(anyString(), anyString(), anyLong(), anyString());
        assertThat(jdbc.queryForObject("SELECT stock FROM product", Integer.class)).isEqualTo(5);
        assertThat(orders.get(parent, order.id()).paymentStatus()).isEqualTo("REFUNDED");
        var staff =
                new Actor("staff", "1", Set.of("b"), Set.of("PRINCIPAL"), Set.of("commerce:write"));
        assertThatThrownBy(
                        () ->
                                new CatalogService(jdbc)
                                        .save(
                                                staff,
                                                new CommerceModels.ProductInput(
                                                        productBeforeRefund.id(),
                                                        "b",
                                                        "COURSE",
                                                        "新标题",
                                                        "课程",
                                                        5000,
                                                        productBeforeRefund.stock(),
                                                        "course",
                                                        3,
                                                        true,
                                                        productBeforeRefund.version())))
                .hasMessageContaining("版本");
        assertThat(jdbc.queryForObject("SELECT stock FROM product", Integer.class)).isEqualTo(5);
    }

    @Test
    void callbackWithoutCurrencyIsValidAndAbnormalKeepsRightsFrozen() {
        when(gateway.queryRefund(anyString())).thenThrow(new PlatformException(503, "unknown"));
        var refund = refunds.request(parent, order.id(), new CommerceModels.RefundInput("r", "原因"));
        refunds.providerResult(result(refund.id(), "ABNORMAL", true), true);
        refunds.advance(refund.id());
        assertThat(refunds.get(refund.id()).status()).isEqualTo("PROVIDER_ABNORMAL");
        verify(client, never()).post(eq("academic"), endsWith("/release"), any(), eq(Map.class));
    }

    @Test
    void failedFreezeNeverCallsProviderAndDuplicateRefundCannotChangeReason() {
        when(client.post(eq("academic"), endsWith("/freeze"), any(), eq(Map.class)))
                .thenThrow(new PlatformException(409, "consumed"));
        var refund = refunds.request(parent, order.id(), new CommerceModels.RefundInput("r", "原因"));
        assertThat(refund.status()).isEqualTo("FAILED");
        verifyNoInteractions(gateway);
        assertThat(orders.get(parent, order.id()).paymentStatus()).isEqualTo("PAID");
        assertThatThrownBy(
                        () ->
                                refunds.request(
                                        parent,
                                        order.id(),
                                        new CommerceModels.RefundInput("r2", "other")))
                .hasMessageContaining("幂等");
    }

    @Test
    void notYetIssuedEntitlementRemainsRecoverableWithOneRefundIdentifier() {
        when(client.post(eq("academic"), endsWith("/freeze"), any(), eq(Map.class)))
                .thenThrow(new PlatformException(404, "grant pending"))
                .thenReturn(Map.of("status", "FROZEN"));
        var input = new CommerceModels.RefundInput("pending-rights", "未开始上课");
        var refund = refunds.request(parent, order.id(), input);
        assertThat(refund.status()).isEqualTo("REQUESTED");
        assertThat(orders.get(parent, order.id()).paymentStatus()).isEqualTo("REFUNDING");
        verifyNoInteractions(gateway);
        when(gateway.queryRefund(refund.id()))
                .thenReturn(json.createObjectNode().put("status", "NOT_FOUND"));
        when(gateway.refund(eq(order.id()), eq(refund.id()), eq(5000L), anyString()))
                .thenReturn(result(refund.id(), "SUCCESS", false));
        assertThat(refunds.request(parent, order.id(), input).status()).isEqualTo("COMPLETED");
        refunds.advance(refund.id());
        verify(gateway, times(1)).refund(eq(order.id()), eq(refund.id()), eq(5000L), anyString());
        verify(client, times(1)).post(eq("academic"), endsWith("/complete"), any(), eq(Map.class));
        assertThat(jdbc.queryForObject("SELECT version FROM product", Integer.class)).isEqualTo(3);
    }

    @Test
    void rejectedOrInaccessibleEntitlementKeepsManualRecoveryWithoutPayingUnfrozenRights() {
        jdbc.update("UPDATE commerce_order SET fulfillment_status='FAILED' WHERE id=?", order.id());
        when(client.post(eq("academic"), endsWith("/freeze"), any(), eq(Map.class)))
                .thenThrow(new PlatformException(403, "authorization rejected"));
        var refund =
                refunds.request(
                        parent, order.id(), new CommerceModels.RefundInput("manual-check", "发放失败"));
        assertThat(refund.status()).isEqualTo("REQUESTED");
        assertThat(orders.get(parent, order.id()).paymentStatus()).isEqualTo("REFUNDING");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT last_error FROM commerce_refund WHERE id=?",
                                String.class,
                                refund.id()))
                .contains("核实授权");
        verifyNoInteractions(gateway);
    }
}
