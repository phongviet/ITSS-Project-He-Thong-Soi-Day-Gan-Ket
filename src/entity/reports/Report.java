package entity.reports;

import java.util.Date;

/**
 * Report entity class representing the Report table in the database
 */
public class Report {
    private int reportId;
    private Integer eventId;
    private Date reportDate;
    private Integer progress;  // Progress as percentage (0-100)
    private String note;

    public Report() {
    }

    public Report(int reportId, Integer eventId, Date reportDate, Integer progress, String note) {
        this.reportId = reportId;
        this.eventId = eventId;
        this.reportDate = reportDate;
        this.progress = progress;
        this.note = note;
    }

    public int getReportId() {
        return reportId;
    }

    public void setReportId(int reportId) {
        this.reportId = reportId;
    }

    public Integer getEventId() {
        return eventId;
    }

    public void setEventId(Integer eventId) {
        this.eventId = eventId;
    }

    public Date getReportDate() {
        return reportDate;
    }

    public void setReportDate(Date reportDate) {
        this.reportDate = reportDate;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
