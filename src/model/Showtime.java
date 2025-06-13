package model;

import service.MovieService;
import service.TheaterService;

public class Showtime {
    private int uid;
    private int movieUid;
    private int theaterUid;
    private String startTime; // 新增開始時間
    private String endTime;   // 新增結束時間
    private int availableSeats; // 可用座位數

    // Constructor for reading from DB
    public Showtime(int uid, int movieUid, int theaterUid,
                    String startTime, String endTime, int availableSeats) {
        this.uid = uid;
        this.movieUid = movieUid;
        this.theaterUid = theaterUid;
        this.startTime = startTime;
        this.endTime = endTime;
        this.availableSeats = availableSeats;
    }

    // Constructor for creating new showtime (availableSeats will be set by DAO)
    public Showtime(int uid, int movieUid, int theaterUid,
                    String startTime, String endTime) {
        this(uid, movieUid, theaterUid, startTime, endTime, 0);
    }

    public int getUid() {
        return uid;
    }

    public int getMovieUid() {
        return movieUid;
    }

    public int getTheaterUid() {
        return theaterUid;
    }

    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }

    public int getAvailableSeats() { return availableSeats; }

    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    /**
     * Returns the Movie object associated with this showtime.
     */
    public Movie getMovie() {
        return new MovieService().getMovieById(movieUid).orElse(null);
    }

    /**
     * Returns the Theater object associated with this showtime.
     */
    public Theater getTheater() {
        return new TheaterService().getTheaterById(theaterUid).orElse(null);
    }

    /**
     * Returns the show time string.
     */
    public String getShowTime() {
        return getStartTime() + " - " + getEndTime();
    }

    /**
     * Returns the theater name associated with this showtime.
     * @return theater name as string
     */
    public String getTheaterName() {
        Theater theater = getTheater();
        return theater != null ? theater.getType() : "未知影廳";
    }

    @Override
    public String toString() {
        return String.format("Showtime [ID: %d, MovieID: %d, TheaterID: %d, Start Time: %s, End Time: %s, Available Seats: %d]",
                             uid, movieUid, theaterUid, startTime, endTime, availableSeats);
    }
}