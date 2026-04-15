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
import com.pucpr.utils.LogUtils;
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
        final String route = "/users";

        try {
            LogUtils.info(route, "Requisição recebida. Method=" + exchange.getRequestMethod());

            if (!"GET".equals(exchange.getRequestMethod())) {
                LogUtils.error(route, "Método não permitido: " + exchange.getRequestMethod());
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            LogUtils.info(route, "Authorization=" + authorization);

            Optional<String> token = extractBearerToken(exchange);
            if (token.isEmpty()) {
                LogUtils.error(route, "Token ausente.");
                writeJson(exchange, 401, Map.of("error", "Token ausente."));
                return;
            }

            String jwt = token.get();
            LogUtils.info(route, "Token extraído com sucesso. Tamanho=" + jwt.length());

            if (!jwtService.validateToken(jwt)) {
                LogUtils.error(route, "Token inválido ou expirado.");
                writeJson(exchange, 401, Map.of("error", "Token inválido ou expirado."));
                return;
            }

            String role = jwtService.extractRole(jwt);
            LogUtils.info(route, "Role extraída do token=" + role);

            if (role == null || !"ADMIN".equalsIgnoreCase(role)) {
                LogUtils.error(route, "Acesso negado para role: " + role);
                writeJson(exchange, 403, Map.of("error", "Acesso restrito a administradores."));
                return;
            }

            List<Usuario> allUsers = repository.findAll();
            LogUtils.info(route, "Total de usuários encontrados no repositório=" + allUsers.size());

            List<Map<String, Object>> users = new ArrayList<>();
            for (Usuario usuario : allUsers) {
                Map<String, Object> safeUser = new LinkedHashMap<>();
                safeUser.put("id", usuario.getId());
                safeUser.put("nome", usuario.getNome());
                safeUser.put("email", usuario.getEmail());
                safeUser.put("tipo", usuario.getTipo());
                users.add(safeUser);
            }

            LogUtils.info(route, "Usuários preparados para resposta=" + users.size());
            LogUtils.info(route, "Lista retornada para ADMIN.");
            writeJson(exchange, 200, Map.of("users", users));

        } catch (IOException | IllegalArgumentException e) {
            LogUtils.error(route, e.getMessage());
            sendJson(exchange, route, 500, "{\"error\":\"Erro interno do servidor\"}");
        } catch (RuntimeException e) {
            LogUtils.error(route, e.getMessage());
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
            LogUtils.error(route, "Retornando: " + json);
        } else {
            LogUtils.info(route, "Retornando: " + json);
        }
    }
}