import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Main extends JFrame {
    private DBManage dbManage;
    private JTable employeeTable;
    private DefaultTableModel model;
    private JComboBox<String> searchRangeBox, deptComboBox, genderComboBox, updateFieldBox;
    private JTextField searchValueField, updateValueField;
    private JCheckBox nameBox, ssnBox, bdateBox, addressBox, sexBox, salaryBox, supervisorBox, departmentBox;
    private JLabel selectedCountLabel;
    private JButton updateButton;
    private JPanel updatePanel;
    private String currentViewMode = "employee";

    public Main() {
        dbManage = new DBManage(); // DB와 연결 준비

        setTitle("Information Retrieval System");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize update components
        updateFieldBox = new JComboBox<>();
        updateValueField = new JTextField(10);
        updateButton = new JButton("UPDATE");

        // Add the update button listener
        updateButton.addActionListener(e -> {
            updateData();
        });

        // Configure the update panel with the instance variables
        updatePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        updatePanel.add(new JLabel("수정:"));
        updatePanel.add(updateFieldBox);
        updatePanel.add(updateValueField);
        updatePanel.add(updateButton);

        // 근무시간 조회 버튼
        JButton worksOnButton = new JButton("근무시간 조회");
        worksOnButton.addActionListener(e -> {
            switchView("works_on", new String[]{"Pno", "Hours"});
        });

        // 가족 정보 조회 버튼
        JButton dependentButton = new JButton("가족 정보 조회");
        dependentButton.addActionListener(e -> {
            switchView("dependent", new String[]{"Fname", "Dependent_name", "Sex", "Bdate", "Relationship"});
        });

        // 프로젝트 조회 버튼
        JButton projectButton = new JButton("프로젝트 조회");
        projectButton.addActionListener(e -> {
            switchView("project", new String[]{"Pname", "Pnumber", "Plocation"});
        });

        // Adding buttons to the search range panel
        JPanel searchRangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchRangePanel.add(new JLabel("검색 범위"));
        searchRangeBox = new JComboBox<>(new String[]{"전체", "부서", "성별", "연봉"});
        searchRangePanel.add(searchRangeBox);
        searchRangePanel.add(worksOnButton);
        searchRangePanel.add(dependentButton);
        searchRangePanel.add(projectButton);

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

        searchAttributesPanel.add(nameBox);
        searchAttributesPanel.add(ssnBox);
        searchAttributesPanel.add(bdateBox);
        searchAttributesPanel.add(addressBox);
        searchAttributesPanel.add(sexBox);
        searchAttributesPanel.add(salaryBox);
        searchAttributesPanel.add(supervisorBox);
        searchAttributesPanel.add(departmentBox);

        JButton searchButton = new JButton("검색");
        searchButton.addActionListener(e -> loadEmployeeData());
        searchAttributesPanel.add(searchButton);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(searchRangePanel);
        topPanel.add(searchAttributesPanel);
        add(topPanel, BorderLayout.NORTH);

        model = new DefaultTableModel() {
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? Boolean.class : String.class;
            }
        };
        employeeTable = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(employeeTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel countPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectedCountLabel = new JLabel("선택한 직원: ");
        countPanel.add(selectedCountLabel);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(countPanel, BorderLayout.WEST);
        bottomPanel.add(updatePanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        loadEmployeeData();
        addRowSelectionListener();
    }

    // View 전환 및 필드 제한 메서드
    private void switchView(String viewMode, String[] editableFields) {
        currentViewMode = viewMode;
        updateFieldBox.removeAllItems();
        for (String field : editableFields) {
            updateFieldBox.addItem(field);
        }
        refreshCurrentView();
    }

    private void refreshCurrentView() {
        switch (currentViewMode) {
            case "works_on" -> displayWorksOnData();
            case "dependent" -> displayDependentData();
            case "project" -> displayProjectData();
            default -> loadEmployeeData();
        }
    }

    private void updateData() {
        int selectedRow = employeeTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "업데이트할 행을 선택하세요.");
            return;
        }
        String selectedField = (String) updateFieldBox.getSelectedItem();
        String newValue = updateValueField.getText().trim();

        if (selectedField == null || newValue.isEmpty()) {
            JOptionPane.showMessageDialog(this, "수정할 필드와 값을 선택하세요.");
            return;
        }

        boolean updateSuccess;
        String identifier = getIdentifierForCurrentView(selectedRow);

        if (currentViewMode.equals("works_on")) {
            String pno = (String) model.getValueAt(selectedRow, model.findColumn("Pno"));
            updateSuccess = dbManage.updateField("works_on", identifier, pno, selectedField, newValue);
        } else if (currentViewMode.equals("dependent")) {
            String dependentName = (String) model.getValueAt(selectedRow, model.findColumn("Dependent_name"));
            updateSuccess = dbManage.updateField("dependent", identifier, dependentName, selectedField, newValue);
        } else if (currentViewMode.equals("project")) {
            updateSuccess = dbManage.updateField("project", identifier, null, selectedField, newValue);
        } else {
            updateSuccess = dbManage.updateField("employee", identifier, null, selectedField, newValue);
        }

        if (updateSuccess) {
            JOptionPane.showMessageDialog(this, "업데이트 성공!");
            refreshCurrentView();
        } else {
            JOptionPane.showMessageDialog(this, "업데이트 실패.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getIdentifierForCurrentView(int selectedRow) {
        return switch (currentViewMode) {
            case "works_on", "dependent" -> (String) employeeTable.getValueAt(selectedRow, model.findColumn("Essn"));
            case "project" -> (String) employeeTable.getValueAt(selectedRow, model.findColumn("Pnumber"));
            default -> (String) employeeTable.getValueAt(selectedRow, model.findColumn("SSN"));
        };
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

    // 직원 정보 로드 메서드 (기본 EMPLOYEE 데이터를 표시)
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

    // Works_On 테이블 데이터 표시 메서드
        private void displayWorksOnData() {
            model.setRowCount(0); // 테이블 초기화
            String[] columns = {"선택", "Essn", "Pno", "Hours"};
            model.setColumnIdentifiers(columns); // 컬럼 이름 설정

            List<Object[]> worksOnData = dbManage.getWorksOnData(); // DB에서 데이터 가져오기
            if (worksOnData.isEmpty()) {
                System.out.println("No data to display in the UI for Works_On table.");
            } else {
                for (Object[] rowData : worksOnData) {
                    Object[] tableRow = new Object[columns.length];
                    tableRow[0] = false; // 체크박스 기본값 false
                    System.arraycopy(rowData, 0, tableRow, 1, rowData.length);
                    model.addRow(tableRow);
                }
            }
        }

    // Dependent 테이블 데이터 표시 메서드
    private void displayDependentData() {
        model.setRowCount(0); // 테이블 초기화
        String[] columns = {"선택", "Essn", "Fname", "Dependent_name", "Sex", "Bdate", "Relationship"};
        model.setColumnIdentifiers(columns); // 컬럼 이름 설정

        List<Object[]> dependentData = dbManage.getDependentData(); // DB에서 데이터 가져오기
        if (dependentData.isEmpty()) {
            System.out.println("No data to display in the UI for Works_On table.");
        } else {
            for (Object[] rowData : dependentData) {
                Object[] tableRow = new Object[columns.length];
                tableRow[0] = false; // 체크박스 기본값 false
                System.arraycopy(rowData, 0, tableRow, 1, rowData.length);
                model.addRow(tableRow);
            }
        }
    }

    // Project 테이블 데이터 표시 메서드
    private void displayProjectData() {
        model.setRowCount(0); // 테이블 초기화
        String[] columns = {"선택", "Pname", "Pnumber", "Plocation", "Dname"};
        model.setColumnIdentifiers(columns); // 컬럼 이름 설정

        List<Object[]> projectData = dbManage.getProjectData(); // DB에서 데이터 가져오기
        if (projectData.isEmpty()) {
            System.out.println("No data to display in the UI for Works_On table.");
        } else {
            for (Object[] rowData : projectData) {
                Object[] tableRow = new Object[columns.length];
                tableRow[0] = false; // 체크박스 기본값 false
                System.arraycopy(rowData, 0, tableRow, 1, rowData.length);
                model.addRow(tableRow);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}