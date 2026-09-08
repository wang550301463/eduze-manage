package com.eduze.commerce;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eduze.platform.runtime.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

class CommerceTest {
    private CatalogService catalog;
    private OrderService orders;
    private JdbcTemplate jdbc;
    private TransactionTemplate tx;
    private Actor staff =
            new Actor("staff", "1", Set.of("b"), Set.of("PRINCIPAL"), Set.of("commerce:write"));
    private Actor parent = new Actor("parent", "1", Set.of(), Set.of("PARENT"), Set.of());

    @BeforeEach
    void setup() {
        var ds =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:commerce"
                                + System.nanoTime()
                                + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "sa",
                        "");
        new ResourceDatabasePopulator(
                        new ClassPathResource("db/migration/V1__commerce.sql"),
                        new ClassPathResource("db/runtime/V0__runtime.sql"))
                .execute(ds);
        jdbc = new JdbcTemplate(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        var json = new ObjectMapper().findAndRegisterModules();
        var client = mock(InternalClient.class);
        when(client.get(eq("academic"), endsWith("/access"), eq(Map.class)))
                .thenReturn(Map.of("allowed", true, "branchId", "b"));
        catalog = new CatalogService(jdbc);
        orders = new OrderService(jdbc, client, new Outbox(jdbc, json));
    }

    @Test
    void orderSnapshotsPriceAndIdempotencyCannotChangePayloadOrOversell() {
        var p =
                catalog.save(
                        staff,
                        new CommerceModels.ProductInput(
                                null, "b", "PHYSICAL", "画具", "介绍", 5000, 1, null, 0, true));
        var input = new CommerceModels.OrderInput("b", p.id(), null, 1, "one");
        var order = tx.execute(s -> orders.create(parent, input));
        assertThat(tx.execute(s -> orders.create(parent, input)).id()).isEqualTo(order.id());
        assertThat(order.totalMinor()).isEqualTo(5000);
        assertThatThrownBy(
                        () ->
                                tx.execute(
                                        s ->
                                                orders.create(
                                                        parent,
                                                        new CommerceModels.OrderInput(
                                                                "b", p.id(), null, 2, "one"))))
                .hasMessageContaining("幂等");
        assertThatThrownBy(
                        () ->
                                tx.execute(
                                        s ->
                                                orders.create(
                                                        parent,
                                                        new CommerceModels.OrderInput(
                                                                "b", p.id(), null, 1, "two"))))
                .hasMessageContaining("库存");
    }

    @Test
    void duplicateProviderSuccessCreatesOneEntitlementEventAndRejectsWrongAmount() {
        var p =
                catalog.save(
                        staff,
                        new CommerceModels.ProductInput(
                                null, "b", "COURSE", "课时包", "介绍", 5000, 10, "course", 3, true));
        var order =
                tx.execute(
                        s ->
                                orders.create(
                                        parent,
                                        new CommerceModels.OrderInput(
                                                "b", p.id(), "student", 2, "key")));
        assertThatThrownBy(
                        () ->
                                tx.execute(
                                        s -> {
                                            orders.paid(order.id(), "wx", 999);
                                            return null;
                                        }))
                .hasMessageContaining("金额");
        tx.execute(
                s -> {
                    orders.paid(order.id(), "wx", 10000);
                    orders.paid(order.id(), "wx", 10000);
                    return null;
                });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_outbox", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT envelope FROM platform_outbox", String.class))
                .contains("\"lessonUnits\":6");
        assertThat(orders.get(parent, order.id()).fulfillmentStatus()).isEqualTo("PENDING");
        assertThatThrownBy(
                        () ->
                                orders.get(
                                        new Actor(
                                                "stranger",
                                                "1",
                                                Set.of(),
                                                Set.of("PARENT"),
                                                Set.of()),
                                        order.id()))
                .isInstanceOf(PlatformException.class);
    }

    @Test
    void deliveryRequiresAddressAndCannotBeChangedByIdempotencyReplay() {
        var p =
                catalog.save(
                        staff,
                        new CommerceModels.ProductInput(
                                null, "b", "PHYSICAL", "画具", "介绍", 5000, 10, null, 0, true));
        assertThatThrownBy(
                        () ->
                                tx.execute(
                                        s ->
                                                orders.create(
                                                        parent,
                                                        new CommerceModels.OrderInput(
                                                                "b",
                                                                p.id(),
                                                                null,
                                                                1,
                                                                "ship",
                                                                "DELIVERY",
                                                                null,
                                                                null,
                                                                null))))
                .hasMessageContaining("输入");
        var input =
                new CommerceModels.OrderInput(
                        "b", p.id(), null, 1, "ship", "DELIVERY", "家长", "13800000001", "上海市收件地址");
        var order = tx.execute(s -> orders.create(parent, input));
        assertThat(order.address()).isEqualTo("上海市收件地址");
        assertThatThrownBy(
                        () ->
                                tx.execute(
                                        s ->
                                                orders.create(
                                                        parent,
                                                        new CommerceModels.OrderInput(
                                                                "b",
                                                                p.id(),
                                                                null,
                                                                1,
                                                                "ship",
                                                                "DELIVERY",
                                                                "家长",
                                                                "13800000001",
                                                                "另一个地址"))))
                .hasMessageContaining("幂等");
    }

    @Test
    void reconciliationVisitsMoreThanOnePage() {
        var p =
                catalog.save(
                        staff,
                        new CommerceModels.ProductInput(
                                null, "b", "PHYSICAL", "画具", "介绍", 5000, 300, null, 0, true));
        for (int i = 0; i < 201; i++) {
            final String key = "page" + i;
            tx.execute(
                    s ->
                            orders.create(
                                    parent,
                                    new CommerceModels.OrderInput("b", p.id(), null, 1, key)));
        }
        PaymentGateway gateway = mock(PaymentGateway.class);
        when(gateway.queryOrder(anyString()))
                .thenReturn(new ObjectMapper().createObjectNode().put("trade_state", "NOTPAY"));
        new CommerceRecovery(jdbc, mock(RefundService.class), gateway, mock(PaymentService.class))
                .reconcile();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM commerce_reconciliation", Integer.class))
                .isEqualTo(201);
    }

    @Test
    void oldProductFormCannotRestoreStockAfterReservationOrCancellation() {
        var p =
                catalog.save(
                        staff,
                        new CommerceModels.ProductInput(
                                null, "b", "PHYSICAL", "画具", "介绍", 5000, 2, null, 0, true));
        var order =
                tx.execute(
                        s ->
                                orders.create(
                                        parent,
                                        new CommerceModels.OrderInput(
                                                "b", p.id(), null, 1, "reserve")));
        assertThat(catalog.get(p.id()).version()).isEqualTo(p.version() + 1);
        assertThatThrownBy(
                        () ->
                                catalog.save(
                                        staff,
                                        new CommerceModels.ProductInput(
                                                p.id(),
                                                "b",
                                                "PHYSICAL",
                                                "新标题",
                                                "介绍",
                                                5000,
                                                2,
                                                null,
                                                0,
                                                true,
                                                p.version())))
                .hasMessageContaining("版本");
        var afterSale = catalog.get(p.id());
        PaymentGateway gateway = mock(PaymentGateway.class);
        when(gateway.queryOrder(order.id()))
                .thenReturn(new ObjectMapper().createObjectNode().put("status", "NOT_FOUND"));
        var payments =
                new PaymentService(
                        jdbc,
                        orders,
                        gateway,
                        mock(InternalClient.class),
                        new WeChatPaymentProperties());
        tx.execute(
                s -> {
                    payments.cancel(parent, order.id());
                    return null;
                });
        assertThat(catalog.get(p.id()).stock()).isEqualTo(2);
        assertThat(catalog.get(p.id()).version()).isEqualTo(afterSale.version() + 1);
        assertThatThrownBy(
                        () ->
                                catalog.save(
                                        staff,
                                        new CommerceModels.ProductInput(
                                                p.id(),
                                                "b",
                                                "PHYSICAL",
                                                "新标题",
                                                "介绍",
                                                5000,
                                                1,
                                                null,
                                                0,
                                                true,
                                                afterSale.version())))
                .hasMessageContaining("版本");
    }
}
