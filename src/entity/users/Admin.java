package entity.users;

public class Admin extends SystemUser {

    public Admin(String username, String password) {
        super(username, password, null, null, null);
    }

    public Admin() {
        super();
    }
}
