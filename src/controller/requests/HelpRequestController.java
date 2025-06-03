package controller.requests;

import entity.requests.HelpRequest;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class HelpRequestController {

    private static final String DB_URL = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db";
    // Date format consistent with EventController, assuming dates might be stored as strings
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd"); 

    // 1. Tạo yêu cầu trợ giúp
    public static boolean createHelpRequest(HelpRequest request) {
        // requestId is now AUTOINCREMENT, so we omit it from the INSERT statement.
        String sql = "INSERT INTO HelpRequest (title, description, contact, personInNeedId, startDate, emergencyLevel, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, request.getTitle());
            pstmt.setString(2, request.getDescription());
            pstmt.setString(3, request.getContact());
            pstmt.setString(4, request.getPersonInNeedId()); // This is the username
            
            if (request.getStartDate() != null) {
                pstmt.setString(5, DATE_FORMAT.format(request.getStartDate()));
            } else {
                pstmt.setNull(5, Types.VARCHAR);
            }
            pstmt.setString(6, request.getEmergencyLevel());
            pstmt.setString(7, request.getStatus() != null ? request.getStatus() : "Pending");

            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error creating help request: " + e.getMessage());
            return false;
        }
    }

    // 2. Lấy danh sách yêu cầu theo ID người cần trợ giúp
    public static List<HelpRequest> getHelpRequestsByUserId(String personInNeedId) {
        List<HelpRequest> list = new ArrayList<>();
        // Assuming all necessary fields are selected by SELECT *
        String sql = "SELECT * FROM HelpRequest WHERE personInNeedId = ?"; 
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, personInNeedId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                // Debugging: Print the raw status for the specific request ID
                if (rs.getInt("requestId") == 2) {
                    System.out.println("DEBUG: For requestId=2, status from DB is: '" + rs.getString("status") + "'");
                }

                Date startDate = null;
                String startDateStr = rs.getString("startDate");
                if (startDateStr != null && !startDateStr.isEmpty()) {
                    try {
                        startDate = DATE_FORMAT.parse(startDateStr);
                    } catch (ParseException ex) {
                        System.err.println("Cannot parse startDate from DB: " + startDateStr + " for requestId: " + rs.getInt("requestId"));
                    }
                }

                HelpRequest req = new HelpRequest(
                        rs.getInt("requestId"), // Changed from id
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("contact"),
                        rs.getString("personInNeedId"),
                        startDate, // Added
                        rs.getString("emergencyLevel"), // Added
                        rs.getString("status") // Changed from isFulfilled (boolean)
                );
                list.add(req);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving help requests: " + e.getMessage());
        }
        return list;
    }

    // 3. Chỉnh sửa yêu cầu
    public static boolean updateHelpRequest(HelpRequest request) {
        // Assuming only these fields are updatable by this specific method.
        // Status, startDate, emergencyLevel could also be made updatable if needed.
        String sql = "UPDATE HelpRequest SET title = ?, description = ?, contact = ? WHERE requestId = ?"; // Changed id to requestId
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, request.getTitle());
            pstmt.setString(2, request.getDescription());
            pstmt.setString(3, request.getContact());
            pstmt.setInt(4, request.getRequestId()); // Changed from getId()

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println("Error updating help request: " + e.getMessage());
            return false;
        }
    }

    // 4. Xoá yêu cầu
    public static boolean deleteHelpRequest(int requestId) {
        String sql = "DELETE FROM HelpRequest WHERE requestId = ?"; // Changed id to requestId
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, requestId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting help request: " + e.getMessage());
            return false;
        }
    }

    // 5. Xác nhận đã được giúp đỡ (e.g., set status to "Fulfilled")
    public static boolean markAsFulfilled(int requestId) {
        String sql = "UPDATE HelpRequest SET status = ? WHERE requestId = ?"; // Changed isFulfilled and id
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "Fulfilled"); // Example status for fulfilled
            pstmt.setInt(2, requestId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println("Error marking request as fulfilled: " + e.getMessage());
            return false;
        }
    }

    // 6. Generic method to update the status of a HelpRequest
    public static boolean updateHelpRequestStatus(int requestId, String newStatus) {
        String sql = "UPDATE HelpRequest SET status = ? WHERE requestId = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, requestId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("HelpRequest ID: " + requestId + " status updated to: " + newStatus);
            }
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println("Error updating help request status for ID " + requestId + " to " + newStatus + ": " + e.getMessage());
            return false;
        }
    }
}
