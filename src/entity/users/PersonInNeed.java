package entity.users;

import java.util.Date;

public class PersonInNeed extends SystemUser{
    private String fullName;
    private String cccd;        // Citizen ID
    private Date dateOfBirth;   // Date of Birth

    public PersonInNeed() {

    }

    public PersonInNeed(String username, String password, String email, String phone, String address, String fullName, String cccd, Date dateOfBirth) {
        super(username, password, email, phone, address);
        this.fullName = fullName;
        this.cccd = cccd;
        this.dateOfBirth = dateOfBirth;
    }

    // Constructor for backward compatibility
    public PersonInNeed(String username, String password, String email, String phone, String address, String fullName) {
        super(username, password, email, phone, address);
        this.fullName = fullName;
    }

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

    // Quick fix for UI handlers
    public int getId() {
        // This is a temporary solution as PersonInNeed does not have a dedicated ID field yet.
        // Consider implementing a proper ID in SystemUser or PersonInNeed.
        // Using username's hashcode as a placeholder if username is unique and set.
        if (this.getUsername() != null && !this.getUsername().isEmpty()) {
            return this.getUsername().hashCode(); 
        }
        return System.identityHashCode(this); // Fallback to object's identity hashcode
    }

    public String getName() {
        return getFullName();
    }
}
