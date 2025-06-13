package util;

import model.Movie;
import model.Theater;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具類別用於從JSON檔案載入電影和影廳資訊
 */
public class JsonDataLoader {

    /**
     * 從JSON檔案載入電影資訊
     * @param filePath JSON檔案路徑
     * @return 電影列表
     */
    public static List<Movie> loadMoviesFromJson(String filePath) {
        List<Movie> movies = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line.trim());
            }
            
            // 非常簡單的解析，不處理複雜JSON格式
            String content = jsonContent.toString();
            // 移除開頭的 [ 和結尾的 ]
            content = content.substring(1, content.length() - 1);
            
            // 依據 },{ 分割成電影資料
            String[] movieJsons = content.split("\\},\\{");
            
            for (int i = 0; i < movieJsons.length; i++) {
                String movieJson = movieJsons[i];
                // 處理第一個和最後一個特殊情況
                if (i == 0) {
                    movieJson = movieJson + "}";
                } else if (i == movieJsons.length - 1) {
                    movieJson = "{" + movieJson;
                } else {
                    movieJson = "{" + movieJson + "}";
                }
                
                // 解析電影資訊
                String title = extractJsonValue(movieJson, "title_zh");
                int duration = Integer.parseInt(extractJsonValue(movieJson, "length"));
                String summary = extractJsonValue(movieJson, "summary");
                String classification = extractJsonValue(movieJson, "classification");
                
                // 建立電影物件
                Movie movie = new Movie(0, title, duration, summary, classification);
                movies.add(movie);
            }
        } catch (IOException e) {
            System.err.println("Error loading movies from JSON: " + e.getMessage());
            e.printStackTrace();
        }

        return movies;
    }

    /**
     * 從JSON檔案載入大廳和小廳的座位資訊
     * @param bigRoomPath 大廳JSON檔案路徑
     * @param smallRoomPath 小廳JSON檔案路徑
     * @return 映射包含大廳和小廳的座位資訊
     */
    public static Map<String, List<Map<String, Object>>> loadTheaterSeatsFromJson(String bigRoomPath, String smallRoomPath) {
        Map<String, List<Map<String, Object>>> theaters = new HashMap<>();
        
        // 載入大廳座位
        List<Map<String, Object>> bigRoomSeats = loadSeatsFromFile(bigRoomPath, true);
        theaters.put("大廳", bigRoomSeats);
        
        // 載入小廳座位
        List<Map<String, Object>> smallRoomSeats = loadSeatsFromFile(smallRoomPath, false);
        theaters.put("小廳", smallRoomSeats);
        
        return theaters;
    }
    
    /**
     * 從檔案載入座位資訊
     * @param filePath 檔案路徑
     * @param hasRegions 是否包含區域資訊
     * @return 座位資訊列表
     */
    private static List<Map<String, Object>> loadSeatsFromFile(String filePath, boolean hasRegions) {
        List<Map<String, Object>> seats = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line.trim());
            }
            
            // 簡單解析
            String content = jsonContent.toString();
            // 移除開頭的 [ 和結尾的 ]
            content = content.substring(1, content.length() - 1);
            
            // 依據 },{ 分割成座位資料
            String[] seatJsons = content.split("\\},\\{");
            
            for (int i = 0; i < seatJsons.length; i++) {
                String seatJson = seatJsons[i];
                // 處理第一個和最後一個特殊情況
                if (i == 0) {
                    seatJson = seatJson + "}";
                } else if (i == seatJsons.length - 1) {
                    seatJson = "{" + seatJson;
                } else {
                    seatJson = "{" + seatJson + "}";
                }
                
                Map<String, Object> seat = new HashMap<>();
                seat.put("row", extractJsonValue(seatJson, "row"));
                seat.put("seatNum", Integer.parseInt(extractJsonValue(seatJson, "seatNum")));
                
                if (hasRegions) {
                    seat.put("region", extractJsonValue(seatJson, "region"));
                } else {
                    seat.put("region", "standard"); // 小廳沒有區域區分，使用標準區域
                }
                
                seats.add(seat);
            }
        } catch (IOException e) {
            System.err.println("Error loading seats from JSON: " + e.getMessage());
            e.printStackTrace();
        }
        
        return seats;
    }

    /**
     * 從JSON字串中提取值
     * @param json JSON字串
     * @param key 鍵值
     * @return 對應的值
     */
    private static String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return "";
        
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return "";
        
        int valueStart = json.indexOf("\"", colonIndex);
        if (valueStart == -1) { 
            // 可能是數值
            int commaIndex = json.indexOf(",", colonIndex);
            int closeBracketIndex = json.indexOf("}", colonIndex);
            int endIndex = (commaIndex != -1 && commaIndex < closeBracketIndex) ? commaIndex : closeBracketIndex;
            return json.substring(colonIndex + 1, endIndex).trim();
        }
        
        valueStart++; // 跳過引號
        int valueEnd = json.indexOf("\"", valueStart);
        if (valueEnd == -1) return "";
        
        return json.substring(valueStart, valueEnd);
    }

    /**
     * 取得每個影廳的總座位數
     * @param theatersData 影廳座位資料
     * @return 映射包含每個影廳的總座位數
     */
    public static Map<String, Integer> getTheaterSeatCounts(Map<String, List<Map<String, Object>>> theatersData) {
        Map<String, Integer> seatCounts = new HashMap<>();
        
        for (Map.Entry<String, List<Map<String, Object>>> entry : theatersData.entrySet()) {
            seatCounts.put(entry.getKey(), entry.getValue().size());
        }
        
        return seatCounts;
    }
}