package com.eduze.commerce;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eduze.platform.runtime.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
class MySqlCommerceIT {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Test
    void twoConcurrentBuyersCannotReserveOnePhysicalItem() throws Exception {
        var ds =
                new DriverManagerDataSource(
                        MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        new ResourceDatabasePopulator(
                        new ClassPathResource("db/migration/V1__commerce.sql"),
                        new ClassPathResource("db/runtime/V0__runtime.sql"))
                .execute(ds);
        var jdbc = new JdbcTemplate(ds);
        var tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        var staff =
                new Actor("staff", "1", Set.of("b"), Set.of("PRINCIPAL"), Set.of("commerce:write"));
        var product =
                new CatalogService(jdbc)
                        .save(
                                staff,
                                new CommerceModels.ProductInput(
                                        null,
                                        "b",
                                        "PHYSICAL",
                                        "唯一画具",
                                        "介绍",
                                        5000,
                                        1,
                                        null,
                                        0,
                                        true));
        var orders =
                new OrderService(
                        jdbc,
                        mock(InternalClient.class),
                        new Outbox(jdbc, new ObjectMapper().findAndRegisterModules()));
        var barrier = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var futures = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < 2; i++) {
                final String user = "buyer" + i;
                futures.add(
                        executor.submit(
                                () -> {
                                    barrier.await(5, TimeUnit.SECONDS);
                                    try {
                                        tx.execute(
                                                s ->
                                                        orders.create(
                                                                new Actor(
                                                                        user,
                                                                        "1",
                                                                        Set.of(),
                                                                        Set.of("PARENT"),
                                                                        Set.of()),
                                                                new CommerceModels.OrderInput(
                                                                        "b",
                                                                        product.id(),
                                                                        null,
                                                                        1,
                                                                        user)));
                                        return true;
                                    } catch (PlatformException expected) {
                                        assertThat(expected.getStatus()).isEqualTo(409);
                                        return false;
                                    }
                                }));
            }
            int succeeded = 0;
            for (var future : futures) {
                if (future.get(20, TimeUnit.SECONDS)) {
                    succeeded++;
                }
            }
            assertThat(succeeded).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT stock FROM product", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM commerce_order", Integer.class))
                    .isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
