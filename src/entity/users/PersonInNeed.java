package entity.users;

public class PersonInNeed extends SystemUser{
    private String fullName;

    public PersonInNeed(String username, String password, String email, String phone, String address, String fullName) {
        super(username, password, email, phone, address);
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }
}
