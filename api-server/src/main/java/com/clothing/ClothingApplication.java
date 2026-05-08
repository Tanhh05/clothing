package com.clothing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
@EnableScheduling
public class ClothingApplication {

    public static void main(String[] args) {
        boolean runningInsideContainer = isRunningInsideContainer();
        if (!runningInsideContainer) {
            normalizeLocalDbHost();
        }
        SpringApplication application = new SpringApplication(ClothingApplication.class);
        if (!hasExplicitActiveProfile()) {
            application.setAdditionalProfiles("sql-only");
        }
        application.run(args);
    }

    private static boolean hasExplicitActiveProfile() {
        String systemPropertyProfile = System.getProperty("spring.profiles.active");
        if (systemPropertyProfile != null && !systemPropertyProfile.isBlank()) {
            return true;
        }
        String envProfile = System.getenv("SPRING_PROFILES_ACTIVE");
        return envProfile != null && !envProfile.isBlank();
    }

    private static boolean isRunningInsideContainer() {
        return System.getenv("RUNNING_IN_DOCKER") != null
                || System.getenv("KUBERNETES_SERVICE_HOST") != null
                || new java.io.File("/.dockerenv").exists();
    }

    private static void normalizeLocalDbHost() {
        if (System.getProperty("spring.datasource.url") != null) {
            return;
        }
        String dbHostFromSystem = System.getProperty("DB_HOST");
        String dbHostFromEnv = System.getenv("DB_HOST");
        String dbHost = firstNonBlank(dbHostFromSystem, dbHostFromEnv, "localhost").trim();
        if ("postgres".equalsIgnoreCase(dbHost)) {
            System.setProperty("DB_HOST", "localhost");
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
