package com.pucpr.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pucpr.model.Usuario;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioRepository {

    private static final String FILE_PATH = "data/usuarios.json";
    private final ObjectMapper mapper = new ObjectMapper();
    private final File file = new File(FILE_PATH);

    public UsuarioRepository() {
        ensureStorageFile();
    }

    private void ensureStorageFile() {
        if (file.exists()) {
            return;
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, new ArrayList<Usuario>());
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível criar o arquivo de armazenamento: " + e.getMessage(), e);
        }
    }

    public Optional<Usuario> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return findAll().stream()
                .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    public Optional<Usuario> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        return findAll().stream()
                .filter(u -> u.getId() != null && u.getId().equals(id))
                .findFirst();
    }

    public List<Usuario> findAll() {
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try {
            return mapper.readValue(file, new TypeReference<List<Usuario>>() {
            });
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public void save(Usuario usuario) throws IOException {
        List<Usuario> usuarios = findAll();

        boolean emailJaExiste = usuarios.stream()
                .anyMatch(u -> u.getEmail() != null
                        && usuario.getEmail() != null
                        && u.getEmail().equalsIgnoreCase(usuario.getEmail()));

        if (emailJaExiste) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }

        usuarios.add(usuario);

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        mapper.writerWithDefaultPrettyPrinter().writeValue(file, usuarios);
    }

    public boolean deleteById(String id) throws IOException {
        if (id == null || id.isBlank()) {
            return false;
        }

        List<Usuario> usuarios = findAll();
        boolean removed = usuarios.removeIf(u -> u.getId() != null && u.getId().equals(id));
        if (!removed) {
            return false;
        }

        mapper.writerWithDefaultPrettyPrinter().writeValue(file, usuarios);
        return true;
    }
}