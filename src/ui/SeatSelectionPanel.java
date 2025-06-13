package ui;

import model.Showtime;
import model.Theater;
import service.ReservationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * 提供圖形化座位選擇界面
 */
public class SeatSelectionPanel extends JPanel {
    private static final int SEAT_SIZE = 30; // 座位按鈕大小
    private static final int SEAT_GAP = 5;   // 座位之間的間隙
    
    private final Showtime selectedShowtime;
    private final ReservationService reservationService;
    private final JPanel seatsPanel;
    private final JPanel selectedSeatsPanel;
    private final JLabel screenLabel;
    private final JLabel theaterInfoLabel;
    
    private final Map<String, JToggleButton> seatButtons = new HashMap<>();
    private final Set<String> selectedSeats = new HashSet<>();
    private final Set<String> reservedSeats = new HashSet<>();
    
    private final Color standardSeatColor = Color.LIGHT_GRAY;
    private final Color blueSeatColor = new Color(173, 216, 230); // 淺藍色
    private final Color yellowSeatColor = new Color(255, 255, 153); // 淺黃色
    private final Color redSeatColor = new Color(255, 153, 153); // 淺紅色
    // 將選中座位的顏色改為鮮艷的綠色，增加識別度
    private final Color selectedSeatColor = new Color(50, 205, 50); // 亮綠色
    private final Color reservedSeatColor = Color.GRAY; // 灰色
    private final Color invalidSeatColor = new Color(240, 240, 240); // 淺灰色背景，用於無效座位
    
    // 定義影廳類型常量
    private static final String BIG_THEATER_TYPE = "A";
    private static final String SMALL_THEATER_TYPE = "B";
    
    /**
     * 選擇座位後的回調接口
     */
    public interface SeatSelectionCallback {
        void onSeatsSelected(List<String> selectedSeats);
    }
    
    private SeatSelectionCallback callback;
    
    /**
     * 創建座位選擇面板
     * @param showtime 場次信息
     * @param reservationService 訂票服務
     */
    public SeatSelectionPanel(Showtime showtime, ReservationService reservationService) {
        this.selectedShowtime = showtime;
        this.reservationService = reservationService;
        
        setLayout(new BorderLayout(10, 10));
        
        // 螢幕顯示區域
        screenLabel = new JLabel("▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃ 銀幕 ▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃", SwingConstants.CENTER);
        screenLabel.setBackground(Color.DARK_GRAY);
        screenLabel.setForeground(Color.WHITE);
        screenLabel.setOpaque(true);
        screenLabel.setPreferredSize(new Dimension(getWidth(), 30));
        add(screenLabel, BorderLayout.NORTH);
        
        // 新增：劇院資訊標籤
        String theaterType = showtime != null ? showtime.getTheaterName() : "未知影廳";
        theaterInfoLabel = new JLabel("影廳: " + theaterType, SwingConstants.CENTER);
        theaterInfoLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        theaterInfoLabel.setForeground(Color.BLUE);
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        infoPanel.add(theaterInfoLabel);
        infoPanel.setBorder(new EmptyBorder(5, 0, 5, 0));
        add(infoPanel, BorderLayout.NORTH);
        
        // 座位區域面板
        seatsPanel = new JPanel();
        seatsPanel.setBackground(invalidSeatColor); // 淺灰色背景
        JScrollPane scrollPane = new JScrollPane(seatsPanel);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // 改善滾動速度
        add(scrollPane, BorderLayout.CENTER);
        
        // 已選擇的座位資訊區域
        selectedSeatsPanel = new JPanel();
        selectedSeatsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        selectedSeatsPanel.setBorder(BorderFactory.createTitledBorder("已選座位"));
        JLabel noSeatsLabel = new JLabel("尚未選擇座位");
        selectedSeatsPanel.add(noSeatsLabel);
        add(selectedSeatsPanel, BorderLayout.SOUTH);
        
        // 載入已預訂座位
        loadReservedSeats();
        
        // 確認座位計畫已載入，並且檢查影廳類型
        if (showtime != null) {
            // 直接從 Theater 物件獲取類型，避免依賴 getTheaterName
            Theater theater = showtime.getTheater();
            String type = theater != null ? theater.getType() : null;
            System.out.println("===== 除錯信息 =====");
            System.out.println("影廳類型: " + type);
            System.out.println("影廳名稱: " + theaterType);
            System.out.println("影廳ID: " + (theater != null ? theater.getUid() : "N/A"));
            System.out.println("====================");
            
            // 根據資料庫中的實際影廳類型值進行判斷
            if (type != null && (type.equalsIgnoreCase(BIG_THEATER_TYPE) || type.contains("大") || type.contains("A"))) {
                System.out.println("創建大廳座位配置");
                createBigRoomSeats();
                updateTheaterInfoLabel("大廳 (VIP影廳 - " + type + ")");
            } else if (type != null && (type.equalsIgnoreCase(SMALL_THEATER_TYPE) || type.contains("小") || type.contains("B"))) {
                System.out.println("創建小廳座位配置");
                createSmallRoomSeats();
                updateTheaterInfoLabel("小廳 (標準影廳 - " + type + ")");
            } else {
                System.out.println("無法識別的影廳類型: '" + type + "'，使用預設小廳配置");
                createSmallRoomSeats();
                updateTheaterInfoLabel("標準影廳 (未識別類型 - " + (type != null ? type : "未知") + ")");
            }
        }
        
        // 增加座位圖例
        JPanel legendPanel = createLegendPanel();
        add(legendPanel, BorderLayout.EAST);
    }
    
    /**
     * 創建座位圖例面板
     */
    private JPanel createLegendPanel() {
        JPanel legendPanel = new JPanel();
        legendPanel.setLayout(new BoxLayout(legendPanel, BoxLayout.Y_AXIS));
        legendPanel.setBorder(BorderFactory.createTitledBorder("圖例"));
        
        // 圖例條目
        addLegendItem(legendPanel, standardSeatColor, "標準座位");
        addLegendItem(legendPanel, blueSeatColor, "藍區座位");
        addLegendItem(legendPanel, yellowSeatColor, "黃區座位");
        addLegendItem(legendPanel, redSeatColor, "紅區座位");
        addLegendItem(legendPanel, selectedSeatColor, "已選擇");
        addLegendItem(legendPanel, reservedSeatColor, "已訂位");
        
        return legendPanel;
    }
    
    /**
     * 新增一個圖例項目
     */
    private void addLegendItem(JPanel legendPanel, Color color, String text) {
        JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel colorBox = new JPanel();
        colorBox.setBackground(color);
        colorBox.setPreferredSize(new Dimension(20, 20));
        
        itemPanel.add(colorBox);
        itemPanel.add(new JLabel(text));
        
        legendPanel.add(itemPanel);
    }
    
    /**
     * 從資料庫載入已預訂座位
     */
    private void loadReservedSeats() {
        if (selectedShowtime != null) {
            List<String> bookedSeats = reservationService.getBookedSeatsForShowtime(selectedShowtime.getUid());
            reservedSeats.addAll(bookedSeats);
        }
    }
    
    /**
     * 設置座位選擇後的回調
     */
    public void setSeatSelectionCallback(SeatSelectionCallback callback) {
        this.callback = callback;
    }
    
    /**
     * 獲取當前選擇的座位列表
     */
    public List<String> getSelectedSeats() {
        return new ArrayList<>(selectedSeats);
    }
    
    /**
     * 創建大廳座位配置
     * 根據 big_room.json 資料建立座位
     */
    private void createBigRoomSeats() {
        seatsPanel.removeAll();
        
        // 使用 GridBagLayout 以實現更靈活的布局
        seatsPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        int maxRows = 13; // A-M
        int maxCols = 38; // 1-38
        
        // 添加列標題（數字）
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        for (int col = 1; col <= maxCols; col++) {
            gbc.gridx = col;
            if (col % 5 == 0) { // 每5列顯示一次列號
                JLabel colLabel = new JLabel(String.valueOf(col));
                colLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
                colLabel.setForeground(Color.DARK_GRAY);
                seatsPanel.add(colLabel, gbc);
            }
        }
        
        // 建立所有座位
        for (int row = 0; row < maxRows; row++) {
            char rowChar = (char) ('A' + row);
            
            // 添加行標題（字母）
            gbc.gridx = 0;
            gbc.gridy = row + 1;
            JLabel rowLabel = new JLabel(String.valueOf(rowChar));
            rowLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            rowLabel.setForeground(Color.DARK_GRAY);
            seatsPanel.add(rowLabel, gbc);
            
            for (int col = 1; col <= maxCols; col++) {
                gbc.gridx = col;
                String seatId = rowChar + "-" + col;
                
                // 根據座位位置判斷區域
                String region = getBigRoomSeatRegion(rowChar, col);
                
                // 如果不是有效座位，添加無效座位標記
                if (region.equals("invalid")) {
                    JPanel invalidSeat = new JPanel();
                    invalidSeat.setPreferredSize(new Dimension(SEAT_SIZE, SEAT_SIZE));
                    invalidSeat.setBackground(invalidSeatColor);
                    invalidSeat.setBorder(null);
                    seatsPanel.add(invalidSeat, gbc);
                } else {
                    // 創建座位按鈕
                    JToggleButton seatButton = createSeatButton(seatId, region);
                    seatsPanel.add(seatButton, gbc);
                }
            }
        }
        
        seatsPanel.revalidate();
        seatsPanel.repaint();
    }
    
    /**
     * 創建小廳座位配置
     * 根據 small_room.json 資料建立座位
     */
    private void createSmallRoomSeats() {
        seatsPanel.removeAll();
        
        // 使用 GridBagLayout 以實現更靈活的布局
        seatsPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        int maxRows = 9; // A-I
        int maxCols = 16; // 1-16
        
        // 添加列標題（數字）
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        for (int col = 1; col <= maxCols; col++) {
            gbc.gridx = col;
            JLabel colLabel = new JLabel(String.valueOf(col));
            colLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
            colLabel.setForeground(Color.DARK_GRAY);
            seatsPanel.add(colLabel, gbc);
        }
        
        // 建立所有座位
        for (int row = 0; row < maxRows; row++) {
            char rowChar = (char) ('A' + row);
            
            // 添加行標題（字母）
            gbc.gridx = 0;
            gbc.gridy = row + 1;
            JLabel rowLabel = new JLabel(String.valueOf(rowChar));
            rowLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            rowLabel.setForeground(Color.DARK_GRAY);
            seatsPanel.add(rowLabel, gbc);
            
            for (int col = 1; col <= maxCols; col++) {
                gbc.gridx = col;
                String seatId = rowChar + "-" + col;
                
                // 在小廳中添加一些間隔來模擬過道
                if (col == 4 || col == 13) {
                    // 過道位置
                    JPanel aisle = new JPanel();
                    aisle.setPreferredSize(new Dimension(SEAT_SIZE/2, SEAT_SIZE));
                    aisle.setBackground(invalidSeatColor);
                    aisle.setBorder(null);
                    seatsPanel.add(aisle, gbc);
                } else {
                    // 根據位置確定座位區域
                    String region = getSmallRoomSeatRegion(rowChar, col);
                    
                    // 創建座位按鈕
                    JToggleButton seatButton = createSeatButton(seatId, region);
                    seatsPanel.add(seatButton, gbc);
                }
            }
        }
        
        seatsPanel.revalidate();
        seatsPanel.repaint();
        
        // 更新影廳信息標籤
        updateTheaterInfoLabel("小廳 (標準影廳) - 共" + maxRows + "排，每排最多" + maxCols + "個座位");
    }
    
    /**
     * 更新影廳信息標籤
     */
    private void updateTheaterInfoLabel(String info) {
        theaterInfoLabel.setText(info);
    }
    
    /**
     * 創建單個座位按鈕
     */
    private JToggleButton createSeatButton(String seatId, String region) {
        JToggleButton seatButton = new JToggleButton();
        seatButton.setPreferredSize(new Dimension(SEAT_SIZE, SEAT_SIZE));
        
        // 顯示座位號碼
        seatButton.setText(seatId);
        seatButton.setFont(new Font("SansSerif", Font.PLAIN, 9));
        seatButton.setMargin(new Insets(0, 0, 0, 0));
        
        // 根據座位區域設置顏色
        switch (region) {
            case "blue":
                seatButton.setBackground(blueSeatColor);
                break;
            case "yellow":
                seatButton.setBackground(yellowSeatColor);
                break;
            case "red":
                seatButton.setBackground(redSeatColor);
                break;
            default:
                seatButton.setBackground(standardSeatColor);
                break;
        }
        
        // 如果座位已被預訂，設置為不可用
        if (reservedSeats.contains(seatId)) {
            seatButton.setEnabled(false);
            seatButton.setBackground(reservedSeatColor);
            seatButton.setText("X");
        }
        
        // 添加座位選擇事件
        seatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (seatButton.isSelected()) {
                    selectedSeats.add(seatId);
                    // 增強選中效果：更亮的顏色與邊框
                    seatButton.setBackground(selectedSeatColor);
                    seatButton.setBorder(new LineBorder(Color.BLACK, 2));
                    seatButton.setForeground(Color.WHITE);  // 白色文字增加對比度
                } else {
                    selectedSeats.remove(seatId);
                    // 恢復原來的顏色和邊框
                    if ("blue".equals(region)) {
                        seatButton.setBackground(blueSeatColor);
                    } else if ("yellow".equals(region)) {
                        seatButton.setBackground(yellowSeatColor);
                    } else if ("red".equals(region)) {
                        seatButton.setBackground(redSeatColor);
                    } else {
                        seatButton.setBackground(standardSeatColor);
                    }
                    seatButton.setBorder(UIManager.getBorder("ToggleButton.border"));
                    seatButton.setForeground(Color.BLACK);  // 恢復黑色文字
                }
                
                // 更新已選座位顯示區域
                updateSelectedSeatsPanel();
                
                // 如果有回調，通知上層組件
                if (callback != null) {
                    callback.onSeatsSelected(getSelectedSeats());
                }
            }
        });
        
        seatButtons.put(seatId, seatButton);
        return seatButton;
    }
    
    /**
     * 更新已選擇座位的顯示
     */
    private void updateSelectedSeatsPanel() {
        selectedSeatsPanel.removeAll();
        
        if (selectedSeats.isEmpty()) {
            selectedSeatsPanel.add(new JLabel("尚未選擇座位"));
        } else {
            List<String> sortedSeats = new ArrayList<>(selectedSeats);
            Collections.sort(sortedSeats);
            
            JLabel label = new JLabel("已選座位: ");
            selectedSeatsPanel.add(label);
            
            for (String seat : sortedSeats) {
                JLabel seatLabel = new JLabel(seat + " ");
                seatLabel.setForeground(Color.BLUE);
                selectedSeatsPanel.add(seatLabel);
            }
        }
        
        selectedSeatsPanel.revalidate();
        selectedSeatsPanel.repaint();
    }
    
    /**
     * 大廳座位區域判斷邏輯
     */
    private String getBigRoomSeatRegion(char row, int col) {
        // 檢查是否在有效範圍內
        if (row < 'A' || row > 'M') return "invalid";
        if (col < 1 || col > 38) return "invalid";
        
        // 大廳布局：
        // 紅區：黃金中心區域 (H-J, 13-26)
        // 黃區：次佳視野區 (F-L, 8-31)
        // 藍區：標準視野區 (C-N, 5-34)
        // 其餘為標準座位
        
        // 檢查是否為走道 - 大廳有三個走道
        if (col == 9 || col == 20 || col == 30) {
            return "invalid"; // 走道
        }
        
        // 檢查是否為無效座位區域 - 例如角落或特殊區域
        if ((row >= 'K' && col <= 6) || (row >= 'K' && col >= 33)) {
            return "invalid"; // 角落無效區域
        }
        
        // 判斷不同價格區域
        if (row >= 'H' && row <= 'J' && col >= 13 && col <= 26) {
            return "red"; // 紅區 - VIP座位
        } else if (row >= 'F' && row <= 'L' && col >= 8 && col <= 31) {
            return "yellow"; // 黃區 - 次佳座位
        } else if (row >= 'C' && row <= 'N' && col >= 5 && col <= 34) {
            return "blue"; // 藍區 - 標準優良座位
        } else {
            return "standard"; // 標準座位
        }
    }
    
    /**
     * 小廳座位區域判斷邏輯
     */
    private String getSmallRoomSeatRegion(char row, int col) {
        // 小廳只有藍區和標準區
        if ((row >= 'D' && row <= 'F') && (col >= 5 && col <= 12)) {
            return "blue"; // 中央較好的座位為藍區
        } else {
            return "standard"; // 其他為標準座位
        }
    }
}