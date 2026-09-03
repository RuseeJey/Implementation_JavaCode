package gui.panels;

import database.OrdersDB;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class OrdersPanel extends JPanel {

    private JTable catalogueTable, orderTable, orderHistoryTable, puOrderTable;
    private DefaultTableModel catalogueModel, orderModel, orderHistoryModel, puOrderModel;
    private JTextField quantityField;
    private JTextField catalogueSearchField;
    private JTextField orderHistorySearchField;
    private TableRowSorter<DefaultTableModel> catalogueSorter;
    private TableRowSorter<DefaultTableModel> orderHistorySorter;
    private JLabel totalLabel;
    private final OrdersDB ordersDB;
    private boolean loadingHistory   = false;
    private boolean loadingPUOrders  = false;

    private static final String[] SA_STATUSES = {
            "accepted","ready to dispatch","dispatched","delivered","cancelled"
    };
    private static final String[] PU_STATUSES = {
            "PENDING","APPLIED","FAILED","CANCELLED"
    };

    public OrdersPanel() {
        ordersDB = new OrdersDB();
        setLayout(new BorderLayout(10, 10));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Place Order",  createPlaceOrderPanel());
        tabs.addTab("Track Orders", createTrackOrderPanel());
        add(tabs, BorderLayout.CENTER);

        loadCatalogueItems();
        loadOrderHistory();
    }

    // ── Place Order tab ───────────────────────────────────────────────────────

    private JPanel createPlaceOrderPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
        searchPanel.add(new JLabel("Search:"));
        catalogueSearchField = new JTextField(22);
        searchPanel.add(catalogueSearchField);
        JButton catalogueSearchBtn = new JButton("Search");
        JButton catalogueClearBtn = new JButton("Clear");
        searchPanel.add(catalogueSearchBtn);
        searchPanel.add(catalogueClearBtn);

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        inputPanel.add(new JLabel("Quantity:"));
        quantityField = new JTextField(8);
        inputPanel.add(quantityField);
        JButton addBtn = new JButton("Add to Order");
        inputPanel.add(addBtn);

        JPanel topPanel = new JPanel(new BorderLayout(0, 4));
        topPanel.add(searchPanel, BorderLayout.NORTH);
        topPanel.add(inputPanel, BorderLayout.CENTER);
        panel.add(topPanel, BorderLayout.NORTH);

        catalogueModel = new DefaultTableModel(
                new String[]{"Item ID", "Item Name", "Unit Price (£)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        orderModel = new DefaultTableModel(
                new String[]{"Item ID", "Item Name", "Unit Price", "Quantity", "Line Total"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        catalogueTable = new JTable(catalogueModel);
        orderTable     = new JTable(orderModel);
        catalogueSorter = new TableRowSorter<>(catalogueModel);
        catalogueTable.setRowSorter(catalogueSorter);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(catalogueTable), new JScrollPane(orderTable));
        split.setDividerLocation(180);
        panel.add(split, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton removeBtn = new JButton("Remove Selected");
        JButton submitBtn = new JButton("Submit Order");
        totalLabel = new JLabel("Order Total: £0.00");
        bottomPanel.add(removeBtn);
        bottomPanel.add(submitBtn);
        bottomPanel.add(totalLabel);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> addSelectedToOrder());
        removeBtn.addActionListener(e -> removeSelectedOrderItem());
        submitBtn.addActionListener(e -> submitOrder());
        catalogueSearchBtn.addActionListener(e -> applyCatalogueSearch());
        catalogueClearBtn.addActionListener(e -> clearCatalogueSearch());
        catalogueSearchField.addActionListener(e -> applyCatalogueSearch());
        return panel;
    }

    // ── Track Orders tab ──────────────────────────────────────────────────────

    private JPanel createTrackOrderPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
        searchPanel.add(new JLabel("Search:"));
        orderHistorySearchField = new JTextField(22);
        searchPanel.add(orderHistorySearchField);
        JButton historySearchBtn = new JButton("Search");
        JButton historyClearBtn = new JButton("Clear");
        searchPanel.add(historySearchBtn);
        searchPanel.add(historyClearBtn);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton refreshBtn = new JButton("Refresh");
        JButton detailsBtn = new JButton("View Details");
        JButton cancelBtn  = new JButton("Cancel Order");
        topPanel.add(refreshBtn);
        topPanel.add(detailsBtn);
        topPanel.add(cancelBtn);

        JPanel northPanel = new JPanel(new BorderLayout(0, 4));
        northPanel.add(searchPanel, BorderLayout.NORTH);
        northPanel.add(topPanel, BorderLayout.CENTER);
        panel.add(northPanel, BorderLayout.NORTH);

        orderHistoryModel = new DefaultTableModel(
                new String[]{"Order ID", "Status", "Total (£)", "Date"}, 0) {
            public boolean isCellEditable(int r, int c) {
                if (c != 1) return false;
                Object v = getValueAt(r, 1);
                return v != null
                        && !"delivered".equalsIgnoreCase(v.toString())
                        && !"cancelled".equalsIgnoreCase(v.toString());
            }
        };
        orderHistoryTable = new JTable(orderHistoryModel);
        orderHistorySorter = new TableRowSorter<>(orderHistoryModel);
        orderHistoryTable.setRowSorter(orderHistorySorter);
        JComboBox<String> statusCombo = new JComboBox<>(SA_STATUSES);
        orderHistoryTable.getColumnModel().getColumn(1).setCellEditor(
                new DefaultCellEditor(statusCombo));

        orderHistoryModel.addTableModelListener(e -> {
            if (loadingHistory
                    || e.getType() != javax.swing.event.TableModelEvent.UPDATE
                    || e.getColumn() != 1) return;
            int row = e.getFirstRow();
            String orderId   = orderHistoryModel.getValueAt(row, 0).toString();
            String newStatus = orderHistoryModel.getValueAt(row, 1).toString();
            if (!ordersDB.updateOrderStatus(orderId, newStatus)) {
                JOptionPane.showMessageDialog(this, "Failed to update status.");
                loadOrderHistory();
            } else {
                loadOrderHistory();
            }
        });

        panel.add(new JScrollPane(orderHistoryTable), BorderLayout.CENTER);
        refreshBtn.addActionListener(e -> loadOrderHistory());
        detailsBtn.addActionListener(e -> showOrderDetails());
        cancelBtn.addActionListener(e -> cancelOrder());
        historySearchBtn.addActionListener(e -> applyOrderHistorySearch());
        historyClearBtn.addActionListener(e -> clearOrderHistorySearch());
        orderHistorySearchField.addActionListener(e -> applyOrderHistorySearch());
        return panel;
    }

    // ── PU Orders tab — reads from ca_inventory_adjustments ──────────────────

    private JPanel createPUOrderPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton refreshBtn = new JButton("Refresh");
        JLabel info = new JLabel("Inbound adjustments from IPOS-PU portal (ca_inventory_adjustments)");
        info.setFont(new Font("Arial", Font.ITALIC, 12));
        topPanel.add(refreshBtn);
        topPanel.add(info);
        panel.add(topPanel, BorderLayout.NORTH);

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

        puOrderTable = new JTable(puOrderModel);
        JComboBox<String> puStatusCombo = new JComboBox<>(PU_STATUSES);
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
            } else {
                loadPUOrders();
            }
        });

        panel.add(new JScrollPane(puOrderTable), BorderLayout.CENTER);
        refreshBtn.addActionListener(e -> loadPUOrders());
        return panel;
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadCatalogueItems() {
        catalogueModel.setRowCount(0);
        for (OrdersDB.CatalogueItem item : ordersDB.getCatalogueItems()) {
            catalogueModel.addRow(new Object[]{
                    item.getItemId(),
                    item.getItemName(),
                    String.format("%.2f", item.getUnitPrice())
            });
        }
    }

    private void loadOrderHistory() {
        if (orderHistoryModel == null) return;
        loadingHistory = true;
        orderHistoryModel.setRowCount(0);
        for (OrdersDB.OrderSummary s : ordersDB.getOrderHistory()) {
            orderHistoryModel.addRow(new Object[]{
                    s.getOrderId(),
                    s.getStatus(),
                    String.format("%.2f", s.getTotalCost()),
                    s.getCreatedAt() != null ? s.getCreatedAt().toString() : ""
            });
        }
        loadingHistory = false;
    }

    private void applyCatalogueSearch() {
        if (catalogueSorter == null || catalogueSearchField == null) return;
        String text = catalogueSearchField.getText().trim();
        if (text.isEmpty()) {
            catalogueSorter.setRowFilter(null);
            return;
        }
        try {
            catalogueSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0, 1));
        } catch (java.util.regex.PatternSyntaxException ex) {
            catalogueSorter.setRowFilter(RowFilter.regexFilter(
                    "(?i)" + java.util.regex.Pattern.quote(text), 0, 1));
        }
    }

    private void clearCatalogueSearch() {
        if (catalogueSearchField == null || catalogueSorter == null) return;
        catalogueSearchField.setText("");
        catalogueSorter.setRowFilter(null);
    }

    private void applyOrderHistorySearch() {
        if (orderHistorySorter == null || orderHistorySearchField == null) return;
        String text = orderHistorySearchField.getText().trim();
        if (text.isEmpty()) {
            orderHistorySorter.setRowFilter(null);
            return;
        }
        try {
            orderHistorySorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0, 1));
        } catch (java.util.regex.PatternSyntaxException ex) {
            orderHistorySorter.setRowFilter(RowFilter.regexFilter(
                    "(?i)" + java.util.regex.Pattern.quote(text), 0, 1));
        }
    }

    private void clearOrderHistorySearch() {
        if (orderHistorySearchField == null || orderHistorySorter == null) return;
        orderHistorySearchField.setText("");
        orderHistorySorter.setRowFilter(null);
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

    // ── Actions ───────────────────────────────────────────────────────────────

    private void addSelectedToOrder() {
        int row = catalogueTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select an item."); return; }
        int modelRow = catalogueTable.convertRowIndexToModel(row);
        String qtyText = quantityField.getText().trim();
        if (qtyText.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter quantity."); return; }
        try {
            int qty = Integer.parseInt(qtyText);
            if (qty <= 0) { JOptionPane.showMessageDialog(this, "Quantity must be > 0."); return; }
            String id    = catalogueModel.getValueAt(modelRow, 0).toString();
            String name  = catalogueModel.getValueAt(modelRow, 1).toString();
            double price = Double.parseDouble(catalogueModel.getValueAt(modelRow, 2).toString());
            orderModel.addRow(new Object[]{
                    id, name,
                    String.format("%.2f", price),
                    qty,
                    String.format("%.2f", price * qty)
            });
            quantityField.setText("");
            updateOrderTotal();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantity.");
        }
    }

    private void removeSelectedOrderItem() {
        int row = orderTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a row to remove."); return; }
        orderModel.removeRow(row);
        updateOrderTotal();
    }

    private void submitOrder() {
        if (orderModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No items in order."); return;
        }
        List<OrdersDB.OrderItem> items = new ArrayList<>();
        for (int i = 0; i < orderModel.getRowCount(); i++) {
            items.add(new OrdersDB.OrderItem(
                    orderModel.getValueAt(i, 0).toString(),
                    orderModel.getValueAt(i, 1).toString(),
                    Double.parseDouble(orderModel.getValueAt(i, 2).toString()),
                    Integer.parseInt(orderModel.getValueAt(i, 3).toString())
            ));
        }
        String orderId = ordersDB.submitOrder(items);
        if (orderId == null) {
            JOptionPane.showMessageDialog(this, "Failed to submit order."); return;
        }
        JOptionPane.showMessageDialog(this, "Order submitted successfully.\nOrder ID: " + orderId);
        orderModel.setRowCount(0);
        updateOrderTotal();
        loadOrderHistory();
    }

    private void showOrderDetails() {
        int row = orderHistoryTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select an order."); return; }
        int modelRow = orderHistoryTable.convertRowIndexToModel(row);
        String orderId = orderHistoryModel.getValueAt(modelRow, 0).toString();
        List<OrdersDB.OrderItemDetail> details = ordersDB.getOrderItemsByOrderId(orderId);
        if (details.isEmpty()) { JOptionPane.showMessageDialog(this, "No items found."); return; }
        StringBuilder sb = new StringBuilder("Order ID: " + orderId + "\n\n");
        for (OrdersDB.OrderItemDetail d : details)
            sb.append(d.getItemId()).append(" | ").append(d.getItemName())
                    .append(" | Qty: ").append(d.getQuantity()).append("\n");
        JTextArea ta = new JTextArea(sb.toString(), 12, 50);
        ta.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta),
                "Order Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void cancelOrder() {
        int row = orderHistoryTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select an order."); return; }
        int modelRow = orderHistoryTable.convertRowIndexToModel(row);
        String status = orderHistoryModel.getValueAt(modelRow, 1).toString();
        if (!"accepted".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "Only 'accepted' orders can be cancelled.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Cancel this order?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        String orderId = orderHistoryModel.getValueAt(modelRow, 0).toString();
        if (ordersDB.cancelPendingOrderItems(orderId)) {
            JOptionPane.showMessageDialog(this, "Order cancelled.");
            loadOrderHistory();
        } else {
            JOptionPane.showMessageDialog(this, "Cancel failed.");
        }
    }

    private void updateOrderTotal() {
        double total = 0.0;
        for (int i = 0; i < orderModel.getRowCount(); i++)
            total += Double.parseDouble(orderModel.getValueAt(i, 4).toString());
        totalLabel.setText(String.format("Order Total: £%.2f", total));
    }
}