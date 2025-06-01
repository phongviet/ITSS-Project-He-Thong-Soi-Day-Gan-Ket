package controller.verification;

import entity.users.Admin;
import entity.users.Volunteer;
import entity.users.VolunteerOrganization;
import entity.users.PersonInNeed;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
     * @param cccd Citizen ID card number
     * @param dateOfBirth Date of birth
     * @param freeHourPerWeek Free hours per week available for volunteering
     * @param skills List of skills volunteer possesses
     * @return true if registration successful, false otherwise
     */
    public boolean registerVolunteer(String username, String password, String email,
                                     String phone, String address, String fullName,
                                     String cccd, String dateOfBirth, int freeHourPerWeek,
                                     List<String> skills) {
        // Check if username already exists
        if (usernameExists(username)) {
            System.out.println("Username already exists: " + username);
            return false;
        }

        Connection conn = null;
        PreparedStatement pstmtUser = null;
        PreparedStatement pstmtVolunteer = null;
        PreparedStatement pstmtSkill = null;

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

            // Then insert into Volunteer table with the updated schema
            String insertVolunteerSQL = "INSERT INTO Volunteer (username, fullName, cccd, dateOfBirth, averageRating, ratingCount, freeHourPerWeek) VALUES (?, ?, ?, ?, ?, ?, ?)";
            pstmtVolunteer = conn.prepareStatement(insertVolunteerSQL);
            pstmtVolunteer.setString(1, username);
            pstmtVolunteer.setString(2, fullName);
            pstmtVolunteer.setString(3, cccd);
            pstmtVolunteer.setString(4, dateOfBirth); // Assuming dateOfBirth is in YYYY-MM-DD format
            pstmtVolunteer.setDouble(5, 0.0); // Default rating
            pstmtVolunteer.setInt(6, 0);      // Default rating count
            pstmtVolunteer.setInt(7, freeHourPerWeek);
            pstmtVolunteer.executeUpdate();

            // Insert skills if provided
            if (skills != null && !skills.isEmpty()) {
                // Create skills in the Skills table first
                for (String skill : skills) {
                    // Check if skill already exists
                    String checkSkillSQL = "SELECT skillId FROM Skills WHERE skill = ?";
                    PreparedStatement checkSkillStmt = conn.prepareStatement(checkSkillSQL);
                    checkSkillStmt.setString(1, skill);
                    ResultSet skillRs = checkSkillStmt.executeQuery();

                    int skillId;
                    if (skillRs.next()) {
                        skillId = skillRs.getInt("skillId");
                    } else {
                        // Insert new skill
                        String insertSkillSQL = "INSERT INTO Skills (skill) VALUES (?)";
                        PreparedStatement insertSkillStmt = conn.prepareStatement(insertSkillSQL, Statement.RETURN_GENERATED_KEYS);
                        insertSkillStmt.setString(1, skill);
                        insertSkillStmt.executeUpdate();

                        ResultSet generatedKeys = insertSkillStmt.getGeneratedKeys();
                        if (generatedKeys.next()) {
                            skillId = generatedKeys.getInt(1);
                        } else {
                            throw new SQLException("Creating skill failed, no ID obtained.");
                        }
                        insertSkillStmt.close();
                    }

                    // Link volunteer to skill
                    String linkSkillSQL = "INSERT INTO VolunteerSkills (username, skillId) VALUES (?, ?)";
                    PreparedStatement linkSkillStmt = conn.prepareStatement(linkSkillSQL);
                    linkSkillStmt.setString(1, username);
                    linkSkillStmt.setInt(2, skillId);
                    linkSkillStmt.executeUpdate();
                    linkSkillStmt.close();

                    checkSkillStmt.close();
                }
            }

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
            closeStatement(pstmtSkill);
        }
    }

    /**
     * Register a new Volunteer in the system (overloaded method for backward compatibility)
     */
    public boolean registerVolunteer(String username, String password, String email,
                                     String phone, String address, String fullName) {
        return registerVolunteer(username, password, email, phone, address, fullName,
                               null, null, 0, null);
    }

    /**
     * Register a new Volunteer in the system (compatibility with previous skills/availability API)
     */
    public boolean registerVolunteer(String username, String password, String email,
                                     String phone, String address, String fullName,
                                     List<String> skills, Map<String, Integer> availability) {
        // Calculate total free hours per week from availability map
        int freeHourPerWeek = 0;
        if (availability != null && !availability.isEmpty()) {
            for (Integer hours : availability.values()) {
                freeHourPerWeek += hours;
            }
        }

        return registerVolunteer(username, password, email, phone, address, fullName,
                               null, null, freeHourPerWeek, skills);
    }

    /**
     * Register a new Volunteer Organization in the system
     * @param username Unique username
     * @param password User's password
     * @param email User's email
     * @param phone User's phone number
     * @param address User's address
     * @param organizationName Name of the organization
     * @param licenseNumber Organization's license number
     * @param field Field of operation
     * @param representative Organization representative
     * @param sponsor Organization sponsor
     * @param info Additional information
     * @return true if registration successful, false otherwise
     */
    public boolean registerVolunteerOrganization(String username, String password, String email,
                                                 String phone, String address, String organizationName,
                                                 String licenseNumber, String field, String representative,
                                                 String sponsor, String info) {
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

            // Then insert into VolunteerOrganization table with new fields
            String insertOrgSQL = "INSERT INTO VolunteerOrganization (username, organizationName, licenseNumber, field, representative, sponsor, info) VALUES (?, ?, ?, ?, ?, ?, ?)";
            pstmtOrg = conn.prepareStatement(insertOrgSQL);
            pstmtOrg.setString(1, username);
            pstmtOrg.setString(2, organizationName);
            pstmtOrg.setString(3, licenseNumber);
            pstmtOrg.setString(4, field);
            pstmtOrg.setString(5, representative);
            pstmtOrg.setString(6, sponsor);
            pstmtOrg.setString(7, info);
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
     * Register a new Volunteer Organization in the system (overloaded method for backward compatibility)
     */
    public boolean registerVolunteerOrganization(String username, String password, String email,
                                                 String phone, String address, String organizationName,
                                                 String licenseNumber) {
        // Use default values for new fields
        return registerVolunteerOrganization(username, password, email, phone, address,
                                          organizationName, licenseNumber, "", "",
                                          "None", "No additional information");
    }

    /**
     * Register a new Person In Need in the system
     * @param username Unique username
     * @param password User's password
     * @param email User's email
     * @param phone User's phone number
     * @param address User's address
     * @param fullName Person's full name
     * @param cccd Citizen ID card number
     * @param dateOfBirth Date of birth
     * @return true if registration successful, false otherwise
     */
    public boolean registerPersonInNeed(String username, String password, String email,
                                        String phone, String address, String fullName,
                                        String cccd, String dateOfBirth) {
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

            // Then insert into PersonInNeed table with additional fields
            String insertPersonSQL = "INSERT INTO PersonInNeed (username, fullName, cccd, dateOfBirth) VALUES (?, ?, ?, ?)";
            pstmtPerson = conn.prepareStatement(insertPersonSQL);
            pstmtPerson.setString(1, username);
            pstmtPerson.setString(2, fullName);
            pstmtPerson.setString(3, cccd);
            pstmtPerson.setString(4, dateOfBirth); // Assuming dateOfBirth is in YYYY-MM-DD format
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
     * Register a new Person In Need in the system (overloaded method for backward compatibility)
     */
    public boolean registerPersonInNeed(String username, String password, String email,
                                        String phone, String address, String fullName) {
        return registerPersonInNeed(username, password, email, phone, address, fullName, null, null);
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
     * @return "Admin", "Volunteer", "VolunteerOrganization", "PersonInNeed", or null if not found
     */
    public String getUserType(String username) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DriverManager.getConnection(DB_URL);
            
            // Check if user is an Admin
            pstmt = conn.prepareStatement("SELECT * FROM Admin WHERE username = ?");
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return "Admin";
            }

            // Check if user is a Volunteer
            closeResources(null, pstmt, rs);
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
            
            // Get organization data with all fields from the updated schema
            pstmt = conn.prepareStatement(
                "SELECT u.username, u.email, u.phone, u.address, " +
                "o.organizationName, o.licenseNumber, o.field, o.representative, o.sponsor, o.info " +
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
                org.setField(rs.getString("field"));
                org.setRepresentative(rs.getString("representative"));
                org.setSponsor(rs.getString("sponsor"));
                org.setInfo(rs.getString("info"));
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

    /**
     * Get Admin object by username
     * @param username The username of the admin
     * @return Admin object or null if not found
     */
    public Admin getAdmin(String username) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(DB_URL);

            // Check if user is an Admin
            pstmt = conn.prepareStatement("SELECT * FROM Admin WHERE username = ?");
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                Admin admin = new Admin();
                admin.setUsername(username);
                // Password is not set for security reasons
                return admin;
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
     * Get Volunteer object by username
     * @param username The username of the volunteer
     * @return Volunteer object or null if not found
     */
    public Volunteer getVolunteer(String username) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(DB_URL);

            // Get volunteer data with all fields from the updated schema
            String sql = "SELECT u.username, u.email, u.phone, u.address, " +
                         "v.fullName, v.cccd, v.dateOfBirth, v.averageRating, v.ratingCount, v.freeHourPerWeek " +
                         "FROM SystemUser u " +
                         "JOIN Volunteer v ON u.username = v.username " +
                         "WHERE u.username = ?";

            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                Volunteer volunteer = new Volunteer();
                volunteer.setUsername(rs.getString("username"));
                volunteer.setEmail(rs.getString("email"));
                volunteer.setPhone(rs.getString("phone"));
                volunteer.setAddress(rs.getString("address"));
                volunteer.setFullName(rs.getString("fullName"));
                volunteer.setCccd(rs.getString("cccd"));

                // Convert string date to Date object if not null
                String dateStr = rs.getString("dateOfBirth");
                if (dateStr != null && !dateStr.isEmpty()) {
                    try {
                        java.sql.Date sqlDate = rs.getDate("dateOfBirth");
                        if (sqlDate != null) {
                            volunteer.setDateOfBirth(new Date(sqlDate.getTime()));
                        }
                    } catch (SQLException e) {
                        System.err.println("Error parsing date: " + e.getMessage());
                    }
                }

                volunteer.setAverageRating(rs.getDouble("averageRating"));
                volunteer.setRatingCount(rs.getInt("ratingCount"));
                volunteer.setFreeHourPerWeek(rs.getInt("freeHourPerWeek"));

                // Get volunteer's skills (optional)
                volunteer.setSkills(getVolunteerSkills(username, conn));

                return volunteer;
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
     * Helper method to get a volunteer's skills
     */
    private List<String> getVolunteerSkills(String username, Connection existingConn) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<String> skills = new ArrayList<>();

        try {
            conn = existingConn != null ? existingConn : DriverManager.getConnection(DB_URL);

            String sql = "SELECT s.skill FROM Skills s " +
                         "JOIN VolunteerSkills vs ON s.skillId = vs.skillId " +
                         "WHERE vs.username = ?";

            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                skills.add(rs.getString("skill"));
            }

            return skills;
        } catch (SQLException e) {
            e.printStackTrace();
            return skills;
        } finally {
            if (existingConn == null) {
                closeResources(conn, pstmt, rs);
            } else {
                closeStatement(pstmt);
                try {
                    if (rs != null) rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Get PersonInNeed object by username
     * @param username The username of the person in need
     * @return PersonInNeed object or null if not found
     */
    public PersonInNeed getPersonInNeed(String username) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(DB_URL);

            // Get person in need data with all fields from the updated schema
            String sql = "SELECT u.username, u.email, u.phone, u.address, p.fullName, p.cccd, p.dateOfBirth " +
                         "FROM SystemUser u " +
                         "JOIN PersonInNeed p ON u.username = p.username " +
                         "WHERE u.username = ?";

            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                PersonInNeed personInNeed = new PersonInNeed();
                personInNeed.setUsername(rs.getString("username"));
                personInNeed.setEmail(rs.getString("email"));
                personInNeed.setPhone(rs.getString("phone"));
                personInNeed.setAddress(rs.getString("address"));
                personInNeed.setFullName(rs.getString("fullName"));
                personInNeed.setCccd(rs.getString("cccd"));

                // Convert string date to Date object if not null
                String dateStr = rs.getString("dateOfBirth");
                if (dateStr != null && !dateStr.isEmpty()) {
                    try {
                        java.sql.Date sqlDate = rs.getDate("dateOfBirth");
                        if (sqlDate != null) {
                            personInNeed.setDateOfBirth(new Date(sqlDate.getTime()));
                        }
                    } catch (SQLException e) {
                        System.err.println("Error parsing date: " + e.getMessage());
                    }
                }

                return personInNeed;
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
