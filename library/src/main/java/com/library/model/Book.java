package com.library.model;

public class Book {
    private int id;
    private String title;
    private String author;
    private int stock;
    private String coverPath;
    private String description;

    public Book() {}

    public Book(int id, String title, String author, int stock, String coverPath, String description) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.stock = stock;
        this.coverPath = coverPath;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getCoverPath() { return coverPath; }
    public void setCoverPath(String coverPath) { this.coverPath = coverPath; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
