package dao;

import model.Member;
import util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MemberDAO {

    public void addMember(Member member) {
        // 檢查 email 是否已存在
        if (getMemberByEmail(member.getEmail()) != null) {
            throw new IllegalArgumentException("Email already exists: " + member.getEmail());
        }

        String sql = "INSERT INTO member (email, password, birth_date) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, member.getEmail());
            stmt.setString(2, member.getPassword());
            stmt.setString(3, member.getBirthDate());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Member getMemberByEmail(String email) {
        String sql = "SELECT * FROM member WHERE email = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Member(
                            rs.getInt("uid"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("birth_date")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves a member by their unique ID (uid).
     * @param uid The unique ID of the member.
     * @return The Member object if found, null otherwise.
     */
    public Member getMemberByUid(int uid) {
        String sql = "SELECT * FROM member WHERE uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, uid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Member(
                            rs.getInt("uid"),
                            rs.getString("email"),
                            rs.getString("password"), // Note: Password hash is returned
                            rs.getString("birth_date")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Member> getAllMembers() {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT * FROM member";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                members.add(new Member(
                        rs.getInt("uid"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("birth_date")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    public void updateMember(Member member) {
        String sql = "UPDATE member SET password = ?, birth_date = ? WHERE email = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, member.getPassword());
            stmt.setString(2, member.getBirthDate());
            stmt.setString(3, member.getEmail());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteMember(String email) {
        String sql = "DELETE FROM member WHERE email = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}