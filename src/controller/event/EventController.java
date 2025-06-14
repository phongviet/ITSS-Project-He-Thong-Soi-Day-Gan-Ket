package controller.event;

import dao.EventDAO;
import entity.events.*;
import entity.users.VolunteerOrganization;
import entity.users.Volunteer;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import entity.reports.Report;

public class EventController {

    private EventDAO eventDAO;

    public EventController() {
        this.eventDAO = new EventDAO();
    }

    public List<EventParticipantDetails> getParticipantDetailsForEvent(int eventId) {
        return eventDAO.getEventParticipantDetailsList(eventId);
    }
    
    public boolean registerEvent(Event event, VolunteerOrganization organization) {
        return eventDAO.registerEvent(event, organization);
    }

    public List<Event> getEventsByOrganizerId(String organizerId) {
        return eventDAO.getEventsByOrganizerId(organizerId);
    }

    public List<Event> getAllEvents() {
        return eventDAO.getAllEvents();
    }
    
    public Event getEventById(int eventId) {
       return eventDAO.getEventById(eventId);
    }

    public int getEmergencyLevelPriority(String emergencyLevel) {
        if (emergencyLevel == null || emergencyLevel.trim().isEmpty()) {
            return Integer.MAX_VALUE;
        }


        String level = emergencyLevel.trim().toLowerCase();
        switch (level) {
            case "urgent":
                return 1;
            case "high":
                return 2;
            case "normal":
                return 3;
            case "low":
                return 4;
            default:
                System.out.println("Unknown emergency level for priority: " + emergencyLevel);
                return 5;
        }
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
