import java.sql.*;
import util.DBUtil;
import model.Member;
import dao.MemberDAO;
import model.Movie;
import dao.MovieDAO;

public class CheckMovieRating {
    public static void main(String[] args) {
        try (Connection conn = DBUtil.getConnection()) {
            // 檢查所有電影的分級
            System.out.println("所有電影的分級:");
            PreparedStatement stmt = conn.prepareStatement("SELECT uid, name, rating FROM movie");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                System.out.printf("ID: %d, 名稱: %s, 分級: %s%n", 
                    rs.getInt("uid"), rs.getString("name"), rs.getString("rating"));
            }
            
            // 檢查 PG 級電影
            System.out.println("\nPG級電影:");
            PreparedStatement pgStmt = conn.prepareStatement("SELECT uid, name, rating FROM movie WHERE rating = 'PG'");
            ResultSet pgRs = pgStmt.executeQuery();
            while (pgRs.next()) {
                System.out.printf("ID: %d, 名稱: %s, 分級: %s%n", 
                    pgRs.getInt("uid"), pgRs.getString("name"), pgRs.getString("rating"));
            }
            
            // 檢查 PG 級電影的最低年齡
            System.out.println("\n檢查 PG 級電影的最低年齡:");
            MovieDAO movieDAO = new MovieDAO();
            Movie zootopia = movieDAO.getMovieByName("Zootopia");
            Movie frozen = movieDAO.getMovieByName("Frozen");
            
            if (zootopia != null) {
                System.out.printf("Zootopia - 分級: %s, 最低年齡: %d%n", 
                    zootopia.getRating(), zootopia.getMinimumAge());
            }
            if (frozen != null) {
                System.out.printf("Frozen - 分級: %s, 最低年齡: %d%n", 
                    frozen.getRating(), frozen.getMinimumAge());
            }
            
            // 檢查會員年齡計算
            System.out.println("\n檢查會員年齡計算 (當前日期: 2025年6月9日):");
            MemberDAO memberDAO = new MemberDAO();
            
            // 測試不同生日的年齡計算
            String[] testBirthDates = {
                "2019-06-09", // 應該是 6 歲
                "2018-06-09", // 應該是 7 歲
                "2017-06-09", // 應該是 8 歲
                "2020-06-09", // 應該是 5 歲
            };
            
            for (String birthDate : testBirthDates) {
                Member testMember = new Member(0, "test@test.com", "password", birthDate);
                int age = testMember.getAge();
                System.out.printf("生日: %s -> 年齡: %d%n", birthDate, age);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}