package model;

import java.util.Collections;
import java.util.List;
import service.MovieService;
import service.ShowtimeService;
import service.TheaterService;

public class Reservation {
    private int uid;
    private int memberUid;
    private int movieUid; 
    private int theaterUid; 
    private String time; 
    private String seatNo; 
    private String status;
    private int numTickets;
    private int showtimeUid; // 新增場次 UID

    // 更新建構函數，新增 showtimeUid 參數
    public Reservation(int uid, int memberUid, int movieUid, int theaterUid, String time, String seatNo, String status, int numTickets) {
        this.uid = uid;
        this.memberUid = memberUid;
        this.movieUid = movieUid;
        this.theaterUid = theaterUid;
        this.time = time;
        this.seatNo = seatNo;
        this.status = status;
        this.numTickets = numTickets;
        // 將場次 UID 設為 theaterUid 欄位值
        this.showtimeUid = theaterUid;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getMemberUid() {
        return memberUid;
    }

    public void setMemberUid(int memberUid) {
        this.memberUid = memberUid;
    }

    public int getMovieUid() {
        return movieUid;
    }

    public void setMovieUid(int movieUid) {
        this.movieUid = movieUid;
    }

    public int getTheaterUid() {
        return theaterUid;
    }

    public void setTheaterUid(int theaterUid) {
        this.theaterUid = theaterUid;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getSeatNo() {
        return seatNo;
    }

    public void setSeatNo(String seatNo) {
        this.seatNo = seatNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getNumTickets() {
        return numTickets;
    }

    public void setNumTickets(int numTickets) {
        this.numTickets = numTickets;
    }

    /**
     * Returns the showtime associated with this reservation.
     */
    public Showtime getShowtime() {
        return new ShowtimeService().getShowtimeById(getShowtimeUid());
    }

    /**
     * Returns the list of seat numbers for this reservation.
     */
    public List<String> getSeatNumbers() {
        return Collections.singletonList(getSeatNo());
    }

    /**
     * Returns the showtime UID for this reservation.
     */
    public int getShowtimeUid() {
        // 修正：返回真正的場次ID而非影廳ID
        return showtimeUid; 
    }

    /**
     * 設置場次 UID
     */
    public void setShowtimeUid(int showtimeUid) {
        this.showtimeUid = showtimeUid;
    }

    /**
     * 取得關聯的電影
     */
    public Movie getMovie() {
        try {
            MovieService movieService = new MovieService();
            return movieService.getMovieById(movieUid).orElse(null);
        } catch (Exception e) {
            System.err.println("無法獲取電影資訊，電影ID: " + movieUid + "，錯誤: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("Reservation [ID: %d, MemberID: %d, MovieID: %d, TheaterID: %d, Time: %s, Seat: %s, Status: %s, NumTickets: %d]",
                uid, memberUid, movieUid, theaterUid, time, seatNo, status, numTickets);
    }
}