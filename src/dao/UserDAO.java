package dao;

import entity.users.Admin;
import entity.users.PersonInNeed;
import entity.users.Volunteer;
import entity.users.VolunteerOrganization;
import entity.users.SystemUser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.sql.Types;
import entity.events.EventParticipantDetails;
import utils.AppConstants;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import entity.events.Event;

/**
 * UserDAO class handles all database operations related to users.
 * This includes SystemUser, Volunteer, PersonInNeed, Admin, and VolunteerOrganization.
 */
public class UserDAO {

    private static final String DB_URL = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db";

    /**
     * Authenticates a user by checking their username and password
     * @param username The username to check
     * @param password The password to check
     * @return true if credentials are valid, false otherwise
     */
    public boolean authenticateUser(String username, String password) {
        String sql = "SELECT * FROM SystemUser WHERE username = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks if a username already exists in the SystemUser table.
     * @param username The username to check.
     * @return true if the username exists, false otherwise.
     */
    public boolean usernameExists(String username) {
        String query = "SELECT username FROM SystemUser WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking username existence: " + e.getMessage());
            e.printStackTrace();
            return false;
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
        if (usernameExists(username)) {
            System.out.println("Username already exists: " + username);
            return false;
        }

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(false);

            String insertUserSQL = "INSERT INTO SystemUser (username, password, email, phone, address) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmtUser = conn.prepareStatement(insertUserSQL)) {
                pstmtUser.setString(1, username);
                pstmtUser.setString(2, password);
                pstmtUser.setString(3, email);
                pstmtUser.setString(4, phone);
                pstmtUser.setString(5, address);
                pstmtUser.executeUpdate();
            }

            String insertVolunteerSQL = "INSERT INTO Volunteer (username, fullName, cccd, dateOfBirth, averageRating, ratingCount, freeHourPerWeek) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmtVolunteer = conn.prepareStatement(insertVolunteerSQL)) {
                pstmtVolunteer.setString(1, username);
                pstmtVolunteer.setString(2, fullName);
                pstmtVolunteer.setString(3, cccd);
                pstmtVolunteer.setString(4, dateOfBirth);
                pstmtVolunteer.setDouble(5, 0.0);
                pstmtVolunteer.setInt(6, 0);
                pstmtVolunteer.setInt(7, freeHourPerWeek);
                pstmtVolunteer.executeUpdate();
            }

            if (skills != null && !skills.isEmpty()) {
                for (String skill : skills) {
                    int skillId = getOrCreateSkill(conn, skill);

                    String linkSkillSQL = "INSERT INTO VolunteerSkills (username, skillId) VALUES (?, ?)";
                    try (PreparedStatement linkSkillStmt = conn.prepareStatement(linkSkillSQL)) {
                        linkSkillStmt.setString(1, username);
                        linkSkillStmt.setInt(2, skillId);
                        linkSkillStmt.executeUpdate();
                    }
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error registering volunteer: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            closeResources(conn, null, null);
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
        if (usernameExists(username)) {
            System.out.println("Username already exists: " + username);
            return false;
        }

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(false);

            String insertUserSQL = "INSERT INTO SystemUser (username, password, email, phone, address) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmtUser = conn.prepareStatement(insertUserSQL)) {
                pstmtUser.setString(1, username);
                pstmtUser.setString(2, password);
                pstmtUser.setString(3, email);
                pstmtUser.setString(4, phone);
                pstmtUser.setString(5, address);
                pstmtUser.executeUpdate();
            }

            String insertOrgSQL = "INSERT INTO VolunteerOrganization (username, organizationName, licenseNumber, field, representative, sponsor, info) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmtOrg = conn.prepareStatement(insertOrgSQL)) {
                pstmtOrg.setString(1, username);
                pstmtOrg.setString(2, organizationName);
                pstmtOrg.setString(3, licenseNumber);
                pstmtOrg.setString(4, field);
                pstmtOrg.setString(5, representative);
                pstmtOrg.setString(6, sponsor);
                pstmtOrg.setString(7, info);
                pstmtOrg.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error registering volunteer organization: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            closeResources(conn, null, null);
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
     * @param cccd Citizen ID card number
     * @param dateOfBirth Date of birth
     * @return true if registration successful, false otherwise
     */
    public boolean registerPersonInNeed(String username, String password, String email,
                                        String phone, String address, String fullName,
                                        String cccd, String dateOfBirth) {
        if (usernameExists(username)) {
            System.out.println("Username already exists: " + username);
            return false;
        }

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(false);

            String insertUserSQL = "INSERT INTO SystemUser (username, password, email, phone, address) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmtUser = conn.prepareStatement(insertUserSQL)) {
                pstmtUser.setString(1, username);
                pstmtUser.setString(2, password);
                pstmtUser.setString(3, email);
                pstmtUser.setString(4, phone);
                pstmtUser.setString(5, address);
                pstmtUser.executeUpdate();
            }

            String insertPersonSQL = "INSERT INTO PersonInNeed (username, fullName, cccd, dateOfBirth) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmtPerson = conn.prepareStatement(insertPersonSQL)) {
                pstmtPerson.setString(1, username);
                pstmtPerson.setString(2, fullName);
                pstmtPerson.setString(3, cccd);
                pstmtPerson.setString(4, dateOfBirth);
                pstmtPerson.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error registering person in need: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            closeResources(conn, null, null);
        }
    }

    /**
     * Get the type of user based on username
     * @param username The username to check
     * @return "Admin", "Volunteer", "VolunteerOrganization", "PersonInNeed", or null if not found
     */
    public String getUserType(String username) {
        String[] tables = {"Admin", "Volunteer", "VolunteerOrganization", "PersonInNeed"};
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            for (String table : tables) {
                String sql = "SELECT * FROM " + table + " WHERE username = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            return table;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Admin getAdmin(String username) {
        String sql = "SELECT * FROM Admin WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Admin admin = new Admin();
                    admin.setUsername(username);
                    return admin;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public VolunteerOrganization getVolunteerOrganization(String username) {
        String sql = "SELECT u.username, u.email, u.phone, u.address, " +
                     "o.organizationName, o.licenseNumber, o.field, o.representative, o.sponsor, o.info " +
                     "FROM SystemUser u JOIN VolunteerOrganization o ON u.username = o.username " +
                     "WHERE u.username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Volunteer getVolunteer(String username) {
        String sql = "SELECT u.username, u.email, u.phone, u.address, " +
                     "v.fullName, v.cccd, v.dateOfBirth, v.averageRating, v.ratingCount, v.freeHourPerWeek " +
                     "FROM SystemUser u JOIN Volunteer v ON u.username = v.username " +
                     "WHERE u.username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Volunteer volunteer = new Volunteer();
                        volunteer.setUsername(rs.getString("username"));
                        volunteer.setEmail(rs.getString("email"));
                        volunteer.setPhone(rs.getString("phone"));
                        volunteer.setAddress(rs.getString("address"));
                        volunteer.setFullName(rs.getString("fullName"));
                        volunteer.setCccd(rs.getString("cccd"));
                        String dateStr = rs.getString("dateOfBirth");
                        if (dateStr != null && !dateStr.isEmpty()) {
                            try {
                                java.sql.Date sqlDate = java.sql.Date.valueOf(dateStr);
                                volunteer.setDateOfBirth(new Date(sqlDate.getTime()));
                            } catch (IllegalArgumentException e) {
                                System.err.println("Error parsing date: " + e.getMessage());
                            }
                        }
                        volunteer.setAverageRating(rs.getDouble("averageRating"));
                        volunteer.setRatingCount(rs.getInt("ratingCount"));
                        volunteer.setFreeHourPerWeek(rs.getInt("freeHourPerWeek"));
                        volunteer.setSkills(getVolunteerSkills(username, conn));
                        return volunteer;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public PersonInNeed getPersonInNeed(String username) {
        String sql = "SELECT u.username, u.email, u.phone, u.address, p.fullName, p.cccd, p.dateOfBirth " +
                     "FROM SystemUser u JOIN PersonInNeed p ON u.username = p.username " +
                     "WHERE u.username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    PersonInNeed personInNeed = new PersonInNeed();
                    personInNeed.setUsername(rs.getString("username"));
                    personInNeed.setEmail(rs.getString("email"));
                    personInNeed.setPhone(rs.getString("phone"));
                    personInNeed.setAddress(rs.getString("address"));
                    personInNeed.setFullName(rs.getString("fullName"));
                    personInNeed.setCccd(rs.getString("cccd"));
                    String dateStr = rs.getString("dateOfBirth");
                    if (dateStr != null && !dateStr.isEmpty()) {
                        try {
                            java.sql.Date sqlDate = java.sql.Date.valueOf(dateStr);
                            personInNeed.setDateOfBirth(new Date(sqlDate.getTime()));
                        } catch (IllegalArgumentException e) {
                            System.err.println("Error parsing date: " + e.getMessage());
                        }
                    }
                    return personInNeed;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<String> getVolunteerSkills(String username, Connection conn) throws SQLException {
        List<String> skills = new ArrayList<>();
        String sql = "SELECT s.skill FROM Skills s " +
                     "JOIN VolunteerSkills vs ON s.skillId = vs.skillId " +
                     "WHERE vs.username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    skills.add(rs.getString("skill"));
                }
            }
        }
        return skills;
    }

    private int getOrCreateSkill(Connection conn, String skill) throws SQLException {
        String checkSkillSQL = "SELECT skillId FROM Skills WHERE skill = ?";
        try (PreparedStatement checkSkillStmt = conn.prepareStatement(checkSkillSQL)) {
            checkSkillStmt.setString(1, skill);
            try (ResultSet skillRs = checkSkillStmt.executeQuery()) {
                if (skillRs.next()) {
                    return skillRs.getInt("skillId");
                }
            }
        }

        String insertSkillSQL = "INSERT INTO Skills (skill) VALUES (?)";
        try (PreparedStatement insertSkillStmt = conn.prepareStatement(insertSkillSQL, Statement.RETURN_GENERATED_KEYS)) {
            insertSkillStmt.setString(1, skill);
            insertSkillStmt.executeUpdate();
            try (ResultSet generatedKeys = insertSkillStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating skill failed, no ID obtained.");
                }
            }
        }
    }

    // Methods for user authentication, creation, updates, and retrieval will be added here.
    // For example:
    // public SystemUser findUserByUsername(String username) { ... }
    // public boolean saveUser(SystemUser user) { ... }
    // public String getUserRole(String username) { ... }

    /**
     * A utility method to close database resources safely.
     * @param conn The database connection
     * @param stmt The statement
     * @param rs The result set
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
     * Get all users from the database.
     * This method retrieves all users from SystemUser table and then fetches
     * the specific user type object with all details.
     * @return List of all SystemUser objects, fully populated.
     */
    public List<SystemUser> getAllUsers() {
        List<SystemUser> users = new ArrayList<>();
        String query = "SELECT username FROM SystemUser";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String username = rs.getString("username");
                String userType = getUserType(username);

                if (userType == null) {
                    continue; // Should not happen in a consistent DB
                }
                
                SystemUser user = null;
                switch (userType) {
                    case "Admin":
                        user = getAdmin(username);
                        break;
                    case "Volunteer":
                        user = getVolunteer(username);
                        break;
                    case "PersonInNeed":
                        user = getPersonInNeed(username);
                        break;
                    case "VolunteerOrganization":
                        user = getVolunteerOrganization(username);
                        break;
                    default:
                        continue;
                }
                if (user != null) {
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error retrieving all users from database: " + e.getMessage());
        }

        return users;
    }

    public Map<String, Integer> getUserStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        String totalUsersQuery = "SELECT COUNT(*) FROM SystemUser";
        String volunteersQuery = "SELECT COUNT(*) FROM Volunteer";
        String peopleInNeedQuery = "SELECT COUNT(*) FROM PersonInNeed";
        String organizationsQuery = "SELECT COUNT(*) FROM VolunteerOrganization";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery(totalUsersQuery)) {
                if (rs.next()) {
                    stats.put("totalUsers", rs.getInt(1));
                }
            }
            try (ResultSet rs = stmt.executeQuery(volunteersQuery)) {
                if (rs.next()) {
                    stats.put("volunteers", rs.getInt(1));
                }
            }
            try (ResultSet rs = stmt.executeQuery(peopleInNeedQuery)) {
                if (rs.next()) {
                    stats.put("peopleInNeed", rs.getInt(1));
                }
            }
            try (ResultSet rs = stmt.executeQuery(organizationsQuery)) {
                if (rs.next()) {
                    stats.put("organizations", rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error fetching user statistics: " + e.getMessage());
        }
        return stats;
    }

    public List<Volunteer> getEventParticipants(int eventId) {
        List<Volunteer> participants = new ArrayList<>();
        String sql = "SELECT v.* FROM EventParticipants ep " +
                     "JOIN Volunteer v ON ep.username = v.username " +
                     "WHERE ep.eventId = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Volunteer volunteer = new Volunteer();
                    volunteer.setUsername(rs.getString("username"));
                    volunteer.setFullName(rs.getString("fullName"));
                    // Set other properties as needed
                    participants.add(volunteer);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return participants;
    }

    public boolean updateVolunteerRating(String volunteerUsername, int newScore) {
        String selectSql = "SELECT averageRating, ratingCount FROM Volunteer WHERE username = ?";
        String updateSql = "UPDATE Volunteer SET averageRating = ?, ratingCount = ? WHERE username = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement selectPstmt = conn.prepareStatement(selectSql)) {
            selectPstmt.setString(1, volunteerUsername);
            try (ResultSet rs = selectPstmt.executeQuery()) {
                if (rs.next()) {
                    double oldAvg = rs.getDouble("averageRating");
                    int oldCount = rs.getInt("ratingCount");
                    int newCount = oldCount + 1;
                    double newAvg = (oldCount == 0) ? newScore : (oldAvg * oldCount + newScore) / newCount;

                    try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                        updatePstmt.setDouble(1, newAvg);
                        updatePstmt.setInt(2, newCount);
                        updatePstmt.setString(3, volunteerUsername);
                        return updatePstmt.executeUpdate() > 0;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean updateEventParticipantDetails(int eventId, String volunteerUsername, Integer hours, Integer rating) {
        String sql = "UPDATE EventParticipants SET hoursParticipated = ?, ratingByOrg = ? WHERE eventId = ? AND username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (hours != null) pstmt.setInt(1, hours); else pstmt.setNull(1, Types.INTEGER);
            if (rating != null) pstmt.setInt(2, rating); else pstmt.setNull(2, Types.INTEGER);
            pstmt.setInt(3, eventId);
            pstmt.setString(4, volunteerUsername);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<EventParticipantDetails> getEventParticipationDetailsForVolunteer(String volunteerUsername) {
        List<EventParticipantDetails> participationDetailsList = new ArrayList<>();
        String sql = "SELECT e.*, ep.hoursParticipated, ep.ratingByOrg FROM Events e LEFT JOIN EventParticipants ep ON e.eventId = ep.eventId AND ep.username = ? JOIN Notification n ON e.eventId = n.eventId AND n.username = ? WHERE n.acceptStatus = 'Registered' OR n.acceptStatus = 'Attended' OR e.status = 'Done'";
        String volunteerFullName = getVolunteer(volunteerUsername) != null ? getVolunteer(volunteerUsername).getFullName() : "N/A";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, volunteerUsername);
            pstmt.setString(2, volunteerUsername);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Event event = new Event();
                event.setEventId(rs.getInt("eventId"));
                event.setTitle(rs.getString("title"));
                try {
                    String startDateStr = rs.getString("startDate");
                    if (startDateStr != null) event.setStartDate(new SimpleDateFormat("yyyy-MM-dd").parse(startDateStr));
                    String endDateStr = rs.getString("endDate");
                    if (endDateStr != null) event.setEndDate(new SimpleDateFormat("yyyy-MM-dd").parse(endDateStr));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                event.setStatus(rs.getString("status"));
                event.setOrganizer(rs.getString("organizer"));

                Integer hoursParticipated = rs.getObject("hoursParticipated") != null ? rs.getInt("hoursParticipated") : null;
                Integer ratingByOrg = rs.getObject("ratingByOrg") != null ? rs.getInt("ratingByOrg") : null;
                String volunteerParticipationStatus = getVolunteerEventParticipationStatus(conn, volunteerUsername, event.getEventId());

                EventParticipantDetails details = new EventParticipantDetails(event, volunteerUsername, volunteerFullName, hoursParticipated, ratingByOrg, volunteerParticipationStatus);
                details.setOrganizerName(getOrganizerNameById(conn, event.getOrganizer()));
                participationDetailsList.add(details);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return participationDetailsList;
    }

    private String getVolunteerEventParticipationStatus(Connection conn, String volunteerUsername, int eventId) throws SQLException {
        String sqlNotif = "SELECT acceptStatus FROM Notification WHERE username = ? AND eventId = ? ORDER BY notificationId DESC LIMIT 1";
        try (PreparedStatement pstmtNotif = conn.prepareStatement(sqlNotif)) {
            pstmtNotif.setString(1, volunteerUsername);
            pstmtNotif.setInt(2, eventId);
            try (ResultSet rsNotif = pstmtNotif.executeQuery()) {
                if (rsNotif.next()) return rsNotif.getString("acceptStatus");
            }
        }
        return "Registered"; // Default status
    }
    
    private String getOrganizerNameById(Connection conn, String organizerUsername) throws SQLException {
        String sqlOrg = "SELECT organizationName FROM VolunteerOrganization WHERE username = ?";
        try (PreparedStatement pstmtOrg = conn.prepareStatement(sqlOrg)) {
            pstmtOrg.setString(1, organizerUsername);
            try (ResultSet rsOrg = pstmtOrg.executeQuery()) {
                if (rsOrg.next()) return rsOrg.getString("organizationName");
            }
        }
        return "Unknown Organization";
    }
} 