package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TimeAllowance implements Serializable {

    private static final long serialVersionUID = 1L;

    private int allocated;
    private int used;
    private int reserved;

    private ArrayList<String> grantHistory = new ArrayList<>();

    //Start with 100 GPU minutes
    public TimeAllowance() {
        allocated = 100;
        used = 0;
        reserved = 0;
    }

    //Return total allocated minutes
    public int getAllocated() {
        return allocated;
    }

    //Return charged minutes
    public int getUsed() {
        return used;
    }

    //Return minutes held for jobs
    public int getReserved() {
        return reserved;
    }

    //Calculate available minutes
    public int getAvailable() {
        return allocated - used - reserved;
    }

    //Hold minutes for an accepted job
    public void reserve(int minutes) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("Minutes must be positive.");
        }

        if (minutes > getAvailable()) {
            throw new IllegalArgumentException("Not enough available minutes.");
        }

        reserved += minutes;
    }

    //Release reserved minutes and charge runtime
    public void settle(int duration, int runtime) {
        if (duration <= 0 || duration > reserved) {
            throw new IllegalArgumentException("Invalid reserved duration.");
        }

        if (runtime < 0 || runtime > duration) {
            throw new IllegalArgumentException("Invalid runtime.");
        }

        reserved -= duration;
        used += runtime;
    }

    //Add extra minutes with a reason
    public void grantTime(int minutes, String reason) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("Minutes must be positive.");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A reason is required.");
        }

        allocated = Math.addExact(allocated, minutes);
        grantHistory.add(minutes + " minutes: " + reason.trim());
    }

    //Return recorded grants
    public List<String> getGrantHistory() {
        return Collections.unmodifiableList(grantHistory);
    }
}
