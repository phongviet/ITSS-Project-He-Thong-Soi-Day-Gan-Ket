package controller;

import dao.EventDAO;
import dao.HelpRequestDAO;
import entity.requests.HelpRequest;
import java.util.List;
import utils.AppConstants;

/**
 * Controller class for handling administrative approval of help requests and events
 */
public class AdminApprovalController {

    private EventDAO eventDAO;
    private HelpRequestDAO helpRequestDAO;

    /**
     * Constructor for the AdminApprovalController
     */
    public AdminApprovalController() {
        this.eventDAO = new EventDAO();
        this.helpRequestDAO = new HelpRequestDAO();
    }

    /**
     * Approves an event
     *
     * @param eventId The ID of the event to approve
     * @return true if approval was successful, false otherwise
     */
    public boolean approveEvent(int eventId) {
        return eventDAO.approveEvent(eventId);
    }

    /**
     * Rejects an event
     *
     * @param eventId The ID of the event to reject
     * @return true if rejection was successful, false otherwise
     */
    public boolean rejectEvent(int eventId) {
        return eventDAO.rejectEvent(eventId);
    }

    /**
     * Retrieves all help requests for admin review
     *
     * @return List of all help requests in the system
     */
    public List<HelpRequest> getAllHelpRequests() {
        return helpRequestDAO.getAllHelpRequests();
    }

    /**
     * Approves a help request
     *
     * @param helpRequest The help request to approve
     * @return true if approval was successful, false otherwise
     */
    public boolean approveHelpRequest(HelpRequest helpRequest) {
        return helpRequestDAO.updateHelpRequestStatus(helpRequest.getRequestId(), AppConstants.REQUEST_APPROVED);
    }

    /**
     * Rejects a help request
     *
     * @param helpRequest The help request to reject
     * @return true if rejection was successful, false otherwise
     */
    public boolean rejectHelpRequest(HelpRequest helpRequest) {
        return helpRequestDAO.updateHelpRequestStatus(helpRequest.getRequestId(), AppConstants.REQUEST_REJECTED);
    }
}

