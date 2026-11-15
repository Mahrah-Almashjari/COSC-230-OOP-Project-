package model;

public class Student extends User {

    private static final long serialVersionUID = 1L;

    private String name;
    private String email;
    private String phone;
    private TimeAllowance allowance;

    //Set student details and default allowance
    public Student(String studentId, String password, String name,
                   String email, String phone) {
        super(studentId, password);

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required.");
        }

        if (email == null
                || !email.trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Enter a valid email.");
        }

        if (phone == null || !phone.trim().matches("\\+?[0-9]{7,15}")) {
            throw new IllegalArgumentException("Enter a valid phone number.");
        }

        this.name = name.trim();
        this.email = email.trim();
        this.phone = phone.trim();
        allowance = new TimeAllowance();
    }

    //Return the university ID
    public String getStudentId() {
        return getUsername();
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

    //Return the student's allowance
    public TimeAllowance getAllowance() {
        return allowance;
    }

    //Identify the user as a student
    @Override
    public String getRole() {
        return "Student";
    }
}
