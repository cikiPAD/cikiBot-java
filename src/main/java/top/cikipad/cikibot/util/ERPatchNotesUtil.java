package top.cikipad.cikibot.util;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class ERPatchNotesUtil {
    private static final String PATCH_NOTES_URL = "https://playeternalreturn.com/api/v1/posts/news?category=patchnote&page=1";
    private static final int MAX_PATCH_NOTES = 5;


    private static class ERPatchNote {
        private final String title;
        private final String url;

        public ERPatchNote(String title, String url) {
            this.title = title;
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }
    }

    public static String generateLatestPatchNotesReport() {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(PATCH_NOTES_URL);
            // 添加Accept-Language头，请求中文内容
            request.setHeader("Accept-Language", "zh-CN,zh;q=0.9");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                log.debug("API Response: {}", jsonResponse);

                List<PatchNote> patchNotes = parsePatchNotes(jsonResponse);
                if (patchNotes != null && !patchNotes.isEmpty()) {
                    return formatPatchNotesReport(patchNotes);
                }
                return "未找到更新日志";
            }
        } catch (IOException e) {
            log.error("Failed to fetch patch notes", e);
            return "获取更新日志失败: " + e.getMessage();
        } catch (Exception e) {
            log.error("Unexpected error while generating patch notes report", e);
            return "生成更新日志报告时发生错误: " + e.getMessage();
        }
    }

    private static List<PatchNote> parsePatchNotes(String jsonResponse) {
        try {
            JSONObject json = JSONObject.parseObject(jsonResponse);
            if (json == null) {
                log.error("Failed to parse JSON response");
                return null;
            }

            // 检查响应中是否包含错误信息
            if (json.containsKey("error")) {
                log.error("API returned error: {}", json.getString("error"));
                return null;
            }

            // 获取articles数组
            List<Map<String, Object>> articles = JSONObject.parseObject(
                json.getJSONArray("articles").toJSONString(),
                new TypeReference<List<Map<String, Object>>>() {}
            );

            if (articles == null || articles.isEmpty()) {
                log.error("No articles found in API response");
                return null;
            }

            List<PatchNote> patchNotes = new ArrayList<>();
            int count = 0;

            // 遍历文章，直到找到主要更新或达到最大数量
            for (Map<String, Object> article : articles) {
                if (count >= MAX_PATCH_NOTES) {
                    break;
                }

                Map<String, Object> i18ns = (Map<String, Object>) article.get("i18ns");
                Map<String, Object> zhCN = (Map<String, Object>) i18ns.get("zh_CN");
                if (zhCN == null) {
                    // 如果没有中文内容，使用英文内容
                    zhCN = (Map<String, Object>) i18ns.get("en_US");
                }

                String title = zhCN.get("title").toString();

                // 如果还没有找到主要更新，或者当前是热修复，则添加到列表
                PatchNote patchNote = new PatchNote();
                patchNote.setTitle(title);
                patchNote.setCreatedAt(zhCN.get("created_at_for_humans").toString());
                patchNote.setUrl(zhCN.get("content_link").toString());
                patchNotes.add(patchNote);
                count++;

                // 如果找到主要更新，标记一下
                if (!title.contains("不停机维护")) {
                    break;
                }

            }

            // 获取所有更新日志的内容
            for (PatchNote patchNote : patchNotes) {
                patchNote.setContent(getPatchNoteContent(patchNote.getUrl()));
            }

            return patchNotes;

        } catch (Exception e) {
            log.error("Error parsing patch notes", e);
            return null;
        }
    }




    private static String getPatchNoteContent(String url) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            // 添加Accept-Language头，请求中文内容
            request.setHeader("Accept-Language", "zh-CN,zh;q=0.9");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String htmlContent = EntityUtils.toString(response.getEntity());
                Document doc = Jsoup.parse(htmlContent);

                StringBuilder content = new StringBuilder();

                // 获取主要内容区域
                Element mainContent = doc.selectFirst(".er-article-detail__content.er-article-content.fr-view");
                if (mainContent != null) {
                    // 处理所有主要部分
                    Elements sections = mainContent.children();
                    for (Element section : sections) {
                        String tagName = section.tagName();

                        if (tagName.startsWith("h")) {
                            // 处理标题
                            content.append("\n").append(section.text()).append("\n");
                        } else if (tagName.equals("ul")) {
                            // 处理列表
                            Elements items = section.children();
                            for (Element item : items) {
                                content.append("• ").append(item.text()).append("\n");
                            }
                        } else if (tagName.equals("p")) {
                            // 处理段落
                            String text = section.text().trim();
                            if (!text.isEmpty()) {
                                content.append(text).append("\n");
                            }
                            else {
                                content.append("\n");
                            }
                        } else if (tagName.equals("div")) {
                            // 处理div中的内容
                            String text = section.text().trim();
                            if (!text.isEmpty()) {
                                content.append(text).append("\n");
                            }
                        }
                    }
                } else {
                    log.error("Could not find main content element");
                    return "无法获取更新内容";
                }

                return content.toString();
            }
        } catch (IOException e) {
            log.error("Failed to fetch patch note content from URL: {}", url, e);
            return "无法获取更新内容: " + e.getMessage();
        }
    }

    private static String formatPatchNotesReport(List<PatchNote> patchNotes) {
        StringBuilder report = new StringBuilder();
        report.append("""
            🎮 ER更新日志
            ====================
            
            """);

        for (PatchNote patchNote : patchNotes) {
            report.append(String.format("""
                📝 %s
                ⏰ %s
                
                %s
                
                --------------------
                
                """,
                patchNote.getTitle(),
                patchNote.getCreatedAt(),
                patchNote.getContent()
            ));
        }

        return report.toString();
    }

}