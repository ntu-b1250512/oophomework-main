package ui;

import model.Member;
import service.MemberService;

import javax.swing.*;
import java.awt.*;

public class LoginPanel extends JPanel {

    private final CinemaBookingGUI mainGUI;
    private final MemberService memberService;

    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;

    public LoginPanel(CinemaBookingGUI mainGUI, MemberService memberService) {
        this.mainGUI = mainGUI;
        this.memberService = memberService;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("歡迎使用電影訂票系統", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 24));

        JLabel emailLabel = new JLabel("Email:");
        emailField = new JTextField(25);
        JLabel passwordLabel = new JLabel("Password:");
        passwordField = new JPasswordField(25);
        loginButton = new JButton("登入");
        registerButton = new JButton("前往註冊");

        // Title
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.weighty = 0.1;
        add(titleLabel, gbc);

        // Email Label
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weighty = 0; gbc.anchor = GridBagConstraints.EAST;
        add(emailLabel, gbc);

        // Email Field
        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST;
        add(emailField, gbc);

        // Password Label
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        add(passwordLabel, gbc);

        // Password Field
        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST;
        add(passwordField, gbc);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.NONE;
        add(buttonPanel, gbc);

        // --- Event Listeners ---
        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> mainGUI.showRegisterPanel());

        // Allow login with Enter key in password field
        passwordField.addActionListener(e -> handleLogin());
    }

    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Email 和密碼不能為空", "登入錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Member user = memberService.login(email, password);
            if (user != null) {
                JOptionPane.showMessageDialog(this, "登入成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                // Clear fields after successful login
                emailField.setText("");
                passwordField.setText("");
                // Switch panel based on user type
                if ("admin@admin.com".equals(user.getEmail())) {
                    mainGUI.showAdminMenuPanel(user);
                } else {
                    mainGUI.showUserMenuPanel(user);
                }
            } else {
                JOptionPane.showMessageDialog(this, "登入失敗，請檢查 Email 或密碼。", "登入失敗", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "登入時發生錯誤: " + ex.getMessage(), "系統錯誤", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
}
