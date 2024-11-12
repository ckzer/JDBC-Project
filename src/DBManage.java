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


    // 조건 검색
    public ResultSet searchEmployeesByCondition(String searchType, String searchValue, ArrayList<String> selectedColumns) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ").append(String.join(", ", selectedColumns))
                .append(" FROM EMPLOYEE e ")
                .append("JOIN DEPARTMENT d ON e.Dno = d.Dnumber ")
                .append("LEFT JOIN EMPLOYEE s ON e.Super_ssn = s.Ssn ");

        if (!searchType.equals("All") && !searchValue.isEmpty()) {
            queryBuilder.append("WHERE ");
            switch(searchType) {
                case "Department":
                    queryBuilder.append("d.Dname = ?");
                    break;
                case "Gender":
                    queryBuilder.append("e.Sex = ?");
                    break;
                case "Salary":
                    queryBuilder.append("e.Salary >= ?");
                    break;
                case "Name":
                    queryBuilder.append("CONCAT(e.Fname, ' ', e.Minit, ' ', e.Lname) LIKE ?");
                    break;
                case "SSN":
                    queryBuilder.append("e.Ssn = ?");
                    break;
                case "Birth Date":
                    queryBuilder.append("e.Bdate = ?");
                    break;
                case "Address":
                    queryBuilder.append("e.Address LIKE ?");
                    break;
                case "Supervisor":
                    queryBuilder.append("CONCAT(s.Fname, ' ', s.Minit, ' ', s.Lname) LIKE ?");
                    break;
            }
        }

        try {
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(queryBuilder.toString());

            if (!searchType.equals("All") && !searchValue.isEmpty()) {
                if (searchType.equals("Salary")) {
                    pstmt.setDouble(1, Double.parseDouble(searchValue));
                } else if (searchType.equals("Name") || searchType.equals("Address") ||
                        searchType.equals("Supervisor")) {
                    pstmt.setString(1, "%" + searchValue + "%");
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
        // SSN 체크박스에 체크가 되어있어야만 SSN리스트를 전달받아 삭제가 가능하다.
        if (ssnList == null || ssnList.isEmpty()) {
            return "SSN 검색 조건 활성화가 필요합니다.";
        }
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


    // 직원 추가
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

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public boolean updateEmployeeName(String ssn, String fullName) {
        String[] nameParts = fullName.split(" ");
        if (nameParts.length != 3) {
            return false;
        }

        String query = "UPDATE EMPLOYEE SET Fname = ?, Minit = ?, Lname = ? WHERE Ssn = ?";

        try {
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, nameParts[0]); // Fname
            pstmt.setString(2, nameParts[1]); // Minit
            pstmt.setString(3, nameParts[2]); // Lname
            pstmt.setString(4, ssn);

            int result = pstmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 직원 정보 수정
    public boolean updateEmployee(String ssn, String field, String value) {
        String query = "";
        try {
            switch(field) {
                case "Name":
                    return updateEmployeeName(ssn, value);
                case "SSN":
                    query = "UPDATE EMPLOYEE SET Ssn = ? WHERE Ssn = ?";
                    break;
                case "Birth Date":
                    query = "UPDATE EMPLOYEE SET Bdate = ? WHERE Ssn = ?";
                    break;
                case "Address":
                    query = "UPDATE EMPLOYEE SET Address = ? WHERE Ssn = ?";
                    break;
                case "Sex":
                    query = "UPDATE EMPLOYEE SET Sex = ? WHERE Ssn = ?";
                    break;
                case "Salary":
                    query = "UPDATE EMPLOYEE SET Salary = ? WHERE Ssn = ?";
                    break;
                case "Supervisor":
                    if (value.equals("NULL")) {
                        query = "UPDATE EMPLOYEE SET Super_ssn = NULL WHERE Ssn = ?";
                        Connection conn = getConnection();
                        PreparedStatement pstmt = conn.prepareStatement(query);
                        pstmt.setString(1, ssn);
                        return pstmt.executeUpdate() > 0;
                    } else {
                        query = "UPDATE EMPLOYEE SET Super_ssn = (SELECT Ssn FROM (SELECT Ssn FROM EMPLOYEE WHERE CONCAT(Fname, ' ', Minit, ' ', Lname) = ?) AS temp) WHERE Ssn = ?";
                    }
                    break;
                case "Department":
                    query = "UPDATE EMPLOYEE SET Dno = (SELECT Dnumber FROM DEPARTMENT WHERE Dname = ?) WHERE Ssn = ?";
                    break;
                default:
                    return false;
            }

            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(query);

            if (field.equals("Salary")) {
                pstmt.setDouble(1, Double.parseDouble(value));
            } else {
                pstmt.setString(1, value);
            }
            pstmt.setString(2, ssn);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Works_On 테이블 데이터를 List<Object[]> 형식으로 반환하는 메서드
    public List<Object[]> getWorksOnData() {
        List<Object[]> worksOnData = new ArrayList<>();
        String query = "SELECT Essn, Pno, Hours FROM WORKS_ON";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            if (!rs.isBeforeFirst()) {  // ResultSet이 비어있는지 확인
                System.out.println("No data found in Works_On table.");
            } else {
                while (rs.next()) {
                    Object[] rowData = new Object[3];
                    rowData[0] = rs.getString("Essn");
                    rowData[1] = rs.getString("Pno");
                    rowData[2] = rs.getString("Hours");
                    worksOnData.add(rowData);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return worksOnData;
    }


    // DEPENDENT 테이블 데이터를 List<Object[]> 형식으로 반환하는 메서드
    public List<Object[]> getDependentData() {
        List<Object[]> worksOnData = new ArrayList<>();
        String query = "SELECT D.Essn, E.Fname, D.Dependent_name, D.Sex, D.Bdate, D.Relationship FROM DEPENDENT D, EMPLOYEE E WHERE D.Essn = E.Ssn";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            if (!rs.isBeforeFirst()) {  // ResultSet이 비어있는지 확인
                System.out.println("No data found in Works_On table.");
            } else {
                while (rs.next()) {
                    Object[] rowData = new Object[6];
                    rowData[0] = rs.getString("D.Essn");
                    rowData[1] = rs.getString("E.Fname");
                    rowData[2] = rs.getString("D.Dependent_name");
                    rowData[3] = rs.getString("D.Sex");
                    rowData[4] = rs.getString("D.Bdate");
                    rowData[5] = rs.getString("D.Relationship");
                    worksOnData.add(rowData);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return worksOnData;
    }


    // Works_On 테이블 데이터를 List<Object[]> 형식으로 반환하는 메서드
    public List<Object[]> getProjectData() {
        List<Object[]> worksOnData = new ArrayList<>();
        String query = "SELECT Pname, Pnumber, Plocation, Dname FROM PROJECT, DEPARTMENT WHERE Dnumber = Dnum";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            if (!rs.isBeforeFirst()) {  // ResultSet이 비어있는지 확인
                System.out.println("No data found in Works_On table.");
            } else {
                while (rs.next()) {
                    Object[] rowData = new Object[4];
                    rowData[0] = rs.getString("Pname");
                    rowData[1] = rs.getString("Pnumber");
                    rowData[2] = rs.getString("Plocation");
                    rowData[3] = rs.getString("Dname");
                    worksOnData.add(rowData);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return worksOnData;
    }
}