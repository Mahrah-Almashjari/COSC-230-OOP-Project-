import javax.swing.*;
import java.io.IOException;
import java.awt.*;
import java.awt.event.*;
// Login window shared by students and the administrator.
public class Login extends JFrame implements ActionListener{
	private final FileManager fileManager;
	private final AccountService accountService;
	private JButton loginButton, registerButton;
	private JTextField usernameField; JPasswordField passwordField;
	private JLabel usernameLabel, passwordLabel, messageLabel;
	private final Scheduler scheduler;
	private final SimulationClock clock;
	public Login(AccountService accountService, Scheduler scheduler,
            SimulationClock clock, FileManager fileManager) {
	// Store the shared service, scheduler, clock, and file manager.
   this.accountService = accountService;
   this.scheduler = scheduler;
   this.clock = clock;
   this.fileManager = fileManager;
		usernameField = new JTextField();
		passwordField = new JPasswordField();
		
		loginButton = new JButton("Sign in");
		registerButton = new JButton("Request an account");
		
		usernameLabel = new JLabel("Username (university ID)");
		passwordLabel = new JLabel("Password");
		
		loginButton.addActionListener(this);
		registerButton.addActionListener(this);
		// Arrange the login components vertically.
		JPanel form = new JPanel();
		form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

		form.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

		JLabel title = new JLabel("GPU Utilization Management");
		title.setAlignmentX(Component.CENTER_ALIGNMENT);

		form.add(title);
		form.add(Box.createVerticalStrut(25));

		usernameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
		passwordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
		loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		form.add(usernameLabel);
		form.add(Box.createVerticalStrut(8));
		form.add(usernameField);
		form.add(Box.createVerticalStrut(20));

		form.add(passwordLabel);
		form.add(Box.createVerticalStrut(8));
		form.add(passwordField);
		form.add(Box.createVerticalStrut(20));

		
		form.add(loginButton);
		form.add(Box.createVerticalStrut(12));
		// Display the registration option in one row.
		JPanel registerRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		registerRow.add(new JLabel("New student?"));
		registerRow.add(registerButton);

		form.add(registerRow);
		//reserve space for login error messages.
		messageLabel = new JLabel(" ");
		messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		messageLabel.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));
		form.add(Box.createVerticalStrut(15));
		form.add(messageLabel);
		
		setTitle("GPU Management - Login");

		form.setBackground(Color.WHITE);
		registerRow.setOpaque(false);

		title.setFont(new Font("SansSerif", Font.BOLD, 22));

		Dimension fieldSize = new Dimension(420, 34);

		usernameField.setPreferredSize(fieldSize);
		usernameField.setMaximumSize(fieldSize);

		passwordField.setPreferredSize(fieldSize);
		passwordField.setMaximumSize(fieldSize);

		// Use red for login failure messages.
		messageLabel.setForeground(new Color(180, 50, 50));

		setContentPane(form);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);

	}
	
	public static void main(String[] args) {
		//Start the Swing interface on its event-handling thread.
	    SwingUtilities.invokeLater(() -> {
			//Initialize arrays for accounts and requests, contain null currrently
	        User[] accounts = new User[100];
	        AccessRequest[] requests = new AccessRequest[100];
			// Create the clock without opening setup or reading old settings.
	        SimulationClock clock = new SimulationClock(false);
	        Scheduler scheduler = new Scheduler(clock);
	        FileManager files = new FileManager(accounts, requests, scheduler, clock);

	        try {
	            if (!files.load()) {
	                // First run only. Never replace loaded accounts with fresh ones.
	                accounts[0] = new Administrator();
	            }
				// Report loading failures and stop startup.
	        } catch (IOException | ClassNotFoundException ex) {
	            JOptionPane.showMessageDialog(null,
	                    "Could not load data/gpu-state.dat.\n" + ex.getMessage()
	                    + "\nThe file was not loaded. Fix it or restore a backup, then restart.",
	                    "File error", JOptionPane.ERROR_MESSAGE);
	            clock.dispose();
	            return; // Do not start empty and accidentally overwrite a damaged file.
	        }

	        AccountService service = new AccountService(accounts, requests);
	        new Login(service, scheduler, clock, files);
	    });
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == registerButton) {
			Registration registration = new Registration(accountService);
		    registration.setFileManager(fileManager);
		    registration.setVisible(true);
	    }
		else if (e.getSource() == loginButton) {
			messageLabel.setText(" ");
	        String username = usernameField.getText().trim();
	        char[] password = passwordField.getPassword();

	        if (username.isEmpty() || password.length == 0) { //reject missing credentials
	            JOptionPane.showMessageDialog(this, "Please enter your username and password.");
	            return;
	        }
	
        User user = accountService.login(username, password);

        if (user == null) {
            messageLabel.setText(accountService.getLastMessage());
    		pack();
            return;
        }
        if (user instanceof Administrator) {
            passwordField.setText("");
            AdminDashboard dashboard = new AdminDashboard(
                    (Administrator) user, accountService, scheduler, clock, this);
			// Give the admin screen the shared file manager.
            dashboard.setFileManager(fileManager);
            dashboard.setVisible(true);
            setVisible(false);
        } else if (user instanceof Student) {
            passwordField.setText("");
            StudentDashboard dashboard = new StudentDashboard(
                    (Student) user, scheduler, clock, this);
            dashboard.setFileManager(fileManager);
            dashboard.setVisible(true);
            setVisible(false); //hide Login page so dashboard can reopen it on logout
        }
		}
}}
