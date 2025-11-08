import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.filechooser.FileNameExtensionFilter;

// Model Classes
class Patient {

    private int id;
    private String name;
    private int age;
    private String gender;
    private String phone;
    private String address;
    private String bloodGroup;

    public Patient(int id, String name, int age, String gender, String phone, String address, String bloodGroup) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.phone = phone;
        this.address = address;
        this.bloodGroup = bloodGroup;
    }

    public Patient(String name, int age, String gender, String phone, String address, String bloodGroup) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.phone = phone;
        this.address = address;
        this.bloodGroup = bloodGroup;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }
}

class Visit {
    private int id;
    private int patientId;
    private java.sql.Date visitDate;
    private String doctor;
    private String notes;

    public Visit(int id, int patientId, java.sql.Date visitDate, String doctor, String notes) {
        this.id = id;
        this.patientId = patientId;
        this.visitDate = visitDate;
        this.doctor = doctor;
        this.notes = notes;
    }
    public Visit(int patientId, java.sql.Date visitDate, String doctor, String notes) {
        this.patientId = patientId;
        this.visitDate = visitDate;
        this.doctor = doctor;
        this.notes = notes;
    }
    public int getId() { return id; }
    public int getPatientId() { return patientId; }
    public java.sql.Date getVisitDate() { return visitDate; }
    public String getDoctor() { return doctor; }
    public String getNotes() { return notes; }
}

class DatabaseConfig {

    // These should ideally be loaded from a configuration file
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String USER = "system";
    private static final String PASSWORD = "123";

    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new SQLException("Database connection failed: " + e.getMessage(), e);
        }
    }

    // Initialize database tables if they don't exist
    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // Check if patients table exists
            try {
                ResultSet rs = conn.getMetaData().getTables(null, USER.toUpperCase(), "PATIENTS", null);
                if (!rs.next()) {
                    // Create patients table
                    stmt.execute("CREATE TABLE patients ("
                            + "id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
                            + "name VARCHAR2(100) NOT NULL, "
                            + "age NUMBER NOT NULL, "
                            + "gender VARCHAR2(1) NOT NULL, "
                            + "phone VARCHAR2(15) NOT NULL, "
                            + "address VARCHAR2(200), "
                            + "blood_group VARCHAR2(5))");
                    System.out.println("Patients table created successfully");
                }
            } catch (SQLException e) {
                System.err.println("Error checking/creating patients table: " + e.getMessage());
            }

            // Check if visits table exists
            try {
                ResultSet rs = conn.getMetaData().getTables(null, USER.toUpperCase(), "VISITS", null);
                if (!rs.next()) {
                    stmt.execute("CREATE TABLE visits ("
                        + "id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
                        + "patient_id NUMBER NOT NULL, "
                        + "visit_date DATE NOT NULL, "
                        + "doctor VARCHAR2(100) NOT NULL, "
                        + "notes VARCHAR2(500), "
                        + "FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE)");
                    System.out.println("Visits table created successfully");
                }
            } catch (SQLException e) {
                System.err.println("Error checking/creating visits table: " + e.getMessage());
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database initialization failed: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

class PatientDAO {

    public void addPatient(Patient p) throws SQLException {
        String sql = "INSERT INTO patients (name, age, gender, phone, address, blood_group) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setInt(2, p.getAge());
            ps.setString(3, p.getGender());
            ps.setString(4, p.getPhone());
            ps.setString(5, p.getAddress());
            ps.setString(6, p.getBloodGroup());
            ps.executeUpdate();
        }
    }

    public void updatePatient(Patient p) throws SQLException {
        String sql = "UPDATE patients SET name=?, age=?, gender=?, phone=?, address=?, blood_group=? WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setInt(2, p.getAge());
            ps.setString(3, p.getGender());
            ps.setString(4, p.getPhone());
            ps.setString(5, p.getAddress());
            ps.setString(6, p.getBloodGroup());
            ps.setInt(7, p.getId());
            ps.executeUpdate();
        }
    }

    public void deletePatient(int id) throws SQLException {
        String sql = "DELETE FROM patients WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Patient> getAllPatients() throws SQLException {
        List<Patient> patients = new ArrayList<>();
        String sql = "SELECT id, name, age, gender, phone, address, blood_group FROM patients ORDER BY id";
        try (Connection conn = DatabaseConfig.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Patient p = new Patient(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("age"),
                        rs.getString("gender"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getString("blood_group")
                );
                patients.add(p);
            }
        }
        return patients;
    }

    public List<Patient> searchPatients(String searchTerm) throws SQLException {
        List<Patient> patients = new ArrayList<>();
        String sql = "SELECT id, name, age, gender, phone, address, blood_group FROM patients "
                + "WHERE LOWER(name) LIKE ? OR phone LIKE ? ORDER BY id";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            String term = "%" + searchTerm.toLowerCase() + "%";
            ps.setString(1, term);
            ps.setString(2, term);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Patient p = new Patient(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("age"),
                            rs.getString("gender"),
                            rs.getString("phone"),
                            rs.getString("address"),
                            rs.getString("blood_group")
                    );
                    patients.add(p);
                }
            }
        }
        return patients;
    }

    public Patient getPatientById(int id) throws SQLException {
        String sql = "SELECT id, name, age, gender, phone, address, blood_group FROM patients WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Patient(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("age"),
                            rs.getString("gender"),
                            rs.getString("phone"),
                            rs.getString("address"),
                            rs.getString("blood_group")
                    );
                }
            }
        }
        return null;
    }
}

class VisitDAO {
    public void addVisit(Visit v) throws SQLException {
        String sql = "INSERT INTO visits (patient_id, visit_date, doctor, notes) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, v.getPatientId());
            ps.setDate(2, v.getVisitDate());
            ps.setString(3, v.getDoctor());
            ps.setString(4, v.getNotes());
            ps.executeUpdate();
        }
    }
    public List<Visit> getVisitsForPatient(int patientId) throws SQLException {
        List<Visit> visits = new ArrayList<>();
        String sql = "SELECT id, patient_id, visit_date, doctor, notes FROM visits WHERE patient_id = ? ORDER BY visit_date DESC";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    visits.add(new Visit(
                        rs.getInt("id"),
                        rs.getInt("patient_id"),
                        rs.getDate("visit_date"),
                        rs.getString("doctor"),
                        rs.getString("notes")
                    ));
                }
            }
        }
        return visits;
    }
}

// GUI Classes
class PatientFormPanel extends JPanel {

    private JTextField nameField;
    private JTextField ageField;
    private JComboBox<String> genderCombo;
    private JTextField phoneField;
    private JTextArea addressArea;
    private JComboBox<String> bloodGroupCombo;
    private JButton actionButton;
    private JButton clearButton;
    private JButton deleteButton;
    private JButton visitHistoryButton;
    private VisitDAO visitDAO = new VisitDAO();

    private int currentPatientId = -1;
    private PatientDAO patientDAO;
    private PatientTablePanel tablePanel;

    public PatientFormPanel(PatientDAO patientDAO, PatientTablePanel tablePanel) {
        this.patientDAO = patientDAO;
        this.tablePanel = tablePanel;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Patient Information"));

        // Input fields panel
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        fieldsPanel.add(new JLabel("Name:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        nameField = new JTextField(20);
        fieldsPanel.add(nameField, gbc);

        // Age
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        fieldsPanel.add(new JLabel("Age:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        ageField = new JTextField(5);
        fieldsPanel.add(ageField, gbc);

        // Gender
        gbc.gridx = 0;
        gbc.gridy = 2;
        fieldsPanel.add(new JLabel("Gender:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        String[] genders = {"M", "F", "O"};
        genderCombo = new JComboBox<>(genders);
        fieldsPanel.add(genderCombo, gbc);

        // Phone
        gbc.gridx = 0;
        gbc.gridy = 3;
        fieldsPanel.add(new JLabel("Phone:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        phoneField = new JTextField(15);
        fieldsPanel.add(phoneField, gbc);

        // Blood Group
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        fieldsPanel.add(new JLabel("Blood Group:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 4;
        String[] bloodGroups = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        bloodGroupCombo = new JComboBox<>(bloodGroups);
        fieldsPanel.add(bloodGroupCombo, gbc);

        // Address
        gbc.gridx = 0;
        gbc.gridy = 5;
        fieldsPanel.add(new JLabel("Address:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.gridheight = 2;
        addressArea = new JTextArea(3, 20);
        addressArea.setLineWrap(true);
        JScrollPane addressScroll = new JScrollPane(addressArea);
        fieldsPanel.add(addressScroll, gbc);

        add(fieldsPanel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        actionButton = new JButton("Add Patient");
        actionButton.setBackground(new Color(100, 180, 100));
        actionButton.setForeground(Color.WHITE);
        actionButton.setFocusPainted(false);

        clearButton = new JButton("Clear Form");

        deleteButton = new JButton("Delete");
        deleteButton.setBackground(new Color(220, 80, 80));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFocusPainted(false);
        deleteButton.setEnabled(false);

        visitHistoryButton = new JButton("Visit History");
        visitHistoryButton.setBackground(new Color(70, 130, 180));
        visitHistoryButton.setForeground(Color.WHITE);
        visitHistoryButton.setFocusPainted(false);
        visitHistoryButton.setEnabled(false);

        buttonPanel.add(actionButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(visitHistoryButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // Add action listeners
        actionButton.addActionListener(e -> savePatient());
        clearButton.addActionListener(e -> clearForm());
        deleteButton.addActionListener(e -> deletePatient());
        visitHistoryButton.addActionListener(e -> showVisitHistoryDialog());
    }

    private void savePatient() {
        // Validate input
        if (!validateInput()) {
            return;
        }

        try {
            String name = nameField.getText().trim();
            int age = Integer.parseInt(ageField.getText().trim());
            String gender = genderCombo.getSelectedItem().toString();
            String phone = phoneField.getText().trim();
            String address = addressArea.getText().trim();
            String bloodGroup = bloodGroupCombo.getSelectedItem().toString();

            if (currentPatientId == -1) {
                // Add new patient
                Patient newPatient = new Patient(name, age, gender, phone, address, bloodGroup);
                patientDAO.addPatient(newPatient);
                JOptionPane.showMessageDialog(this, "Patient added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                // Update existing patient
                Patient updatedPatient = new Patient(currentPatientId, name, age, gender, phone, address, bloodGroup);
                patientDAO.updatePatient(updatedPatient);
                JOptionPane.showMessageDialog(this, "Patient updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }

            clearForm();
            tablePanel.refreshTable();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deletePatient() {
        if (currentPatientId == -1) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this patient?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                patientDAO.deletePatient(currentPatientId);
                JOptionPane.showMessageDialog(this, "Patient deleted successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                tablePanel.refreshTable();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error deleting patient: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private boolean validateInput() {
        // Name validation
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter patient name",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            nameField.requestFocus();
            return false;
        }

        // Age validation
        try {
            int age = Integer.parseInt(ageField.getText().trim());
            if (age <= 0 || age > 150) {
                JOptionPane.showMessageDialog(this, "Please enter a valid age (1-150)",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                ageField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid age",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            ageField.requestFocus();
            return false;
        }

        // Phone validation
        String phone = phoneField.getText().trim();
        if (phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter phone number",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            phoneField.requestFocus();
            return false;
        }

        // Simple phone format validation
        if (!Pattern.matches("\\d{10,15}", phone)) {
            JOptionPane.showMessageDialog(this, "Please enter a valid phone number (10-15 digits)",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            phoneField.requestFocus();
            return false;
        }

        return true;
    }

    public void clearForm() {
        nameField.setText("");
        ageField.setText("");
        genderCombo.setSelectedIndex(0);
        phoneField.setText("");
        addressArea.setText("");
        bloodGroupCombo.setSelectedIndex(0);
        currentPatientId = -1;
        actionButton.setText("Add Patient");
        deleteButton.setEnabled(false);
        visitHistoryButton.setEnabled(false);
    }

    public void loadPatient(int patientId) {
        try {
            Patient patient = patientDAO.getPatientById(patientId);
            if (patient != null) {
                currentPatientId = patient.getId();
                nameField.setText(patient.getName());
                ageField.setText(String.valueOf(patient.getAge()));

                // Set gender
                for (int i = 0; i < genderCombo.getItemCount(); i++) {
                    if (genderCombo.getItemAt(i).equals(patient.getGender())) {
                        genderCombo.setSelectedIndex(i);
                        break;
                    }
                }

                phoneField.setText(patient.getPhone());
                addressArea.setText(patient.getAddress());

                // Set blood group
                for (int i = 0; i < bloodGroupCombo.getItemCount(); i++) {
                    if (bloodGroupCombo.getItemAt(i).equals(patient.getBloodGroup())) {
                        bloodGroupCombo.setSelectedIndex(i);
                        break;
                    }
                }

                actionButton.setText("Update Patient");
                deleteButton.setEnabled(true);
                visitHistoryButton.setEnabled(true);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading patient: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showVisitHistoryDialog() {
        if (currentPatientId == -1) {
            JOptionPane.showMessageDialog(this, "No patient selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Visit History", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        // Table for visits
        String[] columns = {"Date", "Doctor", "Notes"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        dialog.add(scrollPane, BorderLayout.CENTER);

        // Load visits
        try {
            for (Visit v : visitDAO.getVisitsForPatient(currentPatientId)) {
                model.addRow(new Object[] { v.getVisitDate(), v.getDoctor(), v.getNotes() });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading visits: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        // Panel to add new visit
        JPanel addPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;
        addPanel.add(new JLabel("Date (yyyy-mm-dd):"), gbc);
        gbc.gridx = 1;
        JTextField dateField = new JTextField(10);
        dateField.setText(new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
        addPanel.add(dateField, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        addPanel.add(new JLabel("Doctor:"), gbc);
        gbc.gridx = 1;
        JTextField doctorField = new JTextField(15);
        addPanel.add(doctorField, gbc);
        gbc.gridx = 0; gbc.gridy = 2;
        addPanel.add(new JLabel("Notes:"), gbc);
        gbc.gridx = 1;
        JTextField notesField = new JTextField(20);
        addPanel.add(notesField, gbc);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JButton addVisitBtn = new JButton("Add Visit");
        addPanel.add(addVisitBtn, gbc);
        dialog.add(addPanel, BorderLayout.SOUTH);

        addVisitBtn.addActionListener(ev -> {
            try {
                java.sql.Date visitDate = java.sql.Date.valueOf(dateField.getText().trim());
                String doctor = doctorField.getText().trim();
                String notes = notesField.getText().trim();
                if (doctor.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Doctor name required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Visit v = new Visit(currentPatientId, visitDate, doctor, notes);
                visitDAO.addVisit(v);
                model.setRowCount(0);
                for (Visit visit : visitDAO.getVisitsForPatient(currentPatientId)) {
                    model.addRow(new Object[] { visit.getVisitDate(), visit.getDoctor(), visit.getNotes() });
                }
                doctorField.setText("");
                notesField.setText("");
                JOptionPane.showMessageDialog(dialog, "Visit added!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error adding visit: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }
}

class PatientTablePanel extends JPanel {

    private JTable patientTable;
    private DefaultTableModel tableModel;
    private PatientDAO patientDAO;
    private PatientFormPanel formPanel;
    private JTextField searchField;

    public PatientTablePanel(PatientDAO patientDAO) {
        this.patientDAO = patientDAO;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Patient List"));

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        searchField = new JTextField(20);
        searchPanel.add(searchField);

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> searchPatients());
        searchPanel.add(searchButton);

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> {
            searchField.setText("");
            refreshTable();
        });
        searchPanel.add(resetButton);

        add(searchPanel, BorderLayout.NORTH);

        // Table
        String[] columnNames = {"ID", "Name", "Age", "Gender", "Phone", "Blood Group", "Address"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };

        patientTable = new JTable(tableModel);
        patientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        patientTable.getTableHeader().setReorderingAllowed(false);

        // Set column widths
        patientTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // ID
        patientTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Name
        patientTable.getColumnModel().getColumn(2).setPreferredWidth(50);  // Age
        patientTable.getColumnModel().getColumn(3).setPreferredWidth(70);  // Gender
        patientTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Phone
        patientTable.getColumnModel().getColumn(5).setPreferredWidth(100); // Blood Group
        patientTable.getColumnModel().getColumn(6).setPreferredWidth(200); // Address

        JScrollPane scrollPane = new JScrollPane(patientTable);
        add(scrollPane, BorderLayout.CENTER);

        // Add mouse listener for row selection
        patientTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // Double click
                    int selectedRow = patientTable.getSelectedRow();
                    if (selectedRow != -1) {
                        int patientId = (int) tableModel.getValueAt(selectedRow, 0);
                        if (formPanel != null) {
                            formPanel.loadPatient(patientId);
                        }
                    }
                }
            }
        });

        // Add key listener for delete
        patientTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    int selectedRow = patientTable.getSelectedRow();
                    if (selectedRow != -1) {
                        int patientId = (int) tableModel.getValueAt(selectedRow, 0);
                        deletePatient(patientId);
                    }
                }
            }
        });

        // Add search field key listener
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchPatients();
                }
            }
        });
    }

    public void setFormPanel(PatientFormPanel formPanel) {
        this.formPanel = formPanel;
    }

    public void refreshTable() {
        tableModel.setRowCount(0); // Clear table

        try {
            List<Patient> patients = patientDAO.getAllPatients();
            for (Patient p : patients) {
                Object[] rowData = {
                    p.getId(),
                    p.getName(),
                    p.getAge(),
                    p.getGender(),
                    p.getPhone(),
                    p.getBloodGroup(),
                    p.getAddress()
                };
                tableModel.addRow(rowData);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading patients: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void searchPatients() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            refreshTable();
            return;
        }

        tableModel.setRowCount(0); // Clear table

        try {
            List<Patient> patients = patientDAO.searchPatients(searchTerm);
            for (Patient p : patients) {
                Object[] rowData = {
                    p.getId(),
                    p.getName(),
                    p.getAge(),
                    p.getGender(),
                    p.getPhone(),
                    p.getBloodGroup(),
                    p.getAddress()
                };
                tableModel.addRow(rowData);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error searching patients: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deletePatient(int patientId) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this patient?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                patientDAO.deletePatient(patientId);
                JOptionPane.showMessageDialog(this, "Patient deleted successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
                if (formPanel != null) {
                    formPanel.clearForm();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error deleting patient: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

class StatusBar extends JPanel {

    private JLabel statusLabel;
    private JLabel timeLabel;

    public StatusBar() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());
        setPreferredSize(new Dimension(getWidth(), 25));

        statusLabel = new JLabel(" Ready");
        add(statusLabel, BorderLayout.WEST);

        timeLabel = new JLabel();
        add(timeLabel, BorderLayout.EAST);

        // Update time every second
        Timer timer = new Timer(1000, e -> updateTime());
        timer.start();
        updateTime();
    }

    public void setStatus(String status) {
        statusLabel.setText(" " + status);
    }

    private void updateTime() {
        timeLabel.setText(new java.util.Date().toString() + " ");
    }
}

// Main Application
public class HospitalManagementSystem extends JFrame {

    private PatientDAO patientDAO;
    private PatientFormPanel formPanel;
    private PatientTablePanel tablePanel;
    private StatusBar statusBar;

    public HospitalManagementSystem() {
        try {
            // Set look and feel to Nimbus for modern UI
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // fallback to system look and feel
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ex) {}
        }

        setTitle("Hospital Management System");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize database
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            DatabaseConfig.initializeDatabase();
            patientDAO = new PatientDAO();
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "Oracle JDBC Driver not found. Include it in your library path.",
                    "Driver Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Create menu bar
        setJMenuBar(createMenuBar());

        // Create main panel with split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(400);

        // Create form panel (left side)
        tablePanel = new PatientTablePanel(patientDAO);
        formPanel = new PatientFormPanel(patientDAO, tablePanel);
        tablePanel.setFormPanel(formPanel);

        splitPane.setLeftComponent(formPanel);
        splitPane.setRightComponent(tablePanel);

        add(splitPane, BorderLayout.CENTER);

        // Create status bar
        statusBar = new StatusBar();
        add(statusBar, BorderLayout.SOUTH);

        // Load initial data
        tablePanel.refreshTable();

        // Center the window
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem refreshItem = new JMenuItem("Refresh Data");
        refreshItem.setMnemonic(KeyEvent.VK_R);
        refreshItem.addActionListener(e -> {
            tablePanel.refreshTable();
            statusBar.setStatus("Data refreshed");
        });

        JMenuItem exportItem = new JMenuItem("Export to CSV");
        exportItem.setMnemonic(KeyEvent.VK_E);
        exportItem.addActionListener(e -> exportPatientsToCSV());
        fileMenu.add(exportItem);

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic(KeyEvent.VK_X);
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(refreshItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Patient menu
        JMenu patientMenu = new JMenu("Patient");
        patientMenu.setMnemonic(KeyEvent.VK_P);

        JMenuItem addItem = new JMenuItem("Add New Patient");
        addItem.setMnemonic(KeyEvent.VK_N);
        addItem.addActionListener(e -> {
            formPanel.clearForm();
            statusBar.setStatus("Ready to add new patient");
        });

        patientMenu.add(addItem);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.setMnemonic(KeyEvent.VK_A);
        aboutItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Hospital Management System\nVersion 1.0\n\n"
                    + "A simple application to manage patient records.",
                    "About", JOptionPane.INFORMATION_MESSAGE);
        });

        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(patientMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private void exportPatientsToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Patients to CSV");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".csv")) {
                filePath += ".csv";
            }
            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write("ID,Name,Age,Gender,Phone,Blood Group,Address\n");
                for (Patient p : patientDAO.getAllPatients()) {
                    writer.write(String.format("%d,%s,%d,%s,%s,%s,%s\n",
                        p.getId(),
                        escapeCsv(p.getName()),
                        p.getAge(),
                        escapeCsv(p.getGender()),
                        escapeCsv(p.getPhone()),
                        escapeCsv(p.getBloodGroup()),
                        escapeCsv(p.getAddress())
                    ));
                }
                JOptionPane.showMessageDialog(this, "Patients exported successfully!", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                statusBar.setStatus("Patients exported to CSV");
            } catch (IOException | SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error exporting patients: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = value.replace("\"", "\"\"");
            return '"' + value + '"';
        }
        return value;
    }

    public static void main(String[] args) {
        // Use invokeLater to ensure thread safety with Swing
        SwingUtilities.invokeLater(() -> {
            try {
                Class.forName("oracle.jdbc.driver.OracleDriver");
                new HospitalManagementSystem();
            } catch (ClassNotFoundException e) {
                JOptionPane.showMessageDialog(null,
                        "Oracle JDBC Driver not found. Include it in your library path.",
                        "Driver Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
