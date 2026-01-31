package com.library.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.library.config.Db;
import com.library.model.Loan;

public class LoanDao {

    public List<Loan> findLoansByUser(int userId) throws Exception {
        String sql = """
            SELECT l.id, l.user_id, l.book_id, b.title AS book_title,
                   l.loan_date, l.due_date, l.return_date, l.status, l.fine
            FROM loans l
            JOIN books b ON b.id = l.book_id
            WHERE l.user_id = ?
            ORDER BY l.id DESC
        """;
        List<Loan> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Loan(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getInt("book_id"),
                            rs.getString("book_title"),
                            rs.getDate("loan_date").toLocalDate(),
                            rs.getDate("due_date").toLocalDate(),
                            rs.getDate("return_date") == null ? null : rs.getDate("return_date").toLocalDate(),
                            rs.getString("status"),
                            rs.getInt("fine")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Mark all active loans (no return_date) whose due_date is before today as TERLAMBAT.
     * This is a proactive update so controllers can show notifications.
     */
    public void markOverdueLoans() throws Exception {
        String sql = """
            UPDATE loans
            SET status = 'TERLAMBAT'
            WHERE return_date IS NULL
              AND due_date < CURRENT_DATE
              AND status <> 'TERLAMBAT'
        """;

        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    /**
     * Count how many active overdue loans a user currently has.
     */
    public int countOverdueByUser(int userId) throws Exception {
        String sql = """
            SELECT COUNT(*) AS cnt
            FROM loans
            WHERE user_id = ?
              AND return_date IS NULL
              AND due_date < CURRENT_DATE
        """;

        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("cnt");
                return 0;
            }
        }
    }

    // Untuk pustakawan: lihat semua yang belum kembali
    public List<Loan> findActiveLoans() throws Exception {
        String sql = """
            SELECT l.id, l.user_id, l.book_id, b.title AS book_title,
                   l.loan_date, l.due_date, l.return_date, l.status, l.fine
            FROM loans l
            JOIN books b ON b.id = l.book_id
            WHERE l.return_date IS NULL
            ORDER BY l.due_date ASC
        """;
        List<Loan> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Loan(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("book_id"),
                        rs.getString("book_title"),
                        rs.getDate("loan_date").toLocalDate(),
                        rs.getDate("due_date").toLocalDate(),
                        null,
                        rs.getString("status"),
                        rs.getInt("fine")
                ));
            }
        }
        return list;
    }

    // TRANSAKSI: Pinjam = cek stok, insert loan, kurangi stok
    public void borrowBookTx(int userId, int bookId, int daysDue) throws Exception {
        try (Connection conn = Db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int stock = getStockForUpdate(conn, bookId);
                if (stock <= 0) throw new IllegalStateException("Stok buku habis.");

                LocalDate loanDate = LocalDate.now();
                LocalDate dueDate = loanDate.plusDays(daysDue);

                try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO loans(user_id, book_id, loan_date, due_date, status, fine)
                    VALUES (?, ?, ?, ?, 'DIPINJAM', 0)
                """)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, bookId);
                    ps.setDate(3, Date.valueOf(loanDate));
                    ps.setDate(4, Date.valueOf(dueDate));
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE books SET stock = stock - 1 WHERE id = ?
                """)) {
                    ps.setInt(1, bookId);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // TRANSAKSI: Kembali = set return_date, hitung telat+fine, update status, tambah stok
    public void returnBookTx(int loanId, int finePerDay) throws Exception {
        try (Connection conn = Db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Ambil loan (lock)
                LoanRow row = getLoanForUpdate(conn, loanId);
                if (row == null) throw new IllegalStateException("Data peminjaman tidak ditemukan.");
                if (row.returnDate != null) throw new IllegalStateException("Buku ini sudah dikembalikan.");

                LocalDate today = LocalDate.now();
                long lateDays = java.time.temporal.ChronoUnit.DAYS.between(row.dueDate, today);
                int fine = (lateDays > 0) ? (int) lateDays * finePerDay : 0;
                String status = (lateDays > 0) ? "TERLAMBAT" : "DIKEMBALIKAN";

                try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE loans
                    SET return_date=?, status=?, fine=?
                    WHERE id=?
                """)) {
                    ps.setDate(1, Date.valueOf(today));
                    ps.setString(2, status);
                    ps.setInt(3, fine);
                    ps.setInt(4, loanId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE books SET stock = stock + 1 WHERE id=?
                """)) {
                    ps.setInt(1, row.bookId);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private int getStockForUpdate(Connection conn, int bookId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT stock FROM books WHERE id=? FOR UPDATE")) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Buku tidak ditemukan.");
                return rs.getInt("stock");
            }
        }
    }

    private static class LoanRow {
        int bookId;
        LocalDate dueDate;
        LocalDate returnDate;
        LoanRow(int bookId, LocalDate dueDate, LocalDate returnDate) {
            this.bookId = bookId; this.dueDate = dueDate; this.returnDate = returnDate;
        }
    }

    private LoanRow getLoanForUpdate(Connection conn, int loanId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT book_id, due_date, return_date
            FROM loans
            WHERE id=? FOR UPDATE
        """)) {
            ps.setInt(1, loanId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new LoanRow(
                        rs.getInt("book_id"),
                        rs.getDate("due_date").toLocalDate(),
                        rs.getDate("return_date") == null ? null : rs.getDate("return_date").toLocalDate()
                );
            }
        }
    }
}
