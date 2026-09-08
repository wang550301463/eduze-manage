package com.eduze.platform.notification;

import static org.assertj.core.api.Assertions.*;

import com.eduze.platform.runtime.*;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.*;

class NotificationPrivacyTest {
    private NotificationService service;

    @BeforeEach
    void setup() {
        var jdbc =
                new JdbcTemplate(
                        new DriverManagerDataSource(
                                "jdbc:h2:mem:notifications;MODE=MySQL;DB_CLOSE_DELAY=-1",
                                "sa",
                                ""));
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute(
                "CREATE TABLE notification_message(id varchar(36) primary key,tenant_id varchar(36),branch_id varchar(36),recipient_id varchar(36),student_id varchar(36),title varchar(100),body varchar(200),business_type varchar(40),business_id varchar(36),path varchar(100),status varchar(20),visible int,read_at timestamp,confirmed_at timestamp,created_at timestamp)");
        jdbc.update(
                "INSERT INTO notification_message(id,tenant_id,branch_id,recipient_id,student_id,title,body,business_type,business_id,path,status,visible,created_at) VALUES('other','1','2','different-parent','3','private','private','PORTFOLIO_PUBLISHED','4','page','SENT',1,CURRENT_TIMESTAMP)");
        var request = new MockHttpServletRequest();
        request.setAttribute(
                Actors.ATTRIBUTE, new Actor("parent", "1", Set.of(), Set.of("PARENT"), Set.of()));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        service = new NotificationService(jdbc, org.mockito.Mockito.mock(InternalClient.class));
    }

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void cannotReadOrAcknowledgeAnotherParentsNotification() {
        assertThat(service.list()).isEmpty();
        assertThatThrownBy(() -> service.mark("other", true)).isInstanceOf(PlatformException.class);
    }
}
