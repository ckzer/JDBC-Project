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
    private TableMode currentMode = TableMode.EMPLOYEE;
    private JComboBox<String> sexComboBox, supervisorComboBox, departmentComboBox;
    private JButton updateButton;

    public enum TableMode {
        EMPLOYEE, WORKS_ON, DEPENDENT, PROJECT
    }

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
        searchRangeBox = new JComboBox<>(new String[]{
                "All", "Department", "Gender", "Salary",
                "Name", "SSN", "Birth Date", "Address", "Supervisor"
        });
        searchRangePanel.add(searchRangeBox);

        // 부서/성별 선택 콤보박스
        deptComboBox = new JComboBox<>(new String[]{"Research", "Administration", "Headquarters"});
        genderComboBox = new JComboBox<>(new String[]{"F", "M"});
        deptComboBox.setVisible(false);
        genderComboBox.setVisible(false);
        searchRangePanel.add(deptComboBox);
        searchRangePanel.add(genderComboBox);

        // 연봉 입력 필드
        searchValueField = new JTextField(15);
        searchValueField.setVisible(false);
        searchRangePanel.add(searchValueField);

        // 검색 범위 변경 이벤트 리스너
        searchRangeBox.addActionListener(e -> {
            String selectedType = (String) searchRangeBox.getSelectedItem();
            deptComboBox.setVisible(selectedType.equals("Department"));
            genderComboBox.setVisible(selectedType.equals("Gender"));
            searchValueField.setVisible(!selectedType.equals("All") &&
                    !selectedType.equals("Department") &&
                    !selectedType.equals("Gender"));
            if (selectedType.equals("Birth Date")) {
                searchValueField.setToolTipText("YYYY-MM-DD 형태나 YYYY 형태로 입력해 주세요!");
            } else if (selectedType.equals("SSN")) {
                searchValueField.setToolTipText("9자리의 Ssn을 입력해 주세요!");
            }
        });

        // 버튼 추가
        JButton worksOnButton = new JButton("근무시간 조회");
        JButton dependentButton = new JButton("가족 정보 조회");
        JButton projectButton = new JButton("프로젝트 조회");

        // 버튼 클릭 이벤트 설정
        worksOnButton.addActionListener(e -> toggleView(TableMode.WORKS_ON));
        dependentButton.addActionListener(e -> toggleView(TableMode.DEPENDENT));
        projectButton.addActionListener(e -> toggleView(TableMode.PROJECT));

        searchRangePanel.add(worksOnButton);
        searchRangePanel.add(dependentButton);
        searchRangePanel.add(projectButton);

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

        // 수정할 필드 선택 콤보박스
        updateFieldBox = new JComboBox<>(new String[]{
                "Name", "SSN", "Birth Date", "Address", "Sex", "Salary", "Supervisor", "Department"
        });
        updateValueField = new JTextField(10);
        sexComboBox = new JComboBox<>(new String[]{"F", "M"});
        supervisorComboBox = new JComboBox<>(new String[]{"Franklin T Wong", "James E Borg", "Jennifer S Wallace", "NULL"});
        departmentComboBox = new JComboBox<>(new String[]{"Research", "Administration", "Headquarters"});

        updatePanel.add(updateFieldBox);
        updatePanel.add(updateValueField);
        updatePanel.add(sexComboBox);
        updatePanel.add(supervisorComboBox);
        updatePanel.add(departmentComboBox);

        // 필드 선택에 따른 입력 컴포넌트 로직
        updateFieldBox.addActionListener(e -> updateUIBasedOnSelectedField((String) updateFieldBox.getSelectedItem()));


        // 처음에는 모든 특정 콤보 상자 숨기기
        sexComboBox.setVisible(false);
        supervisorComboBox.setVisible(false);
        departmentComboBox.setVisible(false);

        // UPDATE 버튼
        updateButton = new JButton("UPDATE");
        updateButton.addActionListener(e -> {
            boolean success = false;
            if (currentMode == TableMode.WORKS_ON) {
                String essn = getSelectedID();
                String pno = getSelectedPno();
                String field = (String) updateFieldBox.getSelectedItem();
                String newValue = getSelectedFieldValue(field);
                success = updateWorksOnData(essn, pno, field, newValue);
                if (success) {
                    JOptionPane.showMessageDialog(this, "WORKS_ON 테이블이 성공적으로 업데이트되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
                    displayWorksOnData(); // 업데이트 후 데이터 새로고침
                } else {
                    JOptionPane.showMessageDialog(this, "WORKS_ON 테이블 업데이트에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                }
            } else if (currentMode == TableMode.DEPENDENT) {
                String essn = getSelectedID();
                String dependentName = getSelectedDependentName();
                String field = (String) updateFieldBox.getSelectedItem();
                String newValue = getSelectedFieldValue(field);
                success = updateDependentData(essn, dependentName, field, newValue);
                if (success) {
                    JOptionPane.showMessageDialog(this, "DEPENDENT 테이블이 성공적으로 업데이트되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
                    displayDependentData(); // 업데이트 후 데이터 새로고침
                } else {
                    JOptionPane.showMessageDialog(this, "DEPENDENT 테이블 업데이트에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                }
            } else if (currentMode == TableMode.PROJECT) {
                String pnumber = getSelectedPno();
                String field = (String) updateFieldBox.getSelectedItem();
                String newValue = getSelectedFieldValue(field);
                success = updateProjectData(pnumber, field, newValue);
                if (success) {
                    JOptionPane.showMessageDialog(this, "PROJECT 테이블이 성공적으로 업데이트되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
                    displayProjectData(); // 업데이트 후 데이터 새로고침
                } else {
                    JOptionPane.showMessageDialog(this, "PROJECT 테이블 업데이트에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                }
            } else if (currentMode == TableMode.EMPLOYEE) {
                success = updateEmployeeData();
                if (success) {
                    JOptionPane.showMessageDialog(this, "직원 정보가 성공적으로 업데이트되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
                    loadEmployeeData(); // EMPLOYEE 모드에서 UI 업데이트
                } else {
                    JOptionPane.showMessageDialog(this, "직원 정보 업데이트에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        updatePanel.add(updateButton);

        //EMPLOYEE 테이블 기본 설정
        resetUpdateFieldBoxForEmployee();

        // 삭제 버튼 패널
        JPanel deletePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton deleteButton = new JButton("선택한 데이터 삭제");
        deletePanel.add(deleteButton);

        // 직원 추가 버튼
        JButton addEmployeeButton = new JButton("직원 추가");
        addEmployeeButton.addActionListener(e -> showAddEmployeeDialog());
        deletePanel.add(addEmployeeButton, 0);

        deleteButton.addActionListener(e -> {
            List<String> selectedSSNs = new ArrayList<>();
            for (int i = 0; i < employeeTable.getRowCount(); i++) {
                Boolean isChecked = (Boolean) employeeTable.getValueAt(i, 0);
                if (isChecked != null && isChecked) {
                    String ssn = (String) employeeTable.getValueAt(i, employeeTable.getColumnModel().getColumnIndex("SSN"));
                    selectedSSNs.add(ssn);
                }
            }
            if (!selectedSSNs.isEmpty()) {
                String result = dbManage.deleteEmployees(selectedSSNs);
                JOptionPane.showMessageDialog(null, result.equals("삭제 완료") ? "선택한 직원이 삭제되었습니다." : result,
                        result.equals("삭제 완료") ? "성공" : "오류",
                        result.equals("삭제 완료") ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                loadEmployeeData();
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

    private void toggleView(TableMode mode) {
        // Check if the current mode is already the selected mode
        if (currentMode == mode) {
            // If already in the selected mode, switch back to EMPLOYEE view
            currentMode = TableMode.EMPLOYEE;
            loadEmployeeData();
            resetUpdateFieldBoxForEmployee();
        } else {
            // Switch to the selected mode and update the view
            currentMode = mode;
            switch (mode) {
                case WORKS_ON:
                    displayWorksOnData();
                    resetUpdateFieldBox(new String[]{"Pno", "Hours"});
                    break;
                case DEPENDENT:
                    displayDependentData();
                    resetUpdateFieldBox(new String[]{"Fname", "Dependent_name", "Sex", "Bdate", "Relationship"});
                    break;
                case PROJECT:
                    displayProjectData();
                    resetUpdateFieldBox(new String[]{"Pname", "Pnumber", "Plocation", "Dname"});
                    break;
                default:
                    // In case of EMPLOYEE or any unknown mode, fallback to EMPLOYEE data load
                    loadEmployeeData();
                    resetUpdateFieldBoxForEmployee();
                    break;
            }
        }
    }


    // Updates the UI based on the selected field in the update combo box
    private void updateUIBasedOnSelectedField(String selectedField) {
        updateValueField.setVisible(true);
        sexComboBox.setVisible(false);
        supervisorComboBox.setVisible(false);
        departmentComboBox.setVisible(false);
        switch (selectedField) {
            case "Sex":
                updateValueField.setVisible(false);
                sexComboBox.setVisible(true);
                break;
            case "Supervisor":
                updateValueField.setVisible(false);
                supervisorComboBox.setVisible(true);
                break;
            case "Department":
                updateValueField.setVisible(false);
                departmentComboBox.setVisible(true);
                break;
            default:
                updateValueField.setToolTipText(selectedField.equals("Birth Date") ? "YYYY-MM-DD 형식으로 입력하세요" :
                        selectedField.equals("SSN") ? "9자리 숫자로 입력하세요" : "");
        }
    }

    private boolean updateEmployeeData() {
        String selectedSSN = getSelectedID();
        String field = (String) updateFieldBox.getSelectedItem();
        String newValue = getSelectedFieldValue(field);

        boolean result = dbManage.updateEmployee(selectedSSN, field, newValue);
        return result;
    }

    private String getSelectedID() {
        int selectedRow = employeeTable.getSelectedRow();
        if (selectedRow == -1) {
            return null;
        }

        String columnName;
        switch (currentMode) {
            case WORKS_ON:
                columnName = "Essn"; // Assuming "Essn" is the identifier for WORKS_ON table
                break;
            case DEPENDENT:
                columnName = "Essn"; // Assuming "Essn" is the identifier for DEPENDENT table
                break;
            case PROJECT:
                columnName = "Pnumber"; // Assuming "Pnumber" is the identifier for PROJECT table
                break;
            case EMPLOYEE:
            default:
                columnName = "SSN"; // Default to "SSN" for EMPLOYEE table
                break;
        }

        // Check if the column exists in the table
        int columnIndex = -1;
        for (int i = 0; i < employeeTable.getColumnCount(); i++) {
            if (employeeTable.getColumnName(i).equals(columnName)) {
                columnIndex = i;
                break;
            }
        }

        // If column is not found, return null to prevent IllegalArgumentException
        if (columnIndex == -1) {
            System.err.println("Error: Column " + columnName + " not found in the current table mode.");
            return null;
        }

        return (String) employeeTable.getValueAt(selectedRow, columnIndex);
    }

    // Gets the selected Pno for the WORKS_ON or PROJECT table
    private String getSelectedPno() {
        int selectedRow = employeeTable.getSelectedRow();
        if (selectedRow == -1) {
            return null;
        }

        int pnoColumnIndex = -1;
        if (currentMode == TableMode.WORKS_ON) {
            pnoColumnIndex = employeeTable.getColumnModel().getColumnIndex("Pno");
        } else if (currentMode == TableMode.PROJECT) {
            pnoColumnIndex = employeeTable.getColumnModel().getColumnIndex("Pnumber");
        }

        return (pnoColumnIndex != -1) ? (String) employeeTable.getValueAt(selectedRow, pnoColumnIndex) : null;
    }

    // Returns the value from the UI component based on the selected field
    private String getSelectedFieldValue(String field) {
        return switch (field) {
            case "Sex" -> (String) sexComboBox.getSelectedItem();
            case "Supervisor" -> (String) supervisorComboBox.getSelectedItem();
            case "Department" -> (String) departmentComboBox.getSelectedItem();
            default -> updateValueField.getText().trim();
        };
    }

    // Gets the selected dependent's name for the DEPENDENT table
    private String getSelectedDependentName() {
        int selectedRow = employeeTable.getSelectedRow();
        if (selectedRow == -1) {
            return null;
        }

        int dependentNameColumnIndex = -1;
        for (int i = 0; i < employeeTable.getColumnCount(); i++) {
            if (employeeTable.getColumnName(i).equals("Dependent_name")) {
                dependentNameColumnIndex = i;
                break;
            }
        }

        return dependentNameColumnIndex != -1 ? (String) employeeTable.getValueAt(selectedRow, dependentNameColumnIndex) : null;
    }


    // Sample methods to update data; adjust them to your DBManage methods
    private boolean updateWorksOnData(String essn, String pno, String field, String newValue) {
        return dbManage.updateWorksOn(essn, pno, field, newValue);
    }

    private boolean updateDependentData(String essn, String dependentName, String field, String newValue) {
        return dbManage.updateDependent(essn, dependentName, field, newValue);
    }

    private boolean updateProjectData(String pnumber, String field, String newValue) {
        return dbManage.updateProject(pnumber, field, newValue);
    }

    // 직원 추가 대화 상자
    private void showAddEmployeeDialog() {
        JDialog dialog = new JDialog(this, "직원 추가", true);
        dialog.setSize(400, 500);
        dialog.setLayout(new GridLayout(11, 2, 5, 5));

        // 각 필드에 대한 라벨과 텍스트 필드 추가
        JLabel fnameLabel = new JLabel("First Name:");
        JTextField fnameField = new JTextField();
        JLabel minitLabel = new JLabel("Middle Initial:");
        JTextField minitField = new JTextField();
        JLabel lnameLabel = new JLabel("Last Name:");
        JTextField lnameField = new JTextField();
        JLabel ssnLabel = new JLabel("SSN:");
        JTextField ssnField = new JTextField();
        JLabel bdateLabel = new JLabel("Birth Date (YYYY-MM-DD):");
        JTextField bdateField = new JTextField();
        JLabel addressLabel = new JLabel("Address:");
        JTextField addressField = new JTextField();
        JLabel sexLabel = new JLabel("Sex:");
        JComboBox<String> sexField = new JComboBox<>(new String[]{"M", "F"});
        JLabel salaryLabel = new JLabel("Salary:");
        JTextField salaryField = new JTextField();
        JLabel superSsnLabel = new JLabel("Supervisor SSN:");
        JTextField superSsnField = new JTextField();
        JLabel dnoLabel = new JLabel("Department Number:");
        JTextField dnoField = new JTextField();

        // 대화 상자에 추가
        dialog.add(fnameLabel); dialog.add(fnameField);
        dialog.add(minitLabel); dialog.add(minitField);
        dialog.add(lnameLabel); dialog.add(lnameField);
        dialog.add(ssnLabel); dialog.add(ssnField);
        dialog.add(bdateLabel); dialog.add(bdateField);
        dialog.add(addressLabel); dialog.add(addressField);
        dialog.add(sexLabel); dialog.add(sexField);
        dialog.add(salaryLabel); dialog.add(salaryField);
        dialog.add(superSsnLabel); dialog.add(superSsnField);
        dialog.add(dnoLabel); dialog.add(dnoField);

        // 확인 버튼 추가
        JButton addButton = new JButton("추가");
        addButton.addActionListener(e -> {
            // 입력된 필드 값 가져오기
            String fname = fnameField.getText().trim();
            String minit = minitField.getText().trim();
            String lname = lnameField.getText().trim();
            String ssn = ssnField.getText().trim();
            String bdate = bdateField.getText().trim();
            String address = addressField.getText().trim();
            String sex = (String) sexField.getSelectedItem();
            double salary;
            try {
                salary = Double.parseDouble(salaryField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "연봉은 숫자로 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String superSsn = superSsnField.getText().trim();
            int dno;
            try {
                dno = Integer.parseInt(dnoField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "부서 번호는 숫자로 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // DB에 직원 추가
            boolean success = dbManage.addEmployee(fname, minit, lname, ssn, bdate, address, sex, salary, superSsn, dno);
            if (success) {
                JOptionPane.showMessageDialog(dialog, "직원이 추가되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadEmployeeData();  // 테이블 갱신
            } else {
                JOptionPane.showMessageDialog(dialog, "직원 추가에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(new JLabel()); // 빈 레이블 (레이아웃 맞추기)
        dialog.add(addButton); // 확인 버튼 추가
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }


    // Resets update field box for the employee table
    private void resetUpdateFieldBoxForEmployee() {
        resetUpdateFieldBox(new String[]{"Name", "SSN", "Birth Date", "Address", "Sex", "Salary", "Supervisor", "Department"});
    }

    // Resets update field box with specific fields based on the selected table
    private void resetUpdateFieldBox(String[] fields) {
        updateFieldBox.removeActionListener(updateFieldBox.getActionListeners()[0]);
        updateFieldBox.removeAllItems();

        for (String field : fields) {
            updateFieldBox.addItem(field);
        }

        updateFieldBox.addActionListener(e -> {
            String selectedField = (String) updateFieldBox.getSelectedItem();
            updateUIBasedOnSelectedField(selectedField);
        });
    }

    // Displays the WORKS_ON data in the table
    private void displayWorksOnData() {
        model.setRowCount(0);
        String[] columns = {"선택", "Essn", "Pno", "Hours"};
        model.setColumnIdentifiers(columns);

        List<Object[]> worksOnData = dbManage.getWorksOnData();
        for (Object[] rowData : worksOnData) {
            Object[] tableRow = new Object[columns.length];
            tableRow[0] = false;
            System.arraycopy(rowData, 0, tableRow, 1, rowData.length);
            model.addRow(tableRow);
        }
    }

    // Displays the DEPENDENT data in the table
    private void displayDependentData() {
        model.setRowCount(0);
        String[] columns = {"선택", "Essn", "Fname", "Dependent_name", "Sex", "Bdate", "Relationship"};
        model.setColumnIdentifiers(columns);

        List<Object[]> dependentData = dbManage.getDependentData();
        for (Object[] rowData : dependentData) {
            Object[] tableRow = new Object[columns.length];
            tableRow[0] = false;
            System.arraycopy(rowData, 0, tableRow, 1, rowData.length);
            model.addRow(tableRow);
        }
    }

    // Displays the PROJECT data in the table
    private void displayProjectData() {
        model.setRowCount(0);
        String[] columns = {"선택", "Pname", "Pnumber", "Plocation", "Dname"};
        model.setColumnIdentifiers(columns);

        List<Object[]> projectData = dbManage.getProjectData();
        for (Object[] rowData : projectData) {
            Object[] tableRow = new Object[columns.length];
            tableRow[0] = false;
            System.arraycopy(rowData, 0, tableRow, 1, rowData.length);
            model.addRow(tableRow);
        }
    }

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
        if (!searchType.equals("All")) {
            queryBuilder.append(" WHERE ");
            switch (searchType) {
                case "Department":
                    queryBuilder.append("d.Dname = '").append(deptComboBox.getSelectedItem()).append("'");
                    break;
                case "Gender":
                    queryBuilder.append("e.Sex = '").append(genderComboBox.getSelectedItem()).append("'");
                    break;
                case "Salary":
                    String salaryValue = searchValueField.getText().trim();
                    if (!salaryValue.isEmpty()) {
                        if (salaryValue.matches("\\d+(\\.\\d+)?")) {
                            queryBuilder.append("e.Salary >= ").append(salaryValue);
                        } else {
                            JOptionPane.showMessageDialog(this, "급여는 숫자만 입력 가능합니다!", "입력 오류", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                    break;
                case "Name":
                    queryBuilder.append("CONCAT(e.Fname, ' ', e.Minit, ' ', e.Lname) LIKE '%")
                            .append(searchValueField.getText().trim()).append("%'");
                    break;
                case "SSN":
                    queryBuilder.append("e.Ssn = '").append(searchValueField.getText().trim()).append("'");
                    break;
                case "Birth Date":
                    String birthDate = searchValueField.getText().trim();
                    if (birthDate.length() == 4) {  // 연도만 입력한 경우
                        queryBuilder.append("YEAR(e.Bdate) = '").append(birthDate).append("'");
                    } else {  // 전체 날짜를 입력한 경우
                        queryBuilder.append("e.Bdate = '").append(birthDate).append("'");
                    }
                    break;
                case "Address":
                    queryBuilder.append("e.Address LIKE '%").append(searchValueField.getText().trim()).append("%'");
                    break;
                case "Supervisor":
                    queryBuilder.append("CONCAT(s.Fname, ' ', s.Minit, ' ', s.Lname) LIKE '%")
                            .append(searchValueField.getText().trim()).append("%'");
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
}
