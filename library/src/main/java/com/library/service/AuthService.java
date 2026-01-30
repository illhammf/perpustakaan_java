package com.library.service;

import com.library.dao.UserDao;
import com.library.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class AuthService {
    private final UserDao userDao = new UserDao();

    public User login(String username, String password) throws Exception {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username dan password wajib diisi.");
        }

        String trimmed = username.trim();

        // First try hashed password (if DB stores hashes). If not found, fallback to raw password.
        String hashed = sha256Hex(password);
        User u = userDao.findByUsernameAndPassword(trimmed, hashed);
        if (u != null) return u;

        // fallback: try raw password (in case DB stores plain passwords)
        return userDao.findByUsernameAndPassword(trimmed, password);
    }

    private String sha256Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
