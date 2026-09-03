package IPOS_CA_USER;

public class SystemUser {

    private String userID;
    private String userName;
    private String password;
    private String role;   // "Admin", "Manager", "Pharmacist"

    public SystemUser() {}

    public SystemUser(String userID, String userName, String password, String role) {
        this.userID    = userID;
        this.userName  = userName;
        this.password  = password;
        this.role      = role;
    }

    // ── Role helpers used by MainFrame to gate tab access ─────────────────────

    public boolean isAdmin() {
        return "Admin".equalsIgnoreCase(role);
    }

    public boolean isManager() {
        return "Manager".equalsIgnoreCase(role) || isAdmin();
    }

    public boolean isPharmacist() {
        // Pharmacist, Accountant, and above can process sales etc.
        return "Pharmacist".equalsIgnoreCase(role)
                || "Accountant".equalsIgnoreCase(role)
                || isManager();
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getUserID()        { return userID; }
    public void setUserID(String v)  { userID = v; }

    public String getUserName()       { return userName; }
    public void setUserName(String v) { userName = v; }

    public String getPassword()       { return password; }
    public void setPassword(String v) { password = v; }

    public String getRole()           { return role; }
    public void setRole(String v)     { role = v; }

    @Override
    public String toString() { return userName + " (" + role + ")"; }
}