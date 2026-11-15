package model;

import java.io.Serializable;

//Parent class for students and administrators
public abstract class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;
    private String password;

    //Set login details
    public User(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }

        this.username = username.trim();
        this.password = password;
    }

    //Return the username
    public String getUsername() {
        return username;
    }

    //Check the entered password
    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    //Child classes provide their role
    public abstract String getRole();
}