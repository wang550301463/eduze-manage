package com.eduze.platform.media;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eduze.platform.runtime.*;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class MediaLifecycleTest {
    private MediaService service;
    private JdbcTemplate jdbc;
    private ObjectStore store;
    private MockHttpServletRequest request;

    @BeforeEach
    void prepare() {
        jdbc =
                new JdbcTemplate(
                        new DriverManagerDataSource(
                                "jdbc:h2:mem:media;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute(
                "CREATE TABLE media_file(id varchar(36) primary key,tenant_id varchar(36),branch_id varchar(36),owner_id varchar(36),file_name varchar(255),purpose varchar(32),content_type varchar(128),file_size bigint,object_key varchar(512),status varchar(20),created_at timestamp,expires_at timestamp,staging_key varchar(512),last_accessed_at timestamp,access_version bigint not null default 0)");
        jdbc.execute(
                "CREATE TABLE media_reference(tenant_id VARCHAR(36),caller VARCHAR(40),owner_type VARCHAR(40),owner_id VARCHAR(36),media_id VARCHAR(36))");
        store = mock(ObjectStore.class);
        when(store.upload(any(), any(), any(), any()))
                .thenReturn(
                        new ObjectStore.Upload("https://object.example/upload", "PUT", Map.of()));
        request = new MockHttpServletRequest();
        request.setAttribute(
                Actors.ATTRIBUTE,
                new Actor("1", "1", Set.of("2"), Set.of("TEACHER"), Set.of("student:write")));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        service = new MediaService(jdbc, store);
    }

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void failedSizeVerificationNeverMakesMediaUsable() {
        var uploaded =
                service.create(
                        new MediaService.UploadRequest(
                                "2", "ARTWORK", "picture.jpg", "image/jpeg", 4));
        when(store.inspect(any()))
                .thenReturn(new ObjectStore.Metadata(2, "image/jpeg", new byte[] {-1, -40, -1}));
        assertThatThrownBy(() -> service.complete(uploaded.id()))
                .isInstanceOf(PlatformException.class);
        assertThat(service.usable(uploaded.id()).usable()).isFalse();
    }

    @Test
    void anotherTeacherCannotConfirmAnUploadTheyDoNotOwn() {
        var uploaded =
                service.create(
                        new MediaService.UploadRequest(
                                "2", "ARTWORK", "picture.jpg", "image/jpeg", 4));
        request.setAttribute(
                Actors.ATTRIBUTE,
                new Actor("8", "1", Set.of("2"), Set.of("TEACHER"), Set.of("student:write")));
        assertThatThrownBy(() -> service.complete(uploaded.id()))
                .isInstanceOf(PlatformException.class);
        verify(store, never()).inspect(any());
    }

    @Test
    void completionSealsAnObjectOutsideTheReusableUploadKey() {
        var uploaded =
                service.create(
                        new MediaService.UploadRequest(
                                "2", "ARTWORK", "picture.jpg", "image/jpeg", 4));
        String uploadKey = service.find(uploaded.id()).objectKey();
        when(store.inspect(any()))
                .thenReturn(new ObjectStore.Metadata(4, "image/jpeg", new byte[] {-1, -40, -1, 0}));
        service.complete(uploaded.id());
        assertThat(service.find(uploaded.id()).objectKey()).isNotEqualTo(uploadKey);
        assertThat(service.usable(uploaded.id()).usable()).isTrue();
    }

    @Test
    void imageAccessReturnsDistinctThumbnailWhileOriginalUrlStaysOriginal() {
        var uploaded =
                service.create(
                        new MediaService.UploadRequest(
                                "2", "ARTWORK", "picture.jpg", "image/jpeg", 4));
        when(store.inspect(any()))
                .thenReturn(new ObjectStore.Metadata(4, "image/jpeg", new byte[] {-1, -40, -1, 0}));
        service.complete(uploaded.id());
        when(store.readUrl(any(), any(), any(), eq(false))).thenReturn("original");
        when(store.readUrl(any(), any(), any(), eq(true))).thenReturn("thumbnail");
        var json =
                new com.fasterxml.jackson.databind.ObjectMapper()
                        .findAndRegisterModules()
                        .valueToTree(
                                service.access(
                                        "teaching",
                                        new MediaService.Access(java.util.List.of(uploaded.id()))));
        assertThat(json.at("/items/0/url").asText()).isEqualTo("original");
        assertThat(json.at("/items/0/thumbnailUrl").asText()).isEqualTo("thumbnail");
        jdbc.update(
                "INSERT INTO media_reference VALUES('1','portfolio','publication','published',?)",
                uploaded.id());
        var publicLink =
                service.publicAccess(
                                "portfolio",
                                new MediaService.PublicAccess(
                                        "1", "published", java.util.List.of(uploaded.id())))
                        .items()
                        .get(0);
        assertThat(publicLink.url()).isEqualTo("original");
        assertThat(publicLink.thumbnailUrl()).isEqualTo("thumbnail");
    }

    @Test
    void audioLinksNeverRequestImageTransformations() {
        var uploaded =
                service.create(
                        new MediaService.UploadRequest("2", "AUDIO", "sound.mp3", "audio/mpeg", 4));
        when(store.inspect(any()))
                .thenReturn(
                        new ObjectStore.Metadata(4, "audio/mpeg", new byte[] {'I', 'D', '3', 0}));
        service.complete(uploaded.id());
        when(store.readUrl(any(), any(), any(), eq(false))).thenReturn("audio-original");
        var link =
                service.access(
                                "teaching",
                                new MediaService.Access(java.util.List.of(uploaded.id())))
                        .items()
                        .get(0);
        assertThat(link.url()).isEqualTo("audio-original");
        assertThat(link.thumbnailUrl()).isNull();
        verify(store, never()).readUrl(any(), any(), any(), eq(true));
    }
}
