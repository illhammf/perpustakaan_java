package com.library.service;

import com.library.dao.UserDao;
import com.library.model.User;

public class AuthService {
    private final UserDao userDao = new UserDao();

    public User login(String username, String password) throws Exception {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username dan password wajib diisi.");
        }
        return userDao.findByUsernameAndPassword(username.trim(), password);
    }
}
