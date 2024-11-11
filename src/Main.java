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
    private JButton worksOnButton, dependentButton, projectButton, deleteButton;
    private JButton addEmployeeButton;
    private boolean isCustomViewActive = false;
    private String activeViewType = null;


    public Main() {
        dbManage = new DBManage();

        // 기본 설정
        setTitle("Information Retrieval System");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 검색 범위 설정
        JPanel searchRangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchRangePanel.add(new JLabel("검색 범위"));
        searchRangeBox = new JComboBox<>(new String[]{"전체", "부서", "성별", "연봉"});
        searchRangePanel.add(searchRangeBox);

        deptComboBox = new JComboBox<>(new String[]{"Research", "Administration", "Headquarters"});
        deptComboBox.setVisible(false);
        searchRangePanel.add(deptComboBox);

        genderComboBox = new JComboBox<>(new String[]{"F", "M"});
        genderComboBox.setVisible(false);
        searchRangePanel.add(genderComboBox);

        searchValueField = new JTextField(10);
        searchValueField.setVisible(false);
        searchRangePanel.add(searchValueField);

        searchRangeBox.addActionListener(e -> {
            String selectedType = (String) searchRangeBox.getSelectedItem();
            deptComboBox.setVisible(selectedType.equals("부서"));
            genderComboBox.setVisible(selectedType.equals("성별"));
            searchValueField.setVisible(selectedType.equals("연봉"));
        });

        JPanel searchAttributesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchAttributesPanel.add(new JLabel("검색 항목"));

        worksOnButton = new JButton("근무시간 조회");
        dependentButton = new JButton("가족 정보 조회");
        projectButton = new JButton("프로젝트 조회");

        worksOnButton.addActionListener(e -> toggleCustomView("works_on"));
        dependentButton.addActionListener(e -> toggleCustomView("dependent"));
        projectButton.addActionListener(e -> toggleCustomView("project"));

        searchRangePanel.add(worksOnButton);
        searchRangePanel.add(dependentButton);
        searchRangePanel.add(projectButton);

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
        searchButton.addActionListener(e -> {
            if (!isCustomViewActive) {
                enableSearchOptions();
                loadEmployeeData();
            }
        });
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
        selectedCountLabel = new JLabel("선택됨: ");
        countPanel.add(selectedCountLabel);

        JPanel updatePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        updatePanel.add(new JLabel("수정:"));
        updateFieldBox = new JComboBox<>();
        updateValueField = new JTextField(10);
        JButton updateButton = new JButton("UPDATE");
        updatePanel.add(updateFieldBox);
        updatePanel.add(updateValueField);
        updatePanel.add(updateButton);

        updateButton.addActionListener(e -> {
            int selectedRow = employeeTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(null, "업데이트할 행을 선택하세요.");
                return;
            }

            String selectedField = (String) updateFieldBox.getSelectedItem();
            String newValue = updateValueField.getText().trim();

            if (selectedField == null || newValue.isEmpty()) {
                JOptionPane.showMessageDialog(null, "수정할 필드와 값을 선택하세요.");
                return;
            }

            boolean updateSuccess = false;

            // 선택된 뷰에 따라 다른 업데이트 메서드를 호출
            if ("works_on".equals(activeViewType)) {
                String essn = (String) model.getValueAt(selectedRow, model.findColumn("Essn"));
                String pno = (String) model.getValueAt(selectedRow, model.findColumn("Pno"));
                updateSuccess = dbManage.updateWorksOnData(essn, pno, selectedField, newValue);

            } else if ("dependent".equals(activeViewType)) {
                String essn = (String) model.getValueAt(selectedRow, model.findColumn("Essn"));
                updateSuccess = dbManage.updateDependentData(essn, selectedField, newValue);

            } else if ("project".equals(activeViewType)) {
                String pnumber = (String) model.getValueAt(selectedRow, model.findColumn("Pnumber"));
                updateSuccess = dbManage.updateProjectData(pnumber, selectedField, newValue);
            }

            if (updateSuccess) {
                JOptionPane.showMessageDialog(null, "업데이트 성공!");
                toggleCustomView(null); // 기본 EMPLOYEE 뷰로 전환 + 새로고침
            } else {
                JOptionPane.showMessageDialog(null, "업데이트 실패.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel deletePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        deleteButton = new JButton("선택한 데이터 삭제");
        deletePanel.add(deleteButton);

        addEmployeeButton = new JButton("직원 추가");
        addEmployeeButton.addActionListener(e -> showAddEmployeeDialog());
        deletePanel.add(addEmployeeButton);

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
                if (result.equals("삭제할 수 없는 직원입니다")) {
                    JOptionPane.showMessageDialog(null, result, "경고", JOptionPane.WARNING_MESSAGE);
                } else if (result.equals("삭제 완료")) {
                    loadEmployeeData();
                } else {
                    JOptionPane.showMessageDialog(null, result, "오류", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(null, "삭제할 직원이 선택되지 않았습니다.");
            }
        });

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(countPanel, BorderLayout.WEST);
        bottomPanel.add(updatePanel, BorderLayout.CENTER);
        bottomPanel.add(deletePanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        loadEmployeeData();
        addRowSelectionListener();
    }

    private void toggleCustomView(String viewType) {
        if (viewType == null) {
            // 기본 EMPLOYEE 뷰로 전환
            enableSearchOptions();
            loadEmployeeData();
            deleteButton.setEnabled(true);
            addEmployeeButton.setEnabled(true);
            isCustomViewActive = false;
            activeViewType = null;
        } else if (isCustomViewActive && viewType.equals(activeViewType)) {
            // 현재 커스텀 뷰가 활성화되어 있고, 동일한 뷰를 다시 클릭한 경우 -> 기본 EMPLOYEE 뷰로 전환
            enableSearchOptions();
            loadEmployeeData();
            deleteButton.setEnabled(true);
            addEmployeeButton.setEnabled(true);
            isCustomViewActive = false;
            activeViewType = null;
        } else {
            // 현재 커스텀 뷰가 비활성화되어 있거나 다른 뷰를 클릭한 경우 -> 새로운 Custom View 활성화
            disableSearchOptions();
            deleteButton.setEnabled(false);
            addEmployeeButton.setEnabled(false);
            isCustomViewActive = true;
            activeViewType = viewType;

            // 선택된 뷰에 따라 뷰/업데이트 필드를 바꿈
            switch (viewType) {
                case "works_on" -> {
                    displayWorksOnData();
                    updateFieldBox.removeAllItems();
                    updateFieldBox.addItem("Pno");
                    updateFieldBox.addItem("Hours");
                }
                case "dependent" -> {
                    displayDependentData();
                    updateFieldBox.removeAllItems();
                    updateFieldBox.addItem("Fname");
                    updateFieldBox.addItem("Dependent_name");
                    updateFieldBox.addItem("Sex");
                    updateFieldBox.addItem("Bdate");
                    updateFieldBox.addItem("Relationship");
                }
                case "project" -> {
                    displayProjectData();
                    updateFieldBox.removeAllItems();
                    updateFieldBox.addItem("Pname");
                    updateFieldBox.addItem("Pnumber");
                    updateFieldBox.addItem("Plocation");
                    updateFieldBox.addItem("Dname");
                }
            }
        }
    }

    private void disableSearchOptions() {
        searchRangeBox.setEnabled(false);
        deptComboBox.setEnabled(false);
        genderComboBox.setEnabled(false);
        searchValueField.setEnabled(false);
        nameBox.setEnabled(false);
        ssnBox.setEnabled(false);
        bdateBox.setEnabled(false);
        addressBox.setEnabled(false);
        sexBox.setEnabled(false);
        salaryBox.setEnabled(false);
        supervisorBox.setEnabled(false);
        departmentBox.setEnabled(false);
    }

    private void enableSearchOptions() {
        searchRangeBox.setEnabled(true);
        deptComboBox.setEnabled(true);
        genderComboBox.setEnabled(true);
        searchValueField.setEnabled(true);
        nameBox.setEnabled(true);
        ssnBox.setEnabled(true);
        bdateBox.setEnabled(true);
        addressBox.setEnabled(true);
        sexBox.setEnabled(true);
        salaryBox.setEnabled(true);
        supervisorBox.setEnabled(true);
        departmentBox.setEnabled(true);
    }

    private void loadEmployeeData() {
        model.setRowCount(0);

        ArrayList<String> selectedColumns = new ArrayList<>();
        if (nameBox.isSelected()) selectedColumns.add("CONCAT(e.Fname, ' ', e.Minit, ' ', e.Lname) AS NAME");
        if (ssnBox.isSelected()) selectedColumns.add("e.Ssn AS SSN");
        if (bdateBox.isSelected()) selectedColumns.add("e.Bdate AS BDATE");
        if (addressBox.isSelected()) selectedColumns.add("e.Address AS ADDRESS");
        if (sexBox.isSelected()) selectedColumns.add("e.Sex AS SEX");
        if (salaryBox.isSelected()) selectedColumns.add("e.Salary AS SALARY");
        if (supervisorBox.isSelected()) selectedColumns.add("CONCAT(s.Fname, ' ', s.Minit, ' ', s.Lname) AS SUPERVISOR");
        if (departmentBox.isSelected()) selectedColumns.add("d.Dname AS DEPARTMENT");

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

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ").append(String.join(", ", selectedColumns))
                .append(" FROM EMPLOYEE e ")
                .append("JOIN DEPARTMENT d ON e.Dno = d.Dnumber ")
                .append("LEFT JOIN EMPLOYEE s ON e.Super_ssn = s.Ssn");

        String searchType = (String) searchRangeBox.getSelectedItem();
        if (!searchType.equals("전체")) {
            queryBuilder.append(" WHERE ");
            switch (searchType) {
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
        selectedCountLabel.setText("선택됨: " + selectedCount + "행");
    }

    private void displayWorksOnData() {
        model.setRowCount(0);
        String[] columns = {"선택", "Essn", "Pno", "Hours"};
        model.setColumnIdentifiers(columns);

        List<Object[]> worksOnData = dbManage.getWorksOnData();
        if (worksOnData.isEmpty()) {
            System.out.println("No data to display in the UI for Works_On table.");
        } else {
            for (Object[] rowData : worksOnData) {
                Object[] tableRow = new Object[columns.length];
                tableRow[0] = false;
                System.arraycopy(rowData, 0, tableRow, 1, rowData.length);
                model.addRow(tableRow);
            }
        }
    }

    private void displayDependentData() {
        model.setRowCount(0);
        String[] columns = {"선택", "Essn", "Fname", "Dependent_name", "Sex", "Bdate", "Relationship"};
        model.setColumnIdentifiers(columns);

        List<Object[]> dependentData = dbManage.getDependentData();
        if (dependentData.isEmpty()) {
            System.out.println("No data to display in the UI for Works_On table.");
        } else {
            for (Object[] rowData : dependentData) {
                Object[] tableRow = new Object[columns.length];
                tableRow[0] = false;
                System.arraycopy(rowData, 0, tableRow, 1, rowData.length);
                model.addRow(tableRow);
            }
        }
    }

    private void displayProjectData() {
        model.setRowCount(0);
        String[] columns = {"선택", "Pname", "Pnumber", "Plocation", "Dname"};
        model.setColumnIdentifiers(columns);

        List<Object[]> projectData = dbManage.getProjectData();
        if (projectData.isEmpty()) {
            System.out.println("No data to display in the UI for Works_On table.");
        } else {
            for (Object[] rowData : projectData) {
                Object[] tableRow = new Object[columns.length];
                tableRow[0] = false;
                System.arraycopy(rowData, 0, tableRow, 1, rowData.length);
                model.addRow(tableRow);
            }
        }
    }

    private void showAddEmployeeDialog() {
        JDialog dialog = new JDialog(this, "직원 추가", true);
        dialog.setSize(400, 500);
        dialog.setLayout(new GridLayout(11, 2, 5, 5));

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

        addButton.addActionListener(e -> {
            String fname = fnameField.getText();
            String minit = minitField.getText();
            String lname = lnameField.getText();
            String ssn = ssnField.getText();
            String bdate = bdateField.getText();
            String address = addressField.getText();
            String sex = (String) sexCombo.getSelectedItem();
            String salaryText = salaryField.getText();
            String superSsn = superSsnField.getText().isEmpty() ? null : superSsnField.getText();
            String dnoText = dnoField.getText();

            if (fname.isEmpty() || lname.isEmpty() || ssn.isEmpty() || bdate.isEmpty() || salaryText.isEmpty() || dnoText.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "모든 필수 정보를 입력하세요.", "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                double salary = Double.parseDouble(salaryText);
                int dno = Integer.parseInt(dnoText);

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
