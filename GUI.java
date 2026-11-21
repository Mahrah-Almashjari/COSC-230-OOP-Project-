import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.time.*;
import javax.swing.event.*;

public class GUI extends JFrame implements ActionListener{
	Scheduler scheduler;
	User currentUser;
	JButton cancelButton;
	Job job;
	SimulationClock clock;
	JList<Job> jobList;
	JLabel title, message, reason;
	JTextField why;
	JButton cancel, keep;
	
	public GUI(Job job, Scheduler scheduler, User currentUser, SimulationClock clock) {
		super();
		this.job = job;
		this.scheduler = scheduler;
		this.currentUser = currentUser;
		this.clock = clock;
	}
	
	public void showCancelDialog() {	
		title = new JLabel("J"+job.getJobId()+" is running on GPU"+job.getAssignedGpuId()+" and has run "+job.getRuntime(clock.getDateTime())+" min.",SwingConstants.LEFT);
		title.setFont(new Font("Arial",Font.BOLD,24));
		message = new JLabel(job.getRuntime(clock.getDateTime())+" min will be charged; the other "+job.getRemainingRuntime(clock.getDateTime())+" min return to the balance",SwingConstants.LEFT);
		reason = new JLabel("Reason (required)",SwingConstants.LEFT);
		why = new JTextField ();
		cancel = new JButton ("Cancel job");
		keep = new JButton("Keep job");
		
		JPanel button = new JPanel();
		button.add(cancel);
		button.add(keep);
		
		GridLayout Layout = new GridLayout(6,1);

		setLayout(Layout);
		
		add(title);
		add(message);
		add(reason);
		add(why);
		add(new JLabel());
		add(button);
		
		cancel.setEnabled(false);
		
		DocumentListener l = new DocumentListener() {

			private void update() {
				cancel.setEnabled(!why.getText().trim().isEmpty());
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				update();
				
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				update();
				
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				update();
				
			}
			
		};
		
		why.getDocument().addDocumentListener(l);
		cancel.addActionListener(this);
		keep.addActionListener(this);
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setSize(800,400);
		setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource()==cancel) {
			scheduler.cancel(job,currentUser,why.getText().trim());
			dispose();
		}
		if(e.getSource()==keep) {
			dispose();
		}
		
	}
	
}
