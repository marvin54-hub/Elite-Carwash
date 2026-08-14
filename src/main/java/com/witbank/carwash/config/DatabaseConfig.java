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
 * Dynamically resolves database URL and driver type for both local H2 development
 * and cloud PostgreSQL deployments (Render, Heroku, etc.).
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
        // 1. Check system environment variables for Render/Cloud database URLs
        String envDbUrl = System.getenv("DATABASE_URL");
        if (envDbUrl == null || envDbUrl.isBlank()) {
            envDbUrl = System.getenv("SPRING_DATASOURCE_URL");
        }

        if (envDbUrl != null && !envDbUrl.isBlank()) {
            envDbUrl = envDbUrl.trim();

            // If Render/Heroku format: postgres://user:pass@host:port/db or postgresql://user:pass@host:port/db
            if (envDbUrl.startsWith("postgres://") || envDbUrl.startsWith("postgresql://")) {
                try {
                    log.info("Detected Cloud DATABASE_URL (postgres://). Parsing connection parameters...");
                    URI uri = new URI(envDbUrl);
                    String host = uri.getHost();
                    int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                    String path = uri.getPath(); // includes leading '/'

                    String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;

                    String username = defaultUsername;
                    String password = defaultPassword;

                    if (uri.getUserInfo() != null && uri.getUserInfo().contains(":")) {
                        String[] userInfo = uri.getUserInfo().split(":", 2);
                        username = userInfo[0];
                        password = userInfo[1];
                    }

                    log.info("Successfully configured PostgreSQL DataSource for host: {}:{}", host, port);

                    return DataSourceBuilder.create()
                            .url(jdbcUrl)
                            .username(username)
                            .password(password)
                            .driverClassName("org.postgresql.Driver")
                            .build();
                } catch (URISyntaxException e) {
                    log.error("Failed to parse DATABASE_URL environment variable: {}", e.getMessage());
                }
            } else if (envDbUrl.startsWith("jdbc:postgresql:")) {
                log.info("Detected JDBC PostgreSQL URL ({})", envDbUrl);
                return DataSourceBuilder.create()
                        .url(envDbUrl)
                        .username(defaultUsername)
                        .password(defaultPassword)
                        .driverClassName("org.postgresql.Driver")
                        .build();
            }
        }

        // 2. Fallback to application.properties defaults
        // Automatically match driver to URL type to prevent driver mismatch
        String driver = defaultDriver;
        if (defaultUrl != null && defaultUrl.startsWith("jdbc:h2:")) {
            driver = "org.h2.Driver";
        } else if (defaultUrl != null && defaultUrl.startsWith("jdbc:postgresql:")) {
            driver = "org.postgresql.Driver";
        }

        log.info("Using fallback DataSource configuration (URL: {}, Driver: {})", defaultUrl, driver);
        return DataSourceBuilder.create()
                .url(defaultUrl)
                .username(defaultUsername)
                .password(defaultPassword)
                .driverClassName(driver != null ? driver : "org.h2.Driver")
                .build();
    }
}
