import java.awt.Component;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import javax.swing.JOptionPane;
import model.*;

// Saves the connected application data, not the Swing windows
public class FileManager {
    private static final String FORMAT = "GPU_PROJECT_1";
    private final File file;
    private final User[] accounts;
    private final AccessRequest[] requests;
    private final Scheduler scheduler;
    private final SimulationClock clock;

    public FileManager(User[] accounts, AccessRequest[] requests,
                       Scheduler scheduler, SimulationClock clock) {
        this(new File("data/gpu-state.dat"), accounts, requests, scheduler, clock);
    }

    // The extra constructor lets tests use a separate file.
    public FileManager(File file, User[] accounts, AccessRequest[] requests,
                       Scheduler scheduler, SimulationClock clock) {
        if (file == null || accounts == null || requests == null
                || scheduler == null || clock == null) {
            throw new IllegalArgumentException("File and shared application objects are required.");
        }
        this.file = file.getAbsoluteFile();
        this.accounts = accounts;
        this.requests = requests;
        this.scheduler = scheduler;
        this.clock = clock;
    }

    public void save() throws IOException {
        File folder = file.getParentFile();
        if (!folder.isDirectory() && !folder.mkdirs()) {
            throw new IOException("Could not create folder: " + folder);
        }
        File temporary = new File(file.getPath() + ".tmp");

        // one stream preserves references shared by accounts, requests, jobs and GPUs.
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(temporary))) {
            out.writeUTF(FORMAT);
            out.writeObject(accounts);
            out.writeObject(requests);
            out.writeObject(clock.getDateTime());
            out.writeObject(clock.getInitialDateTime());
            scheduler.saveStorageState(out);
        }

        //replace the old file only after writing and closing the new one successfully
        Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    //False means first run: there is no saved file. Other failures are errors. 
    public boolean load() throws IOException, ClassNotFoundException {
        if (!file.exists()) return false;

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            if (!FORMAT.equals(in.readUTF())) {
                throw new IOException("Unsupported saved-file format.");
            }
            Object users = in.readObject();
            Object registrations = in.readObject();
            Object currentTime = in.readObject();
            Object initialTime = in.readObject();

            if (!(users instanceof User[]) || !(registrations instanceof AccessRequest[])) {
                throw new IOException("Invalid account or request data.");
            }
            if ((currentTime != null && !(currentTime instanceof LocalDateTime))
                    || (initialTime != null && !(initialTime instanceof LocalDateTime))) {
                throw new IOException("Invalid simulation time data.");
            }
            User[] savedAccounts = (User[]) users;
            AccessRequest[] savedRequests = (AccessRequest[]) registrations;
            LocalDateTime now = (LocalDateTime) currentTime;
            LocalDateTime initial = (LocalDateTime) initialTime;

            if (savedAccounts.length > accounts.length || savedRequests.length > requests.length) {
                throw new IOException("Saved arrays are larger than this application's arrays.");
            }
            if ((now == null) != (initial == null)
                    || (now != null && (initial.isAfter(now)
                    || now.getSecond() != 0 || now.getNano() != 0
                    || initial.getSecond() != 0 || initial.getNano() != 0))) {
                throw new IOException("Invalid simulation clock values.");
            }
            boolean hasAdmin = false;
            long nextId = 101;
            for (User user : savedAccounts) {
                if (user instanceof Administrator) hasAdmin = true;
                if (user instanceof Student) nextId = nextId((Student) user, nextId, now);
            }
            for (AccessRequest request : savedRequests) {
                if (request != null && request.getApprovedStudent() != null) {
                    nextId = nextId(request.getApprovedStudent(), nextId, now);
                }
            }
            if (!hasAdmin) throw new IOException("Saved administrator account is missing.");

            // Restore existing jobs directly. Do not submit them or reserve minutes again.
            scheduler.loadStorageState(in, nextId);
            clock.restoreStorageTime(now, initial);

            // Keep the array objects already shared with AccountService.
            Arrays.fill(accounts, null);
            Arrays.fill(requests, null);
            System.arraycopy(savedAccounts, 0, accounts, 0, savedAccounts.length);
            System.arraycopy(savedRequests, 0, requests, 0, savedRequests.length);
            return true;
        }
    }

    private long nextId(Student student, long nextId, LocalDateTime now) throws IOException {
        for (Job job : student.getJobs()) {
            if (now == null || job.getSubmissionTime().isAfter(now)) {
                throw new IOException("Job history does not match the saved clock.");
            }
            try {
                nextId = Math.max(nextId, Math.addExact(job.getJobNumber(), 1L));
            } catch (ArithmeticException ex) {
                throw new IOException("Saved job ID exceeds the supported range.", ex);
            }
        }
        return nextId;
    }

    /** For use after a successful action in a Swing screen. */
    public boolean saveWithMessage(Component parent) {
        try {
            save();
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent,
                    "The change is in memory, but could not be saved to:\n" + file
                    + "\n" + ex.getMessage() + "\nFix the problem and save again before closing.",
                    "File error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}
