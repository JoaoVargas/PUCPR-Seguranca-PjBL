package com.pucpr.handlers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.mindrot.jbcrypt.BCrypt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pucpr.model.Usuario;
import com.pucpr.repository.UsuarioRepository;
import com.pucpr.service.JwtService;
import com.pucpr.utils.LogUtils;
import com.sun.net.httpserver.HttpExchange;

public class AuthHandler {
    private final UsuarioRepository repository;
    private final JwtService jwtService;
    private final ObjectMapper mapper = new ObjectMapper();

    public AuthHandler(UsuarioRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    private static class LoginRequest {
        public String email;
        public String senha;

        @Override
        public String toString() {
            return "LoginRequest{email='" + email + "', senha='" + senha + "'}";
        }
    }

    private static class RegisterRequest {
        public String nome;
        public String email;
        public String senha;
        public String tipo;

        @Override
        public String toString() {
            return "RegisterRequest{nome='" + nome + "', email='" + email + "', senha='" + senha + "', tipo='" + tipo
                    + "'}";
        }
    }

    public void handleLogin(HttpExchange exchange) throws IOException {
        final String route = "/auth/login";

        try {
            LoginRequest req = mapper.readValue(exchange.getRequestBody(), LoginRequest.class);
            LogUtils.info(route, "Requisição: " + req);

            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            if (isBlank(req.email) || isBlank(req.senha)) {
                LogUtils.error(route, "Dados de login incompletos.");

                writeJson(exchange, 400, Map.of("error", "Dados inválidos."));
                return;
            }

            Optional<Usuario> optUsuario = repository.findByEmail(req.email);

            if (optUsuario.isEmpty() || !BCrypt.checkpw(req.senha, optUsuario.get().getSenhaHash())) {
                LogUtils.error(route, "E-mail ou senha inválidos para email: " + req.email);

                writeJson(exchange, 401, Map.of("error", "E-mail ou senha inválidos."));
                return;
            }

            Usuario usuario = optUsuario.get();
            String token = gerarTokenDinamico(usuario);

            LogUtils.info(route, "Token gerado para email: " + req.email);

            writeJson(exchange, 200, Map.of("token", token));

        } catch (IOException | IllegalArgumentException e) {
            LogUtils.error(route, e.getMessage());

            sendJson(exchange, route, 500, "{\"error\":\"Erro interno do servidor\"}");
        } catch (RuntimeException e) {
            LogUtils.error(route, e.getMessage());

            writeJson(exchange, 500, Map.of("error", "Erro interno do servidor"));
        }
    }

    public void handleRegister(HttpExchange exchange) throws IOException {
        final String route = "/auth/register";

        try {
            RegisterRequest req = mapper.readValue(exchange.getRequestBody(), RegisterRequest.class);
            LogUtils.info(route, "Requisição: " + req);

            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            if (isBlank(req.nome) || isBlank(req.email) || isBlank(req.senha)) {
                LogUtils.error(route, "Dados de registro incompletos.");

                writeJson(exchange, 400, Map.of("error", "Dados inválidos."));
                return;
            }

            if (repository.findByEmail(req.email).isPresent()) {
                LogUtils.error(route, "E-mail já cadastrado.");

                writeJson(exchange, 400, Map.of("error", "E-mail já cadastrado."));
                return;
            }

            String senhaHash = BCrypt.hashpw(req.senha, BCrypt.gensalt(12));
            String tipo = isBlank(req.tipo) ? "USER" : req.tipo;

            Usuario novoUsuario = new Usuario(req.nome, req.email, senhaHash, tipo);
            repository.save(novoUsuario);

            LogUtils.info(route, "Usuário cadastrado com sucesso.");

            writeJson(exchange, 201, Map.of("message", "Usuário cadastrado com sucesso."));

        } catch (IllegalArgumentException e) {
            LogUtils.error(route, e.getMessage());
            
            sendJson(exchange, route, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (IOException | RuntimeException e) {
            LogUtils.error(route, e.getMessage());

            sendJson(exchange, route, 500, "{\"error\":\"Erro interno do servidor\"}");
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String gerarTokenDinamico(Usuario usuario) {
        try {
            Method m = jwtService.getClass().getMethod("generateToken", Usuario.class);
            return (String) m.invoke(jwtService, usuario);
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException ignored) {
        }

        try {
            Method m = jwtService.getClass().getMethod("generateToken", String.class, String.class);
            return (String) m.invoke(jwtService, usuario.getEmail(), usuario.getTipo());
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException ignored) {
        }

        try {
            Method m = jwtService.getClass().getMethod("generateToken", String.class);
            return (String) m.invoke(jwtService, usuario.getEmail());
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException ignored) {
        }

        throw new RuntimeException("Método generateToken não encontrado em JwtService.");
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
