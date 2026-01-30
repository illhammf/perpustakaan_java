package com.library.model;

public class User {
    private final int id;
    private final String username;
    private final String fullName;
    private final String roleName;

    public User(int id, String username, String fullName, String roleName) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.roleName = roleName;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRoleName() { return roleName; }
}
