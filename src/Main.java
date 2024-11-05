import javax.swing.*;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;


public class Main {
    public static void main(String[] args) {

        DBManage dbManage = new DBManage(); // DBManage 객체 생성 및 연결 시도

        // 연결이 성공적으로 이루어졌는지 확인하는 메시지 출력
        if (dbManage.getConnection() != null) {
            System.out.println("데이터베이스에 성공적으로 연결되었습니다.");

            // GUI 생성 및 조회 결과 표시
            SwingUtilities.invokeLater(() -> createAndShowGUI(dbManage));
        } else {
            System.out.println("데이터베이스 연결에 실패했습니다.");
        }

        // 프로그램 종료 시 DB 연결 종료
        dbManage.close();
    }


    private static void createAndShowGUI(DBManage dbManage) {
        JFrame frame = new JFrame("COMPANY DB Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        ResultSet resultSet = dbManage.queryData();

        JTable table = new JTable();
        try {
            table.setModel(DBManage.buildTableModel(resultSet));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.setVisible(true);
    }
}