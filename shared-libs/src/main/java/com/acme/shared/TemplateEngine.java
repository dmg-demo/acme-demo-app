package com.acme.shared;

import org.apache.commons.text.StringSubstitutor;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * Demo utility class — intentionally contains security findings for Frogbot scanning demo.
 * DO NOT use in production.
 */
public final class TemplateEngine {

    // ----------------------------------------------------------------
    // SECRETS DEMO: hard-coded service credentials.
    // NOTE: AKIAIOSFODNN7EXAMPLE is the official AWS docs placeholder
    // and is allowlisted by most scanners. Using realistic-format fakes
    // that are NOT on the allowlist.
    // ----------------------------------------------------------------
    private static final String DB_PASSWORD = "Sup3r$ecretD3m0Key_acme!9x2z";
    private static final String INTERNAL_TOKEN = "acme-svc-token-xK9mQ3vL8nP5rT2wY6uE4oI7jH1";

    private TemplateEngine() {}

    // ----------------------------------------------------------------
    // SCA APPLICABLE DEMO: CVE-2022-42889 (Text4Shell)
    // User-controlled input flows directly into StringSubstitutor as
    // the template string, making the CVE applicable via Contextual
    // Analysis — an attacker can inject ${script:groovy:...} payloads.
    // ----------------------------------------------------------------
    public static String render(String userTemplate, Map<String, String> vars) {
        return StringSubstitutor.replace(userTemplate, vars);
    }

    // ----------------------------------------------------------------
    // SAST DEMO: Command injection
    // System.getProperty() is a recognised taint SOURCE in JFrog SAST.
    // The unsanitised value flows into Runtime.exec() — a known SINK.
    // ----------------------------------------------------------------
    public static void generateReport() throws IOException {
        String reportType = System.getProperty("report.type");
        Runtime.getRuntime().exec(new String[]{"bash", "-c", "generate-report.sh " + reportType});
    }

    // ----------------------------------------------------------------
    // SAST DEMO: SQL injection
    // System.getenv() is a recognised taint SOURCE in JFrog SAST.
    // The unsanitised value flows into executeQuery() — a known SINK.
    // ----------------------------------------------------------------
    public static ResultSet findUser(Connection conn) throws SQLException {
        String userId = System.getenv("QUERY_USER_ID");
        String query = "SELECT * FROM users WHERE id = '" + userId + "'";
        return conn.createStatement().executeQuery(query);
    }
}
