package service;

import dao.MovieDAO;
import dao.TheaterDAO;
import model.Movie;
import model.Theater;
import util.JsonDataLoader;

import java.util.List;
import java.util.Map;

/**
 * 處理從JSON檔案導入資料到資料庫
 */
public class DataImportService {
    private final MovieDAO movieDAO;
    private final TheaterDAO theaterDAO;

    public DataImportService() {
        this.movieDAO = new MovieDAO();
        this.theaterDAO = new TheaterDAO();
    }

    /**
     * 從JSON檔案導入電影資訊
     * @param jsonFilePath 電影JSON檔案路徑
     * @return 成功導入的電影數量
     */
    public int importMoviesFromJson(String jsonFilePath) {
        List<Movie> movies = JsonDataLoader.loadMoviesFromJson(jsonFilePath);
        int importedCount = 0;

        for (Movie movie : movies) {
            // 檢查電影是否已存在
            if (!movieDAO.existsByName(movie.getName())) {
                int uid = movieDAO.addMovie(movie);
                if (uid > 0) {
                    importedCount++;
                    System.out.println("導入電影: " + movie.getName());
                }
            } else {
                System.out.println("電影已存在: " + movie.getName());
            }
        }

        return importedCount;
    }

    /**
     * 從JSON檔案導入影廳座位資訊
     * @param bigRoomPath 大廳JSON檔案路徑
     * @param smallRoomPath 小廳JSON檔案路徑
     * @return 成功導入的影廳數量
     */
    public int importTheatersFromJson(String bigRoomPath, String smallRoomPath) {
        Map<String, List<Map<String, Object>>> theaterData = 
            JsonDataLoader.loadTheaterSeatsFromJson(bigRoomPath, smallRoomPath);
        Map<String, Integer> seatCounts = JsonDataLoader.getTheaterSeatCounts(theaterData);
        
        int importedCount = 0;
        
        // 導入大廳
        String bigRoomType = "大廳";
        if (seatCounts.containsKey(bigRoomType)) {
            int totalSeats = seatCounts.get(bigRoomType);
            // 檢查影廳是否已存在
            Theater existingBigRoom = theaterDAO.getTheaterByType(bigRoomType);
            
            if (existingBigRoom == null) {
                // 新增影廳
                Theater bigRoom = new Theater(0, bigRoomType, totalSeats);
                int uid = theaterDAO.addTheater(bigRoom);
                if (uid > 0) {
                    importedCount++;
                    System.out.println("導入影廳: " + bigRoomType + ", 座位數: " + totalSeats);
                }
            } else {
                // 更新現有影廳的座位數
                Theater updatedBigRoom = new Theater(
                    existingBigRoom.getUid(), 
                    existingBigRoom.getType(), 
                    totalSeats
                );
                if (theaterDAO.updateTheater(updatedBigRoom)) {
                    importedCount++;
                    System.out.println("更新影廳: " + bigRoomType + ", 座位數: " + totalSeats);
                }
            }
        }
        
        // 導入小廳
        String smallRoomType = "小廳";
        if (seatCounts.containsKey(smallRoomType)) {
            int totalSeats = seatCounts.get(smallRoomType);
            // 檢查影廳是否已存在
            Theater existingSmallRoom = theaterDAO.getTheaterByType(smallRoomType);
            
            if (existingSmallRoom == null) {
                // 新增影廳
                Theater smallRoom = new Theater(0, smallRoomType, totalSeats);
                int uid = theaterDAO.addTheater(smallRoom);
                if (uid > 0) {
                    importedCount++;
                    System.out.println("導入影廳: " + smallRoomType + ", 座位數: " + totalSeats);
                }
            } else {
                // 更新現有影廳的座位數
                Theater updatedSmallRoom = new Theater(
                    existingSmallRoom.getUid(), 
                    existingSmallRoom.getType(), 
                    totalSeats
                );
                if (theaterDAO.updateTheater(updatedSmallRoom)) {
                    importedCount++;
                    System.out.println("更新影廳: " + smallRoomType + ", 座位數: " + totalSeats);
                }
            }
        }
        
        return importedCount;
    }
}