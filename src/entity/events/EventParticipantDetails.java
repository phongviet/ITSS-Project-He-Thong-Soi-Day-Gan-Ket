package entity.events; // Hoặc một package DTO thích hợp như dto.event

import entity.users.Volunteer; // Giả sử bạn muốn giữ tham chiếu đến Volunteer
import java.util.Date;
import java.util.List; // Nếu bạn muốn hiển thị cả skill của Event

public class EventParticipantDetails {

    // Thông tin từ Event
    private int eventId;
    private String title;
    private Date startDate;
    private Date endDate;
    private String eventStatus; // Trạng thái chung của sự kiện (ví dụ: Upcoming, Completed)
    private String organizerName; // Tên của tổ chức (có thể lấy qua Event.getOrganizer() rồi truy vấn)
    // Bạn có thể thêm các trường khác từ Event nếu cần hiển thị

    // Thông tin từ EventParticipants (cụ thể cho Volunteer này)
    private String volunteerUsername; // Để xác định Volunteer
    private String volunteerFullName; // Thêm trường này
    private Integer hoursParticipated;
    private Integer ratingByOrg;

    // Trạng thái tham gia của Volunteer với Event này (ví dụ: Registered, Attended, Canceled)
    private String volunteerParticipationStatus;


    // Constructors
    public EventParticipantDetails() {
    }

    // Constructor đầy đủ (ví dụ)
    public EventParticipantDetails(Event event, String volunteerUsername, String volunteerFullName, Integer hoursParticipated, Integer ratingByOrg, String volunteerParticipationStatus) {
        if (event != null) {
            this.eventId = event.getEventId();
            this.title = event.getTitle();
            this.startDate = event.getStartDate();
            this.endDate = event.getEndDate();
            this.eventStatus = event.getStatus(); // Trạng thái chung của Event
            // this.organizerName = ... ; // Cần logic để lấy tên tổ chức
        }
        this.volunteerUsername = volunteerUsername;
        this.volunteerFullName = volunteerFullName; // Gán giá trị
        this.hoursParticipated = hoursParticipated;
        this.ratingByOrg = ratingByOrg;
        this.volunteerParticipationStatus = volunteerParticipationStatus;
        //this.organizerName = organizerName;
    }


    // Getters and Setters

    // --- Từ Event ---
    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getEventStatus() {
        return eventStatus;
    }

    public void setEventStatus(String eventStatus) {
        this.eventStatus = eventStatus;
    }

    public String getOrganizerName() {
        return organizerName;
    }

    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    // --- Từ EventParticipants (cho Volunteer này) ---
    public String getVolunteerUsername() {
        return volunteerUsername;
    }

    public void setVolunteerUsername(String volunteerUsername) {
        this.volunteerUsername = volunteerUsername;
    }

    public Integer getHoursParticipated() {
        return hoursParticipated;
    }

    public void setHoursParticipated(Integer hoursParticipated) {
        this.hoursParticipated = hoursParticipated;
    }

    public Integer getRatingByOrg() {
        return ratingByOrg;
    }

    public void setRatingByOrg(Integer ratingByOrg) {
        this.ratingByOrg = ratingByOrg;
    }

    // --- Trạng thái tham gia của Volunteer ---
    public String getVolunteerParticipationStatus() {
        return volunteerParticipationStatus;
    }

    public void setVolunteerParticipationStatus(String volunteerParticipationStatus) {
        this.volunteerParticipationStatus = volunteerParticipationStatus;
    }

    // Getter và Setter cho volunteerFullName
    public String getVolunteerFullName() {
        return volunteerFullName;
    }

    public void setVolunteerFullName(String volunteerFullName) {
        this.volunteerFullName = volunteerFullName;
    }

    // Bạn có thể thêm các phương thức tiện ích khác nếu cần
}