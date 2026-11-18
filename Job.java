package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Job implements Serializable, Comparable<Job> {

    private static final long serialVersionUID = 1L;

    public enum Status {
        QUEUED, RUNNING, COMPLETED, CANCELLED
    }

    public enum TaskSize {
        SMALL(5),
        MEDIUM(15),
        LARGE(30),
        VERY_LARGE(60);

        private final int minutes;

        //Set the task duration
        TaskSize(int minutes) {
            this.minutes = minutes;
        }

        //Return the task duration
        public int getMinutes() {
            return minutes;
        }
    }

    private final long jobNumber;
    private final Student student;
    private final String title;
    private final String applicationName;
    private final String description;
    private final TaskSize taskSize;
    private final LocalDateTime submissionTime;

    private Status status = Status.QUEUED;
    private Integer assignedGpuId;
    private GPU gpu;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private LocalDateTime cancellationTime;
    private String cancelledBy;
    private String cancellationReason;
    private int allowanceCharged;

    //Create a job and reserve its minutes
    public Job(long jobNumber, Student student, String title,
               String applicationName, String description,
               TaskSize taskSize, LocalDateTime submissionTime) {

        if (jobNumber <= 0) {
            throw new IllegalArgumentException(
                    "Job number must be positive.");
        }

        if (student == null) {
            throw new IllegalArgumentException(
                    "Student is required.");
        }

        if (taskSize == null) {
            throw new IllegalArgumentException(
                    "Select a task size.");
        }

        this.title = User.requireText(title, "Title");
        this.applicationName = User.requireText(
                applicationName, "Application name");
        this.description = User.requireText(
                description, "Description");

        this.jobNumber = jobNumber;
        this.student = student;
        this.taskSize = taskSize;
        this.submissionTime = User.requireTime(submissionTime);

        if (student.findJob(getJobId()) != null) {
            throw new IllegalArgumentException(
                    "Job ID already exists.");
        }

        student.getAllowance().reserve(getEstimatedDuration());
        student.addJob(this);
    }

    //Start through GPU assignment
    void start(GPU gpu, LocalDateTime now) {
        requireStatus(Status.QUEUED);
        User.minutesBetween(submissionTime, now);

        this.gpu = gpu;
        assignedGpuId = gpu.getId();
        startTime = now;
        status = Status.RUNNING;
    }

    //Complete and charge the full runtime
    public void complete(LocalDateTime now) {
        requireStatus(Status.RUNNING);
        User.requireTime(now);

        if (!now.equals(getExpectedFinishTime())) {
            throw new IllegalStateException(
                    "Complete at the expected finish time.");
        }

        int duration = getEstimatedDuration();
        student.getAllowance().settle(duration, duration);

        allowanceCharged = duration;
        finishTime = now;
        status = Status.COMPLETED;

        gpu.release();
        gpu = null;
    }

    //Cancel and charge only elapsed execution
    public void cancel(User actor, String reason, LocalDateTime now) {
        if (status != Status.QUEUED && status != Status.RUNNING) {
            throw new IllegalStateException(
                    "Only queued or running jobs can be cancelled.");
        }

        if (!(actor instanceof Administrator) && actor != student) {
            throw new IllegalArgumentException(
                    "You cannot cancel another student's job.");
        }

        reason = User.requireText(reason, "Cancellation reason");
        User.minutesBetween(submissionTime, now);

        int runtime = 0;

        if (status == Status.RUNNING) {
            long elapsed = User.minutesBetween(startTime, now);

            if (elapsed > getEstimatedDuration()) {
                throw new IllegalStateException(
                        "Process completed jobs first.");
            }

            runtime = (int) elapsed;
        }

        student.getAllowance().settle(
                getEstimatedDuration(), runtime);

        if (status == Status.RUNNING) {
            finishTime = now;
        }

        allowanceCharged = runtime;
        cancellationTime = now;
        cancelledBy = actor.getUsername();
        cancellationReason = reason;
        status = Status.CANCELLED;

        if (gpu != null) {
            gpu.release();
            gpu = null;
        }
    }

    //Check the required job state
    private void requireStatus(Status expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "Invalid job state: " + status);
        }
    }

    //Return execution time without waiting
    public int getRuntime(LocalDateTime now) {
        User.minutesBetween(submissionTime, now);

        if (startTime == null) {
            return 0;
        }

        LocalDateTime end = finishTime == null ? now : finishTime;

        User.minutesBetween(end, now);

        return (int) Math.min(
                getEstimatedDuration(),
                User.minutesBetween(startTime, end));
    }

    //Return remaining execution time
    public int getRemainingRuntime(LocalDateTime now) {
        if (status != Status.RUNNING) {
            return 0;
        }

        return getEstimatedDuration() - getRuntime(now);
    }

    //Return the planned execution finish
    public LocalDateTime getExpectedFinishTime() {
        if (startTime == null) {
            return null;
        }

        return startTime.plusMinutes(getEstimatedDuration());
    }

    //Return elapsed queue waiting time
    public long getQueueWait(LocalDateTime now) {
        User.minutesBetween(submissionTime, now);

        LocalDateTime end = now;

        if (startTime != null) {
            end = startTime;
        } else if (cancellationTime != null) {
            end = cancellationTime;
        }

        User.minutesBetween(end, now);

        return User.minutesBetween(submissionTime, end);
    }

    //Use the start predicted by the scheduler
    public long getRemainingWaitingTime(
            LocalDateTime predictedStart, LocalDateTime now) {
        requireStatus(Status.QUEUED);
        User.minutesBetween(submissionTime, now);

        return User.minutesBetween(now, predictedStart);
    }

    //Return the predicted finish for a queued job
    public LocalDateTime getPredictedFinishTime(
            LocalDateTime predictedStart) {
        requireStatus(Status.QUEUED);
        User.minutesBetween(submissionTime, predictedStart);

        return predictedStart.plusMinutes(getEstimatedDuration());
    }

    //Sort by submission time and numeric job ID
    @Override
    public int compareTo(Job other) {
        int order = submissionTime.compareTo(other.submissionTime);

        if (order == 0) {
            return Long.compare(jobNumber, other.jobNumber);
        }

        return order;
    }

    //Format timestamps for display
    public static String formatTime(LocalDateTime time) {
        if (time == null) {
            return "";
        }

        return time.format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    //Return the simulated result
    public String getResult() {
        if (status == Status.COMPLETED) {
            return "Completed successfully (simulated).";
        }

        if (status == Status.CANCELLED) {
            return "Cancelled. Charged "
                    + allowanceCharged + " minutes.";
        }

        return "Not finished.";
    }

    //Return the job ID
    public String getJobId() {
        return "J" + jobNumber;
    }

    //Return the numeric job ID
    public long getJobNumber() {
        return jobNumber;
    }

    //Return the job owner
    public Student getStudent() {
        return student;
    }

    //Return the owner university ID
    public String getStudentId() {
        return student.getStudentId();
    }

    //Return the title
    public String getTitle() {
        return title;
    }

    //Return the application name
    public String getApplicationName() {
        return applicationName;
    }

    //Return the description
    public String getDescription() {
        return description;
    }

    //Return the task size
    public TaskSize getTaskSize() {
        return taskSize;
    }

    //Return workload units
    public int getWorkloadUnits() {
        return taskSize.getMinutes();
    }

    //Return the estimated duration
    public int getEstimatedDuration() {
        return taskSize.getMinutes();
    }

    //Return the submission time
    public LocalDateTime getSubmissionTime() {
        return submissionTime;
    }

    //Return the current status
    public Status getStatus() {
        return status;
    }

    //Return the assigned GPU
    public Integer getAssignedGpuId() {
        return assignedGpuId;
    }

    //Return the start time
    public LocalDateTime getStartTime() {
        return startTime;
    }

    //Return the execution finish time
    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    //Return the cancellation time
    public LocalDateTime getCancellationTime() {
        return cancellationTime;
    }

    //Return who cancelled the job
    public String getCancelledBy() {
        return cancelledBy;
    }

    //Return the cancellation reason
    public String getCancellationReason() {
        return cancellationReason;
    }

    //Return charged minutes
    public int getAllowanceCharged() {
        return allowanceCharged;
    }
}