package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import views.screen.AdminScreen.StatisticReportScreenHandler.EventStatRow;
import views.screen.AdminScreen.StatisticReportScreenHandler.RequestStatRow;
import views.screen.AdminScreen.StatisticReportScreenHandler.UserStatRow;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminStatisticReportController {

    private static final Logger LOGGER = Logger.getLogger(AdminStatisticReportController.class.getName());
    private static final String DB_URL = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db";

    /**
     * Gets statistics about system users
     * @return A map containing various user statistics
     */
    public Map<String, Integer> getUserStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(DB_URL);

            // Total users
            String totalUsersQuery = "SELECT COUNT(*) FROM SystemUser";
            pstmt = conn.prepareStatement(totalUsersQuery);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                stats.put("totalUsers", rs.getInt(1));
            }
            rs.close();
            pstmt.close();

            // Total volunteers
            String volunteersQuery = "SELECT COUNT(*) FROM Volunteer";
            pstmt = conn.prepareStatement(volunteersQuery);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                stats.put("volunteers", rs.getInt(1));
            }
            rs.close();
            pstmt.close();

            // Total people in need
            String peopleInNeedQuery = "SELECT COUNT(*) FROM PersonInNeed";
            pstmt = conn.prepareStatement(peopleInNeedQuery);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                stats.put("peopleInNeed", rs.getInt(1));
            }
            rs.close();
            pstmt.close();

            // Total organizations
            String organizationsQuery = "SELECT COUNT(*) FROM VolunteerOrganization";
            pstmt = conn.prepareStatement(organizationsQuery);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                stats.put("organizations", rs.getInt(1));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching user statistics", e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return stats;
    }

    /**
     * Gets user activity statistics for the table display
     * @return List of user statistics by type
     */
    public ObservableList<UserStatRow> getUserActivityStatistics() {
        ObservableList<UserStatRow> data = FXCollections.observableArrayList();
        Map<String, Integer> stats = getUserStatistics();

        // Create table rows from the statistics map with simplified data
        data.add(new UserStatRow(
            "Tình nguyện viên",
            stats.getOrDefault("volunteers", 0),
            0 // Last month new volunteers removed
        ));

        data.add(new UserStatRow(
            "Người cần trợ giúp",
            stats.getOrDefault("peopleInNeed", 0),
            0 // Last month new people in need removed
        ));

        data.add(new UserStatRow(
            "Tổ chức tình nguyện",
            stats.getOrDefault("organizations", 0),
            0 // Last month new organizations removed
        ));

        return data;
    }

    /**
     * Gets statistics about events in the system
     * @return A map containing various event statistics
     */
    public Map<String, Object> getEventStatistics() {
        Map<String, Object> stats = new HashMap<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(DB_URL);

            // Total events
            String totalEventsQuery = "SELECT COUNT(*) FROM Events";
            pstmt = conn.prepareStatement(totalEventsQuery);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                stats.put("totalEvents", rs.getInt(1));
            }
            rs.close();
            pstmt.close();

            // Upcoming events (startDate > today)
            String upcomingEventsQuery = "SELECT COUNT(*) FROM Events WHERE startDate > date('now')";
            pstmt = conn.prepareStatement(upcomingEventsQuery);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                stats.put("upcomingEvents", rs.getInt(1));
            }
            rs.close();
            pstmt.close();

            // Ongoing events (startDate <= today AND endDate >= today)
            String ongoingEventsQuery = "SELECT COUNT(*) FROM Events WHERE startDate <= date('now') AND endDate >= date('now')";
            pstmt = conn.prepareStatement(ongoingEventsQuery);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                stats.put("ongoingEvents", rs.getInt(1));
            }
            rs.close();
            pstmt.close();

            // Past events (endDate < today)
            String pastEventsQuery = "SELECT COUNT(*) FROM Events WHERE endDate < date('now')";
            pstmt = conn.prepareStatement(pastEventsQuery);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                stats.put("pastEvents", rs.getInt(1));
            }
            rs.close();
            pstmt.close();

            // Monthly distribution (last 6 months)
            Map<String, Integer> monthlyEvents = new HashMap<>();

            // For simplicity, using fixed date ranges for 6 months of 2025
            String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun"};
            String[] dateFilters = {
                "startDate BETWEEN '2025-01-01' AND '2025-01-31'",
                "startDate BETWEEN '2025-02-01' AND '2025-02-28'",
                "startDate BETWEEN '2025-03-01' AND '2025-03-31'",
                "startDate BETWEEN '2025-04-01' AND '2025-04-30'",
                "startDate BETWEEN '2025-05-01' AND '2025-05-31'",
                "startDate BETWEEN '2025-06-01' AND '2025-06-30'"
            };

            for (int i = 0; i < months.length; i++) {
                String monthlyQuery = "SELECT COUNT(*) FROM Events WHERE " + dateFilters[i];
                pstmt = conn.prepareStatement(monthlyQuery);
                rs = pstmt.executeQuery();

                if (rs.next()) {
                    monthlyEvents.put(months[i], rs.getInt(1));
                }
                rs.close();
                pstmt.close();
            }

            stats.put("monthlyEvents", monthlyEvents);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching event statistics", e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return stats;
    }

    /**
     * Gets statistics about event types
     * @return List of event statistics by type
     */
    public ObservableList<EventStatRow> getEventTypeStatistics() {
        ObservableList<EventStatRow> data = FXCollections.observableArrayList();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(DB_URL);

            // Group events by title keywords to determine types
            String query = "SELECT " +
                    "CASE " +
                    "  WHEN title LIKE '%Dạy%' OR title LIKE '%Giáo dục%' THEN 'Giáo dục' " +
                    "  WHEN title LIKE '%Chăm sóc%' OR title LIKE '%Hỗ trợ%' THEN 'Hỗ trợ' " +
                    "  WHEN title LIKE '%Quyên góp%' OR title LIKE '%Từ thiện%' THEN 'Từ thiện' " +
                    "  WHEN title LIKE '%Y tế%' OR title LIKE '%Sức khỏe%' THEN 'Y tế' " +
                    "  ELSE 'Khác' " +
                    "END as eventType, " +
                    "COUNT(*) as count, " +
                    "AVG(maxParticipantNumber) as avgParticipants " +
                    "FROM Events " +
                    "GROUP BY eventType " +
                    "ORDER BY count DESC";

            pstmt = conn.prepareStatement(query);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                String eventType = rs.getString("eventType");
                int count = rs.getInt("count");
                double avgParticipants = rs.getDouble("avgParticipants");

                data.add(new EventStatRow(eventType, count, avgParticipants));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching event type statistics", e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        // If no data was found, add some placeholder categories
        if (data.isEmpty()) {
            data.add(new EventStatRow("Giáo dục", 0, 0));
            data.add(new EventStatRow("Hỗ trợ", 0, 0));
            data.add(new EventStatRow("Từ thiện", 0, 0));
            data.add(new EventStatRow("Y tế", 0, 0));
            data.add(new EventStatRow("Khác", 0, 0));
        }

        return data;
    }

    /**
     * Gets statistics about help requests
     * @return A map containing various help request statistics
     */
    public Map<String, Object> getHelpRequestStatistics() {
        Map<String, Object> stats = new HashMap<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(DB_URL);

            // Total requests
            String totalRequestsQuery = "SELECT COUNT(*) FROM HelpRequest";
            pstmt = conn.prepareStatement(totalRequestsQuery);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                stats.put("totalRequests", rs.getInt(1));
            }
            rs.close();
            pstmt.close();

            // Pending requests
            String pendingRequestsQuery = "SELECT COUNT(*) FROM HelpRequest WHERE status = 'Pending'";
            pstmt = conn.prepareStatement(pendingRequestsQuery);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                stats.put("pendingRequests", rs.getInt(1));
            }
            rs.close();
            pstmt.close();

            // Approved requests
            String approvedRequestsQuery = "SELECT COUNT(*) FROM HelpRequest WHERE status = 'Approved'";
            pstmt = conn.prepareStatement(approvedRequestsQuery);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                stats.put("approvedRequests", rs.getInt(1));
            }
            rs.close();
            pstmt.close();

            // Rejected requests
            String rejectedRequestsQuery = "SELECT COUNT(*) FROM HelpRequest WHERE status != 'Pending' AND status != 'Approved'";
            pstmt = conn.prepareStatement(rejectedRequestsQuery);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                stats.put("rejectedRequests", rs.getInt(1));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching help request statistics", e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return stats;
    }

    /**
     * Gets statistics about help request types
     * @return List of help request statistics by type
     */
    public ObservableList<RequestStatRow> getRequestTypeStatistics() {
        ObservableList<RequestStatRow> data = FXCollections.observableArrayList();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(DB_URL);

            // Group help requests by title keywords to determine types
            String query = "SELECT " +
                    "CASE " +
                    "  WHEN title LIKE '%Tài chính%' OR title LIKE '%Hỗ trợ tài chính%' THEN 'Hỗ trợ tài chính' " +
                    "  WHEN title LIKE '%Thực phẩm%' OR title LIKE '%Lương thực%' THEN 'Hỗ trợ thực phẩm' " +
                    "  WHEN title LIKE '%Y tế%' OR title LIKE '%Khám bệnh%' OR title LIKE '%Chăm sóc%' THEN 'Hỗ trợ y tế' " +
                    "  WHEN title LIKE '%Nhà ở%' OR title LIKE '%Chỗ ở%' THEN 'Hỗ trợ nhà ở' " +
                    "  WHEN title LIKE '%Giáo dục%' OR title LIKE '%Học tập%' OR title LIKE '%Dạy%' THEN 'Hỗ trợ giáo dục' " +
                    "  ELSE 'Hỗ trợ khác' " +
                    "END as requestType, " +
                    "COUNT(*) as count " +
                    "FROM HelpRequest " +
                    "GROUP BY requestType " +
                    "ORDER BY count DESC";

            pstmt = conn.prepareStatement(query);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                String requestType = rs.getString("requestType");
                int count = rs.getInt("count");

                data.add(new RequestStatRow(requestType, count));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching help request type statistics", e);
        } finally {
            closeResources(conn, pstmt, rs);
        }

        // If no data was found, add some placeholder categories
        if (data.isEmpty()) {
            data.add(new RequestStatRow("Hỗ trợ tài chính", 0));
            data.add(new RequestStatRow("Hỗ trợ thực phẩm", 0));
            data.add(new RequestStatRow("Hỗ trợ y tế", 0));
            data.add(new RequestStatRow("Hỗ trợ nhà ở", 0));
            data.add(new RequestStatRow("Hỗ trợ giáo dục", 0));
        }

        return data;
    }

    /**
     * Closes database resources safely
     */
    private void closeResources(Connection connection, PreparedStatement statement, ResultSet resultSet) {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error closing ResultSet", e);
        }

        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error closing PreparedStatement", e);
        }

        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error closing Connection", e);
        }
    }
}
