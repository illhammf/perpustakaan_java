package com.library.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.library.config.Db;
import com.library.model.Book;

public class BookDao {

    public List<Book> findAll() throws Exception {
        String sql = "SELECT id, title, author, stock, cover_path, description FROM books ORDER BY id ASC";
        List<Book> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("stock"),
                        rs.getString("cover_path"),
                        rs.getString("description")
                ));
            }
        }
        return list;
    }

    public List<Book> searchByTitle(String keyword) throws Exception {
        String sql = "SELECT id, title, author, stock, cover_path, description FROM books WHERE lower(title) LIKE ? ORDER BY id ASC";
        List<Book> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword.toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Book(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("author"),
                            rs.getInt("stock"),
                            rs.getString("cover_path"),
                            rs.getString("description")
                    ));
                }
            }
        }
        return list;
    }

    public void insert(Book b) throws Exception {
        String sql = "INSERT INTO books(title, author, stock, cover_path, description) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getTitle());
            ps.setString(2, b.getAuthor());
            ps.setInt(3, b.getStock());
            ps.setString(4, b.getCoverPath());
            ps.setString(5, b.getDescription());
            ps.executeUpdate();
        }
    }

    public void update(Book b) throws Exception {
        String sql = "UPDATE books SET title=?, author=?, stock=?, cover_path=?, description=? WHERE id=?";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getTitle());
            ps.setString(2, b.getAuthor());
            ps.setInt(3, b.getStock());
            ps.setString(4, b.getCoverPath());
            ps.setString(5, b.getDescription());
            ps.setInt(6, b.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws Exception {
        String sql = "DELETE FROM books WHERE id=?";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
