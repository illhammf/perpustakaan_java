package com.library.dao;

import com.library.config.Db;
import com.library.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDao {

    public User findByUsernameAndPassword(String username, String password) throws Exception {
        String sql = """
            SELECT u.id, u.username, u.full_name, r.name AS role_name
            FROM users u
            JOIN roles r ON r.id = u.role_id
            WHERE u.username = ? AND u.password_hash = ?
        """;

        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("full_name"),
                            rs.getString("role_name")
                    );
                }
                return null;
            }
        }
    }
}
