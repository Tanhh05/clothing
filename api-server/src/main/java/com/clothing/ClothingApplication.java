package com.clothing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
@EnableScheduling
public class ClothingApplication {

    public static void main(String[] args) {
        boolean runningInsideContainer = isRunningInsideContainer();
        if (!runningInsideContainer) {
            normalizeLocalDbHost();
            ensurePostgresIsRunningForLocalSqlMode();
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

    private static void ensurePostgresIsRunningForLocalSqlMode() {
        if (isPostgresReachable()) {
            return;
        }
        ensureDockerIsRunning();
        try {
            Process process = new ProcessBuilder("docker", "compose", "up", "-d", "postgres")
                    .inheritIO()
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Warning: failed to auto-start postgres via docker compose (exit code " + exitCode + ").");
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            System.err.println("Warning: failed to auto-start postgres: " + ex.getMessage());
        }
    }

    private static void ensureDockerIsRunning() {
        if (isDockerReady()) {
            return;
        }
        try {
            new ProcessBuilder("open", "-a", "Docker").inheritIO().start().waitFor(10, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            System.err.println("Warning: failed to open Docker Desktop: " + ex.getMessage());
        }
        long deadline = System.currentTimeMillis() + Duration.ofMinutes(2).toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isDockerReady()) {
                return;
            }
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        System.err.println("Warning: Docker daemon is not ready after waiting.");
    }

    private static boolean isDockerReady() {
        try {
            Process process = new ProcessBuilder("docker", "info")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private static boolean isPostgresReachable() {
        String activeProfiles = firstNonBlank(
                System.getProperty("spring.profiles.active"),
                System.getenv("SPRING_PROFILES_ACTIVE"),
                "sql-only"
        );
        if (!Arrays.stream(activeProfiles.split(","))
                .map(String::trim)
                .anyMatch("sql-only"::equalsIgnoreCase)) {
            return true;
        }
        String dbHost = firstNonBlank(System.getProperty("DB_HOST"), System.getenv("DB_HOST"), "localhost");
        int dbPort = parsePort(firstNonBlank(System.getProperty("DB_PORT"), System.getenv("DB_PORT"), "5432"));
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(dbHost, dbPort), (int) Duration.ofSeconds(1).toMillis());
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private static int parsePort(String rawPort) {
        try {
            return Integer.parseInt(rawPort);
        } catch (NumberFormatException ex) {
            return 5432;
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
