import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
public class Login extends JFrame implements ActionListener{
	JButton loginButton, registerButton;
	JTextField usernameField; JPasswordField passwordField;
	JLabel usernameLabel, passwordLabel, messageLabel;
	public Login() {
		usernameField = new JTextField();
		passwordField = new JPasswordField();
		
		loginButton = new JButton("Sign in");
		registerButton = new JButton("Request an account");
		
		usernameLabel = new JLabel("Username (university ID)");
		passwordLabel = new JLabel("Password");
		
		loginButton.addActionListener(this);
		registerButton.addActionListener(this);
		
		JPanel form = new JPanel();
		form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
		//form.setBackground(Color.WHITE);

		// Padding: top, left, bottom, right.
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
		JPanel registerRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		registerRow.add(new JLabel("New student?"));
		registerRow.add(registerButton);

		form.add(registerRow);
		
		messageLabel = new JLabel(" ");
		messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		messageLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
		form.add(Box.createVerticalStrut(15));
		form.add(messageLabel);

		setContentPane(form);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
		
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);

	}
	
	public static void main(String[] args) {
		User[] accounts = new User[100];
	    AccessRequest[] requests = new AccessRequest[100];

	    AccountService service =
	            new AccountService(accounts, requests);

	        new Login(service);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == registerButton) {
	        new Registration(accountService).setVisible(true);
	    }
		else if (e.getSource() == loginButton) {
	        String username = usernameField.getText().trim();
	        char[] password = passwordField.getPassword();

	        if (username.isEmpty() || password.length == 0) {
	            JOptionPane.showMessageDialog(this, "Please enter your username and password.");
	            return;
	        }
	
        User user = accountService.login(username, password);

        if (user == null) {
            JOptionPane.showMessageDialog(this,"Invalid credentials or account not approved.");
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

