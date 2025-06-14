package controller.requests;

import dao.HelpRequestDAO;
import entity.requests.HelpRequest;
import java.util.List;
import utils.AppConstants;

public class HelpRequestController {

    private HelpRequestDAO helpRequestDAO;

    public HelpRequestController() {
        this.helpRequestDAO = new HelpRequestDAO();
    }

    public boolean createHelpRequest(HelpRequest request) {
        // Set default status if not provided
        if (request.getStatus() == null || request.getStatus().isEmpty()) {
            request.setStatus(AppConstants.REQUEST_PENDING);
        }
        return helpRequestDAO.createHelpRequest(request);
    }

    public List<HelpRequest> getHelpRequestsByUsername(String personInNeedUsername) {
        return helpRequestDAO.getHelpRequestsByUsername(personInNeedUsername);
    }

    public boolean updateHelpRequest(HelpRequest request) {
        return helpRequestDAO.updateHelpRequest(request);
    }

    public boolean deleteHelpRequest(int requestId) {
        return helpRequestDAO.deleteHelpRequest(requestId);
    }

    public boolean markAsSatisfied(int requestId) {
        return helpRequestDAO.updateHelpRequestStatus(requestId, AppConstants.REQUEST_SATISFIED);
    }

    public List<HelpRequest> getApprovedHelpRequests() {
        return helpRequestDAO.getApprovedHelpRequests();
    }
}
