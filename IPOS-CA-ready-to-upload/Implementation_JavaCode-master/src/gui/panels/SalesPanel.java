package gui.panels;

import IPOS_CA_CUST.CustomerAccount;
import IPOS_CA_STOCK.LocalStockItem;
import database.CustomerAccountDB;
import database.DatabaseManager;
import database.LocalStockItemDB;
import database.OrdersDB;
import database.SalesDB;
import database.TemplatesDB;
import gui.util.AutoSuggestSupport;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

public class SalesPanel extends JPanel {

    private final JComboBox<String> customerTypeCombo;
    private final AutoSuggestSupport.TextFieldSuggestion customerNameField;
    private final JTextField stockSearchField;
    private final JTextField saleHistorySearchField;
    private final JTextField quantityField;

    private final JTable stockTable, saleTable, saleHistoryTable, puOrderTable;
    private final DefaultTableModel stockModel, saleModel, saleHistoryModel, puOrderModel;
    private final TableRowSorter<DefaultTableModel> stockSorter;
    private final TableRowSorter<DefaultTableModel> saleHistorySorter;
    private final JLabel totalLabel;

    private final SalesDB salesDB;
    private final OrdersDB ordersDB;
    private final CustomerAccountDB customerAccountDB;
    private final LocalStockItemDB localStockItemDB;
    private final Map<String, LocalStockItem> inStockItemsById = new HashMap<>();
    private boolean loadingPUOrders = false;
    private String lastTransactionID = null;

    private static final String[] PU_STATUSES = {
            "accepted", "ready for shipment", "shipped", "delivered"
    };

    public SalesPanel() {
        salesDB           = new SalesDB();
        ordersDB          = new OrdersDB();
        customerAccountDB = new CustomerAccountDB();
        localStockItemDB  = new LocalStockItemDB();

        setLayout(new BorderLayout(10, 10));

        JPanel stockSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        stockSearchPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
        stockSearchPanel.add(new JLabel("Search:"));
        stockSearchField = new JTextField(22);
        stockSearchPanel.add(stockSearchField);
        JButton stockSearchBtn = new JButton("Search");
        JButton stockClearBtn = new JButton("Clear");
        stockSearchPanel.add(stockSearchBtn);
        stockSearchPanel.add(stockClearBtn);

        // ── Input bar ─────────────────────────────────────────────────────────
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        inputPanel.add(new JLabel("Customer Type:"));
        customerTypeCombo = new JComboBox<>(new String[]{"Occasional Customer", "Account Holder"});
        inputPanel.add(customerTypeCombo);

        inputPanel.add(new JLabel("Customer Name:"));
        customerNameField = new AutoSuggestSupport.TextFieldSuggestion();
        customerNameField.setColumns(16);
        customerNameField.setEditable(false);
        customerNameField.setEnabled(false);
        inputPanel.add(customerNameField);

        inputPanel.add(new JLabel("Quantity:"));
        quantityField = new JTextField(8);
        inputPanel.add(quantityField);

        JButton addBtn       = new JButton("Add to Sale");
        JButton clearBtn     = new JButton("Clear");
        JButton loadStockBtn = new JButton("Reload Stock");
        inputPanel.add(addBtn);
        inputPanel.add(clearBtn);
        inputPanel.add(loadStockBtn);

        // ── Tables ────────────────────────────────────────────────────────────
        stockModel = new DefaultTableModel(
                new String[]{"Item ID", "Item Name", "Retail Price (£)", "Available Qty"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        saleModel = new DefaultTableModel(
                new String[]{"Item ID", "Item Name", "Unit Price (£)", "Quantity", "Line Total (£)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        saleHistoryModel = new DefaultTableModel(
                new String[]{"Transaction ID", "Sale Date", "Customer Type", "Total (£)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        puOrderModel = new DefaultTableModel(
                new String[]{"Adj ID", "Order ID", "Merchant", "Item ID",
                        "Qty", "Delivery Address", "Status",
                        "Created At", "Processed At", "Error"}, 0) {
            public boolean isCellEditable(int r, int c) {
                if (c != 6) return false;
                Object v = getValueAt(r, 6);
                return v != null && "PENDING".equalsIgnoreCase(v.toString());
            }
        };

        stockTable       = new JTable(stockModel);
        saleTable        = new JTable(saleModel);
        saleHistoryTable = new JTable(saleHistoryModel);
        puOrderTable     = new JTable(puOrderModel);
        stockSorter = new TableRowSorter<>(stockModel);
        stockTable.setRowSorter(stockSorter);
        saleHistorySorter = new TableRowSorter<>(saleHistoryModel);
        saleHistoryTable.setRowSorter(saleHistorySorter);

        JTabbedPane tabs = new JTabbedPane();

        // Sale tab
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(stockTable), new JScrollPane(saleTable));
        split.setDividerLocation(180);
        JPanel salePanel = new JPanel(new BorderLayout(10, 10));
        JPanel saleNorth = new JPanel(new BorderLayout(0, 4));
        saleNorth.add(stockSearchPanel, BorderLayout.NORTH);
        saleNorth.add(inputPanel, BorderLayout.CENTER);
        salePanel.add(saleNorth, BorderLayout.NORTH);
        salePanel.add(split, BorderLayout.CENTER);
        tabs.addTab("Sale", salePanel);

        // Sale History tab
        JPanel historyPanel = new JPanel(new BorderLayout(10, 10));
        JPanel historySearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        historySearchPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
        historySearchPanel.add(new JLabel("Search:"));
        saleHistorySearchField = new JTextField(22);
        historySearchPanel.add(saleHistorySearchField);
        JButton historySearchBtn = new JButton("Search");
        JButton historyClearBtn = new JButton("Clear");
        historySearchPanel.add(historySearchBtn);
        historySearchPanel.add(historyClearBtn);

        JPanel historyTop   = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton refreshHistBtn = new JButton("Refresh History");
        JButton viewDetailsBtn = new JButton("View Details");
        historyTop.add(refreshHistBtn);
        historyTop.add(viewDetailsBtn);

        JPanel historyNorth = new JPanel(new BorderLayout(0, 4));
        historyNorth.add(historySearchPanel, BorderLayout.NORTH);
        historyNorth.add(historyTop, BorderLayout.CENTER);
        historyPanel.add(historyNorth, BorderLayout.NORTH);
        historyPanel.add(new JScrollPane(saleHistoryTable), BorderLayout.CENTER);
        tabs.addTab("Sale History", historyPanel);

        // PU Orders tab
        JPanel puPanel = new JPanel(new BorderLayout(10, 10));
        JPanel puTop   = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton refreshPUBtn = new JButton("Refresh PU Orders");
        JLabel puInfo = new JLabel("Inbound orders from the IPOS-PU portal");
        puInfo.setFont(new Font("Arial", Font.ITALIC, 12));
        puTop.add(refreshPUBtn);
        puTop.add(puInfo);

        JComboBox<String> puStatusCombo = new JComboBox<>(
                new String[]{"PENDING","APPLIED","FAILED","CANCELLED"});
        puOrderTable.getColumnModel().getColumn(6).setCellEditor(
                new DefaultCellEditor(puStatusCombo));
        puOrderModel.addTableModelListener(e -> {
            if (loadingPUOrders
                    || e.getType() != javax.swing.event.TableModelEvent.UPDATE
                    || e.getColumn() != 6) return;
            int row = e.getFirstRow();
            String adjustmentId = puOrderModel.getValueAt(row, 0).toString();
            String newStatus    = puOrderModel.getValueAt(row, 6).toString();
            if (!ordersDB.updatePUOrderStatus(adjustmentId, newStatus)) {
                JOptionPane.showMessageDialog(this, "Failed to update PU order status.");
                loadPUOrders();
            }
        });

        puPanel.add(puTop, BorderLayout.NORTH);
        puPanel.add(new JScrollPane(puOrderTable), BorderLayout.CENTER);
        refreshPUBtn.addActionListener(e -> loadPUOrders());
        tabs.addTab("PU Orders", puPanel);

        add(tabs, BorderLayout.CENTER);

        // ── Bottom bar ────────────────────────────────────────────────────────
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        JButton removeBtn       = new JButton("Remove Selected");
        JButton processBtn      = new JButton("Process Payment");
        JButton printReceiptBtn = new JButton("Print Receipt");
        totalLabel = new JLabel("Total: £0.00");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 13));

        bottomPanel.add(removeBtn);
        bottomPanel.add(processBtn);
        bottomPanel.add(printReceiptBtn);
        bottomPanel.add(totalLabel);
        add(bottomPanel, BorderLayout.SOUTH);

        // ── Listeners ─────────────────────────────────────────────────────────
        addBtn.addActionListener(e -> addSelectedToSale());
        clearBtn.addActionListener(e -> clearFields());
        loadStockBtn.addActionListener(e -> loadInStockItems());
        removeBtn.addActionListener(e -> removeSelectedItem());
        processBtn.addActionListener(e -> processPayment());
        printReceiptBtn.addActionListener(e -> printSelectedReceipt());
        refreshHistBtn.addActionListener(e -> loadSaleHistory());
        viewDetailsBtn.addActionListener(e -> showSelectedSaleDetails());
        stockSearchBtn.addActionListener(e -> applyStockSearch());
        stockClearBtn.addActionListener(e -> clearStockSearch());
        stockSearchField.addActionListener(e -> applyStockSearch());
        historySearchBtn.addActionListener(e -> applySaleHistorySearch());
        historyClearBtn.addActionListener(e -> clearSaleHistorySearch());
        saleHistorySearchField.addActionListener(e -> applySaleHistorySearch());
        customerTypeCombo.addActionListener(e -> onCustomerTypeChanged());
        customerNameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateTotal(); }
            @Override public void removeUpdate(DocumentEvent e) { updateTotal(); }
            @Override public void changedUpdate(DocumentEvent e) { updateTotal(); }
        });

        loadCustomerSuggestions();
        loadInStockItems();
        loadSaleHistory();
        loadPUOrders();
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadCustomerSuggestions() {
        List<String> names = new ArrayList<>();
        for (CustomerAccount ca : customerAccountDB.getAllCustomers()) {
            if (ca.getName() != null && !ca.getName().trim().isEmpty())
                names.add(ca.getName().trim());
        }
        customerNameField.setItems(names);
    }

    private void loadInStockItems() {
        inStockItemsById.clear();
        stockModel.setRowCount(0);
        for (LocalStockItem item : localStockItemDB.getAllItems()) {
            if (item.getQuantity() <= 0) continue;
            inStockItemsById.put(item.getItemID(), item);
            stockModel.addRow(new Object[]{
                    item.getItemID(),
                    item.getItemName(),
                    String.format("%.2f", item.calculateRetailPrice()),
                    item.getQuantity()
            });
        }
    }

    private void loadSaleHistory() {
        saleHistoryModel.setRowCount(0);
        for (Object[] row : salesDB.getAllSaleTransactions()) {
            saleHistoryModel.addRow(row);
        }
        applySaleHistorySearch();
    }

    private void applyStockSearch() {
        String text = stockSearchField.getText().trim();
        if (text.isEmpty()) {
            stockSorter.setRowFilter(null);
            return;
        }
        try {
            stockSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0, 1));
        } catch (java.util.regex.PatternSyntaxException ex) {
            stockSorter.setRowFilter(RowFilter.regexFilter(
                    "(?i)" + java.util.regex.Pattern.quote(text), 0, 1));
        }
    }

    private void clearStockSearch() {
        stockSearchField.setText("");
        stockSorter.setRowFilter(null);
    }

    private void applySaleHistorySearch() {
        String text = saleHistorySearchField.getText().trim();
        if (text.isEmpty()) {
            saleHistorySorter.setRowFilter(null);
            return;
        }
        try {
            saleHistorySorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0, 2));
        } catch (java.util.regex.PatternSyntaxException ex) {
            saleHistorySorter.setRowFilter(RowFilter.regexFilter(
                    "(?i)" + java.util.regex.Pattern.quote(text), 0, 2));
        }
    }

    private void clearSaleHistorySearch() {
        saleHistorySearchField.setText("");
        saleHistorySorter.setRowFilter(null);
    }

    private void loadPUOrders() {
        if (puOrderModel == null) return;
        loadingPUOrders = true;
        puOrderModel.setRowCount(0);
        for (OrdersDB.PUOrderRow row : ordersDB.getAllPUOrders()) {
            puOrderModel.addRow(new Object[]{
                    row.getAdjustmentId(),
                    row.getOrderId(),
                    row.getMerchantId(),
                    row.getItemId(),
                    row.getQty(),
                    row.getDeliveryAddress(),
                    row.getStatus(),
                    row.getCreatedAt(),
                    row.getProcessedAt(),
                    row.getError()
            });
        }
        loadingPUOrders = false;
    }

    // ── Sale item management ──────────────────────────────────────────────────

    private void onCustomerTypeChanged() {
        boolean isAccount = "Account Holder".equals(customerTypeCombo.getSelectedItem());
        customerNameField.setEditable(isAccount);
        customerNameField.setEnabled(isAccount);
        if (!isAccount) customerNameField.setText("");
        updateTotal();
    }

    private void addSelectedToSale() {
        int row = stockTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select an item."); return; }
        int modelRow = stockTable.convertRowIndexToModel(row);
        String qtyText = quantityField.getText().trim();
        if (qtyText.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter a quantity."); return; }
        try {
            int qty = Integer.parseInt(qtyText);
            if (qty <= 0) { JOptionPane.showMessageDialog(this, "Quantity must be > 0."); return; }
            String itemId = stockModel.getValueAt(modelRow, 0).toString();
            LocalStockItem item = inStockItemsById.get(itemId);
            if (item == null) { loadInStockItems(); return; }
            if (qty > item.getQuantity()) {
                JOptionPane.showMessageDialog(this, "Not enough stock."); return;
            }
            double price = item.calculateRetailPrice();
            saleModel.addRow(new Object[]{
                    item.getItemID(), item.getItemName(),
                    String.format("%.2f", price), qty,
                    String.format("%.2f", price * qty)
            });
            quantityField.setText("");
            updateTotal();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantity.");
        }
    }

    private void removeSelectedItem() {
        int row = saleTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a row to remove."); return; }
        saleModel.removeRow(row);
        updateTotal();
    }

    private void updateTotal() {
        double subtotal = 0.0;
        for (int i = 0; i < saleModel.getRowCount(); i++) {
            subtotal += Double.parseDouble(saleModel.getValueAt(i, 4).toString());
        }

        double discountRate = getCurrentDiscountRate(subtotal);
        double discountAmount = subtotal * discountRate;
        double netTotal = subtotal - discountAmount;

        if (discountRate > 0.0) {
            totalLabel.setText(String.format("Subtotal: £%.2f | Discount: -£%.2f | Total: £%.2f",
                    subtotal, discountAmount, netTotal));
        } else {
            totalLabel.setText(String.format("Total: £%.2f", netTotal));
        }
    }

    private double getCurrentDiscountRate(double subtotal) {
        String customerType = String.valueOf(customerTypeCombo.getSelectedItem());
        if (!"Account Holder".equalsIgnoreCase(customerType) && !"register".equalsIgnoreCase(customerType)) {
            return 0.0;
        }

        String customerName = customerNameField.getText() == null ? "" : customerNameField.getText().trim();
        if (customerName.isEmpty()) {
            return 0.0;
        }

        String plan = customerAccountDB.getDiscountPlanByName(customerName);
        return SalesDB.resolveFlexibleDiscountRate(plan, subtotal);
    }

    private void clearFields() {
        customerTypeCombo.setSelectedIndex(0);
        customerNameField.setText("");
        customerNameField.setEditable(false);
        customerNameField.setEnabled(false);
        quantityField.setText("");
    }

    // ── Process payment ───────────────────────────────────────────────────────

    private void processPayment() {
        if (saleModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No items in sale."); return;
        }

        boolean isAccount = "Account Holder".equals(customerTypeCombo.getSelectedItem());
        String customerName = customerNameField.getText().trim();

        if (isAccount && customerName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter the customer name."); return;
        }

        // Build items list
        List<SalesDB.SaleItem> items = new ArrayList<>();
        for (int i = 0; i < saleModel.getRowCount(); i++) {
            items.add(new SalesDB.SaleItem(
                    saleModel.getValueAt(i, 0).toString(),
                    saleModel.getValueAt(i, 1).toString(),
                    Double.parseDouble(saleModel.getValueAt(i, 2).toString()),
                    Integer.parseInt(saleModel.getValueAt(i, 3).toString())
            ));
        }

        // ── Collect payment method ────────────────────────────────────────────
        String paymentMethod;
        String cardType = null, cardFirst4 = null, cardLast4 = null, cardExpiry = null;

        if (isAccount) {
            // Account holders MUST pay by card — spec rule, no choice given
            paymentMethod = "card";
            cardType = JOptionPane.showInputDialog(this,
                    "Card type (Visa / Mastercard / Amex / Debit):");
            if (cardType == null) return;
            cardFirst4 = JOptionPane.showInputDialog(this, "First 4 digits of card:");
            if (cardFirst4 == null) return;
            cardLast4 = JOptionPane.showInputDialog(this, "Last 4 digits of card:");
            if (cardLast4 == null) return;
            cardExpiry = JOptionPane.showInputDialog(this, "Card expiry (MM/YYYY):");
            if (cardExpiry == null) return;
        } else {
            // Occasional customers can pay cash OR card
            String[] options = {"Cash", "Card"};
            int choice = JOptionPane.showOptionDialog(
                    this,
                    "Select payment method:",
                    "Payment Method",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);
            if (choice == -1) return;
            paymentMethod = (choice == 0) ? "cash" : "card";

            if ("card".equals(paymentMethod)) {
                cardType = JOptionPane.showInputDialog(this,
                        "Card type (Visa / Mastercard / Amex / Debit):");
                if (cardType == null) return;
                cardFirst4 = JOptionPane.showInputDialog(this, "First 4 digits of card:");
                if (cardFirst4 == null) return;
                cardLast4 = JOptionPane.showInputDialog(this, "Last 4 digits of card:");
                if (cardLast4 == null) return;
                cardExpiry = JOptionPane.showInputDialog(this, "Card expiry (MM/YYYY):");
                if (cardExpiry == null) return;
            }
        }

        // ── Submit sale ───────────────────────────────────────────────────────
        String customerType = isAccount ? "register" : "Walk In";
        String txnID = salesDB.submitSaleWithPayment(
                customerType, customerName, items,
                new Date(System.currentTimeMillis()),
                paymentMethod, cardType, cardFirst4, cardLast4, cardExpiry);

        if (txnID == null) {
            JOptionPane.showMessageDialog(this,
                    "Payment failed. Account may be in default, " +
                            "over credit limit, or insufficient stock.");
            return;
        }

        lastTransactionID = txnID;

        JOptionPane.showMessageDialog(this,
                "Payment processed successfully.\nTransaction ID: " + txnID);

        saleModel.setRowCount(0);
        updateTotal();
        loadInStockItems();
        loadSaleHistory();

        // Automatically offer receipt print after every sale
        printSelectedReceipt();
    }

    // ── Print receipt ─────────────────────────────────────────────────────────

    private void printSelectedReceipt() {
        String txnID = null;
        int selectedRow = saleHistoryTable.getSelectedRow();
        if (selectedRow != -1) {
            int modelRow = saleHistoryTable.convertRowIndexToModel(selectedRow);
            txnID = saleHistoryModel.getValueAt(modelRow, 0).toString();
        } else if (lastTransactionID != null) {
            txnID = lastTransactionID;
        } else {
            JOptionPane.showMessageDialog(this,
                    "Select a transaction from Sale History, or process a sale first.");
            return;
        }

        String txnSQL = """
            SELECT st.transactionID, st.customerID, st.customerType, st.customerName,
                   st.saleDate, st.paymentMethod, st.cardType,
                   st.subTotal, st.discountAmount, st.vatAmount, st.totalAmount,
                   ca.name AS accountName, ca.address AS accountAddress
            FROM SaleTransaction st
            LEFT JOIN CustomerAccount ca ON st.customerID = ca.customerID
            WHERE st.transactionID = ?
            """;
        String lineSQL = """
            SELECT l.itemName, lst.quantitySold, lst.unitRetailPrice, lst.lineTotal
            FROM LocalStockItem_SaleTransaction lst
            JOIN LocalStockItem l ON lst.itemID = l.itemID
            WHERE lst.transactionID = ?
            ORDER BY l.itemName
            """;

        StringBuilder sb = new StringBuilder();

        try (java.sql.Connection conn = DatabaseManager.getConnection()) {

            TemplatesDB templatesDB = new TemplatesDB();
            TemplatesDB.TemplateSettings settings = templatesDB.loadSettings();

            sb.append(settings.getPharmacyName()).append("\n");
            sb.append(settings.getAddress()).append("\n");
            sb.append(settings.getEmail()).append("\n");
            sb.append("─".repeat(52)).append("\n\n");

            try (java.sql.PreparedStatement ps = conn.prepareStatement(txnSQL)) {
                ps.setString(1, txnID);
                java.sql.ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "Transaction not found.");
                    return;
                }

                sb.append("RETAIL INVOICE\n");
                sb.append("Invoice No:  ").append(txnID).append("\n");
                sb.append("Date:        ").append(rs.getDate("saleDate")).append("\n");

                String ctype = rs.getString("customerType");
                if ("account".equals(ctype)) {
                    String accName = rs.getString("accountName");
                    String accAddr = rs.getString("accountAddress");
                    sb.append("Customer:    ").append(accName != null ? accName : "").append("\n");
                    sb.append("Address:     ").append(accAddr != null ? accAddr : "").append("\n");
                } else {
                    sb.append("Customer:    Occasional customer\n");
                }

                String payMethod = rs.getString("paymentMethod");
                String ct        = rs.getString("cardType");
                sb.append("Payment:     ").append(payMethod != null ? payMethod : "");
                if (ct != null && !ct.isEmpty()) sb.append(" (").append(ct).append(")");
                sb.append("\n\n");

                sb.append("─".repeat(52)).append("\n");
                sb.append(String.format("%-26s %5s %9s %9s\n", "Item", "Qty", "Price", "Total"));
                sb.append("─".repeat(52)).append("\n");

                try (java.sql.PreparedStatement lps = conn.prepareStatement(lineSQL)) {
                    lps.setString(1, txnID);
                    java.sql.ResultSet lrs = lps.executeQuery();
                    while (lrs.next()) {
                        sb.append(String.format("%-26s %5d %9.2f %9.2f\n",
                                truncate(lrs.getString("itemName"), 26),
                                lrs.getInt("quantitySold"),
                                lrs.getDouble("unitRetailPrice"),
                                lrs.getDouble("lineTotal")));
                    }
                }

                sb.append("─".repeat(52)).append("\n");
                sb.append(String.format("%-38s %12.2f\n",
                        "Sub-total (£):", rs.getDouble("subTotal")));

                double discount = rs.getDouble("discountAmount");
                if (discount > 0) {
                    sb.append(String.format("%-38s %12.2f\n",
                            "Discount (£):", -discount));
                }

                sb.append(String.format("%-38s %12.2f\n",
                        "VAT @ 0% (£):", rs.getDouble("vatAmount")));
                sb.append("─".repeat(52)).append("\n");
                sb.append(String.format("%-38s %12.2f\n",
                        "AMOUNT DUE (£):", rs.getDouble("totalAmount")));
                sb.append("─".repeat(52)).append("\n\n");

                if ("account".equals(ctype)) {
                    sb.append("Amount charged to account.\n");
                    sb.append("Payment due by end of calendar month.\n");
                } else {
                    sb.append("Paid in full. Thank you.\n");
                }

                sb.append("\nThank you for your valued custom.\n");
                sb.append(settings.getPharmacyName()).append("\n");
            }

        } catch (java.sql.SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Failed to load receipt: " + ex.getMessage());
            return;
        }

        JTextArea printArea = new JTextArea(sb.toString());
        printArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        printArea.setEditable(false);
        printArea.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(printArea);
        scroll.setPreferredSize(new Dimension(560, 520));

        int choice = JOptionPane.showOptionDialog(
                this, scroll, "Receipt — " + txnID,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                new String[]{"Print", "Close"},
                "Print");

        if (choice == 0) {
            try {
                printArea.print();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Print failed: " + ex.getMessage());
            }
        }
    }

    // ── Sale details popup ────────────────────────────────────────────────────

    private void showSelectedSaleDetails() {
        int row = saleHistoryTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a sale first."); return; }
        int modelRow = saleHistoryTable.convertRowIndexToModel(row);
        String txnId = saleHistoryModel.getValueAt(modelRow, 0).toString();
        List<OrdersDB.OrderItemDetail> items = salesDB.getSaleItemsByOrderId(txnId);
        if (items.isEmpty()) { JOptionPane.showMessageDialog(this, "No items found."); return; }
        StringBuilder sb = new StringBuilder("Transaction: " + txnId + "\n\n");
        for (OrdersDB.OrderItemDetail d : items)
            sb.append(d.getItemId()).append(" | ")
                    .append(d.getItemName()).append(" | Qty: ")
                    .append(d.getQuantity()).append("\n");
        JTextArea ta = new JTextArea(sb.toString(), 12, 50);
        ta.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta),
                "Sale Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "…";
    }
}