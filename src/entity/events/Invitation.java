package entity.events;

public class Invitation {
    private int eventId;
    private String username;
    private String status; // "pending", "accepted", "rejected", "expired"
    private String message;
    private String timestamp; // Thời gian gửi lời mời
    private String expirationDuration; // Thời gian hết hạn lời mời
}
