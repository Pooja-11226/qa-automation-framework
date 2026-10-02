package com.sdet.api.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves configuration with a fixed precedence, highest first:
 * <ol>
 *   <li>JVM system property, e.g. {@code -Dapi.base.uri=...}</li>
 *   <li>Environment variable, e.g. {@code API_BASE_URI} (CI secrets)</li>
 *   <li>Local {@code .env} file in the working directory (git-ignored, for developer machines)</li>
 *   <li>{@code config/<env>.properties} on the classpath (non-secret defaults per environment)</li>
 * </ol>
 * The environment is chosen with {@code -Denv=<name>} or {@code API_ENV}, defaulting to {@code qa}.
 */
public final class ConfigManager {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigManager.class);
    private static final String ENV_SELECTOR_PROPERTY = "env";
    private static final String ENV_SELECTOR_VARIABLE = "API_ENV";
    private static final String DEFAULT_ENVIRONMENT = "qa";
    private static final Path DOTENV_FILE = Path.of(".env");
    private static final ConfigManager INSTANCE = new ConfigManager();

    private final String environment;
    private final Properties fileProperties;
    private final Map<String, String> dotEnv;

    private ConfigManager() {
        this.environment = firstNonBlank(
                System.getProperty(ENV_SELECTOR_PROPERTY), System.getenv(ENV_SELECTOR_VARIABLE), DEFAULT_ENVIRONMENT);
        this.fileProperties = loadClasspathProperties("config/" + environment + ".properties");
        this.dotEnv = loadDotEnv(DOTENV_FILE);
        LOG.info("Loaded API configuration for environment '{}'", environment);
    }

    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    public String baseUri() {
        return getRequired(ConfigKey.BASE_URI);
    }

    public String username() {
        return getRequired(ConfigKey.USERNAME);
    }

    public String password() {
        return getRequired(ConfigKey.PASSWORD);
    }

    public int connectTimeoutMs() {
        return getInt(ConfigKey.CONNECT_TIMEOUT_MS);
    }

    public int readTimeoutMs() {
        return getInt(ConfigKey.READ_TIMEOUT_MS);
    }

    public boolean healthCheckEnabled() {
        return Boolean.parseBoolean(getRequired(ConfigKey.HEALTHCHECK_ENABLED));
    }

    public String getRequired(ConfigKey key) {
        String value = resolve(key);
        if (value == null) {
            throw new IllegalStateException(String.format(
                    "Missing required configuration '%s'. Set -D%s, the %s environment variable, "
                            + "add it to .env, or define it in config/%s.properties.",
                    key.propertyName(), key.propertyName(), key.environmentVariable(), environment));
        }
        return value;
    }

    public int getInt(ConfigKey key) {
        String value = getRequired(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    String.format("Configuration '%s' must be an integer but was '%s'", key.propertyName(), value), e);
        }
    }

    private String resolve(ConfigKey key) {
        return firstNonBlank(
                System.getProperty(key.propertyName()),
                System.getenv(key.environmentVariable()),
                dotEnv.get(key.environmentVariable()),
                fileProperties.getProperty(key.propertyName()));
    }

    private static Properties loadClasspathProperties(String resource) {
        Properties properties = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Configuration file not found on the classpath: " + resource);
            }
            properties.load(in);
            return properties;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + resource, e);
        }
    }

    /** Minimal KEY=VALUE parser; avoids a dependency for a few lines of developer convenience. */
    private static Map<String, String> loadDotEnv(Path file) {
        Map<String, String> values = new HashMap<>();
        if (!Files.isRegularFile(file)) {
            return values;
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (String rawLine : lines) {
                String line = rawLine.strip();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                if (line.startsWith("export ")) {
                    line = line.substring("export ".length()).strip();
                }
                int separator = line.indexOf('=');
                String name = line.substring(0, separator).strip();
                String value = stripQuotes(line.substring(separator + 1).strip());
                values.put(name, value);
            }
            return values;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file.toAbsolutePath(), e);
        }
    }

    private static String stripQuotes(String value) {
        boolean quoted = value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")));
        return quoted ? value.substring(1, value.length() - 1) : value;
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.strip();
            }
        }
        return null;
    }
}
