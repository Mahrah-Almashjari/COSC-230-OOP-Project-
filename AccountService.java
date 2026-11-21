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

        // AccessRequest stores the Student created on approval.
        if (user == null && request != null
                && request.getStatus() == AccessRequest.Status.APPROVED) {
            user = request.getApprovedStudent();
        }

        // No account was found in the accounts array or approved request.        
        if (user == null) {
            if (request != null) {
                return fail("The approved account is unavailable. Contact the administrator.");
            }
            return fail("No account or registration request found.");
        }

        //checks if password is wrong
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
