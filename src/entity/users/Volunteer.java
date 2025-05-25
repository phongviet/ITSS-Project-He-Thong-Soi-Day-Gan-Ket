package entity.users;

public class Volunteer extends SystemUser {
    private String fullName;

    public Volunteer(String username, String password, String email, String phone, String address) {
        super(username, password, email, phone, address);
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
