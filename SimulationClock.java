import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.time.*;
import java.util.prefs.Preferences;
import java.util.Date;
import java.text.ParseException;
import java.io.*;

public class SimulationClock extends JFrame implements ActionListener {
	
	//Setting the private attributes for the clock
	JLabel title, message, dateTitle, timeTitle;
	JSpinner date, time; 
	JButton startClock;
	private LocalDateTime DateTime, initialDateTime;
	private Preferences prefs = Preferences.userNodeForPackage(SimulationClock.class);
	
	
	public SimulationClock() {
		
		String saved = prefs.get("time", null);
		String savedInitial = prefs.get("initialTime", null);
		
		if (savedInitial!=null) {
			initialDateTime = LocalDateTime.parse(savedInitial);
		}
		
		if (saved!=null) {
			DateTime = LocalDateTime.parse(saved);
		}
		else {
			title = new JLabel ("Set the starting date and time",SwingConstants.LEFT);
			title.setFont(new Font("Arial",Font.BOLD,24));
			message = new JLabel("This is done once. Afterwards, time moves only with +1 minute.",SwingConstants.LEFT);
			message.setFont(new Font("Arial",Font.PLAIN,14));
			dateTitle = new JLabel ("Date",SwingConstants.LEFT);
			timeTitle = new JLabel ("Time (24-hour)",SwingConstants.LEFT);
			date = new JSpinner( new SpinnerDateModel());
			date.setEditor(new JSpinner.DateEditor(date,"dd/MM/yyyy"));
			date.setPreferredSize(new Dimension(375,45));
			JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			datePanel.add(date);
			time = new JSpinner( new SpinnerDateModel());
			time.setEditor(new JSpinner.DateEditor(time,"HH:mm"));
			time.setPreferredSize(new Dimension(375,45));
			JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			timePanel.add(time);
			startClock = new JButton("Start clock");
			startClock.setPreferredSize(new Dimension(120,35));
			startClock.setBackground(new Color(70,120,200));
			startClock.setForeground(Color.WHITE);
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			buttonPanel.add(startClock);
			
			//Setting the layout of the pop-up
			GridLayout Layout = new GridLayout(7,2);
			
			setLayout(Layout);
			add(title);
			add(new JLabel());
			add(message);
			add(new JLabel());
			add(dateTitle);
			add(timeTitle);
			add(datePanel);
			add(timePanel);
			add(new JLabel());
			add(new JLabel());
			add(new JLabel());
			add(buttonPanel);
			add(new JLabel());
			add(new JLabel());
			
			startClock.addActionListener(this);
			
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			setSize(800,400);
			setVisible(true);
		}
		
		
	}
	
	public LocalDateTime getDateTime() {
		return DateTime;
	}

	public LocalDateTime getInitialDateTime() {
		return initialDateTime;
	}

	//Increasing the clock by one
	public void advance() {
		DateTime = DateTime.plusMinutes(1);
		prefs.put("time", DateTime.toString()); //Since LocalDateTime is immutable, assign the result back
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource()==startClock) {
			try {
				date.commitEdit();
				time.commitEdit();
				LocalTime Time = ((Date) time.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0);				
				LocalDate Date = ((Date) date.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
				DateTime = LocalDateTime.of(Date, Time);
				prefs.put("time", DateTime.toString());
				initialDateTime = DateTime;
				prefs.put("initialTime", initialDateTime.toString());
				dispose();
			}
			catch (ParseException exc) {
				JOptionPane.showMessageDialog(this,"Enter a valid date and time.");
			}
			
		}
	}

	//A clean clock for file-based startup; do not read old Preferences or open setup.
			 public SimulationClock(boolean loadOldPreferences) {
			     if (loadOldPreferences) {
			         String saved = prefs.get("time", null);
			         String savedInitial = prefs.get("initialTime", null);
			         if (saved != null) DateTime = LocalDateTime.parse(saved);
			         if (savedInitial != null) initialDateTime = LocalDateTime.parse(savedInitial);
			         if (DateTime == null) showSetupWindow();
			     }
			 }
			 
			 //Restore the exact saved time. Real time spent with the app closed is ignored.
			 public void restoreStorageTime(LocalDateTime current, LocalDateTime initial) {
			     if ((current == null) != (initial == null)
			             || (current != null && (initial.isAfter(current)
			             || current.getSecond() != 0 || current.getNano() != 0
			             || initial.getSecond() != 0 || initial.getNano() != 0))) {
			         throw new IllegalArgumentException("Invalid stored simulation time.");
			     }
			     DateTime = current;
			     initialDateTime = initial;
			 }
			 
			 public void showSetupWindow() {
				    if (DateTime != null) return; // Initialized clocks cannot be reset here.
				    if (startClock != null) {
				        setVisible(true);
				        toFront();
				        return;
				    }

}
