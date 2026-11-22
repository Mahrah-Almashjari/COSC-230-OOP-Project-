package model;

import java.io.Serializable;
import java.time.LocalDateTime;

public final class GPU implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private Job currentJob;

    //Create a GPU with a valid ID
    public GPU(int id) {
        if (id < 1 || id > 4) {
            throw new IllegalArgumentException(
                    "GPU ID must be 1 to 4.");
        }

        this.id = id;
    }

    //Return the GPU ID
    public int getId() {
        return id;
    }

    //Return the current job
    public Job getCurrentJob() {
        return currentJob;
    }

    //Check whether the GPU is free
    public boolean isFree() {
        return currentJob == null;
    }

    //Assign and start a queued job
    public void assign(Job job, LocalDateTime now) {
        if (!isFree()) {
            throw new IllegalStateException("GPU is busy.");
        }

        if (job == null) {
            throw new IllegalArgumentException("Job is required.");
        }

        job.start(this, now);
        currentJob = job;
    }

    //Release a finished or cancelled job
    public void release() {
        if (isFree()) {
            return;
        }

        Job.Status status = currentJob.getStatus();

        if (status != Job.Status.COMPLETED
                && status != Job.Status.CANCELLED) {
            throw new IllegalStateException(
                    "The job is still active.");
        }

        currentJob = null;
    }

    //Return remaining execution time
    public int getRemainingRuntime(LocalDateTime now) {
        User.requireTime(now);

        if (isFree()) {
            return 0;
        }

        return currentJob.getRemainingRuntime(now);
    }

    //Return the expected finish time
    public LocalDateTime getExpectedFinishTime() {
        if (isFree()) {
            return null;
        }

        return currentJob.getExpectedFinishTime();
    }

    //Return availability for copied predictions
    public LocalDateTime getAvailableTime(LocalDateTime now) {
        User.requireTime(now);

        LocalDateTime finish = getExpectedFinishTime();

        if (finish == null || finish.isBefore(now)) {
            return now;
        }

        return finish;
    }
    public GPU[] getGpus() {
    return gpus.clone();
}
}
