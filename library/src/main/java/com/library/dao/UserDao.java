package com.library.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.library.config.Db;
import com.library.model.Role;
import com.library.model.User;

public class UserDao {

    public User findByUsernameAndPassword(String username, String password) throws Exception {
        String sql =
            "SELECT u.id, u.username, u.full_name, u.password_hash, r.name AS role " +
            "FROM users u JOIN roles r ON r.id = u.role_id " +
            "WHERE u.username = ? AND u.password_hash = ?";

        try (Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setFullName(rs.getString("full_name"));
                u.setPasswordHash(rs.getString("password_hash"));
                u.setRoleName(rs.getString("role")); // pastikan ada field roleName
                return u;
            }
        }
    }


    // ADMIN: list users
    public List<User> findUsers(String keyword) throws Exception {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        String sql = hasKeyword ? """
            SELECT u.id, u.username, u.full_name, u.role_id, r.name AS role_name
            FROM users u JOIN roles r ON r.id=u.role_id
            WHERE LOWER(u.username) LIKE ? OR LOWER(u.full_name) LIKE ?
            ORDER BY u.id ASC
        """ : """
            SELECT u.id, u.username, u.full_name, u.role_id, r.name AS role_name
            FROM users u JOIN roles r ON r.id=u.role_id
            ORDER BY u.id ASC
        """;

        List<User> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (hasKeyword) {
                String k = "%" + keyword.toLowerCase().trim() + "%";
                ps.setString(1, k);
                ps.setString(2, k);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setFullName(rs.getString("full_name"));
                    u.setRoleId(rs.getInt("role_id"));
                    u.setRoleName(rs.getString("role_name"));
                    list.add(u);
                }
            }
        }
        return list;
    }

    // ADMIN: roles
    public List<Role> findRoles() throws Exception {
        String sql = "SELECT id, name FROM roles ORDER BY id ASC";
        List<Role> roles = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) roles.add(new Role(rs.getInt("id"), rs.getString("name")));
        }
        return roles;
    }

    // ADMIN: insert user
    public void insertUser(User u) throws Exception {
        String sql = """
            INSERT INTO users(username, password_hash, full_name, role_id)
            VALUES (?, ?, ?, ?)
        """;
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getUsername().trim());
            ps.setString(2, u.getPasswordHash()); // plain (sesuai proyek kamu)
            ps.setString(3, u.getFullName().trim());
            ps.setInt(4, u.getRoleId());
            ps.executeUpdate();
        }
    }

    // ADMIN: update user (password optional)
    public void updateUser(User u) throws Exception {
        boolean changePass = u.getPasswordHash() != null && !u.getPasswordHash().isBlank();

        String sql = changePass ? """
            UPDATE users
            SET username=?, password_hash=?, full_name=?, role_id=?
            WHERE id=?
        """ : """
            UPDATE users
            SET username=?, full_name=?, role_id=?
            WHERE id=?
        """;

        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int i = 1;
            ps.setString(i++, u.getUsername().trim());
            if (changePass) ps.setString(i++, u.getPasswordHash());
            ps.setString(i++, u.getFullName().trim());
            ps.setInt(i++, u.getRoleId());
            ps.setInt(i, u.getId());

            ps.executeUpdate();
        }
    }

    // ADMIN: delete user
    public void deleteUser(int id) throws Exception {
        String sql = "DELETE FROM users WHERE id=?";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
