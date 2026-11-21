import java.util.*;
import java.util.List;
import javax.swing.*;
import java.awt.*;

public class Scheduler {
	private List<Job> queue = new ArrayList<>();
	private GPU[] gpus = new GPU[4];
	private SimulationClock clock;
	
	//Creating the GPU objects in the array
	public Scheduler(SimulationClock Clock) {
		clock = Clock;
		for (int i = 0; i<4; i++) {
			gpus[i]=new GPU(i+1);
		}
		
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
}
