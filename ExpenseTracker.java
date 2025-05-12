import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ExpenseTracker extends JFrame {

    public static class DatabaseHelper {
        private static final String URL = "jdbc:mysql://localhost:3306/ExpenseTracker";
        private static final String USER = "as2469"; 
        private static final String PASSWORD = "Iambatman123@"; 
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        public static Connection getConnection() throws SQLException {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL JDBC Driver not found", e);
            }
            return DriverManager.getConnection(URL, USER, PASSWORD);
        }

        public static void addExpense(LocalDate date, String category, String description, double amount) throws SQLException {
            String query = "INSERT INTO expenses (date, category, description, amount) VALUES (?, ?, ?, ?)";
            try (Connection conn = getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setDate(1, Date.valueOf(date));
                stmt.setString(2, category);
                stmt.setString(3, description);
                stmt.setDouble(4, amount);
                stmt.executeUpdate();
            }
        }

        public static ResultSet getExpenses() throws SQLException {
            String query = "SELECT id, date, category, description, amount FROM expenses ORDER BY date DESC";
            Connection conn = getConnection();
            Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            return stmt.executeQuery(query);
        }

        public static void deleteExpense(int id) throws SQLException {
            String query = "DELETE FROM expenses WHERE id = ?";
            try (Connection conn = getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }
        }
        
        public static double getTotalSpentByCategory(String category) throws SQLException {
            String query = "SELECT COALESCE(SUM(amount), 0) AS total FROM expenses WHERE category = ?";
            try (Connection conn = getConnection(); 
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, category);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getDouble("total") : 0.0;
                }
            }
        }
        
        public static Map<String, Double> getCategoryTotals() throws SQLException {
            Map<String, Double> totals = new HashMap<>();
            String query = "SELECT category, SUM(amount) AS total FROM expenses GROUP BY category";
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    totals.put(rs.getString("category"), rs.getDouble("total"));
                }
            }
            return totals;
        }
        
        public static Map<String, Double> getMonthlyTotals() throws SQLException {
            Map<String, Double> totals = new HashMap<>();
            String query = "SELECT DATE_FORMAT(date, '%Y-%m') AS month, SUM(amount) AS total " +
                          "FROM expenses GROUP BY DATE_FORMAT(date, '%Y-%m') ORDER BY month";
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    totals.put(rs.getString("month"), rs.getDouble("total"));
                }
            }
            return totals;
        }
        
        public static Map<String, Map<String, Double>> getMonthlyCategoryTotals() throws SQLException {
            Map<String, Map<String, Double>> result = new HashMap<>();
            String query = "SELECT DATE_FORMAT(date, '%Y-%m') AS month, category, SUM(amount) AS total " +
                          "FROM expenses GROUP BY DATE_FORMAT(date, '%Y-%m'), category ORDER BY month, category";
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    String month = rs.getString("month");
                    String category = rs.getString("category");
                    double total = rs.getDouble("total");
                    
                    result.computeIfAbsent(month, k -> new HashMap<>())
                          .put(category, total);
                }
            }
            return result;
        }
    }

    private JTextField txtDate, txtCategory, txtDescription, txtAmount, txtCategoryLimit;
    private DefaultTableModel tableModel;
    private JTable table;
    private JPanel chartPanel;
    private Map<String, Double> categoryLimits;
    private JComboBox<String> categoryComboBox;
    private static final String[] DEFAULT_CATEGORIES = {
        "Food", "Transportation", "Housing", "Entertainment", 
        "Utilities", "Healthcare", "Education", "Shopping", "Other"
    };

    public ExpenseTracker() {
        categoryLimits = new HashMap<>();
        setTitle("Budget Expense Tracker");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        initializeUI();
        loadExpenses();
        updateCharts();
    }

    private void initializeUI() {
        // Input Panel
        JPanel panelInput = new JPanel(new GridLayout(7, 2, 10, 10));
        panelInput.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panelInput.add(new JLabel("Date (YYYY-MM-DD):"));
        txtDate = new JTextField(LocalDate.now().toString());
        panelInput.add(txtDate);

        panelInput.add(new JLabel("Category:"));
        categoryComboBox = new JComboBox<>(DEFAULT_CATEGORIES);
        categoryComboBox.setEditable(true);
        panelInput.add(categoryComboBox);

        panelInput.add(new JLabel("Description:"));
        txtDescription = new JTextField();
        panelInput.add(txtDescription);

        panelInput.add(new JLabel("Amount:"));
        txtAmount = new JTextField();
        panelInput.add(txtAmount);

        panelInput.add(new JLabel("Category Limit (Optional):"));
        txtCategoryLimit = new JTextField();
        panelInput.add(txtCategoryLimit);

        JButton btnAdd = new JButton("Add Expense");
        btnAdd.addActionListener(this::addExpense);
        panelInput.add(btnAdd);

        JButton btnDelete = new JButton("Delete Expense");
        btnDelete.addActionListener(this::deleteExpense);
        panelInput.add(btnAdd);

        JButton btnRefresh = new JButton("Refresh Data");
        btnRefresh.addActionListener(e -> {
            loadExpenses();
            updateCharts();
        });
        panelInput.add(btnRefresh);

        // Table setup
        tableModel = new DefaultTableModel(
            new Object[]{"ID", "Date", "Category", "Description", "Amount"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 4 ? Double.class : String.class;
            }
        };
        table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(500, 400));

        // Main panel layout
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.add(panelInput, BorderLayout.NORTH);
        leftPanel.add(scrollPane, BorderLayout.CENTER);

        // Chart panel
        chartPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        chartPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add components to frame
        add(leftPanel, BorderLayout.WEST);
        add(chartPanel, BorderLayout.CENTER);
    }

    private void addExpense(ActionEvent e) {
        try {
            LocalDate date = LocalDate.parse(txtDate.getText());
            String category = ((String) categoryComboBox.getSelectedItem()).trim();
            String description = txtDescription.getText().trim();
            double amount = Double.parseDouble(txtAmount.getText());
            String limitText = txtCategoryLimit.getText().trim();

            if (category.isEmpty()) {
                throw new IllegalArgumentException("Category cannot be empty");
            }
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be positive");
            }

            DatabaseHelper.addExpense(date, category, description, amount);

            // Handle category limit if provided
            if (!limitText.isEmpty()) {
                double limit = Double.parseDouble(limitText);
                if (limit <= 0) {
                    throw new IllegalArgumentException("Limit must be positive");
                }
                categoryLimits.put(category, limit);
                
                // Check if limit is exceeded
                double totalSpent = DatabaseHelper.getTotalSpentByCategory(category);
                if (totalSpent > limit) {
                    JOptionPane.showMessageDialog(this, 
                        String.format("Warning: You have exceeded your spending limit for %s! (Limit: %.2f, Spent: %.2f)",
                            category, limit, totalSpent),
                        "Budget Warning", JOptionPane.WARNING_MESSAGE);
                }
            }

            clearInputFields();
            loadExpenses();
            updateCharts();
            JOptionPane.showMessageDialog(this, "Expense added successfully!");
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Please use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for amount and limit.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadExpenses() {
        tableModel.setRowCount(0);
        try (ResultSet rs = DatabaseHelper.getExpenses()) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getDate("date").toLocalDate().toString(),
                    rs.getString("category"),
                    rs.getString("description"),
                    rs.getDouble("amount")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load expenses: " + e.getMessage());
        }
    }

    private void deleteExpense(ActionEvent e) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an expense to delete.");
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        try {
            int confirm = JOptionPane.showConfirmDialog(
                this, 
                "Are you sure you want to delete this expense?", 
                "Confirm Deletion", 
                JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                DatabaseHelper.deleteExpense(id);
                loadExpenses();
                updateCharts();
                JOptionPane.showMessageDialog(this, "Expense deleted successfully!");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Failed to delete expense: " + ex.getMessage());
        }
    }

    private void updateCharts() {
        chartPanel.removeAll();
        
        try {
            // Pie Chart
            DefaultPieDataset pieDataset = new DefaultPieDataset();
            Map<String, Double> categoryTotals = DatabaseHelper.getCategoryTotals();
            categoryTotals.forEach(pieDataset::setValue);
            
            JFreeChart pieChart = ChartFactory.createPieChart(
                "Expenses by Category", pieDataset, true, true, false);
            chartPanel.add(new ChartPanel(pieChart));
            
            // Monthly Bar Chart
            DefaultCategoryDataset monthlyDataset = new DefaultCategoryDataset();
            DatabaseHelper.getMonthlyTotals().forEach((month, total) -> {
                monthlyDataset.addValue(total, "Amount", month);
            });
            
            JFreeChart monthlyChart = ChartFactory.createBarChart(
                "Monthly Expenses", "Month", "Amount", monthlyDataset, 
                PlotOrientation.VERTICAL, false, true, false);
            monthlyChart.getCategoryPlot().setRangeGridlinePaint(Color.BLACK);
            chartPanel.add(new ChartPanel(monthlyChart));
            
            // Monthly Category-wise Bar Chart
            DefaultCategoryDataset categoryMonthlyDataset = new DefaultCategoryDataset();
            DatabaseHelper.getMonthlyCategoryTotals().forEach((month, categories) -> {
                categories.forEach((category, total) -> {
                    categoryMonthlyDataset.addValue(total, category, month);
                });
            });
            
            JFreeChart categoryMonthlyChart = ChartFactory.createBarChart(
                "Monthly Category-wise Expenses", "Month", "Amount", 
                categoryMonthlyDataset, PlotOrientation.VERTICAL, true, true, false);
            categoryMonthlyChart.getCategoryPlot().setRangeGridlinePaint(Color.BLACK);
            chartPanel.add(new ChartPanel(categoryMonthlyChart));
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Failed to load chart data: " + ex.getMessage());
        }
        
        chartPanel.revalidate();
        chartPanel.repaint();
    }

    private void clearInputFields() {
        txtDate.setText(LocalDate.now().toString());
        categoryComboBox.setSelectedIndex(0);
        txtDescription.setText("");
        txtAmount.setText("");
        txtCategoryLimit.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ExpenseTracker tracker = new ExpenseTracker();
            tracker.setVisible(true);
            
            // Test database connection
            try {
                DatabaseHelper.getConnection().close();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(tracker, 
                    "Failed to connect to database: " + e.getMessage(), 
                    "Database Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}