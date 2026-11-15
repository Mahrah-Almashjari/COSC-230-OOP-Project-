import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.*;
import javax.swing.*;

public class SimulationClock extends JFrame implements ActionListener{
	
	//Setting the private attributes for the clock
	private int hour;
	private int minute;
	private int second;
	JLabel title, message, dateTitle, timeTitle;
	JSpinner SpinnerDateModel; 
	JButton startClock;
	
	
	public SimulationClock() {
		title = new JLabel ("Set the starting date and time",SwingConstants.LEFT);
		title.setFont(new Font("Ariel",Font.BOLD,24));
		message = new JLabel("This is done once. Afterwards, time moves only with +1 minute.",SwingConstants.LEFT);
		dateTitle = new JLabel ("Date",SwingConstants.LEFT);
		timeTitle = new JLabel ("Time (24-hour)",SwingConstants.LEFT);
		JSpinner date = new JSpinner( new SpinnerDateModel(new Date(),null,null, Calendar.DAY_OF_MONTH));
		date.setEditor(new JSpinner.DateEditor(date,"dd/MM/yyyy HH:mm"));
		startClock = new JButton("Start clock");
		startClock.setPreferredSize(new Dimension(120,35));
		JPanel panel = new JPanel();
		panel.add(startClock);
		
		GridLayout Layout = new GridLayout(7,2);
		
		setLayout(Layout);
		add(title);
		add(new JLabel());
		add(message);
		add(new JLabel());
		add(dateTitle);
		add(timeTitle);
		add(date);
		add(new JLabel());
		add(new JLabel());
		add(new JLabel());
		add(new JLabel());
		add(startClock);
		add(new JLabel());
		add(new JLabel());
		
		setVisible(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(300,600);

	}
	
	
	//Parameterized clock time
	public SimulationClock(int hr, int min, int sec) {
		setTime(hr,min,sec);
	}
	
	//Validating the time
	public static boolean validTime(int hr, int min, int sec) {
		return ((hr>=0 && hr<24) && (min>=0 && min<60) && (sec>=0 && sec<60));
	}
	
	//Setting the time in the system.
	public void setTime(int hr, int min, int sec) {
		if (0<=hr && hr<24) {
			hour = hr;
		}
		else {
			hour = 0;
		}
		if (0<=min && min<60) {
			minute = min;
		}
		else {
			minute = 0;
		}
		if (0<=sec && sec<60) {
			second = sec;
		}
		else {
			second = 0;
		}
	}
	
	//Getting the clock attributes
	public int getHours() {
		return hour;
	}
	public int getMinutes() {
		return minute;
	}
	public int getSeconds() {
		return second;
	}
	
	//Printing the time
	public void printTime() {
		if (hour < 10) {
			System.out.print("0");
		}
		System.out.print(hour+":");
		if (minute < 10) {
			System.out.print("0");
		}
		System.out.print(minute+":");
		if (second < 10) {
			System.out.print("0");
		}
		System.out.print(second);
	}
	
	//Increasing the clock by one
	public void advance() {
		second++;
		if (second>59) {
			second = 0;
			minute ++;
			if (minute>59) {
				minute = 0;
				hour++;
				if (hour>23) {
					hour=0;
				}
			}
		}
	}
	
	public static void main(String[] args) {

		new SimulationClock();

		}


	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}

}
