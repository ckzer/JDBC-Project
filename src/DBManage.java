import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBManage {
    private static final String URL = "jdbc:mysql://localhost:3306/companydb"; // DB 이름을 지정하세요.
    private static final String USER = "newuser"; // DB 사용자 이름
    private static final String PASSWORD = "user"; // DB 비밀번호

    private Connection connection;

    public DBManage() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.out.println("DB 연결 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    // DB 연결 종료 메서드
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("DB 연결 종료");
            }
        } catch (SQLException e) {
            System.out.println("DB 연결 종료 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
