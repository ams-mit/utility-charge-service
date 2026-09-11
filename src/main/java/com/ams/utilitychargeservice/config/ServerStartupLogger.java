package com.ams.utilitychargeservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Arrays;

@Slf4j
@Component
public class ServerStartupLogger implements CommandLineRunner {

    @Autowired
    private Environment env;

    @Autowired
    private DataSource dataSource;

    @Value("${server.port:8082}")
    private String port;

    @Value("${spring.application.name:utility-charge-service}")
    private String appName;

    @Override
    public void run(String... args) {
        log.info("========================================================================");
        log.info("🚀 {} STARTUP DETAILS", appName.toUpperCase().replace("-", " "));
        log.info("========================================================================");
        log.info("Server Port    : {}", port);
        log.info("Base URL       : http://localhost:{}", port);
        log.info("Active Profiles: {}", Arrays.toString(env.getActiveProfiles()));

        String dbUrl = env.getProperty("spring.datasource.url");
        log.info("Database URL   : {}", dbUrl);

        log.info("Database Conn  : {}", checkDatabaseConnection());

        boolean gatewayKeySet = env.containsProperty("ams.security.gateway-public-key");
        boolean serviceKeySet = env.containsProperty("ams.security.service-private-key");
        log.info("JWT Keys Set   : {}", (gatewayKeySet && serviceKeySet) ? "✅ YES" : "❌ NO");

        log.info("========================================================================");
    }

    private String checkDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return "✅ CONNECTED";
        } catch (Exception e) {
            log.error("Database connection failed: {}", e.getMessage());
            return "❌ FAILED (" + e.getMessage() + ")";
        }
    }
}
