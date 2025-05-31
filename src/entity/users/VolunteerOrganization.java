package entity.users;

public class VolunteerOrganization extends SystemUser {
    private String organizationName;
    private String licenseNumber;
    private String field;            // Field of operation
    private String representative;   // Representative person
    private String sponsor;          // Organization sponsor
    private String info;             // Additional information

    public VolunteerOrganization(String username, String password, String email, String phone, String address) {
        super(username, password, email, phone, address);
    }

    public VolunteerOrganization(String username, String password, String email, String phone, String address,
                                String organizationName, String licenseNumber, String field,
                                String representative, String sponsor, String info) {
        super(username, password, email, phone, address);
        this.organizationName = organizationName;
        this.licenseNumber = licenseNumber;
        this.field = field;
        this.representative = representative;
        this.sponsor = sponsor;
        this.info = info;
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

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getRepresentative() {
        return representative;
    }

    public void setRepresentative(String representative) {
        this.representative = representative;
    }

    public String getSponsor() {
        return sponsor;
    }

    public void setSponsor(String sponsor) {
        this.sponsor = sponsor;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
