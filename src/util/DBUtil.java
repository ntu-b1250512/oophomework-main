package util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.mindrot.jbcrypt.BCrypt;

public class DBUtil {
    private static final String URL = "jdbc:sqlite:cinema_booking.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load SQLite JDBC driver", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /**
     * 清空所有資料表
     */
    public static void clearDatabase() {
        try (Connection conn = getConnection()) {
            conn.createStatement().execute("DROP TABLE IF EXISTS reservation;");
            conn.createStatement().execute("DROP TABLE IF EXISTS showtime;");
            conn.createStatement().execute("DROP TABLE IF EXISTS seat;");
            conn.createStatement().execute("DROP TABLE IF EXISTS theater;");
            conn.createStatement().execute("DROP TABLE IF EXISTS movie;");
            conn.createStatement().execute("DROP TABLE IF EXISTS member;");
            conn.createStatement().execute("DROP TABLE IF EXISTS reviews;"); // 新增清除評論表
            System.out.println("Database cleared successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 確保在初始化資料庫時重新建立 reviews 表
    public static void initializeDatabase() {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String createMemberTable = "CREATE TABLE IF NOT EXISTS member (" +
                        "uid INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "email TEXT NOT NULL UNIQUE," +
                        "password TEXT NOT NULL," +
                        "birth_date TEXT NOT NULL" +
                        ");";

                String createMovieTable = "CREATE TABLE IF NOT EXISTS movie (" +
                        "uid INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name TEXT NOT NULL," +
                        "duration INTEGER NOT NULL," +
                        "description TEXT," +
                        "rating TEXT NOT NULL" +
                        ");";

                String createTheaterTable = "CREATE TABLE IF NOT EXISTS theater (" +
                        "uid INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "type TEXT NOT NULL UNIQUE," +
                        "total_seats INTEGER NOT NULL" +
                        ");";

                String createShowtimeTable = "CREATE TABLE IF NOT EXISTS showtime (" +
                        "uid INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "movie_uid INTEGER," +
                        "theater_uid INTEGER," +
                        "start_time TEXT NOT NULL," +
                        "end_time TEXT NOT NULL," +
                        "available_seats INTEGER NOT NULL DEFAULT 0," +
                        "FOREIGN KEY (movie_uid) REFERENCES movie(uid) ON DELETE CASCADE," +
                        "FOREIGN KEY (theater_uid) REFERENCES theater(uid) ON DELETE CASCADE" +
                        ");";

                String createReservationTable = "CREATE TABLE IF NOT EXISTS reservation (" +
                        "uid INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "member_uid INTEGER," +
                        "movie_uid INTEGER," +
                        "theater_uid INTEGER," +
                        "time TEXT NOT NULL," +
                        "seat_no TEXT," +
                        "num_tickets INTEGER NOT NULL," +
                        "status TEXT DEFAULT 'CONFIRMED'," +
                        "FOREIGN KEY (member_uid) REFERENCES member(uid) ON DELETE CASCADE," +
                        "FOREIGN KEY (movie_uid) REFERENCES movie(uid) ON DELETE CASCADE," +
                        "FOREIGN KEY (theater_uid) REFERENCES theater(uid) ON DELETE CASCADE," +
                        "UNIQUE (seat_no, time)" + // 添加唯一性約束
                        ");";

                conn.createStatement().execute(createMemberTable);
                conn.createStatement().execute(createMovieTable);
                String createMovieNameIndex = "CREATE UNIQUE INDEX IF NOT EXISTS idx_movie_name ON movie(name);";
                conn.createStatement().execute(createMovieNameIndex);
                conn.createStatement().execute(createTheaterTable);
                // conn.createStatement().execute(createSeatTable);
                conn.createStatement().execute(createShowtimeTable);
                conn.createStatement().execute(createReservationTable);

                String insertTheaters = "INSERT OR IGNORE INTO theater (type, total_seats) VALUES (?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertTheaters)) {
                    stmt.setString(1, "Hall A");
                    stmt.setInt(2, 100);
                    stmt.addBatch();

                    stmt.setString(1, "Hall B");
                    stmt.setInt(2, 50);
                    stmt.addBatch();

                    stmt.executeBatch();
                }

                // Insert default movies
                String insertMovies = "INSERT OR IGNORE INTO movie (name, duration, description, rating) VALUES (?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertMovies)) {
                    stmt.setString(1, "Star Wars");
                    stmt.setInt(2, 120);
                    stmt.setString(3, "A space opera about the battle between good and evil.");
                    stmt.setString(4, "PG-13");
                    stmt.addBatch();

                    stmt.setString(1, "Zootopia");
                    stmt.setInt(2, 108);
                    stmt.setString(3, "A city of anthropomorphic animals.");
                    stmt.setString(4, "PG");
                    stmt.addBatch();

                    stmt.setString(1, "Inception");
                    stmt.setInt(2, 148);
                    stmt.setString(3, "A mind-bending thriller about dreams within dreams.");
                    stmt.setString(4, "PG-13");
                    stmt.addBatch();

                    stmt.setString(1, "The Godfather");
                    stmt.setInt(2, 175);
                    stmt.setString(3, "The story of a powerful Italian-American crime family.");
                    stmt.setString(4, "R");
                    stmt.addBatch();

                    stmt.setString(1, "Frozen");
                    stmt.setInt(2, 102);
                    stmt.setString(3, "A magical tale of two sisters in a frozen kingdom.");
                    stmt.setString(4, "PG");
                    stmt.addBatch();

                    stmt.setString(1, "Avengers: Endgame");
                    stmt.setInt(2, 181);
                    stmt.setString(3, "The epic conclusion to the Marvel Cinematic Universe's Infinity Saga.");
                    stmt.setString(4, "PG-13");
                    stmt.addBatch();

                    stmt.executeBatch();
                }

                // 移除舊的 insertAdmin 預設管理員，僅保留以下 upsert:
                String adminUpsert = "INSERT INTO member (email, password, birth_date) VALUES (?, ?, ?) " +
                                     "ON CONFLICT(email) DO UPDATE SET password=excluded.password, birth_date=excluded.birth_date;";
                try (PreparedStatement stmt = conn.prepareStatement(adminUpsert)) {
                    stmt.setString(1, "admin@admin.com");
                    // 使用 BCrypt 雜湊預設管理員密碼
                    stmt.setString(2, BCrypt.hashpw("admin123", BCrypt.gensalt()));
                    stmt.setString(3, "2000-01-01");
                    stmt.executeUpdate();
                }

                // conn.createStatement().execute("DELETE FROM showtime;");
                // String insertShowtimes = "INSERT INTO showtime (movie_uid, theater_uid, time, available_seats) VALUES " +
                //                       "((SELECT uid FROM movie WHERE name='StarWar'), (SELECT uid FROM theater WHERE type='Hall A'), '2025-05-10 14:00', 100), " +
                //                       "((SELECT uid FROM movie WHERE name='Zootopia'), (SELECT uid FROM theater WHERE type='Hall B'), '2025-05-10 15:00', 50);";
                // conn.createStatement().execute(insertShowtimes);

                // 移除 SQL 插入預設場次，改為呼叫 Service 計算結束時間
                /*
                String insertShowtimes = "INSERT OR IGNORE INTO showtime (movie_uid, theater_uid, start_time, end_time, available_seats) VALUES ((SELECT uid FROM movie WHERE name = ?), (SELECT uid FROM theater WHERE type = ?), ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertShowtimes)) {
                    // ...batch insert...
                }
                */
                // 使用 ShowtimeService 自動計算結束時間並新增場次
                {
                    service.ShowtimeService showtimeService = new service.ShowtimeService();
                    dao.MovieDAO movieDAO = new dao.MovieDAO();
                    dao.TheaterDAO theaterDAO = new dao.TheaterDAO();
                    // 各影廳 ID
                    int hallAId = theaterDAO.getTheaterByType("Hall A").getUid();
                    int hallBId = theaterDAO.getTheaterByType("Hall B").getUid();
                    // 預設場次資料: {電影名稱, 影廳ID, 開始時間}
                    String[][] defaultShowtimes = {
                        {"Star Wars", String.valueOf(hallAId), "2025-06-10 14:00"},
                        {"Zootopia", String.valueOf(hallBId), "2025-06-10 15:00"},
                        {"Inception", String.valueOf(hallAId), "2025-06-11 16:00"},
                        {"The Godfather", String.valueOf(hallBId), "2025-06-11 17:00"},
                        {"Frozen", String.valueOf(hallAId), "2025-06-12 18:00"},
                        {"Avengers: Endgame", String.valueOf(hallBId), "2025-06-12 19:00"}
                    };
                    for (String[] entry : defaultShowtimes) {
                        String movieName = entry[0];
                        int theaterId = Integer.parseInt(entry[1]);
                        String startTime = entry[2];
                        model.Movie movie = movieDAO.getMovieByName(movieName);
                        if (movie != null) {
                            showtimeService.addShowtime(movie.getUid(), theaterId, startTime);
                        }
                    }
                }

                // 首先確保 reviews 表存在
                ensureReviewsTableExists();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void ensureReviewsTableExists() {
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, "reviews", null)) {
                if (!rs.next()) {
                    // Table does not exist, create it
                    String createTableSQL = "CREATE TABLE reviews (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "movie_id INTEGER NOT NULL," +
                            "user_email TEXT NOT NULL," +
                            "review_text TEXT NOT NULL," +
                            "FOREIGN KEY(movie_id) REFERENCES movie(uid)" +
                            ");";
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(createTableSQL);
                        System.out.println("Table 'reviews' created successfully.");
                    }
                } else {
                    System.out.println("Table 'reviews' already exists.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error ensuring 'reviews' table exists: " + e.getMessage(), e);
        }
    }

    public static void printReviewsTable() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM reviews")) {
            System.out.println("Reviews Table:");
            while (rs.next()) {
                System.out.printf("ID: %d, Movie ID: %d, User Email: %s, Review Text: %s\n",
                        rs.getInt("id"), rs.getInt("movie_id"), rs.getString("user_email"), rs.getString("review_text"));
            }
        } catch (SQLException e) {
            System.err.println("Error reading 'reviews' table: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("Initializing database...");
        initializeDatabase();
        ensureReviewsTableExists();
        System.out.println("Database initialized successfully.");
    }
}