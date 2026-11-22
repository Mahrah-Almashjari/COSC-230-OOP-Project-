import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class Registration extends JFrame implements ActionListener {
    

    private final AccountService accountService;

    private JTextField studentIdField, nameField, emailField, phoneField;
    private JPasswordField passwordField;
    private JButton submitButton, backButton;
    private JLabel statusLabel;

    private FileManager fileManager;

    // Login supplies the same file manager to every screen.
    public void setFileManager(FileManager fileManager) {
    this.fileManager = fileManager;
}

    public Registration(AccountService accountService) {
        if (accountService == null) {
            throw new IllegalArgumentException("Account service is required.");
        }
        this.accountService = accountService;

        setTitle("GPU Management - Registration");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));

        JLabel title = new JLabel("Request a student account");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(title);
        form.add(Box.createVerticalStrut(10));

        JLabel explanation = new JLabel("Admin approval is required before you can sign in.");
        explanation.setForeground(Color.DARK_GRAY);
        explanation.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(explanation);
        form.add(Box.createVerticalStrut(20));

        studentIdField = new JTextField();
        nameField = new JTextField();
        emailField = new JTextField();
        phoneField = new JTextField();
        passwordField = new JPasswordField();

        addField(form, "University ID", studentIdField);
        addField(form, "Full name", nameField);
        addField(form, "Email", emailField);
        addField(form, "Phone number", phoneField);
        addField(form, "Password", passwordField);

        submitButton = new JButton("Submit request");
        backButton = new JButton("Back to login");
        submitButton.addActionListener(this);
        backButton.addActionListener(this);
        getRootPane().setDefaultButton(submitButton);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.add(submitButton);
        buttons.add(backButton);
        form.add(buttons);
        form.add(Box.createVerticalStrut(15));

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(new Color(30, 120, 50));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(statusLabel);

        setContentPane(form);
        pack();
        setLocationRelativeTo(null);
    }

    private void addField(JPanel form, String caption, JTextField field) {
        JLabel label = new JLabel(caption);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setAlignmentX(Component.CENTER_ALIGNMENT);

        Dimension size = new Dimension(420, 34);
        field.setPreferredSize(size);
        field.setMaximumSize(size);

        form.add(label);
        form.add(Box.createVerticalStrut(6));
        form.add(field);
        form.add(Box.createVerticalStrut(14));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == backButton) {
            // Login remains open behind this window.
            dispose();
            return;
        }

        if (e.getSource() == submitButton && submitButton.isEnabled()) {
            String studentId = studentIdField.getText().trim();
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            char[] password = passwordField.getPassword();

            if (studentId.isEmpty() || name.isEmpty() || email.isEmpty()
                    || phone.isEmpty() || password.length == 0) {
                JOptionPane.showMessageDialog(this,
                        "Please complete all five fields.",
                        "Missing information", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // AccountService checks duplicates. AccessRequest validates fields.
                accountService.register(studentId, name, email, phone,
                        new String(password));

      
                statusLabel.setText("Request submitted. Status: Pending.");
                submitButton.setEnabled(false);
                studentIdField.setEditable(false);
                nameField.setEditable(false);
                emailField.setEditable(false);
                phoneField.setEditable(false);
                passwordField.setText("");
                passwordField.setEditable(false);

                JOptionPane.showMessageDialog(this,
                        "Your registration request has been submitted.\n"
                        + "Wait for administrator approval before signing in.",
                        "Request submitted", JOptionPane.INFORMATION_MESSAGE);

            } catch (IllegalArgumentException | IllegalStateException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Registration failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
