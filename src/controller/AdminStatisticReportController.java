package controller;

import dao.EventDAO;
import dao.HelpRequestDAO;
import dao.UserDAO;
import java.util.Map;

public class AdminStatisticReportController {

    private UserDAO userDAO;
    private EventDAO eventDAO;
    private HelpRequestDAO helpRequestDAO;

    public AdminStatisticReportController() {
        this.userDAO = new UserDAO();
        this.eventDAO = new EventDAO();
        this.helpRequestDAO = new HelpRequestDAO();
    }

    /**
     * Gets statistics about system users
     * @return A map containing various user statistics
     */
    public Map<String, Integer> getUserStatistics() {
        return userDAO.getUserStatistics();
    }

    /**
     * Gets statistics about events in the system
     * @return A map containing various event statistics
     */
    public Map<String, Object> getEventStatistics() {
        return eventDAO.getEventStatistics();
    }

    /**
     * Gets statistics about help requests
     * @return A map containing various help request statistics
     */
    public Map<String, Object> getHelpRequestStatistics() {
        return helpRequestDAO.getHelpRequestStatistics();
    }
}
