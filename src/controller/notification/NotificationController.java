package controller.notification;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet; // Cần cho việc kiểm tra


public class NotificationController {
    private static final String DB_URL = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db"; // Đảm bảo tên file DB đúng

    /**
     * Tạo một thông báo đăng ký tham gia sự kiện cho Volunteer.
     * @param eventId ID của sự kiện
     * @param volunteerUsername Username của Volunteer
     * @return true nếu tạo thành công, false nếu không.
     */
    public boolean createRegistrationNotification(int eventId, String volunteerUsername) {
        // Kiểm tra xem đã có notification "Pending" hoặc "Registered" chưa để tránh tạo trùng
        if (isVolunteerPendingOrRegistered(volunteerUsername, eventId)) {
            System.out.println("Volunteer " + volunteerUsername + " already has a pending/registered notification for event " + eventId);
            return false; // Hoặc true nếu bạn coi đây là "đã đăng ký"
        }

        String sql = "INSERT INTO Notification (eventId, username, acceptStatus) VALUES (?, ?, ?)";
        // acceptStatus mặc định là 'Pending' theo schema, hoặc bạn có thể đặt là 'Applied_By_Volunteer'
        String defaultStatus = "Pending"; 

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, eventId);
            pstmt.setString(2, volunteerUsername);
            pstmt.setString(3, defaultStatus);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error creating registration notification: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Lấy trạng thái acceptStatus của notification gần nhất của một Volunteer cho một Event.
     * @param volunteerUsername
     * @param eventId
     * @return Chuỗi trạng thái (ví dụ: "Pending", "Approved", "Rejected", "Canceled") hoặc null nếu không có notification.
     * @throws SQLException
     */
    public String getVolunteerNotificationStatusForEvent(String volunteerUsername, int eventId) throws SQLException {
        String sql = "SELECT acceptStatus FROM Notification " +
                     "WHERE username = ? AND eventId = ? " +
                     "ORDER BY notificationId DESC LIMIT 1"; // Lấy notification mới nhất

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, volunteerUsername);
            pstmt.setInt(2, eventId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("acceptStatus");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting volunteer notification status: " + e.getMessage());
            throw e;
        }
        return null; // Không tìm thấy notification
    }

    /**
     * Kiểm tra xem Volunteer đã có thông báo đang chờ (Pending) hoặc đã được chấp nhận (Registered)
     * cho một sự kiện cụ thể hay chưa.
     * @param volunteerUsername
     * @param eventId
     * @return true nếu có, false nếu không.
     */
    public boolean isVolunteerPendingOrRegistered(String volunteerUsername, int eventId) {
        String sql = "SELECT COUNT(*) FROM Notification WHERE username = ? AND eventId = ? AND (acceptStatus = 'Pending' OR acceptStatus = 'Registered' OR acceptStatus = 'Approved')"; // Thêm 'Approved' nếu tổ chức dùng trạng thái này
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, volunteerUsername);
            pstmt.setInt(2, eventId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking existing notification: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    // Các phương thức khác liên quan đến Notification có thể được thêm vào đây
}