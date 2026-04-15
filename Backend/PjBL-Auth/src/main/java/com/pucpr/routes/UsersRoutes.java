package com.pucpr.routes;

import com.pucpr.handlers.UsersHandler;
import com.pucpr.middleware.CorsMiddleware;
import com.sun.net.httpserver.HttpServer;

public final class UsersRoutes {

    private UsersRoutes() {}

    public static void register(HttpServer server, UsersHandler usersHandler) {
        server.createContext("/api/users", CorsMiddleware.withCors(usersHandler::handleListUsers));
    }
}