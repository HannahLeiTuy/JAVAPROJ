import java.sql.Date;
import java.util.*;
import javax.swing.*;

/** Registrar (everything) and Cashier (payments only). */
public class StaffFrame extends JFrame {
    static final String[] STATUS = {"Pending", "Processing", "Ready for Release", "Released"};
    static final String REQ_SQL = "SELECT r.request_no, r.lrn, s.full_name, st.strand_name, s.grade_level, r.doc_type, "
            + "r.purpose, r.request_date, r.status, p.status AS payment, r.archived FROM Document_Request r "
            + "JOIN Student s ON r.lrn = s.lrn JOIN Strand st ON s.strand_id = st.strand_id "
            + "JOIN Payment p ON p.request_no = r.request_no";
    static final String PAY_SQL = "SELECT p.payment_id, p.request_no, s.full_name, r.doc_type, p.amount, "
            + "p.payment_date, p.method, p.status FROM Payment p JOIN Document_Request r ON p.request_no = r.request_no "
            + "JOIN Student s ON r.lrn = s.lrn";

    String user;

    // ---- Students tab ----
    JTable tbStudent = Ui.table();
    JTextField sSearch = new JTextField(12), fLrn = new JTextField(12), fName = new JTextField(20),
               fSection = new JTextField(10), fContact = new JTextField(11);
    JComboBox<String> sGrade = new JComboBox<>(new String[]{"All", "11", "12"});
    JComboBox<String> sStrand = new JComboBox<>(new String[]{"All", "STEM", "ASHH"});
    JComboBox<String> fGrade = new JComboBox<>(new String[]{"11", "12"});
    JComboBox<String> fStrand = new JComboBox<>(new String[]{"STEM", "ASHH"});

    // ---- Requests tab ----
    JTable tbRequest = Ui.table();
    JTextField rSearch = new JTextField(10), rDate = new JTextField(8);
    JComboBox<String> rStatus = new JComboBox<>(new String[]{"All", "Pending", "Processing", "Ready for Release", "Released"});
    JComboBox<String> rDoc = new JComboBox<>(new String[]{"All", "Form 137", "Form 138 / Report Card",
            "Good Moral Certificate", "Certificate of Enrollment", "Diploma", "Certification of Grades"});
    JComboBox<String> rStrand = new JComboBox<>(new String[]{"All", "STEM", "ASHH"});
    JCheckBox rArchived = new JCheckBox("Show archived");

    // ---- Payments tab ----
    JTable tbPayment = Ui.table();
    JTextField pSearch = new JTextField(10), pAmount = new JTextField(8);
    JComboBox<String> pFilter = new JComboBox<>(new String[]{"All", "Unpaid", "Paid"});
    JComboBox<String> pMethod = new JComboBox<>(new String[]{"Cash", "Check", "Money Order"});
    JComboBox<String> pStatus = new JComboBox<>(new String[]{"Unpaid", "Paid"});

    // ---- Reports tab ----
    JTable tbReport = Ui.table();
    JComboBox<String> cbReport = new JComboBox<>(new String[]{"Document requests", "Payment transactions",
            "Released documents", "Pending requests", "Student transaction history"});
    JTextField rLrn = new JTextField(12);

    public StaffFrame(String user, String role) {
        super("Registrar's Office - " + role + ": " + user);
        this.user = user;

        JTabbedPane tabs = new JTabbedPane();
        boolean registrar = role.equals("Registrar");
        if (registrar) { tabs.add("Students", studentTab()); tabs.add("Requests", requestTab()); }
        tabs.add("Payments", paymentTab()); // Registrar and Cashier
        if (registrar) tabs.add("Reports", reportTab());
        add(tabs);

        Ui.addMenu(this);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 560);
        setLocationRelativeTo(null);
    }

    // =====================  STUDENTS  =====================
    JPanel studentTab() {
        tbStudent.getSelectionModel().addListSelectionListener(e -> pickStudent());
        JPanel top = Ui.row(new JLabel("Search LRN/Name:"), sSearch, new JLabel("Grade:"), sGrade,
                new JLabel("Strand:"), sStrand, Ui.button("Search", this::loadStudents));
        JPanel form = new JPanel(new java.awt.GridLayout(0, 1));
        form.add(Ui.row(new JLabel("LRN:"), fLrn, new JLabel("Name:"), fName));
        form.add(Ui.row(new JLabel("Grade:"), fGrade, new JLabel("Strand:"), fStrand,
                new JLabel("Section:"), fSection, new JLabel("Contact:"), fContact));
        form.add(Ui.row(Ui.button("Save", this::addStudent), Ui.button("Update", this::updateStudent),
                Ui.button("Delete", this::deleteStudent), Ui.button("Clear", this::clearStudent)));
        loadStudents();
        return Ui.page(top, tbStudent, form);
    }

    void loadStudents() {
        String sql = "SELECT s.lrn, s.full_name, s.grade_level, st.strand_name, s.section_name, s.contact "
                + "FROM Student s JOIN Strand st ON s.strand_id = st.strand_id "
                + "WHERE (s.lrn LIKE ? OR UPPER(s.full_name) LIKE ?)";
        String key = "%" + sSearch.getText().trim().toUpperCase() + "%";
        List<Object> p = new ArrayList<>(List.of(key, key));
        if (sGrade.getSelectedIndex() > 0) { sql += " AND s.grade_level = ?"; p.add(Integer.parseInt((String) sGrade.getSelectedItem())); }
        if (sStrand.getSelectedIndex() > 0) { sql += " AND st.strand_name = ?"; p.add(sStrand.getSelectedItem()); }
        Ui.show(tbStudent, sql + " ORDER BY s.full_name", p.toArray());
    }

    void pickStudent() { // copy the selected row into the form
        if (tbStudent.getSelectedRow() < 0) return;
        fLrn.setText(Ui.cell(tbStudent, 0));
        fName.setText(Ui.cell(tbStudent, 1));
        fGrade.setSelectedItem(Ui.cell(tbStudent, 2));
        fStrand.setSelectedItem(Ui.cell(tbStudent, 3));
        fSection.setText(Ui.cell(tbStudent, 4));
        fContact.setText(Ui.cell(tbStudent, 5));
    }

    void checkStudent() throws Exception { // required-field validation
        if (!fLrn.getText().matches("\\d{12}")) throw new Exception("LRN must be exactly 12 digits.");
        if (fName.getText().trim().isEmpty()) throw new Exception("Name is required.");
        if (fSection.getText().trim().isEmpty()) throw new Exception("Section is required.");
        if (!fContact.getText().matches("09\\d{9}")) throw new Exception("Contact must be 11 digits starting with 09.");
    }

    void addStudent() throws Exception {
        checkStudent();
        Db.update("INSERT INTO Student VALUES (?, ?, ?, ?, ?, ?)", fLrn.getText(), fName.getText().trim(),
                Integer.parseInt((String) fGrade.getSelectedItem()), fStrand.getSelectedIndex() + 1,
                fSection.getText().trim(), fContact.getText());
        // the student can log in with LRN as username and 1234 as the starting password
        Db.update("INSERT INTO AppUser VALUES (?, '1234', 'Student', ?)", fLrn.getText(), fLrn.getText());
        Ui.info("Student saved. Login: LRN / password 1234");
        clearStudent();
        loadStudents();
    }

    void updateStudent() throws Exception {
        checkStudent();
        int n = Db.update("UPDATE Student SET full_name = ?, grade_level = ?, strand_id = ?, section_name = ?, contact = ? WHERE lrn = ?",
                fName.getText().trim(), Integer.parseInt((String) fGrade.getSelectedItem()), fStrand.getSelectedIndex() + 1,
                fSection.getText().trim(), fContact.getText(), fLrn.getText());
        if (n == 0) throw new Exception("Select an existing student first.");
        Ui.info("Student updated.");
        loadStudents();
    }

    void deleteStudent() throws Exception {
        String lrn = fLrn.getText();
        if (Ui.cell(tbStudent, 0) == null) throw new Exception("Select a student first.");
        int requests = ((Number) Db.value("SELECT COUNT(*) FROM Document_Request WHERE lrn = ?", lrn)).intValue();
        if (requests > 0) throw new Exception("This student has request records, so it cannot be deleted.");
        if (!Ui.confirm("Are you sure you want to delete this record?")) return;
        Db.update("DELETE FROM AppUser WHERE lrn = ?", lrn);
        Db.update("DELETE FROM Student WHERE lrn = ?", lrn);
        Ui.info("Student deleted.");
        clearStudent();
        loadStudents();
    }

    void clearStudent() {
        fLrn.setText(""); fName.setText(""); fSection.setText(""); fContact.setText("");
        tbStudent.clearSelection();
    }

    // =====================  REQUESTS  =====================
    JPanel requestTab() {
        JPanel top = new JPanel(new java.awt.GridLayout(0, 1));
        top.add(Ui.row(new JLabel("Search Req#/LRN/Name:"), rSearch, new JLabel("Date (yyyy-mm-dd):"), rDate,
                new JLabel("Strand:"), rStrand, rArchived));
        top.add(Ui.row(new JLabel("Status:"), rStatus, new JLabel("Document:"), rDoc, Ui.button("Search", this::loadRequests),
                Ui.button("Move to Next Status", this::advance), Ui.button("Delete / Archive", this::removeRequest)));
        loadRequests();
        return Ui.page(top, tbRequest, null);
    }


void loadRequests() {
    String search = rSearch.getText().trim();
    String key = "%" + search.toUpperCase() + "%";

    String sql = REQ_SQL
            + " WHERE (r.lrn LIKE ? OR UPPER(s.full_name) LIKE ?";

    List<Object> params = new ArrayList<>();
    params.add(key);
    params.add(key);

    if (search.matches("\\d+")) {
        try {
            params.add(Integer.valueOf(search));
            sql += " OR r.request_no = ?";
        } catch (NumberFormatException ex) {
            // Continue searching by LRN and name.
        }
    }

    sql += ")";

    if (!rArchived.isSelected()) {
        sql += " AND r.archived = 'N'";
    }

    if (rStatus.getSelectedIndex() > 0) {
        sql += " AND r.status = ?";
        params.add(rStatus.getSelectedItem());
    }

    if (rDoc.getSelectedIndex() > 0) {
        sql += " AND r.doc_type = ?";
        params.add(rDoc.getSelectedItem());
    }

    if (rStrand.getSelectedIndex() > 0) {
        sql += " AND st.strand_name = ?";
        params.add(rStrand.getSelectedItem());
    }

    if (!rDate.getText().trim().isEmpty()) {
        try {
            params.add(java.sql.Date.valueOf(rDate.getText().trim()));
            sql += " AND r.request_date = ?";
        } catch (IllegalArgumentException ex) {
            Ui.error("Date must look like 2026-10-16.");
            return;
        }
    }

    Ui.show(tbRequest, sql + " ORDER BY r.request_no DESC",
            params.toArray());
}


    /** Pending -> Processing -> Ready for Release -> Released */
    void advance() throws Exception {
        String no = Ui.cell(tbRequest, 0);
        if (no == null) throw new Exception("Select a request first.");
        int i = Arrays.asList(STATUS).indexOf(Ui.cell(tbRequest, 8));
        if (i == STATUS.length - 1) throw new Exception("This request is already Released.");
        String next = STATUS[i + 1];

        if (next.equals("Released")) { // releasing needs payment + release details
            if (!"Paid".equals(Ui.cell(tbRequest, 9))) throw new Exception("The payment must be Paid before release.");
            String who = JOptionPane.showInputDialog(this, "Name of the person receiving the document:");
            if (who == null || who.trim().isEmpty()) throw new Exception("Receiver name is required.");
            Db.update("INSERT INTO Document_Release (request_no, release_date, receiver, released_by) VALUES (?, ?, ?, ?)",
                    Integer.parseInt(no), Db.today(), who.trim(), user);
        }
        Db.update("UPDATE Document_Request SET status = ? WHERE request_no = ?", next, Integer.parseInt(no));
        Ui.info("Request #" + no + " is now: " + next);
        loadRequests();
    }

    void removeRequest() throws Exception {
        String no = Ui.cell(tbRequest, 0);
        if (no == null) throw new Exception("Select a request first.");
        if (Ui.cell(tbRequest, 8).equals("Released")) { // completed = archive, never delete
            if (Ui.confirm("Released requests are archived, not deleted. Archive this one?")) {
                Db.update("UPDATE Document_Request SET archived = 'Y' WHERE request_no = ?", Integer.parseInt(no));
                Ui.info("Request archived.");
                loadRequests();
            }
            return;
        }
        if (!Ui.confirm("Are you sure you want to delete this record?")) return;
        Db.update("DELETE FROM Payment WHERE request_no = ?", Integer.parseInt(no)); // child first
        Db.update("DELETE FROM Document_Request WHERE request_no = ?", Integer.parseInt(no));
        Ui.info("Request deleted.");
        loadRequests();
    }

    // =====================  PAYMENTS  =====================
    JPanel paymentTab() {
        tbPayment.getSelectionModel().addListSelectionListener(e -> {
            if (tbPayment.getSelectedRow() < 0) return;
            pAmount.setText(Ui.cell(tbPayment, 4));
            pStatus.setSelectedItem(Ui.cell(tbPayment, 7));
            if (!Ui.cell(tbPayment, 6).isEmpty()) pMethod.setSelectedItem(Ui.cell(tbPayment, 6));
        });
        JPanel top = Ui.row(new JLabel("Search Req#/LRN/Name:"), pSearch, new JLabel("Status:"), pFilter,
                Ui.button("Search", this::loadPayments));
        JPanel form = Ui.row(new JLabel("Amount:"), pAmount, new JLabel("Method:"), pMethod,
                new JLabel("Status:"), pStatus, Ui.button("Update Payment", this::updatePayment),
                Ui.button("Clear", () -> { pAmount.setText(""); tbPayment.clearSelection(); }));
        loadPayments();
        return Ui.page(top, tbPayment, form);
    }

    

void loadPayments() {
    String search = pSearch.getText().trim();
    String key = "%" + search.toUpperCase() + "%";

    String sql = PAY_SQL
            + " WHERE (r.lrn LIKE ? OR UPPER(s.full_name) LIKE ?";

    List<Object> params = new ArrayList<>();
    params.add(key);
    params.add(key);

    if (search.matches("\\d+")) {
        try {
            params.add(Integer.valueOf(search));
            sql += " OR p.request_no = ?";
        } catch (NumberFormatException ex) {
            // Continue searching by LRN and name.
        }
    }

    sql += ")";

    if (pFilter.getSelectedIndex() > 0) {
        sql += " AND p.status = ?";
        params.add(pFilter.getSelectedItem());
    }

    Ui.show(tbPayment, sql + " ORDER BY p.payment_id DESC",
            params.toArray());
}



    void updatePayment() throws Exception {
        String id = Ui.cell(tbPayment, 0);
        if (id == null) throw new Exception("Select a payment first.");
        double amount;
        try { amount = Double.parseDouble(pAmount.getText().trim()); }
        catch (NumberFormatException e) { throw new Exception("Amount must be a number."); }
        if (amount <= 0) throw new Exception("Amount must be more than 0.");

        if (pStatus.getSelectedItem().equals("Paid"))
            Db.update("UPDATE Payment SET amount = ?, method = ?, status = 'Paid', payment_date = ? WHERE payment_id = ?",
                    amount, pMethod.getSelectedItem(), Db.today(), Integer.parseInt(id));
        else
            Db.update("UPDATE Payment SET amount = ?, method = NULL, status = 'Unpaid', payment_date = NULL WHERE payment_id = ?",
                    amount, Integer.parseInt(id));
        Ui.info("Payment updated.");
        loadPayments();
    }

    // =====================  REPORTS  =====================
    JPanel reportTab() {
        JPanel top = Ui.row(new JLabel("Report:"), cbReport, new JLabel("LRN (for history):"), rLrn,
                Ui.button("Show Report", this::showReport));
        showReport();
        return Ui.page(top, tbReport, null); // tip: click a column heading to sort by type, status or date
    }

    void showReport() {
        switch (cbReport.getSelectedIndex()) {
            case 0 -> Ui.show(tbReport, REQ_SQL + " ORDER BY r.doc_type, r.request_date");
            case 1 -> Ui.show(tbReport, PAY_SQL + " ORDER BY p.payment_date");
            case 2 -> Ui.show(tbReport, "SELECT d.release_id, d.request_no, s.full_name, r.doc_type, d.release_date, "
                    + "d.receiver, d.released_by FROM Document_Release d JOIN Document_Request r ON d.request_no = r.request_no "
                    + "JOIN Student s ON r.lrn = s.lrn ORDER BY d.release_date");
            case 3 -> Ui.show(tbReport, REQ_SQL + " WHERE r.status = 'Pending' ORDER BY r.request_date");
            default -> {
                if (!rLrn.getText().matches("\\d{12}")) { Ui.error("Enter a 12-digit LRN for the history."); return; }
                Ui.show(tbReport, REQ_SQL + " WHERE r.lrn = ? ORDER BY r.request_date", rLrn.getText());
            }
        }
    }
}
