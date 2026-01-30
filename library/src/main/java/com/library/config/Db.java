package com.library.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Db {
    // GANTI sesuai setting postgres kamu
    private static final String URL = "jdbc:postgresql://localhost:5432/perpustakaan_kampus";
    private static final String USER = "postgres";
    private static final String PASS = "ilham1023";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
