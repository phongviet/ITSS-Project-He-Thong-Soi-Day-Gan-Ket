package dao;

import entity.requests.HelpRequest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import utils.AppConstants;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * HelpRequestDAO class handles all database operations related to help requests.
 */
public class HelpRequestDAO {

    private static final String DB_URL = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db";

    public Map<String, Object> getHelpRequestStatistics() {
        Map<String, Object> stats = new HashMap<>();
        String totalQuery = "SELECT COUNT(*) FROM HelpRequest";
        String pendingQuery = "SELECT COUNT(*) FROM HelpRequest WHERE status = ?";
        String approvedQuery = "SELECT COUNT(*) FROM HelpRequest WHERE status = ?";
        String rejectedQuery = "SELECT COUNT(*) FROM HelpRequest WHERE status = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(totalQuery)) {
                if (rs.next()) {
                    stats.put("totalRequests", rs.getInt(1));
                }
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement(pendingQuery)) {
                pstmt.setString(1, AppConstants.REQUEST_PENDING);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        stats.put("pendingRequests", rs.getInt(1));
                    }
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(approvedQuery)) {
                pstmt.setString(1, AppConstants.REQUEST_APPROVED);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        stats.put("approvedRequests", rs.getInt(1));
                    }
                }
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement(rejectedQuery)) {
                pstmt.setString(1, AppConstants.REQUEST_REJECTED);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        stats.put("rejectedRequests", rs.getInt(1));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error fetching help request statistics: " + e.getMessage());
        }
        return stats;
    }

    public List<HelpRequest> getAllHelpRequests() {
        List<HelpRequest> helpRequests = new ArrayList<>();
        String sql = "SELECT * FROM HelpRequest";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                HelpRequest request = new HelpRequest();
                request.setRequestId(rs.getInt("requestId"));
                request.setTitle(rs.getString("title"));
                try {
                    String dateStr = rs.getString("startDate");
                    if (dateStr != null) {
                        request.setStartDate(new SimpleDateFormat("yyyy-MM-dd").parse(dateStr));
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                request.setEmergencyLevel(rs.getString("emergencyLevel"));
                request.setDescription(rs.getString("description"));
                request.setPersonInNeedUsername(rs.getString("personInNeedId"));
                request.setStatus(rs.getString("status"));
                request.setContact(rs.getString("contact"));
                request.setAddress(rs.getString("address"));
                helpRequests.add(request);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return helpRequests;
    }

    public List<HelpRequest> getHelpRequestsByUsername(String personInNeedUsername) {
        List<HelpRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM HelpRequest WHERE personInNeedId = ?"; 
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, personInNeedUsername);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                HelpRequest req = new HelpRequest();
                req.setRequestId(rs.getInt("requestId"));
                req.setTitle(rs.getString("title"));
                req.setDescription(rs.getString("description"));
                req.setContact(rs.getString("contact"));
                req.setPersonInNeedUsername(rs.getString("personInNeedId"));
                try {
                    String dateStr = rs.getString("startDate");
                    if(dateStr != null) {
                        req.setStartDate(new SimpleDateFormat("yyyy-MM-dd").parse(dateStr));
                    }
                } catch (ParseException e) {
                     e.printStackTrace();
                }
                req.setEmergencyLevel(rs.getString("emergencyLevel"));
                req.setStatus(rs.getString("status"));
                req.setAddress(rs.getString("address"));

                list.add(req);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving help requests: " + e.getMessage());
        }
        return list;
    }

    public List<HelpRequest> getApprovedHelpRequests() {
        List<HelpRequest> helpRequests = new ArrayList<>();
        String sql = "SELECT * FROM HelpRequest WHERE status = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, AppConstants.REQUEST_APPROVED);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                HelpRequest hr = new HelpRequest();
                hr.setRequestId(rs.getInt("requestId"));
                hr.setTitle(rs.getString("title"));
                try {
                    String startDateStr = rs.getString("startDate");
                    if (startDateStr != null) {
                        hr.setStartDate(new SimpleDateFormat("yyyy-MM-dd").parse(startDateStr));
                    }
                } catch (ParseException e) {
                     e.printStackTrace();
                }
                hr.setEmergencyLevel(rs.getString("emergencyLevel"));
                hr.setDescription(rs.getString("description"));
                hr.setPersonInNeedUsername(rs.getString("personInNeedId"));
                hr.setStatus(rs.getString("status"));
                helpRequests.add(hr);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return helpRequests;
    }

    public boolean createHelpRequest(HelpRequest request) {
        String sql = "INSERT INTO HelpRequest (title, description, contact, personInNeedId, startDate, emergencyLevel, status, address) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, request.getTitle());
            pstmt.setString(2, request.getDescription());
            pstmt.setString(3, request.getContact());
            pstmt.setString(4, request.getPersonInNeedUsername());
            
            if (request.getStartDate() != null) {
                pstmt.setString(5, new SimpleDateFormat("yyyy-MM-dd").format(request.getStartDate()));
            } else {
                pstmt.setNull(5, java.sql.Types.VARCHAR);
            }
            pstmt.setString(6, request.getEmergencyLevel());
            pstmt.setString(7, request.getStatus() != null ? request.getStatus() : AppConstants.REQUEST_PENDING);
            pstmt.setString(8, request.getAddress());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error creating help request: " + e.getMessage());
            return false;
        }
    }

    public boolean updateHelpRequest(HelpRequest request) {
        String sql = "UPDATE HelpRequest SET title = ?, description = ?, contact = ?, startDate = ?, emergencyLevel = ?, address = ? WHERE requestId = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, request.getTitle());
            pstmt.setString(2, request.getDescription());
            pstmt.setString(3, request.getContact());
            if (request.getStartDate() != null) {
                pstmt.setString(4, new SimpleDateFormat("yyyy-MM-dd").format(request.getStartDate()));
            } else {
                pstmt.setNull(4, java.sql.Types.VARCHAR);
            }
            pstmt.setString(5, request.getEmergencyLevel());
            pstmt.setString(6, request.getAddress());
            pstmt.setInt(7, request.getRequestId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error updating help request: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteHelpRequest(int requestId) {
        String sql = "DELETE FROM HelpRequest WHERE requestId = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, requestId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting help request: " + e.getMessage());
            return false;
        }
    }

    public boolean updateHelpRequestStatus(int requestId, String status) {
        String sql = "UPDATE HelpRequest SET status = ? WHERE requestId = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, requestId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Package-private for use within the DAO layer transaction
    boolean updateHelpRequestStatusInTransaction(Connection conn, int requestId, String status) throws SQLException {
        String sql = "UPDATE HelpRequest SET status = ? WHERE requestId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, requestId);
            return pstmt.executeUpdate() > 0;
        }
    }

}