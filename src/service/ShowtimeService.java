package service;

import dao.ShowtimeDAO;
import model.Showtime;

import java.util.List;
import java.sql.SQLException;

public class ShowtimeService {
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private final MovieService movieService = new MovieService();

    public void addShowtime(int movieUid, int theaterUid, String startTime) {
        // 計算結束時間 = 開始時間 + 影片片長
        int duration = movieService.getMovieById(movieUid)
                            .orElseThrow(() -> new IllegalArgumentException("找不到電影ID: " + movieUid))
                            .getDuration();
        try {
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            java.time.LocalDateTime st = java.time.LocalDateTime.parse(startTime, fmt);
            String endTime = st.plusMinutes(duration).format(fmt);
            // 檢查影廳及同電影時段是否有衝突
            try {
                if (showtimeDAO.hasConflict(null, theaterUid, movieUid, startTime, endTime)) {
                    throw new IllegalArgumentException("該時段或同電影時段有衝突，無法新增場次");
                }
            } catch (SQLException e) {
                throw new RuntimeException("檢查衝突失敗", e);
            }
            Showtime showtime = new Showtime(0, movieUid, theaterUid, startTime, endTime);
            int id = showtimeDAO.addShowtime(showtime);
            if (id < 0) {
                throw new IllegalArgumentException("新增場次失敗(可能衝突或其他問題)");
            }
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalArgumentException("時間格式錯誤，需為 yyyy-MM-dd HH:mm");
        }
    }

    public List<Showtime> listShowtimes() {
        return showtimeDAO.getAllShowtimes();
    }

    public List<Showtime> getShowtimesByMovieId(int movieId) {
        return showtimeDAO.getShowtimesByMovieId(movieId);
    }

    /**
     * Retrieves a showtime by its unique ID.
     * @param showtimeId The UID of the showtime.
     * @return The Showtime object if found, otherwise null.
     */
    public Showtime getShowtimeById(int showtimeId) {
        return showtimeDAO.getShowtimeById(showtimeId);
    }

    /**
     * Returns all showtimes.
     */
    public List<Showtime> getAllShowtimes() {
        return listShowtimes();
    }

    public boolean updateShowtimeTime(int showtimeId, String newStartTime) {
        Showtime showtime = showtimeDAO.getShowtimeById(showtimeId);
        if (showtime == null) {
            throw new IllegalArgumentException("找不到場次ID: " + showtimeId);
        }
        // 重新計算結束時間
        int duration = movieService.getMovieById(showtime.getMovieUid())
                            .orElseThrow(() -> new IllegalArgumentException("找不到電影ID: " + showtime.getMovieUid()))
                            .getDuration();
        try {
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            java.time.LocalDateTime st = java.time.LocalDateTime.parse(newStartTime, fmt);
            String newEnd = st.plusMinutes(duration).format(fmt);
            // 檢查影廳及同電影時段是否有衝突
            try {
                if (showtimeDAO.hasConflict(showtime.getUid(), showtime.getTheaterUid(), showtime.getMovieUid(), newStartTime, newEnd)) {
                    throw new IllegalArgumentException("該時段或同電影時段有衝突，無法更新場次");
                }
            } catch (SQLException e) {
                throw new RuntimeException("檢查衝突失敗", e);
            }
            showtime.setStartTime(newStartTime);
            showtime.setEndTime(newEnd);
            return showtimeDAO.updateShowtime(showtime);
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalArgumentException("時間格式錯誤，需為 yyyy-MM-dd HH:mm");
        }
    }

    /**
     * 刪除指定場次
     * @param showtimeId 要刪除的場次 ID
     * @return 刪除是否成功
     */
    public boolean deleteShowtime(int showtimeId) {
        return showtimeDAO.deleteShowtime(showtimeId);
    }
}