import java.sql.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RunSqlScript {

    public static void main(String[] args) {
        Connection conn = null;
        String dbTestFilePath = "assets/db/script_test_SoiDayGanKet_sqlite.db";
        String sqlScriptPath = "assets/db/SoiDayGanKet.txt";

        try {
            Files.createDirectories(Paths.get(dbTestFilePath).getParent());

            try {
                Files.deleteIfExists(Paths.get(dbTestFilePath));
                System.out.println("Old script test database file deleted (if existed): " + dbTestFilePath);
            } catch (IOException e) {
                System.err.println("Error deleting old script test database file: " + e.getMessage());
            }

            conn = DriverManager.getConnection("jdbc:sqlite:" + dbTestFilePath);
            System.out.println("Opened test database connection to: " + dbTestFilePath);

            executeSqlScript(conn, sqlScriptPath);
            System.out.println("SQL script executed successfully on " + dbTestFilePath);

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Database Error - " + e.getClass().getName() + ": " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("File I/O Error: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    System.out.println("Database connection closed for " + dbTestFilePath);
                } catch (SQLException e) {
                    e.printStackTrace();
                    System.err.println("Error closing database connection: " + e.getMessage());
                }
            }
        }
    }

    private static void executeSqlScript(Connection conn, String filePath) throws SQLException, IOException {
        Statement stmt = null;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            stmt = conn.createStatement();
            String line;
            StringBuilder sqlBuilder = new StringBuilder();

            System.out.println("Executing SQL script from: " + filePath);
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }
                sqlBuilder.append(line);
                
                if (line.endsWith(";")) {
                    String sqlCommand = sqlBuilder.toString();
                    System.out.println("Executing: " + sqlCommand);
                    try {
                        stmt.execute(sqlCommand);
                    } catch (SQLException e) {
                        System.err.println("Error executing SQL command: [" + sqlCommand + "]");
                        System.err.println("SQL Error (" + e.getErrorCode() + "): " + e.getMessage());
                    }
                    sqlBuilder.setLength(0);
                } else {
                    sqlBuilder.append(" ");
                }
            }
            // Execute any remaining command that might not end with a semicolon
            if (sqlBuilder.length() > 0) {
                 String sqlCommand = sqlBuilder.toString().trim();
                 if (!sqlCommand.isEmpty()) {
                    System.out.println("Executing final command: " + sqlCommand);
                    try {
                        stmt.execute(sqlCommand);
                    } catch (SQLException e) {
                        System.err.println("Error executing final SQL command: [" + sqlCommand + "]");
                        System.err.println("SQL Error (" + e.getErrorCode() + "): " + e.getMessage());
                    }
                 }
            }
            System.out.println("Finished executing SQL script: " + filePath);
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }
} 