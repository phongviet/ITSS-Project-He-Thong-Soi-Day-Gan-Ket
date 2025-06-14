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
import java.util.Map;
import java.util.List;

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
        ensureSystemUserExists(connection, username, "defaultPass", username + "@example.com", "000000000", "Default Address");
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
    
 // --- Test Cases for HelpRequestDAO.updateHelpRequest ---

    @Test
    void updateHelpRequest_ExistingRequest_ShouldUpdateFieldsAndReturnTrue() throws SQLException, ParseException {
        // --- Arrange ---
        String personUsername = "personTest1"; // Đã có từ @BeforeEach
        
        // 1. Chèn một HelpRequest ban đầu
        HelpRequest originalRequest = new HelpRequest();
        originalRequest.setTitle("Original Title for Update");
        originalRequest.setDescription("Original description.");
        originalRequest.setContact("0101010101");
        originalRequest.setPersonInNeedUsername(personUsername);
        originalRequest.setStartDate(dateFormat.parse(getFutureDateString(5)));
        originalRequest.setEmergencyLevel(AppConstants.EMERGENCY_NORMAL);
        originalRequest.setStatus(AppConstants.REQUEST_PENDING); // Status này không nên bị thay đổi bởi updateHelpRequest
        originalRequest.setAddress("1 Original St");

        // Giả sử createHelpRequest không trả về ID và requestId không tự tăng.
        // Chúng ta cần một cách để lấy requestId sau khi chèn, hoặc tự gán nó.
        // Để đơn giản, ta sẽ giả định rằng requestId là cố định cho test này.
        // Hoặc, tốt hơn là dùng helper để chèn và lấy ID.
        // Tạm thời, chèn và lấy lại bằng title (không lý tưởng cho sản phẩm thực)
        helpRequestDAO.createHelpRequest(originalRequest); // Chèn vào DB
        
        // Lấy lại request vừa chèn để có requestId (cách này không tốt nếu title không duy nhất)
        // Cách tốt hơn: Sửa createHelpRequest để trả về ID hoặc đối tượng đã được chèn với ID.
        // Hoặc, nếu requestId không tự tăng, bạn phải set nó cho originalRequest trước khi chèn.
        // Trong schema của bạn, requestId là PRIMARY KEY nhưng không AUTOINCREMENT.
        // Vậy, bạn phải cung cấp requestId khi tạo.
        
        int testRequestId = 1; // Giả sử bạn có thể kiểm soát requestId này
                               // hoặc bạn phải tự insert vào DB với một requestId cụ thể.
        // Để test chính xác, chúng ta sẽ insert trực tiếp với ID biết trước.
        clearTableData(connForHelpers, "HelpRequest"); // Xóa lại để chèn với ID cụ thể
        String originalStartDateStr = getFutureDateString(5);
        insertHelpRequestWithId(connForHelpers, testRequestId, "Original Title for Update", originalStartDateStr,
                                AppConstants.EMERGENCY_NORMAL, "Original description.", personUsername,
                                AppConstants.REQUEST_PENDING, "0101010101", "1 Original St");


        // 2. Tạo đối tượng HelpRequest với thông tin cập nhật
        HelpRequest updatedRequestInfo = new HelpRequest();
        updatedRequestInfo.setRequestId(testRequestId); // QUAN TRỌNG: Phải có requestId để update đúng bản ghi
        updatedRequestInfo.setTitle("Updated Title");
        updatedRequestInfo.setDescription("Updated description.");
        updatedRequestInfo.setContact("0909999888");
        String updatedStartDateStr = getFutureDateString(7);
        updatedRequestInfo.setStartDate(dateFormat.parse(updatedStartDateStr));
        updatedRequestInfo.setEmergencyLevel(AppConstants.EMERGENCY_HIGH);
        updatedRequestInfo.setAddress("2 Updated Ave");
        // Không set personInNeedUsername và status vì updateHelpRequest không cập nhật chúng

        // --- Act ---
        boolean result = helpRequestDAO.updateHelpRequest(updatedRequestInfo);

        // --- Assert ---
        assertTrue(result, "updateHelpRequest should return true for a successful update.");

        // Verify data in DB
        HelpRequest_DataInDB requestInDb = getHelpRequestDataFromDBById(testRequestId); // Helper mới
        assertNotNull(requestInDb, "Updated request should be found in DB.");
        assertEquals(updatedRequestInfo.getTitle(), requestInDb.title);
        assertEquals(updatedRequestInfo.getDescription(), requestInDb.description);
        assertEquals(updatedRequestInfo.getContact(), requestInDb.contact);
        assertEquals(formatDate(updatedRequestInfo.getStartDate()), requestInDb.startDate);
        assertEquals(updatedRequestInfo.getEmergencyLevel(), requestInDb.emergencyLevel);
        assertEquals(updatedRequestInfo.getAddress(), requestInDb.address);

        // Kiểm tra các trường không bị thay đổi
        assertEquals(personUsername, requestInDb.personInNeedUsername, "personInNeedUsername should not be changed by updateHelpRequest.");
        assertEquals(AppConstants.REQUEST_PENDING, requestInDb.status, "Status should not be changed by updateHelpRequest.");
    }

    @Test
    void updateHelpRequest_NonExistingRequest_ShouldReturnFalse() throws SQLException, ParseException {
        // --- Arrange ---
        int nonExistingRequestId = 99994;
        
        HelpRequest requestToUpdate = new HelpRequest();
        requestToUpdate.setRequestId(nonExistingRequestId);
        requestToUpdate.setTitle("Title for Non-existing");
        requestToUpdate.setDescription("Desc for Non-existing");
        requestToUpdate.setStartDate(dateFormat.parse(getFutureDateString(1)));
        // ... (set các trường khác nếu cần cho việc gọi hàm, dù nó sẽ không update)

        // --- Act ---
        boolean result = helpRequestDAO.updateHelpRequest(requestToUpdate);

        // --- Assert ---
        assertFalse(result, "updateHelpRequest should return false for a non-existing requestId.");
    }

    // --- Helper methods mới hoặc cập nhật ---

    // Helper để chèn HelpRequest với ID cụ thể (vì requestId không tự tăng trong schema)
    private void insertHelpRequestWithId(Connection connection, int requestId, String title, String startDateStr,
                                         String emergency, String description, String personInNeedID, 
                                         String status, String contact, String address) throws SQLException {
        ensurePersonInNeedExists(connection, personInNeedID, "Person " + personInNeedID, "CCCD-"+personInNeedID, "1985-01-01");
        
        String sql = "INSERT INTO HelpRequest (requestId, title, startDate, emergencyLevel, description, personInNeedId, status, contact, address) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, requestId);
            pstmt.setString(2, title);
            pstmt.setString(3, startDateStr);
            pstmt.setString(4, emergency);
            pstmt.setString(5, description);
            pstmt.setString(6, personInNeedID);
            pstmt.setString(7, status);
            pstmt.setString(8, contact);
            pstmt.setString(9, address);
            pstmt.executeUpdate();
        }
    }

    // Helper để lấy HelpRequest_DataInDB bằng ID
    private HelpRequest_DataInDB getHelpRequestDataFromDBById(int requestId) throws SQLException {
        String sql = "SELECT * FROM HelpRequest WHERE requestId = ?";
        try (PreparedStatement pstmt = connForHelpers.prepareStatement(sql)) {
            pstmt.setInt(1, requestId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    HelpRequest_DataInDB data = new HelpRequest_DataInDB();
                    data.requestId = rs.getInt("requestId");
                    data.title = rs.getString("title");
                    data.description = rs.getString("description");
                    data.contact = rs.getString("contact");
                    data.personInNeedUsername = rs.getString("personInNeedId");
                    data.startDate = rs.getString("startDate");
                    data.emergencyLevel = rs.getString("emergencyLevel");
                    data.status = rs.getString("status");
                    data.address = rs.getString("address");
                    return data;
                }
            }
        }
        return null;
    }

 // --- Test Cases for HelpRequestDAO.getHelpRequestStatistics ---

    @Test
    void getHelpRequestStatistics_WithVariousRequestStatuses_ShouldReturnCorrectCounts() throws SQLException, ParseException {
        // --- Arrange ---
        String personUser = "personForStats";
        ensurePersonInNeedExists(connForHelpers, personUser, "Person For Statistics", "CCCDStats", "1992-02-02");

        // Chèn các HelpRequest với các trạng thái khác nhau
        // RequestId phải là duy nhất vì nó là PRIMARY KEY
        insertHelpRequestWithId(connForHelpers, 101, "Request Pending 1", getFutureDateString(1), AppConstants.EMERGENCY_NORMAL, "Desc P1", personUser, AppConstants.REQUEST_PENDING, "contactP1", "Addr P1");
        insertHelpRequestWithId(connForHelpers, 102, "Request Pending 2", getFutureDateString(2), AppConstants.EMERGENCY_LOW, "Desc P2", personUser, AppConstants.REQUEST_PENDING, "contactP2", "Addr P2");
        
        insertHelpRequestWithId(connForHelpers, 103, "Request Approved 1", getFutureDateString(3), AppConstants.EMERGENCY_HIGH, "Desc A1", personUser, AppConstants.REQUEST_APPROVED, "contactA1", "Addr A1");
        
        insertHelpRequestWithId(connForHelpers, 104, "Request Rejected 1", getFutureDateString(4), AppConstants.EMERGENCY_NORMAL, "Desc R1", personUser, AppConstants.REQUEST_REJECTED, "contactR1", "Addr R1");
        insertHelpRequestWithId(connForHelpers, 105, "Request Rejected 2", getFutureDateString(5), AppConstants.EMERGENCY_URGENT, "Desc R2", personUser, AppConstants.REQUEST_REJECTED, "contactR2", "Addr R2");
        insertHelpRequestWithId(connForHelpers, 106, "Request Rejected 3", getFutureDateString(6), AppConstants.EMERGENCY_LOW, "Desc R3", personUser, AppConstants.REQUEST_REJECTED, "contactR3", "Addr R3");

        // Một request với trạng thái khác (ví dụ: Satisfied) để đảm bảo nó không bị đếm nhầm
        insertHelpRequestWithId(connForHelpers, 107, "Request Satisfied", getFutureDateString(7), AppConstants.EMERGENCY_NORMAL, "Desc S1", personUser, AppConstants.REQUEST_SATISFIED, "contactS1", "Addr S1");


        // --- Act ---
        Map<String, Object> stats = helpRequestDAO.getHelpRequestStatistics();

        // --- Assert ---
        assertNotNull(stats, "Statistics map should not be null.");

        // Tổng số requests: 2 Pending + 1 Approved + 3 Rejected + 1 Satisfied = 7
        assertEquals(7, stats.get("totalRequests"), "Total requests count mismatch.");
        
        assertEquals(2, stats.get("pendingRequests"), "Pending requests count mismatch.");
        assertEquals(1, stats.get("approvedRequests"), "Approved requests count mismatch.");
        assertEquals(3, stats.get("rejectedRequests"), "Rejected requests count mismatch.");
        
        // Kiểm tra kiểu dữ liệu
        assertTrue(stats.get("totalRequests") instanceof Integer, "totalRequests should be Integer.");
        assertTrue(stats.get("pendingRequests") instanceof Integer, "pendingRequests should be Integer.");
        // ... (tương tự cho các key khác)
    }

    @Test
    void getHelpRequestStatistics_WhenNoRequestsExist_ShouldReturnZeroCounts() {
        // --- Arrange ---
        // Không chèn HelpRequest nào (setUpForEachTest đã clear bảng)

        // --- Act ---
        Map<String, Object> stats = helpRequestDAO.getHelpRequestStatistics();

        // --- Assert ---
        assertNotNull(stats, "Statistics map should not be null.");
        assertEquals(0, stats.getOrDefault("totalRequests", 0), "Total requests should be 0.");
        assertEquals(0, stats.getOrDefault("pendingRequests", 0), "Pending requests should be 0.");
        assertEquals(0, stats.getOrDefault("approvedRequests", 0), "Approved requests should be 0.");
        assertEquals(0, stats.getOrDefault("rejectedRequests", 0), "Rejected requests should be 0.");
    }

    @Test
    void getHelpRequestStatistics_OnlyOneTypeOfStatusExists_ShouldReturnCorrectCounts() throws SQLException, ParseException {
        // --- Arrange ---
        String personUser = "personForStatsSingle";
        ensurePersonInNeedExists(connForHelpers, personUser, "Person For Stats Single", "CCCDStatsS", "1993-03-03");

        insertHelpRequestWithId(connForHelpers, 201, "Only Approved 1", getFutureDateString(1), AppConstants.EMERGENCY_NORMAL, "Desc OA1", personUser, AppConstants.REQUEST_APPROVED, "contactOA1", "Addr OA1");
        insertHelpRequestWithId(connForHelpers, 202, "Only Approved 2", getFutureDateString(2), AppConstants.EMERGENCY_HIGH, "Desc OA2", personUser, AppConstants.REQUEST_APPROVED, "contactOA2", "Addr OA2");

        // --- Act ---
        Map<String, Object> stats = helpRequestDAO.getHelpRequestStatistics();

        // --- Assert ---
        assertNotNull(stats);
        assertEquals(2, stats.get("totalRequests"));
        assertEquals(0, stats.get("pendingRequests"));
        assertEquals(2, stats.get("approvedRequests"));
        assertEquals(0, stats.get("rejectedRequests"));
    }
    
 // --- Test Cases for HelpRequestDAO.getAllHelpRequests ---

    @Test
    void getAllHelpRequests_WhenRequestsExist_ShouldReturnAllRequestsWithDetails() throws SQLException, ParseException {
        // --- Arrange ---
        String personUser1 = "personGetAll1";
        ensurePersonInNeedExists(connForHelpers, personUser1, "Person For GetAll 1", "CCCDGA1", "1994-04-04");
        
        String personUser2 = "personGetAll2";
        ensurePersonInNeedExists(connForHelpers, personUser2, "Person For GetAll 2", "CCCDGA2", "1995-05-05");

        // Chèn các HelpRequest
        // RequestId phải là duy nhất
        insertHelpRequestWithId(connForHelpers, 201, "Request All 1 Title", getFutureDateString(1), AppConstants.EMERGENCY_NORMAL, "Desc All 1", personUser1, AppConstants.REQUEST_PENDING, "contactAll1", "Addr All 1");
        insertHelpRequestWithId(connForHelpers, 202, "Request All 2 Title", getFutureDateString(2), AppConstants.EMERGENCY_HIGH, "Desc All 2", personUser2, AppConstants.REQUEST_APPROVED, "contactAll2", "Addr All 2");
        insertHelpRequestWithId(connForHelpers, 203, "Request All 3 Title", getPastDateString(5), AppConstants.EMERGENCY_URGENT, "Desc All 3", personUser1, AppConstants.REQUEST_REJECTED, "contactAll3", "Addr All 3");

        // --- Act ---
        List<HelpRequest> allRequests = helpRequestDAO.getAllHelpRequests();

        // --- Assert ---
        assertNotNull(allRequests, "List of all help requests should not be null.");
        assertEquals(3, allRequests.size(), "Should return all 3 inserted help requests.");

        // Kiểm tra chi tiết một vài request để đảm bảo nạp đúng dữ liệu
        HelpRequest req1 = allRequests.stream().filter(r -> r.getRequestId() == 201).findFirst().orElse(null);
        assertNotNull(req1, "Request with ID 201 should be found.");
        assertEquals("Request All 1 Title", req1.getTitle());
        assertEquals(personUser1, req1.getPersonInNeedUsername());
        assertEquals(AppConstants.REQUEST_PENDING, req1.getStatus());
        assertEquals(AppConstants.EMERGENCY_NORMAL, req1.getEmergencyLevel());
        assertEquals("Addr All 1", req1.getAddress());
        assertEquals("contactAll1", req1.getContact());
        assertEquals(parseDate(getFutureDateString(1)), req1.getStartDate()); // So sánh đối tượng Date

        HelpRequest req3 = allRequests.stream().filter(r -> r.getRequestId() == 203).findFirst().orElse(null);
        assertNotNull(req3, "Request with ID 203 should be found.");
        assertEquals("Request All 3 Title", req3.getTitle());
        assertEquals(personUser1, req3.getPersonInNeedUsername());
        assertEquals(AppConstants.REQUEST_REJECTED, req3.getStatus());
        assertEquals(AppConstants.EMERGENCY_URGENT, req3.getEmergencyLevel());
    }

    @Test
    void getAllHelpRequests_WhenNoRequestsExist_ShouldReturnEmptyList() {
        // --- Arrange ---
        // Không chèn HelpRequest nào (setUpForEachTest đã clear bảng)

        // --- Act ---
        List<HelpRequest> allRequests = helpRequestDAO.getAllHelpRequests();

        // --- Assert ---
        assertNotNull(allRequests, "List of all help requests should not be null.");
        assertTrue(allRequests.isEmpty(), "Should return an empty list when no help requests are in the database.");
    }
    
    private String getPastDateString(int daysToSubtract) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract);
        return dateFormat.format(cal.getTime());
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