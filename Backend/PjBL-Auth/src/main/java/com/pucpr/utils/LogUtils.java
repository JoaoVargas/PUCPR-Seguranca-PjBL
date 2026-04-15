package com.pucpr.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class LogUtils {
    private static final Logger LOGGER = Logger.getLogger("com.pucpr");

    static {
        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.ALL);

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        consoleHandler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return record.getMessage() + System.lineSeparator();
            }
        });

        LOGGER.addHandler(consoleHandler);
    }

    private LogUtils() {
    }

    public static void info(String scope, String message) {
        LOGGER.log(Level.INFO, format("INFO", scope, message));
    }

    public static void error(String scope, String message) {
        LOGGER.log(Level.SEVERE, format("ERROR", scope, message));
    }

    private static String format(String level, String scope, String message) {
        return "[" + level + "] [" + scope + "] " + message;
    }
}