package controller;

import entity.users.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserController {
    private final String DB_URL = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db";

    /**
     * Get all users from the database
     * @return List of all SystemUser objects
     */
    public List<SystemUser> getAllUsers() {
        List<SystemUser> users = new ArrayList<>();
        String query = "SELECT * FROM SystemUser";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String username = rs.getString("username");
                String password = rs.getString("password");
                String email = rs.getString("email");
                String phone = rs.getString("phone");
                String address = rs.getString("address");

                // We need to determine the role by checking other tables
                SystemUser user = null;

                // Check if user is Admin
                if (isUserInTable(username, "Admin")) {
                    user = new Admin();
                }
                // Check if user is Volunteer
                else if (isUserInTable(username, "Volunteer")) {
                    user = new Volunteer();
                }
                // Check if user is PersonInNeed
                else if (isUserInTable(username, "PersonInNeed")) {
                    user = new PersonInNeed();
                }
                // Check if user is VolunteerOrganization
                else if (isUserInTable(username, "VolunteerOrganization")) {
                    user = new VolunteerOrganization();
                }
                // Default case - create a basic SystemUser (this shouldn't normally happen)
                else {
                    continue; // Skip users that don't have a role
                }

                user.setUsername(username);
                user.setPassword(password);
                user.setEmail(email);
                user.setPhone(phone);
                user.setAddress(address);
                users.add(user);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error retrieving users from database: " + e.getMessage());
        }

        return users;
    }

    /**
     * Helper method to check if a user exists in a specific role table
     * @param username The username to check
     * @param tableName The role table to check (Admin, Volunteer, PersonInNeed, or VolunteerOrganization)
     * @return True if the user exists in the specified table
     */
    private boolean isUserInTable(String username, String tableName) {
        String query = "SELECT username FROM " + tableName + " WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            return rs.next(); // If there's a result, the user exists in this table
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get users filtered by role
     * @param role The role to filter by
     * @return List of SystemUser objects with the specified role
     */
    public List<SystemUser> getUsersByRole(String role) {
        if (role.equals("All")) {
            return getAllUsers();
        }

        List<SystemUser> users = new ArrayList<>();
        String tableName;

        // Determine the table name based on role
        switch (role) {
            case "Admin":
            case "Volunteer":
            case "PersonInNeed":
            case "VolunteerOrganization":
                tableName = role;
                break;
            default:
                return users; // Return empty list for invalid roles
        }

        // Query to join the role table with SystemUser table
        String query = "SELECT u.* FROM SystemUser u " +
                       "JOIN " + tableName + " r ON u.username = r.username";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String username = rs.getString("username");
                String password = rs.getString("password");
                String email = rs.getString("email");
                String phone = rs.getString("phone");
                String address = rs.getString("address");

                SystemUser user;
                switch (role) {
                    case "Admin":
                        user = new Admin();
                        break;
                    case "Volunteer":
                        user = new Volunteer();
                        break;
                    case "PersonInNeed":
                        user = new PersonInNeed();
                        break;
                    case "VolunteerOrganization":
                        user = new VolunteerOrganization();
                        break;
                    default:
                        continue; // Skip unknown roles
                }

                user.setUsername(username);
                user.setPassword(password);
                user.setEmail(email);
                user.setPhone(phone);
                user.setAddress(address);
                users.add(user);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error retrieving users by role: " + e.getMessage());
        }

        return users;
    }

    /**
     * Get the role of a user by username
     * @param username The username to look up
     * @return String representing the user's role
     */
    public String getUserRole(String username) {
        // Check each role table
        if (isUserInTable(username, "Admin")) {
            return "Admin";
        } else if (isUserInTable(username, "Volunteer")) {
            return "Volunteer";
        } else if (isUserInTable(username, "PersonInNeed")) {
            return "PersonInNeed";
        } else if (isUserInTable(username, "VolunteerOrganization")) {
            return "VolunteerOrganization";
        }

        return null; // User doesn't have a specific role
    }
}
