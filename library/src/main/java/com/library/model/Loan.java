package com.library.model;

import java.time.LocalDate;

public class Loan {
    private int id;
    private int userId;
    private int bookId;
    private String bookTitle;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private String status; // DIPINJAM / DIKEMBALIKAN / TERLAMBAT
    private int fine;

    public Loan(int id, int userId, int bookId, String bookTitle,
                LocalDate loanDate, LocalDate dueDate, LocalDate returnDate,
                String status, int fine) {
        this.id = id;
        this.userId = userId;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.loanDate = loanDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
        this.fine = fine;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public int getBookId() { return bookId; }
    public String getBookTitle() { return bookTitle; }
    public LocalDate getLoanDate() { return loanDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public String getStatus() { return status; }
    public int getFine() { return fine; }
}
