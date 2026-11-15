package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TimeAllowance implements Serializable {

    private static final long serialVersionUID = 1L;

    private int allocated;
    private int used;
    private int reserved;

    private final ArrayList<String> grantHistory = new ArrayList<>();

    //Start with 100 GPU minutes
    public TimeAllowance() {
        this(100);
    }

    //Set the initial allowance
    public TimeAllowance(int minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException(
                    "Allowance cannot be negative.");
        }

        allocated = minutes;
    }

    //Return allocated minutes
    public int getAllocated() {
        return allocated;
    }

    //Return charged minutes
    public int getUsed() {
        return used;
    }

    //Return reserved minutes
    public int getReserved() {
        return reserved;
    }

    //Calculate available minutes
    public int getAvailable() {
        return allocated - used - reserved;
    }

    //Return recorded allowance changes
    public List<String> getGrantHistory() {
        return Collections.unmodifiableList(grantHistory);
    }

    //Reserve minutes for an accepted job
    void reserve(int minutes) {
        if (minutes <= 0 || minutes > getAvailable()) {
            throw new IllegalArgumentException(
                    "Not enough available GPU minutes.");
        }

        reserved += minutes;
    }

    //Release the reservation and charge runtime
    void settle(int duration, int runtime) {
        if (duration <= 0 || duration > reserved) {
            throw new IllegalArgumentException(
                    "Invalid reserved duration.");
        }

        if (runtime < 0 || runtime > duration) {
            throw new IllegalArgumentException("Invalid runtime.");
        }

        reserved -= duration;
        used += runtime;
    }

    //Add extra minutes with a reason
    void grantTime(int minutes, String reason) {
        reason = User.requireText(reason, "Reason");

        if (minutes <= 0) {
            throw new IllegalArgumentException(
                    "Minutes must be positive.");
        }

        int total = Math.addExact(allocated, minutes);

        grantHistory.add(
                "admin | +" + minutes + " minutes | " + reason);

        allocated = total;
    }

    //Adjust the total allowance safely
    void setAllocation(int minutes, String reason) {
        reason = User.requireText(reason, "Reason");

        if (minutes < used + reserved) {
            throw new IllegalArgumentException(
                    "Allocation cannot be below used plus reserved.");
        }

        grantHistory.add(
                "admin | " + allocated + " -> " + minutes
                        + " | " + reason);

        allocated = minutes;
    }
}
