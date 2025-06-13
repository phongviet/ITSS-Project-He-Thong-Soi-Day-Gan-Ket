package controller.event;

import dao.EventDAO;
import dao.HelpRequestDAO;
import dao.NotificationDAO;
import dao.ReportDAO;
import dao.UserDAO;
import entity.events.*;
import entity.users.VolunteerOrganization;
import entity.users.Volunteer;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import entity.requests.HelpRequest;
import entity.reports.Report;
import utils.AppConstants;

public class EventController {

    private EventDAO eventDAO;
    private UserDAO userDAO;
    private NotificationDAO notificationDAO;
    private HelpRequestDAO helpRequestDAO;
    private ReportDAO reportDAO;

    public EventController() {
        this.eventDAO = new EventDAO();
        this.userDAO = new UserDAO();
        this.notificationDAO = new NotificationDAO();
        this.helpRequestDAO = new HelpRequestDAO();
        this.reportDAO = new ReportDAO();
    }

    public List<HelpRequest> getApprovedHelpRequests() {
        return helpRequestDAO.getApprovedHelpRequests();
    }

    public List<EventParticipantDetails> getParticipantDetailsForEvent(int eventId) {
        return eventDAO.getEventParticipantDetailsList(eventId);
    }
    
    public boolean updateEventParticipantDetails(int eventId, String volunteerUsername, Integer hoursParticipated, Integer ratingByOrg) {
        return userDAO.updateEventParticipantDetails(eventId, volunteerUsername, hoursParticipated, ratingByOrg);
    }

    public boolean registerEvent(Event event, VolunteerOrganization organization) {
        return eventDAO.registerEvent(event, organization);
    }

    public List<Event> getEventsByOrganizerId(String organizerId) {
        return eventDAO.getEventsByOrganizerId(organizerId);
    }

    public boolean recalculateVolunteerAverageRating(String volunteerUsername) {
        return userDAO.recalculateVolunteerAverageRating(volunteerUsername);
    }

    public List<Volunteer> getEventParticipants(int eventId) {
        return userDAO.getEventParticipants(eventId);
    }

    public List<Event> getAllEvents() {
        return eventDAO.getAllEvents();
    }
    
    public List<EventParticipantDetails> getEventParticipationDetailsForVolunteer(String volunteerUsername) {
        return userDAO.getEventParticipationDetailsForVolunteer(volunteerUsername);
    }

    public Event getEventById(int eventId) {
       return eventDAO.getEventById(eventId);
    }

    public List<entity.notifications.Notification> getPendingNotificationsByOrganizer(String organizerUsername) {
        return notificationDAO.getPendingNotificationsByOrganizer(organizerUsername);
    }

    public boolean updateNotificationStatus(int notificationId, String newStatus) {
        return notificationDAO.processRegistrationAndUpdateParticipant(notificationId, newStatus);
    }
    
    public int getEmergencyLevelPriority(String emergencyLevel) {
        if (emergencyLevel == null || emergencyLevel.trim().isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return switch (emergencyLevel.trim().toLowerCase()) {
            case "urgent" -> 1;
            case "high" -> 2;
            case "normal" -> 3;
            case "low" -> 4;
            default -> {
                System.out.println("Unknown emergency level for priority: " + emergencyLevel);
                yield 5;
            }
        };
    }

    public List<Event> getSuggestedEventsForVolunteer(Volunteer volunteer) {
        if (volunteer == null || volunteer.getUsername() == null) {
            System.err.println("getSuggestedEventsForVolunteer: Volunteer object or username is null.");
            return new ArrayList<>();
        }
        List<String> volunteerSkills = volunteer.getSkills();
        if (volunteerSkills == null) {
            System.err.println("getSuggestedEventsForVolunteer: Volunteer skills list is null for user " + volunteer.getUsername());
            return new ArrayList<>();
        }

        List<Event> allOpenEvents = eventDAO.getAllOpenAndAvailableEvents();
        List<SuggestedEventWrapper> suggestedEventsWithScore = new ArrayList<>();

        for (Event event : allOpenEvents) {
            boolean eventRequiresSkills = event.getRequiredSkills() != null && !event.getRequiredSkills().isEmpty();
            int effectiveMatchScore;

            if (eventRequiresSkills) {
                int matchedSkillsCount = 0;
                for (String requiredSkill : event.getRequiredSkills()) {
                    if (volunteerSkills.contains(requiredSkill)) {
                        matchedSkillsCount++;
                    }
                }
                if (matchedSkillsCount > 0) {
                    effectiveMatchScore = matchedSkillsCount;
                    suggestedEventsWithScore.add(new SuggestedEventWrapper(event, effectiveMatchScore));
                }
            } else {
                effectiveMatchScore = Integer.MAX_VALUE;
                suggestedEventsWithScore.add(new SuggestedEventWrapper(event, effectiveMatchScore));
            }
        }

        Collections.sort(suggestedEventsWithScore, Comparator
                .comparingInt(SuggestedEventWrapper::getMatchedSkillsCount).reversed()
                .thenComparing(wrapper -> wrapper.getEvent().getEmergencyLevel(),
                        Comparator.nullsLast(Comparator.comparingInt(this::getEmergencyLevelPriority)))
                .thenComparing(wrapper -> wrapper.getEvent().getStartDate(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
        );

        List<Event> sortedSuggestedEvents = new ArrayList<>();
        for (SuggestedEventWrapper wrapper : suggestedEventsWithScore) {
            sortedSuggestedEvents.add(wrapper.getEvent());
        }
        return sortedSuggestedEvents;
    }

    public boolean updateEventStatus(int eventId, String newStatus) {
        return eventDAO.updateEventStatus(eventId, newStatus);
    }

    public boolean saveProgressReport(Report report, boolean isFinal) {
        return reportDAO.saveProgressReport(report, isFinal);
    }

    public int getTotalEventCountByOrganizer(String organizerId) {
        return eventDAO.getEventsByOrganizerId(organizerId).size();
    }
    
    private static class SuggestedEventWrapper {
        private Event event;
        private int matchedSkillsCount;

        public SuggestedEventWrapper(Event event, int matchedSkillsCount) {
            this.event = event;
            this.matchedSkillsCount = matchedSkillsCount;
        }

        public Event getEvent() {
            return event;
        }

        public int getMatchedSkillsCount() {
            return matchedSkillsCount;
        }
    }
}
