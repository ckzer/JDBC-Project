import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
    public String deleteEmployees(List<String> ssnList) {
        String deleteWorksOnQuery = "DELETE FROM WORKS_ON WHERE Essn = ?"; // 일한 시간 정보를 먼저 제거
        String deleteDependentQuery = "DELETE FROM DEPENDENT WHERE Essn = ?"; // 가족 정보를 먼저 제거
        String deleteQuery = "DELETE FROM EMPLOYEE WHERE Ssn = ?"; // 직원을 제거
        String updateSuperSsnQuery = "UPDATE EMPLOYEE SET Super_ssn = NULL WHERE Super_ssn = ?"; // 직원의 상사를 수정
        String updateDepartmentMgrQuery = "UPDATE DEPARTMENT SET Mgr_ssn = '888665555' WHERE Mgr_ssn = ?"; // 부서의 관리자인 경우를 고려

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // 트랜잭션 시작

            try (PreparedStatement deleteWorksOnPstmt = conn.prepareStatement(deleteWorksOnQuery);
                 PreparedStatement deleteDependentPstmt = conn.prepareStatement(deleteDependentQuery);
                 PreparedStatement deletePstmt = conn.prepareStatement(deleteQuery);
                 PreparedStatement updatePstmt = conn.prepareStatement(updateSuperSsnQuery);
                 PreparedStatement updateDeptMgrPstmt = conn.prepareStatement(updateDepartmentMgrQuery)) {

                for (String ssn : ssnList) {

                    if (ssn.equals("888665555")) {
                        return "삭제할 수 없는 직원입니다.";
                    }

                    System.out.println("Attempting to delete employee with SSN: " + ssn);
                    // 일한 시간 데이터를 삭제
                    deleteWorksOnPstmt.setString(1, ssn);
                    deleteWorksOnPstmt.executeUpdate();
                    // 가족관계 정보 데이터를 삭제
                    deleteDependentPstmt.setString(1, ssn);
                    deleteDependentPstmt.executeUpdate();
                    // 부서의 Mgr_ssn을 NULL로 변경
                    updateDeptMgrPstmt.setString(1, ssn);
                    updateDeptMgrPstmt.executeUpdate();
                    // Super_ssn을 NULL로 변경
                    updatePstmt.setString(1, ssn);
                    updatePstmt.executeUpdate();
                    // 직원 데이터를 삭제
                    deletePstmt.setString(1, ssn);
                    deletePstmt.addBatch();
                }

                deletePstmt.executeBatch();
                conn.commit(); // 트랜잭션 커밋
                System.out.println("Selected employees deleted successfully.");
                return "삭제 완료";
            } catch (SQLException e) {
                conn.rollback(); // 오류 발생 시 롤백
                e.printStackTrace();
                return "삭제 중 오류가 발생했습니다.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "데이터베이스 연결 오류가 발생했습니다.";
        }
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