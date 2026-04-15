package com.pucpr.handlers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pucpr.model.Usuario;
import com.pucpr.repository.UsuarioRepository;
import com.pucpr.service.JwtService;
import com.sun.net.httpserver.HttpExchange;

public class UsersHandler {
    private final UsuarioRepository repository;
    private final JwtService jwtService;
    private final ObjectMapper mapper = new ObjectMapper();

    public UsersHandler(UsuarioRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    public void handleListUsers(HttpExchange exchange) throws IOException {
        final String route = "[/users] ";

        try {
            System.out.println(route + "Requisição recebida. Method=" + exchange.getRequestMethod());

            if (!"GET".equals(exchange.getRequestMethod())) {
                System.out.println(route + "Erro. Método não permitido: " + exchange.getRequestMethod());
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            System.out.println(route + "Authorization=" + authorization);

            Optional<String> token = extractBearerToken(exchange);
            if (token.isEmpty()) {
                System.out.println(route + "Erro. Token ausente.");
                writeJson(exchange, 401, Map.of("error", "Token ausente."));
                return;
            }

            String jwt = token.get();
            System.out.println(route + "Token extraído com sucesso. Tamanho=" + jwt.length());

            if (!jwtService.validateToken(jwt)) {
                System.out.println(route + "Erro. Token inválido ou expirado.");
                writeJson(exchange, 401, Map.of("error", "Token inválido ou expirado."));
                return;
            }

            String role = jwtService.extractRole(jwt);
            System.out.println(route + "Role extraída do token=" + role);

            if (role == null || !"ADMIN".equalsIgnoreCase(role)) {
                System.out.println(route + "Erro. Acesso negado para role: " + role);
                writeJson(exchange, 403, Map.of("error", "Acesso restrito a administradores."));
                return;
            }

            List<Usuario> allUsers = repository.findAll();
            System.out.println(route + "Total de usuários encontrados no repositório=" + allUsers.size());

            List<Map<String, Object>> users = new ArrayList<>();
            for (Usuario usuario : allUsers) {
                Map<String, Object> safeUser = new LinkedHashMap<>();
                safeUser.put("id", usuario.getId());
                safeUser.put("nome", usuario.getNome());
                safeUser.put("email", usuario.getEmail());
                safeUser.put("tipo", usuario.getTipo());
                users.add(safeUser);
            }

            System.out.println(route + "Usuários preparados para resposta=" + users.size());
            System.out.println(route + "Sucesso. Lista retornada para ADMIN.");
            writeJson(exchange, 200, Map.of("users", users));

        } catch (IOException | IllegalArgumentException e) {
            System.out.println(route + "Erro. " + e.getMessage());
            sendJson(exchange, route, 500, "{\"error\":\"Erro interno do servidor\"}");
        } catch (RuntimeException e) {
            System.out.println(route + "Erro. " + e.getMessage());
            writeJson(exchange, 500, Map.of("error", "Erro interno do servidor"));
        }
    }

    private void writeJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] response = mapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private Optional<String> extractBearerToken(HttpExchange exchange) {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Optional.empty();
        }

        String token = authorization.substring(7).trim();
        return token.isBlank() ? Optional.empty() : Optional.of(token);
    }

    private void sendJson(HttpExchange exchange, String route, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }

        if (statusCode >= 400) {
            System.out.println("[" + route + "] Erro. Retornando: " + json);
        } else {
            System.out.println("[" + route + "] Sucesso. Retornando: " + json);
        }
    }
}