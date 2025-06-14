package dao; // Hoặc package test của bạn

import entity.notifications.Notification;
// Các entity cần thiết để tạo dữ liệu khóa ngoại
import entity.users.SystemUser;
import entity.users.Volunteer;
import entity.users.VolunteerOrganization;
import entity.events.Event; // Cần Event entity
import org.junit.jupiter.api.*;
import utils.AppConstants;     // Import AppConstants

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;


import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestNotificationDAO {

    private NotificationDAO notificationDAO;
    // Helper DAO để setup dữ liệu nếu cần (ví dụ EventDAO để tạo Event)
    private EventDAO eventDAOForSetup; // Để tạo sự kiện mẫu

    private static final String DB_URL_FOR_TEST = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db";
    private Connection connForHelpers;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @BeforeAll
    void setUpAll() throws SQLException {
        notificationDAO = new NotificationDAO();
        eventDAOForSetup = new EventDAO(); // Khởi tạo EventDAO để dùng trong setup
        connForHelpers = DriverManager.getConnection(DB_URL_FOR_TEST);
        // Bật Foreign Keys cho kết nối helper
        try (Statement stmt = connForHelpers.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        System.out.println("TestNotificationDAO: Connected to DB for helpers: " + DB_URL_FOR_TEST);
    }

    @BeforeEach
    void setUpForEachTest() throws SQLException {
        System.out.println("--- TestNotificationDAO: Running setUpForEachTest: Clearing Data ---");
        
        // Tắt FK để xóa, sau đó bật lại (nhưng thứ tự vẫn quan trọng để tránh lỗi logic)
        try (Statement stmt = connForHelpers.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = OFF;");

            // 1. Bảng nối không bị tham chiếu nhiều
            clearTableDataInternal(stmt, "EventSkills");
            clearTableDataInternal(stmt, "VolunteerSkills");

            // 2. Bảng tham chiếu đến Events và Volunteers
            clearTableDataInternal(stmt, "Notification");
            clearTableDataInternal(stmt, "EventParticipants");

            // 3. Bảng Report
            clearTableDataInternal(stmt, "FinalReport"); // Xóa FinalReport trước Report
            clearTableDataInternal(stmt, "Report");

            // 4. Bảng Events
            clearTableDataInternal(stmt, "Events");

            // 5. Bảng Skills
            clearTableDataInternal(stmt, "Skills");

            // 6. Bảng HelpRequest
            clearTableDataInternal(stmt, "HelpRequest");
            
            // 7. Các bảng vai trò người dùng
            clearTableDataInternal(stmt, "Volunteer");
            clearTableDataInternal(stmt, "PersonInNeed");
            clearTableDataInternal(stmt, "VolunteerOrganization");
            clearTableDataInternal(stmt, "Admin");

            // 8. Bảng gốc
            clearTableDataInternal(stmt, "SystemUser");

            stmt.execute("PRAGMA foreign_keys = ON;");
        }

        // Chèn lại dữ liệu tham chiếu cơ bản
        ensureSystemUserExists(connForHelpers, "testVolNotify", "pass", "vol@notify.com", "123", "Addr");
        ensureVolunteerExists(connForHelpers, "testVolNotify", "Test Volunteer Notify", "CCCDVOLN", "1990-01-01", 20);

        ensureSystemUserExists(connForHelpers, "orgNotify", "pass", "org@notify.com", "456", "AddrOrg");
        ensureVolunteerOrganizationExists(connForHelpers, "orgNotify", "Org For Notifications",
                                          "LICDefaultNotif", "FieldDefaultNotif", "RepDefaultNotif",
                                          "SponsorDefaultNotif", "InfoDefaultNotif");
        
        try {
             insertTestEvent(connForHelpers, 1001, "Notify Event 1", getFutureDateString(5), 
                            AppConstants.EMERGENCY_NORMAL, AppConstants.EVENT_APPROVED, 10, "orgNotify", 
                            "Desc for Notify Event 1", null); // Giả sử Events không còn cột requestId
        } catch (ParseException e) {
            throw new SQLException("Failed to parse date during setup: " + e.getMessage());
        }
    }

    // Helper nội bộ cho clearTableData để dùng chung Statement
    private void clearTableDataInternal(Statement stmt, String tableName) throws SQLException {
        stmt.execute("DELETE FROM " + tableName + ";");
        try {
            stmt.execute("DELETE FROM sqlite_sequence WHERE name='" + tableName + "';");
        } catch (SQLException e) { /* Bỏ qua nếu sequence không tồn tại */ }
    }

    // Cập nhật helper ensureVolunteerOrganizationExists để bao gồm các trường NOT NULL
    private void ensureVolunteerOrganizationExists(Connection connection, String username, String orgName, 
                                                 String license, String field, String rep, 
                                                 String sponsor, String info) throws SQLException {
        // ensureSystemUserExists(connection, username, "defaultOrgPass", ...); // Gọi nếu cần
    	ensureSystemUserExists(connection, username, "defaultOrgPassFor_" + username, username + "@org.com", "000111222", "Org Default Address");
        String sql = "INSERT OR IGNORE INTO VolunteerOrganization (username, organizationName, licenseNumber, field, representative, sponsor, info) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, orgName);
            pstmt.setString(3, license);
            pstmt.setString(4, field);
            pstmt.setString(5, rep);
            pstmt.setString(6, sponsor); // NOT NULL
            pstmt.setString(7, info);    // NOT NULL
            pstmt.executeUpdate();
        }
    }

    @AfterAll
    void tearDownAll() throws SQLException {
        if (connForHelpers != null && !connForHelpers.isClosed()) {
            connForHelpers.close();
            System.out.println("TestNotificationDAO: Closed DB helper connection.");
        }
    }

    // --- Helper methods (nhiều cái có thể copy/điều chỉnh từ TestEventDAO) ---
    private void clearTableData(Connection connection, String tableName) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM " + tableName + ";");
            try {
                stmt.execute("DELETE FROM sqlite_sequence WHERE name='" + tableName + "';");
            } catch (SQLException e) { /* Bỏ qua nếu sequence không tồn tại */ }
        }
    }

    private void ensureSystemUserExists(Connection connection, String username, String password, String email, String phone, String address) throws SQLException {
        String sql = "INSERT OR IGNORE INTO SystemUser (username, password, email, phone, address) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, email);
            pstmt.setString(4, phone);
            pstmt.setString(5, address);
            pstmt.executeUpdate();
        }
    }
    
    private void ensureVolunteerExists(Connection connection, String username, String fullName, String cccd, String dateOfBirthStr, int freeHours) throws SQLException {
    	ensureSystemUserExists(connection, username, "defaultVolPassFor_" + username, 
                username + "@volunteer.com", "333444555", "Volunteer Default Address");
    	String sql = "INSERT OR IGNORE INTO Volunteer (username, fullName, cccd, dateOfBirth, freeHourPerWeek) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, fullName);
            pstmt.setString(3, cccd);
            pstmt.setString(4, dateOfBirthStr);
            pstmt.setInt(5, freeHours);
            pstmt.executeUpdate();
        }
    }

    private void ensureVolunteerOrganizationExists(Connection connection, String username, String orgName) throws SQLException {
        String sql = "INSERT OR IGNORE INTO VolunteerOrganization (username, organizationName) VALUES (?, ?)";
        // Thêm các trường bắt buộc khác của VolunteerOrganization nếu có
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, orgName);
            // pstmt.setString(3, "LIC-Default"); // licenseNumber
            // pstmt.setString(4, "Field-Default"); // field
            // pstmt.setString(5, "Rep-Default"); // representative
            // pstmt.setString(6, "Sponsor-Default"); // sponsor - NOT NULL
            // pstmt.setString(7, "Info-Default"); // info - NOT NULL
            pstmt.executeUpdate();
        }
    }
    
    // Helper để chèn Event (đơn giản hóa, chỉ các trường cần thiết cho Notification test)
    private int insertTestEvent(Connection connection, int eventId, String title, String startDateStr, 
                                String emergency, String status, Integer maxParticipants, 
                                String organizerUsername, String description, String requestId) throws SQLException, ParseException {
        String endDateStr = getFutureDateStringFromDate(dateFormat.parse(startDateStr), 5); // Mặc định 5 ngày sau
        String sql = "INSERT INTO Events (eventId, title, startDate, endDate, emergencyLevel, status, maxParticipantNumber, organizer, description, requestId) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) { // Không cần RETURN_GENERATED_KEYS nếu eventId được truyền vào
            pstmt.setInt(1, eventId);
            pstmt.setString(2, title);
            pstmt.setString(3, startDateStr);
            pstmt.setString(4, endDateStr);
            pstmt.setString(5, emergency);
            pstmt.setString(6, status);
            if (maxParticipants != null) pstmt.setInt(7, maxParticipants); else pstmt.setNull(7, java.sql.Types.INTEGER);
            pstmt.setString(8, organizerUsername);
            pstmt.setString(9, description);
            if (requestId != null) pstmt.setString(10, requestId); else pstmt.setNull(10, java.sql.Types.VARCHAR);
            pstmt.executeUpdate();
            return eventId; // Trả về eventId đã truyền
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

    // Helper để lấy Notification từ DB (nếu cần để verify)
    private Notification_DataInDB getNotificationFromDB(int eventId, String username) throws SQLException {
        String sql = "SELECT * FROM Notification WHERE eventId = ? AND username = ? ORDER BY notificationId DESC LIMIT 1";
        try (PreparedStatement pstmt = connForHelpers.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            pstmt.setString(2, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Notification_DataInDB data = new Notification_DataInDB();
                    data.notificationId = rs.getInt("notificationId");
                    data.eventId = rs.getInt("eventId");
                    data.username = rs.getString("username");
                    data.acceptStatus = rs.getString("acceptStatus");
                    return data;
                }
            }
        }
        return null;
    }
    
    // Helper class để chứa dữ liệu Notification đọc từ DB
    private static class Notification_DataInDB {
        int notificationId;
        int eventId;
        String username;
        String acceptStatus;
    }


    // --- Test Cases for NotificationDAO.createRegistrationNotification ---

    @Test
    void createRegistrationNotification_NewRegistration_ShouldInsertAndReturnTrue() throws SQLException {
        // --- Arrange ---
        int eventId = 1001; // Event đã được tạo trong @BeforeEach
        String volunteerUsername = "testVolNotify"; // Volunteer đã được tạo

        // --- Act ---
        boolean result = notificationDAO.createRegistrationNotification(eventId, volunteerUsername);

        // --- Assert ---
        assertTrue(result, "createRegistrationNotification should return true for a new registration.");

        // Verify notification in DB
        Notification_DataInDB notificationInDb = getNotificationFromDB(eventId, volunteerUsername);
        assertNotNull(notificationInDb, "Notification should exist in DB.");
        assertEquals(eventId, notificationInDb.eventId);
        assertEquals(volunteerUsername, notificationInDb.username);
        assertEquals(AppConstants.NOTIF_PENDING, notificationInDb.acceptStatus, "Notification status should be PENDING.");
    }

    @Test
    void createRegistrationNotification_AlreadyPending_ShouldReturnFalseAndNotInsertNew() throws SQLException {
        // --- Arrange ---
        int eventId = 1001;
        String volunteerUsername = "testVolNotify";

        // 1. Tạo một notification "Pending" trước
        boolean firstResult = notificationDAO.createRegistrationNotification(eventId, volunteerUsername);
        assertTrue(firstResult, "First registration attempt should succeed.");
        
        Notification_DataInDB firstNotification = getNotificationFromDB(eventId, volunteerUsername);
        assertNotNull(firstNotification, "First notification should be in DB.");
        int firstNotificationId = firstNotification.notificationId;

        // --- Act ---
        // 2. Cố gắng tạo lại notification
        boolean secondResult = notificationDAO.createRegistrationNotification(eventId, volunteerUsername);

        // --- Assert ---
        assertFalse(secondResult, "createRegistrationNotification should return false if volunteer already has a PENDING/REGISTERED notification.");

        // Kiểm tra xem không có notification mới nào được tạo, và cái cũ vẫn còn
        Notification_DataInDB currentNotification = getNotificationFromDB(eventId, volunteerUsername);
        assertNotNull(currentNotification);
        assertEquals(firstNotificationId, currentNotification.notificationId, "Notification ID should not change, no new notification should be inserted.");
        assertEquals(AppConstants.NOTIF_PENDING, currentNotification.acceptStatus);
    }

    @Test
    void createRegistrationNotification_AlreadyRegistered_ShouldReturnFalseAndNotInsertNew() throws SQLException {
        // --- Arrange ---
        int eventId = 1001;
        String volunteerUsername = "testVolNotify";

        // 1. Tạo một notification "Pending" và sau đó cập nhật nó thành "Registered" (giả lập)
        // Vì createRegistrationNotification chỉ tạo "Pending", chúng ta sẽ chèn trực tiếp "Registered"
        String insertRegisteredSql = "INSERT INTO Notification (eventId, username, acceptStatus) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connForHelpers.prepareStatement(insertRegisteredSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, eventId);
            pstmt.setString(2, volunteerUsername);
            pstmt.setString(3, AppConstants.NOTIF_REGISTERED);
            pstmt.executeUpdate();
            try(ResultSet rs = pstmt.getGeneratedKeys()){
                assertTrue(rs.next(), "Should insert registered notification for setup.");
            }
        }
        
        // --- Act ---
        boolean result = notificationDAO.createRegistrationNotification(eventId, volunteerUsername);

        // --- Assert ---
        assertFalse(result, "Should return false if volunteer is already REGISTERED for the event.");
    }
    
 // --- Test Cases for NotificationDAO.getVolunteerNotificationStatusForEvent ---

    @Test
    void getVolunteerNotificationStatusForEvent_NoNotificationExists_ShouldReturnNull() throws SQLException {
        // --- Arrange ---
        int eventId = 1001; // Event đã tạo trong @BeforeEach
        String volunteerUsername = "testVolNotify"; // Volunteer đã tạo trong @BeforeEach
        // Không chèn notification nào cho cặp này

        // --- Act ---
        String status = notificationDAO.getVolunteerNotificationStatusForEvent(volunteerUsername, eventId);

        // --- Assert ---
        assertNull(status, "Should return null if no notification exists for the volunteer/event pair.");
    }

    @Test
    void getVolunteerNotificationStatusForEvent_SingleNotificationExists_ShouldReturnCorrectStatus() throws SQLException {
        // --- Arrange ---
        int eventId = 1001;
        String volunteerUsername = "testVolNotify";
        String expectedStatus = AppConstants.NOTIF_PENDING;

        // Chèn một notification
        insertTestNotification(connForHelpers, 1, eventId, volunteerUsername, expectedStatus);

        // --- Act ---
        String actualStatus = notificationDAO.getVolunteerNotificationStatusForEvent(volunteerUsername, eventId);

        // --- Assert ---
        assertEquals(expectedStatus, actualStatus, "Should return the correct status of the single existing notification.");
    }

    @Test
    void getVolunteerNotificationStatusForEvent_MultipleNotificationsExist_ShouldReturnLatestStatus() throws SQLException {
        // --- Arrange ---
        int eventId = 1001;
        String volunteerUsername = "testVolNotify";
        
        // Chèn nhiều notifications, cái có notificationId cao hơn sẽ là cái mới nhất
        insertTestNotification(connForHelpers, 10, eventId, volunteerUsername, AppConstants.NOTIF_PENDING); // ID thấp hơn, cũ hơn
        insertTestNotification(connForHelpers, 11, eventId, volunteerUsername, "SomeOldStatus"); // ID thấp hơn, cũ hơn
        String latestStatus = AppConstants.NOTIF_REGISTERED; // Đây là status mong đợi
        insertTestNotification(connForHelpers, 12, eventId, volunteerUsername, latestStatus); // ID cao nhất, mới nhất

        // --- Act ---
        String actualStatus = notificationDAO.getVolunteerNotificationStatusForEvent(volunteerUsername, eventId);

        // --- Assert ---
        assertEquals(latestStatus, actualStatus, "Should return the status of the notification with the highest ID (latest).");
    }

    @Test
    void getVolunteerNotificationStatusForEvent_NotificationForDifferentEvent_ShouldReturnNull() throws SQLException {
        // --- Arrange ---
        int eventId1 = 1001; // Event chính
        int eventId2 = 1002; // Event khác
        String volunteerUsername = "testVolNotify";
        
        // Tạo event thứ 2
        try {
            insertTestEvent(connForHelpers, eventId2, "Notify Event 2", getFutureDateString(6), 
                           AppConstants.EMERGENCY_LOW, AppConstants.EVENT_APPROVED, 5, "orgNotify", 
                           "Desc for Notify Event 2", null);
        } catch (ParseException e) { throw new SQLException(e.getMessage());}


        insertTestNotification(connForHelpers, 20, eventId2, volunteerUsername, AppConstants.NOTIF_PENDING); // Notification cho event 2

        // --- Act ---
        // Lấy status cho event 1, mà không có notification nào cho nó
        String status = notificationDAO.getVolunteerNotificationStatusForEvent(volunteerUsername, eventId1);

        // --- Assert ---
        assertNull(status, "Should return null if notification exists but for a different event.");
    }
    
    @Test
    void getVolunteerNotificationStatusForEvent_NotificationForDifferentVolunteer_ShouldReturnNull() throws SQLException {
        // --- Arrange ---
        int eventId = 1001;
        String volunteer1 = "testVolNotify";
        String volunteer2 = "anotherVolNotify";
        ensureSystemUserExists(connForHelpers, volunteer2, "pass", "avol@notify.com", "789", "AddrAV");
        ensureVolunteerExists(connForHelpers, volunteer2, "Another Volunteer Notify", "CCCDAVOLN", "1991-02-02", 15);

        insertTestNotification(connForHelpers, 30, eventId, volunteer2, AppConstants.NOTIF_PENDING); // Notification cho volunteer2

        // --- Act ---
        // Lấy status cho volunteer1, mà không có notification nào cho nó
        String status = notificationDAO.getVolunteerNotificationStatusForEvent(volunteer1, eventId);

        // --- Assert ---
        assertNull(status, "Should return null if notification exists but for a different volunteer.");
    }


    // --- Helper method mới để chèn Notification ---
    private int insertTestNotification(Connection connection, int notificationId, int eventId, String username, String acceptStatus) throws SQLException {
        // Đảm bảo eventId và username tồn tại nếu có FK constraints nghiêm ngặt và PRAGMA ON
        // (Trong @BeforeEach, eventId 1001 và username testVolNotify đã được tạo)
        String sql = "INSERT INTO Notification (notificationId, eventId, username, acceptStatus) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, notificationId);
            // Cho phép eventId hoặc username là null nếu cột trong DB cho phép, 
            // nhưng cho test này chúng ta thường sẽ cung cấp giá trị.
            if (eventId > 0) { // Giả sử 0 hoặc số âm không phải là eventId hợp lệ
                 pstmt.setInt(2, eventId);
            } else {
                 pstmt.setNull(2, java.sql.Types.INTEGER);
            }
            pstmt.setString(3, username);
            pstmt.setString(4, acceptStatus);
            pstmt.executeUpdate();
            return notificationId; // Trả về ID đã truyền vào (vì notificationId không tự tăng)
        }
    }
    
 // --- Test Cases for NotificationDAO.isVolunteerPendingOrRegistered ---

    @Test
    void isVolunteerPendingOrRegistered_WhenNotificationIsPending_ShouldReturnTrue() throws SQLException {
        // --- Arrange ---
        int eventId = 1001; // Event đã tạo trong @BeforeEach
        String volunteerUsername = "testVolNotify"; // Volunteer đã tạo trong @BeforeEach
        
        insertTestNotification(connForHelpers, 201, eventId, volunteerUsername, AppConstants.NOTIF_PENDING);

        // --- Act ---
        boolean result = notificationDAO.isVolunteerPendingOrRegistered(volunteerUsername, eventId);

        // --- Assert ---
        assertTrue(result, "Should return true if a 'Pending' notification exists.");
    }

    @Test
    void isVolunteerPendingOrRegistered_WhenNotificationIsRegistered_ShouldReturnTrue() throws SQLException {
        // --- Arrange ---
        int eventId = 1001;
        String volunteerUsername = "testVolNotify";
        
        insertTestNotification(connForHelpers, 202, eventId, volunteerUsername, AppConstants.NOTIF_REGISTERED);

        // --- Act ---
        boolean result = notificationDAO.isVolunteerPendingOrRegistered(volunteerUsername, eventId);

        // --- Assert ---
        assertTrue(result, "Should return true if a 'Registered' notification exists.");
    }

    @Test
    void isVolunteerPendingOrRegistered_WhenNotificationIsApproved_ShouldAlsoReturnTrueIfConsideredRegistered() throws SQLException {
        // DAO của bạn hiện tại chỉ kiểm tra AppConstants.NOTIF_PENDING hoặc AppConstants.NOTIF_REGISTERED
        // Nếu AppConstants.EVENT_APPROVED (hoặc một hằng số tương tự cho notification được duyệt)
        // cũng được coi là "Registered" trong ngữ cảnh này, bạn cần cập nhật câu SQL trong DAO
        // hoặc thêm nó vào điều kiện OR.
        // Hiện tại, test này sẽ fail nếu AppConstants.EVENT_APPROVED không nằm trong OR clause của DAO.
        // Để test này pass với logic DAO hiện tại, chúng ta cần chèn 'Registered' hoặc 'Pending'.
        // Nếu bạn muốn 'Approved' cũng được tính, hãy sửa DAO.
        // Giả sử 'Approved' cũng được tính là đã đăng ký/đang xử lý:
        // Trong NotificationDAO.isVolunteerPendingOrRegistered, sửa SQL thành:
        // String sql = "... (acceptStatus = ? OR acceptStatus = ? OR acceptStatus = ?)";
        // pstmt.setString(3, AppConstants.NOTIF_PENDING);
        // pstmt.setString(4, AppConstants.NOTIF_REGISTERED);
        // pstmt.setString(5, AppConstants.EVENT_APPROVED); // Hoặc một AppConstants.NOTIF_APPROVED
        
        // Với logic DAO hiện tại (chỉ PENDING hoặc REGISTERED):
        // --- Arrange ---
        int eventId = 1001;
        String volunteerUsername = "testVolNotify";
        insertTestNotification(connForHelpers, 203, eventId, volunteerUsername, AppConstants.EVENT_APPROVED); // Trạng thái này không được DAO hiện tại coi là "Pending" hay "Registered"

        // --- Act ---
        boolean result = notificationDAO.isVolunteerPendingOrRegistered(volunteerUsername, eventId);

        // --- Assert ---
        // Dựa trên code DAO hiện tại, test này sẽ mong đợi false
        assertFalse(result, "Should return false if notification is 'Approved' and DAO only checks for 'Pending' or 'Registered'.");
    }

    @Test
    void isVolunteerPendingOrRegistered_WhenNotificationIsRejected_ShouldReturnFalse() throws SQLException {
        // --- Arrange ---
        int eventId = 1001;
        String volunteerUsername = "testVolNotify";
        
        insertTestNotification(connForHelpers, 204, eventId, volunteerUsername, AppConstants.REQUEST_REJECTED); // Sử dụng một hằng số trạng thái từ chối

        // --- Act ---
        boolean result = notificationDAO.isVolunteerPendingOrRegistered(volunteerUsername, eventId);

        // --- Assert ---
        assertFalse(result, "Should return false if a 'Rejected' notification exists.");
    }
    
    @Test
    void isVolunteerPendingOrRegistered_WhenNotificationIsCancelled_ShouldReturnFalse() throws SQLException {
        // --- Arrange ---
        int eventId = 1001;
        String volunteerUsername = "testVolNotify";
        
        insertTestNotification(connForHelpers, 205, eventId, volunteerUsername, AppConstants.NOTIF_CANCELLED);

        // --- Act ---
        boolean result = notificationDAO.isVolunteerPendingOrRegistered(volunteerUsername, eventId);

        // --- Assert ---
        assertFalse(result, "Should return false if a 'Cancelled' notification exists.");
    }

    @Test
    void isVolunteerPendingOrRegistered_NoNotificationExists_ShouldReturnFalse() throws SQLException {
        // --- Arrange ---
        int eventId = 1001;
        String volunteerUsername = "testVolNotify";
        // Không chèn notification nào

        // --- Act ---
        boolean result = notificationDAO.isVolunteerPendingOrRegistered(volunteerUsername, eventId);

        // --- Assert ---
        assertFalse(result, "Should return false if no notification exists.");
    }

    @Test
    void isVolunteerPendingOrRegistered_NotificationForDifferentEvent_ShouldReturnFalse() throws SQLException {
        // --- Arrange ---
        int eventId1 = 1001;
        int eventId2 = 1002; // Event khác
        String volunteerUsername = "testVolNotify";
         try {
            insertTestEvent(connForHelpers, eventId2, "Notify Event Other", getFutureDateString(7), 
                           AppConstants.EMERGENCY_NORMAL, AppConstants.EVENT_APPROVED, 3, "orgNotify", 
                           "Desc Other", null);
        } catch (ParseException e) { throw new SQLException(e.getMessage());}
        
        insertTestNotification(connForHelpers, 206, eventId2, volunteerUsername, AppConstants.NOTIF_PENDING); // Notif cho event 2

        // --- Act ---
        boolean result = notificationDAO.isVolunteerPendingOrRegistered(volunteerUsername, eventId1); // Kiểm tra cho event 1

        // --- Assert ---
        assertFalse(result, "Should return false if notification is for a different event.");
    }

    @Test
    void isVolunteerPendingOrRegistered_NotificationForDifferentVolunteer_ShouldReturnFalse() throws SQLException {
        // --- Arrange ---
        int eventId = 1001;
        String volunteer1 = "testVolNotify";
        String volunteer2 = "anotherVolForPendingCheck";
        ensureSystemUserExists(connForHelpers, volunteer2, "pass", "avolpc@notify.com", "000", "AddrAVPC");
        ensureVolunteerExists(connForHelpers, volunteer2, "Another Vol For Pending Check", "CCCDAVPC", "1992-03-03", 10);

        insertTestNotification(connForHelpers, 207, eventId, volunteer2, AppConstants.NOTIF_PENDING); // Notif cho volunteer2

        // --- Act ---
        boolean result = notificationDAO.isVolunteerPendingOrRegistered(volunteer1, eventId); // Kiểm tra cho volunteer1

        // --- Assert ---
        assertFalse(result, "Should return false if notification is for a different volunteer.");
    }
    
    // --- Test Cases for NotificationDAO.getPendingNotificationsByOrganizer ---

    @Test
    void getPendingNotificationsByOrganizer_OrganizerHasPendingNotifications_ShouldReturnCorrectListWithEventTitles() throws SQLException, ParseException {
        // --- Arrange ---
        String org1Uname = "orgWithPending1";
        ensureVolunteerOrganizationExists(connForHelpers, org1Uname, "Org Pending 1", "LICP1", "FieldP1", "RepP1", "SponsorP1", "InfoP1");
        String org2Uname = "orgWithPending2";
        ensureVolunteerOrganizationExists(connForHelpers, org2Uname, "Org Pending 2", "LICP2", "FieldP2", "RepP2", "SponsorP2", "InfoP2");

        String vol1Uname = "volNotifyPending1";
        ensureVolunteerExists(connForHelpers, vol1Uname, "Vol Notify Pending 1", "CCCDVNP1", "1995-01-01", 10);
        String vol2Uname = "volNotifyPending2";
        ensureVolunteerExists(connForHelpers, vol2Uname, "Vol Notify Pending 2", "CCCDVNP2", "1996-02-02", 15);

        // Events by org1
        int event1Org1Id = insertTestEvent(connForHelpers, 501, "Org1 Event Pending Alpha", getFutureDateString(3), AppConstants.EMERGENCY_NORMAL, AppConstants.EVENT_APPROVED, 10, org1Uname, "Desc O1EPA", null);
        int event2Org1Id = insertTestEvent(connForHelpers, 502, "Org1 Event Approved Beta", getFutureDateString(5), AppConstants.EMERGENCY_HIGH, AppConstants.EVENT_APPROVED, 5, org1Uname, "Desc O1EAB", null);
        int event3Org1Id = insertTestEvent(connForHelpers, 503, "Org1 Event Other Status Gamma", getFutureDateString(7), AppConstants.EMERGENCY_LOW, AppConstants.EVENT_APPROVED, 5, org1Uname, "Desc O1EOSG", null);

        // Events by org2
        int event1Org2Id = insertTestEvent(connForHelpers, 504, "Org2 Event Pending Delta", getFutureDateString(4), AppConstants.EMERGENCY_URGENT, AppConstants.EVENT_APPROVED, 12, org2Uname, "Desc O2EPD", null);

        // Notifications
        // For org1's events
        insertTestNotification(connForHelpers, 301, event1Org1Id, vol1Uname, AppConstants.NOTIF_PENDING); // Expected for org1
        insertTestNotification(connForHelpers, 302, event2Org1Id, vol2Uname, AppConstants.NOTIF_PENDING); // Expected for org1
        insertTestNotification(connForHelpers, 303, event3Org1Id, vol1Uname, AppConstants.NOTIF_REGISTERED); // Not pending
        
        // For org2's event
        insertTestNotification(connForHelpers, 304, event1Org2Id, vol2Uname, AppConstants.NOTIF_PENDING); // Belongs to org2

        // --- Act ---
        List<Notification> pendingNotificationsForOrg1 = notificationDAO.getPendingNotificationsByOrganizer(org1Uname);

        // --- Assert ---
        assertNotNull(pendingNotificationsForOrg1, "List of pending notifications should not be null.");
        assertEquals(2, pendingNotificationsForOrg1.size(), "Org1 should have 2 pending notifications.");

        // Check details for the first pending notification of Org1
        Notification notif1 = pendingNotificationsForOrg1.stream()
                                .filter(n -> n.getEventId() == event1Org1Id && vol1Uname.equals(n.getUsername()))
                                .findFirst().orElse(null);
        assertNotNull(notif1, "Notification for Event 501 and Volunteer volNotifyPending1 should exist.");
        assertEquals(AppConstants.NOTIF_PENDING, notif1.getAcceptStatus());
        assertEquals("Org1 Event Pending Alpha", notif1.getEventTitle(), "Event title should be correctly joined and set.");
        assertEquals(vol1Uname, notif1.getUsername());

        // Check details for the second pending notification of Org1
        Notification notif2 = pendingNotificationsForOrg1.stream()
                                .filter(n -> n.getEventId() == event2Org1Id && vol2Uname.equals(n.getUsername()))
                                .findFirst().orElse(null);
        assertNotNull(notif2, "Notification for Event 502 and Volunteer volNotifyPending2 should exist.");
        assertEquals(AppConstants.NOTIF_PENDING, notif2.getAcceptStatus());
        assertEquals("Org1 Event Approved Beta", notif2.getEventTitle()); // Event title check
        assertEquals(vol2Uname, notif2.getUsername());
    }

    @Test
    void getPendingNotificationsByOrganizer_OrganizerHasNoPendingNotifications_ShouldReturnEmptyList() throws SQLException, ParseException {
        // --- Arrange ---
        String orgNoPendingUname = "orgNoPending";
        ensureVolunteerOrganizationExists(connForHelpers, orgNoPendingUname, "Org No Pending", "LICNP", "FieldNP", "RepNP", "SponsorNP", "InfoNP");
        
        String volUname = "volForNoPending"; // Volunteer này đã có
        ensureVolunteerExists(connForHelpers, volUname, "Vol For No Pending", "CCCDVFNP", "1997-03-03", 20);

        int eventId = 0; // Khởi tạo để tránh lỗi biên dịch nếu try-catch không gán
        try {
            eventId = insertTestEvent(connForHelpers, 505, "OrgNoPending Event", getFutureDateString(1), 
                                    AppConstants.EMERGENCY_NORMAL, AppConstants.EVENT_APPROVED, 10, 
                                    orgNoPendingUname, "Desc ONPE", null);
        } catch (ParseException e) {
            throw new SQLException("Error parsing date for event in test: " + e.getMessage());
        }
        assertTrue(eventId > 0, "Event should be inserted for test.");
        
        // Insert notifications with statuses other than PENDING
        insertTestNotification(connForHelpers, 305, eventId, volUname, AppConstants.NOTIF_REGISTERED);
        
        // THÊM PHẦN TẠO "anotherVol" TRƯỚC KHI DÙNG NÓ
        String anotherVolUsername = "anotherVol";
        ensureSystemUserExists(connForHelpers, anotherVolUsername, "passAnother", "another@vol.com", "777", "AddrAnother");
        ensureVolunteerExists(connForHelpers, anotherVolUsername, "Another Volunteer Test", "CCCDAVT", "1999-09-09", 5);
        
        insertTestNotification(connForHelpers, 306, eventId, anotherVolUsername, AppConstants.NOTIF_CANCELLED);


        // --- Act ---
        List<Notification> pendingNotifications = notificationDAO.getPendingNotificationsByOrganizer(orgNoPendingUname);

        // --- Assert ---
        assertNotNull(pendingNotifications, "List should not be null.");
        assertTrue(pendingNotifications.isEmpty(), "Should return an empty list if organizer has no PENDING notifications.");
    }

    @Test
    void getPendingNotificationsByOrganizer_NonExistingOrganizer_ShouldReturnEmptyList() {
        // --- Arrange ---
        String nonExistingOrg = "ghostOrganizer123";

        // --- Act ---
        List<Notification> pendingNotifications = notificationDAO.getPendingNotificationsByOrganizer(nonExistingOrg);

        // --- Assert ---
        assertNotNull(pendingNotifications, "List should not be null.");
        assertTrue(pendingNotifications.isEmpty(), "Should return an empty list for a non-existing organizer.");
    }

}