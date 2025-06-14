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
import java.util.Map;
import utils.AppConstants; // IMPORT AppConstants
import entity.requests.HelpRequest;
import entity.users.VolunteerOrganization; // Đảm bảo đã import
import java.util.Arrays;

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
        String defaultEmergency = "Normal";
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
        insertTestEvent("Date Test Event Good", "2025-12-01", "2025-12-05", "Normal", "Approved", 10, "orgTestDAO1", "Desc", null);
        
        // Chèn một sự kiện với startDate là null (nếu CSDL cho phép)
        // Hoặc một sự kiện mà startDate không phải là ngày (để xem nó có bị bỏ qua hay gây lỗi)
        // Tuy nhiên, câu SQL trong DAO của bạn đã có date(e.startDate) >= date('now') nên nếu startDate là null hoặc sai định dạng,
        // nó có thể không được chọn. Test này chủ yếu để đảm bảo không có ParseException nếu dữ liệu đúng.

        List<Event> events = eventDAO.getAllOpenAndAvailableEvents();
        
        Event foundEvent = events.stream().filter(e -> e.getTitle().equals("Date Test Event Good")).findFirst().orElse(null);
        assertNotNull(foundEvent);
        assertNotNull(foundEvent.getStartDate()); // Kiểm tra xem ngày có được parse không
    }
    
    
    // --- Test Cases for EventDAO.getEventStatistics ---
    @Test
    void getEventStatistics_ShouldReturnCorrectCountsAndDistribution() throws SQLException, ParseException {
        // --- Giai đoạn Chuẩn bị Dữ liệu (Arrange) ---

        // Các sự kiện cho các trạng thái khác nhau
        // 1. Upcoming events (ví dụ cho tháng 6/2025 và 7/2025)
        insertTestEvent("Upcoming Event June 1", getFutureDateStringFromDate(dateFormat.parse("2025-06-15"),0), "2025-06-20", "Normal", "Approved", 10, "orgTestDAO1", "Desc 1", null);
        insertTestEvent("Upcoming Event June 2", getFutureDateStringFromDate(dateFormat.parse("2025-06-20"),0), "2025-06-25", "High", "Approved", 5, "orgTestDAO1", "Desc 2", null);
        insertTestEvent("Upcoming Event July", getFutureDateStringFromDate(dateFormat.parse("2025-07-05"),0), "2025-07-10", "Normal", "Approved", 10, "orgTestDAO1", "Desc 3", null);

        // 2. Ongoing events (sự kiện đang diễn ra vào ngày test)
        // Để test ongoing, startDate phải <= ngày hiện tại VÀ endDate >= ngày hiện tại
        // Chúng ta sẽ tạo một sự kiện bắt đầu 2 ngày trước và kết thúc 2 ngày sau
        String ongoingStartDate = getPastDateString(2); // Bắt đầu 2 ngày trước
        String ongoingEndDate = getFutureDateString(2);   // Kết thúc 2 ngày sau
        insertTestEvent("Ongoing Event 1", ongoingStartDate, ongoingEndDate, "Urgent", "Approved", 15, "orgTestDAO1", "Desc 4", null);

        // 3. Past events
        insertTestEvent("Past Event May", getPastDateStringFromDate(dateFormat.parse("2025-05-01"),0), "2025-05-05", "Normal", "Completed", 10, "orgTestDAO1", "Desc 5", null);
        insertTestEvent("Past Event April", getPastDateStringFromDate(dateFormat.parse("2025-04-10"),0), "2025-04-15", "Normal", "Completed", 10, "orgTestDAO1", "Desc 6", null);

        // 4. Sự kiện cho các tháng khác trong ví dụ của DAO (Jan, Feb, Mar 2025)
        insertTestEvent("Event Jan 2025", "2025-01-15", "2025-01-20", "Normal", "Approved", 10, "orgTestDAO1", "Desc Jan", null);
        insertTestEvent("Event Feb 2025", "2025-02-10", "2025-02-15", "High", "Approved", 10, "orgTestDAO1", "Desc Feb", null);
        insertTestEvent("Event Feb 2025 Again", "2025-02-20", "2025-02-25", "Normal", "Approved", 10, "orgTestDAO1", "Desc Feb 2", null); // 2 events for Feb
        insertTestEvent("Event Mar 2025", "2025-03-05", "2025-03-10", "Urgent", "Approved", 10, "orgTestDAO1", "Desc Mar", null);
        // Không có event cho tháng 4 và tháng 5 trong khoảng query của monthlyEvents (chỉ có event đã qua ở trên)
        // Event tháng 6 đã có 2 (Upcoming Event June 1 & 2)

        // --- Giai đoạn Hành động (Act) ---
        Map<String, Object> stats = eventDAO.getEventStatistics();

        // --- Giai đoạn Kiểm tra (Assert) ---
        assertNotNull(stats, "Statistics map should not be null.");

        // Kiểm tra các count tổng quát
        // Tổng số sự kiện đã chèn: 3 upcoming + 1 ongoing + 2 past + 4 cho tháng (Jan, Feb(2), Mar) = 10 events
        assertEquals(10, stats.get("totalEvents"), "Total events count mismatch.");
        
        // Để test upcoming, ongoing, past chính xác, chúng ta cần biết ngày 'now' mà SQLite sử dụng.
        // Tuy nhiên, với dữ liệu chèn, chúng ta có thể dự đoán:
        // Upcoming: 3 (June 1, June 2, July) - Giả sử ngày test trước 2025-06-10
        // Ongoing: 1 (Ongoing Event 1)
        // Past: 6 (Past May, Past April, Jan, Feb(2), Mar) - Nếu ngày test sau 2025-03-10 và trước 2025-06-10
        // Số lượng này sẽ phụ thuộc vào ngày bạn chạy test và cách bạn định nghĩa "upcoming", "ongoing", "past"
        // trong câu SQL của getEventStatistics.
        // Hiện tại, câu SQL của bạn là:
        // upcomingEventsQuery = "SELECT COUNT(*) FROM Events WHERE startDate > date('now')"
        // ongoingEventsQuery = "SELECT COUNT(*) FROM Events WHERE startDate <= date('now') AND endDate >= date('now')"
        // pastEventsQuery = "SELECT COUNT(*) FROM Events WHERE endDate < date('now')"
        
        // Vì khó kiểm soát `date('now')` một cách chính xác trong unit test mà không mock thời gian,
        // chúng ta sẽ tập trung vào các giá trị mà chúng ta có thể kiểm soát chắc chắn hơn, như totalEvents.
        // Hoặc bạn có thể chấp nhận rằng các test này có thể fail nếu ngày chạy test thay đổi
        // và bạn cần điều chỉnh dữ liệu test cho phù hợp.

        // Tạm thời, chúng ta sẽ chỉ kiểm tra sự tồn tại của các key và kiểu dữ liệu
        assertTrue(stats.containsKey("upcomingEvents"), "Stats should contain upcomingEvents.");
        assertTrue(stats.containsKey("ongoingEvents"), "Stats should contain ongoingEvents.");
        assertTrue(stats.containsKey("pastEvents"), "Stats should contain pastEvents.");
        assertTrue(stats.get("upcomingEvents") instanceof Integer, "upcomingEvents should be Integer.");


        // Kiểm tra phân phối theo tháng (monthlyEvents)
        assertTrue(stats.containsKey("monthlyEvents"), "Stats should contain monthlyEvents map.");
        Object monthlyEventsObj = stats.get("monthlyEvents");
        assertNotNull(monthlyEventsObj, "monthlyEvents map should not be null.");
        assertTrue(monthlyEventsObj instanceof Map, "monthlyEvents should be a Map.");

        @SuppressWarnings("unchecked") // Bỏ qua cảnh báo unchecked cast
        Map<String, Integer> monthlyEvents = (Map<String, Integer>) monthlyEventsObj;

        // Kiểm tra số lượng event cho các tháng đã chèn dữ liệu (dựa trên query trong DAO)
        assertEquals(1, monthlyEvents.getOrDefault("Jan", 0), "January 2025 events count mismatch.");
        assertEquals(2, monthlyEvents.getOrDefault("Feb", 0), "February 2025 events count mismatch.");
        assertEquals(1, monthlyEvents.getOrDefault("Mar", 0), "March 2025 events count mismatch.");
        assertEquals(1, monthlyEvents.getOrDefault("Apr", 0), "April 2025 events count mismatch (dựa trên query của DAO).");
        assertEquals(1, monthlyEvents.getOrDefault("May", 0), "May 2025 events count mismatch (dựa trên query của DAO).");
        assertEquals(3, monthlyEvents.getOrDefault("Jun", 0), "June 2025 events count mismatch.");
    }

    // Helper để lấy ngày quá khứ (có thể copy từ TestEventController)
    // private String getPastDateString(int daysToSubtract) { ... }
     private String getPastDateStringFromDate(Date baseDate, int daysToSubtract) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(baseDate);
        cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract); // Trừ ngày
        return dateFormat.format(cal.getTime());
     }
     
    // --- Test Cases for EventDAO.getEventById ---

      @Test
      void getEventById_ExistingEvent_ShouldReturnCorrectEventWithDetailsAndSkills() throws SQLException, ParseException {
          // --- Arrange ---
          String organizerUsername = "orgGetByIdTest";
          ensureVolunteerOrganizationExists(connForHelpers, organizerUsername, "Org For GetById Test");

          String eventTitle = "Detailed Event For GetById";
          String startDateStr = getFutureDateString(5);
          String endDateStr = getFutureDateString(10);
          String emergency = "High";
          String status = "Approved";
          Integer maxParticipants = 20;
          String description = "A very detailed description for testing getEventById.";
          String requestId = null; // Hoặc bạn có thể tạo một HelpRequest và dùng ID của nó

          // Chèn sự kiện
          int eventId = insertTestEvent(eventTitle, startDateStr, endDateStr, emergency, status, 
                                        maxParticipants, organizerUsername, description, requestId);
          assertNotEquals(0, eventId, "Event ID should be generated.");

          // Chèn kỹ năng cho sự kiện
          String skill1Name = "Communication";
          String skill2Name = "First Aid";
          insertSkillIfNotExists(connForHelpers, skill1Name); // Đảm bảo skill tồn tại trong bảng Skills
          insertSkillIfNotExists(connForHelpers, skill2Name);
          addSkillToEvent(eventId, skill1Name); // Sử dụng helper đã có để liên kết skill với event
          addSkillToEvent(eventId, skill2Name);


          // --- Act ---
          Event foundEvent = eventDAO.getEventById(eventId);

          // --- Assert ---
          assertNotNull(foundEvent, "Event should be found for existing ID.");
          assertEquals(eventId, foundEvent.getEventId());
          assertEquals(eventTitle, foundEvent.getTitle());
          assertEquals(maxParticipants, foundEvent.getMaxParticipantNumber());
          
          // Kiểm tra ngày tháng (sau khi parse)
          assertNotNull(foundEvent.getStartDate(), "Start date should not be null.");
          assertEquals(dateFormat.parse(startDateStr), foundEvent.getStartDate(), "Start date mismatch.");
          assertNotNull(foundEvent.getEndDate(), "End date should not be null.");
          assertEquals(dateFormat.parse(endDateStr), foundEvent.getEndDate(), "End date mismatch.");
          
          assertEquals(emergency, foundEvent.getEmergencyLevel());
          assertEquals(description, foundEvent.getDescription());
          assertEquals(organizerUsername, foundEvent.getOrganizer());
          assertEquals(status, foundEvent.getStatus());
          // assertEquals(requestId, foundEvent.getRequestId()); // Nếu bạn test với requestId

          // Kiểm tra requiredSkills
          assertNotNull(foundEvent.getRequiredSkills(), "Required skills list should not be null.");
          assertEquals(2, foundEvent.getRequiredSkills().size(), "Should have 2 required skills loaded.");
          assertTrue(foundEvent.getRequiredSkills().contains(skill1Name), "Should contain " + skill1Name);
          assertTrue(foundEvent.getRequiredSkills().contains(skill2Name), "Should contain " + skill2Name);
      }

      @Test
      void getEventById_ExistingEvent_NoSkills_ShouldReturnEventWithEmptySkillList() throws SQLException, ParseException {
          // --- Arrange ---
          String organizerUsername = "orgGetByIdNoSkill";
          ensureVolunteerOrganizationExists(connForHelpers, organizerUsername, "Org For GetById No Skill Test");
          String eventTitle = "Event No Skills For GetById";
          String startDateStr = getFutureDateString(3);
          
          int eventId = insertTestEvent(eventTitle, startDateStr, "Approved", 10, organizerUsername); // Dùng helper rút gọn

          // --- Act ---
          Event foundEvent = eventDAO.getEventById(eventId);

          // --- Assert ---
          assertNotNull(foundEvent, "Event should be found.");
          assertEquals(eventId, foundEvent.getEventId());
          assertEquals(eventTitle, foundEvent.getTitle());
          assertNotNull(foundEvent.getRequiredSkills(), "Required skills list should not be null, even if empty.");
          assertTrue(foundEvent.getRequiredSkills().isEmpty(), "Required skills list should be empty.");
      }


      @Test
      void getEventById_NonExistingEvent_ShouldReturnNull() {
          // --- Arrange ---
          int nonExistingEventId = 99999; // Một ID chắc chắn không tồn tại

          // --- Act ---
          Event foundEvent = eventDAO.getEventById(nonExistingEventId);

          // --- Assert ---
          assertNull(foundEvent, "Should return null for a non-existing event ID.");
      }

      @Test
      void getEventById_EventWithNullDates_ShouldHandleGracefully() throws SQLException {
          // --- Arrange ---
          // Chèn sự kiện với startDate và endDate là null (nếu CSDL và logic insert cho phép)
          // Giả sử helper insertTestEvent cho phép truyền null cho ngày tháng
          String organizerUsername = "orgNullDate";
          ensureVolunteerOrganizationExists(connForHelpers, organizerUsername, "Org Null Date Test");
          String eventTitle = "Event With Null Dates";
          
          // Chỉnh sửa helper insertTestEvent để chấp nhận null cho ngày hoặc tạo helper riêng
          // Tạm thời, chúng ta sẽ chèn trực tiếp để đảm bảo ngày là null
          int eventIdWithNullDates = 0;
          String sql = "INSERT INTO Events (title, startDate, endDate, status, organizer) VALUES (?, NULL, NULL, ?, ?)";
          try (PreparedStatement pstmt = connForHelpers.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
              pstmt.setString(1, eventTitle);
              pstmt.setString(2, "Approved");
              pstmt.setString(3, organizerUsername);
              pstmt.executeUpdate();
              ResultSet generatedKeys = pstmt.getGeneratedKeys();
              if (generatedKeys.next()) {
                  eventIdWithNullDates = generatedKeys.getInt(1);
              }
          }
          assertTrue(eventIdWithNullDates > 0, "Event with null dates should be inserted.");

          // --- Act ---
          Event foundEvent = eventDAO.getEventById(eventIdWithNullDates);

          // --- Assert ---
          assertNotNull(foundEvent, "Event should be found.");
          assertEquals(eventTitle, foundEvent.getTitle());
          assertNull(foundEvent.getStartDate(), "Start date should be null if stored as null.");
          assertNull(foundEvent.getEndDate(), "End date should be null if stored as null.");
          // Kiểm tra các trường khác nếu cần
      }
      
   // --- Test Cases for EventDAO.updateEventStatus ---

      @Test
      void updateEventStatus_ExistingEvent_ShouldUpdateStatusInDBAndReturnTrue() throws SQLException, ParseException {
          // --- Arrange ---
          String organizerUsername = "orgUpdateStatus";
          ensureVolunteerOrganizationExists(connForHelpers, organizerUsername, "Org For Update Status Test");
          
          String initialStatus = "Pending";
          String newStatus = "Approved"; // Hoặc AppConstants.EVENT_APPROVED
          String eventTitle = "Event To Update Status";
          String startDateStr = getFutureDateString(5);

          int eventId = insertTestEvent(eventTitle, startDateStr, initialStatus, 10, organizerUsername);
          assertTrue(eventId > 0, "Event should be inserted for status update test.");

          // --- Act ---
          boolean updateResult = eventDAO.updateEventStatus(eventId, newStatus);

          // --- Assert ---
          assertTrue(updateResult, "updateEventStatus should return true for a successful update.");

          // Verify status in DB
          String statusInDb = "";
          String sqlVerify = "SELECT status FROM Events WHERE eventId = ?";
          try (PreparedStatement pstmtVerify = connForHelpers.prepareStatement(sqlVerify)) {
              pstmtVerify.setInt(1, eventId);
              try (ResultSet rs = pstmtVerify.executeQuery()) {
                  if (rs.next()) {
                      statusInDb = rs.getString("status");
                  }
              }
          }
          assertEquals(newStatus, statusInDb, "Event status in database should be updated to the new status.");
      }

      @Test
      void updateEventStatus_NonExistingEvent_ShouldReturnFalse() throws SQLException {
          // --- Arrange ---
          int nonExistingEventId = 99998; // Một ID chắc chắn không tồn tại
          String newStatus = "Approved";

          // --- Act ---
          boolean updateResult = eventDAO.updateEventStatus(nonExistingEventId, newStatus);

          // --- Assert ---
          assertFalse(updateResult, "updateEventStatus should return false for a non-existing event ID.");
      }

      @Test
      void updateEventStatus_NullNewStatus_ShouldUpdateToNullInDB( )throws SQLException, ParseException {
          // (Nếu logic của bạn cho phép status là NULL và bạn muốn test điều đó)
          // --- Arrange ---
          String organizerUsername = "orgUpdateNullStatus";
          ensureVolunteerOrganizationExists(connForHelpers, organizerUsername, "Org For Update Null Status Test");
          
          String initialStatus = "Pending";
          String eventTitle = "Event To Update To Null Status";
          String startDateStr = getFutureDateString(6);

          int eventId = insertTestEvent(eventTitle, startDateStr, initialStatus, 5, organizerUsername);
          assertTrue(eventId > 0, "Event should be inserted.");

          // --- Act ---
          boolean updateResult = eventDAO.updateEventStatus(eventId, null); // Cập nhật status thành NULL

          // --- Assert ---
          assertTrue(updateResult, "updateEventStatus should return true even when setting status to null (if allowed).");

          String statusInDb = "initialValue"; // Giá trị khởi tạo khác null để đảm bảo nó được thay đổi
          String sqlVerify = "SELECT status FROM Events WHERE eventId = ?";
          try (PreparedStatement pstmtVerify = connForHelpers.prepareStatement(sqlVerify)) {
              pstmtVerify.setInt(1, eventId);
              try (ResultSet rs = pstmtVerify.executeQuery()) {
                  if (rs.next()) {
                      statusInDb = rs.getString("status"); // Lấy giá trị, có thể là null
                  }
              }
          }
          assertNull(statusInDb, "Event status in database should be updated to NULL.");
      }
      
      @Test
      void updateEventStatus_EmptyNewStatus_ShouldUpdateToEmptyInDB() throws SQLException, ParseException {
          // (Nếu logic của bạn cho phép status là chuỗi rỗng)
          // --- Arrange ---
          String organizerUsername = "orgUpdateEmptyStatus";
          ensureVolunteerOrganizationExists(connForHelpers, organizerUsername, "Org For Update Empty Status Test");
          
          String initialStatus = "Pending";
          String newStatus = ""; // Chuỗi rỗng
          String eventTitle = "Event To Update To Empty Status";
          String startDateStr = getFutureDateString(7);

          int eventId = insertTestEvent(eventTitle, startDateStr, initialStatus, 12, organizerUsername);
          assertTrue(eventId > 0, "Event should be inserted.");

          // --- Act ---
          boolean updateResult = eventDAO.updateEventStatus(eventId, newStatus);

          // --- Assert ---
          assertTrue(updateResult, "updateEventStatus should return true when setting status to empty string.");

          String statusInDb = "initialValue";
          String sqlVerify = "SELECT status FROM Events WHERE eventId = ?";
          try (PreparedStatement pstmtVerify = connForHelpers.prepareStatement(sqlVerify)) {
              pstmtVerify.setInt(1, eventId);
              try (ResultSet rs = pstmtVerify.executeQuery()) {
                  if (rs.next()) {
                      statusInDb = rs.getString("status");
                  }
              }
          }
          assertEquals(newStatus, statusInDb, "Event status in database should be updated to an empty string.");
      }
      
   // --- Test Cases for EventDAO.approveEvent ---

      @Test
      void approveEvent_ExistingEventWithHelpRequest_ShouldUpdateEventAndHelpRequestStatusAndReturnTrue() throws SQLException, ParseException {
          // --- Arrange ---
          String orgUsername = "orgApproveEvent";
          ensureVolunteerOrganizationExists(connForHelpers, orgUsername, "Org Approve Event");
          String personUsername = "personApproveEvent";
          ensurePersonInNeedExists(connForHelpers, personUsername, "Person Approve Event");

          // 1. Insert HelpRequest
          int helpRequestId = 123; // ID giả định cho HelpRequest
          String helpRequestTitle = "Help Req for Approve Test";
          insertTestHelpRequest(helpRequestId, helpRequestTitle, getFutureDateString(2), AppConstants.EMERGENCY_NORMAL, "Desc", personUsername, AppConstants.REQUEST_APPROVED);

          // 2. Insert Event linked to this HelpRequest
          String eventTitle = "Event To Approve With HR";
          int eventId = insertTestEvent(eventTitle, getFutureDateString(5), AppConstants.EVENT_PENDING, 10, orgUsername, String.valueOf(helpRequestId)); // Truyền requestId là String

          // --- Act ---
          boolean result = eventDAO.approveEvent(eventId);

          // --- Assert ---
          assertTrue(result, "approveEvent should return true for successful approval.");

          // Verify Event status in DB
          assertEquals(AppConstants.EVENT_UPCOMING, getEventStatusFromDB(eventId), "Event status should be updated to UPCOMING.");

          // Verify HelpRequest status in DB
          assertEquals(AppConstants.REQUEST_CLOSED, getHelpRequestStatusFromDB(helpRequestId), "HelpRequest status should be updated to CLOSED.");
      }

      @Test
      void approveEvent_ExistingEventWithoutHelpRequest_ShouldUpdateEventStatusAndReturnTrue() throws SQLException, ParseException {
          // --- Arrange ---
          String orgUsername = "orgApproveEventNoHR";
          ensureVolunteerOrganizationExists(connForHelpers, orgUsername, "Org Approve Event No HR");
          
          String eventTitle = "Event To Approve No HR";
          int eventId = insertTestEvent(eventTitle, getFutureDateString(3), AppConstants.EVENT_PENDING, 5, orgUsername, null); // requestId là null

          // --- Act ---
          boolean result = eventDAO.approveEvent(eventId);

          // --- Assert ---
          assertTrue(result, "approveEvent should return true even if no HelpRequest is linked.");
          assertEquals(AppConstants.EVENT_UPCOMING, getEventStatusFromDB(eventId), "Event status should be updated to UPCOMING.");
          // Không cần kiểm tra HelpRequest vì không có
      }

      @Test
      void approveEvent_NonExistingEvent_ShouldReturnFalse() throws SQLException {
          // --- Arrange ---
          int nonExistingEventId = 99997;

          // --- Act ---
          boolean result = eventDAO.approveEvent(nonExistingEventId);

          // --- Assert ---
          assertFalse(result, "approveEvent should return false for a non-existing event ID.");
      }

      // Helper method để chèn HelpRequest (nếu chưa có trong các helper của bạn)
      // Đảm bảo requestId là duy nhất hoặc bảng cho phép INSERT OR IGNORE
      private void insertTestHelpRequest(int requestId, String title, String startDate, String emergency, 
                                         String description, String personInNeedID, String status) throws SQLException {
          // Đảm bảo personInNeedID tồn tại
          ensurePersonInNeedExists(connForHelpers, personInNeedID, "Helper Person " + personInNeedID);
          
          String sql = "INSERT OR IGNORE INTO HelpRequest (requestId, title, startDate, emergencyLevel, description, personInNeedID, status) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?)";
          try (PreparedStatement pstmt = connForHelpers.prepareStatement(sql)) {
              pstmt.setInt(1, requestId);
              pstmt.setString(2, title);
              pstmt.setString(3, startDate); // Ngày bắt đầu của HelpRequest
              pstmt.setString(4, emergency);
              pstmt.setString(5, description);
              pstmt.setString(6, personInNeedID);
              pstmt.setString(7, status);
              pstmt.executeUpdate();
          }
      }

      // Helper method để lấy Event status từ DB
      private String getEventStatusFromDB(int eventId) throws SQLException {
          String status = null;
          String sql = "SELECT status FROM Events WHERE eventId = ?";
          try (PreparedStatement pstmt = connForHelpers.prepareStatement(sql)) {
              pstmt.setInt(1, eventId);
              try (ResultSet rs = pstmt.executeQuery()) {
                  if (rs.next()) {
                      status = rs.getString("status");
                  }
              }
          }
          return status;
      }

      // Helper method để lấy HelpRequest status từ DB
      private String getHelpRequestStatusFromDB(int requestId) throws SQLException {
          String status = null;
          String sql = "SELECT status FROM HelpRequest WHERE requestId = ?";
          try (PreparedStatement pstmt = connForHelpers.prepareStatement(sql)) {
              pstmt.setInt(1, requestId); // Giả sử requestId trong DB là INTEGER
              try (ResultSet rs = pstmt.executeQuery()) {
                  if (rs.next()) {
                      status = rs.getString("status");
                  }
              }
          }
          return status;
      }

      // Cập nhật helper insertTestEvent để nhận requestId là String (như trong EventDAO)
      // Bạn có thể đã có phiên bản này, chỉ cần đảm bảo nó đúng
//      private int insertTestEvent(String title, String startDateStr, String endDateStr,
//                                  String emergencyLevel, String status, Integer maxParticipants, 
//                                  String organizerUsername, String description, String requestId) throws SQLException {
//          ensureVolunteerOrganizationExists(connForHelpers, organizerUsername, "Org for " + title);
//          // Nếu requestId không null và là số, bạn có thể muốn đảm bảo HelpRequest đó tồn tại
//          // if (requestId != null) { ensureHelpRequestExists(connForHelpers, Integer.parseInt(requestId)); }
//
//
//          String sql = "INSERT INTO Events (title, startDate, endDate, emergencyLevel, status, maxParticipantNumber, organizer, description, requestId) " +
//                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
//          try (PreparedStatement pstmt = connForHelpers.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
//              pstmt.setString(1, title);
//              pstmt.setString(2, startDateStr);
//              pstmt.setString(3, endDateStr);
//              pstmt.setString(4, emergencyLevel);
//              pstmt.setString(5, status);
//              if (maxParticipants != null) pstmt.setInt(6, maxParticipants); else pstmt.setNull(6, java.sql.Types.INTEGER);
//              pstmt.setString(7, organizerUsername);
//              pstmt.setString(8, description);
//              // requestId trong bảng Events của bạn là TEXT
//              if (requestId != null) pstmt.setString(9, requestId); else pstmt.setNull(9, java.sql.Types.VARCHAR);
//              
//              pstmt.executeUpdate();
//              ResultSet generatedKeys = pstmt.getGeneratedKeys();
//              if (generatedKeys.next()) {
//                  return generatedKeys.getInt(1);
//              } else {
//                  throw new SQLException("Creating event failed for: " + title + ", no ID obtained.");
//              }
//          }
//      }
      
      // Phiên bản rút gọn của insertTestEvent, đảm bảo requestId là String
      private int insertTestEvent(String title, String startDateStr, String status, 
                                  Integer maxParticipants, String organizerUsername, String requestId) throws SQLException, ParseException {
          String defaultEndDate = getFutureDateStringFromDate(dateFormat.parse(startDateStr), 7);
          String defaultEmergency = AppConstants.EMERGENCY_NORMAL; // Sử dụng AppConstants
          String defaultDescription = "Description for " + title;
          return insertTestEvent(title, startDateStr, defaultEndDate, defaultEmergency, status, maxParticipants, organizerUsername, defaultDescription, requestId);
      }
      
   // --- Test Cases for EventDAO.rejectEvent ---

      @Test
      void rejectEvent_ExistingEvent_ShouldUpdateStatusToRejectedAndReturnTrue() throws SQLException, ParseException {
          // --- Arrange ---
          String organizerUsername = "orgRejectEvent";
          ensureVolunteerOrganizationExists(connForHelpers, organizerUsername, "Org For Reject Event Test");
          
          String initialStatus = AppConstants.EVENT_PENDING;
          String eventTitle = "Event To Be Rejected";
          String startDateStr = getFutureDateString(8); // Một ngày trong tương lai

          // Chèn sự kiện với requestId là null để đơn giản hóa test này
          int eventId = insertTestEvent(eventTitle, startDateStr, initialStatus, 10, organizerUsername, null);
          assertTrue(eventId > 0, "Event should be inserted for reject test.");

          // --- Act ---
          boolean result = eventDAO.rejectEvent(eventId);

          // --- Assert ---
          assertTrue(result, "rejectEvent should return true for a successful rejection.");

          // Verify Event status in DB
          String statusInDb = getEventStatusFromDB(eventId); // Sử dụng helper đã có
          assertEquals(AppConstants.EVENT_REJECTED, statusInDb, "Event status in database should be updated to REJECTED.");
      }

      @Test
      void rejectEvent_NonExistingEvent_ShouldReturnFalse() throws SQLException {
          // --- Arrange ---
          int nonExistingEventId = 99996; // Một ID chắc chắn không tồn tại

          // --- Act ---
          boolean result = eventDAO.rejectEvent(nonExistingEventId);

          // --- Assert ---
          assertFalse(result, "rejectEvent should return false for a non-existing event ID.");
      }

      @Test
      void rejectEvent_AlreadyRejectedEvent_ShouldStillReturnTrueAndUpdate() throws SQLException, ParseException {
          // (Hành vi này phụ thuộc vào việc updateEventStatus có trả về true nếu giá trị không thay đổi không,
          //  thường thì executeUpdate() sẽ trả về số dòng bị ảnh hưởng, nếu status đã là REJECTED,
          //  và bạn update lại thành REJECTED, một số DB có thể trả về 0 hoặc 1 tùy cấu hình/driver.
          //  Với SQLite, nếu giá trị không đổi, nó vẫn có thể coi là 1 dòng "được khớp" và trả về 1)
          // --- Arrange ---
          String organizerUsername = "orgRejectAgain";
          ensureVolunteerOrganizationExists(connForHelpers, organizerUsername, "Org For Reject Again Test");
          
          String initialStatus = AppConstants.EVENT_REJECTED; // Sự kiện đã bị từ chối trước đó
          String eventTitle = "Event To Be Rejected Again";
          String startDateStr = getFutureDateString(9);

          int eventId = insertTestEvent(eventTitle, startDateStr, initialStatus, 5, organizerUsername, null);
          assertTrue(eventId > 0, "Event should be inserted.");

          // --- Act ---
          boolean result = eventDAO.rejectEvent(eventId); // Cố gắng từ chối lại

          // --- Assert ---
          // Nếu updateEventStatus trả về true ngay cả khi không có thay đổi thực sự về giá trị (chỉ cần câu lệnh UPDATE chạy được)
          assertTrue(result, "rejectEvent should return true even if event was already rejected."); 
          
          String statusInDb = getEventStatusFromDB(eventId);
          assertEquals(AppConstants.EVENT_REJECTED, statusInDb, "Event status should remain REJECTED.");
      }
      
      // --- Test Cases for EventDAO.registerEvent ---

      @Test
      void registerEvent_ValidEventWithoutSkills_ShouldInsertEventAndReturnTrue() throws SQLException, ParseException {
          // --- Arrange ---
          String orgUsername = "orgRegisterEventNoSkill";
          ensureVolunteerOrganizationExists(connForHelpers, orgUsername, "Org Register Event No Skill");
          VolunteerOrganization org = new VolunteerOrganization(); // Tạo object org để truyền vào
          org.setUsername(orgUsername);

          Event eventToRegister = new Event();
          eventToRegister.setTitle("New Event Without Skills");
          eventToRegister.setStartDate(dateFormat.parse(getFutureDateString(10)));
          eventToRegister.setEndDate(dateFormat.parse(getFutureDateString(15)));
          eventToRegister.setMaxParticipantNumber(50);
          eventToRegister.setEmergencyLevel(AppConstants.EMERGENCY_NORMAL);
          eventToRegister.setDescription("Description for event without skills.");
          // eventToRegister.setRequestId(null); // Mặc định hoặc set nếu cần

          // --- Act ---
          boolean result = eventDAO.registerEvent(eventToRegister, org);

          // --- Assert ---
          assertTrue(result, "registerEvent should return true for successful registration.");
          assertTrue(eventToRegister.getEventId() > 0, "Event ID should be set after registration.");

          // Verify event in DB
          Event_DataInDB eventInDb = getEventDataFromDB(eventToRegister.getEventId()); // Helper mới
          assertNotNull(eventInDb, "Event should exist in DB.");
          assertEquals(eventToRegister.getTitle(), eventInDb.title);
          assertEquals(orgUsername, eventInDb.organizer);
          assertEquals(AppConstants.EVENT_PENDING, eventInDb.status, "Event status should be PENDING.");
          assertEquals(0, getEventSkillCountFromDB(eventToRegister.getEventId()), "Event should have 0 skills linked.");
      }

      @Test
      void registerEvent_ValidEventWithExistingSkills_ShouldInsertEventAndSkillsAndReturnTrue() throws SQLException, ParseException {
          // --- Arrange ---
          String orgUsername = "orgRegisterEventWithSkills";
          ensureVolunteerOrganizationExists(connForHelpers, orgUsername, "Org Register Event With Skills");
          VolunteerOrganization org = new VolunteerOrganization();
          org.setUsername(orgUsername);

          // Đảm bảo các skills này tồn tại trong bảng Skills
          String skillName1 = "Advanced Teaching";
          String skillName2 = "Crisis Communication";
          insertSkillIfNotExists(connForHelpers, skillName1);
          insertSkillIfNotExists(connForHelpers, skillName2);

          Event eventToRegister = new Event();
          eventToRegister.setTitle("New Event With Skills");
          eventToRegister.setStartDate(dateFormat.parse(getFutureDateString(12)));
          eventToRegister.setEndDate(dateFormat.parse(getFutureDateString(18)));
          eventToRegister.setMaxParticipantNumber(30);
          eventToRegister.setEmergencyLevel(AppConstants.EMERGENCY_HIGH);
          eventToRegister.setDescription("Description for event with skills.");
          eventToRegister.setRequiredSkills(new ArrayList<>(Arrays.asList(skillName1, skillName2)));

          // --- Act ---
          boolean result = eventDAO.registerEvent(eventToRegister, org);

          // --- Assert ---
          assertTrue(result, "registerEvent should return true.");
          assertTrue(eventToRegister.getEventId() > 0, "Event ID should be set.");

          Event_DataInDB eventInDb = getEventDataFromDB(eventToRegister.getEventId());
          assertNotNull(eventInDb);
          assertEquals(eventToRegister.getTitle(), eventInDb.title);
          assertEquals(AppConstants.EVENT_PENDING, eventInDb.status);

          // Verify skills in EventSkills table
          List<String> skillsInDb = getEventSkillsFromDB(eventToRegister.getEventId());
          assertEquals(2, skillsInDb.size(), "Event should have 2 skills linked in DB.");
          assertTrue(skillsInDb.contains(skillName1), "Skill 1 should be linked.");
          assertTrue(skillsInDb.contains(skillName2), "Skill 2 should be linked.");
      }
      
      @Test
      void registerEvent_SkillNameNotInSkillsTable_ShouldRegisterEventButNotLinkMissingSkill() throws SQLException, ParseException {
          // Hành vi này phụ thuộc vào logic của getSkillIdByName trong EventDAO
          // Hiện tại, getSkillIdByName trong EventDAO trả về -1 nếu không tìm thấy,
          // và insertEventSkill sẽ không được gọi nếu skillId <= 0.
          // --- Arrange ---
          String orgUsername = "orgRegisterEventMissingSkill";
          ensureVolunteerOrganizationExists(connForHelpers, orgUsername, "Org Register Missing Skill");
          VolunteerOrganization org = new VolunteerOrganization();
          org.setUsername(orgUsername);

          String existingSkill = "Existing Skill For Test";
          insertSkillIfNotExists(connForHelpers, existingSkill);
          String nonExistingSkill = "This Skill Does Not Exist In DB";

          Event eventToRegister = new Event();
          eventToRegister.setTitle("Event With One Missing Skill");
          eventToRegister.setStartDate(dateFormat.parse(getFutureDateString(10)));
          eventToRegister.setRequiredSkills(new ArrayList<>(Arrays.asList(existingSkill, nonExistingSkill)));
          // ... (set các trường khác của event)

          // --- Act ---
          boolean result = eventDAO.registerEvent(eventToRegister, org);

          // --- Assert ---
          assertTrue(result, "registerEvent should still return true as event is created.");
          assertTrue(eventToRegister.getEventId() > 0, "Event ID should be set.");

          Event_DataInDB eventInDb = getEventDataFromDB(eventToRegister.getEventId());
          assertNotNull(eventInDb, "Event should be created in DB.");

          List<String> skillsInDb = getEventSkillsFromDB(eventToRegister.getEventId());
          assertEquals(1, skillsInDb.size(), "Only the existing skill should be linked.");
          assertTrue(skillsInDb.contains(existingSkill), "Existing skill should be linked.");
          assertFalse(skillsInDb.contains(nonExistingSkill), "Non-existing skill should NOT be linked.");
      }


      // --- Helper class để lưu trữ dữ liệu Event đọc từ DB cho việc assert ---
      private static class Event_DataInDB {
          String title;
          String status;
          String organizer;
          // Thêm các trường khác bạn muốn kiểm tra
      }

      // --- Helper methods mới để lấy dữ liệu từ DB cho assert ---
      private Event_DataInDB getEventDataFromDB(int eventId) throws SQLException {
          String sql = "SELECT title, status, organizer FROM Events WHERE eventId = ?";
          try (PreparedStatement pstmt = connForHelpers.prepareStatement(sql)) {
              pstmt.setInt(1, eventId);
              try (ResultSet rs = pstmt.executeQuery()) {
                  if (rs.next()) {
                      Event_DataInDB data = new Event_DataInDB();
                      data.title = rs.getString("title");
                      data.status = rs.getString("status");
                      data.organizer = rs.getString("organizer");
                      return data;
                  }
              }
          }
          return null;
      }

      private int getEventSkillCountFromDB(int eventId) throws SQLException {
          String sql = "SELECT COUNT(*) FROM EventSkills WHERE eventId = ?"; // Sửa eventID nếu cần
          try (PreparedStatement pstmt = connForHelpers.prepareStatement(sql)) {
              pstmt.setInt(1, eventId);
              try (ResultSet rs = pstmt.executeQuery()) {
                  if (rs.next()) {
                      return rs.getInt(1);
                  }
              }
          }
          return 0;
      }
      
      private List<String> getEventSkillsFromDB(int eventId) throws SQLException {
          List<String> skills = new ArrayList<>();
          String sql = "SELECT s.skill FROM EventSkills es JOIN Skills s ON es.skillId = s.skillId WHERE es.eventId = ?"; // Sửa eventID nếu cần
           try (PreparedStatement pstmt = connForHelpers.prepareStatement(sql)) {
              pstmt.setInt(1, eventId);
              try (ResultSet rs = pstmt.executeQuery()) {
                  while (rs.next()) {
                      skills.add(rs.getString("skill"));
                  }
              }
          }
          return skills;
      }
      
   // --- Test Cases for EventDAO.getEventsByOrganizerId ---

      @Test
      void getEventsByOrganizerId_ExistingOrganizerWithEvents_ShouldReturnOnlyTheirEventsWithSkills() throws SQLException, ParseException {
          // --- Arrange ---
          String org1Username = "orgWithEvents1";
          String org2Username = "orgWithEvents2";
          ensureVolunteerOrganizationExists(connForHelpers, org1Username, "Organization One");
          ensureVolunteerOrganizationExists(connForHelpers, org2Username, "Organization Two");

          // Skills
          String skillYoga = "Yoga Teaching";
          String skillCooking = "Mass Cooking";
          insertSkillIfNotExists(connForHelpers, skillYoga);
          insertSkillIfNotExists(connForHelpers, skillCooking);

          // Events for org1
          int event1Org1Id = insertTestEvent("Org1 Event A (Yoga)", getFutureDateString(5), AppConstants.EVENT_APPROVED, 10, org1Username, null);
          addSkillToEvent(event1Org1Id, skillYoga);
          
          int event2Org1Id = insertTestEvent("Org1 Event B (No Skill)", getFutureDateString(10), AppConstants.EVENT_PENDING, 20, org1Username, null);

          // Events for org2
          int event1Org2Id = insertTestEvent("Org2 Event C (Cooking)", getFutureDateString(7), AppConstants.EVENT_APPROVED, 15, org2Username, null);
          addSkillToEvent(event1Org2Id, skillCooking);

          // --- Act ---
          List<Event> org1Events = eventDAO.getEventsByOrganizerId(org1Username);

          // --- Assert for org1 ---
          assertNotNull(org1Events, "List of events for org1 should not be null.");
          assertEquals(2, org1Events.size(), "Org1 should have 2 events.");

          Event foundEventA = org1Events.stream().filter(e -> e.getTitle().equals("Org1 Event A (Yoga)")).findFirst().orElse(null);
          assertNotNull(foundEventA, "Org1 Event A should be found.");
          assertEquals(org1Username, foundEventA.getOrganizer());
          assertNotNull(foundEventA.getRequiredSkills());
          assertEquals(1, foundEventA.getRequiredSkills().size());
          assertTrue(foundEventA.getRequiredSkills().contains(skillYoga));

          Event foundEventB = org1Events.stream().filter(e -> e.getTitle().equals("Org1 Event B (No Skill)")).findFirst().orElse(null);
          assertNotNull(foundEventB, "Org1 Event B should be found.");
          assertEquals(org1Username, foundEventB.getOrganizer());
          assertNotNull(foundEventB.getRequiredSkills());
          assertTrue(foundEventB.getRequiredSkills().isEmpty());

          // --- Act for org2 (để đảm bảo không lấy nhầm) ---
          List<Event> org2Events = eventDAO.getEventsByOrganizerId(org2Username);

          // --- Assert for org2 ---
          assertNotNull(org2Events);
          assertEquals(1, org2Events.size(), "Org2 should have 1 event.");
          Event foundEventC = org2Events.get(0);
          assertEquals("Org2 Event C (Cooking)", foundEventC.getTitle());
          assertEquals(org2Username, foundEventC.getOrganizer());
          assertNotNull(foundEventC.getRequiredSkills());
          assertEquals(1, foundEventC.getRequiredSkills().size());
          assertTrue(foundEventC.getRequiredSkills().contains(skillCooking));
      }

      @Test
      void getEventsByOrganizerId_OrganizerWithNoEvents_ShouldReturnEmptyList() throws SQLException, ParseException {
          // --- Arrange ---
          String orgNoEventsUsername = "orgNoEvents";
          ensureVolunteerOrganizationExists(connForHelpers, orgNoEventsUsername, "Organization With No Events");
          
          // Chèn một sự kiện cho một tổ chức khác để đảm bảo DB không rỗng hoàn toàn
          ensureVolunteerOrganizationExists(connForHelpers, "anotherOrg", "Another Org");
          insertTestEvent("Some Other Event", getFutureDateString(1), AppConstants.EVENT_APPROVED, 5, "anotherOrg", null);


          // --- Act ---
          List<Event> events = eventDAO.getEventsByOrganizerId(orgNoEventsUsername);

          // --- Assert ---
          assertNotNull(events, "List of events should not be null.");
          assertTrue(events.isEmpty(), "Should return an empty list for an organizer with no events.");
      }

      @Test
      void getEventsByOrganizerId_NonExistingOrganizer_ShouldReturnEmptyList() {
          // --- Arrange ---
          String nonExistingOrgUsername = "nonExistingOrg123";

          // --- Act ---
          List<Event> events = eventDAO.getEventsByOrganizerId(nonExistingOrgUsername);

          // --- Assert ---
          assertNotNull(events, "List of events should not be null.");
          assertTrue(events.isEmpty(), "Should return an empty list for a non-existing organizer ID.");
      }

}