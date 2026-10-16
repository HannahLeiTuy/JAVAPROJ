import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** What a student sees: own profile, new request, own request/payment status. */
public class StudentFrame extends JFrame {
    static final String[] DOCS = {"Form 137", "Form 138 / Report Card", "Good Moral Certificate",
                                  "Certificate of Enrollment", "Diploma", "Certification of Grades"};
    static final int[] FEES = {150, 100, 50, 50, 200, 75}; // same order as DOCS

    String lrn;
    JLabel lblInfo = new JLabel();
    JTextField tfContact = new JTextField(12);
    JComboBox<String> cbDoc = new JComboBox<>(DOCS);
    JLabel lblFee = new JLabel("Fee: PHP 150");
    JTextField tfPurpose = new JTextField(25);
    JTable table = Ui.table();

    public StudentFrame(String lrn) {
        super("Student Portal");
        this.lrn = lrn;

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("My Profile", profileTab());
        tabs.add("New Request", requestTab());
        tabs.add("My Requests", Ui.page(Ui.row(Ui.button("Refresh", this::loadRequests)), table, null));
        add(tabs);

        Ui.addMenu(this);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(850, 420);
        setLocationRelativeTo(null);
        loadProfile();
        loadRequests();
    }

    JPanel profileTab() {
        JPanel p = new JPanel(new GridLayout(0, 1));
        p.add(lblInfo);
        p.add(Ui.row(new JLabel("Contact (09XXXXXXXXX):"), tfContact, Ui.button("Update Contact", this::saveContact)));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(p, BorderLayout.NORTH);
        return wrap;
    }

    JPanel requestTab() {
        cbDoc.addActionListener(e -> lblFee.setText("Fee: PHP " + FEES[cbDoc.getSelectedIndex()]));
        JPanel p = new JPanel(new GridLayout(0, 1));
        p.add(Ui.row(new JLabel("Document:"), cbDoc, lblFee));
        p.add(Ui.row(new JLabel("Purpose:"), tfPurpose));
        p.add(Ui.row(Ui.button("Submit Request", this::submit),
                     Ui.button("Clear", () -> tfPurpose.setText(""))));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(p, BorderLayout.NORTH);
        return wrap;
    }

    void loadProfile() {
        try {
            DefaultTableModel m = Db.query("SELECT s.full_name, s.grade_level, st.strand_name, s.section_name, s.contact "
                    + "FROM Student s JOIN Strand st ON s.strand_id = st.strand_id WHERE s.lrn = ?", lrn);
            lblInfo.setText("<html>LRN: " + lrn + "<br>Name: " + m.getValueAt(0, 0)
                    + "<br>Grade " + m.getValueAt(0, 1) + " - " + m.getValueAt(0, 2)
                    + " - Section " + m.getValueAt(0, 3) + "</html>");
            tfContact.setText(m.getValueAt(0, 4).toString());
        } catch (Exception e) {
            Ui.error("Could not load profile: " + e.getMessage());
        }
    }

    void loadRequests() {
        Ui.show(table, "SELECT r.request_no, r.doc_type, r.purpose, r.request_date, r.status, "
                + "p.amount, p.status AS payment FROM Document_Request r "
                + "JOIN Payment p ON p.request_no = r.request_no WHERE r.lrn = ? ORDER BY r.request_no DESC", lrn);
    }

    void saveContact() throws Exception {
        if (!tfContact.getText().matches("09\\d{9}"))
            throw new Exception("Contact must be 11 digits starting with 09.");
        Db.update("UPDATE Student SET contact = ? WHERE lrn = ?", tfContact.getText(), lrn);
        Ui.info("Contact number updated.");
    }

    void submit() throws Exception {
        String purpose = tfPurpose.getText().trim();
        if (purpose.isEmpty()) throw new Exception("Purpose is required.");

        // 1) save the request, 2) create its (unpaid) payment record
        Db.update("INSERT INTO Document_Request (lrn, doc_type, purpose, request_date) VALUES (?, ?, ?, ?)",
                lrn, cbDoc.getSelectedItem(), purpose, Db.today());
        Object no = Db.value("SELECT MAX(request_no) FROM Document_Request WHERE lrn = ?", lrn);
        Db.update("INSERT INTO Payment (request_no, amount) VALUES (?, ?)", no, FEES[cbDoc.getSelectedIndex()]);

        Ui.info("Request submitted!\nYour request number is " + no + ". Please pay the fee at the cashier.");
        tfPurpose.setText("");
        loadRequests();
    }
}
