import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.time.LocalDateTime;

import java.awt.FlowLayout;

public class SimulationClock extends JFrame implements ActionListener, ChangeListener{
	
	//Setting the private attributes for the clock
	JLabel title, message, dateTitle, timeTitle;
	JSpinner date, time; 
	JButton startClock;
	LocalDateTime DateTime;
	
	
	public SimulationClock() {
		title = new JLabel ("Set the starting date and time",SwingConstants.LEFT);
		title.setFont(new Font("Ariel",Font.BOLD,24));
		message = new JLabel("This is done once. Afterwards, time moves only with +1 minute.",SwingConstants.LEFT);
		message.setFont(new Font("Ariel",Font.PLAIN,14));
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
		
		date.addChangeListener((ChangeListener) this);
		time.addChangeListener((ChangeListener) this);
		startClock.addActionListener(this);
		
		setVisible(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(800,400);

	}
	
	//Increasing the clock by one
	public void advance() {
		DateTime.plusMinutes(1);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource()==startClock) {
			dispose();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		
	}

}
