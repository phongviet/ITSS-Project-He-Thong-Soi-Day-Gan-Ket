package entity.users;

public class VolunteerOrganization extends SystemUser {
    private String organizationName;
    private String licenseNumber;

    public VolunteerOrganization(String username, String password, String email, String phone, String address) {
        super(username, password, email, phone, address);
    }

    public VolunteerOrganization() {
        super();
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }
}
