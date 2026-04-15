package com.pucpr;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.pucpr.config.ServerConfig;
import com.pucpr.handlers.AuthHandler;
import com.pucpr.handlers.UsersHandler;
import com.pucpr.repository.UsuarioRepository;
import com.pucpr.routes.AuthRoutes;
import com.pucpr.routes.UsersRoutes;
import com.pucpr.service.JwtService;
import com.pucpr.utils.LogUtils;
import com.sun.net.httpserver.HttpServer;

public class Main {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(ServerConfig.PORT), 0);

        UsuarioRepository repository = new UsuarioRepository();
        JwtService jwtService = new JwtService();
        AuthHandler authHandler = new AuthHandler(repository, jwtService);
        UsersHandler usersHandler = new UsersHandler(repository, jwtService);

        AuthRoutes.register(server, authHandler);
        UsersRoutes.register(server, usersHandler);

        server.setExecutor(null);
        LogUtils.info("server", "Servidor iniciado na porta " + ServerConfig.PORT);
        server.start();
    }
}