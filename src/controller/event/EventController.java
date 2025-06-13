package controller.event;

import dao.HelpRequestDAO;
import dao.NotificationDAO;
import dao.ReportDAO;
import dao.UserDAO;
import entity.events.*;
import entity.users.VolunteerOrganization;
import entity.users.Volunteer;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import entity.requests.HelpRequest;
import java.text.ParseException;
import entity.events.Event;
import entity.reports.Report;
import java.sql.Statement;
import java.sql.Types;
import utils.AppConstants;


public class EventController {

    private static final String DB_URL = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    private HelpRequestDAO helpRequestDAO;
    private UserDAO userDAO;
    private NotificationDAO notificationDAO;
    private ReportDAO reportDAO;

    public EventController() {
        this.helpRequestDAO = new HelpRequestDAO();
        this.userDAO = new UserDAO();
        this.notificationDAO = new NotificationDAO();
        this.reportDAO = new ReportDAO();
    }

    public List<HelpRequest> getApprovedHelpRequests() {
        return helpRequestDAO.getApprovedHelpRequests();
    }
    public boolean updateHelpRequestStatus(int requestId, String newStatus) {
        return helpRequestDAO.updateHelpRequestStatus(requestId, newStatus);
    }
    
    // This method is complex, joining multiple tables. For now, it's left in EventController
    // as it orchestrates data from Events, Notifications, and Volunteers.
    // A future refactor could move this to a dedicated service layer or a specialized DAO method.
    public List<EventParticipantDetails> getParticipantDetailsForEvent(int eventId) {
        List<EventParticipantDetails> result = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        // Updated SQL to join with Volunteer table and fetch fullName
        String sql = ""
            + "SELECT e.eventId, e.title AS eventTitle, e.startDate, e.endDate, e.status AS eventStatus, "
            + "       ep.username AS volunteerUsername, v.fullName AS volunteerFullName, ep.hoursParticipated, ep.ratingByOrg, "
            + "       n.acceptStatus AS volunteerParticipationStatus "
            + "FROM Events e "
            + "JOIN Notification n ON e.eventId = n.eventId "
            + "JOIN EventParticipants ep ON e.eventId = ep.eventId AND ep.username = n.username "
            + "JOIN Volunteer v ON ep.username = v.username " // Join with Volunteer table
            + "WHERE e.eventId = ? "
            + "  AND n.acceptStatus = ?"; // Assuming 'Registered' means they participated

        try {
            conn = DriverManager.getConnection(DB_URL);
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, eventId);
            pstmt.setString(2, AppConstants.NOTIF_REGISTERED);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                EventParticipantDetails dto = new EventParticipantDetails();

                // --- Lấy info từ Event ---
                dto.setEventId(rs.getInt("eventId"));
                dto.setTitle(rs.getString("eventTitle")); // Use alias for clarity

                String startDateStr = rs.getString("startDate");
                if (startDateStr != null) {
                    dto.setStartDate(java.sql.Date.valueOf(startDateStr));
                }
                String endDateStr = rs.getString("endDate");
                if (endDateStr != null) {
                    dto.setEndDate(java.sql.Date.valueOf(endDateStr));
                }

                dto.setEventStatus(rs.getString("eventStatus"));

                // --- Lấy info từ Volunteer và EventParticipants ---
                dto.setVolunteerUsername(rs.getString("volunteerUsername"));
                dto.setVolunteerFullName(rs.getString("volunteerFullName")); // Get fullName

                int hp = rs.getInt("hoursParticipated");
                if (!rs.wasNull()) {
                    dto.setHoursParticipated(hp);
                }

                int rbo = rs.getInt("ratingByOrg");
                if (!rs.wasNull()) {
                    dto.setRatingByOrg(rbo);
                }

                dto.setVolunteerParticipationStatus(rs.getString("volunteerParticipationStatus"));

                result.add(dto);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null)    rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null)  conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Cập nhật lại hoursParticipated và ratingByOrg cho một volunteer (username) trong EventParticipants.
     * Trả về true nếu update thành công.
     */
    public boolean updateEventParticipantDetails(int eventId, String volunteerUsername,
                                                 Integer hoursParticipated, Integer ratingByOrg) {
        return userDAO.updateEventParticipantDetails(eventId, volunteerUsername, hoursParticipated, ratingByOrg);
    }
    public boolean registerEvent(Event event, VolunteerOrganization organization) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DriverManager.getConnection(DB_URL);

            // Start a transaction
            conn.setAutoCommit(false);

            // Set the initial status to "pending"
            event.setStatus(AppConstants.EVENT_PENDING);

            // Format dates for SQLite
            String startDateStr = null;
            String endDateStr = null;

            if (event.getStartDate() != null) {
                startDateStr = DATE_FORMAT.format(event.getStartDate());
            }

            if (event.getEndDate() != null) {
                endDateStr = DATE_FORMAT.format(event.getEndDate());
            }

            // Insert into Events table using startDate and endDate columns
            String insertEventSQL = "INSERT INTO Events (title, maxParticipantNumber, startDate, endDate, " +
                            "emergencyLevel, description, organizer, status, requestId) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            pstmt = conn.prepareStatement(insertEventSQL, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, event.getTitle());
            pstmt.setInt(2, event.getMaxParticipantNumber());
            pstmt.setString(3, startDateStr);
            pstmt.setString(4, endDateStr);
            pstmt.setString(5, event.getEmergencyLevel());
            pstmt.setString(6, event.getDescription());
            pstmt.setString(7, organization.getUsername());
            pstmt.setString(8, event.getStatus());
            pstmt.setString(9, event.getRequestId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating event failed, no rows affected.");
            }

            // Get the generated event ID
            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int eventId = generatedKeys.getInt(1);
                event.setEventId(eventId);

                // Insert required skills
                if (event.getRequiredSkills() != null && !event.getRequiredSkills().isEmpty()) {
                    // Lấy ID của kỹ năng từ bảng Skills
                    for (String skillName : event.getRequiredSkills()) {
                        int skillId = getSkillIdByName(conn, skillName);
                        if (skillId > 0) {
                            insertEventSkill(conn, eventId, skillId);
                        }
                    }
                }
            } else {
                throw new SQLException("Creating event failed, no ID obtained.");
            }

            // Commit the transaction
            conn.commit();
            return true;

        } catch (SQLException e) {
            // If there is an error, rollback the transaction
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;

        } finally {
            // Close resources
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Phương thức trợ giúp để lấy skillId dựa vào tên kỹ năng
     */
    private int getSkillIdByName(Connection conn, String skillName) throws SQLException {
        String sql = "SELECT skillId FROM Skills WHERE skill = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, skillName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("skillId");
                }
            }
        }
        return -1; // Không tìm thấy
    }

    /**
     * Phương thức trợ giúp để thêm kỹ năng cho event
     */
    private void insertEventSkill(Connection conn, int eventId, int skillId) throws SQLException {
        String sql = "INSERT INTO EventSkills (eventId, skillId) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            pstmt.setInt(2, skillId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Retrieves all events created by a specific organization
     *
     * @param organizerId The ID (username) of the organization
     * @return List of events organized by the specified organization
     */
    public List<Event> getEventsByOrganizerId(String organizerId) {
        List<Event> events = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(DB_URL);
            String sql = "SELECT * FROM Events WHERE organizer = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, organizerId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Event event = new Event();
                int eventId = rs.getInt("eventId");
                event.setEventId(eventId);
                event.setTitle(rs.getString("title"));
                event.setMaxParticipantNumber(rs.getInt("maxParticipantNumber"));

                // Lấy ngày từ cột DATE trong DB
                try {
                    String startDateStr = rs.getString("startDate");
                    String endDateStr = rs.getString("endDate");

                    if (startDateStr != null && !startDateStr.isEmpty()) {
                        try {
                            // Parse từ chuỗi định dạng YYYY-MM-DD
                            event.setStartDate(DATE_FORMAT.parse(startDateStr));
                        } catch (Exception ex) {
                            System.err.println("Cannot convert start date: " + startDateStr);
                        }
                    }

                    if (endDateStr != null && !endDateStr.isEmpty()) {
                        try {
                            // Parse từ chuỗi định dạng YYYY-MM-DD
                            event.setEndDate(DATE_FORMAT.parse(endDateStr));
                        } catch (Exception ex) {
                            System.err.println("Unable to convert end date: " + endDateStr);
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Error reading date:" + e.getMessage());
                }

                event.setEmergencyLevel(rs.getString("emergencyLevel"));
                event.setDescription(rs.getString("description"));
                event.setOrganizer(rs.getString("organizer"));
                event.setRequestId(rs.getString("RequestId"));

                // Get status from database, default to "pending" if null
                String status = rs.getString("status");
                event.setStatus(status != null ? status : "Pending");

                // Load required skills for this event
                loadEventSkills(conn, event);

                events.add(event);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return events;
    }

    public List<Event> getEventsByStatusForOrganizer(String organizerId, List<String> statuses) {
        List<Event> events = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DriverManager.getConnection(DB_URL);

            // Xây dựng câu SQL với IN (?,?,...)
            StringBuilder sql = new StringBuilder(
                "SELECT eventId, title, status, requestId FROM Events " +
                "WHERE organizer = ? AND status IN ("
            );
            for (int i = 0; i < statuses.size(); i++) {
                sql.append("?");
                if (i < statuses.size() - 1) sql.append(",");
            }
            sql.append(")");
            pstmt = conn.prepareStatement(sql.toString());
            pstmt.setString(1, organizerId);
            for (int i = 0; i < statuses.size(); i++) {
                pstmt.setString(2 + i, statuses.get(i));
            }

            rs = pstmt.executeQuery();
            while (rs.next()) {
                Event event = new Event();
                event.setEventId(rs.getInt("eventId"));
                event.setTitle(rs.getString("title"));
                event.setStatus(rs.getString("status"));

                // Lấy requestId (String) nếu có
                String reqId = rs.getString("requestId");
                if (reqId != null) {
                    event.setRequestId(reqId);
                }
                // (Không gọi event.setNeeder(...) vì Event.java không có setter đó)
                events.add(event);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null)    rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null)  conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return events;
    }
    /**
     * Cập nhật rating cho Volunteer: tăng ratingCount lên 1, tính averageRating mới
     * @param volunteerUsername username của Volunteer
     * @param newScore điểm mới (1..5)
     * @return true nếu thành công
     */
    public boolean updateVolunteerRating(String volunteerUsername, int newScore) {
        return userDAO.updateVolunteerRating(volunteerUsername, newScore);
    }

    /**
     * Load the required skills for a given event
     *
     * @param conn Database connection
     * @param event The event to load skills for
     */
    private void loadEventSkills(Connection conn, Event event) throws SQLException {
        String sql = "SELECT s.skill FROM EventSkills es JOIN Skills s ON es.skillId = s.skillId WHERE es.eventId = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, event.getEventId());
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            event.addRequiredSkill(rs.getString("skill"));
        }

        rs.close();
        pstmt.close();
    }

    /**
     * Get the list of volunteers participating in an event
     *
     * @param eventId The ID of the event
     * @return List of volunteers participating in the event
     */
    public List<Volunteer> getEventParticipants(int eventId) {
        return userDAO.getEventParticipants(eventId);
    }

    /**
     * Register a volunteer to participate in an event
     *
     * @param eventId The ID of the event
     * @param volunteerId The ID (username) of the volunteer
     * @return true if registration was successful, false otherwise
     */
    public boolean registerVolunteerForEvent(int eventId, String volunteerId) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DriverManager.getConnection(DB_URL);

            // Check if the event is approved and not full
            String checkEventSQL = "SELECT status, maxParticipantNumber, " +
                                  "(SELECT COUNT(*) FROM EventParticipants WHERE eventId = ?) as currentParticipants " +
                                  "FROM Events WHERE eventId = ?";
            pstmt = conn.prepareStatement(checkEventSQL);
            pstmt.setInt(1, eventId);
            pstmt.setInt(2, eventId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String status = rs.getString("status");
                int maxParticipants = rs.getInt("maxParticipantNumber");
                int currentParticipants = rs.getInt("currentParticipants");

                if (!"Approved".equals(status)) {
                    return false; // Event is not Approved
                }

                if (currentParticipants >= maxParticipants) {
                    return false; // Event is full
                }
            } else {
                return false; // Event not found
            }

            rs.close();
            pstmt.close();

            // Insert the volunteer registration
            String insertSQL = "INSERT INTO EventParticipants (eventId, username) VALUES (?, ?)";
            pstmt = conn.prepareStatement(insertSQL);
            pstmt.setInt(1, eventId);
            pstmt.setString(2, volunteerId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get all available events with their current participation status
     *
     * @return List of events with participation information
     */
    public List<Event> getAllEvents() {
        List<Event> events = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(DB_URL);
            stmt = conn.createStatement();
            String sql = "SELECT e.*, " +
                        "(SELECT COUNT(*) FROM EventParticipants WHERE eventId = e.eventId) as participantCount " +
                        "FROM Events e";
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Event event = new Event();
                int eventId = rs.getInt("eventId");
                event.setEventId(eventId);
                event.setTitle(rs.getString("title"));
                event.setMaxParticipantNumber(rs.getInt("maxParticipantNumber"));

                // Lấy ngày từ cột DATE trong DB
                try {
                    String startDateStr = rs.getString("startDate");
                    String endDateStr = rs.getString("endDate");

                    if (startDateStr != null && !startDateStr.isEmpty()) {
                        try {
                            // Parse từ chuỗi định dạng YYYY-MM-DD
                            event.setStartDate(DATE_FORMAT.parse(startDateStr));
                        } catch (Exception ex) {
                            System.err.println("Cannot convert start date: " + startDateStr);
                        }
                    }

                    if (endDateStr != null && !endDateStr.isEmpty()) {
                        try {
                            // Parse từ chuỗi định dạng YYYY-MM-DD
                            event.setEndDate(DATE_FORMAT.parse(endDateStr));
                        } catch (Exception ex) {
                            System.err.println("Unable to convert end date: " + endDateStr);
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Error reading date: " + e.getMessage());
                }

                event.setEmergencyLevel(rs.getString("emergencyLevel"));
                event.setDescription(rs.getString("description"));
                event.setOrganizer(rs.getString("organizer"));
                event.setRequestId(rs.getString("RequestId"));
                event.setStatus(rs.getString("status"));

                // Load required skills for this event
                loadEventSkills(conn, event);

                events.add(event);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return events;
    }
    
    /**
     * Lấy chi tiết tham gia sự kiện cho một Volunteer.
     * Bao gồm thông tin Event và thông tin tham gia từ EventParticipants.
     * @param volunteerUsername Username của tình nguyện viên
     * @return List các EventParticipantDetails
     */
    public List<EventParticipantDetails> getEventParticipationDetailsForVolunteer(String volunteerUsername) {
        List<EventParticipantDetails> participationDetailsList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        // Lấy fullName của volunteer trước
        String volunteerFullName = "N/A"; // Default value
        try {
            conn = DriverManager.getConnection(DB_URL);
            PreparedStatement namePstmt = conn.prepareStatement("SELECT fullName FROM Volunteer WHERE username = ?");
            namePstmt.setString(1, volunteerUsername);
            ResultSet nameRs = namePstmt.executeQuery();
            if (nameRs.next()) {
                volunteerFullName = nameRs.getString("fullName");
            }
            if (nameRs != null) nameRs.close();
            if (namePstmt != null) namePstmt.close();
            // Không đóng conn ở đây vì sẽ dùng tiếp
        } catch (SQLException e) {
            e.printStackTrace(); // Log lỗi nhưng vẫn tiếp tục để thử lấy các thông tin khác
        }

        // Câu SQL gốc để lấy các sự kiện volunteer đã tham gia hoặc đăng ký
        String sql = "SELECT e.*, ep.hoursParticipated, ep.ratingByOrg " +
                     "FROM Events e " +
                     "LEFT JOIN EventParticipants ep ON e.eventId = ep.eventId AND ep.username = ? " +
                     "JOIN Notification n ON e.eventId = n.eventId AND n.username = ? " +
                     "WHERE n.acceptStatus = 'Registered' OR n.acceptStatus = 'Attended' OR n.acceptStatus = 'Completed_Volunteer_Side' OR e.status = 'Done'"; // Adjust based on how participation is tracked

        try {
            if (conn == null || conn.isClosed()) { // Mở lại nếu đã bị đóng ở trên hoặc chưa mở
                conn = DriverManager.getConnection(DB_URL);
            }
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, volunteerUsername);
            pstmt.setString(2, volunteerUsername);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Event event = new Event();
                event.setEventId(rs.getInt("eventId"));
                event.setTitle(rs.getString("title"));
                // ... (Nạp các thuộc tính khác của Event như cũ)
                String startDateStr = rs.getString("startDate");
                String endDateStr = rs.getString("endDate");
                try {
                    if (startDateStr != null && !startDateStr.isEmpty()) {
                        event.setStartDate(DATE_FORMAT.parse(startDateStr));
                    }
                    if (endDateStr != null && !endDateStr.isEmpty()) {
                        event.setEndDate(DATE_FORMAT.parse(endDateStr));
                    }
                } catch (java.text.ParseException e) {
                    System.err.println("Error parsing date from DB string: " + e.getMessage());
                    event.setStartDate(null);
                    event.setEndDate(null);
                }
                event.setStatus(rs.getString("status"));
                event.setOrganizer(rs.getString("organizer"));

                Integer hoursParticipated = rs.getObject("hoursParticipated") != null ? rs.getInt("hoursParticipated") : null;
                Integer ratingByOrg = rs.getObject("ratingByOrg") != null ? rs.getInt("ratingByOrg") : null;
                String volunteerParticipationStatus = getVolunteerEventParticipationStatus(conn, volunteerUsername, event.getEventId());

                EventParticipantDetails details = new EventParticipantDetails(
                        event,
                        volunteerUsername,
                        volunteerFullName, // Sử dụng fullName đã lấy được
                        hoursParticipated,
                        ratingByOrg,
                        volunteerParticipationStatus
                );
                details.setOrganizerName(getOrganizerNameById(conn, event.getOrganizer()));
                participationDetailsList.add(details);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(conn, pstmt, rs); // conn sẽ được đóng ở đây
        }
        return participationDetailsList;
    }
    
    /**
     * Phương thức trợ giúp để lấy trạng thái tham gia của Volunteer cho một Event.
     * Bạn cần định nghĩa logic này dựa trên cách hệ thống quản lý (ví dụ: qua Notification)
     */
    private String getVolunteerEventParticipationStatus(Connection conn, String volunteerUsername, int eventId) throws SQLException {
        // Ví dụ: lấy từ bảng Notification
        PreparedStatement pstmtNotif = null;
        ResultSet rsNotif = null;
        // Lấy thông báo mới nhất liên quan đến volunteer và event này
        String sqlNotif = "SELECT acceptStatus FROM Notification " +
                          "WHERE username = ? AND eventId = ? " +
                          "ORDER BY notificationId DESC LIMIT 1";
        try {
            pstmtNotif = conn.prepareStatement(sqlNotif);
            pstmtNotif.setString(1, volunteerUsername);
            pstmtNotif.setInt(2, eventId);
            rsNotif = pstmtNotif.executeQuery();
            if (rsNotif.next()) {
                String status = rsNotif.getString("acceptStatus");
                return status != null ? status : "Unknown"; // Hoặc một trạng thái mặc định
            }
        } finally {
            if (rsNotif != null) rsNotif.close();
            if (pstmtNotif != null) pstmtNotif.close();
        }
        // Nếu không có trong Notification, có thể tình nguyện viên đã được thêm trực tiếp
        // hoặc bạn có logic khác để xác định trạng thái.
        // Đây là một placeholder, bạn cần điều chỉnh cho phù hợp.
        return "Registered"; // Trạng thái mặc định nếu không tìm thấy trong Notification
    }
    
    // Phương thức trợ giúp để lấy tên tổ chức từ username (nếu cần)
    private String getOrganizerNameById(Connection conn, String organizerUsername) throws SQLException {
        PreparedStatement pstmtOrg = null;
        ResultSet rsOrg = null;
        String sqlOrg = "SELECT organizationName FROM VolunteerOrganization WHERE username = ?";
        try {
            pstmtOrg = conn.prepareStatement(sqlOrg);
            pstmtOrg.setString(1, organizerUsername);
            rsOrg = pstmtOrg.executeQuery();
            if (rsOrg.next()) {
                return rsOrg.getString("organizationName");
            }
        } finally {
            if (rsOrg != null) rsOrg.close();
            if (pstmtOrg != null) pstmtOrg.close();
        }
        return "Unknown Organization";
    }


    // Phương thức trợ giúp để đóng tài nguyên
    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close(); // Chỉ đóng nếu kết nối được tạo trong phương thức đó
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Lấy thông tin chi tiết của một Event dựa trên eventId.
     * Bao gồm cả requiredSkills.
     * @param eventId ID của sự kiện
     * @return Đối tượng Event hoặc null nếu không tìm thấy
     */
   public Event getEventById(int eventId) {
       Event event = null;
       Connection conn = null;
       PreparedStatement pstmt = null;
       ResultSet rs = null;
       String sql = "SELECT * FROM Events WHERE eventId = ?";

       try {
           conn = DriverManager.getConnection(DB_URL);
           pstmt = conn.prepareStatement(sql);
           pstmt.setInt(1, eventId);
           rs = pstmt.executeQuery();

           if (rs.next()) {
               event = new Event();
               event.setEventId(rs.getInt("eventId"));
               event.setTitle(rs.getString("title"));
               event.setMaxParticipantNumber(rs.getObject("maxParticipantNumber") != null ? rs.getInt("maxParticipantNumber") : null);

               String startDateStr = rs.getString("startDate");
               String endDateStr = rs.getString("endDate");
               try {
                   if (startDateStr != null && !startDateStr.isEmpty()) {
                       event.setStartDate(DATE_FORMAT.parse(startDateStr));
                   }
                   if (endDateStr != null && !endDateStr.isEmpty()) {
                       event.setEndDate(DATE_FORMAT.parse(endDateStr));
                   }
               } catch (java.text.ParseException e) {
                   System.err.println("Error parsing date for event " + eventId + ": " + e.getMessage());
               }

               event.setEmergencyLevel(rs.getString("emergencyLevel"));
               event.setDescription(rs.getString("description"));
               event.setOrganizer(rs.getString("organizer"));
               event.setRequestId(rs.getString("RequestId"));
               event.setStatus(rs.getString("status"));

               // Load required skills
               loadEventSkills(conn, event); // Giả sử bạn có phương thức này (từ code EventController bạn gửi ban đầu)
           }
       } catch (SQLException e) {
           e.printStackTrace();
       } finally {
           closeResources(conn, pstmt, rs);
       }
       return event;
   }
    public List<entity.notifications.Notification> getPendingNotificationsByOrganizer(String organizerUsername) {
        return notificationDAO.getPendingNotificationsByOrganizer(organizerUsername);
    }

    public boolean updateNotificationStatus(int notificationId, String newStatus) {
        return notificationDAO.updateNotificationStatus(notificationId, newStatus);
    }

   
   
// File: src/controller/event/EventController.java

// ... (các hằng số DB_URL, DATE_FORMAT_DB đã có)

    /**
     * Lớp nội bộ để bọc Event cùng với điểm số phù hợp (số skill khớp)
     */
    private static class SuggestedEventWrapper {
        private Event event;
        private int matchedSkillsCount; // Điểm này có thể là số skill khớp, hoặc một giá trị đặc biệt
                                        // cho sự kiện không yêu cầu skill (ví dụ Integer.MAX_VALUE)

        public SuggestedEventWrapper(Event event, int matchedSkillsCount) {
            this.event = event;
            this.matchedSkillsCount = matchedSkillsCount;
        }

        public Event getEvent() {
            return event;
        }

        public int getMatchedSkillsCount() {
            return matchedSkillsCount;
        }
    }

    /**
     * Chuyển đổi mức độ khẩn cấp (String) thành một số nguyên để sắp xếp.
     * Số nhỏ hơn nghĩa là ưu tiên cao hơn (khẩn cấp hơn).
     * @param emergencyLevel Chuỗi mức độ khẩn cấp
     * @return Số nguyên đại diện cho độ ưu tiên
     */
    public int getEmergencyLevelPriority(String emergencyLevel) {
        if (emergencyLevel == null || emergencyLevel.trim().isEmpty()) {
            return Integer.MAX_VALUE; // Mức ưu tiên thấp nhất nếu không có hoặc rỗng
        }
        switch (emergencyLevel.trim().toLowerCase()) {
            case "urgent":        // Hoặc "Urgent", "Critical", etc.
                return 1;
            case "high":             // Hoặc "High"
                return 2;
            case "normal":     // Hoặc "Normal", "Medium"
                return 3;
            case "low":            // Hoặc "Low"
                return 4;
            default:
                System.out.println("Unknown emergency level for priority: " + emergencyLevel);
                return 5; // Mức thấp nhất cho các giá trị không xác định
        }
    }

    /**
     * Lấy tất cả các sự kiện đang mở và còn chỗ.
     * Mở: status là "Approved", "Pending", "Coming Soon" VÀ startDate là trong tương lai.
     * Còn chỗ: số người tham gia hiện tại < maxParticipantNumber.
     * Đồng thời load requiredSkills cho mỗi event.
     * @return List các Event
     */
    public List<Event> getAllOpenAndAvailableEvents() {
        List<Event> events = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = "SELECT *, (SELECT COUNT(*) FROM EventParticipants ep WHERE ep.eventId = e.eventId) as currentParticipants FROM Events e WHERE status = ? OR status = ?";

        try {
            conn = DriverManager.getConnection(DB_URL);
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, AppConstants.EVENT_APPROVED);
            pstmt.setString(2, AppConstants.EVENT_UPCOMING);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                int currentParticipants = rs.getInt("currentParticipants");
                Integer maxParticipants = rs.getObject("maxParticipantNumber") != null ? rs.getInt("maxParticipantNumber") : null;

                // Chỉ thêm vào nếu còn chỗ hoặc không giới hạn số lượng người tham gia
                if (maxParticipants == null || currentParticipants < maxParticipants) {
                    Event event = new Event();
                    event.setEventId(rs.getInt("eventId"));
                    event.setTitle(rs.getString("title"));
                    event.setMaxParticipantNumber(maxParticipants);

                    String startDateStr = rs.getString("startDate");
                    String endDateStr = rs.getString("endDate");
                    try {
                        if (startDateStr != null && !startDateStr.isEmpty()) {
                            event.setStartDate(DATE_FORMAT.parse(startDateStr));
                        }
                        if (endDateStr != null && !endDateStr.isEmpty()) {
                            event.setEndDate(DATE_FORMAT.parse(endDateStr));
                        }
                    } catch (java.text.ParseException e) {
                        System.err.println("Error parsing date in getAllOpenAndAvailableEvents for eventId " + event.getEventId() + ": " + e.getMessage());
                    }
                    event.setEmergencyLevel(rs.getString("emergencyLevel"));
                    event.setDescription(rs.getString("description"));
                    event.setOrganizer(rs.getString("organizer"));
                    event.setStatus(rs.getString("status"));

                    loadEventSkills(conn, event); // Load kỹ năng yêu cầu cho sự kiện này

                    events.add(event);
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Error in getAllOpenAndAvailableEvents: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Sử dụng hàm closeResources đã có
            closeResources(conn, pstmt, rs);
        }
        return events;
    }

    /**
     * Đề xuất danh sách các sự kiện phù hợp cho một Tình nguyện viên cụ thể.
     *
     * Thuật toán:
     * 1. Lấy tất cả sự kiện đang mở và còn chỗ (sử dụng getAllOpenAndAvailableEvents).
     * 2. Lọc ra các sự kiện có ít nhất một kỹ năng yêu cầu khớp với kỹ năng của TNV
     *    (hoặc sự kiện không yêu cầu kỹ năng nào).
     * 3. Sắp xếp sự kiện:
     *    - Ưu tiên 1 (Giảm dần): Số kỹ năng khớp (sự kiện không yêu cầu skill được coi là khớp nhiều nhất).
     *    - Ưu tiên 2 (Giảm dần theo độ ưu tiên - tức là tăng dần theo số priority): Mức độ khẩn cấp.
     *    - Ưu tiên 3 (Tăng dần): Ngày bắt đầu sự kiện (sớm hơn được ưu tiên).
     *
     * @param volunteer TNV cần đề xuất sự kiện
     * @return List các Event đã được sắp xếp theo mức độ phù hợp
     */
    public List<Event> getSuggestedEventsForVolunteer(Volunteer volunteer) {
        if (volunteer == null || volunteer.getUsername() == null) {
            System.err.println("getSuggestedEventsForVolunteer: Volunteer object or username is null.");
            return new ArrayList<>();
        }
        // Đảm bảo volunteer.getSkills() đã được nạp trước khi gọi hàm này
        List<String> volunteerSkills = volunteer.getSkills();
        if (volunteerSkills == null) {
            System.err.println("getSuggestedEventsForVolunteer: Volunteer skills list is null for user " + volunteer.getUsername());
            // Có thể thử nạp lại skills ở đây nếu cần, hoặc trả về rỗng
            // volunteerSkills = verificationController.getVolunteer(volunteer.getUsername()).getSkills(); // Ví dụ
            // if(volunteerSkills == null) return new ArrayList<>();
            return new ArrayList<>();
        }


        List<Event> allOpenEvents = getAllOpenAndAvailableEvents();
        List<SuggestedEventWrapper> suggestedEventsWithScore = new ArrayList<>();

        for (Event event : allOpenEvents) {
            int matchedSkillsCount = 0;
            boolean eventRequiresSkills = event.getRequiredSkills() != null && !event.getRequiredSkills().isEmpty();
            int effectiveMatchScore;

            if (eventRequiresSkills) {
                for (String requiredSkill : event.getRequiredSkills()) {
                    if (volunteerSkills.contains(requiredSkill)) {
                        matchedSkillsCount++;
                    }
                }
                // Chỉ thêm nếu có ít nhất 1 skill khớp
                if (matchedSkillsCount > 0) {
                    effectiveMatchScore = matchedSkillsCount;
                    suggestedEventsWithScore.add(new SuggestedEventWrapper(event, effectiveMatchScore));
                }
            } else {
                // Sự kiện không yêu cầu kỹ năng nào => coi như phù hợp cao nhất về mặt kỹ năng
                effectiveMatchScore = Integer.MAX_VALUE; // Để ưu tiên lên đầu khi sắp xếp theo skill
                suggestedEventsWithScore.add(new SuggestedEventWrapper(event, effectiveMatchScore));
            }
        }

        // Sắp xếp danh sách đề xuất
        Collections.sort(suggestedEventsWithScore, Comparator
                .comparingInt(SuggestedEventWrapper::getMatchedSkillsCount).reversed() // 1. Skill khớp giảm dần
                .thenComparing(wrapper -> wrapper.getEvent().getEmergencyLevel(),
                        Comparator.nullsLast(Comparator.comparingInt(this::getEmergencyLevelPriority))) // 2. Mức độ khẩn cấp (ưu tiên cao hơn lên trước)
                .thenComparing(wrapper -> wrapper.getEvent().getStartDate(),
                        Comparator.nullsLast(Comparator.naturalOrder())) // 3. Ngày bắt đầu tăng dần (sớm hơn lên trước)
        );

        List<Event> sortedSuggestedEvents = new ArrayList<>();
        for (SuggestedEventWrapper wrapper : suggestedEventsWithScore) {
            sortedSuggestedEvents.add(wrapper.getEvent());
        }

        return sortedSuggestedEvents;
    }

    // Add new method to update event status
    public boolean updateEventStatus(int eventId, String newStatus) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        String sql = "UPDATE Events SET status = ? WHERE eventId = ?";

        try {
            conn = DriverManager.getConnection(DB_URL);
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, eventId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating event status for eventId " + eventId + " to " + newStatus + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public List<EventParticipantDetails> getEventParticipantDetailsList(int eventId) {
        List<EventParticipantDetails> participants = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        String sql = "SELECT " +
                     "    SU.username AS volunteerUsername, SU.email, SU.phone, SU.address, " +
                     "    V.fullName AS volunteerFullName, V.cccd, V.dateOfBirth, V.averageRating, V.ratingCount, V.freeHourPerWeek, " +
                     "    EP.hoursParticipated, EP.ratingByOrg " +
                     "FROM " +
                     "    EventParticipants EP " +
                     "JOIN " +
                     "    Volunteer V ON EP.username = V.username " +
                     "JOIN " +
                     "    SystemUser SU ON V.username = SU.username " +
                     "WHERE " +
                     "    EP.eventId = ?;";

        try {
            conn = DriverManager.getConnection(DB_URL);
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, eventId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                EventParticipantDetails details = new EventParticipantDetails();
                details.setVolunteerUsername(rs.getString("volunteerUsername"));
                details.setVolunteerFullName(rs.getString("volunteerFullName"));
                // We can populate more fields in EventParticipantDetails if it's designed to hold them,
                // or if the Volunteer object is part of it.
                // For now, sticking to what's directly in EventParticipantDetails from its definition.

                // If EventParticipantDetails needs more Volunteer fields, they are available here:
                // String email = rs.getString("email");
                // String phone = rs.getString("phone");
                // String address = rs.getString("address");
                // String cccd = rs.getString("cccd");
                // Date dateOfBirth = parseDate(rs.getString("dateOfBirth"));
                // double averageRating = rs.getDouble("averageRating");
                // int ratingCount = rs.getInt("ratingCount");
                // int freeHourPerWeek = rs.getInt("freeHourPerWeek");


                if (rs.getObject("hoursParticipated") != null) {
                    details.setHoursParticipated(rs.getInt("hoursParticipated"));
                } else {
                    details.setHoursParticipated(null); // Or 0, depending on desired default
                }

                if (rs.getObject("ratingByOrg") != null) {
                    details.setRatingByOrg(rs.getInt("ratingByOrg"));
                } else {
                    details.setRatingByOrg(null); // Or 0
                }
                
                // The EventParticipantDetails class also has event-specific fields (eventId, title, eventStatus etc.)
                // These are not fetched by this query as it focuses on participants.
                // They might need to be set separately if this DTO is used in a context requiring them.
                // For displaying a list of volunteers for *this* event, we primarily need volunteer info.

                participants.add(details);
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Consider more robust logging
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return participants;
    }

    public boolean saveProgressReport(Report report, boolean isFinal) {
        return reportDAO.saveProgressReport(report, isFinal);
    }

    public boolean eventHasFinalHundredPercentReport(int eventId) {
        return reportDAO.eventHasFinalHundredPercentReport(eventId);
    }

    /**
     * Retrieves the total number of events created by a specific organization.
     *
     * @param organizerId The ID (username) of the organization.
     * @return The total count of events for that organization.
     */
    public int getTotalEventCountByOrganizer(String organizerId) {
        List<Event> events = getEventsByOrganizerId(organizerId); // Reuse existing method
        if (events != null) {
            return events.size();
        }
        return 0; // Return 0 if the list is null (though getEventsByOrganizerId initializes an ArrayList)
    }
}
