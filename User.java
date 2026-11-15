package model;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;

public abstract class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String username;
    private final String password;

    //Set login details
    protected User(String username, String password) {
        this.username = requireText(username, "Username");
        requireText(password, "Password");
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

    //Return the user role
    public abstract String getRole();

    //Check required text
    static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required.");
        }

        return value.trim();
    }

    //Check the email format
    static String requireEmail(String email) {
        email = requireText(email, "Email");

        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Enter a valid email.");
        }

        return email;
    }

    //Check the phone format
    static String requirePhone(String phone) {
        phone = requireText(phone, "Phone");
        String digits = phone.replaceAll("[^0-9]", "");

        if (!phone.matches("\\+?[0-9 ()-]+")
                || digits.length() < 7
                || digits.length() > 15) {
            throw new IllegalArgumentException(
                    "Enter a valid phone number.");
        }

        return phone;
    }

    //Check simulated time
    static LocalDateTime requireTime(LocalDateTime time) {
        if (time == null
                || time.getSecond() != 0
                || time.getNano() != 0) {
            throw new IllegalArgumentException(
                    "Use simulation time in whole minutes.");
        }

        return time;
    }

    //Calculate elapsed minutes
    static long minutesBetween(LocalDateTime from, LocalDateTime to) {
        requireTime(from);
        requireTime(to);

        if (to.isBefore(from)) {
            throw new IllegalArgumentException(
                    "Time cannot move backwards.");
        }

        return Duration.between(from, to).toMinutes();
    }
}
