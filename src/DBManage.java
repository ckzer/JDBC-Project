import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBManage {
    private static final String url = "jdbc:mysql://localhost:3306/mydb?serverTimeZone=UTC";
    private static final String user = "root";
    private static final String password = "qwer";

    // 데이터베이스 연결을 생성하는 메서드
    public Connection getConnection() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Database connected.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    // 모든 직원 정보를 조회하는 메서드
    // SQL 쿼리문을 정의하여, EMPLOYEE 테이블의 각 직원 정보를 조회
    public ResultSet getAllEmployees() {
        String query = "SELECT e.Fname, e.Minit, e.Lname, e.Ssn, e.Bdate, e.Address, e.Sex, e.Salary, d.Dname AS DepartmentName " +
                "FROM EMPLOYEE e JOIN DEPARTMENT d ON e.Dno = d.Dnumber";

        // ResultSet 객체를 선언하고 초기화하여, 이후 쿼리 결과를 저장
        ResultSet rs = null;
        try {
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(query);
            rs = pstmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // 쿼리 결과를 담은 ResultSet 객체를 반환
        return rs;
    }

    // 사용자 지정 쿼리를 실행하는 메서드
    public ResultSet executeCustomQuery(String query) {
        ResultSet rs = null;
        try {
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(query);
            rs = pstmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rs;
    }
}
