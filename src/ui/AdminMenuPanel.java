package ui;

import model.Member;
import model.Movie;
import model.Reservation;
import model.Showtime;
import model.Theater;
import service.MemberService;
import service.MovieService;
import service.ReservationService;
import service.ShowtimeService;
import service.TheaterService;
import util.DBUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import com.toedter.calendar.JDateChooser; // 添加 JDateChooser 庫

public class AdminMenuPanel extends JPanel {

    private final CinemaBookingGUI mainGUI;
    private final ReservationService reservationService;
    private final MovieService movieService;
    private final ShowtimeService showtimeService;
    private final MemberService memberService; // Needed for some operations potentially
    private final TheaterService theaterService = new TheaterService();

    private JTabbedPane tabbedPane;

    // Tab 1: Movie Management
    private JTable moviesTable;
    private DefaultTableModel moviesTableModel;
    private JTextField movieNameField, movieDurationField, movieDescField, movieRatingField;
    private JButton addMovieButton;
    private JTextField removeMovieIdField;
    private JButton removeMovieButton;

    // Tab 2: Showtime Management
    private JTable showtimesTable;
    private DefaultTableModel showtimesTableModel;
    private JTextField updateShowtimeIdField;
    private JDateChooser updateShowtimeTimeChooser; // 替代 updateShowtimeTimeField
    private JButton updateShowtimeButton;
    private JComboBox<String> movieComboBoxForAddShowtime; // Field for movie selection in add showtime
    private JDateChooser showtimeTimeChooser; // 替代 showtimeTimeField
    private JTextField removeShowtimeIdField;
    private JButton removeShowtimeButton;

    // Tab 3: Reservation Management
    private JTable reservationsTable;
    private DefaultTableModel reservationsTableModel;
    private JTextField updateReservationIdField;
    private JComboBox<String> updateReservationStatusCombo;
    private JButton updateReservationStatusButton;
    private JTextField searchReservationField; // 新增：訂單搜索欄位
    private JComboBox<String> filterStatusCombo; // 新增：訂單狀態過濾器

    // Tab 4: Database Management
    private JButton resetDatabaseButton;

    public AdminMenuPanel(CinemaBookingGUI mainGUI, ReservationService reservationService, MovieService movieService, ShowtimeService showtimeService, MemberService memberService) {
        this.mainGUI = mainGUI;
        this.reservationService = reservationService;
        this.movieService = movieService;
        this.showtimeService = showtimeService;
        this.memberService = memberService;

        setLayout(new BorderLayout());

        // Logout Button (Top Right)
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("管理員模式");
        JButton logoutButton = new JButton("登出");
        logoutButton.addActionListener(e -> mainGUI.showLoginPanel());
        // 新增「查看電影場次」按鈕
        JButton btnViewShowtimes = new JButton("查看電影場次");
        btnViewShowtimes.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            for (Movie m : movieService.getAllMovies()) {
                sb.append(String.format("[%d] %s (%d 分鐘)%n", m.getUid(), m.getName(), m.getDuration()));
                List<Showtime> sts = showtimeService.getShowtimesByMovieId(m.getUid());
                if (sts.isEmpty()) {
                    sb.append("   無排程\n");
                } else {
                    for (Showtime s : sts) {
                        sb.append(String.format("   ID:%d 時間:%s 可用座位:%d%n", s.getUid(), s.getShowTime(), s.getAvailableSeats()));
                    }
                }
            }
            JTextArea ta = new JTextArea(sb.toString());
            ta.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(ta), "電影場次一覽", JOptionPane.INFORMATION_MESSAGE);
        });
        // 在頂部面板中置中顯示此按鈕
        topPanel.add(btnViewShowtimes, BorderLayout.CENTER);
        topPanel.add(welcomeLabel, BorderLayout.WEST);
        topPanel.add(logoutButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Tabbed Pane
        tabbedPane = new JTabbedPane();

        // --- Tab 1: Movie Management ---
        JPanel moviePanel = createMovieManagementPanel();
        tabbedPane.addTab("電影管理", null, moviePanel, "新增、移除電影");

        // --- Tab 2: Showtime Management ---
        JPanel showtimePanel = createShowtimeManagementPanel();
        tabbedPane.addTab("場次管理", null, showtimePanel, "更新場次時間");

        // --- Tab 3: Reservation Management ---
        JPanel reservationPanel = createReservationManagementPanel();
        tabbedPane.addTab("訂票管理", null, reservationPanel, "查看與更新訂票狀態");

        // --- Tab 4: Database Management ---
        JPanel databasePanel = createDatabaseManagementPanel();
        tabbedPane.addTab("資料庫管理", null, databasePanel, "重設資料庫");

        add(tabbedPane, BorderLayout.CENTER);

        // Initial data load for tables
        loadMovies();
        loadAllShowtimes();
        loadAllReservations();
    }

    // =========================================================================
    // Movie Management Panel (Tab 1)
    // =========================================================================
    private JPanel createMovieManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Movie List Table ---
        moviesTableModel = new DefaultTableModel(new String[]{"ID", "名稱", "分級", "片長(分)", "描述"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        moviesTable = new JTable(moviesTableModel);
        JScrollPane moviesScrollPane = new JScrollPane(moviesTable);
        panel.add(moviesScrollPane, BorderLayout.CENTER);

        // --- Action Panel (Add/Remove) ---
        JPanel actionPanel = new JPanel(new GridLayout(2, 1, 10, 10)); // 2 rows for Add and Remove sections

        // Add Movie Section
        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addPanel.setBorder(BorderFactory.createTitledBorder("新增電影"));
        addPanel.add(new JLabel("名稱:"));
        movieNameField = new JTextField(15);
        addPanel.add(movieNameField);
        addPanel.add(new JLabel("片長(分):"));
        movieDurationField = new JTextField(5);
        addPanel.add(movieDurationField);
        addPanel.add(new JLabel("描述:"));
        movieDescField = new JTextField(20);
        addPanel.add(movieDescField);
        addPanel.add(new JLabel("分級:"));
        movieRatingField = new JTextField(5);
        addPanel.add(movieRatingField);
        addMovieButton = new JButton("新增");
        addMovieButton.addActionListener(e -> handleAddMovie());
        addPanel.add(addMovieButton);

        // Remove Movie Section
        JPanel removePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        removePanel.setBorder(BorderFactory.createTitledBorder("移除電影"));
        removePanel.add(new JLabel("輸入要移除的電影 ID:"));
        removeMovieIdField = new JTextField(5);
        removePanel.add(removeMovieIdField);
        removeMovieButton = new JButton("移除");
        removeMovieButton.addActionListener(e -> handleRemoveMovie());
        removePanel.add(removeMovieButton);

        actionPanel.add(addPanel);
        actionPanel.add(removePanel);

        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadMovies() {
        moviesTableModel.setRowCount(0);
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

    private void handleAddMovie() {
        String name = movieNameField.getText().trim();
        String durationStr = movieDurationField.getText().trim();
        String desc = movieDescField.getText().trim();
        String rating = movieRatingField.getText().trim();

        if (name.isEmpty() || durationStr.isEmpty() || desc.isEmpty() || rating.isEmpty()) {
            JOptionPane.showMessageDialog(this, "所有欄位皆為必填", "新增錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int duration = Integer.parseInt(durationStr);
            Movie added = movieService.addMovie(name, duration, desc, rating);
            if (added != null) {
                JOptionPane.showMessageDialog(this, "電影新增成功!", "成功", JOptionPane.INFORMATION_MESSAGE);
                // Clear fields and reload table
                movieNameField.setText("");
                movieDurationField.setText("");
                movieDescField.setText("");
                movieRatingField.setText("");
                loadMovies();
                updateMovieComboBoxInAddShowtime(); // Refresh movie combobox in add showtime
            } else {
                JOptionPane.showMessageDialog(this, "新增電影失敗 (可能是內部錯誤或電影名稱重複)", "新增失敗", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "片長必須是有效的數字", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "新增電影時發生錯誤: " + ex.getMessage(), "系統錯誤", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void handleRemoveMovie() {
        String idStr = removeMovieIdField.getText().trim();
        if (idStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "請輸入要移除的電影 ID", "移除錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int movieId = Integer.parseInt(idStr);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "確定要移除電影 ID: " + movieId + " 嗎？相關的場次和訂票也會被影響！",
                    "確認移除",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                boolean removed = movieService.removeMovie(movieId);
                if (removed) {
                    JOptionPane.showMessageDialog(this, "電影移除成功!", "成功", JOptionPane.INFORMATION_MESSAGE);
                    removeMovieIdField.setText("");
                    loadMovies();
                    updateMovieComboBoxInAddShowtime(); // Refresh movie combobox in add showtime
                    loadAllShowtimes(); // Refresh showtimes as they might be affected
                    loadAllReservations(); // Refresh reservations
                } else {
                    JOptionPane.showMessageDialog(this, "移除電影失敗 (可能是 ID 不存在)", "移除失敗", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "請輸入有效的數字電影 ID", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "移除電影時發生錯誤: " + ex.getMessage(), "系統錯誤", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // =========================================================================
    // Showtime Management Panel (Tab 2)
    // =========================================================================
    private JPanel createShowtimeManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Showtime List Table ---
        showtimesTableModel = new DefaultTableModel(new String[]{"ID", "電影 ID", "電影名稱", "影廳", "時間"}, 0) {
             @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        showtimesTable = new JTable(showtimesTableModel);
        JScrollPane showtimesScrollPane = new JScrollPane(showtimesTable);
        panel.add(showtimesScrollPane, BorderLayout.CENTER);

        // --- Action Panel (Update and Add Showtime) ---
        JPanel actionPanel = new JPanel(new GridLayout(3, 1, 10, 10));

        // Update Showtime Section
        JPanel updatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        updatePanel.setBorder(BorderFactory.createTitledBorder("更新場次時間"));
        updatePanel.add(new JLabel("場次 ID:"));
        updateShowtimeIdField = new JTextField(5);
        updatePanel.add(updateShowtimeIdField);
        
        // 使用日期時間選擇器替代文本輸入框
        updatePanel.add(new JLabel("新時間:"));
        // 使用日期時間選擇器替代文本輸入框
        updatePanel.add(new JLabel("新時間:"));
        updateShowtimeTimeChooser = new JDateChooser();
        updateShowtimeTimeChooser.setDateFormatString("yyyy-MM-dd HH:mm");
        // 設定當前時間作為默認值
        updateShowtimeTimeChooser.setDate(new Date());
        // JDateChooser 不需要額外設定 JSpinnerDateEditor
        updatePanel.add(updateShowtimeTimeChooser);
        
        updateShowtimeButton = new JButton("更新時間");
        updateShowtimeButton.addActionListener(e -> handleUpdateShowtime());
        updatePanel.add(updateShowtimeButton);

        // Add Showtime Section
        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addPanel.setBorder(BorderFactory.createTitledBorder("新增場次"));
        addPanel.add(new JLabel("電影:"));
        movieComboBoxForAddShowtime = new JComboBox<>(); // Initialize the field
        // Populate initial items
        updateMovieComboBoxInAddShowtime(); // Populate using the new method

        addPanel.add(movieComboBoxForAddShowtime);
        addPanel.add(new JLabel("影廳:"));
        JComboBox<String> theaterComboBox = new JComboBox<>();
        for (Theater t : theaterService.listTheaters()) {
            theaterComboBox.addItem(t.getUid() + " - " + t.getType());
        }
        addPanel.add(theaterComboBox);
        
        // 使用日期時間選擇器
        addPanel.add(new JLabel("時間: "));
        showtimeTimeChooser = new JDateChooser();
        showtimeTimeChooser.setDateFormatString("yyyy-MM-dd HH:mm");
        // 設定當前時間作為默認值
        showtimeTimeChooser.setDate(new Date());
        addPanel.add(showtimeTimeChooser);
        
        JButton addShowtimeButton = new JButton("新增場次");
        addShowtimeButton.addActionListener(e -> {
            String movieSel = (String) movieComboBoxForAddShowtime.getSelectedItem(); // Use the field
            if (movieSel == null || movieSel.isEmpty()) {
                JOptionPane.showMessageDialog(this, "請選擇電影", "新增錯誤", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int movieId = Integer.parseInt(movieSel.split(" - ")[0]);
            String theaterSel = (String) theaterComboBox.getSelectedItem();
            if (theaterSel == null || theaterSel.isEmpty()) {
                JOptionPane.showMessageDialog(this, "請選擇影廳", "新增錯誤", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int theaterId = Integer.parseInt(theaterSel.split(" - ")[0]);
            
            // 從日期選擇器獲取日期時間
            Date selectedDate = showtimeTimeChooser.getDate();
            if (selectedDate == null) {
                JOptionPane.showMessageDialog(this, "請選擇時間", "新增錯誤", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // 格式化日期為所需格式
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String time = sdf.format(selectedDate);
            
            try {
                showtimeService.addShowtime(movieId, theaterId, time);
                JOptionPane.showMessageDialog(this, "場次新增成功!", "成功", JOptionPane.INFORMATION_MESSAGE);
                // 重置日期選擇器為當前時間
                showtimeTimeChooser.setDate(new Date());
                loadAllShowtimes();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "新增場次錯誤: " + ex.getMessage(), "系統錯誤", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });
        addPanel.add(addShowtimeButton);

        // 移除場次區域
        JPanel deletePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        deletePanel.setBorder(BorderFactory.createTitledBorder("移除場次"));
        deletePanel.add(new JLabel("場次 ID:"));
        removeShowtimeIdField = new JTextField(5);
        deletePanel.add(removeShowtimeIdField);
        removeShowtimeButton = new JButton("移除場次");
        removeShowtimeButton.addActionListener(e -> handleRemoveShowtime());
        deletePanel.add(removeShowtimeButton);

        actionPanel.add(updatePanel);
        actionPanel.add(addPanel);
        actionPanel.add(deletePanel);
        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void updateMovieComboBoxInAddShowtime() {
        if (movieComboBoxForAddShowtime == null) {
            movieComboBoxForAddShowtime = new JComboBox<>(); // Ensure it's initialized if called early
        }
        movieComboBoxForAddShowtime.removeAllItems();
        List<Movie> movies = movieService.getAllMovies();
        if (movies != null) {
            for (Movie m : movies) {
                movieComboBoxForAddShowtime.addItem(m.getUid() + " - " + m.getName());
            }
        }
    }

    private void loadAllShowtimes() {
        showtimesTableModel.setRowCount(0);
        List<Showtime> showtimes = showtimeService.getAllShowtimes(); // Need a method to get all showtimes
        for (Showtime st : showtimes) {
            Movie movie = st.getMovie(); // Assuming Showtime has getMovie()
            String movieName = (movie != null) ? movie.getName() : "N/A";
            String theaterType = (st.getTheater() != null) ? st.getTheater().getType() : "N/A";
            showtimesTableModel.addRow(new Object[]{
                    st.getUid(),
                    (movie != null) ? movie.getUid() : -1,
                    movieName,
                    theaterType, // Use getType
                    st.getShowTime() // Consider formatting
            });
        }
    }

    private void handleUpdateShowtime() {
        String idStr = updateShowtimeIdField.getText().trim();
        
        // 從日期選擇器獲取日期時間
        Date selectedDate = updateShowtimeTimeChooser.getDate();

        if (idStr.isEmpty() || selectedDate == null) {
            JOptionPane.showMessageDialog(this, "場次 ID 和新時間不能為空", "更新錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 格式化日期為所需格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String newTime = sdf.format(selectedDate);

        try {
            int showtimeId = Integer.parseInt(idStr);
            boolean updated = movieService.updateShowtimeTime(showtimeId, newTime); // Using movieService method as per Main.java
            if (updated) {
                JOptionPane.showMessageDialog(this, "場次時間更新成功!", "成功", JOptionPane.INFORMATION_MESSAGE);
                updateShowtimeIdField.setText("");
                // 重置日期選擇器為當前時間
                updateShowtimeTimeChooser.setDate(new Date());
                loadAllShowtimes(); // Reload table
            } else {
                JOptionPane.showMessageDialog(this, "更新場次時間失敗 (可能是 ID 不存在或格式錯誤)", "更新失敗", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "請輸入有效的數字場次 ID", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "更新場次時發生錯誤: " + ex.getMessage(), "系統錯誤", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * 處理移除場次
     */
    private void handleRemoveShowtime() {
        String idStr = removeShowtimeIdField.getText().trim();
        if (idStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "請輸入要移除的場次 ID", "移除錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int showtimeId = Integer.parseInt(idStr);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "確定要移除場次 ID: " + showtimeId + " 嗎？相關的訂票也會被取消！",
                    "確認移除",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean removed = showtimeService.deleteShowtime(showtimeId);
                if (removed) {
                    JOptionPane.showMessageDialog(this, "場次移除成功!", "成功", JOptionPane.INFORMATION_MESSAGE);
                    removeShowtimeIdField.setText("");
                    loadAllShowtimes();
                    loadAllReservations();
                } else {
                    JOptionPane.showMessageDialog(this, "移除場次失敗 (可能是 ID 不存在)", "移除失敗", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "請輸入有效的數字場次 ID", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "移除場次時發生錯誤: " + ex.getMessage(), "系統錯誤", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // =========================================================================
    // Reservation Management Panel (Tab 3)
    // =========================================================================
    private JPanel createReservationManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- 上方搜索和過濾區域 ---
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBorder(BorderFactory.createTitledBorder("搜索和過濾"));
        
        // 搜索欄位
        searchPanel.add(new JLabel("搜索訂票:"));
        searchReservationField = new JTextField(15);
        searchPanel.add(searchReservationField);
        JButton searchButton = new JButton("搜索");
        searchButton.addActionListener(e -> {
            searchReservations(searchReservationField.getText());
        });
        searchPanel.add(searchButton);
        
        // 狀態過濾
        searchPanel.add(new JLabel("訂單狀態:"));
        filterStatusCombo = new JComboBox<>(new String[]{"全部", "已確認", "已取消"});
        filterStatusCombo.addActionListener(e -> {
            filterReservationsByStatus((String)filterStatusCombo.getSelectedItem());
        });
        searchPanel.add(filterStatusCombo);
        
        // 刷新按鈕
        JButton refreshButton = new JButton("刷新資料");
        refreshButton.addActionListener(e -> loadAllReservations());
        searchPanel.add(refreshButton);

        panel.add(searchPanel, BorderLayout.NORTH);

        // --- Reservation List Table ---
        reservationsTableModel = new DefaultTableModel(
            new String[]{"訂票 ID", "會員 ID", "會員名稱", "電影", "場次 ID", "影廳", "時間", "座位", "狀態"}, 0) {
             @Override 
             public boolean isCellEditable(int row, int column) { return false; }
        };
        reservationsTable = new JTable(reservationsTableModel);
        reservationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // 添加排序功能
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(reservationsTableModel);
        reservationsTable.setRowSorter(sorter);
        
        JScrollPane reservationsScrollPane = new JScrollPane(reservationsTable);
        panel.add(reservationsScrollPane, BorderLayout.CENTER);

        // --- Action Panel (Update Status) ---
        JPanel actionPanel = new JPanel(new BorderLayout(10, 10));
        actionPanel.setBorder(BorderFactory.createTitledBorder("訂單管理操作"));
        
        // 訂單詳細資訊面板
        JPanel detailsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        detailsPanel.add(new JLabel("選中訂單詳細資訊："));
        detailsPanel.add(new JLabel("訂票 ID:"));
        updateReservationIdField = new JTextField(5);
        detailsPanel.add(updateReservationIdField);
        detailsPanel.add(new JLabel("新狀態:"));
        updateReservationStatusCombo = new JComboBox<>(new String[]{"CONFIRMED", "CANCELLED"});
        detailsPanel.add(updateReservationStatusCombo);
        updateReservationStatusButton = new JButton("更新狀態");
        updateReservationStatusButton.addActionListener(e -> handleUpdateReservationStatus());
        detailsPanel.add(updateReservationStatusButton);
        
        actionPanel.add(detailsPanel, BorderLayout.CENTER);

        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    // 搜索訂單
    private void searchReservations(String query) {
        if (query.isEmpty()) {
            loadAllReservations(); // 如果查詢為空，顯示所有訂單
            return;
        }
        
        // 先加載所有訂單
        List<Reservation> allReservations = reservationService.listReservations();
        reservationsTableModel.setRowCount(0);
        
        // 只顯示匹配的訂單
        for (Reservation res : allReservations) {
            // 取得會員名稱
            Member member = memberService.getMemberById(res.getMemberUid());
            String memberName = (member != null) ? member.getUsername() : "未知";
            
            // 取得電影名稱和影廳資訊
            Showtime showtime = res.getShowtime();
            Movie movie = (showtime != null) ? showtime.getMovie() : null;
            String movieName = (movie != null) ? movie.getName() : "未知電影";
            String theaterType = (showtime != null && showtime.getTheater() != null) ? 
                                showtime.getTheater().getType() : "未知影廳";
            String showTimeStr = (showtime != null) ? showtime.getShowTime() : "未知時間";
            
            // 檢查是否包含搜索詞
            String resInfo = res.getUid() + " " + res.getMemberUid() + " " + memberName + " " + 
                            movieName + " " + theaterType + " " + showTimeStr + " " + 
                            res.getSeatNo() + " " + res.getStatus();
            
            if (resInfo.toLowerCase().contains(query.toLowerCase())) {
                reservationsTableModel.addRow(new Object[]{
                    res.getUid(),
                    res.getMemberUid(),
                    memberName,
                    movieName,
                    res.getShowtimeUid(),
                    theaterType,
                    showTimeStr,
                    String.join(", ", res.getSeatNumbers()),
                    res.getStatus()
                });
            }
        }
        
        if (reservationsTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "沒有找到匹配的訂單", "搜索結果", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    // 按狀態過濾訂單
    private void filterReservationsByStatus(String status) {
        // 先加載所有訂單
        List<Reservation> allReservations = reservationService.listReservations();
        reservationsTableModel.setRowCount(0);
        
        for (Reservation res : allReservations) {
            // 狀態過濾
            if (!status.equals("全部")) {
                if (status.equals("已確認") && !res.getStatus().equals("CONFIRMED")) continue;
                if (status.equals("已取消") && !res.getStatus().equals("CANCELLED")) continue;
            }
            
            // 取得會員名稱
            Member member = memberService.getMemberById(res.getMemberUid());
            String memberName = (member != null) ? member.getUsername() : "未知";
            
            // 取得電影名稱和影廳資訊
            Showtime showtime = res.getShowtime();
            Movie movie = (showtime != null) ? showtime.getMovie() : null;
            String movieName = (movie != null) ? movie.getName() : "未知電影";
            String theaterType = (showtime != null && showtime.getTheater() != null) ? 
                                showtime.getTheater().getType() : "未知影廳";
            String showTimeStr = (showtime != null) ? showtime.getShowTime() : "未知時間";
            
            reservationsTableModel.addRow(new Object[]{
                res.getUid(),
                res.getMemberUid(),
                memberName,
                movieName,
                res.getShowtimeUid(),
                theaterType,
                showTimeStr,
                String.join(", ", res.getSeatNumbers()),
                res.getStatus()
            });
        }
    }

    private void loadAllReservations() {
        reservationsTableModel.setRowCount(0);
        List<Reservation> reservations = reservationService.listReservations();
        
        // 如果沒有訂單，顯示提示訊息
        if (reservations.isEmpty()) {
            System.out.println("沒有找到任何訂單記錄");
            JOptionPane.showMessageDialog(this, "資料庫中沒有找到任何訂單記錄", "無訂單資料", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        System.out.println("找到 " + reservations.size() + " 條訂單記錄");
        
        for (Reservation res : reservations) {
            try {
                // 取得會員名稱 (使用 email 作為用戶名)
                Member member = memberService.getMemberById(res.getMemberUid());
                String memberName = (member != null) ? member.getEmail() : "未知會員";
                
                // 直接從 Reservation 獲取電影信息
                Movie movie = null;
                String movieName = "未知電影";
                try {
                    movie = res.getMovie();
                    if (movie != null) {
                        movieName = movie.getName();
                    }
                } catch (Exception e) {
                    System.err.println("無法獲取電影資訊: " + e.getMessage());
                }
                
                // 顯示場次和影廳資訊
                Showtime showtimeObj = res.getShowtime();
                String theaterType = (showtimeObj != null && showtimeObj.getTheater() != null) ? showtimeObj.getTheater().getType() : "未知影廳";
                String showTimeStr = res.getTime();
                
                System.out.println("處理訂單 ID: " + res.getUid() + ", 會員: " + memberName + ", 電影: " + movieName + ", 時間: " + showTimeStr);
                
                // 將資訊添加到表格
                reservationsTableModel.addRow(new Object[]{
                    res.getUid(),
                    res.getMemberUid(),
                    memberName,
                    movieName,
                    res.getShowtimeUid(),
                    theaterType,
                    showTimeStr,
                    res.getSeatNo(),
                    res.getStatus()
                });
            } catch (Exception e) {
                System.err.println("處理訂單時發生錯誤: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // 如果表格為空，顯示提示訊息
        if (reservationsTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "無法載入訂單資料，請檢查系統日誌", "資料載入失敗", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleUpdateReservationStatus() {
        String idStr = updateReservationIdField.getText().trim();
        String newStatus = (String) updateReservationStatusCombo.getSelectedItem();

        if (idStr.isEmpty() || newStatus == null) {
            JOptionPane.showMessageDialog(this, "訂票 ID 和新狀態不能為空", "更新錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int reservationId = Integer.parseInt(idStr);
            boolean updated = reservationService.setReservationStatus(reservationId, newStatus);
            if (updated) {
                JOptionPane.showMessageDialog(this, "訂票狀態更新成功!", "成功", JOptionPane.INFORMATION_MESSAGE);
                updateReservationIdField.setText("");
                loadAllReservations(); // 重新載入表格
            } else {
                JOptionPane.showMessageDialog(this, "更新訂票狀態失敗 (可能是 ID 不存在)", "更新失敗", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "請輸入有效的數字訂票 ID", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "更新訂票狀態時發生錯誤: " + ex.getMessage(), "系統錯誤", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // =========================================================================
    // Database Management Panel (Tab 4)
    // =========================================================================
    private JPanel createDatabaseManagementPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        resetDatabaseButton = new JButton("重設資料庫至預設狀態");
        resetDatabaseButton.setForeground(Color.RED);
        resetDatabaseButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        resetDatabaseButton.addActionListener(e -> handleResetDatabase());

        panel.add(resetDatabaseButton);
        return panel;
    }

    private void handleResetDatabase() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "警告：此操作將清除所有資料並還原至初始狀態！\n確定要重設資料庫嗎？",
                "確認重設資料庫",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                DBUtil.clearDatabase();
                DBUtil.initializeDatabase();
                JOptionPane.showMessageDialog(this, "資料庫已成功重設至預設狀態。", "重設成功", JOptionPane.INFORMATION_MESSAGE);
                // Reload all data in other tabs
                loadMovies();
                loadAllShowtimes();
                loadAllReservations();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "重設資料庫時發生錯誤: " + ex.getMessage(), "系統錯誤", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
}
