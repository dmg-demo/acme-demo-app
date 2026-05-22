package com.acme.api.config;

import org.apache.coyote.http2.Http2Protocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hardens the embedded Tomcat connector against CVE-2024-34750.
 *
 * <p><b>CVE-2024-34750</b> — Apache Tomcat HTTP/2 stream exhaustion (CVSS 7.5 High).<br>
 * When HTTP/2 error conditions do not fully tear down stream state, a remote,
 * unauthenticated attacker can exhaust server threads/memory by holding open a
 * large number of half-closed streams.
 *
 * <p><b>Primary fix</b>: {@code spring-boot-starter-parent} upgraded to 3.3.6 which
 * bundles {@code tomcat-embed-core 10.1.33} (patched since 10.1.26).
 *
 * <p><b>This class</b> provides belt-and-suspenders protection:
 * <ul>
 *   <li>If HTTP/2 is NOT enabled (see {@code application.properties}): this bean
 *       is a no-op — no Http2Protocol upgrade handler is registered so nothing
 *       to configure.</li>
 *   <li>If HTTP/2 is later re-enabled: the stream and timeout limits below cap
 *       the maximum resource consumption, reducing blast radius to a bounded
 *       number of concurrent streams even on a pre-patch Tomcat.</li>
 * </ul>
 *
 * <p>Guidance source: JFrog {@code remediation-guide} MCP tool — CVE-2024-34750,
 * Java/Spring Boot application category.
 */
@Configuration
public class TomcatHardeningConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatHardeningCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {

            // Apply HTTP/2 stream limits to every Http2Protocol upgrade handler
            // registered on this connector (typically zero when HTTP/2 is disabled).
            connector.findUpgradeProtocols().stream()
                .filter(p -> p instanceof Http2Protocol)
                .map(p -> (Http2Protocol) p)
                .forEach(http2 -> {

                    // ── Stream concurrency caps ───────────────────────────────
                    // Default: 200 concurrent streams per connection.
                    // Reduced to 50 to limit the DoS window.
                    http2.setMaxConcurrentStreams(50);
                    http2.setMaxConcurrentStreamExecution(20);

                    // ── Timeouts ──────────────────────────────────────────────
                    // Force idle streams to close; prevents stream accumulation.
                    http2.setKeepAliveTimeout(30_000);
                    http2.setStreamReadTimeout(20_000);
                    http2.setStreamWriteTimeout(20_000);

                    // ── Header size limits ────────────────────────────────────
                    http2.setMaxHeaderCount(100);
                    http2.setMaxHeaderSize(8_192);
                });
        });
    }
}
