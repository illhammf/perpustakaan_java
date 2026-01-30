package com.library.dao;

import com.library.config.Db;
import com.library.model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDao {

    public List<Book> findAll(String keyword) throws Exception {
        String base = """
            SELECT id, isbn, title, author, publisher, publish_year, category, stock
            FROM books
        """;
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        String sql = hasKeyword ? base + " WHERE LOWER(title) LIKE ? OR LOWER(author) LIKE ? ORDER BY id DESC"
                                : base + " ORDER BY id DESC";

        List<Book> result = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (hasKeyword) {
                String k = "%" + keyword.toLowerCase().trim() + "%";
                ps.setString(1, k);
                ps.setString(2, k);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Book(
                            rs.getInt("id"),
                            rs.getString("isbn"),
                            rs.getString("title"),
                            rs.getString("author"),
                            rs.getString("publisher"),
                            (Integer) rs.getObject("publish_year"),
                            rs.getString("category"),
                            rs.getInt("stock")
                    ));
                }
            }
        }
        return result;
    }

    public void insert(Book b) throws Exception {
        String sql = """
            INSERT INTO books(isbn, title, author, publisher, publish_year, category, stock)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, blankToNull(b.getIsbn()));
            ps.setString(2, b.getTitle());
            ps.setString(3, b.getAuthor());
            ps.setString(4, blankToNull(b.getPublisher()));
            if (b.getPublishYear() == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, b.getPublishYear());
            ps.setString(6, blankToNull(b.getCategory()));
            ps.setInt(7, b.getStock());
            ps.executeUpdate();
        }
    }

    public void update(Book b) throws Exception {
        String sql = """
            UPDATE books SET isbn=?, title=?, author=?, publisher=?, publish_year=?, category=?, stock=?
            WHERE id=?
        """;
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, blankToNull(b.getIsbn()));
            ps.setString(2, b.getTitle());
            ps.setString(3, b.getAuthor());
            ps.setString(4, blankToNull(b.getPublisher()));
            if (b.getPublishYear() == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, b.getPublishYear());
            ps.setString(6, blankToNull(b.getCategory()));
            ps.setInt(7, b.getStock());
            ps.setInt(8, b.getId());
            ps.executeUpdate();
        }
    }

    public void deleteById(int id) throws Exception {
        String sql = "DELETE FROM books WHERE id=?";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
