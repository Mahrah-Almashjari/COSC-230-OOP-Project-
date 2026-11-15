package model;

import java.io.Serializable;

public class AccessRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status {
        PENDING, APPROVED, REJECTED
    }

    private String studentId;
    private String name;
    private String email;
    private String phone;
    private String password;
    private Status status;

    //Store a new registration request
    public AccessRequest(String studentId, String name, String email,
                         String phone, String password) {
        this.studentId = requireText(studentId);
        this.name = requireText(name);
        this.email = requireText(email);
        this.phone = requireText(phone);
        requireText(password);
        this.password = password;

        if (this.studentId.equalsIgnoreCase("admin")) {
            throw new IllegalArgumentException("This username is reserved.");
        }

        if (!this.email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Enter a valid email.");
        }

        if (!this.phone.matches("\\+?[0-9]{7,15}")) {
            throw new IllegalArgumentException("Enter a valid phone number.");
        }

        status = Status.PENDING;
    }

    //Check that a field is filled
    private String requireText(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("All fields are required.");
        }

        return value.trim();
    }

    //Approve once and create the student
    Student approve() {
        if (status != Status.PENDING) {
            throw new IllegalStateException("Request already reviewed.");
        }

        Student student = new Student(
                studentId, password, name, email, phone);

        status = Status.APPROVED;
        return student;
    }

    //Reject a pending request
    void reject() {
        if (status != Status.PENDING) {
            throw new IllegalStateException("Request already reviewed.");
        }

        status = Status.REJECTED;
    }

    //Return the university ID
    public String getStudentId() {
        return studentId;
    }

    //Return the student's name
    public String getName() {
        return name;
    }

    //Return the email
    public String getEmail() {
        return email;
    }

    //Return the phone number
    public String getPhone() {
        return phone;
    }

    //Return the password for admin review
    public String getPassword() {
        return password;
    }

    //Return the request status
    public Status getStatus() {
        return status;
    }
}