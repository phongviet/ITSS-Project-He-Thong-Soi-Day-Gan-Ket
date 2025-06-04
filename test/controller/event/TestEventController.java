package controller.event; // Hoặc package test của bạn, ví dụ: com.example.controller

import controller.event.EventController; // Import lớp cần test
import entity.events.Event;              // Import entity Event
// Import các entity khác nếu cần cho việc tạo dữ liệu FK
import entity.users.SystemUser; 
import entity.users.VolunteerOrganization;
import entity.users.PersonInNeed;
import entity.requests.HelpRequest; // Giả sử bạn có entity này

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
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestEventController {

    private EventController eventController;
    // QUAN TRỌNG: DB_URL_FOR_TEST này PHẢI GIỐNG HỆT DB_URL trong EventController.java
    // hoặc là file CSDL mà bạn muốn các test này chạy trên đó.
    private static final String DB_URL_FOR_TEST = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db";
    private Connection connForHelpers; // Connection dùng cho các helper để chèn/xóa dữ liệu test
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @BeforeAll
    void setUpAll() throws SQLException {
        eventController = new EventController(); // EventController sẽ dùng DB_URL hardcode của nó

        // connForHelpers sẽ kết nối đến cùng CSDL mà EventController đang dùng
        connForHelpers = DriverManager.getConnection(DB_URL_FOR_TEST);
        System.out.println("TestEventController: Connected to DB for helpers: " + DB_URL_FOR_TEST);
        
        // KHÔNG gọi createTestDatabaseSchema ở đây nếu bạn đang dùng CSDL chính
        // vì nó sẽ xóa và tạo lại toàn bộ bảng.
        // Chỉ gọi nếu DB_URL_FOR_TEST là một file test riêng và bạn muốn khởi tạo schema.
    }

    @BeforeEach
    void setUpForEachTest() throws SQLException {
        System.out.println("--- Running setUpForEachTest: Clearing and Initializing Relevant Test Data ---");
        // Xóa dữ liệu liên quan đến Events, Skills, và các bảng nối của chúng
        clearEventRelatedTestData(connForHelpers); 
        // Xóa dữ liệu trong HelpRequest, PersonInNeed, VolunteerOrganization, SystemUser
        // nếu các test case cụ thể sẽ chèn dữ liệu mới vào đó và bạn muốn tránh xung đột.
        // Hoặc chỉ xóa những bản ghi cụ thể bạn tạo ra cho test.
        // Để đơn giản, chúng ta sẽ xóa và chèn lại một số dữ liệu cơ bản.
        clearUserAndRequestTestData(connForHelpers);

        insertInitialSkills(connForHelpers);

        // Chèn dữ liệu tham chiếu cơ bản cần cho hầu hết các test Events
        // Đảm bảo các 'organizer' và 'requestId' (thông qua HelpRequest) này tồn tại
        ensureSystemUserExists(connForHelpers, "org1", "test");
        ensureVolunteerOrganizationExists(connForHelpers, "org1", "Test Org 1");
        
        ensureSystemUserExists(connForHelpers, "orgS", "test");
        ensureVolunteerOrganizationExists(connForHelpers, "orgS", "Org For Skills Test");

        ensureSystemUserExists(connForHelpers, "person1", "test");
        ensurePersonInNeedExists(connForHelpers, "person1", "Person One");
        // Chèn HelpRequest nếu Event có tham chiếu đến requestId
        insertHelpRequest(connForHelpers, 1, "Sample Help Request 1", getFutureDateString(2), "Bình thường", "Test desc", "person1", "Approved");
        // (Giả sử requestId là INTEGER và là 1)
    }

    @AfterAll
    void tearDownAll() throws SQLException {
        if (connForHelpers != null && !connForHelpers.isClosed()) {
            connForHelpers.close();
            System.out.println("TestEventController: Closed DB helper connection.");
        }
    }

 // --- Các test cho getEmergencyLevelPriority (giữ nguyên từ code của bạn) ---
    @Test
    void getEmergencyLevelPriority_ValidLevels_ShouldReturnCorrectPriority() {
        assertEquals(1, eventController.getEmergencyLevelPriority("khẩn cấp"), "Khẩn cấp should be priority 1");
        assertEquals(1, eventController.getEmergencyLevelPriority("KHẨN CẤP"), "Case insensitivity for Khẩn cấp");
     
        assertEquals(2, eventController.getEmergencyLevelPriority("cao"), "Cao should be priority 2");
        // assertEquals(2, eventController.getEmergencyLevelPriority("High"), "High should be priority 2");
        assertEquals(3, eventController.getEmergencyLevelPriority("bình thường"), "Bình thường should be priority 3");
        // assertEquals(3, eventController.getEmergencyLevelPriority("Normal"), "Normal should be priority 3");
        assertEquals(4, eventController.getEmergencyLevelPriority("thấp"), "Thấp should be priority 4");
        // assertEquals(4, eventController.getEmergencyLevelPriority("Low"), "Low should be priority 4");
    }

    @Test
    void getEmergencyLevelPriority_UnknownLevel_ShouldReturnLowestPriority() {
        assertEquals(5, eventController.getEmergencyLevelPriority("không rõ"), "Unknown level should return lowest priority");
    }

    @Test
    void getEmergencyLevelPriority_NullLevel_ShouldReturnLowestPriority() {
        assertEquals(Integer.MAX_VALUE, eventController.getEmergencyLevelPriority(null), "Null level should return max int");
    }


    // --- Helper methods ---
    private void clearEventRelatedTestData(Connection connection) throws SQLException {
        String[] tablesToClear = {"EventSkills", "EventParticipants", "Events", "Skills"};
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = OFF;");
            for (String table : tablesToClear) {
                stmt.execute("DELETE FROM " + table + ";");
                try {
                    stmt.execute("DELETE FROM sqlite_sequence WHERE name='" + table + "';");
                } catch (SQLException e) { /* Bỏ qua nếu sequence không tồn tại */ }
            }
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
    }
    
    private void clearUserAndRequestTestData(Connection connection) throws SQLException {
         String[] tablesToClear = {
            // Xóa theo thứ tự FK ngược nếu các bảng này có liên kết với nhau
            // Ví dụ: Events trỏ đến HelpRequest, HelpRequest trỏ đến PersonInNeed
            // VolunteerSkills trỏ đến Volunteer...
            // Đây là ví dụ, bạn cần điều chỉnh theo FK của bạn
            "HelpRequest", // Vì Events trỏ đến đây
            "PersonInNeed", "VolunteerOrganization", // Các bảng role
            "SystemUser" // Bảng user gốc
        };
         try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = OFF;");
            for (String table : tablesToClear) {
                stmt.execute("DELETE FROM " + table + ";");
                try {
                    stmt.execute("DELETE FROM sqlite_sequence WHERE name='" + table + "';");
                } catch (SQLException e) { /* Bỏ qua */ }
            }
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
    }


    private void insertInitialSkills(Connection connection) throws SQLException {
        getSkillIdByName(connection, "Teaching");
        getSkillIdByName(connection, "First Aid");
        getSkillIdByName(connection, "Communication");
    }

    private int getSkillIdByName(Connection connection, String skillName) throws SQLException {
        String selectSql = "SELECT skillId FROM Skills WHERE skill = ?";
        try (PreparedStatement pstmtSelect = connection.prepareStatement(selectSql)) {
            pstmtSelect.setString(1, skillName);
            try (ResultSet rs = pstmtSelect.executeQuery()) {
                if (rs.next()) return rs.getInt("skillId");
            }
        }
        String insertSql = "INSERT INTO Skills (skill) VALUES (?)";
        try (PreparedStatement pstmtInsert = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmtInsert.setString(1, skillName);
            pstmtInsert.executeUpdate();
            try (ResultSet generatedKeys = pstmtInsert.getGeneratedKeys()) {
                if (generatedKeys.next()) return generatedKeys.getInt(1);
                else throw new SQLException("Creating skill failed: " + skillName);
            }
        }
    }
    
    // Helper để chèn SystemUser, đảm bảo không lỗi nếu đã tồn tại
    private void ensureSystemUserExists(Connection connection, String username, String password) throws SQLException {
        String sql = "INSERT OR IGNORE INTO SystemUser (username, password, email, phone, address) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, username + "@example.com"); // Email giả định
            pstmt.setString(4, "0123456789");           // Phone giả định
            pstmt.setString(5, "Test Address");         // Address giả định
            pstmt.executeUpdate();
        }
    }

    // Helper để chèn VolunteerOrganization, đảm bảo SystemUser tương ứng đã có
    private void ensureVolunteerOrganizationExists(Connection connection, String username, String orgName) throws SQLException {
        ensureSystemUserExists(connection, username, "testorgpass"); // Tạo SystemUser nếu chưa có
        String sql = "INSERT OR IGNORE INTO VolunteerOrganization (username, organizationName, licenseNumber, field, representative, sponsor, info) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, orgName);
            pstmt.setString(3, "LIC-" + username.toUpperCase()); // License giả định
            pstmt.setString(4, "General");                      // Field giả định
            pstmt.setString(5, "Rep-" + orgName);               // Representative giả định
            pstmt.setString(6, "Sponsor-" + username);          // Sponsor giả định
            pstmt.setString(7, "Info for " + orgName);          // Info giả định
            pstmt.executeUpdate();
        }
    }
    
    // Helper để chèn PersonInNeed
    private void ensurePersonInNeedExists(Connection connection, String username, String fullName) throws SQLException {
        ensureSystemUserExists(connection, username, "testpersonpass");
        String sql = "INSERT OR IGNORE INTO PersonInNeed (username, fullName, cccd, dateOfBirth) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, fullName);
            pstmt.setString(3, "CCCD-" + username); // CCCD giả định
            pstmt.setString(4, "2000-01-01");     // Date of birth giả định
            pstmt.executeUpdate();
        }
    }
    
    // Helper để chèn HelpRequest
    private void insertHelpRequest(Connection connection, int requestId, String title, String startDate, String emergency, String description, String personInNeedID, String status) throws SQLException {
        // Đảm bảo personInNeedID này đã tồn tại
        ensurePersonInNeedExists(connection, personInNeedID, "Person " + personInNeedID);

        String sql = "INSERT OR IGNORE INTO HelpRequest (requestId, title, startDate, emergencyLevel, description, personInNeedID, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, requestId);
            pstmt.setString(2, title);
            pstmt.setString(3, startDate);
            pstmt.setString(4, emergency);
            pstmt.setString(5, description);
            pstmt.setString(6, personInNeedID);
            pstmt.setString(7, status);
            pstmt.executeUpdate();
        }
    }


    private int insertEvent(Connection connection, String title, String startDateStr, String endDateStr,
                            String emergencyLevel, String status, Integer maxParticipants, String organizerUsername,
                            String description, Integer requestId) throws SQLException { // requestId là Integer
        // Đảm bảo organizerUsername tồn tại
        ensureVolunteerOrganizationExists(connection, organizerUsername, "Org " + organizerUsername);
        // Nếu requestId không null, đảm bảo nó tồn tại trong HelpRequest
        if (requestId != null) {
            // Bạn cần một cách để đảm bảo HelpRequest với ID này tồn tại,
            // hoặc chèn nó nếu chưa có. Tạm thời giả định nó đã được chèn trong @BeforeEach.
        }

        String sql = "INSERT INTO Events (title, startDate, endDate, emergencyLevel, status, maxParticipantNumber, organizer, description, requestId) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, title);
            pstmt.setString(2, startDateStr);
            pstmt.setString(3, endDateStr);
            pstmt.setString(4, emergencyLevel);
            pstmt.setString(5, status);
            if (maxParticipants != null) pstmt.setInt(6, maxParticipants); else pstmt.setNull(6, java.sql.Types.INTEGER);
            pstmt.setString(7, organizerUsername);
            pstmt.setString(8, description);
            if (requestId != null) pstmt.setInt(9, requestId); else pstmt.setNull(9, java.sql.Types.INTEGER);
            pstmt.executeUpdate();
            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) return generatedKeys.getInt(1); else throw new SQLException("Creating event failed for: " + title);
        }
    }
    
    // Phiên bản rút gọn của insertEvent
    private int insertEvent(Connection connection, String title, String startDateStr, 
                            String emergencyLevel, String status, Integer maxParticipants, 
                            String organizerUsername) throws SQLException, ParseException {
        String defaultEndDate = getFutureDateStringFromDate(dateFormat.parse(startDateStr), 5);
        String defaultDescription = "Test description for event: " + title;
        return insertEvent(connection, title, startDateStr, defaultEndDate, emergencyLevel, status, maxParticipants, organizerUsername, defaultDescription, null); // requestId là null
    }

    private void addSkillToEvent(Connection connection, int eventId, int skillId) throws SQLException {
        String sql = "INSERT INTO EventSkills (eventID, skillId) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            pstmt.setInt(2, skillId);
            pstmt.executeUpdate();
        }
    }
    
    private void addParticipantToEvent(Connection connection, int eventId, String volunteerUsername) throws SQLException {
        String sql = "INSERT INTO EventParticipants (eventId, username) VALUES (?, ?)"; // Giả sử chỉ cần username
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            pstmt.setString(2, volunteerUsername);
            pstmt.executeUpdate();
        }
    }

    private String getFutureDateString(int daysToAdd) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, daysToAdd);
        return dateFormat.format(cal.getTime());
    }
    
    private String getFutureDateStringFromDate(Date startDate, int daysToAdd) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        cal.add(Calendar.DAY_OF_YEAR, daysToAdd);
        return dateFormat.format(cal.getTime());
    }

    // --- Test Cases for getAllOpenAndAvailableEvents ---
    // (Giữ nguyên các test case bạn đã có, ví dụ)
    @Test
    void getAllOpenAndAvailableEvents_NoEvents_ShouldReturnEmptyList() {
        List<Event> events = eventController.getAllOpenAndAvailableEvents();
        assertTrue(events.isEmpty(), "Should be empty if no matching events.");
    }

    @Test
    void getAllOpenAndAvailableEvents_FiltersByStatus() throws SQLException, ParseException {
        // Sử dụng organizer đã được chèn trong @BeforeEach hoặc chèn mới nếu cần
        insertEvent(connForHelpers, "Approved Event", getFutureDateString(5), "Bình thường", "Approved", 10, "org1");
        insertEvent(connForHelpers, "Pending Event", getFutureDateString(6), "Cao", "Pending", 10, "org1");
        // ... các event khác ...
        List<Event> events = eventController.getAllOpenAndAvailableEvents();
        // ... assertions ...
        assertEquals(2, events.size(), "Should only return 'Approved' or 'Pending' future events that are open. Check schema for 'Coming Soon'"); 
        // Lưu ý: getAllOpenAndAvailableEvents có thể chỉ lấy 'Approved', 'Pending', 'Coming Soon'
    }

    // ... (CÁC TEST CASE KHÁC CHO getAllOpenAndAvailableEvents GIỮ NGUYÊN) ...
    // getAllOpenAndAvailableEvents_FiltersByStartDate
    // getAllOpenAndAvailableEvents_FiltersByParticipantCount
    // getAllOpenAndAvailableEvents_LoadsRequiredSkills
    @Test
    void getAllOpenAndAvailableEvents_FiltersByStartDate() throws SQLException, ParseException {
        insertEvent(connForHelpers, "Future Event Today", getFutureDateString(0), "Bình thường", "Approved", 10, "org1");
        insertEvent(connForHelpers, "Future Event Tomorrow", getFutureDateString(1), "Bình thường", "Approved", 10, "org1");
        insertEvent(connForHelpers, "Past Event Yesterday", getFutureDateString(-1), "Bình thường", "Approved", 10, "org1");

        List<Event> events = eventController.getAllOpenAndAvailableEvents();
        assertEquals(2, events.size(), "Should only return events with start date from today onwards.");
        assertFalse(events.stream().anyMatch(e -> e.getTitle().equals("Past Event Yesterday")));
    }


    @Test
    void getAllOpenAndAvailableEvents_LoadsRequiredSkills() throws SQLException, ParseException {
        int teachingId = getSkillIdByName(connForHelpers, "Teaching");
        int commId = getSkillIdByName(connForHelpers, "Communication");

        int eventWithSkillsId = insertEvent(connForHelpers, "Event With Skills Loaded", getFutureDateString(1), "Cao", "Approved", 10, "orgS");
        addSkillToEvent(connForHelpers, eventWithSkillsId, teachingId);
        addSkillToEvent(connForHelpers, eventWithSkillsId, commId);

        int eventNoSkillsId = insertEvent(connForHelpers, "Event No Skills Loaded", getFutureDateString(2), "Bình thường", "Approved", 5, "orgS");

        List<Event> events = eventController.getAllOpenAndAvailableEvents();
        
        Event foundWithSkills = events.stream().filter(e -> e.getEventId() == eventWithSkillsId).findFirst().orElse(null);
        assertNotNull(foundWithSkills);
        assertNotNull(foundWithSkills.getRequiredSkills());
        assertEquals(2, foundWithSkills.getRequiredSkills().size());
        assertTrue(foundWithSkills.getRequiredSkills().contains("Teaching"));
        assertTrue(foundWithSkills.getRequiredSkills().contains("Communication"));

        Event foundNoSkills = events.stream().filter(e -> e.getEventId() == eventNoSkillsId).findFirst().orElse(null);
        assertNotNull(foundNoSkills);
        assertNotNull(foundNoSkills.getRequiredSkills());
        assertTrue(foundNoSkills.getRequiredSkills().isEmpty());
    }
}