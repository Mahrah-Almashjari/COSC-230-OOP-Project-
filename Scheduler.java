import java.util.*;

public class Scheduler {
	private List<Job> queue = new ArrayList<>();
	private GPU[] gpus = new GPU[4];
	private SimulationClock clock;
	
	//Creating the GPU objects in the array
	public Scheduler(SimulationClock Clock) {
		clock = Clock;
		
		for (int i = 0; i<4; i++) {
			gpus[i]=new GPU(i);
		}
		
	}
	
	//Submitting the job and assigning it to the person
	public void submit (Job job) {
		queue.add(job);
		
		for (int i=0; i<4;i++) {
			if (gpus[i].isFree() && !queue.isEmpty()) {
				Job newJob = queue.remove(0);
				gpus[i].assign(newJob);
			}
		}
	}
	
	//Advancing a minute
	public void advanceMinute() {
		clock.advance();
	}
	
	//Cancelling the job
	public void cancel(Job job, User by, String why) {
		queue.remove(job);
		//ADD POP UP FOR THE CANCELLATION
		for (int i=0; i<4;i++) {
			if (gpus[i].isFree() && !queue.isEmpty()) {
				Job newJob = queue.remove(0);
				gpus[i].assign(newJob);
			}
		}
	}
}
