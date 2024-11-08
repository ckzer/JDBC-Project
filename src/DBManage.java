import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class DBManage {
    private static final String url = "jdbc:mysql://localhost:3306/mydb?serverTimeZone=UTC";
    private static final String user = "root";
    private static final String password = "root";

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
    public ResultSet getAllEmployees() {
        String query = "SELECT e.Fname, e.Minit, e.Lname, e.Ssn, e.Bdate, e.Address, e.Sex, e.Salary, " +
                "d.Dname AS DepartmentName, CONCAT(s.Fname, ' ', s.Minit, ' ', s.Lname) AS Supervisor " +
                "FROM EMPLOYEE e " +
                "JOIN DEPARTMENT d ON e.Dno = d.Dnumber " +
                "LEFT JOIN EMPLOYEE s ON e.Super_ssn = s.Ssn";
        return executeCustomQuery(query);
    }

    // 조건 검색을 위한 메서드 추가
    public ResultSet searchEmployeesByCondition(String searchType, String searchValue, ArrayList<String> selectedColumns) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ").append(String.join(", ", selectedColumns))
                .append(" FROM EMPLOYEE e ")
                .append("JOIN DEPARTMENT d ON e.Dno = d.Dnumber ")
                .append("LEFT JOIN EMPLOYEE s ON e.Super_ssn = s.Ssn ");

        if (!searchType.equals("전체") && !searchValue.isEmpty()) {
            queryBuilder.append("WHERE ");
            switch(searchType) {
                case "부서":
                    queryBuilder.append("d.Dname = ?");
                    break;
                case "성별":
                    queryBuilder.append("e.Sex = ?");
                    break;
                case "연봉":
                    queryBuilder.append("e.Salary >= ?");
                    break;
            }
        }

        try {
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(queryBuilder.toString());

            if (!searchType.equals("전체") && !searchValue.isEmpty()) {
                if (searchType.equals("연봉")) {
                    pstmt.setDouble(1, Double.parseDouble(searchValue));
                } else {
                    pstmt.setString(1, searchValue);
                }
            }

            return pstmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 부서별 평균 급여를 조회하는 메서드 추가
    public ResultSet getDepartmentAverageSalary() {
        String query = "SELECT d.Dname, AVG(e.Salary) as AvgSalary " +
                "FROM EMPLOYEE e JOIN DEPARTMENT d ON e.Dno = d.Dnumber " +
                "GROUP BY d.Dname";
        return executeCustomQuery(query);
    }


    // 사용자가 선택한 직원(들)을 삭제하는 메서드
    public ResultSet deleteEmployees() {
        String query = "";
        return executeCustomQuery(query);
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