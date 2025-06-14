package utils;

public final class AppConstants {

    private AppConstants() {}

    // --- Event Statuses ---
    public static final String EVENT_PENDING = "Pending";
    public static final String EVENT_APPROVED = "Approved";
    public static final String EVENT_UPCOMING = "Upcoming";
    public static final String EVENT_DONE = "Done";
    public static final String EVENT_CANCELLED = "Cancelled";
    public static final String EVENT_REJECTED = "Rejected";

    // --- Emergency Levels ---
    public static final String EMERGENCY_URGENT = "khẩn cấp";
    public static final String EMERGENCY_HIGH = "cao";
    public static final String EMERGENCY_NORMAL = "bình thường";
    public static final String EMERGENCY_LOW = "thấp";

    // --- Help Request Statuses ---
    public static final String REQUEST_PENDING = "Pending";
    public static final String REQUEST_APPROVED = "Approved";
    public static final String REQUEST_REJECTED = "Rejected";
    public static final String REQUEST_SATISFIED = "Satisfied";
    public static final String REQUEST_CLOSED = "Closed"; //not open for organization

    // --- Notification and Participation Statuses ---
    public static final String NOTIF_PENDING = "Pending";
    public static final String NOTIF_REGISTERED = "Registered";
    public static final String NOTIF_CANCELLED = "Cancelled";
}