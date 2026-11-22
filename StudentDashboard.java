import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.util.List;
import model.*;

//Student window with two tabs: Home (balance + submit job) and My jobs (search + cancel)
public class StudentDashboard extends JFrame implements ActionListener {

    private final Student student;
    private final JFrame loginWindow;
    private Timer refreshTimer;
    private GUI cancellationWindow;
    private final Scheduler scheduler;
    private final SimulationClock clock;

    private FileManager fileManager;

    // Login supplies the same file manager to every screen.
    public void setFileManager(FileManager fileManager) {
    this.fileManager = fileManager;
}

    // Header components
    private JLabel timeLabel;
    private JButton logoutButton;

    // Balance labels
    private JLabel allocatedValue, usedValue, reservedValue, availableValue;

    // Job submission form components
    private JTextField titleField, appField;
    private JTextArea descriptionArea;
    private JComboBox<Job.TaskSize> sizeBox;
    private JLabel durationLabel;
    private JButton submitJobButton;

    // My jobs tab components
    private JTextField searchField;
    private JComboBox<String> statusFilter;
    private JButton searchButton, cancelJobButton;
    private JTable jobsTable;
    private DefaultTableModel tableModel;

    public StudentDashboard(Student student, Scheduler scheduler, SimulationClock clock, JFrame loginWindow) {
        if (student == null || scheduler == null || clock == null || loginWindow == null) {
            throw new IllegalArgumentException("Student, scheduler, clock, and login window are required.");
        }
        this.student = student;
        this.loginWindow = loginWindow;
        this.scheduler = scheduler;
        this.clock = clock;

        setTitle("GPU Utilization Management - Student");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                logout();
            }
        });
        setSize(920, 620);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Top header bar with student info, simulated clock, and Log out button
        add(createHeaderPanel(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Home", createHomeTab());
        tabs.addTab("My jobs", createMyJobsTab());
        add(tabs, BorderLayout.CENTER);

        //Refresh labels and job rows to reflect any new changes
        refreshAll();
        refreshTimer = new Timer(500, e -> refreshAll());
        refreshTimer.start();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        //Left side: welcome text and student ID
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel welcome = new JLabel("Welcome, " + student.getName());
        welcome.setFont(new Font("SansSerif", Font.BOLD, 20));
        JLabel sub = new JLabel("Student • " + student.getStudentId());
        sub.setForeground(Color.DARK_GRAY);

        left.add(welcome);
        left.add(sub);

        // Right side: simulated time and Log out button
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        timeLabel = new JLabel("");
        timeLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        logoutButton = new JButton("Log out");
        logoutButton.addActionListener(this);

        right.add(timeLabel);
        right.add(logoutButton);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel createHomeTab() {
        JPanel home = new JPanel(new BorderLayout(15, 15));
        home.setBackground(Color.WHITE);
        home.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        //balance information row (Allocated, Used, Reserved, Available)
        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        cards.setOpaque(false);

        allocatedValue = new JLabel("0");
        usedValue = new JLabel("0");
        reservedValue = new JLabel("0");
        availableValue = new JLabel("0");

        cards.add(createBalanceCard("Allocated", allocatedValue, Color.BLACK));
        cards.add(createBalanceCard("Used", usedValue, Color.DARK_GRAY));
        cards.add(createBalanceCard("Reserved", reservedValue, new Color(180, 120, 0)));
        cards.add(createBalanceCard("Available", availableValue, new Color(20, 130, 60)));

        home.add(cards, BorderLayout.NORTH);

        //Submit a new job form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Submit a new job"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;

        titleField = new JTextField();
        appField = new JTextField();
        descriptionArea = new JTextArea(4, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        sizeBox = new JComboBox<>(Job.TaskSize.values());
        sizeBox.addActionListener(this);

        durationLabel = new JLabel("Duration: 5 minutes");
        durationLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        submitJobButton = new JButton("Submit job");
        submitJobButton.addActionListener(this);

        //Row 0: Labels for Title and Application
        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Job title"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; form.add(new JLabel("Application name"), gbc);

        //Row 1: Fields for Title and Application
        gbc.gridx = 0; gbc.gridy = 1; form.add(titleField, gbc);
        gbc.gridx = 1; gbc.gridy = 1; form.add(appField, gbc);

        //Row 2: Description label
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        form.add(new JLabel("Description"), gbc);

        // Row 3: Description text area
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        form.add(new JScrollPane(descriptionArea), gbc);

        // Row 4: Task size label
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1;
        form.add(new JLabel("Task size"), gbc);

        // Row 5: Task size dropdown, duration text, and submit button
        gbc.gridx = 0; gbc.gridy = 5;
        form.add(sizeBox, gbc);

        JPanel submitRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        submitRow.setOpaque(false);
        submitRow.add(durationLabel);
        submitRow.add(submitJobButton);
        gbc.gridx = 1; gbc.gridy = 5;
        form.add(submitRow, gbc);

        home.add(form, BorderLayout.CENTER);
        return home;
    }

    private JPanel createBalanceCard(String title, JLabel valueLabel, Color valueColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(248, 249, 250));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setForeground(Color.GRAY);

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valueLabel.setForeground(valueColor);

        card.add(titleLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(valueLabel);
        return card;
    }

    private JPanel createMyJobsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        //Search box, status filter, and cancel button
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topBar.setOpaque(false);

        searchField = new JTextField(20);
        statusFilter = new JComboBox<>(new String[]{
                "All statuses", "QUEUED", "RUNNING", "COMPLETED", "CANCELLED"
        });
        searchButton = new JButton("Search");
        cancelJobButton = new JButton("Cancel selected job");

        searchButton.addActionListener(this);
        statusFilter.addActionListener(this);
        cancelJobButton.addActionListener(this);

        topBar.add(new JLabel("Search:"));
        topBar.add(searchField);
        topBar.add(statusFilter);
        topBar.add(searchButton);
        topBar.add(cancelJobButton);

        panel.add(topBar, BorderLayout.NORTH);

        // Table showing only this student's jobs
        String[] columns = {
                "Job", "Title", "Application", "Size", "Status",
                "GPU", "Submitted", "Start", "Finish", "Wait", "Runtime", "Charged", "Result"
        };

        tableModel = new DefaultTableModel(columns, 0);
        jobsTable = new JTable(tableModel);
        jobsTable.setDefaultEditor(Object.class, null);
        

        jobsTable = new JTable(tableModel);
        jobsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jobsTable.setRowHeight(24);
        jobsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {75, 140, 120, 130, 110, 60, 150, 160, 160, 100, 100, 100, 240};
        for (int i = 0; i < widths.length; i++) {
            jobsTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        panel.add(new JScrollPane(jobsTable), BorderLayout.CENTER);

        JLabel note = new JLabel("~ = predicted (job still queued). Showing only your own jobs.");
        note.setForeground(Color.GRAY);
        panel.add(note, BorderLayout.SOUTH);

        return panel;
    }

    // Reloads clock, balance cards, and the student's jobs table
    public void refreshAll() {
        LocalDateTime now = clock.getDateTime();
        if (now == null) {
            timeLabel.setText("Simulated time: Not set");
        } else {
            timeLabel.setText("Simulated time: " + Job.formatTime(now));
        }

        TimeAllowance a = student.getAllowance();
        allocatedValue.setText(String.valueOf(a.getAllocated()));
        usedValue.setText(String.valueOf(a.getUsed()));
        reservedValue.setText(String.valueOf(a.getReserved()));
        availableValue.setText(String.valueOf(a.getAvailable()));
        //enables submit button only if clock is set and student has available minutes
        submitJobButton.setEnabled(now != null && a.getAvailable() > 0);

        refreshJobsTable();
    }

    //refresh the student's job table using the search and status filters.
    private void refreshJobsTable() {
        String selectedJobId = null;
      
        //stores selected job row index 
        int selectedRow = jobsTable.getSelectedRow();
      
        //If a row is selected, use the stored row index to extract job id and stores it
        if (selectedRow >= 0) {
            selectedJobId = (String) tableModel.getValueAt(selectedRow, 0);  //use selectedrow and 0 as rows and column indices
        }

        //clear displayed rows
        tableModel.setRowCount(0);
      
        //Reads current time and filter inputs
        LocalDateTime now = clock.getDateTime();  //Stores simulation time
        String query = searchField.getText().trim();  //Reads what user searched
        String selectedStatus = (String) statusFilter.getSelectedItem();  //reads selected dropdown option

        //returns all jobs if query is empty, otherwise returns jobs that match query
        List<Job> jobs = query.isEmpty() ? student.getJobs() : student.searchJobs(query);
        //loops through jobs that match query
      
        for (Job job : jobs) {
            //if job does not match selected status, continue
            if (!"All statuses".equals(selectedStatus) && !job.getStatus().name().equals(selectedStatus)) {
                continue;
            }

            //gets gpu id, if no gpu has been assigned, it assigns "-"
            String gpuText = job.getAssignedGpuId() == null ? "-" : "GPU" + job.getAssignedGpuId();

            //gets job status
            String statusText = job.getStatus().name();

            //placeholders for start, finish, and wait time
            String startText = "-";
            String finishText = "-";
            String waitText = "-";

            //conditional statements to choose what to store in start, finish, and wait based on status
            if (job.getStatus() == Job.Status.QUEUED && now != null) {  //if job is queued, display wait time with predicted start and finish
                int pos = scheduler.getQueuePosition(job);
                LocalDateTime predStart = scheduler.getPredictedStartTime(job);
                statusText = "Queued #" + pos;
                startText = "~ " + Job.formatTime(predStart);
                finishText = "~ " + Job.formatTime(job.getPredictedFinishTime(predStart));
                waitText = job.getRemainingWaitingTime(predStart, now) + " min";
            } else if (job.getStatus() == Job.Status.RUNNING && now != null) { //if running, display actual start, expected finish, and how long it waited
                startText = Job.formatTime(job.getStartTime());
                finishText = Job.formatTime(job.getExpectedFinishTime());
                waitText = job.getQueueWait(now) + " min";
            } else if (job.getStatus() == Job.Status.COMPLETED) { //if completed, display actual start and finish, how long it waited before starting
                startText = Job.formatTime(job.getStartTime());
                finishText = Job.formatTime(job.getFinishTime());
                waitText = job.getQueueWait(job.getFinishTime()) + " min";
            } else if (job.getStatus() == Job.Status.CANCELLED) { //if cancelled display actual start if started, cancellation time, and how long it waited before starting/cancelling
                if (job.getStartTime() != null) {
                    startText = Job.formatTime(job.getStartTime());
                }
                if (job.getFinishTime() != null) {
                    finishText = Job.formatTime(job.getFinishTime());
                }
                waitText = job.getQueueWait(job.getCancellationTime()) + " min";
            }

            //add this job's latest details back to the table in column order.
            tableModel.addRow(new Object[]{
                    job.getJobId(),
                    job.getTitle(),
                    job.getApplicationName(),
                    job.getTaskSize() + " (" + job.getEstimatedDuration() + ")",
                    statusText,
                    gpuText,
                    Job.formatTime(job.getSubmissionTime()),
                    startText,
                    finishText,
                    waitText,
                    now == null ? "-" : job.getRuntime(now) + " min",
                    job.getAllowanceCharged() + " min",
                    job.getResult()
            });
        }
        // Retain the selected job when automatic refresh rebuilds the rows.
        if (selectedJobId != null) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (selectedJobId.equals(tableModel.getValueAt(i, 0))) {
                    jobsTable.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
    }

    private void handleSubmitJob() {
        //check that the administrator has initialized the simulation clock.
        LocalDateTime now = clock.getDateTime();
        if (now == null) {
            JOptionPane.showMessageDialog(this,
                    "The simulation clock has not been set by the administrator yet.",
                    "Clock not set", JOptionPane.ERROR_MESSAGE);
          //stop submission because no valid simulation timestamp exists.
            return;
        }

        String title = titleField.getText().trim();
        String app = appField.getText().trim();
        String desc = descriptionArea.getText().trim();
        Job.TaskSize size = (Job.TaskSize) sizeBox.getSelectedItem();

        //If any field is empty, dialog message appears
        if (title.isEmpty() || app.isEmpty() || desc.isEmpty() || size == null) {
            JOptionPane.showMessageDialog(this,
                    "Please fill in title, application name, description, and task size.",
                    "Missing information", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int duration = size.getMinutes();
        int available = student.getAllowance().getAvailable();

        //if the student does not have enough minutes, dialog message appears
        if (duration > available) {
            JOptionPane.showMessageDialog(this,
                    "This job needs " + duration + " min, but you only have "
                    + available + " min available.\nChoose a smaller size or ask the administrator for more time.",
                    "Not enough GPU time", JOptionPane.ERROR_MESSAGE);
            return;
        }

      //Confirmation popup appears after user presses submit
        int confirm = JOptionPane.showConfirmDialog(this,
                "Submit \"" + title + "\" as a " + size + " job?\n"
                + "Duration: " + duration + " min\n"
                + "Available after submitting: " + (available - duration) + " min",
                "Confirm job", JOptionPane.OK_CANCEL_OPTION);

        //if user does not confirm, return
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }

        //if user does confirm, submit job and clear fields
        try {
            scheduler.submit(student, title, app, desc, size);

            // Save the submitted job and updated allowance.
        if (fileManager != null) {
            fileManager.saveWithMessage(this);
        }
            titleField.setText("");
            appField.setText("");
            descriptionArea.setText("");
            refreshAll();
            JOptionPane.showMessageDialog(this, "Job submitted successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Submission failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCancelJob() {
        // Get the row selected by the user
        int row = jobsTable.getSelectedRow();
        //return if no job is selected.
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a job to cancel.");
            return;
        }

        //read the job ID and find it in this student's history
        String jobId = (String) tableModel.getValueAt(row, 0);
        
        Job job = student.findJob(jobId);

        //allow cancellation only for queued or running jobs
        if (job == null || (job.getStatus() != Job.Status.QUEUED
                && job.getStatus() != Job.Status.RUNNING)) {
            JOptionPane.showMessageDialog(this, "Only queued or running jobs can be cancelled.");
            return;
        }

        //bring existing cancellation window forward instead of opening another
        if (cancellationWindow != null && cancellationWindow.isDisplayable()) {
            cancellationWindow.toFront();
            return;
        }
        // Create the cancellation window using the selected job and shared objects.        
        cancellationWindow = new GUI(job, scheduler, student, clock);

        //refresh the dashboard after the cancellation window closes
        cancellationWindow.addWindowListener(new WindowAdapter() {
            @Override
        public void windowClosed(WindowEvent e) {
        cancellationWindow = null;
    
        // Save only if the job was cancelled.
        if (job.getStatus() == Job.Status.CANCELLED
            && fileManager != null) {
        fileManager.saveWithMessage(StudentDashboard.this);
    }

    if (isDisplayable()) {
        refreshAll();
    }
}
        });
        cancellationWindow.showCancelDialog();
    }

    //close the dashboard and return to existing login screen
    private void logout() {
        dispose();
        loginWindow.setVisible(true);
    }

    //clean up dashboard resources before closing the window.
    @Override
    public void dispose() {
        //stop automatic refresh when the dashboard closes.
        if (refreshTimer != null) refreshTimer.stop();

        // Close any associated cancellation window
        if (cancellationWindow != null) cancellationWindow.dispose();

        //call the inherited method to close and release the dashboard window
        super.dispose();
    }

    //handle button clicks and dropdown selection change
    @Override
    public void actionPerformed(ActionEvent e) {
        //update the displayed duration when the task size changes
        if (e.getSource() == sizeBox) {
            Job.TaskSize selected = (Job.TaskSize) sizeBox.getSelectedItem();
            if (selected != null) {
                durationLabel.setText("Duration: " + selected.getMinutes() + " minutes");
            }
        } else if (e.getSource() == submitJobButton) { //validate and submit the entered job details.
            handleSubmitJob();
        } else if (e.getSource() == searchButton || e.getSource() == statusFilter) { //rebuild table using current search and status filters
            refreshJobsTable();
        } else if (e.getSource() == cancelJobButton) { //open cancellation for the selected job.
            handleCancelJob();
        } else if (e.getSource() == logoutButton) { //return the user to login
            logout();
        }
    }
}
