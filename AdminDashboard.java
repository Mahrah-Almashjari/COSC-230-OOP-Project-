import java.awt.*;
import java.awt.event.*;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Locale;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

//Administrator screens. Domain changes go through AccountService/Scheduler/models
public class AdminDashboard extends JFrame implements ActionListener {
    private final Administrator admin;
    private final AccountService accountService;
    private final Scheduler scheduler;
    private final SimulationClock clock;
    private final JFrame loginWindow;
    private FileManager fileManager;
    private Timer refreshTimer;
    private GUI cancellationWindow;
    private WindowAdapter clockListener;
    private boolean refreshing;
    private Student selectedStudent;
    private String selectedRequestId;

    private JTabbedPane tabs;
    private JLabel timeLabel, messageLabel, selectedStudentLabel;
    private final JLabel[] summaryValues = new JLabel[6];
    private final JLabel[] studentBalances = new JLabel[4];
    private JButton advanceButton, setupButton, logoutButton;
    private JButton approveButton, rejectButton, requestDetailsButton;
    private JButton findStudentButton, grantButton, allocationButton;
    private JButton cancelWaitingButton, cancelStudentButton, cancelAllButton;
    private JButton studentDetailsButton, allDetailsButton, searchJobsButton;
    private JSpinner initialMinutes, grantMinutes, allocationMinutes;
    private JTextField studentIdField, grantReasonField, allocationReasonField, jobSearchField;
    private JTextArea grantHistory;
    private JComboBox<String> requestFilter, jobFilter;
    private JTable gpuTable, waitingTable, completedTable, balancesTable;
    private JTable requestsTable, studentJobsTable, allJobsTable;

    private static final Color BLUE = new Color(35, 105, 185);
    private static final String[] JOB_COLUMNS = {"Job", "Student", "Title", "Application",
        "Status", "GPU", "Queue position", "Start / predicted", "Finish / predicted",
        "Wait (min)", "Runtime (min)", "Charged (min)", "Result"};

    public AdminDashboard(Administrator admin, AccountService accountService,
                          Scheduler scheduler, SimulationClock clock, JFrame loginWindow) {
        if (admin == null || accountService == null || scheduler == null
                || clock == null || loginWindow == null) {
            throw new IllegalArgumentException("Admin, account service, scheduler, clock, and login window are required.");
        }
        accountService.getRequests(admin); // Verify the administrator belongs to the shared accounts.
        this.admin = admin;
        this.accountService = accountService;
        this.scheduler = scheduler;
        this.clock = clock;
        this.loginWindow = loginWindow;

        setTitle("GPU Utilization Management - Administrator");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { logout(); }
        });

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBackground(new Color(234, 242, 252));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        content.add(createHeader(), BorderLayout.NORTH);
        tabs = new JTabbedPane();
        tabs.addTab("Dashboard", createOverview());
        tabs.addTab("Requests", createRequestsPanel());
        tabs.addTab("Students", createStudentsPanel());
        tabs.addTab("All jobs", createAllJobsPanel());
        content.add(tabs, BorderLayout.CENTER);
        messageLabel = new JLabel(" ");
        content.add(messageLabel, BorderLayout.SOUTH);
        setContentPane(content);
        setSize(1200, 800);
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(loginWindow);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        clockListener = new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (isDisplayable()) {
                    if (clock.getDateTime() != null) afterChange("Simulation clock initialized.");
                    else refreshAll();
                }
            }
        };
        clock.addWindowListener(clockListener);
        refreshAll();
        // Display refresh only. Time advances solely through the +1 minute button.
        refreshTimer = new Timer(500, e -> refreshAll());
        refreshTimer.start();
    }

    // Receive the shared file manager from Login.
    public void setFileManager(FileManager fileManager) {
    this.fileManager = fileManager;
}

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 8));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(15, 18, 15, 18));
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 6));
        text.setOpaque(false);
        JLabel title = new JLabel("GPU Utilization Dashboard - Administrator");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(BLUE);
        timeLabel = new JLabel();
        text.add(title);
        text.add(timeLabel);
        header.add(text, BorderLayout.CENTER);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);
        setupButton = button("Set clock");
        advanceButton = button("+1 minute");
        logoutButton = button("Log out");
        controls.add(setupButton);
        controls.add(advanceButton);
        controls.add(logoutButton);
        header.add(controls, BorderLayout.EAST);
        return header;
    }

    private JPanel createOverview() {
        JPanel overview = paddedPanel(new BorderLayout(10, 12));
        JPanel summaries = new JPanel(new GridLayout(1, 6, 8, 0));
        String[] captions = {"Queued", "Running", "Completed", "Cancelled", "Busy GPUs", "Utilization"};
        for (int i = 0; i < captions.length; i++) {
            summaryValues[i] = new JLabel("-", SwingConstants.CENTER);
            summaryValues[i].setFont(new Font("SansSerif", Font.BOLD, 20));
            JPanel card = new JPanel(new BorderLayout());
            card.setBorder(BorderFactory.createTitledBorder(captions[i]));
            card.add(summaryValues[i], BorderLayout.CENTER);
            summaries.add(card);
        }
        overview.add(summaries, BorderLayout.NORTH);
        JTabbedPane sections = new JTabbedPane();
        gpuTable = table(new String[]{"GPU", "Availability", "Current job", "Remaining (min)", "Expected finish"});
        waitingTable = table(JOB_COLUMNS);
        completedTable = table(new String[]{"Job", "Student", "Application", "GPU", "Submitted",
            "Started", "Finished", "Runtime (min)", "Queue wait (min)", "Charged (min)"});
        balancesTable = table(new String[]{"Student ID", "Name", "Allocated", "Used", "Reserved", "Available"});
        sections.addTab("GPUs", new JScrollPane(gpuTable));
        JPanel waiting = new JPanel(new BorderLayout());
        waiting.add(new JScrollPane(waitingTable), BorderLayout.CENTER);
        cancelWaitingButton = button("Cancel selected job");
        waiting.add(buttonRow(cancelWaitingButton), BorderLayout.SOUTH);
        sections.addTab("Waiting jobs", waiting);
        sections.addTab("Student balances", new JScrollPane(balancesTable));
        sections.addTab("Completed jobs", new JScrollPane(completedTable));
        overview.add(sections, BorderLayout.CENTER);
        return overview;
    }

    private JPanel createRequestsPanel() {
        JPanel panel = paddedPanel(new BorderLayout(10, 12));
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        requestFilter = new JComboBox<>(new String[]{"All statuses", "PENDING", "APPROVED", "REJECTED"});
        requestFilter.setSelectedItem("PENDING");
        requestFilter.addActionListener(this);
        filters.add(new JLabel("Show registration requests:"));
        filters.add(requestFilter);
        panel.add(filters, BorderLayout.NORTH);
        requestsTable = table(new String[]{"University ID", "Name", "Email", "Phone", "Password", "Status"});
        requestsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!refreshing && !e.getValueIsAdjusting()) updateRequestControls();
        });
        panel.add(new JScrollPane(requestsTable), BorderLayout.CENTER);
        initialMinutes = new JSpinner(new SpinnerNumberModel(100, 0, Integer.MAX_VALUE, 1));
        initialMinutes.setPreferredSize(new Dimension(95, 30));
        approveButton = button("Approve");
        rejectButton = button("Reject");
        rejectButton.setBackground(new Color(170, 55, 55));
        requestDetailsButton = button("View request details");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        actions.add(new JLabel("Initial GPU minutes:"));
        actions.add(initialMinutes);
        actions.add(approveButton);
        actions.add(rejectButton);
        actions.add(requestDetailsButton);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createStudentsPanel() {
        JPanel panel = paddedPanel(new BorderLayout(10, 12));
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT));
        studentIdField = new JTextField(18);
        studentIdField.addActionListener(this);
        findStudentButton = button("Search student");
        search.add(new JLabel("University ID:"));
        search.add(studentIdField);
        search.add(findStudentButton);
        top.add(search);
        selectedStudentLabel = new JLabel("Search for an approved student account.");
        top.add(selectedStudentLabel);
        JPanel balances = new JPanel(new GridLayout(1, 4, 8, 0));
        String[] captions = {"Allocated", "Used", "Reserved", "Available"};
        for (int i = 0; i < captions.length; i++) {
            studentBalances[i] = new JLabel("-", SwingConstants.CENTER);
            studentBalances[i].setFont(new Font("SansSerif", Font.BOLD, 18));
            JPanel value = new JPanel(new BorderLayout());
            value.setBorder(BorderFactory.createTitledBorder(captions[i]));
            value.add(studentBalances[i], BorderLayout.CENTER);
            balances.add(value);
        }
        top.add(balances);

        JPanel grant = new JPanel(new FlowLayout(FlowLayout.LEFT));
        grantMinutes = new JSpinner(new SpinnerNumberModel(10, 1, Integer.MAX_VALUE, 1));
        grantReasonField = new JTextField(25);
        grantButton = button("Grant extra time");
        grant.add(new JLabel("Extra minutes:")); grant.add(grantMinutes);
        grant.add(new JLabel("Reason:")); grant.add(grantReasonField); grant.add(grantButton);
        top.add(grant);
        JPanel adjust = new JPanel(new FlowLayout(FlowLayout.LEFT));
        allocationMinutes = new JSpinner(new SpinnerNumberModel(100, 0, Integer.MAX_VALUE, 1));
        allocationReasonField = new JTextField(25);
        allocationButton = button("Adjust allocation");
        adjust.add(new JLabel("Total allocation:")); adjust.add(allocationMinutes);
        adjust.add(new JLabel("Reason:")); adjust.add(allocationReasonField); adjust.add(allocationButton);
        top.add(adjust);
        panel.add(top, BorderLayout.NORTH);

        studentJobsTable = table(JOB_COLUMNS);
        grantHistory = new JTextArea(5, 30);
        grantHistory.setEditable(false);
        grantHistory.setLineWrap(true);
        grantHistory.setWrapStyleWord(true);
        JScrollPane history = new JScrollPane(grantHistory);
        history.setBorder(BorderFactory.createTitledBorder("Time grant / allocation history"));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(studentJobsTable), history);
        split.setResizeWeight(0.75);
        panel.add(split, BorderLayout.CENTER);
        cancelStudentButton = button("Cancel selected job");
        studentDetailsButton = button("View job details");
        panel.add(buttonRow(studentDetailsButton, cancelStudentButton), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createAllJobsPanel() {
        JPanel panel = paddedPanel(new BorderLayout(10, 12));
        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jobSearchField = new JTextField(25);
        jobSearchField.addActionListener(this);
        jobFilter = new JComboBox<>(new String[]{"All statuses", "QUEUED", "RUNNING", "COMPLETED", "CANCELLED"});
        jobFilter.addActionListener(this);
        searchJobsButton = button("Search jobs");
        search.add(new JLabel("Search jobs / students:"));
        search.add(jobSearchField); search.add(jobFilter); search.add(searchJobsButton);
        panel.add(search, BorderLayout.NORTH);
        allJobsTable = table(JOB_COLUMNS);
        panel.add(new JScrollPane(allJobsTable), BorderLayout.CENTER);
        cancelAllButton = button("Cancel selected job");
        allDetailsButton = button("View job details");
        panel.add(buttonRow(allDetailsButton, cancelAllButton), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel paddedPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return panel;
    }

    private JButton button(String caption) {
        JButton button = new JButton(caption);
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        button.setBackground(BLUE); button.setForeground(Color.WHITE); button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        button.addActionListener(this);
        return button;
    }

    private JPanel buttonRow(JButton... buttons) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        for (JButton button : buttons) row.add(button);
        return row;
    }

    private JTable table(String[] columns) {
        JTable table = new JTable(new DefaultTableModel(columns, 0));
        table.setDefaultEditor(Object.class, null);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(25);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int i = 0; i < columns.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(145);
        return table;
    }

    private DefaultTableModel model(JTable table) { return (DefaultTableModel) table.getModel(); }

    private String selectedId(JTable table) {
        int row = table.getSelectedRow();
        return row < 0 ? null : String.valueOf(model(table).getValueAt(row, 0));
    }

    private void restoreSelection(JTable table, String id) {
        if (id == null) return;
        for (int i = 0; i < model(table).getRowCount(); i++) {
            if (id.equals(String.valueOf(model(table).getValueAt(i, 0)))) {
                table.setRowSelectionInterval(i, i);
                break;
            }
        }
    }

    // Rebuild display data only. Never advance time or change model state here.
    public void refreshAll() {
        if (refreshing) return;
        refreshing = true;
        try {
            LocalDateTime now = clock.getDateTime();
            timeLabel.setText(now == null ? "Simulation time: not initialized"
                    : "Simulation time: " + Job.formatTime(now));
            setupButton.setEnabled(now == null);
            advanceButton.setEnabled(now != null);

            Student[] students = accountService.getStudents(admin);
            Job[] jobs = accountService.getAllJobs(admin);
            int[] counts = new int[Job.Status.values().length];
            for (Job job : jobs) counts[job.getStatus().ordinal()]++;
            summaryValues[0].setText(String.valueOf(counts[Job.Status.QUEUED.ordinal()]));
            summaryValues[1].setText(String.valueOf(counts[Job.Status.RUNNING.ordinal()]));
            summaryValues[2].setText(String.valueOf(counts[Job.Status.COMPLETED.ordinal()]));
            summaryValues[3].setText(String.valueOf(counts[Job.Status.CANCELLED.ordinal()]));

            model(gpuTable).setRowCount(0);
            int busy = 0;
            for (GPU gpu : scheduler.getGpus()) {
                if (!gpu.isFree()) busy++;
                model(gpuTable).addRow(new Object[]{"GPU" + gpu.getId(), gpu.isFree() ? "Free" : "Busy",
                    gpu.isFree() ? "-" : gpu.getCurrentJob().getJobId(),
                    now == null ? "-" : gpu.getRemainingRuntime(now), Job.formatTime(gpu.getExpectedFinishTime())});
            }
            summaryValues[4].setText(busy + " / 4");
            double utilization = now == null ? Double.NaN : scheduler.getUtilization();
            boolean timeZero = now == null || clock.getInitialDateTime() == null
                    || !now.isAfter(clock.getInitialDateTime());
            summaryValues[5].setText(timeZero || Double.isNaN(utilization) ? "N/A"
                    : String.format(Locale.ROOT, "%.1f%%", utilization));

            String balanceSelection = selectedId(balancesTable);
            model(balancesTable).setRowCount(0);
            for (Student student : students) {
                TimeAllowance a = student.getAllowance();
                model(balancesTable).addRow(new Object[]{student.getStudentId(), student.getName(),
                    a.getAllocated(), a.getUsed(), a.getReserved(), a.getAvailable()});
            }
            restoreSelection(balancesTable, balanceSelection);
            fillJobs(waitingTable, accountService.searchAllJobs(admin, "", Job.Status.QUEUED));
            String completedSelection = selectedId(completedTable);
            model(completedTable).setRowCount(0);
            for (Job job : jobs) {
                if (job.getStatus() == Job.Status.COMPLETED) {
                    model(completedTable).addRow(new Object[]{job.getJobId(), job.getStudentId(),
                        job.getApplicationName(), job.getAssignedGpuId(), Job.formatTime(job.getSubmissionTime()),
                        Job.formatTime(job.getStartTime()), Job.formatTime(job.getFinishTime()),
                        job.getRuntime(job.getFinishTime()), job.getQueueWait(job.getFinishTime()), job.getAllowanceCharged()});
                }
            }
            restoreSelection(completedTable, completedSelection);
            refreshRequests();
            refreshStudent();
            String filter = (String) jobFilter.getSelectedItem();
            Job.Status status = "All statuses".equals(filter) ? null : Job.Status.valueOf(filter);
            fillJobs(allJobsTable, accountService.searchAllJobs(admin, jobSearchField.getText(), status));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            messageLabel.setForeground(Color.RED.darker());
            messageLabel.setText(ex.getMessage());
        } finally {
            refreshing = false;
            updateRequestControls();
        }
    }

    private void refreshRequests() {
        String selection = selectedId(requestsTable);
        model(requestsTable).setRowCount(0);
        String filter = (String) requestFilter.getSelectedItem();
        int pending = 0;
        for (AccessRequest request : accountService.getRequests(admin)) {
            if (request == null) continue;
            if (request.getStatus() == AccessRequest.Status.PENDING) pending++;
            if (!"All statuses".equals(filter) && !request.getStatus().name().equals(filter)) continue;
            model(requestsTable).addRow(new Object[]{request.getStudentId(), request.getName(),
                request.getEmail(), request.getPhone(), "••••••", request.getStatus()});
        }
        tabs.setTitleAt(1, "Requests (" + pending + ")");
        restoreSelection(requestsTable, selection);
    }

    private AccessRequest selectedRequest() {
        String id = selectedId(requestsTable);
        if (id == null) return null;
        for (AccessRequest request : accountService.getRequests(admin)) {
            if (request != null && id.equals(request.getStudentId())) return request;
        }
        return null;
    }

    private void updateRequestControls() {
        if (approveButton == null) return;
        AccessRequest request = selectedRequest();
        boolean pending = request != null && request.getStatus() == AccessRequest.Status.PENDING;
        approveButton.setEnabled(pending);
        rejectButton.setEnabled(pending);
        initialMinutes.setEnabled(pending);
        requestDetailsButton.setEnabled(request != null);
        if (pending && !request.getStudentId().equals(selectedRequestId)) initialMinutes.setValue(100);
        selectedRequestId = request == null ? null : request.getStudentId();
    }

    private void refreshStudent() {
        boolean found = selectedStudent != null;
        grantButton.setEnabled(found); allocationButton.setEnabled(found);
        if (!found) {
            for (JLabel value : studentBalances) value.setText("-");
            fillJobs(studentJobsTable, new Job[0]);
            grantHistory.setText("");
            return;
        }
        selectedStudentLabel.setText(selectedStudent.getName() + " | " + selectedStudent.getStudentId()
                + " | " + selectedStudent.getEmail() + " | " + selectedStudent.getPhone());
        TimeAllowance a = selectedStudent.getAllowance();
        int[] values = {a.getAllocated(), a.getUsed(), a.getReserved(), a.getAvailable()};
        for (int i = 0; i < values.length; i++) studentBalances[i].setText(String.valueOf(values[i]));
        fillJobs(studentJobsTable, selectedStudent.getJobs().toArray(new Job[0]));
        StringBuilder history = new StringBuilder();
        for (String entry : a.getGrantHistory()) history.append(entry).append('\n');
        grantHistory.setText(history.length() == 0 ? "No time changes recorded." : history.toString());
    }

    private void fillJobs(JTable table, Job[] jobs) {
        String selection = selectedId(table);
        model(table).setRowCount(0);
        LocalDateTime now = clock.getDateTime();
        for (Job job : jobs) {
            LocalDateTime start = job.getStartTime();
            LocalDateTime finish = job.getFinishTime();
            Object position = "-", wait = "-", runtime = "-";
            if (now != null) {
                runtime = job.getRuntime(now);
                if (job.getStatus() == Job.Status.QUEUED) {
                    int pos = scheduler.getQueuePosition(job);
                    position = pos > 0 ? pos : "Unavailable";
                    if (pos > 0) {
                        start = scheduler.getPredictedStartTime(job);
                        finish = job.getPredictedFinishTime(start);
                        wait = job.getRemainingWaitingTime(start, now);
                    }
                } else {
                    wait = job.getQueueWait(now);
                }
            }
            if (job.getStatus() == Job.Status.RUNNING) finish = job.getExpectedFinishTime();
            String prefix = job.getStatus() == Job.Status.QUEUED ? "~ " : "";
            model(table).addRow(new Object[]{job.getJobId(), job.getStudentId(), job.getTitle(),
                job.getApplicationName(), job.getStatus(), job.getAssignedGpuId() == null ? "-" : job.getAssignedGpuId(),
                position, start == null ? "-" : prefix + Job.formatTime(start),
                finish == null ? "-" : prefix + Job.formatTime(finish), wait, runtime,
                job.getAllowanceCharged(), job.getResult()});
        }
        restoreSelection(table, selection);
    }

    private Job selectedJob(JTable table) {
        String id = selectedId(table);
        if (id != null) {
            for (Job job : accountService.getAllJobs(admin)) if (id.equals(job.getJobId())) return job;
        }
        throw new IllegalArgumentException("Select a job first.");
    }

    private void cancelJob(JTable table) {
        Job job = selectedJob(table);
        if (job.getStatus() != Job.Status.QUEUED && job.getStatus() != Job.Status.RUNNING) {
            throw new IllegalStateException("Only queued or running jobs can be cancelled.");
        }
        if (cancellationWindow != null && cancellationWindow.isDisplayable()) {
            cancellationWindow.toFront(); return;
        }
        final Job.Status previous = job.getStatus();
        cancellationWindow = new GUI(job, scheduler, admin, clock);
        cancellationWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
            	cancellationWindow = null;
            	if (previous != Job.Status.CANCELLED
            	        && job.getStatus() == Job.Status.CANCELLED) {
            	    afterChange(job.getJobId() + " cancelled.");
            	} else if (isDisplayable()) {
            	    refreshAll();
            	}
            }
        });
        cancellationWindow.showCancelDialog();
    }

    private void showJobDetails(JTable table) {
        Job job = selectedJob(table);
        LocalDateTime now = clock.getDateTime();
        String details = "Job: " + job.getJobId() + "\nStudent: " + job.getStudentId()
                + "\nTitle: " + job.getTitle() + "\nApplication: " + job.getApplicationName()
                + "\nDescription: " + job.getDescription() + "\nStatus: " + job.getStatus()
                + "\nDuration: " + job.getEstimatedDuration() + " minutes"
                + "\nGPU: " + job.getAssignedGpuId()
                + "\nSubmitted: " + Job.formatTime(job.getSubmissionTime())
                + "\nStarted: " + Job.formatTime(job.getStartTime())
                + "\nFinished: " + Job.formatTime(job.getFinishTime())
                + "\nRuntime: " + (now == null ? "Unavailable" : job.getRuntime(now))
                + "\nQueue wait: " + (now == null ? "Unavailable" : job.getQueueWait(now))
                + "\nAllowance charged: " + job.getAllowanceCharged()
                + "\nResult: " + job.getResult();
        if (job.getStatus() == Job.Status.CANCELLED) details += "\nCancelled: "
                + Job.formatTime(job.getCancellationTime()) + "\nBy: " + job.getCancelledBy()
                + "\nReason: " + job.getCancellationReason();
        showText("Job details", details);
    }

    private void showText(String caption, String content) {
        JTextArea text = new JTextArea(content, 16, 55);
        text.setEditable(false); text.setLineWrap(true); text.setWrapStyleWord(true); text.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(text), caption, JOptionPane.INFORMATION_MESSAGE);
    }

    private int readMinutes(JSpinner spinner) throws ParseException {
        spinner.commitEdit();
        Number number = (Number) spinner.getValue();
        int value = number.intValue();
        if (number.doubleValue() != value || value < 0) {
            throw new IllegalArgumentException("Minutes must be a non-negative whole number within the supported range.");
        }
        return value;
    }

    private void afterChange(String message) {
        refreshAll();
        messageLabel.setForeground(new Color(25, 115, 50));
        messageLabel.setText(message);

        // Save the administrator's changes.
        if (fileManager != null) {
        fileManager.saveWithMessage(this);
    }
        }
    

    private void showError(String message) {
        messageLabel.setForeground(Color.RED.darker()); messageLabel.setText(message);
        JOptionPane.showMessageDialog(this, message, "Cannot complete action", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        try {
            if (source == logoutButton) logout();
            else if (source == setupButton) {
                clock.showSetupWindow();
            }
            else if (source == advanceButton) {
                scheduler.advanceMinute();
                afterChange("Simulation advanced by one minute.");
            } else if (source == approveButton) {
                AccessRequest request = selectedRequest();
                if (request == null) throw new IllegalArgumentException("Select a pending request.");
                int minutes = readMinutes(initialMinutes);
                if (JOptionPane.showConfirmDialog(this, "Approve " + request.getStudentId() + " with "
                        + minutes + " GPU minutes?", "Approve account", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    accountService.approveRequest(admin, request, minutes);
                    afterChange("Approved " + request.getStudentId() + ".");
                }
            } else if (source == rejectButton) {
                AccessRequest request = selectedRequest();
                if (request == null) throw new IllegalArgumentException("Select a pending request.");
                if (JOptionPane.showConfirmDialog(this, "Reject " + request.getStudentId() + "?",
                        "Reject request", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    accountService.rejectRequest(admin, request);
                    afterChange("Rejected " + request.getStudentId() + ".");
                }
            } else if (source == requestDetailsButton) {
                AccessRequest request = selectedRequest();
                if (request == null) throw new IllegalArgumentException("Select a request.");
                showText("Registration request", "University ID: " + request.getStudentId()
                        + "\nName: " + request.getName() + "\nEmail: " + request.getEmail()
                        + "\nPhone: " + request.getPhone() + "\nChosen password: " + request.getPassword()
                        + "\nStatus: " + request.getStatus());
            } else if (source == findStudentButton || source == studentIdField) {
                selectedStudent = accountService.findStudent(admin, studentIdField.getText());
                if (selectedStudent == null) {
                    selectedStudentLabel.setText("No matching approved student account.");
                    refreshAll();
                    throw new IllegalArgumentException("No matching approved student account was found.");
                }
                allocationMinutes.setValue(selectedStudent.getAllowance().getAllocated());
                refreshAll();
            } else if (source == grantButton) {
            // Pass simulation time so the grant history records when it happened.
            accountService.grantTime(
            admin,
            selectedStudent,
            readMinutes(grantMinutes),
            grantReasonField.getText(),
            clock.getDateTime()
        );

            grantReasonField.setText("");
            afterChange("Extra time granted to "
            + selectedStudent.getStudentId() + ".");
            }
                 else if (source == allocationButton) {
                accountService.setAllocation(admin, selectedStudent, readMinutes(allocationMinutes), allocationReasonField.getText());
                allocationReasonField.setText("");
                afterChange("Allocation adjusted for " + selectedStudent.getStudentId() + ".");
            } else if (source == cancelWaitingButton) cancelJob(waitingTable);
            else if (source == cancelStudentButton) cancelJob(studentJobsTable);
            else if (source == cancelAllButton) cancelJob(allJobsTable);
            else if (source == studentDetailsButton) showJobDetails(studentJobsTable);
            else if (source == allDetailsButton) showJobDetails(allJobsTable);
            else refreshAll(); // Request/status filters, search button, or Enter in search.
        } catch (ParseException ex) {
            showError("Enter valid whole minutes.");
        } catch (ArithmeticException ex) {
            showError("The requested minutes exceed the supported range.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showError(ex.getMessage());
        }
    }

    private void logout() {
        dispose();
        loginWindow.setVisible(true);
    }

    @Override
    public void dispose() {
        if (refreshTimer != null) refreshTimer.stop();
        if (clockListener != null) clock.removeWindowListener(clockListener);
        if (cancellationWindow != null) cancellationWindow.dispose();
        if (clock != null && clock.getDateTime() == null) clock.dispose();
        super.dispose();
    }
}
