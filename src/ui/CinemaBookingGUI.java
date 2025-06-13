package ui;

import service.MemberService;
import service.MovieService;
import service.ReservationService;
import service.ShowtimeService;
import util.DBUtil;
import model.Member;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class CinemaBookingGUI {

    private static final MemberService memberService = new MemberService();
    private static final MovieService movieService = new MovieService();
    private static final ShowtimeService showtimeService = new ShowtimeService();
    private static final ReservationService reservationService = new ReservationService();

    private JFrame mainFrame;
    private CardLayout cardLayout;
    private JPanel mainPanel;

    private LoginPanel loginPanel;
    private RegisterPanel registerPanel;
    private UserMenuPanel userMenuPanel;
    private AdminMenuPanel adminMenuPanel;

    public CinemaBookingGUI() {
        initializeDatabaseIfNeeded();
        createAndShowGUI();
    }

    private void initializeDatabaseIfNeeded() {
        File dbFile = new File("cinema_booking.db");
        if (!dbFile.exists()) {
            System.out.println("Database not found. Initializing database...");
            DBUtil.initializeDatabase();
            System.out.println("Database initialized successfully.");
        } else {
            System.out.println("Database found. Skipping initialization.");
        }
    }

    private void createAndShowGUI() {
        mainFrame = new JFrame("電影訂票系統");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(800, 600); // 設定初始視窗大小

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // 建立各功能面板
        loginPanel = new LoginPanel(this, memberService);
        registerPanel = new RegisterPanel(this, memberService);
        // UserMenuPanel 和 AdminMenuPanel 會在登入成功後建立

        // 將面板加入 CardLayout
        mainPanel.add(loginPanel, "Login");
        mainPanel.add(registerPanel, "Register");

        mainFrame.add(mainPanel);
        cardLayout.show(mainPanel, "Login"); // 初始顯示登入面板

        mainFrame.setLocationRelativeTo(null); // 置中顯示
        mainFrame.setVisible(true);
    }

    // --- 畫面切換方法 ---
    public void showLoginPanel() {
        cardLayout.show(mainPanel, "Login");
        // 清除可能殘留的面板
        if (userMenuPanel != null) mainPanel.remove(userMenuPanel);
        if (adminMenuPanel != null) mainPanel.remove(adminMenuPanel);
        userMenuPanel = null;
        adminMenuPanel = null;
        mainFrame.setTitle("電影訂票系統 - 登入");
    }

    public void showRegisterPanel() {
        cardLayout.show(mainPanel, "Register");
        mainFrame.setTitle("電影訂票系統 - 註冊");
    }

    public void showUserMenuPanel(Member user) {
        userMenuPanel = new UserMenuPanel(this, reservationService, movieService, showtimeService, user);
        mainPanel.add(userMenuPanel, "UserMenu");
        cardLayout.show(mainPanel, "UserMenu");
        mainFrame.setTitle("電影訂票系統 - 使用者選單 (" + user.getEmail() + ")");
    }

    public void showAdminMenuPanel(Member admin) {
        adminMenuPanel = new AdminMenuPanel(this, reservationService, movieService, showtimeService, memberService);
        mainPanel.add(adminMenuPanel, "AdminMenu");
        cardLayout.show(mainPanel, "AdminMenu");
        mainFrame.setTitle("電影訂票系統 - 管理員選單");
    }

    // --- 取得 Service 實例的方法 (供 Panel 使用) ---
    public ReservationService getReservationService() { return reservationService; }
    public MovieService getMovieService() { return movieService; }
    public ShowtimeService getShowtimeService() { return showtimeService; }
    public MemberService getMemberService() { return memberService; }


    public static void main(String[] args) {
        // 使用 SwingUtilities.invokeLater 確保 GUI 在事件分派執行緒上建立
        SwingUtilities.invokeLater(CinemaBookingGUI::new);
    }
}
