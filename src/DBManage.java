import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.table.DefaultTableModel;


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

    // 특정 테이블의 데이터를 조회하는 메서드
    public ResultSet queryData() {
        String query = "SELECT * FROM your_table_name"; // 조회할 테이블 이름으로 수정하세요
        ResultSet resultSet = null;
        try {
            Statement statement = connection.createStatement();
            resultSet = statement.executeQuery(query);
        } catch (SQLException e) {
            System.out.println("데이터 조회 오류: " + e.getMessage());
            e.printStackTrace();
        }
        return resultSet;
    }

    // ResultSet을 JTable에서 사용할 수 있는 TableModel로 변환하는 메서드
    public static DefaultTableModel buildTableModel(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();

        // 열 이름 가져오기
        int columnCount = metaData.getColumnCount();
        String[] columnNames = new String[columnCount];
        for (int i = 1; i <= columnCount; i++) {
            columnNames[i - 1] = metaData.getColumnName(i);
        }

        // 데이터 추가
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        while (rs.next()) {
            Object[] row = new Object[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                row[i - 1] = rs.getObject(i);
            }
            model.addRow(row);
        }
        return model;
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
