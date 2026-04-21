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

            String method = exchange.getRequestMethod();
            if ("GET".equals(method) && "/api/users".equals(exchange.getRequestURI().getPath())) {
                handleGetUsers(exchange, route);
                return;
            }

            if ("DELETE".equals(method)) {
                handleDeleteUser(exchange, route);
                return;
            }

            LogUtils.error(route, "Método não permitido: " + exchange.getRequestMethod());
            exchange.sendResponseHeaders(405, -1);

        } catch (IOException | IllegalArgumentException e) {
            LogUtils.error(route, e.getMessage());
            sendJson(exchange, route, 500, "{\"error\":\"Erro interno do servidor\"}");
        } catch (RuntimeException e) {
            LogUtils.error(route, e.getMessage());
            writeJson(exchange, 500, Map.of("error", "Erro interno do servidor"));
        }
    }

    private void handleGetUsers(HttpExchange exchange, String route) throws IOException {
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
    }

    private void handleDeleteUser(HttpExchange exchange, String route) throws IOException {
        Optional<String> token = extractBearerToken(exchange);
        if (token.isEmpty()) {
            LogUtils.error(route, "Token ausente para exclusão.");
            writeJson(exchange, 401, Map.of("error", "Token ausente."));
            return;
        }

        String jwt = token.get();
        if (!jwtService.validateToken(jwt)) {
            LogUtils.error(route, "Token inválido ou expirado para exclusão.");
            writeJson(exchange, 401, Map.of("error", "Token inválido ou expirado."));
            return;
        }

        String role = jwtService.extractRole(jwt);
        if (role == null || !"ADMIN".equalsIgnoreCase(role)) {
            LogUtils.error(route, "Acesso negado para exclusão. role=" + role);
            writeJson(exchange, 403, Map.of("error", "Acesso restrito a administradores."));
            return;
        }

        Optional<String> targetId = extractUserIdFromPath(exchange);
        if (targetId.isEmpty()) {
            writeJson(exchange, 400, Map.of("error", "ID do usuário inválido."));
            return;
        }

        Optional<Usuario> targetUser = repository.findById(targetId.get());
        if (targetUser.isEmpty()) {
            writeJson(exchange, 404, Map.of("error", "Usuário não encontrado."));
            return;
        }

        String requesterEmail = jwtService.extractEmail(jwt);
        String targetEmail = targetUser.get().getEmail();
        if (requesterEmail != null && requesterEmail.equalsIgnoreCase(targetEmail)) {
            LogUtils.error(route, "Tentativa de autoexclusão bloqueada para " + requesterEmail);
            writeJson(exchange, 400, Map.of("error", "Você não pode excluir seu próprio usuário."));
            return;
        }

        boolean removed = repository.deleteById(targetId.get());
        if (!removed) {
            writeJson(exchange, 404, Map.of("error", "Usuário não encontrado."));
            return;
        }

        LogUtils.info(route, "Usuário removido. id=" + targetId.get());
        writeJson(exchange, 200, Map.of("message", "Usuário excluído com sucesso."));
    }

    private Optional<String> extractUserIdFromPath(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String basePath = "/api/users/";
        if (path == null || !path.startsWith(basePath) || path.length() <= basePath.length()) {
            return Optional.empty();
        }

        String id = path.substring(basePath.length()).trim();
        if (id.contains("/")) {
            return Optional.empty();
        }

        return id.isBlank() ? Optional.empty() : Optional.of(id);
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