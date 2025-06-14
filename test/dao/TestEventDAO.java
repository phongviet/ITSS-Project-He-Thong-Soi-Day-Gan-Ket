package dao; // Hoặc package test của bạn

import entity.events.Event;
import org.junit.jupiter.api.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Chạy @BeforeAll và @AfterAll một lần cho cả class
class TestEventDAO {

    private EventDAO eventDAO;
    // QUAN TRỌNG: DB_URL_FOR_TEST này PHẢI GIỐNG HỆT DB_URL trong EventDAO.java
    private static final String DB_URL_FOR_TEST = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db";
    private Connection connForHelpers; // Connection dùng cho các helper để chèn/xóa dữ liệu test
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @BeforeAll
    void setUpAll() throws SQLException {
        eventDAO = new EventDAO(); // Khởi tạo DAO cần test

        connForHelpers = DriverManager.getConnection(DB_URL_FOR_TEST);
        System.out.println("TestEventDAO: Connected to DB for helpers: " + DB_URL_FOR_TEST);
        
        // Nếu bạn muốn mỗi lần chạy test là một CSDL sạch hoàn toàn (khuyến nghị cho test DAO)
        // bạn có thể tạo schema ở đây nếu DB_URL_FOR_TEST là một file test riêng.
        // Hoặc, nếu dùng chung DB, thì chỉ clear dữ liệu trong @BeforeEach.
        // createTestDatabaseSchema(connForHelpers); // Ví dụ nếu có hàm này
    }

    @BeforeEach
    void setUpForEachTest() throws SQLException {
        System.out.println("--- TestEventDAO: Running setUpForEachTest: Clearing Data ---");
        // Xóa dữ liệu từ các bảng liên quan trước mỗi test để đảm bảo tính độc lập
        clearTableData(connForHelpers, "EventSkills");
        clearTableData(connForHelpers, "EventParticipants");
        clearTableData(connForHelpers, "Events");
        clearTableData(connForHelpers, "Skills");
        clearTableData(connForHelpers, "VolunteerOrganization");
        clearTableData(connForHelpers, "PersonInNeed");
        clearTableData(connForHelpers, "HelpRequest"); // Nếu Events có liên quan
        clearTableData(connForHelpers, "Volunteer");  // Nếu EventParticipants liên quan
        clearTableData(connForHelpers, "SystemUser"); // Bảng gốc

        // Chèn lại dữ liệu tham chiếu cơ bản cần thiết
        insertInitialSkills(connForHelpers);
        ensureSystemUserExists(connForHelpers, "orgTestDAO1", "test");
        ensureVolunteerOrganizationExists(connForHelpers, "orgTestDAO1", "Org For DAO Test 1");
        ensureSystemUserExists(connForHelpers, "orgTestDAOSkills", "test");
        ensureVolunteerOrganizationExists(connForHelpers, "orgTestDAOSkills", "Org For Skills DAO Test");
    }

    @AfterAll
    void tearDownAll() throws SQLException {
        if (connForHelpers != null && !connForHelpers.isClosed()) {
            // Có thể muốn xóa toàn bộ dữ liệu test ở đây nếu bạn không làm ở BeforeEach
            // hoặc nếu bạn muốn CSDL trở lại trạng thái ban đầu sau bộ test.
            connForHelpers.close();
            System.out.println("TestEventDAO: Closed DB helper connection.");
        }
    }

    // --- Helper methods (Nhiều helper có thể copy từ TestEventController cũ của bạn) ---
    private void clearTableData(Connection connection, String tableName) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Tắt kiểm tra khóa ngoại để xóa dễ dàng hơn
            stmt.execute("PRAGMA foreign_keys = OFF;");
            stmt.execute("DELETE FROM " + tableName + ";");
            // Reset auto-increment key (nếu bảng có PRIMARY KEY AUTOINCREMENT)
            try {
                 stmt.execute("DELETE FROM sqlite_sequence WHERE name='" + tableName + "';");
            } catch (SQLException e) {
                // Bỏ qua nếu bảng không có trong sqlite_sequence (ví dụ: không có key AUTOINCREMENT)
                // System.out.println("Note: No sequence found for table " + tableName + " to reset.");
            }
            // Bật lại kiểm tra khóa ngoại
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
    }
    
    private void insertInitialSkills(Connection connection) throws SQLException {
        // Sử dụng getSkillIdByName để chèn nếu chưa có, hoặc chỉ chèn trực tiếp
        // Giả sử getSkillIdByName trong EventDAO (hoặc một SkillDAO) có logic "insert if not exists"
        // Hoặc chúng ta tự chèn ở đây
        insertSkillIfNotExists(connection, "Teaching");
        insertSkillIfNotExists(connection, "Communication");
        insertSkillIfNotExists(connection, "First Aid");
        insertSkillIfNotExists(connection, "Driving");
    }

    private int insertSkillIfNotExists(Connection connection, String skillName) throws SQLException {
        // Kiểm tra skill đã tồn tại chưa
        String selectSql = "SELECT skillId FROM Skills WHERE skill = ?";
        try (PreparedStatement pstmtSelect = connection.prepareStatement(selectSql)) {
            pstmtSelect.setString(1, skillName);
            try (ResultSet rs = pstmtSelect.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("skillId"); // Trả về ID nếu đã tồn tại
                }
            }
        }
        // Nếu chưa tồn tại, chèn mới
        String insertSql = "INSERT INTO Skills (skill) VALUES (?)";
        try (PreparedStatement pstmtInsert = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmtInsert.setString(1, skillName);
            pstmtInsert.executeUpdate();
            try (ResultSet generatedKeys = pstmtInsert.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating skill failed, no ID obtained for: " + skillName);
                }
            }
        }
    }
    
    // Các helper ensureSystemUserExists, ensureVolunteerOrganizationExists có thể copy từ TestEventController
     private void ensureSystemUserExists(Connection connection, String username, String password) throws SQLException {
        String sql = "INSERT OR IGNORE INTO SystemUser (username, password, email, phone, address) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, username + "@example.com");
            pstmt.setString(4, "0123456789");
            pstmt.setString(5, "Test Address for " + username);
            pstmt.executeUpdate();
        }
    }

    private void ensureVolunteerOrganizationExists(Connection connection, String username, String orgName) throws SQLException {
        ensureSystemUserExists(connection, username, "testorgpassdao");
        String sql = "INSERT OR IGNORE INTO VolunteerOrganization (username, organizationName, licenseNumber, field, representative, sponsor, info) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, orgName);
            pstmt.setString(3, "LIC-DAO-" + username.toUpperCase());
            pstmt.setString(4, "DAO Test Field");
            pstmt.setString(5, "Rep-DAO-" + orgName);
            pstmt.setString(6, "Sponsor-DAO-" + username);
            pstmt.setString(7, "Info for DAO Test Org " + orgName);
            pstmt.executeUpdate();
        }
    }
    
     private void ensurePersonInNeedExists(Connection connection, String username, String fullName) throws SQLException {
        ensureSystemUserExists(connection, username, "testpersondao");
        String sql = "INSERT OR IGNORE INTO PersonInNeed (username, fullName, cccd, dateOfBirth) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, fullName);
            pstmt.setString(3, "CCCD-DAO-" + username);
            pstmt.setString(4, "1990-01-01"); 
            pstmt.executeUpdate();
        }
    }
    
    private void ensureVolunteerExists(Connection connection, String username, String fullName) throws SQLException {
        ensureSystemUserExists(connection, username, "testvolunteerdao");
        String sqlVolunteer = "INSERT OR IGNORE INTO Volunteer (username, fullName, cccd, dateOfBirth, freeHourPerWeek) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sqlVolunteer)) {
            pstmt.setString(1, username);
            pstmt.setString(2, fullName);
            pstmt.setString(3, "CCCDVOL-DAO-" + username);
            pstmt.setString(4, "1998-01-01");
            pstmt.setInt(5, 10); // Default free hours
            pstmt.executeUpdate();
        }
    }


    // Helper để chèn Event vào CSDL (Tương tự như trong TestEventController, nhưng giờ nó là một helper chung)
    private int insertTestEvent(String title, String startDateStr, String endDateStr,
                                String emergencyLevel, String status, Integer maxParticipants, 
                                String organizerUsername, String description, String requestIdStr) throws SQLException {
        // Đảm bảo organizerUsername tồn tại
        ensureVolunteerOrganizationExists(connForHelpers, organizerUsername, "Org for " + title);
        // Nếu requestIdStr không null, đảm bảo nó tồn tại trong HelpRequest (cần thêm helper cho HelpRequest nếu phức tạp)

        String sql = "INSERT INTO Events (title, startDate, endDate, emergencyLevel, status, maxParticipantNumber, organizer, description, requestId) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connForHelpers.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, title);
            pstmt.setString(2, startDateStr);
            pstmt.setString(3, endDateStr);
            pstmt.setString(4, emergencyLevel);
            pstmt.setString(5, status);
            if (maxParticipants != null) pstmt.setInt(6, maxParticipants); else pstmt.setNull(6, java.sql.Types.INTEGER);
            pstmt.setString(7, organizerUsername);
            pstmt.setString(8, description);
            // Chú ý: requestId trong bảng Events của bạn là TEXT, không phải INTEGER
            if (requestIdStr != null) pstmt.setString(9, requestIdStr); else pstmt.setNull(9, java.sql.Types.VARCHAR);
            
            pstmt.executeUpdate();
            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            } else {
                throw new SQLException("Creating event failed for: " + title + ", no ID obtained.");
            }
        }
    }
    
    // Phiên bản rút gọn hơn của insertTestEvent
    private int insertTestEvent(String title, String startDateStr, String status, Integer maxParticipants, String organizerUsername) throws SQLException, ParseException {
        String defaultEndDate = getFutureDateStringFromDate(dateFormat.parse(startDateStr), 7); // Mặc định 7 ngày sau
        String defaultEmergency = "Bình thường";
        String defaultDescription = "Description for " + title;
        return insertTestEvent(title, startDateStr, defaultEndDate, defaultEmergency, status, maxParticipants, organizerUsername, defaultDescription, null);
    }


    private void addSkillToEvent(int eventId, String skillName) throws SQLException {
        int skillId = insertSkillIfNotExists(connForHelpers, skillName);
        String sql = "INSERT INTO EventSkills (eventId, skillId) VALUES (?, ?)"; // Sửa eventID thành eventId cho nhất quán
        try (PreparedStatement pstmt = connForHelpers.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            pstmt.setInt(2, skillId);
            pstmt.executeUpdate();
        }
    }
    
    private void addParticipantToEvent(int eventId, String volunteerUsername, String volunteerFullName) throws SQLException {
        ensureVolunteerExists(connForHelpers, volunteerUsername, volunteerFullName); // Đảm bảo volunteer tồn tại
        String sql = "INSERT INTO EventParticipants (eventId, username, hoursParticipated, ratingByOrg) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connForHelpers.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            pstmt.setString(2, volunteerUsername);
            pstmt.setNull(3, java.sql.Types.INTEGER); // Giờ tham gia ban đầu là null
            pstmt.setNull(4, java.sql.Types.INTEGER); // Rating ban đầu là null
            pstmt.executeUpdate();
        }
    }

    private String getFutureDateString(int daysToAdd) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, daysToAdd);
        return dateFormat.format(cal.getTime());
    }
    
    private String getPastDateString(int daysToSubtract) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract);
        return dateFormat.format(cal.getTime());
    }
    
    private String getFutureDateStringFromDate(Date startDate, int daysToAdd) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        cal.add(Calendar.DAY_OF_YEAR, daysToAdd);
        return dateFormat.format(cal.getTime());
    }

    // --- Test Cases for EventDAO.getAllOpenAndAvailableEvents ---
    @Test
    void getAllOpenAndAvailableEvents_NoEvents_ShouldReturnEmptyList() {
        List<Event> events = eventDAO.getAllOpenAndAvailableEvents();
        assertTrue(events.isEmpty(), "Should return an empty list when no events are in the database.");
    }

    @Test
    void getAllOpenAndAvailableEvents_ShouldReturnOnlyOpenAndAvailableEvents() throws SQLException, ParseException {
        // Event 1: Approved, tương lai, còn chỗ
        insertTestEvent("Approved Future Event", getFutureDateString(5), "Approved", 10, "orgTestDAO1");
        // Event 2: Upcoming, tương lai, còn chỗ
        insertTestEvent("Upcoming Event", getFutureDateString(10), "Upcoming", 5, "orgTestDAO1");
        // Event 3: Pending, tương lai, còn chỗ
        insertTestEvent("Pending Future Event", getFutureDateString(3), "Pending", 8, "orgTestDAO1");
        
        // Event 4: Approved, đã qua
        insertTestEvent("Approved Past Event", getPastDateString(5), "Approved", 10, "orgTestDAO1");
        // Event 5: Trạng thái khác (ví dụ: Completed)
        insertTestEvent("Completed Future Event", getFutureDateString(7), "Completed", 10, "orgTestDAO1");
        // Event 6: Approved, tương lai, nhưng đã đầy
        int fullEventId = insertTestEvent("Full Future Event", getFutureDateString(2), "Approved", 1, "orgTestDAO1");
        addParticipantToEvent(fullEventId, "testVol1", "Test Volunteer 1"); // Giả sử đã có volunteer này

        List<Event> events = eventDAO.getAllOpenAndAvailableEvents();

        assertEquals(2, events.size(), "Should return 2 events (Approved, Upcoming, all future and not full).");
        assertTrue(events.stream().anyMatch(e -> e.getTitle().equals("Approved Future Event")));
        assertTrue(events.stream().anyMatch(e -> e.getTitle().equals("Upcoming Event")));
        assertFalse(events.stream().anyMatch(e -> e.getTitle().equals("Pending Future Event")));
        assertFalse(events.stream().anyMatch(e -> e.getTitle().equals("Approved Past Event")));
        assertFalse(events.stream().anyMatch(e -> e.getTitle().equals("Completed Future Event")));
        assertFalse(events.stream().anyMatch(e -> e.getTitle().equals("Full Future Event")));
    }

    @Test
    void getAllOpenAndAvailableEvents_LoadsRequiredSkillsCorrectly() throws SQLException, ParseException {
        int eventId = insertTestEvent("Event For Skills Test DAO", getFutureDateString(3), "Approved", 5, "orgTestDAOSkills");
        addSkillToEvent(eventId, "Teaching");
        addSkillToEvent(eventId, "Communication");

        int eventIdNoSkills = insertTestEvent("Event No Skills DAO", getFutureDateString(4), "Approved", 5, "orgTestDAOSkills");


        List<Event> events = eventDAO.getAllOpenAndAvailableEvents();
        
        Event eventWithSkills = events.stream()
                                    .filter(e -> e.getTitle().equals("Event For Skills Test DAO"))
                                    .findFirst().orElse(null);
        assertNotNull(eventWithSkills, "Event with skills should be found.");
        assertNotNull(eventWithSkills.getRequiredSkills(), "Required skills list should not be null.");
        assertEquals(2, eventWithSkills.getRequiredSkills().size(), "Should have 2 required skills.");
        assertTrue(eventWithSkills.getRequiredSkills().contains("Teaching"));
        assertTrue(eventWithSkills.getRequiredSkills().contains("Communication"));
        
        Event eventWithoutSkills = events.stream()
                                     .filter(e -> e.getTitle().equals("Event No Skills DAO"))
                                     .findFirst().orElse(null);
        assertNotNull(eventWithoutSkills, "Event without skills should be found.");
        assertNotNull(eventWithoutSkills.getRequiredSkills(), "Required skills list should not be null (can be empty).");
        assertTrue(eventWithoutSkills.getRequiredSkills().isEmpty(), "Should have 0 required skills.");
    }
    
    @Test
    void getAllOpenAndAvailableEvents_HandlesDateParsingCorrectly() throws SQLException {
        // Chèn một sự kiện với ngày tháng đúng định dạng
        insertTestEvent("Date Test Event Good", "2025-12-01", "2025-12-05", "Bình thường", "Approved", 10, "orgTestDAO1", "Desc", null);
        
        // Chèn một sự kiện với startDate là null (nếu CSDL cho phép)
        // Hoặc một sự kiện mà startDate không phải là ngày (để xem nó có bị bỏ qua hay gây lỗi)
        // Tuy nhiên, câu SQL trong DAO của bạn đã có date(e.startDate) >= date('now') nên nếu startDate là null hoặc sai định dạng,
        // nó có thể không được chọn. Test này chủ yếu để đảm bảo không có ParseException nếu dữ liệu đúng.

        List<Event> events = eventDAO.getAllOpenAndAvailableEvents();
        
        Event foundEvent = events.stream().filter(e -> e.getTitle().equals("Date Test Event Good")).findFirst().orElse(null);
        assertNotNull(foundEvent);
        assertNotNull(foundEvent.getStartDate()); // Kiểm tra xem ngày có được parse không
    }
}