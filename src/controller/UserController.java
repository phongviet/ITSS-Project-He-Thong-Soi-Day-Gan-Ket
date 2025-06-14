package controller;

import dao.UserDAO;
import entity.events.EventParticipantDetails;
import entity.users.SystemUser;
import entity.users.Volunteer;

import java.util.List;

public class UserController {
    private UserDAO userDAO;

    public UserController() {
        this.userDAO = new UserDAO();
    }

    /**
     * Get all users from the database
     * @return List of all SystemUser objects
     */
    public List<SystemUser> getAllUsers() {
        return userDAO.getAllUsers();
    }

    public boolean updateEventParticipantDetails(int eventId, String volunteerUsername, Integer hoursParticipated, Integer ratingByOrg) {
        return userDAO.updateEventParticipantDetails(eventId, volunteerUsername, hoursParticipated, ratingByOrg);
    }

    public boolean recalculateVolunteerAverageRating(String volunteerUsername) {
        return userDAO.recalculateVolunteerAverageRating(volunteerUsername);
    }

    public List<Volunteer> getEventParticipants(int eventId) {
        return userDAO.getEventParticipants(eventId);
    }
    
    public List<EventParticipantDetails> getEventParticipationDetailsForVolunteer(String volunteerUsername) {
        return userDAO.getEventParticipationDetailsForVolunteer(volunteerUsername);
    }
}
