package com.library.service;

import com.library.dao.UserDao;
import com.library.model.Role;
import com.library.model.User;

import java.util.List;

public class AdminService {

    private final UserDao userDao = new UserDao();

    public List<User> getUsers(String keyword) throws Exception {
        return userDao.findUsers(keyword);
    }

    public List<Role> getRoles() throws Exception {
        return userDao.findRoles();
    }

    public void addUser(User u) throws Exception {
        validate(u, true);
        userDao.insertUser(u);
    }

    public void updateUser(User u) throws Exception {
        validate(u, false);
        userDao.updateUser(u);
    }

    public void deleteUser(int userId) throws Exception {
        if (userId <= 0) throw new IllegalArgumentException("User tidak valid.");
        userDao.deleteUser(userId);
    }

    private void validate(User u, boolean isCreate) {
        if (u.getUsername() == null || u.getUsername().trim().isEmpty())
            throw new IllegalArgumentException("Username wajib diisi.");
        if (u.getFullName() == null || u.getFullName().trim().isEmpty())
            throw new IllegalArgumentException("Nama lengkap wajib diisi.");
        if (u.getRoleId() <= 0)
            throw new IllegalArgumentException("Role wajib dipilih.");

        // password wajib saat tambah
        if (isCreate) {
            if (u.getPasswordHash() == null || u.getPasswordHash().trim().isEmpty())
                throw new IllegalArgumentException("Password wajib diisi.");
        }
    }
}
