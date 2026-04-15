package com.pucpr.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pucpr.model.Usuario;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonUsuarioRepository {

    private final String FILE_PATH = "data/usuarios.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonUsuarioRepository() {
        File file = new File(FILE_PATH);

        if (!file.exists()) {
            try {
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                objectMapper.writeValue(file, new ArrayList<Usuario>());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private List<Usuario> loadUsers() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try {
            List<Usuario> users = objectMapper.readValue(file, new TypeReference<List<Usuario>>() {});
            return users != null ? users : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
}
