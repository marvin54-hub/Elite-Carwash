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
 * Automatically parses Cloud/Render DATABASE_URL format (postgres://user:pass@host:port/db)
 * and transforms it into valid JDBC format (jdbc:postgresql://host:port/db) required by PostgreSQL JDBC driver.
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

    @Value("${spring.datasource.driver-class-name:org.h2.Driver}")
    private String defaultDriver;

    @Bean
    @Primary
    public DataSource dataSource() {
        String envDbUrl = System.getenv("DATABASE_URL");
        if (envDbUrl != null && (envDbUrl.startsWith("postgres://") || envDbUrl.startsWith("postgresql://"))) {
            try {
                log.info("Detected Render/Cloud DATABASE_URL. Parsing connection parameters...");
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
        }

        log.info("Using standard DataSource configuration (URL: {})", defaultUrl);
        return DataSourceBuilder.create()
                .url(defaultUrl)
                .username(defaultUsername)
                .password(defaultPassword)
                .driverClassName(defaultDriver)
                .build();
    }
}
