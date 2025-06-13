package dao;

import entity.reports.Report;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;

/**
 * ReportDAO class handles all database operations related to reports.
 */
public class ReportDAO {

    private static final String DB_URL = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    // Methods for Report management will be moved here.
    // For example:
    // public List<Report> getReportsForEvent(int eventId) { ... }
    // public boolean saveReport(Report report) { ... }
    // public boolean isFinalReport(int reportId) { ... }

    public boolean saveProgressReport(Report report, boolean isFinal) {
        String insertReportSQL = "INSERT INTO Report (eventId, reportDate, progress, note) VALUES (?, ?, ?, ?)";
        String insertFinalReportSQL = "INSERT INTO FinalReport (reportId) VALUES (?)";

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(false);

            try (PreparedStatement pstmtReport = conn.prepareStatement(insertReportSQL, Statement.RETURN_GENERATED_KEYS)) {
                pstmtReport.setInt(1, report.getEventId());
                if (report.getReportDate() != null) {
                    pstmtReport.setString(2, DATE_FORMAT.format(report.getReportDate()));
                } else {
                    pstmtReport.setNull(2, java.sql.Types.VARCHAR);
                }
                if (report.getProgress() != null) {
                    pstmtReport.setInt(3, report.getProgress());
                } else {
                    pstmtReport.setNull(3, java.sql.Types.INTEGER);
                }
                pstmtReport.setString(4, report.getNote());

                int affectedRows = pstmtReport.executeUpdate();
                if (affectedRows == 0) throw new SQLException("Creating report failed, no rows affected.");
                
                if (isFinal) {
                    try (ResultSet generatedKeys = pstmtReport.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int reportId = generatedKeys.getInt(1);
                            try (PreparedStatement pstmtFinal = conn.prepareStatement(insertFinalReportSQL)) {
                                pstmtFinal.setInt(1, reportId);
                                pstmtFinal.executeUpdate();
                            }
                        } else {
                            throw new SQLException("Failed to retrieve generated reportId.");
                        }
                    }
                }
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    public boolean eventHasFinalHundredPercentReport(int eventId) {
        String sql = "SELECT COUNT(*) FROM Report r JOIN FinalReport fr ON r.reportId = fr.reportId WHERE r.eventId = ? AND r.progress = 100";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

}