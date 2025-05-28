package entity.users;

import java.util.List;
import java.util.Map;

public class Volunteer extends SystemUser {
    private String fullName;
    private List<String> skills; // VD: "Food", "Shelter", "Medical", "Education", "Financial".
    private Map<String, Integer> availability; // VD: {"Monday": 2 giờ, "Saturday": 4 giờ}

    private double averageRating; // 1 - 5
    private int ratingCount; // Số lượng đánh giá

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public double getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }
}
