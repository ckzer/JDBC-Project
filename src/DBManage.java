import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBManage {
    private static final String url = "jdbc:mysql://localhost:3306/mydb?serverTimeZone=UTC";
    private static final String user = "root";
    private static final String password = "root";

    // 데이터베이스 연결 생성
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 모든 직원 정보 조회 메서드
    public ResultSet getAllEmployees() {
        String query = "SELECT e.Fname, e.Minit, e.Lname, e.Ssn, e.Bdate, e.Address, e.Sex, e.Salary, " +
                "d.Dname AS DepartmentName, CONCAT(s.Fname, ' ', s.Minit, ' ', s.Lname) AS Supervisor " +
                "FROM EMPLOYEE e " +
                "JOIN DEPARTMENT d ON e.Dno = d.Dnumber " +
                "LEFT JOIN EMPLOYEE s ON e.Super_ssn = s.Ssn";
        return executeCustomQuery(query);
    }

    // 조건 검색 메서드
    public ResultSet searchEmployeesByCondition(String searchType, String searchValue, ArrayList<String> selectedColumns) {
        StringBuilder queryBuilder = new StringBuilder("SELECT ");
        queryBuilder.append(String.join(", ", selectedColumns))
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

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(queryBuilder.toString())) {

            if (!searchType.equals("전체") && !searchValue.isEmpty()) {
                if ("연봉".equals(searchType)) {
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

    // 부서별 평균 급여 조회 메서드
    public ResultSet getDepartmentAverageSalary() {
        String query = "SELECT d.Dname, AVG(e.Salary) as AvgSalary " +
                "FROM EMPLOYEE e JOIN DEPARTMENT d ON e.Dno = d.Dnumber " +
                "GROUP BY d.Dname";
        return executeCustomQuery(query);
    }

    // 선택한 직원 삭제 메서드
    public String deleteEmployees(List<String> ssnList) {
        if (ssnList == null || ssnList.isEmpty()) {
            return "SSN 검색 조건 활성화가 필요합니다.";
        }
        String deleteWorksOnQuery = "DELETE FROM WORKS_ON WHERE Essn = ?";
        String deleteDependentQuery = "DELETE FROM DEPENDENT WHERE Essn = ?";
        String deleteQuery = "DELETE FROM EMPLOYEE WHERE Ssn = ?";
        String updateSuperSsnQuery = "UPDATE EMPLOYEE SET Super_ssn = NULL WHERE Super_ssn = ?";
        String updateDepartmentMgrQuery = "UPDATE DEPARTMENT SET Mgr_ssn = '888665555' WHERE Mgr_ssn = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement deleteWorksOnPstmt = conn.prepareStatement(deleteWorksOnQuery);
                 PreparedStatement deleteDependentPstmt = conn.prepareStatement(deleteDependentQuery);
                 PreparedStatement deletePstmt = conn.prepareStatement(deleteQuery);
                 PreparedStatement updatePstmt = conn.prepareStatement(updateSuperSsnQuery);
                 PreparedStatement updateDeptMgrPstmt = conn.prepareStatement(updateDepartmentMgrQuery)) {

                for (String ssn : ssnList) {
                    if ("888665555".equals(ssn)) {
                        return "삭제할 수 없는 직원입니다.";
                    }
                    deleteWorksOnPstmt.setString(1, ssn);
                    deleteWorksOnPstmt.executeUpdate();
                    deleteDependentPstmt.setString(1, ssn);
                    deleteDependentPstmt.executeUpdate();
                    updateDeptMgrPstmt.setString(1, ssn);
                    updateDeptMgrPstmt.executeUpdate();
                    updatePstmt.setString(1, ssn);
                    updatePstmt.executeUpdate();
                    deletePstmt.setString(1, ssn);
                    deletePstmt.addBatch();
                }
                deletePstmt.executeBatch();
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

    // 사용자 지정 쿼리 실행 메서드
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

    // 직원 추가 메서드
    public boolean addEmployee(String fname, String minit, String lname, String ssn, String bdate, String address,
                               String sex, double salary, String superSsn, int dno) {
        String query = "INSERT INTO EMPLOYEE (Fname, Minit, Lname, Ssn, Bdate, Address, Sex, Salary, Super_ssn, Dno) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
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

    // Works_On 데이터 반환 메서드
    public List<Object[]> getWorksOnData() {
        List<Object[]> worksOnData = new ArrayList<>();
        String query = "SELECT Essn, Pno, Hours FROM WORKS_ON";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                worksOnData.add(new Object[]{rs.getString("Essn"), rs.getString("Pno"), rs.getString("Hours")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return worksOnData;
    }

    // Dependent 데이터 반환 메서드
    public List<Object[]> getDependentData() {
        List<Object[]> dependentData = new ArrayList<>();
        String query = "SELECT D.Essn, E.Fname, D.Dependent_name, D.Sex, D.Bdate, D.Relationship FROM DEPENDENT D, EMPLOYEE E WHERE D.Essn = E.Ssn";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                dependentData.add(new Object[]{rs.getString("D.Essn"), rs.getString("E.Fname"),
                        rs.getString("D.Dependent_name"), rs.getString("D.Sex"),
                        rs.getString("D.Bdate"), rs.getString("D.Relationship")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dependentData;
    }

    // Project 데이터 반환 메서드
    public List<Object[]> getProjectData() {
        List<Object[]> projectData = new ArrayList<>();
        // Replace 'Dname' with the actual column name in your DEPARTMENT table
        String query = "SELECT Pname, Pnumber, Plocation, d.Dname FROM PROJECT p JOIN DEPARTMENT d ON p.Dnum = d.Dnumber";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Object[] rowData = new Object[4];
                rowData[0] = rs.getString("Pname");
                rowData[1] = rs.getString("Pnumber");
                rowData[2] = rs.getString("Plocation");
                rowData[3] = rs.getString("Dname");  // Adjust here if necessary
                projectData.add(rowData);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return projectData;
    }

    // 필드 업데이트 메서드
    public boolean updateField(String viewType, String identifier, String secondaryIdentifier, String field, String newValue) {
        String query;

        switch (viewType) {
            case "works_on":
                query = "UPDATE WORKS_ON SET " + field + " = ? WHERE Essn = ? AND Pno = ?";
                break;
            case "dependent":
                query = "UPDATE DEPENDENT SET " + field + " = ? WHERE Essn = ? AND Dependent_name = ?";
                break;
            case "project":
                query = "UPDATE PROJECT SET " + field + " = ? WHERE Pnumber = ?";
                break;
            default:
                query = "UPDATE EMPLOYEE SET " + field + " = ? WHERE Ssn = ?";
                break;
        }

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newValue);
            pstmt.setString(2, identifier);

            if ("works_on".equals(viewType) || "dependent".equals(viewType)) {
                pstmt.setString(3, secondaryIdentifier);
            }

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
