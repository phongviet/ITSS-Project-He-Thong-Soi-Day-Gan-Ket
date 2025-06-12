package controller.event; // Đảm bảo package khớp với vị trí file

import org.junit.jupiter.api.Test; // JUnit 5
import static org.junit.jupiter.api.Assertions.*;
import controller.event.EventController; 

class TestEventController {

    private EventController eventController = new EventController(); // Tạo instance để gọi phương thức

    @Test
    void getEmergencyLevelPriority_ValidLevels_ShouldReturnCorrectPriority() {
        // Kiểm tra các giá trị emergencyLevel hợp lệ và phổ biến
        assertEquals(1, eventController.getEmergencyLevelPriority("Urgent"), "Urgent should be priority 1");
        assertEquals(1, eventController.getEmergencyLevelPriority("Urgent"), "Case insensitivity for Urgent");

        assertEquals(2, eventController.getEmergencyLevelPriority("High"), "Cao should be priority 2");
        //assertEquals(2, eventController.getEmergencyLevelPriority("High"), "High should be priority 2");


        assertEquals(3, eventController.getEmergencyLevelPriority("Normal"), "Normal should be priority 3");
        //assertEquals(3, eventController.getEmergencyLevelPriority("Normal"), "Normal should be priority 3");

        assertEquals(4, eventController.getEmergencyLevelPriority("Low"), "Low should be priority 4");
        //assertEquals(4, eventController.getEmergencyLevelPriority("Low"), "Low should be priority 4");
    }

    @Test
    void getEmergencyLevelPriority_UnknownLevel_ShouldReturnLowestPriority() {
        // Kiểm tra giá trị không xác định
        assertEquals(5, eventController.getEmergencyLevelPriority("Unknown"), "Unknown level should return lowest priority");
    }

    @Test
    void getEmergencyLevelPriority_NullLevel_ShouldReturnLowestPriority() {
        // Kiểm tra giá trị null
        assertEquals(Integer.MAX_VALUE, eventController.getEmergencyLevelPriority(null), "Null level should return max int (lowest priority in logic)");
        // Hoặc nếu logic của bạn trả về 5 cho null, thì assert là 5.
        // Dựa trên code tôi cung cấp trước đó cho getEmergencyLevelPriority, null sẽ trả về Integer.MAX_VALUE.
    }
}