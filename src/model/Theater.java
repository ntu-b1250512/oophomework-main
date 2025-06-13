package model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class Theater {
    private int uid;
    private String type; // e.g., "大廳", "小廳"
    private int totalSeats;
    private List<Screening> screenings = new ArrayList<>();

    public Theater(int uid, String type, int totalSeats) {
        this.uid = uid;
        this.type = type;
        this.totalSeats = totalSeats;
    }

    public int getUid() {
        return uid;
    }

    public String getType() {
        return type;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public static class Screening {
        private LocalDateTime start;
        private LocalDateTime end;

        public Screening(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }
        public LocalDateTime getStart() { return start; }
        public LocalDateTime getEnd() { return end; }
        @Override
        public String toString() {
            return "[" + start + " → " + end + "]";
        }
    }

    public void addScreening(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException("開始時間須早於結束時間");
        }
        for (Screening s : screenings) {
            if (start.isBefore(s.getEnd()) && end.isAfter(s.getStart())) {
                throw new IllegalArgumentException(
                    "時段衝突: 新場次 " + new Screening(start,end) + " 與既有場次 " + s + " 衝突"
                );
            }
        }
        screenings.add(new Screening(start, end));
    }

    public List<Screening> getScreenings() {
        return new ArrayList<>(screenings);
    }

    @Override
    public String toString() {
        return "Theater [uid=" + uid + ", type=" + type + ", totalSeats=" + totalSeats + "]";
    }
}
