import java.sql.*;

public class TestDB {

    public static void main(String[] args) {
        Connection conn = null;
        try{
            conn = DriverManager.getConnection("jdbc:sqlite:assets\\db\\SoiDayGanKet_sqlite.db");
            System.out.println("Opened database connection!");

            try{
                deleteTable(conn);
            }
            catch (Exception ignored){
                //Do nothing
            }

            createTable(conn);
            insertData(conn, "Phong", "123456");
            insertData(conn, "Peter", "1234567");
            displayTable(conn, "Test");
        }
        catch (SQLException e){
            e.printStackTrace();
            System.out.println(e.getClass().getName() + ": " + e.getMessage());
        }
        finally {
            if(conn != null){
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    private static void displayTable(Connection conn, String tableName) throws SQLException {
        String selectSQL = "SELECT * from " + tableName;
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(selectSQL);

        System.out.println("-------" + tableName + "-------");
        while (rs.next()) {
            System.out.print("Username: " + rs.getString("username") + ", ");
            System.out.println("Password: " + rs.getString("password"));
        }
        System.out.println("------------------");
    }

    private static void createTable(Connection conn) throws SQLException {
        String createTableSQL = "" +
                "CREATE TABLE Test " +
                "(username VARCHAR(50) PRIMARY KEY," +
                " password VARCHAR(20) NOT NULL);";
        Statement stmt = conn.createStatement();
        stmt.execute(createTableSQL);
    }

    private static void deleteTable(Connection conn) throws SQLException {
        String deleteTableSQL = "DROP TABLE Test";
        Statement stmt = conn.createStatement();
        stmt.execute(deleteTableSQL);
    }

    private static void insertData(Connection conn, String username, String password) throws SQLException {
        String insertSQL = "INSERT INTO Test (username, password) VALUES (?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(insertSQL);
        pstmt.setString(1, username);
        pstmt.setString(2, password);
        pstmt.executeUpdate();
    }
}