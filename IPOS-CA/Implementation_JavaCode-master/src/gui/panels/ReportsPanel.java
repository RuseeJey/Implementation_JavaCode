package gui.panels;

import database.ReportsDB;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReportsPanel extends JPanel {

    private final JComboBox<String> reportTypeCombo;
    private final JTextField startDateField;
    private final JTextField endDateField;
    private final JTable reportTable;
    private final DefaultTableModel tableModel;
    private final JTextArea summaryArea;
    private final ReportsDB reportsDB;

    private static final DateTimeFormatter INPUT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DB_DATE    = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReportsPanel() {
        reportsDB = new ReportsDB();
        setLayout(new BorderLayout(10, 10));

        // ── Top controls ──────────────────────────────────────────────────────
        JPanel topPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));

        topPanel.add(new JLabel("Report Type:"));
        reportTypeCombo = new JComboBox<>(new String[]{
                "Turnover Report",
                "Stock Report",
                "Debt Analysis Report"
        });
        topPanel.add(reportTypeCombo);

        topPanel.add(new JLabel("Start Date (dd/mm/yyyy):"));
        startDateField = new JTextField();
        topPanel.add(startDateField);

        topPanel.add(new JLabel("End Date (dd/mm/yyyy):"));
        endDateField = new JTextField();
        topPanel.add(endDateField);

        add(topPanel, BorderLayout.NORTH);

        // ── Button bar ────────────────────────────────────────────────────────
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        JButton generateButton = new JButton("Generate Report");
        JButton printButton    = new JButton("Print Report");
        JButton clearButton    = new JButton("Clear");

        buttonPanel.add(generateButton);
        buttonPanel.add(printButton);
        buttonPanel.add(clearButton);

        // ── Table ─────────────────────────────────────────────────────────────
        tableModel = new DefaultTableModel() {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        reportTable = new JTable(tableModel);
        reportTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        summaryArea = new JTextArea(6, 30);
        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(reportTable),
                new JScrollPane(summaryArea)
        );
        splitPane.setDividerLocation(300);

        // Stack button bar above split pane
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(buttonPanel, BorderLayout.NORTH);
        centerPanel.add(splitPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // ── Listeners ─────────────────────────────────────────────────────────
        generateButton.addActionListener(e -> generateReport());
        printButton.addActionListener(e -> printReport());
        clearButton.addActionListener(e -> clearReport());
    }

    // ── Generate ──────────────────────────────────────────────────────────────

    private void generateReport() {
        String reportType = (String) reportTypeCombo.getSelectedItem();
        String startInput = startDateField.getText().trim();
        String endInput   = endDateField.getText().trim();

        if (startInput.isEmpty() || endInput.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both start and end dates.");
            return;
        }

        String dbStart, dbEnd;
        try {
            dbStart = LocalDate.parse(startInput, INPUT_DATE).format(DB_DATE);
            dbEnd   = LocalDate.parse(endInput,   INPUT_DATE).format(DB_DATE);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Please enter dates in dd/MM/yyyy format.");
            return;
        }

        switch (reportType) {
            case "Turnover Report"      -> populateTable(reportsDB.getTurnoverReport(dbStart, dbEnd));
            case "Stock Report"         -> populateTable(reportsDB.getStockReport(dbStart, dbEnd));
            case "Debt Analysis Report" -> populateTable(reportsDB.getDebtAnalysisReport(dbStart, dbEnd));
            default -> JOptionPane.showMessageDialog(this, "Unknown report type.");
        }
    }

    private void populateTable(ReportsDB.ReportResult result) {
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        tableModel.setColumnIdentifiers(result.getColumns());
        for (Object[] row : result.getRows()) {
            tableModel.addRow(row);
        }
        summaryArea.setText(result.getSummary());
        summaryArea.setCaretPosition(0);
    }

    // ── Print ─────────────────────────────────────────────────────────────────

    private void printReport() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Please generate a report first.");
            return;
        }

        StringBuilder sb = new StringBuilder();

        // Summary block
        sb.append(summaryArea.getText());
        sb.append("\n\n");

        // Column headers
        int colCount = tableModel.getColumnCount();
        int colWidth = 28;
        for (int j = 0; j < colCount; j++) {
            sb.append(String.format("%-" + colWidth + "s", tableModel.getColumnName(j)));
        }
        sb.append("\n").append("─".repeat(colWidth * colCount)).append("\n");

        // Data rows
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            for (int j = 0; j < colCount; j++) {
                Object val = tableModel.getValueAt(i, j);
                sb.append(String.format("%-" + colWidth + "s", val != null ? val.toString() : ""));
            }
            sb.append("\n");
        }

        sb.append("\n").append("─".repeat(colWidth * colCount)).append("\n");
        sb.append("Generated: ").append(LocalDate.now()).append("\n");
        sb.append("Generated by: Cosymed Ltd — IPOS-CA\n");

        // Print preview dialog
        JTextArea printArea = new JTextArea(sb.toString());
        printArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        printArea.setEditable(false);

        JScrollPane scroll = new JScrollPane(printArea);
        scroll.setPreferredSize(new Dimension(750, 520));

        int choice = JOptionPane.showOptionDialog(
                this,
                scroll,
                "Print Preview — " + reportTypeCombo.getSelectedItem(),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                new String[]{"Print", "Close"},
                "Print"
        );

        if (choice == 0) {
            try {
                printArea.print();
                JOptionPane.showMessageDialog(this, "Report sent to printer successfully.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Print failed: " + ex.getMessage());
            }
        }
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    private void clearReport() {
        startDateField.setText("");
        endDateField.setText("");
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        summaryArea.setText("");
        reportTypeCombo.setSelectedIndex(0);
    }
}