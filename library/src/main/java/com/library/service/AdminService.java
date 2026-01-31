package com.library.service;

import java.time.LocalDate;
import java.util.List;

import com.library.dao.LoanDao;
import com.library.dao.UserDao;
import com.library.model.Loan;
import com.library.model.Role;
import com.library.model.User;

public class AdminService {

    private final UserDao userDao = new UserDao();
    private final LoanDao loanDao = new LoanDao();

    // ==========================
    // USER / ROLE (ADMIN)
    // ==========================

    public List<User> getUsers(String keyword) throws Exception {
        // keyword boleh null/kosong => tampilkan semua
        return userDao.findUsers(keyword);
    }

    public List<Role> getRoles() throws Exception {
        return userDao.findRoles();
    }

    public void addUser(User u) throws Exception {
        validateUserForAdd(u);
        userDao.insertUser(u);
    }

    public void updateUser(User u) throws Exception {
        validateUserForEdit(u);
        userDao.updateUser(u);
    }

    public void deleteUser(int userId) throws Exception {
        if (userId <= 0) throw new IllegalArgumentException("ID user tidak valid.");
        userDao.deleteUser(userId);
    }

    private void validateUserForAdd(User u) {
        if (u == null) throw new IllegalArgumentException("Data user kosong.");
        if (isBlank(u.getUsername())) throw new IllegalArgumentException("Username wajib diisi.");
        if (isBlank(u.getPasswordHash())) throw new IllegalArgumentException("Password wajib diisi.");
        if (isBlank(u.getFullName())) throw new IllegalArgumentException("Nama lengkap wajib diisi.");
        if (u.getRoleId() <= 0) throw new IllegalArgumentException("Role wajib dipilih.");
    }

    private void validateUserForEdit(User u) {
        if (u == null) throw new IllegalArgumentException("Data user kosong.");
        if (u.getId() <= 0) throw new IllegalArgumentException("ID user tidak valid.");
        if (isBlank(u.getUsername())) throw new IllegalArgumentException("Username wajib diisi.");
        // password boleh kosong saat edit (berarti tidak ganti)
        if (isBlank(u.getFullName())) throw new IllegalArgumentException("Nama lengkap wajib diisi.");
        if (u.getRoleId() <= 0) throw new IllegalArgumentException("Role wajib dipilih.");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ==========================
    // LOAN (ADMIN bisa semua seperti pustakawan)
    // ==========================

    public List<Loan> getActiveLoans() throws Exception {
        loanDao.markOverdueLoans();
        return loanDao.findActiveLoans();
    }

    public void returnBook(int loanId, int finePerDay) throws Exception {
        if (loanId <= 0) throw new IllegalArgumentException("Loan ID tidak valid.");
        if (finePerDay < 0) throw new IllegalArgumentException("Denda per hari tidak valid.");
        loanDao.returnBookTx(loanId, finePerDay);
    }

    public void updateLoanDates(int loanId, LocalDate loanDate, LocalDate dueDate) throws Exception {
        if (loanId <= 0) throw new IllegalArgumentException("Loan ID tidak valid.");
        if (loanDate == null) throw new IllegalArgumentException("Tanggal pinjam tidak boleh kosong.");
        if (dueDate == null) throw new IllegalArgumentException("Tanggal jatuh tempo tidak boleh kosong.");
        if (dueDate.isBefore(loanDate)) throw new IllegalArgumentException("Jatuh tempo tidak boleh sebelum tanggal pinjam.");

        loanDao.updateLoanDates(loanId, loanDate, dueDate);
    }
}
