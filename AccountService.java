
import java.util.Arrays;
import java.util.Locale;
import java.time.*;
import java.io.*;


// Login and registration logic using shared arrays; no Swing code. 
public class AccountService {

    private final User[] accounts;
    private final AccessRequest[] requests;
    private String lastMessage = "";

    public AccountService(User[] accounts, AccessRequest[] requests) {
        if (accounts == null || requests == null) {
            throw new IllegalArgumentException("Account and request arrays are required.");
        }

        // Keep the same arrays used by registration, approval, and storage.
        // Unused positions may be null. Accounts must contain active users only.
        this.accounts = accounts;
        this.requests = requests;
    }

    // Return the account on success, or null with a message on failure.
    public User login(String username, char[] password) {
        lastMessage = "";

        if (username == null || username.trim().isEmpty()
                || password == null || password.length == 0) {
            return fail("Please enter your username and password.");
        }

        username = username.trim();
        String enteredPassword = new String(password);
        AccessRequest request = findRequest(username);

        if (request != null) {
            if (!request.getPassword().equals(enteredPassword)) {
                return fail("Incorrect password. Please try again.");
            }

            if (request.getStatus() == AccessRequest.Status.PENDING) {
                return fail("Your registration request is still pending approval.");
            }

            if (request.getStatus() == AccessRequest.Status.REJECTED) {
                return fail("Your registration request was rejected.");
            }
        }

        User user = findAccount(username);

        //AccessRequest stores the Student created on approval.
        if (user == null && request != null
                && request.getStatus() == AccessRequest.Status.APPROVED) {
            user = request.getApprovedStudent();
        }

        if (user == null) {
            if (request != null) {
                return fail("The approved account is unavailable. Contact the administrator.");
            }
            return fail("No account or registration request found.");
        }

        if (!user.checkPassword(enteredPassword)) {
            return fail("Incorrect password. Please try again.");
        }

        return user;
    }

    // Validate details and store a pending request in the first unused slot.
    // Save application data after this returns successfully.
    public AccessRequest register(String studentId, String name, String email,
                                  String phone, String password) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("University ID is required.");
        }

        studentId = studentId.trim();

        if (findAccount(studentId) != null || findRequest(studentId) != null) {
            throw new IllegalArgumentException(
                    "This university ID already has an account or registration request.");
        }

        for (int i = 0; i < requests.length; i++) {
            if (requests[i] == null) {
                // The supplied model validates the fields and starts as PENDING.
                AccessRequest request = new AccessRequest(
                        studentId, name, email, phone, password);
                requests[i] = request;
                return request;
            }
        }

        throw new IllegalStateException("Registration storage is full.");
    }

    public String getLastMessage() {
        return lastMessage;
    }

    // Administrators receive array copies; the contained model objects stay shared.
    public AccessRequest[] getRequests(Administrator admin) {
        requireAdministrator(admin);
        return requests.clone();
    }

    public Student[] getStudents(Administrator admin) {
        requireAdministrator(admin);
        Student[] result = new Student[accounts.length + requests.length];
        int count = 0;
        for (User account : accounts) {
            if (account instanceof Student) {
                count = addStudent(result, count, (Student) account);
            }
        }
        for (AccessRequest request : requests) {
            if (request != null && request.getStatus() == AccessRequest.Status.APPROVED
                    && request.getApprovedStudent() != null) {
                count = addStudent(result, count, request.getApprovedStudent());
            }
        }
        return Arrays.copyOf(result, count);
    }

    private int addStudent(Student[] result, int count, Student student) {
        for (int i = 0; i < count; i++) {
            if (result[i].getStudentId().equals(student.getStudentId())) {
                if (result[i] != student) {
                    throw new IllegalStateException(
                            "Conflicting account objects for student " + student.getStudentId());
                }
                return count;
            }
        }
        result[count] = student;
        return count + 1;
    }

    public Student findStudent(Administrator admin, String studentId) {
        requireAdministrator(admin);
        if (studentId == null || studentId.trim().isEmpty()) {
            return null;
        }
        studentId = studentId.trim();
        for (Student student : getStudents(admin)) {
            if (student.getStudentId().equals(studentId)) {
                return student;
            }
        }
        return null;
    }

    public Job[] getAllJobs(Administrator admin) {
        Student[] students = getStudents(admin);
        int count = 0;
        for (Student student : students) count += student.getJobs().size();
        Job[] result = new Job[count];
        int i = 0;
        for (Student student : students) {
            for (Job job : student.getJobs()) result[i++] = job;
        }
        Arrays.sort(result);
        return result;
    }

    public Job[] searchAllJobs(Administrator admin, String query, Job.Status status) {
        String text = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        Job[] all = getAllJobs(admin);
        Job[] matches = new Job[all.length];
        int count = 0;
        for (Job job : all) {
            String details = job.getJobId() + " " + job.getStudentId() + " "
                    + job.getStudent().getName() + " " + job.getTitle() + " "
                    + job.getApplicationName() + " " + job.getDescription() + " " + job.getStatus();
            if ((status == null || status == job.getStatus())
                    && details.toLowerCase(Locale.ROOT).contains(text)) {
                matches[count++] = job;
            }
        }
        return Arrays.copyOf(matches, count);
    }

    public Student approveRequest(Administrator admin, AccessRequest request, int minutes) {
        requireAdministrator(admin);
        requireRequest(request);
        if (request.getStatus() != AccessRequest.Status.PENDING) {
            throw new IllegalStateException("Request already reviewed.");
        }
        if (findAccount(request.getStudentId()) != null) {
            throw new IllegalArgumentException("This university ID already has an account.");
        }
        int slot = -1;
        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i] == null) { slot = i; break; }
        }
        // Check capacity before approval changes the request's status.
        if (slot < 0) throw new IllegalStateException("Account storage is full.");
        Student student = admin.approve(request, minutes);
        accounts[slot] = student;
        return student;
    }

    public void rejectRequest(Administrator admin, AccessRequest request) {
        requireAdministrator(admin);
        requireRequest(request);
        admin.reject(request);
    }

    public void grantTime(Administrator admin, Student student,
                      int minutes, String reason, LocalDateTime now) {
    requireStudent(admin, student);

    // Validate the original reason before adding timestamp information.
    if (reason == null || reason.trim().isEmpty()) {
        throw new IllegalArgumentException("A grant reason is required.");
    }

    if (now == null) {
        throw new IllegalStateException(
                "Initialize the simulation clock before granting time.");
    }

    // The existing allowance model stores this text in its grant history.
    String recordedReason = reason.trim()
            + " | Simulation time: " + Job.formatTime(now);

    admin.grantTime(student, minutes, recordedReason);
}

    public void setAllocation(Administrator admin, Student student, int minutes, String reason) {
        requireStudent(admin, student);
        admin.setAllocation(student, minutes, reason);
    }

    private void requireAdministrator(Administrator admin) {
        if (admin == null || findAccount("admin") != admin) {
            throw new IllegalArgumentException("Administrator access is required.");
        }
    }

    private void requireStudent(Administrator admin, Student student) {
        requireAdministrator(admin);
        if (student == null || findStudent(admin, student.getStudentId()) != student) {
            throw new IllegalArgumentException("Select a student account from this application.");
        }
    }

    private void requireRequest(AccessRequest request) {
        for (AccessRequest existing : requests) {
            if (existing != null && existing == request) return;
        }
        throw new IllegalArgumentException("Select a registration request from this application.");
    }

    private User findAccount(String username) {
        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i] != null
                    && accounts[i].getUsername().equals(username)) {
                return accounts[i];
            }
        }
        return null;
    }

    private AccessRequest findRequest(String studentId) {
        for (int i = 0; i < requests.length; i++) {
            if (requests[i] != null
                    && requests[i].getStudentId().equals(studentId)) {
                return requests[i];
            }
        }
        return null;
    }

    private User fail(String message) {
        lastMessage = message;
        return null;
    }
}

