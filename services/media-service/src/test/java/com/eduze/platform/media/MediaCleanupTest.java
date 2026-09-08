package com.eduze.platform.media;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eduze.platform.runtime.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.*;

class MediaCleanupTest {
    JdbcTemplate jdbc;
    ObjectStore store;
    TransactionTemplate tx;

    @BeforeEach
    void prepare() {
        var ds =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "sa",
                        "");
        jdbc = new JdbcTemplate(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        jdbc.execute(
                "CREATE TABLE media_file(id VARCHAR(36) PRIMARY KEY, tenant_id VARCHAR(36),branch_id VARCHAR(36),owner_id VARCHAR(36),file_name VARCHAR(255),purpose VARCHAR(32),content_type VARCHAR(128),file_size BIGINT,object_key VARCHAR(512),status VARCHAR(20),created_at TIMESTAMP,expires_at TIMESTAMP,staging_key VARCHAR(512),last_accessed_at TIMESTAMP,access_version BIGINT NOT NULL DEFAULT 0)");
        jdbc.execute(
                "CREATE TABLE media_reference(tenant_id VARCHAR(36),caller VARCHAR(40),owner_type VARCHAR(40),owner_id VARCHAR(36),media_id VARCHAR(36))");
        store = mock(ObjectStore.class);
        var request = new MockHttpServletRequest();
        request.setAttribute(
                Actors.ATTRIBUTE,
                new Actor("teacher", "tenant", Set.of("branch"), Set.of("SUPER_ADMIN"), Set.of()));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    void insert(String id, String status) {
        var old = Timestamp.from(Instant.now().minusSeconds(40 * 86400L));
        jdbc.update(
                "INSERT INTO media_file VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                id,
                "tenant",
                "branch",
                "teacher",
                "x.jpg",
                "ARTWORK",
                "image/jpeg",
                4,
                status.equals("READY") ? "sealed/" + id : "staging/" + id,
                status,
                old,
                old,
                "staging/" + id,
                old,
                0);
    }

    @Test
    void expiredStagingIsRemovedWithoutDeletingSealedFileAwaitingDelayedReferences() {
        insert("published", "READY");
        tx.executeWithoutResult(s -> new MediaCleanup(jdbc, store).cleanup());
        verify(store).delete("staging/published");
        verify(store, never()).delete("sealed/published");
        assertEquals("READY", jdbc.queryForObject("SELECT status FROM media_file", String.class));
        assertNull(jdbc.queryForObject("SELECT staging_key FROM media_file", String.class));
        jdbc.update(
                "INSERT INTO media_reference VALUES('tenant','portfolio','publication','owner','published')");
        tx.executeWithoutResult(s -> new MediaCleanup(jdbc, store).cleanup());
        verify(store, never()).delete("sealed/published");
    }

    @Test
    void cleanupRechecksStatusUnderLockWhenCompletionWinsTheRace() {
        insert("upload", "PENDING");
        var observed = spy(jdbc);
        doAnswer(
                        invocation -> {
                            var candidates =
                                    jdbc.queryForList(
                                            invocation.getArgument(0),
                                            String.class,
                                            (Object) invocation.getArgument(2));
                            jdbc.update(
                                    "UPDATE media_file SET status='READY',object_key='sealed/upload' WHERE id='upload'");
                            return candidates;
                        })
                .when(observed)
                .queryForList(anyString(), eq(String.class), any(Object.class));
        tx.executeWithoutResult(s -> new MediaCleanup(observed, store).cleanup());
        verify(store).delete("staging/upload");
        verify(store, never()).delete("sealed/upload");
        assertEquals("READY", jdbc.queryForObject("SELECT status FROM media_file", String.class));
    }

    @Test
    void reuseInvalidatesReviewVersionAndDeletionIsAlwaysAuditedAsBlocked() {
        jdbc.execute(
                "CREATE TABLE media_cleanup_audit(id VARCHAR(36),media_id VARCHAR(36),tenant_id VARCHAR(36),actor_id VARCHAR(36),expected_version BIGINT,decision VARCHAR(32),reason VARCHAR(1000),reviewed_at TIMESTAMP)");
        insert("candidate", "READY");
        var reviews = new MediaCleanupReview(jdbc);
        var snapshot = reviews.candidates().get(0);
        assertEquals(0, snapshot.version());
        assertTrue(new MediaService(jdbc, store).usable("candidate").usable());
        assertThrows(
                PlatformException.class,
                () ->
                        tx.execute(
                                s ->
                                        reviews.review(
                                                "candidate",
                                                new MediaCleanupReview.Review(
                                                        snapshot.version(),
                                                        "RETAIN",
                                                        "In active reuse"))));
        assertEquals(
                0, jdbc.queryForObject("SELECT COUNT(*) FROM media_cleanup_audit", Integer.class));
        jdbc.update(
                "UPDATE media_file SET last_accessed_at=?",
                Timestamp.from(Instant.now().minusSeconds(40 * 86400L)));
        var fresh = reviews.candidates().get(0);
        var result =
                tx.execute(
                        s ->
                                reviews.review(
                                        "candidate",
                                        new MediaCleanupReview.Review(
                                                fresh.version(), "DELETE", "Investigated orphan")));
        assertEquals("BLOCKED_OWNER_PROTOCOL_REQUIRED", result.decision());
        assertFalse(result.deleted());
        assertEquals("READY", jdbc.queryForObject("SELECT status FROM media_file", String.class));
        assertEquals(
                "teacher",
                jdbc.queryForObject("SELECT actor_id FROM media_cleanup_audit", String.class));
        assertEquals(
                "BLOCKED_OWNER_PROTOCOL_REQUIRED",
                jdbc.queryForObject("SELECT decision FROM media_cleanup_audit", String.class));
        verifyNoInteractions(store);
    }

    @Test
    void lateReferenceRemovesCandidateEvenWhenTheFileHasNeverBeenRead() {
        insert("candidate", "READY");
        var reviews = new MediaCleanupReview(jdbc);
        assertEquals(1, reviews.candidates().size());
        jdbc.update(
                "INSERT INTO media_reference VALUES('tenant','portfolio','publication','owner','candidate')");
        assertTrue(reviews.candidates().isEmpty());
        assertThrows(
                PlatformException.class,
                () ->
                        tx.execute(
                                s ->
                                        reviews.review(
                                                "candidate",
                                                new MediaCleanupReview.Review(
                                                        0, "RETAIN", "Stale review"))));
    }

    @Test
    void concurrentReuseCommitsBeforeReviewCompareAndSetAndForcesConflict() throws Exception {
        insert("racing", "READY");
        var observed = spy(jdbc);
        var compareAndSetEntered = new java.util.concurrent.CountDownLatch(1);
        doAnswer(
                        invocation -> {
                            compareAndSetEntered.countDown();
                            return invocation.callRealMethod();
                        })
                .when(observed)
                .update(
                        startsWith(
                                "UPDATE media_file SET last_accessed_at=CURRENT_TIMESTAMP,access_version=access_version+1 WHERE id=? AND tenant_id=? AND access_version=?"),
                        any(Object[].class));
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        var future =
                new java.util.concurrent.atomic.AtomicReference<
                        java.util.concurrent.Future<Boolean>>();
        try {
            tx.executeWithoutResult(
                    s -> {
                        assertTrue(new MediaService(jdbc, store).usable("racing").usable());
                        future.set(
                                executor.submit(
                                        () -> {
                                            var request = new MockHttpServletRequest();
                                            request.setAttribute(
                                                    Actors.ATTRIBUTE,
                                                    new Actor(
                                                            "teacher",
                                                            "tenant",
                                                            Set.of("branch"),
                                                            Set.of("SUPER_ADMIN"),
                                                            Set.of()));
                                            RequestContextHolder.setRequestAttributes(
                                                    new ServletRequestAttributes(request));
                                            try {
                                                assertThrows(
                                                        PlatformException.class,
                                                        () ->
                                                                tx.execute(
                                                                        t ->
                                                                                new MediaCleanupReview(
                                                                                                observed)
                                                                                        .review(
                                                                                                "racing",
                                                                                                new MediaCleanupReview
                                                                                                        .Review(
                                                                                                        0,
                                                                                                        "DELETE",
                                                                                                        "Concurrent review"))));
                                                return true;
                                            } finally {
                                                RequestContextHolder.resetRequestAttributes();
                                            }
                                        }));
                        try {
                            assertTrue(
                                    compareAndSetEntered.await(
                                            5, java.util.concurrent.TimeUnit.SECONDS));
                        } catch (InterruptedException e) {
                            throw new AssertionError(e);
                        }
                    });
            assertTrue(future.get().get(5, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(
                    "READY", jdbc.queryForObject("SELECT status FROM media_file", String.class));
            verifyNoInteractions(store);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void branchScopeAppliesBeforeLimitAndOtherBranchReviewNeverWritesAudit() {
        for (int i = 0; i < 101; i++) {
            insert("a" + i, "READY");
        }
        jdbc.update("UPDATE media_file SET branch_id='other'");
        insert("z-own-branch", "READY");
        var request = new MockHttpServletRequest();
        request.setAttribute(
                Actors.ATTRIBUTE,
                new Actor(
                        "principal",
                        "tenant",
                        Set.of("branch"),
                        Set.of("PRINCIPAL"),
                        Set.of("media:manage")));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        var reviews = new MediaCleanupReview(jdbc);
        assertEquals(
                List.of("z-own-branch"),
                reviews.candidates().stream().map(MediaCleanupReview.Candidate::id).toList());
        assertThrows(
                PlatformException.class,
                () ->
                        tx.execute(
                                t ->
                                        reviews.review(
                                                "a0",
                                                new MediaCleanupReview.Review(
                                                        0, "RETAIN", "Wrong branch"))));
        assertEquals(
                0L,
                jdbc.queryForObject(
                        "SELECT access_version FROM media_file WHERE id='a0'", Long.class));
    }
}
