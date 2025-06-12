import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

public class studentManagement extends JFrame {
    private JTextField idField, nameField, courseField, searchField;
    private JButton addButton, updateButton, deleteButton, showButton, chartButton,
            saveButton, loadButton, sortButton, reportButton;
    private JTextArea outputArea;

    private final String URL = "jdbc:mysql://localhost:3306/student_db";
    private final String USER = "root";
    private final String PASSWORD = "root";

    public studentManagement() {
        setTitle("Student Management System");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(7, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Enter Student Details"));

        inputPanel.add(new JLabel("Student ID:"));
        idField = new JTextField();
        inputPanel.add(idField);

        inputPanel.add(new JLabel("Name:"));
        nameField = new JTextField();
        inputPanel.add(nameField);

        inputPanel.add(new JLabel("Course:"));
        courseField = new JTextField();
        inputPanel.add(courseField);

        addButton = new JButton("Add Student");
        inputPanel.add(addButton);

        updateButton = new JButton("Update Student");
        inputPanel.add(updateButton);

        deleteButton = new JButton("Delete by ID");
        inputPanel.add(deleteButton);

        saveButton = new JButton("Save Output to File");
        inputPanel.add(saveButton);

        loadButton = new JButton("Load Output from File");
        inputPanel.add(loadButton);

        add(inputPanel, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(new JLabel("Live Search:"), BorderLayout.WEST);
        searchField = new JTextField();
        searchPanel.add(searchField, BorderLayout.CENTER);
        add(searchPanel, BorderLayout.AFTER_LAST_LINE);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        showButton = new JButton("Show All Students");
        chartButton = new JButton("Show Course Chart");
        sortButton = new JButton("Sort by Name");
        reportButton = new JButton("Generate Report");

        bottomPanel.add(showButton);
        bottomPanel.add(sortButton);
        bottomPanel.add(reportButton);
        bottomPanel.add(chartButton);
        add(bottomPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addStudent());
        updateButton.addActionListener(e -> updateStudent());
        deleteButton.addActionListener(e -> deleteStudent());
        showButton.addActionListener(e -> showStudents(""));
        chartButton.addActionListener(e -> showChart());
        saveButton.addActionListener(e -> saveToFile());
        loadButton.addActionListener(e -> loadFromFile());
        sortButton.addActionListener(e -> sortStudents());
        reportButton.addActionListener(e -> generateReport());

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                showStudents(searchField.getText().trim());
            }
            public void removeUpdate(DocumentEvent e) {
                showStudents(searchField.getText().trim());
            }
            public void changedUpdate(DocumentEvent e) {
                showStudents(searchField.getText().trim());
            }
        });

        setVisible(true);
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private void addStudent() {
        String id = idField.getText().trim();
        String name = nameField.getText().trim();
        String course = courseField.getText().trim();

        if (id.isEmpty() || name.isEmpty() || course.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields.");
            return;
        }

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO students VALUES (?, ?, ?)");) {
            stmt.setString(1, id);
            stmt.setString(2, name);
            stmt.setString(3, course);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Student added to database.");
            clearFields();
            showStudents("");
        } catch (SQLIntegrityConstraintViolationException e) {
            JOptionPane.showMessageDialog(this, "Student ID already exists.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
        }
    }

    private void updateStudent() {
        String id = idField.getText().trim();
        String name = nameField.getText().trim();
        String course = courseField.getText().trim();

        if (id.isEmpty() || name.isEmpty() || course.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields to update.");
            return;
        }

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement("UPDATE students SET name = ?, course = ? WHERE id = ?")) {
            stmt.setString(1, name);
            stmt.setString(2, course);
            stmt.setString(3, id);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Student updated.");
            } else {
                JOptionPane.showMessageDialog(this, "Student not found.");
            }
            clearFields();
            showStudents("");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
        }
    }

    private void deleteStudent() {
        String id = idField.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter ID to delete student.");
            return;
        }

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM students WHERE id = ?")) {
            stmt.setString(1, id);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Student deleted.");
            } else {
                JOptionPane.showMessageDialog(this, "Student not found.");
            }
            clearFields();
            showStudents("");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
        }
    }

    private void showStudents(String search) {
        outputArea.setText("=== Student List ===\n");
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM students WHERE name LIKE ?")) {
            stmt.setString(1, "%" + search + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                outputArea.append("ID: " + rs.getString("id") +
                        ", Name: " + rs.getString("name") +
                        ", Course: " + rs.getString("course") + "\n");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
        }
    }

    private void sortStudents() {
        outputArea.setText("=== Sorted Student List by Name ===\n");
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM students ORDER BY name ASC")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                outputArea.append("ID: " + rs.getString("id") +
                        ", Name: " + rs.getString("name") +
                        ", Course: " + rs.getString("course") + "\n");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
        }
    }

    private void generateReport() {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT course, COUNT(*) as count FROM students GROUP BY course")) {
            outputArea.setText("=== Student Course Report ===\n");
            while (rs.next()) {
                String course = rs.getString("course");
                int count = rs.getInt("count");
                outputArea.append("Course: " + course + ", Students: " + count + "\n");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Report generation error: " + e.getMessage());
        }
    }

    private void showChart() {
        HashMap<String, Integer> courseCount = new HashMap<>();

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT course, COUNT(*) as count FROM students GROUP BY course")) {

            while (rs.next()) {
                courseCount.put(rs.getString("course"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error generating chart: " + e.getMessage());
            return;
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (String course : courseCount.keySet()) {
            dataset.addValue(courseCount.get(course), "Students", course);
        }

        JFreeChart chart = ChartFactory.createBarChart("Students Per Course", "Course", "No. of Students", dataset);
        ChartPanel chartPanel = new ChartPanel(chart);
        JFrame chartFrame = new JFrame("Course Chart");
        chartFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        chartFrame.setContentPane(chartPanel);
        chartFrame.setSize(600, 400);
        chartFrame.setVisible(true);
    }

    private void saveToFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileChooser.getSelectedFile()))) {
                writer.write(outputArea.getText());
                JOptionPane.showMessageDialog(this, "Output saved successfully.");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "File error: " + e.getMessage());
            }
        }
    }

    private void loadFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (BufferedReader reader = new BufferedReader(new FileReader(fileChooser.getSelectedFile()))) {
                outputArea.read(reader, null);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "File error: " + e.getMessage());
            }
        }
    }

    private void clearFields() {
        idField.setText("");
        nameField.setText("");
        courseField.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(studentManagement::new);
    }
}
