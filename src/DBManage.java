import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;


public class DBManage extends JFrame {
    private Connection conn;
    private JComboBox<String> selAttributes;
    private JComboBox<String> selGender;
    private JComboBox<String> selDept;
    private JTextField userIn;
    private JTable resTable;
    private DefaultTableModel model;

    private final String[] attributes = {"전체", "부서", "이름", "주민번호", "성별", "생년월일", "주소", "임금"};
    private final String[] transAttribute = {"*", "Dname", "Name", "Ssn", "Sex", "Bdate", "Address", "Salary"};
    private final String[] gender = {"남성", "여성"};
    private final String[] dept = {"Research", "Administration", "Headquarters"};

    public DBManage() {
        super("직원 정보 검색 시스템");
        setLayout(new BorderLayout());
        connectDB();
        initializeUI();
    }
    private void connectDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/mydb?serverTimezone=UTC";
            conn = DriverManager.getConnection(url, "root", "root");
            System.out.println("데이터베이스 연결 성공");
        } catch (Exception e) {
            System.out.println("데이터베이스 연결 실패: " + e.getMessage());
            System.exit(1);
        }
    }

    private void initializeUI() {
        // 검색 패널
        JPanel searchPanel = createSearchPanel();

        // 결과 테이블 패널
        JPanel resultPanel = createResultPanel();

        // 메인 패널에 추가
        add(searchPanel, BorderLayout.NORTH);
        add(resultPanel, BorderLayout.CENTER);

        // 전체 직원 보고서 버튼
        JButton reportBtn = new JButton("전체 직원 보고서");
        reportBtn.addActionListener(e -> showFullReport());
        add(reportBtn, BorderLayout.SOUTH);

        setSize(1000, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("검색 조건"));

        selAttributes = new JComboBox<>(attributes);
        userIn = new JTextField(20);
        JButton searchBtn = new JButton("검색");

        searchBtn.addActionListener(e -> performSearch());

        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("검색 조건:"));
        inputPanel.add(selAttributes);
        inputPanel.add(userIn);
        inputPanel.add(searchBtn);

        panel.add(inputPanel);
        return panel;
    }

    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("검색 결과"));

        model = new DefaultTableModel();
        model.setColumnIdentifiers(new String[]{"이름", "주민번호", "부서", "급여", "주소"});
        resTable = new JTable(model);

        panel.add(new JScrollPane(resTable));
        return panel;
    }

    private void performSearch() {
        String searchType = selAttributes.getSelectedItem().toString();
        String searchValue = userIn.getText().trim();

        String query = "SELECT e.Fname, e.Minit, e.Lname, e.Ssn, d.Dname, e.Salary, e.Address " +
                "FROM EMPLOYEE e JOIN DEPARTMENT d ON e.Dno = d.Dnumber WHERE ";

        try {
            PreparedStatement pstmt;
            if (searchType.equals("전체")) {
                query = query.substring(0, query.length() - 7);
                pstmt = conn.prepareStatement(query);
            } else {
                // 검색 조건에 따른 쿼리 생성
                switch(searchType) {
                    case "이름":
                        query += "e.Fname LIKE ?";
                        pstmt = conn.prepareStatement(query);
                        pstmt.setString(1, "%" + searchValue + "%");
                        break;
                    case "부서":
                        query += "d.Dname = ?";
                        pstmt = conn.prepareStatement(query);
                        pstmt.setString(1, searchValue);
                        break;
                    default:
                        query += "e." + transAttribute[Arrays.asList(attributes).indexOf(searchType)] + " LIKE ?";
                        pstmt = conn.prepareStatement(query);
                        pstmt.setString(1, "%" + searchValue + "%");
                }
            }

            ResultSet rs = pstmt.executeQuery();
            updateTable(rs);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "검색 오류: " + e.getMessage());
        }
    }

    private void showFullReport() {
        try {
            String query = "SELECT e.Fname, e.Minit, e.Lname, e.Ssn, d.Dname, e.Salary, e.Address " +
                    "FROM EMPLOYEE e JOIN DEPARTMENT d ON e.Dno = d.Dnumber";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            updateTable(rs);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "보고서 생성 오류: " + e.getMessage());
        }
    }

    private void updateTable(ResultSet rs) throws SQLException {
        model.setRowCount(0);
        while (rs.next()) {
            String fullName = rs.getString("Fname") + " " +
                    rs.getString("Minit") + " " +
                    rs.getString("Lname");
            model.addRow(new Object[]{
                    fullName,
                    rs.getString("Ssn"),
                    rs.getString("Dname"),
                    rs.getDouble("Salary"),
                    rs.getString("Address")
            });
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new DBManage().setVisible(true);
        });
    }
}