package ui;

import model.Member;
import model.Movie;
import model.Reservation;
import model.Showtime;
import service.MovieService;
import service.ReservationService;
import service.ShowtimeService;
import exception.AgeRestrictionException;
import exception.SeatUnavailableException;
import util.DBUtil;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UserMenuPanel extends JPanel {

    private final CinemaBookingGUI mainGUI;
    private final ReservationService reservationService;
    private final MovieService movieService;
    private final ShowtimeService showtimeService;
    private final Member currentUser;

    private JTabbedPane tabbedPane;

    // Tab 1: View Movies & Showtimes / Book Tickets
    private JTable moviesTable;
    private JTable showtimesTable;
    private DefaultTableModel moviesTableModel;
    private DefaultTableModel showtimesTableModel;
    private JTextField seatsField;
    private JButton bookButton;
    private JButton selectSeatsButton; // 新增選擇座位按鈕
    private Movie selectedMovie = null;
    private Showtime selectedShowtime = null;
    private List<String> selectedSeats = new ArrayList<>(); // 儲存使用者選擇的座位

    // Tab 2: My Reservations / Cancel Reservation
    private JTable reservationsTable;
    private DefaultTableModel reservationsTableModel;
    private JTextField reservationIdToCancelField;
    private JButton cancelReservationButton;

    // 定義 reviewTableModel
    private DefaultTableModel reviewTableModel = new DefaultTableModel(new String[]{"使用者", "評論"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    // 使用暫存評論的方式來顯示評論
    private List<String[]> temporaryReviews = new ArrayList<>(); // 暫存評論
    // 更新評論邏輯，確保每部電影有不同的留言串
    private Map<Integer, List<String[]>> movieReviews = new HashMap<>(); // 每部電影的評論暫存

    public UserMenuPanel(CinemaBookingGUI mainGUI, ReservationService reservationService, MovieService movieService, ShowtimeService showtimeService, Member currentUser) {
        this.mainGUI = mainGUI;
        this.reservationService = reservationService;
        this.movieService = movieService;
        this.showtimeService = showtimeService;
        this.currentUser = currentUser;

        setLayout(new BorderLayout());

        // Logout Button (Top Right)
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("歡迎, " + currentUser.getEmail());
        JButton logoutButton = new JButton("登出");
        logoutButton.addActionListener(e -> mainGUI.showLoginPanel());
        topPanel.add(welcomeLabel, BorderLayout.WEST);
        topPanel.add(logoutButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);


        // Tabbed Pane for different functions
        tabbedPane = new JTabbedPane();

        // --- Tab 1: Booking ---
        JPanel bookingPanel = createBookingPanel();
        tabbedPane.addTab("查看電影與訂票", null, bookingPanel, "查看目前上映電影、場次並訂票");

        // --- Tab 2: Reservations Management ---
        JPanel reservationsPanel = createReservationsPanel();
        tabbedPane.addTab("我的訂票紀錄", null, reservationsPanel, "查看與管理您的訂票");

        add(tabbedPane, BorderLayout.CENTER);

        // Initial data load
        loadMovies();
        loadUserReservations();
    }

    // =========================================================================
    // Booking Panel (Tab 1)
    // =========================================================================
    private JPanel createBookingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Movie List ---
        JPanel moviesPanel = new JPanel(new BorderLayout());
        moviesPanel.setBorder(BorderFactory.createTitledBorder("選擇電影"));
        moviesTableModel = new DefaultTableModel(new String[]{"ID", "名稱", "分級", "片長(分)", "描述"}, 0) {
             @Override public boolean isCellEditable(int row, int column) { return false; } // Not editable
        };
        moviesTable = new JTable(moviesTableModel);
        moviesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        moviesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && moviesTable.getSelectedRow() != -1) {
                int selectedRow = moviesTable.getSelectedRow();
                int movieId = (int) moviesTableModel.getValueAt(selectedRow, 0);
                selectedMovie = movieService.getMovieById(movieId).orElse(null); // Get full movie object
                loadShowtimesForMovie(movieId);
                clearBookingSelection(); // Clear previous showtime/seat selection
                loadReviewsForMovie(movieId); // Load reviews for the selected movie
            }
        });
        JScrollPane moviesScrollPane = new JScrollPane(moviesTable);
        moviesPanel.add(moviesScrollPane, BorderLayout.CENTER);


        // --- Showtime List ---
        JPanel showtimesPanel = new JPanel(new BorderLayout());
        showtimesPanel.setBorder(BorderFactory.createTitledBorder("選擇場次"));
        showtimesTableModel = new DefaultTableModel(new String[]{"ID", "影廳", "時段", "剩餘座位"}, 0){
             @Override public boolean isCellEditable(int row, int column) { return false; } // Not editable
        };
        showtimesTable = new JTable(showtimesTableModel);
        showtimesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        showtimesTable.getSelectionModel().addListSelectionListener(e -> {
             if (!e.getValueIsAdjusting() && showtimesTable.getSelectedRow() != -1) {
                int selectedRow = showtimesTable.getSelectedRow();
                int showtimeId = (int) showtimesTableModel.getValueAt(selectedRow, 0);
                selectedShowtime = showtimeService.getShowtimeById(showtimeId); // Directly get Showtime object
                selectSeatsButton.setEnabled(selectedShowtime != null); // 啟用選擇座位按鈕
                bookButton.setEnabled(selectedShowtime != null && !selectedSeats.isEmpty()); // 更新訂票按鈕狀態
             }
        });
        JScrollPane showtimesScrollPane = new JScrollPane(showtimesTable);
        showtimesPanel.add(showtimesScrollPane, BorderLayout.CENTER);


        // --- Booking Action Area ---
        JPanel bookingActionPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        bookingActionPanel.setBorder(BorderFactory.createTitledBorder("訂票操作"));
        
        // 第一行：座位選擇按鈕和已選座位顯示
        JPanel seatSelectionPanel = new JPanel(new BorderLayout(5, 0));
        selectSeatsButton = new JButton("選擇座位");
        selectSeatsButton.setEnabled(false); // 初始時禁用
        selectSeatsButton.addActionListener(e -> showSeatSelectionDialog());
        
        JPanel selectedSeatsDisplayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel selectedSeatsLabel = new JLabel("已選座位: ");
        JLabel selectedSeatsValueLabel = new JLabel("(尚未選擇)");
        selectedSeatsDisplayPanel.add(selectedSeatsLabel);
        selectedSeatsDisplayPanel.add(selectedSeatsValueLabel);
        
        seatSelectionPanel.add(selectSeatsButton, BorderLayout.WEST);
        seatSelectionPanel.add(selectedSeatsDisplayPanel, BorderLayout.CENTER);
        
        // 第二行：訂票按鈕
        JPanel bookButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bookButton = new JButton("確認訂票");
        bookButton.setEnabled(false); // 初始時禁用
        bookButton.addActionListener(e -> handleBooking());
        bookButtonPanel.add(bookButton);

        bookingActionPanel.add(seatSelectionPanel);
        bookingActionPanel.add(bookButtonPanel);


        // --- Layout Setup ---
        JSplitPane movieShowtimeSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, moviesPanel, showtimesPanel);
        movieShowtimeSplit.setResizeWeight(0.5); // Distribute space equally initially

        panel.add(movieShowtimeSplit, BorderLayout.CENTER);
        panel.add(bookingActionPanel, BorderLayout.SOUTH);

        // 添加評論區
        createReviewPanel(panel);

        return panel;
    }
    
    /**
     * 顯示座位選擇對話框
     */
    private void showSeatSelectionDialog() {
        if (selectedShowtime == null) {
            JOptionPane.showMessageDialog(this, "請先選擇一個場次", "選擇座位", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 創建座位選擇面板
        SeatSelectionPanel seatSelectionPanel = new SeatSelectionPanel(selectedShowtime, reservationService);
        
        // 設置已選擇的座位
        if (!selectedSeats.isEmpty()) {
            // 這裡需要在 SeatSelectionPanel 中添加相關方法來預設已選擇的座位
            // 目前先跳過此步驟
        }
        
        // 設置座位選擇回調
        seatSelectionPanel.setSeatSelectionCallback(newSelectedSeats -> {
            selectedSeats = newSelectedSeats;
            updateSelectedSeatsDisplay(); // 更新顯示已選座位的標籤
            bookButton.setEnabled(!selectedSeats.isEmpty()); // 更新訂票按鈕狀態
        });
        
        // 創建並顯示對話框
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "選擇座位", true);
        dialog.setContentPane(seatSelectionPanel);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        // 添加確認按鈕
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton confirmButton = new JButton("確認座位選擇");
        confirmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedSeats = seatSelectionPanel.getSelectedSeats();
                updateSelectedSeatsDisplay();
                bookButton.setEnabled(!selectedSeats.isEmpty());
                dialog.dispose();
            }
        });
        buttonPanel.add(confirmButton);
        
        // 將按鈕面板添加到對話框底部
        dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        
        // 顯示對話框
        dialog.setVisible(true);
    }
    
    /**
     * 更新已選座位的顯示
     */
    private void updateSelectedSeatsDisplay() {
        // 找到顯示已選座位的標籤（假設是第一個選項卡中第一個面板的第二個組件中的第二個組件）
        JPanel bookingPanel = (JPanel) tabbedPane.getComponentAt(0);
        JPanel bookingActionPanel = (JPanel) bookingPanel.getComponent(1);
        JPanel seatSelectionPanel = (JPanel) bookingActionPanel.getComponent(0);
        JPanel selectedSeatsDisplayPanel = (JPanel) seatSelectionPanel.getComponent(1);
        JLabel selectedSeatsValueLabel = (JLabel) selectedSeatsDisplayPanel.getComponent(1);
        
        if (selectedSeats.isEmpty()) {
            selectedSeatsValueLabel.setText("(尚未選擇)");
        } else {
            String seatsText = String.join(", ", selectedSeats);
            selectedSeatsValueLabel.setText(seatsText);
        }
    }

    private void loadMovies() {
        moviesTableModel.setRowCount(0); // Clear existing data
        List<Movie> movies = movieService.getAllMovies();
        for (Movie movie : movies) {
            moviesTableModel.addRow(new Object[]{
                    movie.getUid(),
                    movie.getName(),
                    movie.getRating(),
                    movie.getDuration(),
                    movie.getDescription()
            });
        }
    }

    private void loadShowtimesForMovie(int movieId) {
        showtimesTableModel.setRowCount(0); // Clear existing data
        if (movieId <= 0) return;

        List<Showtime> showtimes = showtimeService.getShowtimesByMovieId(movieId);
        for (Showtime st : showtimes) {
             // Calculate remaining seats (This might need optimization or a dedicated service method)
             List<String> bookedSeats = reservationService.getBookedSeatsForShowtime(st.getUid());
             int totalSeats = st.getTheater().getTotalSeats(); // Assuming Theater has capacity -> Changed to getTotalSeats
             int availableSeats = totalSeats - bookedSeats.size();

            showtimesTableModel.addRow(new Object[]{
                    st.getUid(),
                    st.getTheater().getType(), // Assuming Theater has name -> Changed to getType
                    st.getShowTime(), // Consider formatting the date/time
                    availableSeats + " / " + totalSeats
            });
        }
    }

    private void clearBookingSelection() {
        selectedShowtime = null;
        selectedSeats.clear(); // 清除已選座位
        showtimesTable.clearSelection();
        selectSeatsButton.setEnabled(false); // 禁用選擇座位按鈕
        bookButton.setEnabled(false); // 禁用訂票按鈕
        updateSelectedSeatsDisplay(); // 更新顯示
    }

    private void handleBooking() {
        if (selectedShowtime == null) {
            JOptionPane.showMessageDialog(this, "請先選擇一個場次", "訂票錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedMovie == null) {
             JOptionPane.showMessageDialog(this, "請先選擇一部電影", "訂票錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedSeats.isEmpty()) {
            JOptionPane.showMessageDialog(this, "請選擇至少一個座位", "訂票錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Call the booking service
            String result = reservationService.bookTickets(currentUser.getUid(), selectedShowtime.getUid(), selectedSeats);

            // Display result
            JOptionPane.showMessageDialog(this, result, "訂票結果", JOptionPane.INFORMATION_MESSAGE);

            // If successful, clear fields and refresh relevant data
            if (result.startsWith("訂票成功")) {
                selectedSeats.clear(); // 清除已選座位
                updateSelectedSeatsDisplay(); // 更新顯示
                loadShowtimesForMovie(selectedMovie.getUid()); // Refresh showtimes to show updated seat count
                loadUserReservations(); // Refresh user's reservations list on the other tab
                tabbedPane.setSelectedIndex(1); // Switch to reservations tab
                bookButton.setEnabled(false); // 禁用訂票按鈕
            }

        } catch (IllegalArgumentException ex) {
             JOptionPane.showMessageDialog(this, "訂票失敗: " + ex.getMessage(), "訂票失敗", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) { // Keep a general catch for other unexpected errors
            JOptionPane.showMessageDialog(this, "訂票時發生未知錯誤: " + ex.getMessage(), "系統錯誤", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }


    // =========================================================================
    // Reservations Management Panel (Tab 2)
    // =========================================================================
    private JPanel createReservationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Reservations List ---
        JPanel reservationsListPanel = new JPanel(new BorderLayout());
        reservationsListPanel.setBorder(BorderFactory.createTitledBorder("我的訂票紀錄"));
        reservationsTableModel = new DefaultTableModel(new String[]{"訂票 ID", "電影", "影廳", "時間", "座位", "狀態"}, 0){
             @Override public boolean isCellEditable(int row, int column) { return false; } // Not editable
        };
        reservationsTable = new JTable(reservationsTableModel);
        reservationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane reservationsScrollPane = new JScrollPane(reservationsTable);
        reservationsListPanel.add(reservationsScrollPane, BorderLayout.CENTER);

        // --- Cancel Action Area ---
        JPanel cancelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cancelPanel.setBorder(BorderFactory.createTitledBorder("取消訂票"));
        JLabel cancelLabel = new JLabel("輸入要取消的訂票 ID:");
        reservationIdToCancelField = new JTextField(10);
        cancelReservationButton = new JButton("確認取消");

        cancelPanel.add(cancelLabel);
        cancelPanel.add(reservationIdToCancelField);
        cancelPanel.add(cancelReservationButton);

        cancelReservationButton.addActionListener(e -> handleCancelReservation());

        // 在取消訂票介面上方新增退票說明
        JLabel cancelInfoLabel = new JLabel("提示：電影播映前60分鐘內退票需支付手續費，且電影播映前30分鐘內無法退票。");
        panel.add(cancelInfoLabel, BorderLayout.NORTH);

        // --- Layout ---
        panel.add(reservationsListPanel, BorderLayout.CENTER);
        panel.add(cancelPanel, BorderLayout.SOUTH);

        return panel;
    }

    // 更新評論邏輯，使用資料庫存儲和讀取評論
    private void createReviewPanel(JPanel panel) {
        JPanel reviewPanel = new JPanel(new BorderLayout(10, 10));
        reviewPanel.setBorder(BorderFactory.createTitledBorder("電影評論"));

        // 顯示評論區
        reviewTableModel = new DefaultTableModel(new String[]{"使用者", "評論"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable reviewTable = new JTable(reviewTableModel);
        JScrollPane reviewScrollPane = new JScrollPane(reviewTable);
        reviewPanel.add(reviewScrollPane, BorderLayout.CENTER);

        // 新增評論區
        JPanel addReviewPanel = new JPanel(new BorderLayout(5, 5));
        JTextField reviewField = new JTextField();
        JButton submitReviewButton = new JButton("提交評論");
        submitReviewButton.addActionListener(e -> {
            if (selectedMovie == null) {
                JOptionPane.showMessageDialog(this, "請先選擇一部電影", "評論錯誤", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String reviewText = reviewField.getText().trim();
            if (reviewText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "評論內容不可為空", "評論錯誤", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                reservationService.addReview(selectedMovie.getUid(), currentUser.getEmail(), reviewText); // 存入資料庫
                reviewField.setText("");
                JOptionPane.showMessageDialog(this, "評論已成功提交", "提交成功", JOptionPane.INFORMATION_MESSAGE);
                loadReviewsForMovie(selectedMovie.getUid());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "提交評論時發生錯誤: " + ex.getMessage(), "提交失敗", JOptionPane.ERROR_MESSAGE);
            }
        });
        addReviewPanel.add(reviewField, BorderLayout.CENTER);
        addReviewPanel.add(submitReviewButton, BorderLayout.EAST);
        reviewPanel.add(addReviewPanel, BorderLayout.SOUTH);

        // 在電影選擇面板下方新增評論面板
        panel.add(reviewPanel, BorderLayout.EAST);
    }

    private void loadReviewsForMovie(int movieId) {
        reviewTableModel.setRowCount(0); // 清空現有評論
        try {
            // 首先檢查資料庫連線
            try (Connection testConn = DBUtil.getConnection()) {
                // 檢查 reviews 表是否存在
                DatabaseMetaData metaData = testConn.getMetaData();
                try (ResultSet rs = metaData.getTables(null, null, "reviews", null)) {
                    if (!rs.next()) {
                        // 如果表不存在，先創建它
                        DBUtil.ensureReviewsTableExists();
                    }
                }
            }
            
            List<String[]> reviews = reservationService.getReviewsByMovieId(movieId); // 從資料庫讀取評論
            for (String[] review : reviews) {
                reviewTableModel.addRow(review);
            }
        } catch (SQLException e) {
            System.err.println("資料庫連線錯誤: " + e.getMessage());
            // 如果資料庫有問題，顯示預設訊息
            reviewTableModel.addRow(new String[]{"系統", "目前無法載入評論，請稍後再試"});
        } catch (Exception ex) {
            System.err.println("載入評論時發生錯誤: " + ex.getMessage());
            ex.printStackTrace();
            // 顯示錯誤訊息而不是彈出對話框
            reviewTableModel.addRow(new String[]{"系統", "載入評論時發生錯誤: " + ex.getMessage()});
        }
    }

    private void loadUserReservations() {
        reservationsTableModel.setRowCount(0); // Clear existing data
        List<Reservation> reservations = reservationService.listReservationsByMember(currentUser.getUid());

        for (Reservation res : reservations) {
            Showtime st = res.getShowtime(); // Assuming Reservation has getShowtime()
            Movie mv = (st != null) ? st.getMovie() : null; // Assuming Showtime has getMovie()
            String movieName = (mv != null) ? mv.getName() : "N/A";
            String theaterType = (st != null && st.getTheater() != null) ? st.getTheater().getType() : "N/A"; // Use getType
            String showTimeStr = (st != null) ? st.getShowTime().toString() : "N/A"; // Consider formatting

            reservationsTableModel.addRow(new Object[]{
                    res.getUid(),
                    movieName,
                    theaterType, // Use getType
                    showTimeStr,
                    String.join(", ", res.getSeatNumbers()),
                    res.getStatus()
            });
        }
    }

    private void handleCancelReservation() {
        String idText = reservationIdToCancelField.getText().trim();
        if (idText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "請輸入要取消的訂票 ID。", "錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int reservationId = Integer.parseInt(idText);

            // 先檢查退票時間
            Reservation targetRes = reservationService.listReservationsByMember(currentUser.getUid())
                    .stream().filter(r -> r.getUid() == reservationId).findFirst().orElse(null);
            if (targetRes == null) {
                JOptionPane.showMessageDialog(this, "找不到指定的訂票 ID。", "錯誤", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Showtime targetSt = showtimeService.getShowtimeById(targetRes.getShowtimeUid());
            try {
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                Date showDate = fmt.parse(targetSt.getStartTime());
                long diffMin = (showDate.getTime() - new Date().getTime()) / (60 * 1000);
                if (diffMin <= 30) {
                    JOptionPane.showMessageDialog(this,
                        "無法在電影開始前30分鐘內取消訂票（因臨近放映時間，影廳無法再進行座位調度）。",
                        "取消失敗",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // 收取手續費區間: 60 到 31 分鐘
                if (diffMin <= 60) {
                    int feeConfirm = JOptionPane.showConfirmDialog(this,
        "退票需支付50元手續費（因退票時間接近放映，需留給其他用戶購票與系統處理時間，並負擔平台手續與票務處理成本），是否要繼續？",
        "退票手續費",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE);
                    if (feeConfirm != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
            } catch (ParseException pe) {
                // 日期解析失敗，忽略並繼續
            }
            // 原有首次確認對話框
            int confirm = JOptionPane.showConfirmDialog(this,
                    "確定要取消訂票 ID: " + reservationId + " 嗎？此操作無法復原。",
                    "確認取消",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                // 取得退票操作前的系統錯誤輸出
                ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
                PrintStream oldErr = System.err;
                System.setErr(new PrintStream(errorStream));
                
                boolean success = reservationService.cancelReservation(reservationId, currentUser.getUid());
                
                // 恢復標準錯誤輸出並獲取錯誤消息
                System.setErr(oldErr);
                String errorMsg = errorStream.toString();

                if (success) {
                    JOptionPane.showMessageDialog(this, "訂票 ID: " + reservationId + " 已成功取消。", "取消成功", JOptionPane.INFORMATION_MESSAGE);
                    reservationIdToCancelField.setText(""); // Clear input field
                    loadUserReservations(); // Refresh the reservations list
                    loadShowtimesForMovie(selectedMovie != null ? selectedMovie.getUid() : -1); // Refresh showtimes on the other tab if a movie is selected
                } else {
                    String failureReason = "取消失敗。";
                    
                    // 解析錯誤訊息
                    if (errorMsg.contains("Cannot cancel ticket within 30 minutes before showtime")) {
                        failureReason += "\n無法在電影開始前30分鐘內取消訂票。";
                    } else if (errorMsg.contains("Cannot cancel ticket after showtime has started")) {
                        failureReason += "\n無法在電影已開始放映後取消訂票。";
                    } else if (errorMsg.contains("does not belong to member")) {
                        failureReason += "\n此訂票不屬於您的帳號。";
                    } else if (errorMsg.contains("Reservation with ID") && errorMsg.contains("not found")) {
                        failureReason += "\n找不到指定的訂票 ID。";
                    } else if (errorMsg.contains("already cancelled")) {
                        failureReason += "\n此訂票已經被取消。";
                    } else {
                        // 一般性失敗原因
                        failureReason += "\n可能原因：\n- 訂票 ID 不存在\n- 此訂票不屬於您\n- 訂票狀態無法取消 (例如已過期或已取消)";
                    }
                    
                    JOptionPane.showMessageDialog(this, failureReason, "取消失敗", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "請輸入有效的數字訂票 ID。", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "取消時發生錯誤: " + ex.getMessage(), "系統錯誤", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
}
