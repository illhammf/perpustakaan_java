package com.library.service;

import com.library.dao.BookDao;
import com.library.dao.LoanDao;
import com.library.model.Book;
import com.library.model.Loan;

import java.util.List;

public class LibraryService {
    public static final int DUE_DAYS = 7;      // deadline 7 hari
    public static final int FINE_PER_DAY = 1000; // denda per hari

    private final BookDao bookDao = new BookDao();
    private final LoanDao loanDao = new LoanDao();

    // BOOKS
    public List<Book> getBooks(String keyword) throws Exception {
        return bookDao.findAll(keyword);
    }

    public void addBook(Book b) throws Exception {
        validateBook(b);
        bookDao.insert(b);
    }

    public void updateBook(Book b) throws Exception {
        validateBook(b);
        if (b.getId() <= 0) throw new IllegalArgumentException("Pilih buku yang mau di-edit.");
        bookDao.update(b);
    }

    public void deleteBook(int id) throws Exception {
        if (id <= 0) throw new IllegalArgumentException("Pilih buku yang mau dihapus.");
        bookDao.deleteById(id);
    }

    private void validateBook(Book b) {
        if (b.getTitle() == null || b.getTitle().isBlank()) throw new IllegalArgumentException("Judul wajib diisi.");
        if (b.getAuthor() == null || b.getAuthor().isBlank()) throw new IllegalArgumentException("Penulis wajib diisi.");
        if (b.getStock() < 0) throw new IllegalArgumentException("Stok tidak boleh negatif.");
    }

    // LOANS
    public void borrowBook(int userId, int bookId) throws Exception {
        loanDao.borrowBookTx(userId, bookId, DUE_DAYS);
    }

    public void returnBook(int loanId) throws Exception {
        loanDao.returnBookTx(loanId, FINE_PER_DAY);
    }

    public List<Loan> getLoansByUser(int userId) throws Exception {
        return loanDao.findLoansByUser(userId);
    }

    public List<Loan> getActiveLoans() throws Exception {
        return loanDao.findActiveLoans();
    }
}
