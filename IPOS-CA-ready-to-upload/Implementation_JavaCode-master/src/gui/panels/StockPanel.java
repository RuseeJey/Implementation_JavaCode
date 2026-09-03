package gui.panels;

import IPOS_CA_STOCK.LocalStockItem;
import database.LocalStockItemDB;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.UUID;

public class StockPanel extends JPanel {

    private JTable stockTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    private JTextField searchField;
    private JTextField itemNameField;
    private JTextField quantityField;
    private JTextField thresholdField;
    private JTextField wholesaleCostField;
    private JTextField markupRateField;
    private JTextField vatRateField;
    private LocalStockItemDB stockDAO;

    public StockPanel() {
        stockDAO = new LocalStockItemDB();
        setLayout(new BorderLayout(10, 10));

        // ── Search bar ────────────────────────────────────────────────────────
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
        searchPanel.add(new JLabel("Search:"));
        searchField = new JTextField(22);
        searchPanel.add(searchField);
        JButton searchBtn = new JButton("Search");
        JButton clearBtn  = new JButton("Clear");
        searchPanel.add(searchBtn);
        searchPanel.add(clearBtn);

        searchBtn.addActionListener(e -> applySearch());
        clearBtn.addActionListener(e -> clearSearch());
        searchField.addActionListener(e -> applySearch());

        // ── Input form ────────────────────────────────────────────────────────
        JPanel inputPanel = new JPanel(new GridLayout(3, 6, 10, 10));

        inputPanel.add(new JLabel("Item Name:"));
        itemNameField = new JTextField();
        inputPanel.add(itemNameField);

        inputPanel.add(new JLabel("Quantity:"));
        quantityField = new JTextField();
        inputPanel.add(quantityField);

        inputPanel.add(new JLabel("Low Stock Threshold:"));
        thresholdField = new JTextField();
        inputPanel.add(thresholdField);

        inputPanel.add(new JLabel("Retail Cost (£):"));
        wholesaleCostField = new JTextField();
        inputPanel.add(wholesaleCostField);

        inputPanel.add(new JLabel("Markup Rate (%):"));
        markupRateField = new JTextField();
        inputPanel.add(markupRateField);

        inputPanel.add(new JLabel("VAT Rate (%):"));
        vatRateField = new JTextField();
        inputPanel.add(vatRateField);

        JButton addButton             = new JButton("Add Stock Item");
        JButton updateButton          = new JButton("Update Quantity");
        JButton updateThresholdButton = new JButton("Update Low Stock Threshold");
        JButton updatePriceButton     = new JButton("Update Price");
        JButton deleteButton          = new JButton("Delete Stock");
        JButton refreshButton         = new JButton("Refresh Stock");

        inputPanel.add(addButton);
        inputPanel.add(updateButton);
        inputPanel.add(updateThresholdButton);
        inputPanel.add(updatePriceButton);
        inputPanel.add(deleteButton);
        inputPanel.add(refreshButton);

        // Stack search bar above the input form
        JPanel northPanel = new JPanel(new BorderLayout(0, 4));
        northPanel.add(searchPanel, BorderLayout.NORTH);
        northPanel.add(inputPanel, BorderLayout.CENTER);
        add(northPanel, BorderLayout.NORTH);

        // ── Table ─────────────────────────────────────────────────────────────
        String[] columns = {"Item ID", "Item Name", "Qty", "Min Level", "Retail Price (£)", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        stockTable = new JTable(tableModel);
        stockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Attach sorter for search filtering
        sorter = new TableRowSorter<>(tableModel);
        stockTable.setRowSorter(sorter);

        add(new JScrollPane(stockTable), BorderLayout.CENTER);

        // ── Listeners ─────────────────────────────────────────────────────────
        addButton.addActionListener(e -> addStockItem());
        updateButton.addActionListener(e -> updateQuantity());
        updateThresholdButton.addActionListener(e -> updateLowStockThreshold());
        updatePriceButton.addActionListener(e -> updatePrice());
        deleteButton.addActionListener(e -> deleteStockItem());
        refreshButton.addActionListener(e -> {
            clearSearch();
            loadStockData();
        });

        loadStockData();
    }

    // ── Load table ────────────────────────────────────────────────────────────

    private void loadStockData() {
        tableModel.setRowCount(0);
        for (LocalStockItem item : stockDAO.getAllItems()) {
            tableModel.addRow(new Object[]{
                    item.getItemID(),
                    item.getItemName(),
                    item.getQuantity(),
                    item.getMinimumStockLevel(),
                    String.format("%.2f", item.calculateRetailPrice()),
                    item.isLowStock() ? "LOW STOCK" : "OK"
            });
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private void applySearch() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }
        // Search across Item ID (col 0) and Item Name (col 1), case-insensitive
        try {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0, 1));
        } catch (java.util.regex.PatternSyntaxException ex) {
            // If user typed a special regex character, escape it and retry
            sorter.setRowFilter(RowFilter.regexFilter(
                    "(?i)" + java.util.regex.Pattern.quote(text), 0, 1));
        }
    }

    private void clearSearch() {
        searchField.setText("");
        sorter.setRowFilter(null);
    }

    // ── Add stock item ────────────────────────────────────────────────────────

    private void addStockItem() {
        String itemName   = itemNameField.getText().trim();
        String qtyText    = quantityField.getText().trim();
        String threshText = thresholdField.getText().trim();

        if (qtyText.isEmpty() || threshText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in quantity and threshold.");
            return;
        }
        try {
            int qty       = Integer.parseInt(qtyText);
            int threshold = Integer.parseInt(threshText);
            String itemId = UUID.randomUUID().toString();

            LocalStockItem item = new LocalStockItem(
                    itemId,
                    itemName.isEmpty() ? itemId : itemName,
                    "",
                    "",
                    parseDouble(wholesaleCostField.getText().trim()),
                    qty,
                    threshold
            );

            if (stockDAO.addItem(item)) {
                JOptionPane.showMessageDialog(this, "Stock item added successfully.");
                clearFields();
                loadStockData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add stock item.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantity and threshold must be numbers.");
        }
    }

    // ── Delete stock item ─────────────────────────────────────────────────────

    private void deleteStockItem() {
        int row = stockTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a stock item first.");
            return;
        }
        // Convert view index to model index (important when sorter is active)
        int modelRow = stockTable.convertRowIndexToModel(row);
        String itemId = tableModel.getValueAt(modelRow, 0).toString();

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete stock item " + itemId + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        if (stockDAO.deleteItem(itemId)) {
            JOptionPane.showMessageDialog(this, "Stock item deleted.");
            loadStockData();
        } else {
            JOptionPane.showMessageDialog(this, "Delete failed.");
        }
    }

    // ── Update quantity ───────────────────────────────────────────────────────

    private void updateQuantity() {
        int row = stockTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a stock item first.");
            return;
        }
        int modelRow = stockTable.convertRowIndexToModel(row);
        String itemId = tableModel.getValueAt(modelRow, 0).toString();
        String input  = JOptionPane.showInputDialog(this, "Enter new quantity:");
        if (input == null || input.trim().isEmpty()) return;
        try {
            int qty = Integer.parseInt(input.trim());
            if (stockDAO.updateQuantity(itemId, qty)) {
                JOptionPane.showMessageDialog(this, "Quantity updated.");
                loadStockData();
            } else {
                JOptionPane.showMessageDialog(this, "Update failed.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid number.");
        }
    }

    // ── Update low stock threshold ────────────────────────────────────────────

    private void updateLowStockThreshold() {
        int row = stockTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a stock item first.");
            return;
        }
        int modelRow = stockTable.convertRowIndexToModel(row);
        String itemId  = tableModel.getValueAt(modelRow, 0).toString();
        String current = tableModel.getValueAt(modelRow, 3).toString();
        String input   = JOptionPane.showInputDialog(this,
                "Enter new low stock threshold:", current);
        if (input == null || input.trim().isEmpty()) return;
        try {
            int threshold = Integer.parseInt(input.trim());
            if (threshold < 0) {
                JOptionPane.showMessageDialog(this, "Threshold cannot be negative.");
                return;
            }
            if (stockDAO.updateMinimumStockLevel(itemId, threshold)) {
                JOptionPane.showMessageDialog(this, "Threshold updated.");
                loadStockData();
            } else {
                JOptionPane.showMessageDialog(this, "Update failed.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid number.");
        }
    }

    // ── Update price ──────────────────────────────────────────────────────────

    private void updatePrice() {
        int row = stockTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a stock item first.");
            return;
        }
        int modelRow = stockTable.convertRowIndexToModel(row);
        String itemId = tableModel.getValueAt(modelRow, 0).toString();
        LocalStockItem item = stockDAO.getItemByID(itemId);
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Stock item not found.");
            return;
        }

        wholesaleCostField.setText(String.valueOf(item.getWholesaleCost()));
        markupRateField.setText(String.valueOf(item.getMarkupRate()));
        vatRateField.setText(String.valueOf(item.getVatRate()));

        int option = JOptionPane.showConfirmDialog(this,
                createPricePanel(),
                "Update Price for " + item.getItemName(),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (option != JOptionPane.OK_OPTION) return;

        try {
            double wholesaleCost = parseDouble(wholesaleCostField.getText().trim());
            double markupRate    = parseDouble(markupRateField.getText().trim());
            double vatRate       = parseDouble(vatRateField.getText().trim());

            if (wholesaleCost < 0 || markupRate < 0 || vatRate < 0) {
                JOptionPane.showMessageDialog(this, "Price values cannot be negative.");
                return;
            }

            item.setWholesaleCost(wholesaleCost);
            item.setMarkupRate(markupRate);
            item.setVatRate(vatRate);

            if (stockDAO.updateItem(item)) {
                JOptionPane.showMessageDialog(this, "Price updated successfully.");
                clearFields();
                loadStockData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update price.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for all price fields.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JPanel createPricePanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.add(new JLabel("Wholesale Cost (£):"));
        panel.add(wholesaleCostField);
        panel.add(new JLabel("Markup Rate (%):"));
        panel.add(markupRateField);
        panel.add(new JLabel("VAT Rate (%):"));
        panel.add(vatRateField);
        return panel;
    }

    private void clearFields() {
        itemNameField.setText("");
        quantityField.setText("");
        thresholdField.setText("");
        wholesaleCostField.setText("");
        markupRateField.setText("");
        vatRateField.setText("");
    }

    private double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }
}