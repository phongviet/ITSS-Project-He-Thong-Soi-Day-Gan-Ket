package dao; // Hoặc package test của bạn

import entity.requests.HelpRequest;
import entity.users.PersonInNeed; // Cần để tạo PersonInNeed làm khóa ngoại
import entity.users.SystemUser;  // Cần để tạo SystemUser cho PersonInNeed
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

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestHelpRequestDAO {

    private HelpRequestDAO helpRequestDAO;
    private static final String DB_URL_FOR_TEST = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db"; // Phải giống DB_URL trong DAO
    private Connection connForHelpers;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @BeforeAll
    void setUpAll() throws SQLException {
        helpRequestDAO = new HelpRequestDAO();
        connForHelpers = DriverManager.getConnection(DB_URL_FOR_TEST);
        System.out.println("TestHelpRequestDAO: Connected to DB for helpers: " + DB_URL_FOR_TEST);
    }

    @BeforeEach
    void setUpForEachTest() throws SQLException {
        System.out.println("--- TestHelpRequestDAO: Running setUpForEachTest: Clearing Data ---");
        // Xóa dữ liệu từ các bảng liên quan trước mỗi test
        clearTableData(connForHelpers, "HelpRequest");
        clearTableData(connForHelpers, "PersonInNeed"); // Vì HelpRequest tham chiếu đến PersonInNeed
        clearTableData(connForHelpers, "SystemUser");   // Vì PersonInNeed tham chiếu đến SystemUser

        // Chèn dữ liệu tham chiếu cơ bản nếu cần cho các test
        // Ví dụ, một PersonInNeed mặc định để các HelpRequest có thể được tạo
        ensureSystemUserExists(connForHelpers, "personTest1", "testPass", "person1@example.com", "111", "Address 1");
        ensurePersonInNeedExists(connForHelpers, "personTest1", "Test Person One", "CCCD1", "1990-01-01");
    }

    @AfterAll
    void tearDownAll() throws SQLException {
        if (connForHelpers != null && !connForHelpers.isClosed()) {
            connForHelpers.close();
            System.out.println("TestHelpRequestDAO: Closed DB helper connection.");
        }
    }

    // --- Helper methods ---
    private void clearTableData(Connection connection, String tableName) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = OFF;");
            stmt.execute("DELETE FROM " + tableName + ";");
            try {
                stmt.execute("DELETE FROM sqlite_sequence WHERE name='" + tableName + "';");
            } catch (SQLException e) { /* Bỏ qua nếu sequence không tồn tại */ }
            stmt.execute("PRAGMA foreign_keys = ON;");
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

    private void ensurePersonInNeedExists(Connection connection, String username, String fullName, String cccd, String dateOfBirthStr) throws SQLException {
        // Đảm bảo SystemUser tồn tại trước
        // ensureSystemUserExists(connection, username, "defaultPass", username + "@example.com", "000000000", "Default Address");
        // Dòng trên không cần thiết nếu bạn gọi nó trước trong setUpForEachTest hoặc trong chính test case

        String sql = "INSERT OR IGNORE INTO PersonInNeed (username, fullName, cccd, dateOfBirth) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, fullName);
            pstmt.setString(3, cccd);
            pstmt.setString(4, dateOfBirthStr); // Lưu dưới dạng chuỗi YYYY-MM-DD
            pstmt.executeUpdate();
        }
    }
    
    private String formatDate(Date date) {
        if (date == null) return null;
        return dateFormat.format(date);
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return dateFormat.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private String getFutureDateString(int daysToAdd) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, daysToAdd);
        return dateFormat.format(cal.getTime());
    }


    // --- Test Cases for HelpRequestDAO.createHelpRequest ---

    @Test
    void createHelpRequest_ValidRequest_ShouldInsertAndReturnTrue() throws SQLException, ParseException {
        // --- Arrange ---
        String personInNeedUsername = "personTest1"; // Đã được tạo trong @BeforeEach
        
        HelpRequest request = new HelpRequest();
        request.setTitle("Need Food Supplies");
        request.setDescription("Requesting urgent food supplies for a family of 4.");
        request.setContact("0909123456");
        request.setPersonInNeedUsername(personInNeedUsername);
        request.setStartDate(dateFormat.parse(getFutureDateString(1))); // Ngày mai
        request.setEmergencyLevel(AppConstants.EMERGENCY_URGENT);
        request.setStatus(AppConstants.REQUEST_PENDING); // Controller sẽ set nếu null, nhưng ở đây ta set luôn
        request.setAddress("123 Main St, District 1");

        // --- Act ---
        boolean result = helpRequestDAO.createHelpRequest(request);

        // --- Assert ---
        assertTrue(result, "createHelpRequest should return true for successful insertion.");

        // Verify data in DB. Chúng ta cần một cách để lấy lại request vừa tạo.
        // Vì requestId không được trả về và không tự tăng trong schema của bạn,
        // chúng ta sẽ tìm theo title và personInNeedUsername (giả định là đủ duy nhất cho test này).
        // HOẶC, tốt hơn là bạn nên sửa schema để requestId tự tăng hoặc DAO trả về ID.
        // Tạm thời, chúng ta lấy tất cả request của user đó và kiểm tra cái mới nhất/phù hợp.
        
        HelpRequest_DataInDB requestInDb = null;
        String sqlVerify = "SELECT * FROM HelpRequest WHERE title = ? AND personInNeedId = ? ORDER BY requestId DESC LIMIT 1";
        try (PreparedStatement pstmtVerify = connForHelpers.prepareStatement(sqlVerify)) {
            pstmtVerify.setString(1, request.getTitle());
            pstmtVerify.setString(2, request.getPersonInNeedUsername());
            try (ResultSet rs = pstmtVerify.executeQuery()) {
                if (rs.next()) {
                    requestInDb = new HelpRequest_DataInDB();
                    requestInDb.requestId = rs.getInt("requestId"); // Lấy requestId từ DB
                    requestInDb.title = rs.getString("title");
                    requestInDb.description = rs.getString("description");
                    requestInDb.contact = rs.getString("contact");
                    requestInDb.personInNeedUsername = rs.getString("personInNeedId");
                    requestInDb.startDate = rs.getString("startDate"); // Lấy dưới dạng String từ DB
                    requestInDb.emergencyLevel = rs.getString("emergencyLevel");
                    requestInDb.status = rs.getString("status");
                    requestInDb.address = rs.getString("address");
                }
            }
        }
        assertNotNull(requestInDb, "Request should be found in DB after creation.");
        // Gán requestId được tạo tự động (nếu có) vào đối tượng request ban đầu để so sánh các trường khác
        // Nếu requestId không tự tăng, bạn cần cung cấp nó trong đối tượng request ban đầu.
        // Giả sử schema HelpRequest có requestId là PRIMARY KEY nhưng không AUTOINCREMENT
        // và bạn không set requestId cho `request` object, thì việc kiểm tra sẽ khó khăn.
        // Trong trường hợp này, ta chỉ kiểm tra các trường khác.
        assertEquals(request.getTitle(), requestInDb.title);
        assertEquals(request.getDescription(), requestInDb.description);
        assertEquals(request.getContact(), requestInDb.contact);
        assertEquals(request.getPersonInNeedUsername(), requestInDb.personInNeedUsername);
        assertEquals(formatDate(request.getStartDate()), requestInDb.startDate); // So sánh chuỗi ngày
        assertEquals(request.getEmergencyLevel(), requestInDb.emergencyLevel);
        assertEquals(request.getStatus(), requestInDb.status);
        assertEquals(request.getAddress(), requestInDb.address);
    }

    @Test
    void createHelpRequest_WithNullStartDate_ShouldInsertAndReturnTrue() throws SQLException {
        // --- Arrange ---
        String personInNeedUsername = "personTest1";
        
        HelpRequest request = new HelpRequest();
        request.setTitle("Request With Null Start Date");
        request.setDescription("Description for null start date.");
        request.setContact("0909000111");
        request.setPersonInNeedUsername(personInNeedUsername);
        request.setStartDate(null); // StartDate là null
        request.setEmergencyLevel(AppConstants.EMERGENCY_NORMAL);
        request.setStatus(AppConstants.REQUEST_PENDING);
        request.setAddress("456 Side St, District 2");

        // --- Act ---
        boolean result = helpRequestDAO.createHelpRequest(request);

        // --- Assert ---
        assertTrue(result, "createHelpRequest should return true even with null startDate.");
        
        HelpRequest_DataInDB requestInDb = null;
        String sqlVerify = "SELECT startDate FROM HelpRequest WHERE title = ? AND personInNeedId = ?";
        try (PreparedStatement pstmtVerify = connForHelpers.prepareStatement(sqlVerify)) {
            pstmtVerify.setString(1, request.getTitle());
            pstmtVerify.setString(2, request.getPersonInNeedUsername());
            try (ResultSet rs = pstmtVerify.executeQuery()) {
                if (rs.next()) {
                    requestInDb = new HelpRequest_DataInDB(); // Chỉ cần check startDate
                    requestInDb.startDate = rs.getString("startDate");
                }
            }
        }
        assertNotNull(requestInDb, "Request should be found in DB.");
        assertNull(requestInDb.startDate, "startDate in DB should be null.");
    }

    
    // --- Helper class để lưu trữ dữ liệu HelpRequest đọc từ DB ---
    private static class HelpRequest_DataInDB {
        int requestId;
        String title;
        String description;
        String contact;
        String personInNeedUsername;
        String startDate; // Lưu trữ dưới dạng String để so sánh với DB
        String emergencyLevel;
        String status;
        String address;
    }
}