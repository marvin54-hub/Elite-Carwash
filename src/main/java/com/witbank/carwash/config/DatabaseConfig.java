package com.witbank.carwash.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Custom DataSource configuration for Witbank Elite Car Wash.
 * Dynamically resolves and sanitizes database URLs and driver types for both
 * local H2 development and cloud PostgreSQL deployments (Render, Heroku, etc.).
 */
@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url}")
    private String defaultUrl;

    @Value("${spring.datasource.username:sa}")
    private String defaultUsername;

    @Value("${spring.datasource.password:}")
    private String defaultPassword;

    @Value("${spring.datasource.driver-class-name:#{null}}")
    private String defaultDriver;

    @Bean
    @Primary
    public DataSource dataSource() {
        // 1. Check environment variables for Render / Cloud database URLs
        String envDbUrl = System.getenv("DATABASE_URL");
        if (envDbUrl == null || envDbUrl.isBlank()) {
            envDbUrl = System.getenv("SPRING_DATASOURCE_URL");
        }

        if (envDbUrl != null && !envDbUrl.isBlank()) {
            envDbUrl = envDbUrl.trim();

            if (isPostgresUrl(envDbUrl)) {
                return createPostgresDataSource(envDbUrl);
            }
        }

        // Also check if defaultUrl from application.properties is Postgres
        if (isPostgresUrl(defaultUrl)) {
            return createPostgresDataSource(defaultUrl);
        }

        // 2. Fallback to application.properties defaults (e.g. H2)
        String driver = defaultDriver;
        if (defaultUrl != null && defaultUrl.startsWith("jdbc:h2:")) {
            driver = "org.h2.Driver";
        }

        log.info("Using standard H2/fallback DataSource configuration (URL: {}, Driver: {})", defaultUrl, driver);
        return DataSourceBuilder.create()
                .url(defaultUrl)
                .username(defaultUsername)
                .password(defaultPassword)
                .driverClassName(driver != null ? driver : "org.h2.Driver")
                .build();
    }

    private boolean isPostgresUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.startsWith("postgres://") || lower.startsWith("postgresql://") || lower.startsWith("jdbc:postgresql:");
    }

    private DataSource createPostgresDataSource(String rawUrl) {
        log.info("Parsing and normalizing PostgreSQL URL: {}", rawUrl);

        String workingUrl = rawUrl.trim();
        if (workingUrl.toLowerCase().startsWith("jdbc:")) {
            workingUrl = workingUrl.substring(5); // strip "jdbc:" -> leaves "postgresql://..."
        }

        String username = defaultUsername;
        String password = defaultPassword;
        String host = "localhost";
        int port = 5432;
        String dbPath = "/carwashdb";

        try {
            URI uri = new URI(workingUrl);
            if (uri.getHost() != null) {
                host = uri.getHost();
            }
            if (uri.getPort() != -1) {
                port = uri.getPort();
            }
            if (uri.getPath() != null && !uri.getPath().isEmpty()) {
                dbPath = uri.getPath();
            }

            if (uri.getUserInfo() != null && !uri.getUserInfo().isEmpty()) {
                String userInfo = uri.getUserInfo();
                if (userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    username = parts[0];
                    password = parts[1];
                } else {
                    password = userInfo;
                }
            }
        } catch (URISyntaxException e) {
            log.warn("Could not parse URI syntax for PostgreSQL URL: {}", e.getMessage());
        }

        String cleanJdbcUrl = "jdbc:postgresql://" + host + ":" + port + dbPath;
        log.info("Cleaned PostgreSQL JDBC URL: {} (User: {})", cleanJdbcUrl, username);

        return DataSourceBuilder.create()
                .url(cleanJdbcUrl)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}
