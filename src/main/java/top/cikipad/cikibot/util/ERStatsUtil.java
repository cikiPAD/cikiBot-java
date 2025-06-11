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
        private int damageToPlayer;
        private int damageToMonster;
        private int monsterKills;
        private int teamKills;
        private int viewContribution;
        private long playTime;
        private List<CharacterStats> characterStats;
    }

    @Data
    public static class CharacterStats {
        private int characterId;
        private int games;
        private int wins;
        private int top2;
        private int top3;
        private int kills;
        private int assists;
        private int deaths;
        private double kda;
        private double winRate;
        private int damageToPlayer;
        private int damageToMonster;
        private int monsterKills;
        private int teamKills;
        private int viewContribution;
        private long playTime;
    }

    @Data
    public static class MatchHistory {
        private String gameId;
        private String characterName;
        private int gameRank;
        private int playerKill;
        private int playerAssistant;
        private int playerDeaths;
        private double kda;
        private int damageToPlayer;
        private int damageToMonster;
        private int monsterKill;
        private int teamKill;
        private int viewContribution;
        private long duration;
        private String startTime;
        private String matchType;
        private int mmrChange;
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
        stats.setTotalGames(Integer.valueOf(currentOverview.get("play").toString()));
        stats.setWins(Integer.valueOf(currentOverview.get("win").toString()));
        stats.setTop2(Integer.valueOf(currentOverview.get("top2").toString()));
        stats.setTop3(Integer.valueOf(currentOverview.get("top3").toString()));
        stats.setTotalKills(Integer.valueOf(currentOverview.get("playerKill").toString()));
        stats.setTotalAssists(Integer.valueOf(currentOverview.get("playerAssistant").toString()));
        stats.setTotalDeaths(Integer.valueOf(currentOverview.get("playerDeaths").toString()));
        stats.setMmr(Integer.valueOf(currentSeason.get("mmr").toString()));
        stats.setDamageToPlayer(Integer.valueOf(currentOverview.get("damageToPlayer").toString()));
        stats.setDamageToMonster(Integer.valueOf(currentOverview.get("damageToMonster").toString()));
        stats.setMonsterKills(Integer.valueOf(currentOverview.get("monsterKill").toString()));
        stats.setTeamKills(Integer.valueOf(currentOverview.get("teamKill").toString()));
        stats.setViewContribution(Integer.valueOf(currentOverview.get("viewContribution").toString()));
        stats.setPlayTime(Long.valueOf(currentOverview.get("playTime").toString()));

        // 计算比率
        stats.setWinRate(stats.getTotalGames() > 0 ? (double) stats.getWins() / stats.getTotalGames() * 100 : 0);
        stats.setTop3Rate(stats.getTotalGames() > 0 ? (double) stats.getTop3() / stats.getTotalGames() * 100 : 0);
        stats.setKda(stats.getTotalDeaths() > 0 ? 
            (double) (stats.getTotalKills() + stats.getTotalAssists()) / stats.getTotalDeaths() : 
            stats.getTotalKills() + stats.getTotalAssists());

        // 设置段位信息
        stats.setTier(getTierName((Integer) currentSeason.get("tierId")));
        stats.setTierGrade(getTierGrade((Integer) currentSeason.get("tierGradeId")));

        // 解析角色数据
        List<Map<String, Object>> characterStatsList = JSONObject.parseObject(
            currentOverview.get("characterStats").toString(),
            new TypeReference<List<Map<String, Object>>>() {}
        );

        stats.setCharacterStats(characterStatsList.stream().map(charStat -> {
            CharacterStats cs = new CharacterStats();
            cs.setCharacterId(Integer.valueOf(charStat.get("key").toString()));
            cs.setGames(Integer.valueOf(charStat.get("play").toString()));
            cs.setWins(Integer.valueOf(charStat.get("win").toString()));
            cs.setTop2(Integer.valueOf(charStat.get("top2").toString()));
            cs.setTop3(Integer.valueOf(charStat.get("top3").toString()));
            cs.setKills(Integer.valueOf(charStat.get("playerKill").toString()));
            cs.setAssists(Integer.valueOf(charStat.get("playerAssistant").toString()));
            cs.setDeaths(Integer.valueOf(charStat.get("playerDeaths").toString()));
            cs.setDamageToPlayer(Integer.valueOf(charStat.get("damageToPlayer").toString()));
            cs.setDamageToMonster(Integer.valueOf(charStat.get("damageToMonster").toString()));
            cs.setMonsterKills(Integer.valueOf(charStat.get("monsterKill").toString()));
            cs.setTeamKills(Integer.valueOf(charStat.get("teamKill").toString()));
            cs.setViewContribution(Integer.valueOf(charStat.get("viewContribution").toString()));
            cs.setPlayTime(Long.valueOf(charStat.get("playTime").toString()));
            
            // 计算角色KDA和胜率
            cs.setKda(cs.getDeaths() > 0 ? 
                (double) (cs.getKills() + cs.getAssists()) / cs.getDeaths() : 
                cs.getKills() + cs.getAssists());
            cs.setWinRate(cs.getGames() > 0 ? (double) cs.getWins() / cs.getGames() * 100 : 0);
            
            return cs;
        }).toList());

        return stats;
    }

    public static String generateStatsReport(String playerName) {
        ERStats stats = getPlayerStats(playerName);
        StringBuilder report = new StringBuilder();
        
        // 基本信息
        report.append(String.format("""
            🎮 ER战绩报告 - %s
            ====================
            
            📊 基础数据
            • 总场次: %d
            • 胜场: %d (%.1f%%)
            • 前三: %d (%.1f%%)
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
            stats.getKda(),
            stats.getTier(),
            stats.getTierGrade(),
            stats.getMmr()
        ));

        // 角色数据
        if (!stats.getCharacterStats().isEmpty()) {
            report.append("\n🎭 角色数据 (前三)\n");
            report.append("====================\n");
            
            stats.getCharacterStats().stream()
                .sorted((cs1, cs2) -> Integer.compare(cs2.getGames(), cs1.getGames()))
                .limit(3)
                .forEach(cs -> {
                    String characterName = getCharacterName(cs.getCharacterId());
                    report.append(String.format("""
                        %s
                        • 场次: %d
                        • 胜场: %d (%.1f%%)
                        • 前三: %d (%.1f%%)
                        • KDA: %.2f
                        
                        """,
                        characterName,
                        cs.getGames(),
                        cs.getWins(),
                        cs.getWinRate(),
                        cs.getTop3(),
                        cs.getGames() > 0 ? (double) cs.getTop3() / cs.getGames() * 100 : 0,
                        cs.getKda()
                    ));
                });
        }

        return report.toString();
    }

    public static String generateRecentMatchesReport(String playerName) {
        String apiUrl = String.format("https://er.dakgg.io/api/v1/players/%s/matches?matchingMode=ALL&teamMode=ALL&page=1", playerName);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(apiUrl);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                return parseRecentMatches(jsonResponse, playerName);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch recent matches for player: " + playerName, e);
        }
    }

    private static String parseRecentMatches(String jsonResponse, String playerName) {
        JSONObject json = JSONObject.parseObject(jsonResponse);
        List<Map<String, Object>> matches = JSONObject.parseObject(
            json.getJSONArray("matches").toJSONString(),
            new TypeReference<List<Map<String, Object>>>() {}
        );

        StringBuilder report = new StringBuilder();
        report.append(String.format("""
            🎮 ER近期战绩 - %s (最近5场)
            ====================
            
            """, playerName));

        // 只取最近5场比赛
        matches.stream()
            .limit(5)
            .forEach(match -> {
                int characterId = Integer.valueOf(match.get("characterNum").toString());
                String characterName = getCharacterName(characterId);
                int rank = Integer.valueOf(match.get("gameRank").toString());
                int kills = Integer.valueOf(match.get("playerKill").toString());
                int assists = Integer.valueOf(match.get("playerAssistant").toString());
                int deaths = Integer.valueOf(match.get("playerDeaths").toString());
                double kda = deaths > 0 ? (double) (kills + assists) / deaths : kills + assists;
                int damageToPlayer = Integer.valueOf(match.get("damageToPlayer").toString());
                int damageToMonster = Integer.valueOf(match.get("damageToMonster").toString());
                int monsterKills = Integer.valueOf(match.get("monsterKill").toString());
                int teamKills = Integer.valueOf(match.get("teamKill").toString());
                int viewContribution = Integer.valueOf(match.get("viewContribution").toString());
                long duration = Long.valueOf(match.get("duration").toString());
                String startTime = match.get("startDtm").toString();
                int matchingMode = Integer.valueOf(match.get("matchingMode").toString());
                String matchType = getMatchType(matchingMode);
                int mmrChange = 0;
                if (matchingMode == 3) {
                    mmrChange = Integer.valueOf(match.get("mmrGain").toString());
                }

                report.append(String.format("""
                    🎯 %s [%s]
                    • 排名: %d
                    • KDA: %.2f (%d/%d/%d)
                    • 对玩家伤害: %,d
                    • 对怪物伤害: %,d
                    • 怪物击杀: %d
                    • 团队击杀: %d
                    • 视野贡献: %d
                    • 游戏时长: %d分钟
                    • 开始时间: %s
                    %s
                    
                    """,
                    characterName,
                    matchType,
                    rank,
                    kda,
                    kills,
                    deaths,
                    assists,
                    damageToPlayer,
                    damageToMonster,
                    monsterKills,
                    teamKills,
                    viewContribution,
                    duration / 60,
                    startTime,
                    matchingMode == 3 ? String.format("• 排位得分: %+d", mmrChange) : ""
                ));
            });

        return report.toString();
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

    private static String getCharacterName(int characterId) {
        Map<String, Object> characterData = ERDataManager.getInstance().findById("characters", characterId);
        return characterData != null ? (String) characterData.get("name") : "未知角色";
    }

    private static String getMatchType(int matchingMode) {
        return switch (matchingMode) {
            case 2 -> "普通";
            case 3 -> "排位";
            case 6 -> "钴协议";
            default -> "未知";
        };
    }
} 