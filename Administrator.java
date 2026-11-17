package model;

public final class Administrator extends User {

    private static final long serialVersionUID = 1L;

    //Set the predefined admin login
    public Administrator() {
        super("admin", "gpu2026");
    }

    //Return the administrator role
    @Override
    public String getRole() {
        return "Administrator";
    }

    //Approve with the default allowance
    public Student approve(AccessRequest request) {
        return approve(request, 100);
    }

    //Approve with a chosen allowance
    public Student approve(AccessRequest request, int minutes) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Request is required.");
        }

        return request.approve(minutes);
    }

    //Reject a pending request
    public void reject(AccessRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Request is required.");
        }

        request.reject();
    }

    //Grant extra minutes with a reason
    public void grantTime(Student student, int minutes, String reason) {
        if (student == null) {
            throw new IllegalArgumentException(
                    "Student is required.");
        }

        student.getAllowance().grantTime(minutes, reason);
    }

    //Adjust the total allowance
    public void setAllocation(Student student, int minutes,
                              String reason) {
        if (student == null) {
            throw new IllegalArgumentException(
                    "Student is required.");
        }

        student.getAllowance().setAllocation(minutes, reason);
    }
}