package controller.verification;

import entity.users.Admin;
import entity.users.Volunteer;
import entity.users.VolunteerOrganization;
import entity.users.PersonInNeed;
import dao.UserDAO;

import java.util.List;

public class VerificationController {
    //private static final String DB_URL = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db";
    private UserDAO userDAO;

    public VerificationController() {
        this.userDAO = new UserDAO();
    }

    public boolean checkInfo(String username, String password) {
        return userDAO.authenticateUser(username, password);
    }

    /**
     * Register a new Volunteer in the system
     * @param username Unique username
     * @param password User's password
     * @param email User's email
     * @param phone User's phone number
     * @param address User's address
     * @param fullName Volunteer's full name
     * @param cccd Citizen ID card number
     * @param dateOfBirth Date of birth
     * @param freeHourPerWeek Free hours per week available for volunteering
     * @param skills List of skills volunteer possesses
     * @return true if registration successful, false otherwise
     */
    public boolean registerVolunteer(String username, String password, String email,
                                     String phone, String address, String fullName,
                                     String cccd, String dateOfBirth, int freeHourPerWeek,
                                     List<String> skills) {
        return userDAO.registerVolunteer(username, password, email, phone, address, fullName, cccd, dateOfBirth, freeHourPerWeek, skills);
    }

    /**
     * Register a new Volunteer Organization in the system
     * @param username Unique username
     * @param password User's password
     * @param email User's email
     * @param phone User's phone number
     * @param address User's address
     * @param organizationName Name of the organization
     * @param licenseNumber Organization's license number
     * @param field Field of operation
     * @param representative Organization representative
     * @param sponsor Organization sponsor
     * @param info Additional information
     * @return true if registration successful, false otherwise
     */
    public boolean registerVolunteerOrganization(String username, String password, String email,
                                                 String phone, String address, String organizationName,
                                                 String licenseNumber, String field, String representative,
                                                 String sponsor, String info) {
        return userDAO.registerVolunteerOrganization(username, password, email, phone, address, organizationName, licenseNumber, field, representative, sponsor, info);
    }

    /**
     * Register a new Person In Need in the system
     * @param username Unique username
     * @param password User's password
     * @param email User's email
     * @param phone User's phone number
     * @param address User's address
     * @param fullName Person's full name
     * @param cccd Citizen ID card number
     * @param dateOfBirth Date of birth
     * @return true if registration successful, false otherwise
     */
    public boolean registerPersonInNeed(String username, String password, String email,
                                        String phone, String address, String fullName,
                                        String cccd, String dateOfBirth) {
        return userDAO.registerPersonInNeed(username, password, email, phone, address, fullName, cccd, dateOfBirth);
    }

    /**
     * Get the type of user based on username
     * @param username The username to check
     * @return "Admin", "Volunteer", "VolunteerOrganization", "PersonInNeed", or null if not found
     */
    public String getUserType(String username) {
        return userDAO.getUserType(username);
    }
    
    /**
     * Get VolunteerOrganization object by username
     * @param username The username of the organization
     * @return VolunteerOrganization object or null if not found
     */
    public VolunteerOrganization getVolunteerOrganization(String username) {
        return userDAO.getVolunteerOrganization(username);
    }

    /**
     * Get Admin object by username
     * @param username The username of the admin
     * @return Admin object or null if not found
     */
    public Admin getAdmin(String username) {
        return userDAO.getAdmin(username);
    }

    /**
     * Get Volunteer object by username
     * @param username The username of the volunteer
     * @return Volunteer object or null if not found
     */
    public Volunteer getVolunteer(String username) {
        return userDAO.getVolunteer(username);
    }

    /**
     * Get PersonInNeed object by username
     * @param username The username of the person in need
     * @return PersonInNeed object or null if not found
     */
    public PersonInNeed getPersonInNeed(String username) {
        return userDAO.getPersonInNeed(username);
    }

}
