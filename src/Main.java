import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Main extends JFrame {
    private DBManage dbManage; // DB와 연결 & 데이터 가져옴
    private JTable employeeTable; // 직원 정보 테이블, 데이터 저장
    private DefaultTableModel model;
    private JComboBox searchRangeBox; // 검색 범위 및 항목 설정하는 드롭다운&체크박스
    private JComboBox<String> deptComboBox; // 부서 선택 콤보박스
    private JComboBox<String> genderComboBox; // 성별 선택 콤보박스
    private JTextField searchValueField; // 연봉 입력 필드
    private JCheckBox nameBox, ssnBox, bdateBox, addressBox, sexBox, salaryBox, supervisorBox, departmentBox;
    private JLabel selectedCountLabel; // 선택된 직원수 표시

    public Main() {
        dbManage = new DBManage(); // DB와 연결 준비

        // 기본 설정
        setTitle("Information Retrieval System"); // 창 제목
        setSize(1000, 600); // 창 크기
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 닫기 버튼 설정
        setLayout(new BorderLayout());

        // 검색 범위 설정
        JPanel searchRangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchRangePanel.add(new JLabel("검색 범위"));
        searchRangeBox = new JComboBox<>(new String[]{"전체", "부서", "성별", "연봉"});
        searchRangePanel.add(searchRangeBox);

        // 부서 선택 콤보박스
        deptComboBox = new JComboBox<>(new String[]{"Research", "Administration", "Headquarters"});
        deptComboBox.setVisible(false);
        searchRangePanel.add(deptComboBox);

        // 성별 선택 콤보박스
        genderComboBox = new JComboBox<>(new String[]{"F", "M"});
        genderComboBox.setVisible(false);
        searchRangePanel.add(genderComboBox);

        // 연봉 입력 필드
        searchValueField = new JTextField(10);
        searchValueField.setVisible(false);
        searchRangePanel.add(searchValueField);

        // 검색 범위 변경 이벤트 리스너
        searchRangeBox.addActionListener(e -> {
            String selectedType = (String) searchRangeBox.getSelectedItem();
            deptComboBox.setVisible(selectedType.equals("부서"));
            genderComboBox.setVisible(selectedType.equals("성별"));
            searchValueField.setVisible(selectedType.equals("연봉"));
        });

        // 검색 항목 설정
        JPanel searchAttributesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchAttributesPanel.add(new JLabel("검색 항목"));

        // 각각의 JCheckBox 초기화
        nameBox = new JCheckBox("Name");
        ssnBox = new JCheckBox("SSN");
        bdateBox = new JCheckBox("Bdate");
        addressBox = new JCheckBox("Address");
        sexBox = new JCheckBox("Sex");
        salaryBox = new JCheckBox("Salary");
        supervisorBox = new JCheckBox("Supervisor");
        departmentBox = new JCheckBox("Department");

        // 각 JCheckBox를 searchAttributesPanel 패널에 추가
        searchAttributesPanel.add(nameBox);
        searchAttributesPanel.add(ssnBox);
        searchAttributesPanel.add(bdateBox);
        searchAttributesPanel.add(addressBox);
        searchAttributesPanel.add(sexBox);
        searchAttributesPanel.add(salaryBox);
        searchAttributesPanel.add(supervisorBox);
        searchAttributesPanel.add(departmentBox);

        // 검색 버튼을 패널에 추가
        JButton searchButton = new JButton("검색");
        searchButton.addActionListener(e -> loadEmployeeData());
        searchAttributesPanel.add(searchButton);

        // 상단 패널에 검색 범위와 검색 항목 패널을 추가
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(searchRangePanel);
        topPanel.add(searchAttributesPanel);
        add(topPanel, BorderLayout.NORTH);

        // 테이블 및 스크롤 패널을 센터에 추가
        model = new DefaultTableModel() {
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? Boolean.class : String.class;
            }
        };
        employeeTable = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(employeeTable);
        add(scrollPane, BorderLayout.CENTER);

        // 선택한 직원 수 표시 패널
        JPanel countPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectedCountLabel = new JLabel("선택한 직원: ");
        countPanel.add(selectedCountLabel);

        // 수정 및 삭제 기능 패널
        JPanel updatePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        updatePanel.add(new JLabel("수정:"));
        JComboBox updateFieldBox = new JComboBox<>(new String[]{"Name", "SSN", "Bdate", "Address", "Sex", "Salary", "Supervisor", "Department"});
        JTextField updateValueField = new JTextField(10);
        JButton updateButton = new JButton("UPDATE");
        updatePanel.add(updateFieldBox);
        updatePanel.add(updateValueField);
        updatePanel.add(updateButton);

        // 삭제 버튼 패널
        JPanel deletePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton deleteButton = new JButton("선택한 데이터 삭제");
        deletePanel.add(deleteButton);
        
        // 직원 추가 버튼
        JButton addEmployeeButton = new JButton("직원 추가");
        addEmployeeButton.addActionListener(e -> showAddEmployeeDialog());
        deletePanel.add(addEmployeeButton, 0);


        deleteButton.addActionListener(e -> {
            List<String> selectedSSNs = new ArrayList<>(); // java.util.List 사용
            for (int i = 0; i < employeeTable.getRowCount(); i++) {
                Boolean isChecked = (Boolean) employeeTable.getValueAt(i, 0); // 체크박스 열이 0번째 열
                if (isChecked != null && isChecked) {
                    String ssn = (String) employeeTable.getValueAt(i, employeeTable.getColumnModel().getColumnIndex("SSN")); // SSN 열 인덱스를 정확히 가져옴
                    System.out.println("Selected for deletion, SSN: " + ssn);
                    selectedSSNs.add(ssn);
                }
            }
            if (!selectedSSNs.isEmpty()) {
                String result = dbManage.deleteEmployees(selectedSSNs);
                if (result.equals("삭제할 수 없는 직원입니다")) {
                    JOptionPane.showMessageDialog(null, result, "경고", JOptionPane.WARNING_MESSAGE);
                } else if (result.equals("삭제 완료")) {
                    loadEmployeeData(); // 체크박스 상태에 따라 테이블 다시 조회
                } else {
                    JOptionPane.showMessageDialog(null, result, "오류", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(null, "삭제할 직원이 선택되지 않았습니다.");
            }
        });


        // 하단 패널에 추가
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(countPanel, BorderLayout.WEST);
        bottomPanel.add(updatePanel, BorderLayout.CENTER);
        bottomPanel.add(deletePanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // 직원 정보 로드
        loadEmployeeData();
        addRowSelectionListener();        
    }

    // 직원 정보 로드 메서드
    private void loadEmployeeData() {
        model.setRowCount(0); // 테이블 초기화

        // 선택한 검색 항목에 따라 표시할 컬럼을 설정
        ArrayList<String> selectedColumns = new ArrayList<>();
        if (nameBox.isSelected()) selectedColumns.add("CONCAT(e.Fname, ' ', e.Minit, ' ', e.Lname) AS NAME");
        if (ssnBox.isSelected()) selectedColumns.add("e.Ssn AS SSN");
        if (bdateBox.isSelected()) selectedColumns.add("e.Bdate AS BDATE");
        if (addressBox.isSelected()) selectedColumns.add("e.Address AS ADDRESS");
        if (sexBox.isSelected()) selectedColumns.add("e.Sex AS SEX");
        if (salaryBox.isSelected()) selectedColumns.add("e.Salary AS SALARY");
        if (supervisorBox.isSelected()) selectedColumns.add("CONCAT(s.Fname, ' ', s.Minit, ' ', s.Lname) AS SUPERVISOR");
        if (departmentBox.isSelected()) selectedColumns.add("d.Dname AS DEPARTMENT");

        // 선택한 항목이 없으면 기본으로 모든 항목을 추가
        if (selectedColumns.isEmpty()) {
            selectedColumns.add("CONCAT(e.Fname, ' ', e.Minit, ' ', e.Lname) AS NAME");
            selectedColumns.add("e.Ssn AS SSN");
            selectedColumns.add("e.Bdate AS BDATE");
            selectedColumns.add("e.Address AS ADDRESS");
            selectedColumns.add("e.Sex AS SEX");
            selectedColumns.add("e.Salary AS SALARY");
            selectedColumns.add("CONCAT(s.Fname, ' ', s.Minit, ' ', s.Lname) AS SUPERVISOR");
            selectedColumns.add("d.Dname AS DEPARTMENT");
        }

        // 쿼리 생성
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ").append(String.join(", ", selectedColumns))
                .append(" FROM EMPLOYEE e ")
                .append("JOIN DEPARTMENT d ON e.Dno = d.Dnumber ")
                .append("LEFT JOIN EMPLOYEE s ON e.Super_ssn = s.Ssn");

        // 검색 조건 추가
        String searchType = (String) searchRangeBox.getSelectedItem();
        if (!searchType.equals("전체")) {
            queryBuilder.append(" WHERE ");
            switch(searchType) {
                case "부서":
                    queryBuilder.append("d.Dname = '").append(deptComboBox.getSelectedItem()).append("'");
                    break;
                case "성별":
                    queryBuilder.append("e.Sex = '").append(genderComboBox.getSelectedItem()).append("'");
                    break;
                case "연봉":
                    String salaryValue = searchValueField.getText().trim();
                    if (!salaryValue.isEmpty()) {
                        if (salaryValue.matches("\\d+(\\.\\d+)?")) {
                            queryBuilder.append("e.Salary >= ").append(salaryValue);
                        } else {
                            JOptionPane.showMessageDialog(this, "연봉은 숫자만 입력 가능합니다.");
                            return;
                        }
                    }
                    break;
            }
        }

        // 테이블 모델에 데이터 설정
        ArrayList<String> columnNames = new ArrayList<>();
        columnNames.add("선택");
        for (String column : selectedColumns) {
            if (column.contains(" AS ")) {
                columnNames.add(column.split(" AS ")[1]);
            } else {
                columnNames.add(column);
            }
        }
        model.setColumnIdentifiers(columnNames.toArray(new String[0]));

        // DB에서 결과를 가져와 테이블에 표시
        try {
            ResultSet rs = dbManage.executeCustomQuery(queryBuilder.toString());
            while (rs.next()) {
                ArrayList<Object> rowData = new ArrayList<>();
                rowData.add(false);
                for (int i = 1; i < columnNames.size(); i++) {
                    rowData.add(rs.getObject(columnNames.get(i)));
                }
                model.addRow(rowData.toArray());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        updateSelectedCount();
    }

    private void addRowSelectionListener() {
        model.addTableModelListener(e -> updateSelectedCount());
    }

    private void updateSelectedCount() {
        int selectedCount = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            if ((Boolean) model.getValueAt(i, 0)) {
                selectedCount++;
            }
        }
        selectedCountLabel.setText("선택한 직원: " + selectedCount + "명");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }

    // 직원 추가
    private void showAddEmployeeDialog() {
        JDialog dialog = new JDialog(this, "직원 추가", true);
        dialog.setSize(400, 500);
        dialog.setLayout(new GridLayout(11, 2, 5, 5));

        // 입력 필드 생성
        JTextField fnameField = new JTextField();
        JTextField minitField = new JTextField();
        JTextField lnameField = new JTextField();
        JTextField ssnField = new JTextField();
        JTextField bdateField = new JTextField();
        JTextField addressField = new JTextField();
        JComboBox<String> sexCombo = new JComboBox<>(new String[]{"F", "M"});
        JTextField salaryField = new JTextField();
        JTextField superSsnField = new JTextField();
        JTextField dnoField = new JTextField();

        // 필드 추가
        dialog.add(new JLabel("First Name:"));
        dialog.add(fnameField);
        dialog.add(new JLabel("Middle Init.:"));
        dialog.add(minitField);
        dialog.add(new JLabel("Last Name:"));
        dialog.add(lnameField);
        dialog.add(new JLabel("Ssn:"));
        dialog.add(ssnField);
        dialog.add(new JLabel("Birthdate:"));
        dialog.add(bdateField);
        dialog.add(new JLabel("Address:"));
        dialog.add(addressField);
        dialog.add(new JLabel("Sex:"));
        dialog.add(sexCombo);
        dialog.add(new JLabel("Salary:"));
        dialog.add(salaryField);
        dialog.add(new JLabel("Super_ssn:"));
        dialog.add(superSsnField);
        dialog.add(new JLabel("Dno:"));
        dialog.add(dnoField);

        JButton addButton = new JButton("정보 추가하기");
        dialog.add(addButton);

        // "정보 추가하기" 버튼 동작
        addButton.addActionListener(e -> {
            String fname = fnameField.getText();
            String minit = minitField.getText();
            String lname = lnameField.getText();
            String ssn = ssnField.getText();
            String bdate = bdateField.getText();
            String address = addressField.getText();
            String sex = (String) sexCombo.getSelectedItem();
            String salaryText = salaryField.getText();
            String superSsn = superSsnField.getText().isEmpty() ? null: superSsnField.getText();
            String dnoText = dnoField.getText();

            // 데이터 검증
            if (fname.isEmpty() || lname.isEmpty() || ssn.isEmpty() || bdate.isEmpty() || salaryText.isEmpty() || dnoText.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "모든 필수 정보를 입력하세요.", "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                double salary = Double.parseDouble(salaryText);
                int dno = Integer.parseInt(dnoText);

                // DBManage 데이터 삽입
                boolean success = dbManage.addEmployee(fname, minit, lname, ssn, bdate, address, sex, salary, superSsn, dno);
                if (success) {
                    JOptionPane.showMessageDialog(dialog, "직원 정보가 추가되었습니다.");
                    loadEmployeeData();
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "직원 정보 추가에 실패하였습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "연봉 및 부서 번호는 숫자로 입력해야 합니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        
    }
}