package controller.report;

import dao.ReportDAO;
import entity.reports.Report;

public class ReportController {

    private ReportDAO reportDAO;

    public ReportController() {
        this.reportDAO = new ReportDAO();
    }

    public boolean saveProgressReport(Report report, boolean isFinal) {
        return reportDAO.saveProgressReport(report, isFinal);
    }
} 