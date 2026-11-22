import java.util.*;
import java.util.List;
import java.util.prefs.*;
import javax.swing.*;
import java.awt.*;
import java.time.*;

public class Scheduler {
	private List<Job> queue = new ArrayList<>();
	private GPU[] gpus = new GPU[4];
	private SimulationClock clock;
	private double utilization, simulatedMinutes;
	private Preferences prefs = Preferences.userNodeForPackage(Scheduler.class);
	private int busyGpuCount=0;
	Map<Job, LocalDateTime> start = new HashMap<>();
	
	//Creating the GPU objects in the array
	public Scheduler(SimulationClock Clock) {
		clock = Clock;
		for (int i = 0; i<4; i++) {
			gpus[i]=new GPU(i+1);
		}
		busyGpuCount = prefs.getInt("busyGpuMinutes", 0);
		
	}
	
	//Submitting the job and assigning it to the person
	public void submit (Job job) {
		queue.add(job);
		for (int i=0; i<4;i++) {
			if (gpus[i].isFree() && !queue.isEmpty()) {
				Job newJob = queue.remove(0);
				gpus[i].assign(newJob, clock.getDateTime());
			}
		}
	}
	
	//Advancing a minute
	public void advanceMinute() {
		clock.advance();
		for (int i=0; i<4;i++) {
			if (!gpus[i].isFree()) {
				busyGpuCount++;
				
			}
			Job job = gpus[i].getCurrentJob();
			if (job!=null) {
				if (job.getRemainingRuntime(clock.getDateTime())<=0) {
					job.complete(clock.getDateTime());
					gpus[i].release();
					
				}
				
			}
			if (gpus[i].isFree() && !queue.isEmpty()) {
						Job newJob = queue.remove(0);
						gpus[i].assign(newJob, clock.getDateTime());
					}
		}
		prefs.putInt("busyGpuMinutes", busyGpuCount);
	}
	
	//Cancelling the job
	public void cancel(Job job, User by, String why) {
		if (queue.contains(job)) {
			queue.remove(job);
			job.cancel(by,why,clock.getDateTime());
		}
		else {
			for (int i=0; i<4;i++) {
				if (gpus[i].getCurrentJob()==job) {
					job.cancel(by,why,clock.getDateTime());
					gpus[i].release();
					break;
				}
			}
		}
		for (int i=0; i<4;i++) {
			if (gpus[i].isFree() && !queue.isEmpty()) {
				Job newJob = queue.remove(0);
				gpus[i].assign(newJob, clock.getDateTime());
				}
			}
	}
	
	public double getUtilization() {
		simulatedMinutes = Duration.between(clock.getInitialDateTime(),clock.getDateTime()).toMinutes();
		if (simulatedMinutes==0) {
			return 0.0;
		}
		else {
			utilization = ((double)busyGpuCount/(4*simulatedMinutes))*100;
			return utilization;
		}
	}
	
	//For the predictions.
	public LocalDateTime[] copyOfGpuFreeTimes() {
		LocalDateTime[] freeGPUTimes = new LocalDateTime[4];
		for (int i =  0; i<4;i++) {
			if (gpus[i].isFree()) {
				freeGPUTimes[i]=clock.getDateTime();
			}
			else {
				Job job = gpus[i].getCurrentJob();
				freeGPUTimes[i] = job.getExpectedFinishTime();
			}
		}
		return freeGPUTimes;
	}
	
	//For finding the earliest index
	public int earliest(LocalDateTime[] freeAt) {
		int earliest = 0;
		for (int i = 1; i<4;i++) {
			if (freeAt[i].isBefore(freeAt[earliest])) {
				earliest = i;
			}
			else if (freeAt[i].equals(freeAt[earliest])) {
				if (gpus[i].getId()<gpus[earliest].getId()) {
					earliest=i;
				}
			}
		}

		return earliest;
	}
	
	public List<Job> queueInFcfsOrder() {
		return new ArrayList<>(queue);
	}
	
	//Predicting when a queued job starts
	public void predict() {
		LocalDateTime[] freeAt = copyOfGpuFreeTimes();
		start.clear();
		
		for (Job job : queueInFcfsOrder()) {
			int g = earliest(freeAt); // tie: lowest ID
			start.put(job, freeAt[g]);
			freeAt[g] = freeAt[g].plusMinutes(job.getEstimatedDuration());
		}
	}

}
