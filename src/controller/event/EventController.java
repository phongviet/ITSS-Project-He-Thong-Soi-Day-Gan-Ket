package controller.event;

import entity.events.*;
import entity.users.VolunteerOrganization;
import entity.users.Volunteer;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import entity.requests.HelpRequest;
import java.text.ParseException;
import java.util.Date;
import java.text.SimpleDateFormat;
import entity.events.Event;


public class EventController {

    private static final String DB_URL = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public List<HelpRequest> getApprovedHelpRequests() {
        List<HelpRequest> helpRequests = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(DB_URL);
            String sql = "SELECT * FROM HelpRequest WHERE status = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "approved");
            rs = pstmt.executeQuery();

            while (rs.next()) {
                HelpRequest hr = new HelpRequest();
                hr.setRequestId(rs.getInt("requestId"));
                hr.setTitle(rs.getString("title"));
                String startDateStr = rs.getString("startDate");
                if (startDateStr != null && !startDateStr.isEmpty()) {
                    try {
                        Date d = DATE_FORMAT.parse(startDateStr);
                        hr.setStartDate(d);
                    } catch (ParseException ex) {
                        System.err.println("Không thể parse startDate: " + startDateStr);
                    }
                }
                hr.setEmergencyLevel(rs.getString("emergencyLevel"));
                hr.setDescription(rs.getString("description"));
                hr.setPersonInNeedID(rs.getString("personInNeedID"));
                hr.setStatus(rs.getString("status"));
                helpRequests.add(hr);
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

        return helpRequests;
    }
    public boolean updateHelpRequestStatus(int requestId, String newStatus) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
            String sql = "UPDATE HelpRequest SET status = ? WHERE requestId = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, requestId);
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
    public List<EventParticipantDetails> getEventParticipantDetails(int eventId) {
        List<EventParticipantDetails> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(DB_URL);

            // Query kết hợp giữa Events và EventParticipants (không join Volunteer vì chúng ta chỉ cần username ở đây)
            String sql = "SELECT e.eventId, e.title, e.startDate, e.endDate, e.status AS eventStatus, " +
                         // Giả sử bạn muốn hiển thị tên tổ chức: cần join VolunteerOrganization hoặc SystemUser, 
                         // nhưng ở đây ta chỉ giữ organizer username:
                         "e.organizer, ep.username AS volunteerUsername, ep.hoursParticipated, ep.ratingByOrg, ep.acceptStatus " +
                         "FROM Events e " +
                         "JOIN EventParticipants ep ON e.eventId = ep.eventId " +
                         "WHERE e.eventId = ?";

            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, eventId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                EventParticipantDetails dto = new EventParticipantDetails();

                // Lấy trường từ Event
                dto.setEventId(rs.getInt("eventId"));
                dto.setTitle(rs.getString("title"));
                // Chuyển từ java.sql.Date sang java.util.Date
                java.sql.Date sdate = rs.getDate("startDate");
                java.sql.Date edate = rs.getDate("endDate");
                if (sdate != null) dto.setStartDate(new java.util.Date(sdate.getTime()));
                if (edate != null) dto.setEndDate(new java.util.Date(edate.getTime()));
                dto.setEventStatus(rs.getString("eventStatus"));
                // Nếu muốn lấy tên tổ chức đầy đủ, bạn cần query thêm VolunteerOrganization—
                // ở đây tạm set organizerName = organizer username:
                dto.setOrganizerName(rs.getString("organizer"));

                // Lấy trường từ EventParticipants
                dto.setVolunteerUsername(rs.getString("volunteerUsername"));
                int hours = rs.getInt("hoursParticipated");
                if (!rs.wasNull()) dto.setHoursParticipated(hours);
                int rateByOrg = rs.getInt("ratingByOrg");
                if (!rs.wasNull()) dto.setRatingByOrg(rateByOrg);
                // acceptStatus (có thể là Registered/Attended/…)
                dto.setVolunteerParticipationStatus(rs.getString("acceptStatus"));

                list.add(dto);
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

        return list;
    }
    public boolean registerEvent(Event event, VolunteerOrganization organization) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DriverManager.getConnection(DB_URL);

            // Start a transaction
            conn.setAutoCommit(false);

            // Set the initial status to "pending"
            event.setStatus("pending");

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
                            System.err.println("Không thể chuyển đổi ngày bắt đầu: " + startDateStr);
                        }
                    }

                    if (endDateStr != null && !endDateStr.isEmpty()) {
                        try {
                            // Parse từ chuỗi định dạng YYYY-MM-DD
                            event.setEndDate(DATE_FORMAT.parse(endDateStr));
                        } catch (Exception ex) {
                            System.err.println("Không thể chuyển đổi ngày kết thúc: " + endDateStr);
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Lỗi khi đọc ngày tháng: " + e.getMessage());
                }

                event.setEmergencyLevel(rs.getString("emergencyLevel"));
                event.setDescription(rs.getString("description"));
                event.setOrganizer(rs.getString("organizer"));
                event.setRequestId(rs.getString("RequestId"));

                // Get status from database, default to "pending" if null
                String status = rs.getString("status");
                event.setStatus(status != null ? status : "pending");

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
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DriverManager.getConnection(DB_URL);

            // Truy xuất dữ liệu cũ
            String selectSql = "SELECT averageRating, ratingCount FROM Volunteer WHERE username = ?";
            pstmt = conn.prepareStatement(selectSql);
            pstmt.setString(1, volunteerUsername);
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                rs.close();
                pstmt.close();
                return false; // không tìm thấy volunteer
            }
            double oldAvg = rs.getDouble("averageRating");
            int oldCount  = rs.getInt("ratingCount");
            rs.close();
            pstmt.close();

            int newCount = oldCount + 1;
            double newAvg;
            if (oldCount == 0) {
                newAvg = newScore;
            } else {
                newAvg = (oldAvg * oldCount + newScore) / newCount;
            }

            // Cập nhật vào DB
            String updateSql = "UPDATE Volunteer SET averageRating = ?, ratingCount = ? WHERE username = ?";
            pstmt = conn.prepareStatement(updateSql);
            pstmt.setDouble(1, newAvg);
            pstmt.setInt(2, newCount);
            pstmt.setString(3, volunteerUsername);
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (pstmt != null) pstmt.close();
                if (conn != null)  conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
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
        List<Volunteer> participants = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(DB_URL);
            // Join EventParticipants with Volunteers to get volunteer details
            String sql = "SELECT v.* FROM EventParticipants ep " +
                         "JOIN Volunteer v ON ep.username = v.username " +
                         "WHERE ep.eventId = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, eventId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Volunteer volunteer = new Volunteer();
                volunteer.setUsername(rs.getString("username"));
                volunteer.setFullName(rs.getString("fullName"));
                // Set other properties as needed

                participants.add(volunteer);
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

        return participants;
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

                if (!"approved".equals(status)) {
                    return false; // Event is not approved
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

                // Lấy ngày trực tiếp từ cơ sở dữ liệu bằng rs.getDate
                java.sql.Date startSqlDate = rs.getDate("startDate");
                java.sql.Date endSqlDate = rs.getDate("endDate");

                if (startSqlDate != null) {
                    // Chuyển từ java.sql.Date sang java.util.Date
                    event.setStartDate(new java.util.Date(startSqlDate.getTime()));
                }

                if (endSqlDate != null) {
                    // Chuyển từ java.sql.Date sang java.util.Date
                    event.setEndDate(new java.util.Date(endSqlDate.getTime()));
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
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
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

        // Lấy thông tin từ Events và EventParticipants
        String sql = "SELECT e.*, ep.hoursParticipated, ep.ratingByOrg " +
                     "FROM Events e " +
                     "JOIN EventParticipants ep ON e.eventId = ep.eventId " +
                     "WHERE ep.username = ?";

        try {
            conn = DriverManager.getConnection(DB_URL);
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, volunteerUsername);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Event event = new Event();
                event.setEventId(rs.getInt("eventId"));
                event.setTitle(rs.getString("title"));
                // ... (set các thuộc tính khác cho event từ ResultSet rs)
                String startDateStr = rs.getString("startDate"); // Đọc dưới dạng String
                String endDateStr = rs.getString("endDate");     // Đọc dưới dạng String

                try {
                    if (startDateStr != null && !startDateStr.isEmpty()) {
                        event.setStartDate(DATE_FORMAT.parse(startDateStr)); // Parse bằng SimpleDateFormat đã định nghĩa
                    }
                    if (endDateStr != null && !endDateStr.isEmpty()) {
                        event.setEndDate(DATE_FORMAT.parse(endDateStr));     // Parse bằng SimpleDateFormat đã định nghĩa
                    }
                } catch (java.text.ParseException e) {
                    System.err.println("Error parsing date from DB string: " + e.getMessage());
                    // Có thể gán null hoặc xử lý khác nếu parse lỗi
                    event.setStartDate(null);
                    event.setEndDate(null);
                }
                event.setStatus(rs.getString("status")); // Trạng thái chung của Event
                event.setOrganizer(rs.getString("organizer"));
                // ... (nạp các thuộc tính Event khác nếu cần)


                Integer hoursParticipated = rs.getObject("hoursParticipated") != null ? rs.getInt("hoursParticipated") : null;
                Integer ratingByOrg = rs.getObject("ratingByOrg") != null ? rs.getInt("ratingByOrg") : null;

                // Lấy trạng thái tham gia của Volunteer (ví dụ từ Notification)
                // Đây là phần bạn cần xác định logic chính xác cho hệ thống của mình
                // Ví dụ: "Registered", "Attended", "Canceled by Volunteer"
                String volunteerParticipationStatus = getVolunteerEventParticipationStatus(conn, volunteerUsername, event.getEventId());

                EventParticipantDetails details = new EventParticipantDetails(
                        event,
                        volunteerUsername,
                        hoursParticipated,
                        ratingByOrg,
                        volunteerParticipationStatus
                );
                
                // Lấy tên Tổ chức nếu cần (có thể làm ở đây hoặc trong constructor của EventParticipantDetails)
                // details.setOrganizerName(getOrganizerNameById(conn, event.getOrganizer()));


                participationDetailsList.add(details);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Có thể ném một custom exception ở đây
        } finally {
            closeResources(conn, pstmt, rs);
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
        List<entity.notifications.Notification> result = new ArrayList<>();
        String sql = "SELECT n.notificationId, n.eventId, n.username, n.acceptStatus, e.title " +
                    "FROM Notification n " +
                    "JOIN Events e ON n.eventId = e.eventId " +
                    "WHERE e.organizer = ? AND n.acceptStatus = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, organizerUsername);
            pstmt.setString(2, "pending");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    entity.notifications.Notification no = new entity.notifications.Notification();
                    no.setNotificationId(rs.getInt("notificationId"));
                    no.setEventId(rs.getInt("eventId"));
                    no.setUsername(rs.getString("username"));
                    no.setAcceptStatus(rs.getString("acceptStatus"));
                    // Giả sử Notification class có thêm field eventTitle và setter tương ứng
                    no.setEventTitle(rs.getString("title"));
                    result.add(no);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean updateNotificationStatus(int notificationId, String newStatus) {
        String sql = "UPDATE Notification SET acceptStatus = ? WHERE notificationId = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setInt(2, notificationId);
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
