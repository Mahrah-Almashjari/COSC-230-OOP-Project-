import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

//Entry point login page shared by students and administrator
public class Login extends JFrame implements ActionListener{
	private final AccountService accountService;
	JButton loginButton, registerButton;
	JTextField usernameField; JPasswordField passwordField;
	JLabel usernameLabel, passwordLabel, messageLabel;
	public Login(AccountService accountService) {
		//Store the service passed into the constructor.
    	this.accountService = accountService;

		//initalize text fields, button, and labels
		usernameField = new JTextField();
		passwordField = new JPasswordField();
		
		loginButton = new JButton("Sign in");
		registerButton = new JButton("Request an account");
		
		usernameLabel = new JLabel("Username (university ID)");
		passwordLabel = new JLabel("Password");

		//add action listeners to buttons to handle clicks
		loginButton.addActionListener(this);
		registerButton.addActionListener(this);
		
		JPanel form = new JPanel();
		form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

		form.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

		JLabel title = new JLabel("GPU Utilization Management");
		title.setAlignmentX(Component.CENTER_ALIGNMENT);

		form.add(title);
		form.add(Box.createVerticalStrut(25));

		//Center align all components
		usernameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
		passwordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
		loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		//Username and password labels and input field with vertical spaces between
		form.add(usernameLabel);
		form.add(Box.createVerticalStrut(8));
		form.add(usernameField);
		form.add(Box.createVerticalStrut(20));
		form.add(passwordLabel);
		form.add(Box.createVerticalStrut(8));
		form.add(passwordField);
		form.add(Box.createVerticalStrut(20));

		//log in button followed by a single row containing both text and registration button
		form.add(loginButton);
		form.add(Box.createVerticalStrut(12));
		JPanel registerRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		registerRow.add(new JLabel("New student?"));
		registerRow.add(registerButton);
		form.add(registerRow);

		//Blank label reserved for displaying login errors or status messages
		messageLabel = new JLabel(" ");
		messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		messageLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
		form.add(Box.createVerticalStrut(15));
		form.add(messageLabel);

		//add title, set background to white, and make the registerRow transparent
		setTitle("GPU Management - Login");
		form.setBackground(Color.WHITE);
		registerRow.setOpaque(false);

		//title font and dimensions to prevent text fields from stretching 
		title.setFont(new Font("SansSerif", Font.BOLD, 22));
		Dimension fieldSize = new Dimension(420, 34);

		usernameField.setPreferredSize(fieldSize);
		usernameField.setMaximumSize(fieldSize);
		passwordField.setPreferredSize(fieldSize);
		passwordField.setMaximumSize(fieldSize);

		//Use red for login failure messages.
		messageLabel.setForeground(new Color(180, 50, 50));

		// Finalize and display window centered on screen
		setContentPane(form);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);

	}
	public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
		//initialize accounts and requests array to contain 100 values each, initially null
        User[] accounts = new User[100];
        AccessRequest[] requests = new AccessRequest[100];
        SimulationClock clock = new SimulationClock(false);
        Scheduler scheduler = new Scheduler(clock);
        FileManager files = new FileManager(accounts, requests, scheduler, clock);

        try {
            if (!files.load()) {
                // First run only. Never replace loaded accounts with fresh ones.
                accounts[0] = new Administrator();
            }
        } catch (IOException | ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(null,
                    "Could not load data/gpu-state.dat.\n" + ex.getMessage()
                    + "\nThe file was not loaded. Fix it or restore a backup, then restart.",
                    "File error", JOptionPane.ERROR_MESSAGE);
            clock.dispose();
            return; // Do not start empty and accidentally overwrite a damaged file.
        }
				//Shared account logic used by this login window.

        AccountService service = new AccountService(accounts, requests);
        new Login(service, scheduler, clock, files);
    });
}
	
	// Handle button clicks.
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == registerButton) {
			//open registration page using the same account service.
	        new Registration(accountService).setVisible(true);
	    }
		else if (e.getSource() == loginButton) {
			//clear previous message before another login attempt.
			messageLabel.setText(" ");
	        String username = usernameField.getText().trim();
	        char[] password = passwordField.getPassword();

	        if (username.isEmpty() || password.length == 0) {
	            JOptionPane.showMessageDialog(this, "Please enter your username and password.");
	            return;
	        }
		//check whether the account can sign in.
        User user = accountService.login(username, password);

        if (user == null) {
            messageLabel.setText(accountService.getLastMessage());
    		pack();
            return;
        }

        if (user instanceof Administrator) {
            new AdminDashboard((Administrator) user).setVisible(true);
            dispose(); 
        }
        else if (user instanceof Student) {
            new StudentDashboard((Student) user).setVisible(true);
            dispose();
        }
	
		}
}}

