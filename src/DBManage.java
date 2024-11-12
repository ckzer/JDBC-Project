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

    // 직원 데이터 추가 메서드
    public boolean addEmployee(String fname, String minit, String lname, String ssn, String bdate, String address,
                               String sex, double salary, String superSsn, int dno) {
        String query = "INSERT INTO EMPLOYEE (Fname, Minit, Lname, Ssn, Bdate, Address, Sex, Salary, Super_ssn, Dno) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, fname);
            pstmt.setString(2, minit);
            pstmt.setString(3, lname);
            pstmt.setString(4, ssn);
            pstmt.setString(5, bdate);
            pstmt.setString(6, address);
            pstmt.setString(7, sex);
            pstmt.setDouble(8, salary);
            pstmt.setString(9, superSsn);
            pstmt.setInt(10, dno);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 직원 정보 삭제 메서드
    public String deleteEmployees(List<String> ssnList) {
        if (ssnList == null || ssnList.isEmpty()) {
            return "SSN 검색 조건 활성화가 필요합니다.";
        }
        String deleteWorksOnQuery = "DELETE FROM WORKS_ON WHERE Essn = ?";
        String deleteDependentQuery = "DELETE FROM DEPENDENT WHERE Essn = ?";
        String deleteEmployeeQuery = "DELETE FROM EMPLOYEE WHERE Ssn = ?";
        String updateSuperSsnQuery = "UPDATE EMPLOYEE SET Super_ssn = NULL WHERE Super_ssn = ?";
        String updateDepartmentMgrQuery = "UPDATE DEPARTMENT SET Mgr_ssn = '888665555' WHERE Mgr_ssn = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement deleteWorksOnStmt = conn.prepareStatement(deleteWorksOnQuery);
                 PreparedStatement deleteDependentStmt = conn.prepareStatement(deleteDependentQuery);
                 PreparedStatement deleteEmployeeStmt = conn.prepareStatement(deleteEmployeeQuery);
                 PreparedStatement updateSuperSsnStmt = conn.prepareStatement(updateSuperSsnQuery);
                 PreparedStatement updateDepartmentMgrStmt = conn.prepareStatement(updateDepartmentMgrQuery)) {

                for (String ssn : ssnList) {
                    if (ssn.equals("888665555")) {
                        return "삭제할 수 없는 직원입니다.";
                    }

                    deleteWorksOnStmt.setString(1, ssn);
                    deleteWorksOnStmt.executeUpdate();

                    deleteDependentStmt.setString(1, ssn);
                    deleteDependentStmt.executeUpdate();

                    updateDepartmentMgrStmt.setString(1, ssn);
                    updateDepartmentMgrStmt.executeUpdate();

                    updateSuperSsnStmt.setString(1, ssn);
                    updateSuperSsnStmt.executeUpdate();

                    deleteEmployeeStmt.setString(1, ssn);
                    deleteEmployeeStmt.addBatch();
                }

                deleteEmployeeStmt.executeBatch();
                conn.commit();
                return "삭제 완료";
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return "삭제 중 오류가 발생했습니다.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "데이터베이스 연결 오류가 발생했습니다.";
        }
    }

    // 직원 검색 및 조회 메서드
    public ResultSet executeCustomQuery(String query) {
        try {
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(query);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
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
            switch (searchType) {
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

    // WORKS_ON 테이블 업데이트 메서드
    public boolean updateWorksOnData(String essn, String pno, String field, String value) {
        String query = "UPDATE WORKS_ON SET " + field + " = ? WHERE Essn = ? AND Pno = ?";
        System.out.println("Executing query: " + query + " with values: " + value + ", " + essn + ", " + pno);
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, value);
            pstmt.setString(2, essn);
            pstmt.setString(3, pno);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // DEPENDENT 테이블 업데이트 메서드
    public boolean updateDependentData(String essn, String field, String value) {
        String query = "UPDATE DEPENDENT SET " + field + " = ? WHERE Essn = ?";
        System.out.println("Executing query: " + query + " with values: " + value + ", " + essn);
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, value);
            pstmt.setString(2, essn);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // PROJECT 테이블 업데이트 메서드
    public boolean updateProjectData(String pnumber, String field, String value) {
        if ("Dname".equals(field)) {
            String dnumQuery = "SELECT Dnumber FROM DEPARTMENT WHERE Dname = ?";
            try (Connection conn = getConnection();
                 PreparedStatement dnumStmt = conn.prepareStatement(dnumQuery)) {
                dnumStmt.setString(1, value);
                ResultSet rs = dnumStmt.executeQuery();
                if (rs.next()) {
                    value = rs.getString("Dnumber");
                    field = "Dnum";
                } else {
                    return false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        String query = "UPDATE PROJECT SET " + field + " = ? WHERE Pnumber = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, value);
            pstmt.setString(2, pnumber);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // WORKS_ON 테이블 데이터 조회 메서드
    public List<Object[]> getWorksOnData() {
        List<Object[]> worksOnData = new ArrayList<>();
        String query = "SELECT Essn, Pno, Hours FROM WORKS_ON";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                worksOnData.add(new Object[]{rs.getString("Essn"), rs.getString("Pno"), rs.getString("Hours")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return worksOnData;
    }

    // DEPENDENT 테이블 데이터 조회 메서드
    public List<Object[]> getDependentData() {
        List<Object[]> dependentData = new ArrayList<>();
        String query = "SELECT D.Essn, E.Fname, D.Dependent_name, D.Sex, D.Bdate, D.Relationship " +
                "FROM DEPENDENT D JOIN EMPLOYEE E ON D.Essn = E.Ssn";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                dependentData.add(new Object[]{
                        rs.getString("Essn"), rs.getString("Fname"), rs.getString("Dependent_name"),
                        rs.getString("Sex"), rs.getString("Bdate"), rs.getString("Relationship")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dependentData;
    }

    // PROJECT 테이블 데이터 조회 메서드
    public List<Object[]> getProjectData() {
        List<Object[]> projectData = new ArrayList<>();
        String query = "SELECT Pname, Pnumber, Plocation, Dname FROM PROJECT JOIN DEPARTMENT ON Dnumber = Dnum";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                projectData.add(new Object[]{
                        rs.getString("Pname"), rs.getString("Pnumber"),
                        rs.getString("Plocation"), rs.getString("Dname")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return projectData;
    }
}
