package com.pucpr.middleware;

import java.io.IOException;

import com.pucpr.config.ServerConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public final class CorsMiddleware {

    private CorsMiddleware() {}

    public static HttpHandler withCors(HttpHandler next) {
        return (HttpExchange exchange) -> {
            try (HttpExchange ex = exchange) {
                addCorsHeaders(ex);

                if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                    ex.sendResponseHeaders(204, -1);
                    return;
                }

                next.handle(ex);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                exchange.sendResponseHeaders(500, -1);
            }
        };
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", ServerConfig.ALLOWED_ORIGIN);
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().add("Access-Control-Max-Age", "86400");
    }
}