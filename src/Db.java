
import java.sql.*;
import javax.swing.table.DefaultTableModel;

public class Db {

    static final String URL = "jdbc:derby:SchoolDB;create=true";

    static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    static boolean tableExists(Connection con, String table)
            throws SQLException {

        try (ResultSet rs = con.getMetaData().getTables(
                null, "APP", table.toUpperCase(), new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    static DefaultTableModel query(String sql, Object... params)
            throws SQLException {

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();

                DefaultTableModel model = new DefaultTableModel() {
                    @Override
                    public boolean isCellEditable(int row, int col) {
                        return false;
                    }
                };

                for (int i = 1; i <= cols; i++) {
                    model.addColumn(md.getColumnLabel(i));
                }

                while (rs.next()) {
                    Object[] row = new Object[cols];

                    for (int i = 0; i < cols; i++) {
                        row[i] = rs.getObject(i + 1);
                    }

                    model.addRow(row);
                }

                return model;
            }
        }
    }

    static int update(String sql, Object... params)
            throws SQLException {

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            return ps.executeUpdate();
        }
    }

    static Object value(String sql, Object... params)
            throws SQLException {

        DefaultTableModel model = query(sql, params);

        return model.getRowCount() == 0
                ? null : model.getValueAt(0, 0);
    }

    static Date today() {
        return Date.valueOf(java.time.LocalDate.now());
    }

    static void setup() throws SQLException {

        try (Connection con = connect();
             Statement s = con.createStatement()) {

            System.out.println("Connected to: "
                    + con.getMetaData().getURL());

            if (!tableExists(con, "STRAND")) {
                s.executeUpdate(
                    "CREATE TABLE APP.STRAND (" +
                    "strand_id INT PRIMARY KEY, " +
                    "strand_name VARCHAR(10) NOT NULL)"
                );
            }

            if (!tableExists(con, "STUDENT")) {
                s.executeUpdate(
                    "CREATE TABLE APP.STUDENT (" +
                    "lrn VARCHAR(12) PRIMARY KEY, " +
                    "full_name VARCHAR(80) NOT NULL, " +
                    "grade_level INT NOT NULL CHECK (grade_level IN (11,12)), " +
                    "strand_id INT NOT NULL REFERENCES APP.STRAND(strand_id), " +
                    "section_name VARCHAR(30) NOT NULL, " +
                    "contact VARCHAR(11) NOT NULL)"
                );
            }

            if (!tableExists(con, "APPUSER")) {
                s.executeUpdate(
                    "CREATE TABLE APP.APPUSER (" +
                    "username VARCHAR(20) PRIMARY KEY, " +
                    "password VARCHAR(30) NOT NULL, " +
                    "role VARCHAR(10) NOT NULL, " +
                    "lrn VARCHAR(12) REFERENCES APP.STUDENT(lrn))"
                );
            }

            if (!tableExists(con, "DOCUMENT_REQUEST")) {
                s.executeUpdate(
                    "CREATE TABLE APP.DOCUMENT_REQUEST (" +
                    "request_no INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "lrn VARCHAR(12) NOT NULL REFERENCES APP.STUDENT(lrn), " +
                    "doc_type VARCHAR(40) NOT NULL, " +
                    "purpose VARCHAR(100) NOT NULL, " +
                    "request_date DATE NOT NULL, " +
                    "status VARCHAR(20) NOT NULL DEFAULT 'Pending', " +
                    "archived CHAR(1) NOT NULL DEFAULT 'N')"
                );
            }

            if (!tableExists(con, "PAYMENT")) {
                s.executeUpdate(
                    "CREATE TABLE APP.PAYMENT (" +
                    "payment_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "request_no INT NOT NULL REFERENCES APP.DOCUMENT_REQUEST(request_no), " +
                    "amount DECIMAL(8,2) NOT NULL, " +
                    "payment_date DATE, method VARCHAR(20), " +
                    "status VARCHAR(10) NOT NULL DEFAULT 'Unpaid')"
                );
            }

            if (!tableExists(con, "DOCUMENT_RELEASE")) {
                s.executeUpdate(
                    "CREATE TABLE APP.DOCUMENT_RELEASE (" +
                    "release_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "request_no INT NOT NULL REFERENCES APP.DOCUMENT_REQUEST(request_no), " +
                    "release_date DATE NOT NULL, " +
                    "receiver VARCHAR(60) NOT NULL, " +
                    "released_by VARCHAR(20) NOT NULL)"
                );
            }

            if (!exists(con, "SELECT 1 FROM APP.STRAND")) {
                s.executeUpdate(
                    "INSERT INTO APP.STRAND VALUES (1, 'STEM')");
                s.executeUpdate(
                    "INSERT INTO APP.STRAND VALUES (2, 'ASHH')");
            }

            if (!exists(con, "SELECT 1 FROM APP.STUDENT")) {
                s.executeUpdate(
                    "INSERT INTO APP.STUDENT VALUES " +
                    "('123456789012','Juan Dela Cruz',12,1,'Newton','09171234567')"
                );

                s.executeUpdate(
                    "INSERT INTO APP.STUDENT VALUES " +
                    "('210987654321','Maria Santos',11,2,'Rizal','09981234567')"
                );
            }

            if (!exists(con, "SELECT 1 FROM APP.APPUSER")) {
                s.executeUpdate(
                    "INSERT INTO APP.APPUSER VALUES " +
                    "('registrar','admin123','Registrar',NULL)"
                );

                s.executeUpdate(
                    "INSERT INTO APP.APPUSER VALUES " +
                    "('cashier','cash123','Cashier',NULL)"
                );

                s.executeUpdate(
                    "INSERT INTO APP.APPUSER VALUES " +
                    "('123456789012','1234','Student','123456789012')"
                );

                s.executeUpdate(
                    "INSERT INTO APP.APPUSER VALUES " +
                    "('210987654321','1234','Student','210987654321')"
                );
            }

            System.out.println("Database setup successful!");
        }
    }

    private static boolean exists(Connection con, String sql)
            throws SQLException {

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next();
        }
    }
}
