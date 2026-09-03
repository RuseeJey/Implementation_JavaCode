package gui.auth;

import database.DatabaseManager;
import database.UserDB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel statusLabel;

    // Brand colours
    private static final Color PRIMARY      = new Color(26, 82, 118);   // dark blue
    private static final Color PRIMARY_DARK = new Color(17, 57, 84);    // hover
    private static final Color ACCENT       = new Color(40, 180, 133);  // teal green
    private static final Color BG_DARK      = new Color(18, 30, 49);    // left panel bg
    private static final Color BG_LIGHT     = new Color(245, 247, 250); // right panel bg
    private static final Color TEXT_LIGHT   = new Color(220, 230, 240);
    private static final Color TEXT_MUTED   = new Color(140, 160, 180);
    private static final Color FIELD_BG     = new Color(255, 255, 255);
    private static final Color FIELD_BORDER = new Color(210, 218, 230);
    private static final Color ERROR_RED    = new Color(192, 57, 43);

    public LoginFrame() {
        new UserDB();

        setTitle("IPOS-CA — Cosymed Ltd");
        setSize(820, 500);
        setMinimumSize(new Dimension(720, 450));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        setContentPane(buildContent());
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new GridLayout(1, 2));

        root.add(buildLeftPanel());
        root.add(buildRightPanel());

        return root;
    }

    // ── Left branding panel ───────────────────────────────────────────────────

    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background
                g2.setColor(BG_DARK);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Decorative circles
                g2.setColor(new Color(255, 255, 255, 12));
                g2.fillOval(-60, -60, 280, 280);
                g2.setColor(new Color(255, 255, 255, 8));
                g2.fillOval(80, getHeight() - 180, 260, 260);
                g2.setColor(new Color(40, 180, 133, 30));
                g2.fillOval(getWidth() - 100, getHeight() / 2 - 60, 180, 180);

                // Accent bar at top
                g2.setColor(ACCENT);
                g2.fillRect(0, 0, 5, getHeight());
            }
        };
        panel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(8, 30, 8, 30);

        // Logo circle with initials
        JPanel logoCircle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 26));
                FontMetrics fm = g2.getFontMetrics();
                String text = "CL";
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(text, x, y);
            }
        };
        logoCircle.setPreferredSize(new Dimension(72, 72));
        logoCircle.setOpaque(false);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 30, 16, 30);
        panel.add(logoCircle, gbc);

        // System name
        JLabel sysLabel = new JLabel("IPOS-CA");
        sysLabel.setFont(new Font("Arial", Font.BOLD, 30));
        sysLabel.setForeground(Color.WHITE);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 30, 4, 30);
        panel.add(sysLabel, gbc);

        // Subtitle
        JLabel subLabel = new JLabel("Client Application");
        subLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        subLabel.setForeground(TEXT_MUTED);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 30, 24, 30);
        panel.add(subLabel, gbc);

        // Divider
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255, 255, 255, 40));
        sep.setPreferredSize(new Dimension(160, 1));
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 30, 24, 30);
        panel.add(sep, gbc);

        // Pharmacy name
        JLabel pharmLabel = new JLabel("Cosymed Ltd");
        pharmLabel.setFont(new Font("Arial", Font.BOLD, 16));
        pharmLabel.setForeground(TEXT_LIGHT);
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 30, 6, 30);
        panel.add(pharmLabel, gbc);

        // Address
        JLabel addrLabel = new JLabel("<html><center>25 Bond Street<br>London WC1V 8LS</center></html>");
        addrLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        addrLabel.setForeground(TEXT_MUTED);
        addrLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 30, 0, 30);
        panel.add(addrLabel, gbc);

        // Version tag at bottom
        JLabel versionLabel = new JLabel("v1.0  •  IN2033 Team 41");
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        versionLabel.setForeground(new Color(100, 120, 140));
        gbc.gridy = 6;
        gbc.insets = new Insets(40, 30, 0, 30);
        panel.add(versionLabel, gbc);

        return panel;
    }

    // ── Right login panel ─────────────────────────────────────────────────────

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_LIGHT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Welcome heading
        JLabel welcomeLabel = new JLabel("Welcome back");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        welcomeLabel.setForeground(new Color(30, 40, 55));
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 48, 4, 48);
        panel.add(welcomeLabel, gbc);

        JLabel instrLabel = new JLabel("Sign in to your account to continue");
        instrLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        instrLabel.setForeground(new Color(110, 120, 135));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 48, 32, 48);
        panel.add(instrLabel, gbc);

        // Username field
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 48, 6, 48);
        panel.add(makeFieldLabel("Username"), gbc);

        usernameField = new JTextField();
        styleTextField(usernameField, "Enter your username");
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 48, 18, 48);
        panel.add(usernameField, gbc);

        // Password field
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 48, 6, 48);
        panel.add(makeFieldLabel("Password"), gbc);

        passwordField = new JPasswordField();
        styleTextField(passwordField, "Enter your password");
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 48, 28, 48);
        panel.add(passwordField, gbc);

        // Login button
        JButton loginButton = makeLoginButton();
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 48, 16, 48);
        panel.add(loginButton, gbc);

        // Status label
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setForeground(ERROR_RED);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 48, 0, 48);
        panel.add(statusLabel, gbc);

        // Enter key triggers login
        usernameField.addActionListener(e -> handleLogin());
        passwordField.addActionListener(e -> handleLogin());
        loginButton.addActionListener(e -> handleLogin());

        return panel;
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private JLabel makeFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setForeground(new Color(60, 75, 95));
        return label;
    }

    private void styleTextField(JTextField field, String placeholder) {
        field.setPreferredSize(new Dimension(0, 42));
        field.setFont(new Font("Arial", Font.PLAIN, 13));
        field.setBackground(FIELD_BG);
        field.setForeground(new Color(30, 40, 55));
        field.setCaretColor(PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER, 1, true),
                new EmptyBorder(0, 14, 0, 14)
        ));

        // Placeholder text
        field.setText(placeholder);
        field.setForeground(new Color(170, 180, 195));

        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(new Color(30, 40, 55));
                    if (field instanceof JPasswordField pf) {
                        pf.setEchoChar('•');
                    }
                }
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                boolean isEmpty = field instanceof JPasswordField
                        ? new String(((JPasswordField) field).getPassword()).isEmpty()
                        : field.getText().isEmpty();
                if (isEmpty) {
                    field.setText(placeholder);
                    field.setForeground(new Color(170, 180, 195));
                    if (field instanceof JPasswordField pf) {
                        pf.setEchoChar((char) 0);
                    }
                }
            }
        });

        // For password field, hide echo char initially (showing placeholder)
        if (field instanceof JPasswordField pf) {
            pf.setEchoChar((char) 0);
        }
    }

    private JButton makeLoginButton() {
        JButton btn = new JButton("Sign In") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? PRIMARY_DARK
                        : getModel().isRollover() ? new Color(31, 97, 141)
                        : PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(0, 44));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { btn.repaint(); }
        });

        return btn;
    }

    // ── Login logic ───────────────────────────────────────────────────────────

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        // Ignore placeholder text
        if (username.equals("Enter your username")) username = "";
        if (password.equals("Enter your password")) password = "";

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both username and password.");
            return;
        }

        String[] result = authenticateUser(username, password);

        if (result != null) {
            statusLabel.setText(" ");
            dispose();
            String role = result[0];
            new gui.main.MainFrame(role, username).setVisible(true);
        } else {
            statusLabel.setText("Incorrect username or password. Please try again.");
            passwordField.setText("");
            passwordField.setForeground(new Color(170, 180, 195));
            passwordField.setEchoChar((char) 0);
        }
    }

    private String[] authenticateUser(String username, String password) {
        String sql = "SELECT role FROM SystemUser WHERE userName = ? AND password = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new String[]{ rs.getString("role").toLowerCase() };
            }
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Database error. Please check your connection.");
        }
        return null;
    }
}