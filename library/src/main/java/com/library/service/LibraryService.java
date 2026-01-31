package com.library.service;

import java.time.LocalDate;
import java.util.List;

import com.library.dao.BookDao;
import com.library.dao.LoanDao;
import com.library.model.Book;
import com.library.model.Loan;

public class LibraryService {

    private final BookDao bookDao = new BookDao();
    private final LoanDao loanDao = new LoanDao();

    // ==== BOOKS ====
    public List<Book> getAllBooks() throws Exception {
        return bookDao.findAll();
    }

    public List<Book> searchBooksByTitle(String keyword) throws Exception {
        if (keyword == null || keyword.isBlank()) return bookDao.findAll();
        return bookDao.searchByTitle(keyword);
    }

    public void addBook(Book b) throws Exception {
        validateBook(b);
        bookDao.insert(b);
    }

    public void updateBook(Book b) throws Exception {
        if (b == null || b.getId() <= 0) throw new IllegalArgumentException("ID buku tidak valid.");
        validateBook(b);
        bookDao.update(b);
    }

    public void deleteBook(int id) throws Exception {
        if (id <= 0) throw new IllegalArgumentException("ID buku tidak valid.");
        bookDao.delete(id);
    }

    private void validateBook(Book b) {
        if (b == null) throw new IllegalArgumentException("Data buku kosong.");
        if (b.getTitle() == null || b.getTitle().isBlank()) throw new IllegalArgumentException("Judul wajib diisi.");
        if (b.getAuthor() == null || b.getAuthor().isBlank()) throw new IllegalArgumentException("Penulis wajib diisi.");
        if (b.getStock() < 0) throw new IllegalArgumentException("Stok tidak boleh negatif.");
    }

    // ==== LOANS ====
    public List<Loan> getLoansByUser(int userId) throws Exception {
        loanDao.markOverdueLoans();
        return loanDao.findLoansByUser(userId);
    }

    public List<Loan> getActiveLoans() throws Exception {
        loanDao.markOverdueLoans();
        return loanDao.findActiveLoans();
    }

    public void updateOverdueStatus() throws Exception {
        loanDao.markOverdueLoans();
    }

    public int getOverdueCountForUser(int userId) throws Exception {
        return loanDao.countOverdueByUser(userId);
    }

    public void borrowBook(int userId, int bookId, int daysDue) throws Exception {
        if (userId <= 0) throw new IllegalArgumentException("User tidak valid.");
        if (bookId <= 0) throw new IllegalArgumentException("Buku tidak valid.");
        if (daysDue <= 0) throw new IllegalArgumentException("Durasi pinjam tidak valid.");
        loanDao.borrowBookTx(userId, bookId, daysDue);
    }

    public void returnBook(int loanId, int finePerDay) throws Exception {
        if (loanId <= 0) throw new IllegalArgumentException("Loan ID tidak valid.");
        if (finePerDay < 0) throw new IllegalArgumentException("Fine per hari tidak valid.");
        loanDao.returnBookTx(loanId, finePerDay);
    }

    // ✅ UPDATE TANGGAL PINJAM & JATUH TEMPO
    public void updateLoanDates(int loanId, LocalDate loanDate, LocalDate dueDate) throws Exception {
        loanDao.updateLoanDates(loanId, loanDate, dueDate);
    }
}
