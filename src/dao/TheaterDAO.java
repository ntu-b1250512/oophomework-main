package dao;

import model.Theater;
import util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TheaterDAO {

    public int addTheater(Theater theater) {
        String sql = "INSERT INTO theater (type, total_seats) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, theater.getType());
            stmt.setInt(2, theater.getTotalSeats());
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                return -1; // 插入失敗
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1); // 返回生成的 ID
                } else {
                    return -1; // 無法獲取生成的 key
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public Theater getTheaterById(int id) {
        String sql = "SELECT * FROM theater WHERE uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Theater(
                            rs.getInt("uid"),
                            rs.getString("type"),
                            rs.getInt("total_seats")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public Theater getTheaterByType(String type) {
        String sql = "SELECT * FROM theater WHERE type = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Theater(
                            rs.getInt("uid"),
                            rs.getString("type"),
                            rs.getInt("total_seats")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Theater> getAllTheaters() {
        List<Theater> theaters = new ArrayList<>();
        String sql = "SELECT * FROM theater";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                theaters.add(new Theater(
                        rs.getInt("uid"),
                        rs.getString("type"),
                        rs.getInt("total_seats")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return theaters;
    }

    public boolean existsByType(String type) {
        String sql = "SELECT 1 FROM theater WHERE type = ?";
        try (var conn = DBUtil.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type);
            try (var rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateTheater(Theater theater) {
        String sql = "UPDATE theater SET type = ?, total_seats = ? WHERE uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, theater.getType());
            stmt.setInt(2, theater.getTotalSeats());
            stmt.setInt(3, theater.getUid());
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0; // 返回 true 如果至少修改了一行
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteTheater(int id) {
        String sql = "DELETE FROM theater WHERE uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0; // 返回 true 如果至少刪除了一行
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
