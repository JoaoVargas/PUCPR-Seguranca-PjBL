package com.pucpr.routes;

import com.pucpr.handlers.AuthHandler;
import com.pucpr.middleware.CorsMiddleware;
import com.sun.net.httpserver.HttpServer;

public final class AuthRoutes {

    private AuthRoutes() {}

    public static void register(HttpServer server, AuthHandler authHandler) {
        server.createContext("/api/auth/register", CorsMiddleware.withCors(authHandler::handleRegister));
        server.createContext("/api/auth/login", CorsMiddleware.withCors(authHandler::handleLogin));
    }
}