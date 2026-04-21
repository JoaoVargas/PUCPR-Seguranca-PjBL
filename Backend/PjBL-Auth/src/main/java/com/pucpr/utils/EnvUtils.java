package com.pucpr.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class EnvUtils {
    private static final String JWT_SECRET = "JWT_SECRET";

    private EnvUtils() {
    }

    public static void bootstrapJwtSecret() {
        if (hasJwtSecretConfigured()) {
            return;
        }

        List<Path> candidates = List.of(
                Path.of(".env"),
                Path.of("Backend", "PjBL-Auth", ".env"));

        for (Path candidate : candidates) {
            if (!Files.exists(candidate)) {
                continue;
            }

            String value = readEnvValue(candidate, JWT_SECRET);
            if (value != null && !value.isBlank()) {
                System.setProperty(JWT_SECRET, value);
                LogUtils.info("env", "JWT_SECRET carregada de " + candidate.toString());
                return;
            }
        }
    }

    private static boolean hasJwtSecretConfigured() {
        String envSecret = System.getenv(JWT_SECRET);
        if (envSecret != null && !envSecret.isBlank()) {
            return true;
        }

        String propSecret = System.getProperty(JWT_SECRET);
        return propSecret != null && !propSecret.isBlank();
    }

    private static String readEnvValue(Path path, String key) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            String prefix = key + "=";

            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.startsWith(prefix)) {
                    continue;
                }

                String value = line.substring(prefix.length()).trim();
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }

                return value;
            }
        } catch (IOException e) {
            LogUtils.error("env", "Falha ao ler " + path + ": " + e.getMessage());
        }

        return null;
    }
}
