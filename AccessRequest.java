package model;
import java.io.*;


import java.io.Serializable;

public final class AccessRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status {
        PENDING, APPROVED, REJECTED
    }

    private final String studentId;
    private final String name;
    private final String email;
    private final String phone;
    private final String password;

    private Status status = Status.PENDING;
    private Student approvedStudent;

    //Store registration details
    public AccessRequest(String studentId, String name, String email,
                         String phone, String password) {
        this.studentId = User.requireText(
                studentId, "University ID");

        if (this.studentId.equalsIgnoreCase("admin")) {
            throw new IllegalArgumentException(
                    "This username is reserved.");
        }

        this.name = User.requireText(name, "Name");
        this.email = User.requireEmail(email);
        this.phone = User.requirePhone(phone);

        User.requireText(password, "Password");
        this.password = password;
    }

    //Approve once and create the student
    Student approve(int minutes) {
        requirePending();

        Student student = new Student(
                studentId, password, name, email, phone, minutes);

        approvedStudent = student;
        status = Status.APPROVED;

        return student;
    }

    //Reject a pending request
    void reject() {
        requirePending();
        status = Status.REJECTED;
    }

    //Prevent repeated reviews
    private void requirePending() {
        if (status != Status.PENDING) {
            throw new IllegalStateException(
                    "Request already reviewed.");
        }
    }

    //Return the university ID
    public String getStudentId() {
        return studentId;
    }

    //Return the student name
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

    //Return the approved account
    public Student getApprovedStudent() {
        return approvedStudent;
    }
}
