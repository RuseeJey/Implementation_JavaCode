package gui.main;

import database.CustomerAccountDB;
import gui.auth.LoginFrame;
import gui.panels.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainFrame extends JFrame {

    private final JTabbedPane tabbedPane;
    private final String role;
    private final String username;

    // Brand colours — matching LoginFrame
    private static final Color PRIMARY    = new Color(26, 82, 118);
    private static final Color BG_HEADER  = new Color(18, 30, 49);
    private static final Color ACCENT     = new Color(40, 180, 133);
    private static final Color TEXT_LIGHT = new Color(220, 230, 240);
    private static final Color TEXT_MUTED = new Color(140, 160, 180);

    public MainFrame(String role) {
        this(role, "");
    }

    public MainFrame(String role, String username) {
        this.role     = role.toLowerCase();
        this.username = username;

        // Run status engine for all customers on every login
        SwingUtilities.invokeLater(() -> {
            try {
                new CustomerAccountDB().runStatusEngineForAll();
            } catch (Exception e) {
                System.err.println("Status engine error: " + e.getMessage());
            }
        });

        setTitle("IPOS-CA — Cosymed Ltd  |  " + getDisplayRole());
        setSize(1200, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        styleTabbedPane();
        addRoleTabs(this.role);
        add(tabbedPane, BorderLayout.CENTER);
    }


    // ── Display role mapping ──────────────────────────────────────────────────

    private String getDisplayRole() {
        return switch (username.toLowerCase()) {
            case "sysdba"     -> "Administrator";
            case "manager"    -> "Director of Operations / Manager";
            case "accountant" -> "Senior Accountant";
            case "clerk"      -> "Accountant";
            default           -> role.substring(0, 1).toUpperCase() + role.substring(1);
        };
    }

    // ── Header panel ──────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background
                g2.setColor(BG_HEADER);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Accent bar at left edge
                g2.setColor(ACCENT);
                g2.fillRect(0, 0, 4, getHeight());

                // Subtle decorative circle
                g2.setColor(new Color(255, 255, 255, 8));
                g2.fillOval(getWidth() - 120, -40, 180, 180);
            }
        };
        header.setPreferredSize(new Dimension(0, 64));
        header.setBorder(new EmptyBorder(0, 20, 0, 16));

        // Left: logo circle + title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);

        // Small logo circle
        JPanel logoCircle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                String text = "CL";
                g2.drawString(text,
                        (getWidth() - fm.stringWidth(text)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        logoCircle.setPreferredSize(new Dimension(36, 36));
        logoCircle.setOpaque(false);
        leftPanel.add(logoCircle);
        leftPanel.add(Box.createHorizontalStrut(12));

        // Title + subtitle stacked
        JPanel titleStack = new JPanel(new GridLayout(2, 1, 0, 0));
        titleStack.setOpaque(false);

        JLabel titleLabel = new JLabel("IPOS-CA  —  Cosymed Ltd");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);

        JLabel roleLabel = new JLabel("Logged in as: " + getDisplayRole());
        roleLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        roleLabel.setForeground(TEXT_MUTED);

        titleStack.add(titleLabel);
        titleStack.add(roleLabel);
        leftPanel.add(titleStack);

        // Align left panel vertically
        JPanel leftWrapper = new JPanel(new GridBagLayout());
        leftWrapper.setOpaque(false);
        leftWrapper.add(leftPanel);

        header.add(leftWrapper, BorderLayout.WEST);

        // Right: logout button
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setOpaque(false);

        JButton logoutButton = new JButton("Logout") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed() ? new Color(180, 40, 40)
                        : getModel().isRollover() ? new Color(210, 60, 60)
                        : new Color(192, 57, 43);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        logoutButton.setPreferredSize(new Dimension(90, 34));
        logoutButton.setContentAreaFilled(false);
        logoutButton.setBorderPainted(false);
        logoutButton.setFocusPainted(false);
        logoutButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutButton.addActionListener(e -> handleLogout());
        logoutButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { logoutButton.repaint(); }
            @Override public void mouseExited(java.awt.event.MouseEvent e)  { logoutButton.repaint(); }
        });

        rightPanel.add(logoutButton);
        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }

    // ── Tabs ──────────────────────────────────────────────────────────────────

    private void styleTabbedPane() {
        tabbedPane.setFont(new Font("Arial", Font.PLAIN, 13));
        tabbedPane.setBackground(new Color(240, 243, 248));
        tabbedPane.setBorder(new EmptyBorder(4, 4, 4, 4));
    }

    private void addRoleTabs(String role) {
        if ("admin".equals(role)) {
            tabbedPane.addTab("Company Account", new CompanyAccountPanel());
            tabbedPane.addTab("User Management", new UserManagementPanel());
            return;
        }

        // Both pharmacist and manager get all operational tabs
        tabbedPane.addTab("Sales",             new SalesPanel());
        tabbedPane.addTab("Customer Accounts", new CustomerAccountPanel(role));
        tabbedPane.addTab("Stock",             new StockPanel());
        tabbedPane.addTab("Orders",            new OrdersPanel());
        tabbedPane.addTab("Reminders",         new RemindersPanel());
        tabbedPane.addTab("Templates",         new TemplatesPanel());
        tabbedPane.addTab("Reports",           new ReportsPanel());
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    private void handleLogout() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out?",
                "Logout",
                JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }
    }
}