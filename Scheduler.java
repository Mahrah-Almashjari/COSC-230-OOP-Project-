import java.util.*;
import java.util.List;
import java.util.prefs.*;
import javax.swing.*;
import java.awt.*;
import java.time.*;
import java.io.*;

public class Scheduler {
	private long nextJobNumber = 101;
	private List<Job> queue = new ArrayList<>();
	private GPU[] gpus = new GPU[4];
	private SimulationClock clock;
	private double utilization, simulatedMinutes;
	private Preferences prefs = Preferences.userNodeForPackage(Scheduler.class);
	private int busyGpuCount=0;
	Map<Job, LocalDateTime> start = new HashMap<>();

	//creates job then calls submit(job) method
	public Job submit(Student student, String title, String application, String description, Job.TaskSize size) {
		LocalDateTime now = clock.getDateTime();
		if (now == null) {
			throw new IllegalStateException("The simulation clock has not been initialized.");
		}

		Job job = new Job(
				nextJobNumber,
				student,
				title,
				application,
				description,
				size,
				now);

		submit(job); 
		return job;
	}

	//returns position in queue
	public int getQueuePosition(Job job) {
	    int index = queue.indexOf(job);
	    if (index == -1) {
	        return 0;
	    }
	    return index + 1;
	}
	
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

	//returns predicted start time
	public LocalDateTime getPredictedStartTime(Job job) {
    	if (job == null) throw new IllegalArgumentException("Job is required.");
    	if (job.getStatus() != Job.Status.QUEUED) return job.getStartTime();
    	predict();
    	LocalDateTime result = start.get(job);
    	if (result == null) {
      	  throw new IllegalStateException("This job is not in the waiting queue.");
    	}
    	return result;
	}
	
	//Write live state using the same stream as accounts and requests.
	 public void saveStorageState(ObjectOutputStream out) throws IOException {
	     out.writeObject(queue);
	     out.writeObject(gpus);
	     out.writeInt(busyGpuCount);
	     out.writeLong(nextJobNumber);
	 }

	//getter for GPUs
	public GPU[] getGpus() {
    return gpus.clone();
}
	
//Restore assignments directly without starting jobs or charging allowance again.
	 public void loadStorageState(ObjectInputStream in, long minimumNextId)
	         throws IOException, ClassNotFoundException {
	     Object savedQueue = in.readObject();
	     Object savedGpus = in.readObject();
	     int savedBusy = in.readInt();
	     long savedNextId = in.readLong();

	     if (!(savedQueue instanceof List<?>) || !(savedGpus instanceof GPU[])
	             || savedBusy < 0 || savedNextId <= 0) {
	         throw new IOException("Invalid saved scheduler data.");
	     }

	     List<Job> restoredQueue = new ArrayList<>();
	     for (Object value : (List<?>) savedQueue) {
	         if (!(value instanceof Job) || ((Job) value).getStatus() != Job.Status.QUEUED
	                 || restoredQueue.contains(value)) {
	             throw new IOException("Invalid job in saved waiting queue.");
	         }
	         restoredQueue.add((Job) value);
	     }
	     GPU[] restoredGpus = (GPU[]) savedGpus;
	     if (restoredGpus.length != 4) {
	         throw new IOException("The saved application must contain four GPUs.");
	     }
	     for (int i = 0; i < restoredGpus.length; i++) {
	         GPU gpu = restoredGpus[i];
	         if (gpu == null || gpu.getId() != i + 1
	                 || (!gpu.isFree() && (gpu.getCurrentJob().getStatus() != Job.Status.RUNNING
	                 || !Integer.valueOf(gpu.getId()).equals(gpu.getCurrentJob().getAssignedGpuId())))) {
	             throw new IOException("Invalid saved GPU assignment.");
	         }
	     }
	     restoredQueue.sort(null);
	     queue = restoredQueue;
	     gpus = restoredGpus;
	     busyGpuCount = savedBusy;
	     nextJobNumber = Math.max(savedNextId, minimumNextId);
	     start.clear(); // Predictions are recalculated from restored jobs and clock.
	 }

}
