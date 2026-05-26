package com.acme.shared;

import org.apache.commons.text.StringSubstitutor;
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
    // SECRETS DEMO: hard-coded example credentials (not real — these
    // are the canonical AWS documentation placeholder values)
    // ----------------------------------------------------------------
    private static final String EXAMPLE_AWS_ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE";
    private static final String EXAMPLE_AWS_SECRET_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";

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
    // SAST DEMO: SQL injection via string concatenation
    // User-supplied username is concatenated directly into the query
    // instead of being passed as a prepared-statement parameter.
    // ----------------------------------------------------------------
    public static ResultSet findUser(Connection conn, String username) throws SQLException {
        String query = "SELECT * FROM users WHERE username = '" + username + "'";
        return conn.createStatement().executeQuery(query);
    }
}
