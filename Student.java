package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class Student extends User {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final String email;
    private final String phone;
    private final TimeAllowance allowance;

    private final ArrayList<Job> jobs = new ArrayList<>();

    //Create an approved student account
    Student(String studentId, String password, String name,
            String email, String phone, int minutes) {
        super(studentId, password);

        this.name = requireText(name, "Name");
        this.email = requireEmail(email);
        this.phone = requirePhone(phone);
        allowance = new TimeAllowance(minutes);
    }

    //Return the university ID
    public String getStudentId() {
        return getUsername();
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

    //Return the student allowance
    public TimeAllowance getAllowance() {
        return allowance;
    }

    //Return the student role
    @Override
    public String getRole() {
        return "Student";
    }

    //Add an accepted job to history
    void addJob(Job job) {
        if (job == null || job.getStudent() != this) {
            throw new IllegalArgumentException("Invalid job owner.");
        }

        if (findJob(job.getJobId()) != null) {
            throw new IllegalArgumentException(
                    "Job ID already exists.");
        }

        jobs.add(job);
    }

    //Return the student job history
    public List<Job> getJobs() {
        return Collections.unmodifiableList(jobs);
    }

    //Find a job by ID
    public Job findJob(String jobId) {
        for (Job job : jobs) {
            if (job.getJobId().equals(jobId)) {
                return job;
            }
        }

        return null;
    }

    //Search the student's jobs
    public List<Job> searchJobs(String text) {
        if (text == null) {
            throw new IllegalArgumentException(
                    "Search text is required.");
        }

        String query = text.trim().toLowerCase(Locale.ROOT);
        List<Job> found = new ArrayList<>();

        for (Job job : jobs) {
            String details = job.getJobId() + " "
                    + job.getTitle() + " "
                    + job.getApplicationName() + " "
                    + job.getDescription() + " "
                    + job.getStatus();

            if (details.toLowerCase(Locale.ROOT).contains(query)) {
                found.add(job);
            }
        }

        return found;
    }
}
