package service;

import dao.ReservationDAO;
import dao.MemberDAO;
import dao.ShowtimeDAO;
import dao.MovieDAO;
import model.Reservation;
import exception.AgeRestrictionException;
import exception.SeatUnavailableException;
import model.Movie;
import model.Showtime;
import model.Member;
import util.DBUtil;

import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReservationService {
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private final MovieDAO movieDAO = new MovieDAO();

    /**
     * 單一座位訂票方法 (保留舊方法以支援向下兼容)
     * 內部調用優化後的多座位訂票方法
     */
    public void bookTicket(int memberUid, int showtimeUid, String seatNo, int numTickets) throws AgeRestrictionException, SeatUnavailableException {
        // 獲取場次資訊
        Showtime showtime = showtimeDAO.getShowtimeById(showtimeUid);
        if (showtime == null) {
            throw new IllegalArgumentException("電影場次不存在");
        }

        // 檢查會員是否存在
        Member member = memberDAO.getMemberByUid(memberUid);
        if (member == null) {
            throw new IllegalArgumentException("會員不存在");
        }

        // 檢查電影分級與會員年齡
        Movie movie = movieDAO.getMovieById(showtime.getMovieUid());
        if (movie == null) {
            throw new IllegalArgumentException("電影不存在");
        }
        int memberAge = member.getAge();
        int minimumAge = movie.getMinimumAge();
        
        // 如果年齡計算失敗（返回-1），拒絕訂票
        if (memberAge < 0) {
            throw new AgeRestrictionException("無法確認會員年齡，請檢查生日資料");
        }
        
        // 年齡限制檢查：會員年齡必須大於等於電影最低年齡
        if (memberAge < minimumAge) {
            throw new AgeRestrictionException(String.format("會員年齡不符合電影分級要求（會員年齡：%d歲，電影最低年齡：%d歲）", memberAge, minimumAge));
        }

        // 調用多座位訂票方法，只訂一個座位
        List<String> seats = new ArrayList<>();
        seats.add(seatNo);
        List<Integer> reservations = bookTickets(memberUid, showtime.getMovieUid(), showtimeUid, seats);
        
        if (reservations == null || reservations.isEmpty()) {
            throw new RuntimeException("訂票失敗");
        }
        
        System.out.println("訂票成功，訂票編號: " + reservations.get(0));
    }

    /**
     * 訂票方法，重構版本，添加事務控制和原子鎖定機制
     * @param memberUid 會員ID
     * @param movieUid 電影ID
     * @param showtimeUid 場次ID
     * @param selectedSeats 選擇的座位
     * @return 訂單ID列表，如果為空表示預訂失敗
     * @throws AgeRestrictionException 年齡限制異常
     * @throws SeatUnavailableException 座位不可用異常
     */
    public List<Integer> bookTickets(int memberUid, int movieUid, int showtimeUid, List<String> selectedSeats) 
            throws AgeRestrictionException, SeatUnavailableException {
        List<Integer> reservationIds = new ArrayList<>();
        Connection conn = null;
        
        try {
            // 檢查會員是否存在
            Member member = memberDAO.getMemberByUid(memberUid);
            if (member == null) {
                throw new IllegalArgumentException("會員不存在");
            }
            
            // 檢查場次是否存在
            Showtime showtime = showtimeDAO.getShowtimeById(showtimeUid);
            if (showtime == null) {
                throw new IllegalArgumentException("電影場次不存在");
            }
            
            // 確認場次對應的電影
            Movie movie = movieDAO.getMovieById(movieUid);
            if (movie == null) {
                throw new IllegalArgumentException("電影不存在");
            }

            // 檢查電影分級與會員年齡
            int memberAge = member.getAge();
            int minimumAge = movie.getMinimumAge();
            
            // 如果年齡計算失敗（返回-1），拒絕訂票
            if (memberAge < 0) {
                throw new AgeRestrictionException("無法確認會員年齡，請檢查生日資料");
            }
            
            // 年齡限制檢查：會員年齡必須大於等於電影最低年齡
            if (memberAge < minimumAge) {
                throw new AgeRestrictionException(String.format("會員年齡不符合電影分級要求（會員年齡：%d歲，電影最低年齡：%d歲）", memberAge, minimumAge));
            }
            
            // 確認電影ID與場次所屬電影ID一致
            if (showtime.getMovieUid() != movieUid) {
                throw new IllegalArgumentException("電影ID與場次所屬電影ID不一致");
            }

            // 開始事務
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);
            
            // 1. 檢查座位是否已被預訂（使用行鎖定）
            for (String seat : selectedSeats) {
                List<Reservation> existingReservations = reservationDAO.getLockedReservationsByShowtimeAndSeat(conn, showtimeUid, seat);
                if (!existingReservations.isEmpty()) {
                    // 檢查是否有非取消狀態的訂票
                    boolean seatOccupied = existingReservations.stream()
                            .anyMatch(r -> !"CANCELLED".equals(r.getStatus()));
                    if (seatOccupied) {
                        // 座位已被佔用，拋出異常
                        throw new SeatUnavailableException("座位 " + seat + " 已被預訂");
                    }
                }
            }
            
            // 2. 檢查場次是否有足夠的可用座位
            if (showtime.getAvailableSeats() < selectedSeats.size()) {
                throw new SeatUnavailableException("場次沒有足夠的可用座位");
            }
            
            // 3. 減少場次的可用座位數量
            boolean seatsDecreased = showtimeDAO.decreaseAvailableSeatsWithConnection(conn, showtimeUid, selectedSeats.size());
            if (!seatsDecreased) {
                throw new SeatUnavailableException("無法預訂所選座位，可能座位已被他人預訂");
            }
            
            // 4. 創建訂票紀錄
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            for (String seat : selectedSeats) {
                // 創建一個新的 Reservation 對象，使用適當的參數 (uid為0,會在DAO中生成)
                Reservation reservation = new Reservation(
                    0, // uid - 將由數據庫生成
                    memberUid,
                    movieUid,
                    showtime.getTheaterUid(),
                    currentTime,
                    seat,
                    "CONFIRMED",
                    1 // 每個座位對應一張票
                );
                // 設置場次ID
                reservation.setShowtimeUid(showtimeUid);
                
                // 使用事務內的方法添加訂票
                int reservationId = reservationDAO.addReservationWithConnection(conn, reservation);
                if (reservationId > 0) {
                    reservationIds.add(reservationId);
                } else {
                    // 如果添加失敗，拋出異常
                    throw new SQLException("無法創建訂票記錄");
                }
            }
            
            // 提交事務
            conn.commit();
            return reservationIds;
            
        } catch (SeatUnavailableException | AgeRestrictionException | IllegalArgumentException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            // 重新拋出以通知上層
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("訂票失敗，錯誤詳情：" + e.getMessage());
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return new ArrayList<>(); // 返回空列表表示預訂失敗
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true); // 恢復自動提交
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 訂票方法重載版本，自動從場次中獲取電影ID
     * @param memberUid 會員ID
     * @param showtimeUid 場次ID
     * @param selectedSeats 選擇的座位
     * @return 成功訊息或錯誤訊息
     */
    public String bookTickets(int memberUid, int showtimeUid, List<String> selectedSeats) {
        try {
            // 先獲取場次信息
            Showtime showtime = showtimeDAO.getShowtimeById(showtimeUid);
            if (showtime == null) {
                return "錯誤：找不到指定場次";
            }
            
            // 從場次中獲取電影ID
            int movieUid = showtime.getMovieUid();
            
            // 調用原有方法進行訂票
            List<Integer> reservationIds = bookTickets(memberUid, movieUid, showtimeUid, selectedSeats);
            
            if (reservationIds != null && !reservationIds.isEmpty()) {
                return "訂票成功！訂票編號: " + reservationIds;
            } else {
                return "訂票失敗，請稍後再試";
            }
        } catch (AgeRestrictionException e) {
            return "錯誤：" + e.getMessage();
        } catch (SeatUnavailableException e) {
            return "錯誤：" + e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return "發生錯誤：" + e.getMessage();
        }
    }

    /**
     * 獲取場次的已預訂座位
     * 只考慮狀態為 CONFIRMED 的訂票
     */
    public List<String> getBookedSeatsForShowtime(int showtimeUid) {
        List<Reservation> reservations = reservationDAO.getReservationsByShowtime(showtimeUid);
        return reservations.stream()
                .filter(r -> "CONFIRMED".equals(r.getStatus())) // 只考慮確認狀態的訂票
                .map(Reservation::getSeatNo)
                .collect(Collectors.toList());
    }

    public List<Reservation> listReservations() {
        return reservationDAO.getAllReservations();
    }

    public List<Reservation> listReservationsByMember(int memberUid) {
        return reservationDAO.getReservationsByMemberId(memberUid);
    }

    public boolean cancelReservation(int reservationId, int memberUid) {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);
            
            // 1. 使用鎖定方法獲取訂單，防止高併發時的問題
            Reservation reservation = reservationDAO.getLockedReservationById(conn, reservationId);

            // 2. 檢查訂單是否存在且屬於該會員
            if (reservation == null) {
                System.err.println("Cancellation failed: Reservation with ID " + reservationId + " not found.");
                conn.rollback();
                return false;
            }
            if (reservation.getMemberUid() != memberUid) {
                System.err.println("Cancellation failed: Reservation ID " + reservationId + " does not belong to member ID " + memberUid + ".");
                conn.rollback();
                return false;
            }

            // 3. 檢查狀態是否已取消
            if ("CANCELLED".equalsIgnoreCase(reservation.getStatus())) {
                System.err.println("Cancellation failed: Reservation ID " + reservationId + " is already cancelled.");
                conn.rollback();
                return false;
            }

            // 4. 檢查電影開始時間是否在30分鐘內
            try {
                // 獲取場次信息
                Showtime showtime = showtimeDAO.getShowtimeById(reservation.getShowtimeUid());
                if (showtime == null) {
                    System.err.println("Cancellation failed: Showtime not found for reservation ID " + reservationId);
                    conn.rollback();
                    return false;
                }

                // 解析電影開始時間
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                Date showtimeDate = dateFormat.parse(showtime.getStartTime());
                Date currentTime = new Date();

                // 計算時間差（毫秒）
                long differenceInMillis = showtimeDate.getTime() - currentTime.getTime();
                long differenceInMinutes = differenceInMillis / (60 * 1000);

                // 如果在電影開始前30分鐘或之後，拒絕取消訂票
                if (differenceInMinutes <= 30) {
                    if (differenceInMinutes >= 0) {
                        // 在電影開始前30分鐘內(含)
                        System.err.println("Cancellation failed: Cannot cancel ticket within 30 minutes before showtime.");
                    } else {
                        // 電影已經開始後
                        System.err.println("Cancellation failed: Cannot cancel ticket after showtime has started.");
                    }
                    conn.rollback();
                    return false;
                }

                // 更新訂票狀態為已取消
                boolean updateSuccess = reservationDAO.updateReservationStatusWithConnection(conn, reservationId, "CANCELLED");
                if (!updateSuccess) {
                    System.err.println("Cancellation failed: Could not update reservation status.");
                    conn.rollback();
                    return false;
                }

                // 增加場次的可用座位數量
                boolean seatsIncreased = showtimeDAO.increaseAvailableSeatsWithConnection(conn, showtime.getUid(), reservation.getNumTickets());
                if (!seatsIncreased) {
                    System.err.println("Cancellation failed: Could not update available seats.");
                    conn.rollback();
                    return false;
                }

                // 提交事務
                conn.commit();
                System.out.println("Reservation ID " + reservationId + " has been successfully cancelled.");
                return true;
                
            } catch (ParseException e) {
                System.err.println("Cancellation failed: Error parsing showtime date.");
                conn.rollback();
                return false;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 管理員設置訂票狀態
     * @param reservationId 訂票ID
     * @param status 新狀態
     * @return 是否更新成功
     */
    public boolean setReservationStatus(int reservationId, String status) {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);
            
            // 獲取並鎖定訂單
            Reservation reservation = reservationDAO.getLockedReservationById(conn, reservationId);
            
            // 檢查訂單是否存在
            if (reservation == null) {
                System.err.println("Status update failed: Reservation with ID " + reservationId + " not found.");
                conn.rollback();
                return false;
            }

            // 如果要取消訂單，需要釋放座位
            if ("CANCELLED".equalsIgnoreCase(status) && !"CANCELLED".equalsIgnoreCase(reservation.getStatus())) {
                Showtime showtime = showtimeDAO.getShowtimeById(reservation.getShowtimeUid());
                if (showtime != null) {
                    // 增加場次的可用座位數量
                    boolean seatsIncreased = showtimeDAO.increaseAvailableSeatsWithConnection(conn, showtime.getUid(), reservation.getNumTickets());
                    if (!seatsIncreased) {
                        System.err.println("Status update failed: Could not update available seats.");
                        conn.rollback();
                        return false;
                    }
                }
            }
            
            // 更新訂單狀態
            boolean updateSuccess = reservationDAO.updateReservationStatusWithConnection(conn, reservationId, status);
            if (!updateSuccess) {
                System.err.println("Status update failed: Could not update reservation status.");
                conn.rollback();
                return false;
            }
            
            // 提交事務
            conn.commit();
            System.out.println("Reservation ID " + reservationId + " status has been successfully updated to " + status);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 新增評論相關方法
    // 確保評論能正常提交並讓所有使用者都能看到
    public void addReview(int movieId, String userEmail, String reviewText) throws SQLException {
        String sql = "INSERT INTO reviews (movie_id, user_email, review_text) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, movieId);
            stmt.setString(2, userEmail);
            stmt.setString(3, reviewText);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("SQL錯誤: " + e.getMessage());
            throw new SQLException("無法提交評論，請檢查資料庫連線或查詢語法。", e);
        } catch (Exception e) {
            System.err.println("未知錯誤: " + e.getMessage());
            throw new RuntimeException("提交評論時發生未知錯誤。", e);
        }
    }

    // 增加詳細的例外處理和日誌
    public List<String[]> getReviewsByMovieId(int movieId) throws SQLException {
        List<String[]> reviews = new ArrayList<>();
        String sql = "SELECT user_email, review_text FROM reviews WHERE movie_id = ? ORDER BY id ASC"; // 確保評論按順序顯示
        try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, movieId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reviews.add(new String[]{rs.getString("user_email"), rs.getString("review_text")});
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL錯誤: " + e.getMessage());
            throw new SQLException("無法載入評論，請檢查資料庫連線或查詢語法。", e);
        } catch (Exception e) {
            System.err.println("未知錯誤: " + e.getMessage());
            throw new RuntimeException("載入評論時發生未知錯誤。", e);
        }
        return reviews;
    }
}