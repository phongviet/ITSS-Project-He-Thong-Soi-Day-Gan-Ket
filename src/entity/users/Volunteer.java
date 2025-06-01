package entity.users;

import java.util.List;
import java.util.Map;
import java.util.Date;

public class Volunteer extends SystemUser {
    private String fullName;
    private String cccd;         // Citizen ID
    private Date dateOfBirth;    // Date of Birth
    private List<String> skills; // Skill list
    private Map<String, Integer> availability; // VD: {"Monday": 2 giờ, "Saturday": 4 giờ}
    private int freeHourPerWeek; // Total free hours per week

    private double averageRating; // 1 - 5
    private int ratingCount;     // Number of ratings

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCccd() {
        return cccd;
    }

    public void setCccd(String cccd) {
        this.cccd = cccd;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public int getFreeHourPerWeek() {
        return freeHourPerWeek;
    }

    public void setFreeHourPerWeek(int freeHourPerWeek) {
        this.freeHourPerWeek = freeHourPerWeek;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public Map<String, Integer> getAvailability() {
        return availability;
    }

    public void setAvailability(Map<String, Integer> availability) {
        this.availability = availability;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }
}
