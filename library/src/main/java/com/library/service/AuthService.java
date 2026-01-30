package com.library.service;

import com.library.dao.UserDao;
import com.library.model.User;

public class AuthService {

    private final UserDao userDao = new UserDao();

    public User login(String username, String password) throws Exception {
        // Pastikan: di DB kamu password disimpan plain / hash?
        // Dari screenshot kamu: password_hash berisi plain (admin123 dll),
        // jadi sementara compare langsung.
        return userDao.findByUsernameAndPassword(username, password);
    }
}
