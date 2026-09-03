package gui.panels;

import database.UserDB;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class UserManagementPanel extends JPanel {

    private static final int COL_USER_ID = 0;
    private static final int COL_USERNAME = 1;
    private static final int COL_ROLE = 2;

    private final JTable userTable;
    private final DefaultTableModel tableModel;

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JComboBox<String> roleCombo;
    private final UserDB userDB;

    public UserManagementPanel() {
        userDB = new UserDB();

        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(2, 4, 10, 10));

        formPanel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        formPanel.add(usernameField);

        formPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        formPanel.add(passwordField);

        formPanel.add(new JLabel("Role:"));
        roleCombo = new JComboBox<>(new String[]{"Admin", "Manager", "Pharmacist"});
        formPanel.add(roleCombo);

        JButton createButton = new JButton("Create User");
        JButton clearButton = new JButton("Clear Fields");

        formPanel.add(createButton);
        formPanel.add(clearButton);

        add(formPanel, BorderLayout.NORTH);

        String[] columns = {"User ID", "Username", "Role"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        userTable = new JTable(tableModel);
        add(new JScrollPane(userTable), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton updateButton = new JButton("Update User");
        JButton deleteButton = new JButton("Delete User");
        JButton resetPasswordButton = new JButton("Reset Password");

        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(resetPasswordButton);

        add(buttonPanel, BorderLayout.SOUTH);

        createButton.addActionListener(e -> createUser());
        updateButton.addActionListener(e -> updateUser());
        clearButton.addActionListener(e -> clearFields());
        deleteButton.addActionListener(e -> deleteUser());
        resetPasswordButton.addActionListener(e -> resetPassword());

        userTable.getSelectionModel().addListSelectionListener(e -> fillFieldsFromSelectedRow());

        loadUsers();
    }

    private void loadUsers() {
        tableModel.setRowCount(0);
        for (UserDB.UserRow user : userDB.getAllUsers()) {
            tableModel.addRow(new Object[]{user.getUserId(), user.getUsername(), user.getRole()});
        }
    }

    private void createUser() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String role = String.valueOf(roleCombo.getSelectedItem());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password.");
            return;
        }

        if (userDB.usernameExists(username)) {
            JOptionPane.showMessageDialog(this, "That username already exists.");
            return;
        }

        boolean created = userDB.createUser(username, password, role);
        if (!created) {
            JOptionPane.showMessageDialog(this, "Failed to create user in database.");
            return;
        }

        loadUsers();
        JOptionPane.showMessageDialog(this, "User created successfully.");
        clearFields();
    }

    private void deleteUser() {
        int selectedRow = userTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete.");
            return;
        }

        String userId = tableModel.getValueAt(selectedRow, COL_USER_ID).toString();
        String username = tableModel.getValueAt(selectedRow, COL_USERNAME).toString();

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Delete user '" + username + "' (ID: " + userId + ")?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            boolean deleted = userDB.deleteUser(username);
            if (!deleted) {
                JOptionPane.showMessageDialog(this, "Failed to delete user from database.");
                return;
            }

            tableModel.removeRow(selectedRow);
            JOptionPane.showMessageDialog(this, "User deleted successfully.");
            clearFields();
        }
    }

    private void updateUser() {
        int selectedRow = userTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to update.");
            return;
        }

        String userId = tableModel.getValueAt(selectedRow, COL_USER_ID).toString();
        String username = usernameField.getText().trim();
        String role = String.valueOf(roleCombo.getSelectedItem());

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username is required.");
            return;
        }

        if (userDB.usernameExistsForOtherUser(userId, username)) {
            JOptionPane.showMessageDialog(this, "That username already exists.");
            return;
        }

        boolean updated = userDB.updateUser(userId, username, role);
        if (!updated) {
            JOptionPane.showMessageDialog(this, "Failed to update user in database.");
            return;
        }

        loadUsers();
        JOptionPane.showMessageDialog(this, "User updated successfully.");
        clearFields();
    }

    private void resetPassword() {
        int selectedRow = userTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user first.");
            return;
        }

        String newPassword = JOptionPane.showInputDialog(this, "Enter new password:");

        if (newPassword == null || newPassword.trim().isEmpty()) {
            return;
        }

        String username = tableModel.getValueAt(selectedRow, COL_USERNAME).toString();
        boolean updated = userDB.resetPassword(username, newPassword.trim());
        if (!updated) {
            JOptionPane.showMessageDialog(this, "Failed to reset password in database.");
            return;
        }

        JOptionPane.showMessageDialog(this, "Password reset successfully for selected user.");
    }


    private void fillFieldsFromSelectedRow() {
        int selectedRow = userTable.getSelectedRow();

        if (selectedRow == -1) {
            return;
        }

        usernameField.setText(tableModel.getValueAt(selectedRow, COL_USERNAME).toString());
        roleCombo.setSelectedItem(tableModel.getValueAt(selectedRow, COL_ROLE).toString());
        passwordField.setText("");
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        roleCombo.setSelectedIndex(0);
        userTable.clearSelection();
    }
}