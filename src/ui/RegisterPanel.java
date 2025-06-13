package ui;

import service.MemberService;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import com.toedter.calendar.JDateChooser; // 需要添加 jcalendar 庫

public class RegisterPanel extends JPanel {

    private final CinemaBookingGUI mainGUI;
    private final MemberService memberService;

    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JDateChooser birthDateChooser; // 使用 JDateChooser 替代 JTextField
    private JButton registerButton;
    private JButton backToLoginButton;
    private Calendar calendar; // 定義為類成員變數

    public RegisterPanel(CinemaBookingGUI mainGUI, MemberService memberService) {
        this.mainGUI = mainGUI;
        this.memberService = memberService;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("建立新帳戶", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 24));

        JLabel emailLabel = new JLabel("Email:");
        emailField = new JTextField(25);
        JLabel passwordLabel = new JLabel("Password:");
        passwordField = new JPasswordField(25);
        JLabel confirmPasswordLabel = new JLabel("Confirm Password:");
        confirmPasswordField = new JPasswordField(25);
        
        // 創建日期選擇器
        JLabel birthDateLabel = new JLabel("出生日期:");
        birthDateChooser = new JDateChooser();
        birthDateChooser.setDateFormatString("yyyy-MM-dd");
        birthDateChooser.setPreferredSize(new Dimension(200, 20));
        
        // 設置日期選擇器的最大日期為當前日期(不能選擇未來日期)
        birthDateChooser.setMaxSelectableDate(new Date());
        
        // 設置默認日期為18年前
        calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -18);
        birthDateChooser.setDate(calendar.getTime());
        
        registerButton = new JButton("註冊");
        backToLoginButton = new JButton("返回登入");

        // Title
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.weighty = 0.1;
        add(titleLabel, gbc);

        // Email
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weighty = 0; gbc.anchor = GridBagConstraints.EAST;
        add(emailLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST;
        add(emailField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        add(passwordLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST;
        add(passwordField, gbc);

        // Confirm Password
        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST;
        add(confirmPasswordLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.anchor = GridBagConstraints.WEST;
        add(confirmPasswordField, gbc);

        // Birth Date
        gbc.gridx = 0; gbc.gridy = 4; gbc.anchor = GridBagConstraints.EAST;
        add(birthDateLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 4; gbc.anchor = GridBagConstraints.WEST;
        add(birthDateChooser, gbc);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.add(registerButton);
        buttonPanel.add(backToLoginButton);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.NONE;
        add(buttonPanel, gbc);

        // --- Event Listeners ---
        registerButton.addActionListener(e -> handleRegister());
        backToLoginButton.addActionListener(e -> mainGUI.showLoginPanel());
    }

    private void handleRegister() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        
        // 從日期選擇器獲取日期
        Date birthDate = birthDateChooser.getDate();
        
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || birthDate == null) {
            JOptionPane.showMessageDialog(this, "所有欄位皆為必填", "註冊錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "兩次輸入的密碼不一致", "註冊錯誤", JOptionPane.WARNING_MESSAGE);
            passwordField.setText("");
            confirmPasswordField.setText("");
            passwordField.requestFocus(); // 將焦點移回密碼欄位
            return;
        }

        // 格式化日期為 YYYY-MM-DD
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String birthDateStr = sdf.format(birthDate);

        try {
            memberService.register(email, password, birthDateStr);
            JOptionPane.showMessageDialog(this, "註冊成功！請返回登入頁面進行登入。", "成功", JOptionPane.INFORMATION_MESSAGE);
            // 清空欄位並返回登入頁面
            emailField.setText("");
            passwordField.setText("");
            confirmPasswordField.setText("");
            // 重置為預設日期
            calendar = Calendar.getInstance();
            calendar.add(Calendar.YEAR, -18);
            birthDateChooser.setDate(calendar.getTime());
            mainGUI.showLoginPanel();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "註冊失敗: " + ex.getMessage(), "註冊失敗", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "註冊時發生未知錯誤: " + ex.getMessage(), "系統錯誤", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
}
