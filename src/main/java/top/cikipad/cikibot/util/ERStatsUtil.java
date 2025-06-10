package top.cikipad.cikibot.util;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import lombok.Data;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ERStatsUtil {
    private static final String API_BASE_URL = "https://er.dakgg.io/api/v1/players/%s/profile";
    
    @Data
    public static class ERStats {
        private String playerName;
        private int totalGames;
        private int wins;
        private int top2;
        private int top3;
        private int totalKills;
        private int totalAssists;
        private int totalDeaths;
        private double kda;
        private double winRate;
        private double top3Rate;
        private int mmr;
        private String tier;
        private String tierGrade;
    }

    public static ERStats getPlayerStats(String playerName) {
        String apiUrl = String.format(API_BASE_URL, playerName);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(apiUrl);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                return parseStats(jsonResponse, playerName);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch ER stats for player: " + playerName, e);
        }
    }

    private static ERStats parseStats(String jsonResponse, String playerName) {
        JSONObject json = JSONObject.parseObject(jsonResponse);
        ERStats stats = new ERStats();
        stats.setPlayerName(playerName);
        
        // 获取当前赛季数据
        List<Map<String, Object>> playerSeasons = JSONObject.parseObject(
            json.getJSONArray("playerSeasons").toJSONString(),
            new TypeReference<List<Map<String, Object>>>() {}
        );
        
        Map<String, Object> currentSeason = playerSeasons.stream()
            .filter(season -> season.get("seasonId") != null && (Integer)season.get("seasonId") > 0)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No valid season data found"));

        // 获取赛季概览数据
        List<Map<String, Object>> seasonOverviews = JSONObject.parseObject(
            json.getJSONArray("playerSeasonOverviews").toJSONString(),
            new TypeReference<List<Map<String, Object>>>() {}
        );
        
        Map<String, Object> currentOverview = seasonOverviews.stream()
            .filter(overview -> overview.get("seasonId").equals(currentSeason.get("seasonId")))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No valid season overview found"));

        // 设置基础数据
        stats.setTotalGames((Integer) currentOverview.get("play"));
        stats.setWins((Integer) currentOverview.get("win"));
        stats.setTop2((Integer) currentOverview.get("top2"));
        stats.setTop3((Integer) currentOverview.get("top3"));
        stats.setTotalKills((Integer) currentOverview.get("playerKill"));
        stats.setTotalAssists((Integer) currentOverview.get("playerAssistant"));
        stats.setTotalDeaths((Integer) currentOverview.get("playerDeaths"));
        stats.setMmr((Integer) currentSeason.get("mmr"));

        // 计算比率
        stats.setWinRate(stats.getTotalGames() > 0 ? (double) stats.getWins() / stats.getTotalGames() * 100 : 0);
        stats.setTop3Rate(stats.getTotalGames() > 0 ? (double) stats.getTop3() / stats.getTotalGames() * 100 : 0);
        stats.setKda(stats.getTotalDeaths() > 0 ? 
            (double) (stats.getTotalKills() + stats.getTotalAssists()) / stats.getTotalDeaths() : 
            stats.getTotalKills() + stats.getTotalAssists());

        // 设置段位信息
        stats.setTier(getTierName((Integer) currentSeason.get("tierId")));
        stats.setTierGrade(getTierGrade((Integer) currentSeason.get("tierGradeId")));

        return stats;
    }

    public static String generateStatsReport(String playerName) {
        ERStats stats = getPlayerStats(playerName);
        return String.format("""
            🎮 ER战绩报告 - %s
            ====================
            
            📊 基础数据
            • 总场次: %d
            • 胜场: %d (%.1f%%)
            • 前三: %d (%.1f%%)
            • 击杀: %d
            • 助攻: %d
            • 死亡: %d
            • KDA: %.2f
            
            🏆 段位信息
            • 当前段位: %s %s
            • 段位分: %d
            
            ====================
            """,
            stats.getPlayerName(),
            stats.getTotalGames(),
            stats.getWins(),
            stats.getWinRate(),
            stats.getTop3(),
            stats.getTop3Rate(),
            stats.getTotalKills(),
            stats.getTotalAssists(),
            stats.getTotalDeaths(),
            stats.getKda(),
            stats.getTier(),
            stats.getTierGrade(),
            stats.getMmr()
        );
    }

    private static String getTierName(int tierId) {
        Map<String, Object> tierData = ERDataManager.getInstance().findById("tiers", tierId);
        return tierData != null ? (String) tierData.get("name") : "未知";
    }

    private static String getTierGrade(int gradeId) {
        return switch (gradeId) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> "";
        };
    }
} 