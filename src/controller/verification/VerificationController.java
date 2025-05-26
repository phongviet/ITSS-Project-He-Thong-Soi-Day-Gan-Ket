package controller.verification;

import entity.users.VolunteerOrganization;

import java.sql.*;

public class VerificationController {
    private static final String DB_URL = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db";

    public boolean checkInfo(String username, String password) {
        String url = "jdbc:sqlite:assets\\db\\SoiDayGanKet_sqlite.db";
        String sql = "SELECT * FROM SystemUser WHERE username = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean usernameExists(String username) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(DB_URL);
            String query = "SELECT username FROM SystemUser WHERE username = ?";
            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();

            return rs.next(); // Returns true if username exists
        } catch (SQLException e) {
            System.err.println("Error checking username existence: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }

    /**
     * Register a new Volunteer in the system
     * @param username Unique username
     * @param password User's password
     * @param email User's email
     * @param phone User's phone number
     * @param address User's address
     * @param fullName Volunteer's full name
     * @return true if registration successful, false otherwise
     */
    public boolean registerVolunteer(String username, String password, String email,
                                     String phone, String address, String fullName) {
        // Check if username already exists
        if (usernameExists(username)) {
            System.out.println("Username already exists: " + username);
            return false;
        }

        Connection conn = null;
        PreparedStatement pstmtUser = null;
        PreparedStatement pstmtVolunteer = null;

        try {
            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(false); // Start transaction

            // First insert into SystemUser table
            String insertUserSQL = "INSERT INTO SystemUser (username, password, email, phone, address) VALUES (?, ?, ?, ?, ?)";
            pstmtUser = conn.prepareStatement(insertUserSQL);
            pstmtUser.setString(1, username);
            pstmtUser.setString(2, password);
            pstmtUser.setString(3, email);
            pstmtUser.setString(4, phone);
            pstmtUser.setString(5, address);
            pstmtUser.executeUpdate();

            // Then insert into Volunteer table
            String insertVolunteerSQL = "INSERT INTO Volunteer (username, fullName) VALUES (?, ?)";
            pstmtVolunteer = conn.prepareStatement(insertVolunteerSQL);
            pstmtVolunteer.setString(1, username);
            pstmtVolunteer.setString(2, fullName);
            pstmtVolunteer.executeUpdate();

            conn.commit(); // Commit transaction
            System.out.println("Volunteer registered successfully: " + username);
            return true;

        } catch (SQLException e) {
            System.err.println("Error registering volunteer: " + e.getMessage());
            e.printStackTrace();

            // Rollback transaction on error
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            closeResources(conn, pstmtUser, null);
            closeStatement(pstmtVolunteer);
        }
    }

    /**
     * Register a new Volunteer Organization in the system
     * @param username Unique username
     * @param password User's password
     * @param email User's email
     * @param phone User's phone number
     * @param address User's address
     * @param organizationName Name of the organization
     * @param licenseNumber Organization's license number (optional)
     * @return true if registration successful, false otherwise
     */
    public boolean registerVolunteerOrganization(String username, String password, String email,
                                                 String phone, String address, String organizationName,
                                                 String licenseNumber) {
        // Check if username already exists
        if (usernameExists(username)) {
            System.out.println("Username already exists: " + username);
            return false;
        }

        Connection conn = null;
        PreparedStatement pstmtUser = null;
        PreparedStatement pstmtOrg = null;

        try {
            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(false); // Start transaction

            // First insert into SystemUser table
            String insertUserSQL = "INSERT INTO SystemUser (username, password, email, phone, address) VALUES (?, ?, ?, ?, ?)";
            pstmtUser = conn.prepareStatement(insertUserSQL);
            pstmtUser.setString(1, username);
            pstmtUser.setString(2, password);
            pstmtUser.setString(3, email);
            pstmtUser.setString(4, phone);
            pstmtUser.setString(5, address);
            pstmtUser.executeUpdate();

            // Then insert into VolunteerOrganization table
            String insertOrgSQL = "INSERT INTO VolunteerOrganization (username, organizationName, licenseNumber) VALUES (?, ?, ?)";
            pstmtOrg = conn.prepareStatement(insertOrgSQL);
            pstmtOrg.setString(1, username);
            pstmtOrg.setString(2, organizationName);
            pstmtOrg.setString(3, licenseNumber);
            pstmtOrg.executeUpdate();

            conn.commit(); // Commit transaction
            System.out.println("Volunteer Organization registered successfully: " + username);
            return true;

        } catch (SQLException e) {
            System.err.println("Error registering volunteer organization: " + e.getMessage());
            e.printStackTrace();

            // Rollback transaction on error
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            closeResources(conn, pstmtUser, null);
            closeStatement(pstmtOrg);
        }
    }

    /**
     * Register a new Person In Need in the system
     * @param username Unique username
     * @param password User's password
     * @param email User's email
     * @param phone User's phone number
     * @param address User's address
     * @param fullName Person's full name
     * @return true if registration successful, false otherwise
     */
    public boolean registerPersonInNeed(String username, String password, String email,
                                        String phone, String address, String fullName) {
        // Check if username already exists
        if (usernameExists(username)) {
            System.out.println("Username already exists: " + username);
            return false;
        }

        Connection conn = null;
        PreparedStatement pstmtUser = null;
        PreparedStatement pstmtPerson = null;

        try {
            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(false); // Start transaction

            // First insert into SystemUser table
            String insertUserSQL = "INSERT INTO SystemUser (username, password, email, phone, address) VALUES (?, ?, ?, ?, ?)";
            pstmtUser = conn.prepareStatement(insertUserSQL);
            pstmtUser.setString(1, username);
            pstmtUser.setString(2, password);
            pstmtUser.setString(3, email);
            pstmtUser.setString(4, phone);
            pstmtUser.setString(5, address);
            pstmtUser.executeUpdate();

            // Then insert into HelpSeeker table (mapped to PersonInNeed in the application)
            String insertPersonSQL = "INSERT INTO PersonInNeed (username, fullName) VALUES (?, ?)";
            pstmtPerson = conn.prepareStatement(insertPersonSQL);
            pstmtPerson.setString(1, username);
            pstmtPerson.setString(2, fullName);
            pstmtPerson.executeUpdate();

            conn.commit(); // Commit transaction
            System.out.println("Person In Need registered successfully: " + username);
            return true;

        } catch (SQLException e) {
            System.err.println("Error registering person in need: " + e.getMessage());
            e.printStackTrace();

            // Rollback transaction on error
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            closeResources(conn, pstmtUser, null);
            closeStatement(pstmtPerson);
        }
    }

    /**
     * Close database connection and statement resources
     */
    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Close statement resource
     */
    private void closeStatement(Statement stmt) {
        try {
            if (stmt != null) stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Get the type of user based on username
     * @param username The username to check
     * @return "Volunteer", "VolunteerOrganization", "PersonInNeed", or null if not found
     */
    public String getUserType(String username) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DriverManager.getConnection(DB_URL);
            
            // Check if user is a Volunteer
            pstmt = conn.prepareStatement("SELECT * FROM Volunteer WHERE username = ?");
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return "Volunteer";
            }
            
            // Check if user is a VolunteerOrganization
            closeResources(null, pstmt, rs);
            pstmt = conn.prepareStatement("SELECT * FROM VolunteerOrganization WHERE username = ?");
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return "VolunteerOrganization";
            }
            
            // Check if user is a PersonInNeed
            closeResources(null, pstmt, rs);
            pstmt = conn.prepareStatement("SELECT * FROM PersonInNeed WHERE username = ?");
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return "PersonInNeed";
            }
            
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }
    
    /**
     * Get VolunteerOrganization object by username
     * @param username The username of the organization
     * @return VolunteerOrganization object or null if not found
     */
    public VolunteerOrganization getVolunteerOrganization(String username) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DriverManager.getConnection(DB_URL);
            
            // Get organization data
            pstmt = conn.prepareStatement(
                "SELECT u.username, u.email, u.phone, u.address, o.organizationName, o.licenseNumber " +
                "FROM SystemUser u " +
                "JOIN VolunteerOrganization o ON u.username = o.username " +
                "WHERE u.username = ?"
            );
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                VolunteerOrganization org = new VolunteerOrganization();
                org.setUsername(rs.getString("username"));
                org.setEmail(rs.getString("email"));
                org.setPhone(rs.getString("phone"));
                org.setAddress(rs.getString("address"));
                org.setOrganizationName(rs.getString("organizationName"));
                org.setLicenseNumber(rs.getString("licenseNumber"));
                return org;
            }
            
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }
}