package dao;

import model.Reservation;
import util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    /**
     * Adds a new reservation to the database.
     * reservation_time and status are set by DB defaults.
     * Returns the generated UID of the new reservation, or -1 on failure.
     */
    public int addReservation(Reservation reservation) {
        String checkSql = "SELECT uid FROM reservation WHERE seat_no = ? AND time = ? AND status = 'CANCELLED'";
        String updateSql = "UPDATE reservation SET member_uid = ?, movie_uid = ?, theater_uid = ?, num_tickets = ?, status = 'CONFIRMED' WHERE uid = ?";
        String insertSql = "INSERT INTO reservation (member_uid, movie_uid, theater_uid, time, seat_no, num_tickets, status) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBUtil.getConnection()) {
            // 檢查是否存在相同座位和時間的 CANCELLED 記錄
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, reservation.getSeatNo());
                checkStmt.setString(2, reservation.getTime());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        // 如果存在，更新該記錄
                        int existingId = rs.getInt("uid");
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setInt(1, reservation.getMemberUid());
                            updateStmt.setInt(2, reservation.getMovieUid());
                            // 使用 showtimeUid 而非 theaterUid，但由於資料庫限制，我們將其存入 theater_uid 欄位
                            updateStmt.setInt(3, reservation.getShowtimeUid());
                            updateStmt.setInt(4, reservation.getNumTickets());
                            updateStmt.setInt(5, existingId);
                            updateStmt.executeUpdate();
                            return existingId;
                        }
                    }
                }
            }

            // 如果不存在，插入新記錄
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setInt(1, reservation.getMemberUid());
                insertStmt.setInt(2, reservation.getMovieUid());
                // 使用 showtimeUid 而非 theaterUid，但由於資料庫限制，我們將其存入 theater_uid 欄位
                insertStmt.setInt(3, reservation.getShowtimeUid());
                insertStmt.setString(4, reservation.getTime());
                insertStmt.setString(5, reservation.getSeatNo());
                insertStmt.setInt(6, reservation.getNumTickets());
                insertStmt.setString(7, reservation.getStatus());
                insertStmt.executeUpdate();

                try (ResultSet rs = insertStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                System.err.println("座位已被預訂: " + reservation.getSeatNo());
            } else {
                e.printStackTrace();
            }
        }
        return -1;
    }

    public Reservation getReservationById(int id) {
        String sql = "SELECT uid, member_uid, movie_uid, theater_uid, time, seat_no, num_tickets, status FROM reservation WHERE uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToReservation(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Reservation> getAllReservations() {
        List<Reservation> reservations = new ArrayList<>();
        // 修改 SQL 查詢以獲取所有訂單，包括所有欄位，無論狀態如何
        String sql = "SELECT uid, member_uid, movie_uid, theater_uid, time, seat_no, status, num_tickets FROM reservation ORDER BY time DESC";
        System.out.println("執行查詢: " + sql);
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            int count = 0;
            while (rs.next()) {
                count++;
                Reservation res = new Reservation(
                    rs.getInt("uid"),
                    rs.getInt("member_uid"),
                    rs.getInt("movie_uid"),
                    rs.getInt("theater_uid"),
                    rs.getString("time"),
                    rs.getString("seat_no"),
                    rs.getString("status"),
                    rs.getInt("num_tickets")
                );
                // 確保設置正確的場次ID
                res.setShowtimeUid(rs.getInt("theater_uid"));
                reservations.add(res);
                System.out.println("讀取訂單 #" + count + ": " + res.getUid() + ", 會員ID: " + res.getMemberUid() + ", 狀態: " + res.getStatus());
            }
            System.out.println("總共讀取到 " + count + " 條訂單記錄");
        } catch (SQLException e) {
            System.err.println("getAllReservations 發生 SQL 錯誤: " + e.getMessage());
            e.printStackTrace();
        }
        return reservations;
    }

    /**
     * Retrieves all reservations for a specific member.
     */
    public List<Reservation> getReservationsByMemberId(int memberUid) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservation WHERE member_uid = ? AND status != 'CANCELLED'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, memberUid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(new Reservation(
                            rs.getInt("uid"),
                            rs.getInt("member_uid"),
                            rs.getInt("movie_uid"),
                            rs.getInt("theater_uid"),
                            rs.getString("time"),
                            rs.getString("seat_no"),
                            rs.getString("status"),
                            rs.getInt("num_tickets")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reservations;
    }

    /**
     * Updates the status of a specific reservation.
     * Returns true if the update was successful, false otherwise.
     */
    public boolean updateReservationStatus(int reservationId, String status) {
        String sql = "UPDATE reservation SET status = ? WHERE uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, reservationId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 在指定的數據庫連接上更新訂票狀態，用於事務控制
     * @param conn 資料庫連接
     * @param reservationId 訂票ID
     * @param status 新狀態
     * @return 是否更新成功
     * @throws SQLException 如果發生 SQL 錯誤
     */
    public boolean updateReservationStatusWithConnection(Connection conn, int reservationId, String status) throws SQLException {
        String sql = "UPDATE reservation SET status = ? WHERE uid = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, reservationId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    // 刪除重複的 deleteReservation 方法，保留唯一的定義
    public void deleteReservation(int id) {
        String sql = "DELETE FROM reservation WHERE uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Reservation> getReservationsByShowtimeAndSeat(int showtimeUid, String seatNo) {
        List<Reservation> reservations = new ArrayList<>();
        // 修正：使用 theater_uid 欄位存儲 showtimeUid
        String sql = "SELECT * FROM reservation WHERE theater_uid = ? AND seat_no = ? AND status = 'CONFIRMED'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, showtimeUid);
            stmt.setString(2, seatNo);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Reservation res = mapResultSetToReservation(rs);
                    // 確保設置正確的場次ID
                    res.setShowtimeUid(rs.getInt("theater_uid"));
                    reservations.add(res);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reservations;
    }

    /**
     * Retrieves all confirmed reservations for a specific showtime.
     */
    public List<Reservation> getReservationsByShowtimeId(int showtimeUid) {
        List<Reservation> reservations = new ArrayList<>();
        // 修正：使用 theater_uid 欄位存儲 showtimeUid
        String sql = "SELECT * FROM reservation WHERE theater_uid = ? AND status = 'CONFIRMED'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, showtimeUid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Reservation res = mapResultSetToReservation(rs);
                    // 確保設置正確的場次ID
                    res.setShowtimeUid(rs.getInt("theater_uid"));
                    reservations.add(res);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reservations;
    }

    /**
     * 獲取指定場次和座位的訂票，並使用 FOR UPDATE 鎖定相關行，防止並發問題
     * 此方法必須在一個事務中調用
     */
    public List<Reservation> getLockedReservationsByShowtimeAndSeat(Connection conn, int showtimeUid, String seatNo) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservation WHERE theater_uid = ? AND seat_no = ? AND status = 'CONFIRMED'";  // SQLite 不支援 FOR UPDATE
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, showtimeUid);
            stmt.setString(2, seatNo);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Reservation res = mapResultSetToReservation(rs);
                    // 確保設置正確的場次ID
                    res.setShowtimeUid(rs.getInt("theater_uid"));
                    reservations.add(res);
                }
            }
        }
        return reservations;
    }

    /**
     * 在指定的數據庫連接上添加一個新的訂票，用於事務控制
     */
    public int addReservationWithConnection(Connection conn, Reservation reservation) throws SQLException {
        String checkSql = "SELECT uid FROM reservation WHERE seat_no = ? AND theater_uid = ? AND status = 'CANCELLED'";
        String updateSql = "UPDATE reservation SET member_uid = ?, movie_uid = ?, theater_uid = ?, num_tickets = ?, status = 'CONFIRMED' WHERE uid = ?";
        String insertSql = "INSERT INTO reservation (member_uid, movie_uid, theater_uid, time, seat_no, num_tickets, status) VALUES (?, ?, ?, ?, ?, ?, ?)";

        // 檢查是否存在相同座位和時間的 CANCELLED 記錄
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, reservation.getSeatNo());
            checkStmt.setInt(2, reservation.getShowtimeUid());
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    // 如果存在，更新該記錄
                    int existingId = rs.getInt("uid");
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, reservation.getMemberUid());
                        updateStmt.setInt(2, reservation.getMovieUid());
                        updateStmt.setInt(3, reservation.getShowtimeUid());
                        updateStmt.setInt(4, reservation.getNumTickets());
                        updateStmt.setInt(5, existingId);
                        updateStmt.executeUpdate();
                        return existingId;
                    }
                }
            }
        }

        // 如果不存在，插入新記錄
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            insertStmt.setInt(1, reservation.getMemberUid());
            insertStmt.setInt(2, reservation.getMovieUid());
            insertStmt.setInt(3, reservation.getShowtimeUid());
            insertStmt.setString(4, reservation.getTime());
            insertStmt.setString(5, reservation.getSeatNo());
            insertStmt.setInt(6, reservation.getNumTickets());
            insertStmt.setString(7, reservation.getStatus());
            insertStmt.executeUpdate();

            try (ResultSet rs = insertStmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        return -1;
    }

    /**
     * 獲取指定 ID 的訂票，並使用 FOR UPDATE 鎖定相關行，防止並發問題
     * 此方法必須在一個事務中調用
     */
    public Reservation getLockedReservationById(Connection conn, int id) throws SQLException {
        String sql = "SELECT * FROM reservation WHERE uid = ?";  // SQLite 不支援 FOR UPDATE
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToReservation(rs);
                }
            }
        }
        return null;
    }

    /**
     * 獲取指定場次的所有訂票
     */
    public List<Reservation> getReservationsByShowtime(int showtimeUid) {
        List<Reservation> reservations = new ArrayList<>();
        // 由於資料庫結構限制，使用 theater_uid 欄位存儲 showtimeUid
        String sql = "SELECT * FROM reservation WHERE theater_uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, showtimeUid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Reservation res = mapResultSetToReservation(rs);
                    // 確保設置正確的場次ID
                    res.setShowtimeUid(rs.getInt("theater_uid"));
                    reservations.add(res);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reservations;
    }

    // Helper method to map ResultSet to Reservation object
    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        Reservation res = new Reservation(
                rs.getInt("uid"),
                rs.getInt("member_uid"),
                rs.getInt("movie_uid"),
                rs.getInt("theater_uid"),  // 這裡實際存儲的是場次ID
                rs.getString("time"),
                rs.getString("seat_no"),
                rs.getString("status"),
                rs.getInt("num_tickets")
        );
        // 將 theater_uid 欄位值設為 showtimeUid
        res.setShowtimeUid(rs.getInt("theater_uid"));
        return res;
    }
}