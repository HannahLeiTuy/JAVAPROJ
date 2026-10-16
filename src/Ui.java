
import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;

/** Small helpers so the screens stay short. */
public class Ui {

    /** A button action that is allowed to throw errors. */
    interface Action {
        void run() throws Exception;
    }

    static void info(String msg) {
        JOptionPane.showMessageDialog(
            null, msg, "Success",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    static void error(String msg) {
        JOptionPane.showMessageDialog(
            null, msg, "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

    static boolean confirm(String msg) {
        return JOptionPane.showConfirmDialog(
            null, msg, "Confirm",
            JOptionPane.YES_NO_OPTION
        ) == JOptionPane.YES_OPTION;
    }

    /** Creates a button and displays errors when an action fails. */
    static JButton button(String text, Action action) {
        JButton b = new JButton(text);

        b.addActionListener(e -> {
            try {
                action.run();
            } catch (SQLException ex) {
                ex.printStackTrace();

                error("Database error: " + ex.getMessage()
                    + "\nSQLState: " + ex.getSQLState());

            } catch (Exception ex) {
                ex.printStackTrace();
                error(ex.getMessage());
            }
        });

        return b;
    }

    /** Creates a horizontal row of components. */
    static JPanel row(Component... items) {
        JPanel p = new JPanel(
            new FlowLayout(FlowLayout.LEFT)
        );

        for (Component c : items) {
            p.add(c);
        }

        return p;
    }

    /** Creates a page with controls, a table, and optional bottom controls. */
    static JPanel page(
            Component top,
            JTable table,
            Component bottom) {

        JPanel p = new JPanel(new BorderLayout(5, 5));

        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);

        if (bottom != null) {
            p.add(bottom, BorderLayout.SOUTH);
        }

        return p;
    }

    /** Creates a table with sorting and single-row selection. */
    static JTable table() {
        JTable t = new JTable();

        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );
        t.setRowHeight(22);

        return t;
    }

    /** Loads database query results into a JTable. */
    static void show(
            JTable table,
            String sql,
            Object... params) {

        try {
            table.setModel(Db.query(sql, params));
        } catch (SQLException e) {
            e.printStackTrace();

            error("Database error: " + e.getMessage()
                + "\nSQLState: " + e.getSQLState());
        }
    }

    /** Gets a value from the selected table row. */
    static String cell(JTable t, int col) {
        int r = t.getSelectedRow();

        if (r < 0) {
            return null;
        }

        // Convert the sorted view row to the actual model row.
        r = t.convertRowIndexToModel(r);

        Object v = t.getModel().getValueAt(r, col);

        return v == null ? "" : v.toString();
    }

    /** Adds Logout and Exit options to the menu. */
    static void addMenu(JFrame f) {
        JMenuItem logout = new JMenuItem("Logout");
        JMenuItem exit = new JMenuItem("Exit");

        logout.addActionListener(e -> {
            f.dispose();
            new Login().setVisible(true);
        });

        exit.addActionListener(e -> System.exit(0));

        JMenu menu = new JMenu("File");
        menu.add(logout);
        menu.add(exit);

        JMenuBar bar = new JMenuBar();
        bar.add(menu);

        f.setJMenuBar(bar);
    }
}
