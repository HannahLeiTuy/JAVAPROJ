
import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;

public class Login extends JFrame {

    JTextField tfUser = new JTextField(15);
    JPasswordField pfPass = new JPasswordField(15);

    public Login() {
        super("Document Request System - Login");

        JLabel title = new JLabel(
            "School Document Request System",
            JLabel.CENTER
        );
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setForeground(new Color(30, 80, 160));

        JPanel form = new JPanel(new GridLayout(2, 2, 5, 5));
        form.add(new JLabel("Username / LRN:"));
        form.add(tfUser);
        form.add(new JLabel("Password:"));
        form.add(pfPass);

        JPanel buttons = Ui.row(
            Ui.button("Login", this::doLogin),
            Ui.button("Clear", () -> {
                tfUser.setText("");
                pfPass.setText("");
            }),
            Ui.button("Exit", () -> System.exit(0))
        );

        setLayout(new BorderLayout(10, 10));
        add(title, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(
            (JButton) buttons.getComponent(0)
        );

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    void doLogin() throws SQLException {

        String user = tfUser.getText().trim();
        String pass = new String(pfPass.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            Ui.error("Please enter your username and password.");
            return;
        }

        var model = Db.query(
            "SELECT role, lrn FROM AppUser " +
            "WHERE username = ? AND password = ?",
            user, pass
        );

        if (model.getRowCount() == 0) {
            Ui.error("Incorrect username or password.");
            return;
        }

        String role = (String) model.getValueAt(0, 0);

        if (role.equals("Student")) {
            new StudentFrame(
                (String) model.getValueAt(0, 1)
            ).setVisible(true);
        } else {
            new StaffFrame(user, role).setVisible(true);
        }

        dispose();
    }

    public static void main(String[] args) {

        try {
            Db.setup();
        } catch (SQLException e) {
    e.printStackTrace();

    StringBuilder details = new StringBuilder();
    details.append("Database setup failed:\n");

    SQLException current = e;

    while (current != null) {
        details.append("\nMessage: ")
               .append(current.getMessage());

        details.append("\nSQLState: ")
               .append(current.getSQLState());

        details.append("\nError Code: ")
               .append(current.getErrorCode());

        current = current.getNextException();
    }

    JOptionPane.showMessageDialog(
        null,
        details.toString(),
        "Database Error",
        JOptionPane.ERROR_MESSAGE
    );

    return;
}

        SwingUtilities.invokeLater(() ->
            new Login().setVisible(true)
        );
    }
}
