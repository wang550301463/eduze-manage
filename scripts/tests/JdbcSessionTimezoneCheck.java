import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;

/**
 * Standalone Connector/J integration check against an existing disposable test database.
 * Compile with javac --release 17; run with Connector/J on the classpath and
 * SPRING_DATASOURCE_URL / USERNAME / PASSWORD in the environment.
 * First argument: JDBC query options. Optional second argument: JVM zone override.
 * Uses only a connection-local temporary table, removed automatically on disconnect.
 */
public class JdbcSessionTimezoneCheck {
    public static void main(String[] args) throws Exception {
        if (args.length > 1) {
            TimeZone.setDefault(TimeZone.getTimeZone(args[1]));
        }
        String url = System.getenv("SPRING_DATASOURCE_URL").split("\\?")[0] + "?" + args[0];
        try (Connection connection = DriverManager.getConnection(
                url, System.getenv("SPRING_DATASOURCE_USERNAME"),
                System.getenv("SPRING_DATASOURCE_PASSWORD"))) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TEMPORARY TABLE timezone_check "
                        + "(next_attempt_at DATETIME(6),created_at TIMESTAMP(6),lesson_start DATETIME(6))");
            }
            Instant now = Instant.now().minusSeconds(1);
            LocalDateTime lesson = LocalDateTime.of(2026, 9, 9, 9, 30);
            try (var insert = connection.prepareStatement("INSERT INTO timezone_check VALUES(?,?,?)")) {
                insert.setTimestamp(1, Timestamp.from(now));
                insert.setTimestamp(2, Timestamp.from(now));
                insert.setObject(3, lesson);
                insert.executeUpdate();
            }
            try (var statement = connection.createStatement();
                    var row = statement.executeQuery("SELECT @@session.time_zone AS zone,"
                            + "TIMESTAMPDIFF(SECOND,next_attempt_at,CURRENT_TIMESTAMP) AS delay_seconds,"
                            + "next_attempt_at<=CURRENT_TIMESTAMP AS due,next_attempt_at,created_at,"
                            + "lesson_start,CURRENT_TIMESTAMP AS sql_now FROM timezone_check")) {
                row.next();
                long lag = row.getLong("delay_seconds");
                boolean due = row.getBoolean("due");
                System.out.println("jvm=" + TimeZone.getDefault().getID()
                        + ", session=" + row.getString("zone") + ", due=" + due
                        + ", lagSeconds=" + lag
                        + ", lesson=" + row.getObject("lesson_start", LocalDateTime.class));
                require(due && Math.abs(lag) <= 5, "An event written now must be immediately due");
                require(Math.abs(Duration.between(now,
                        row.getTimestamp("next_attempt_at").toInstant()).toMillis()) <= 2,
                        "DATETIME instant did not round trip");
                require(Math.abs(Duration.between(now,
                        row.getTimestamp("created_at").toInstant()).toMillis()) <= 2,
                        "TIMESTAMP instant did not round trip");
                require(lesson.equals(row.getTimestamp("lesson_start").toLocalDateTime()),
                        "Legacy Timestamp-to-LocalDateTime lesson clock changed");
                require(lesson.equals(row.getObject("lesson_start", LocalDateTime.class)),
                        "Chinese lesson wall time changed");
                require(Math.abs(Duration.between(Instant.now(),
                        row.getTimestamp("sql_now").toInstant()).getSeconds()) <= 5,
                        "SQL clock and Instant clock differ");
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
