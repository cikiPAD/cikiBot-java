package top.cikipad.cikibot.util;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;


@Slf4j
public class ImagePatchNotesUtils {

    private static final Pattern ILLEGAL_CHARS = Pattern.compile("[\\\\/:*?\"<>|\0\\s]");

    private static final String PATCH_NOTES_URL = "https://playeternalreturn.com/api/v1/posts/news?category=patchnote&page=1";
    private static final int MAX_PATCH_NOTES = 5;
    public static List<PatchNote> getLatestPatchNotesUrls() {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(PATCH_NOTES_URL);
            // 添加Accept-Language头，请求中文内容
            request.setHeader("Accept-Language", "zh-CN,zh;q=0.9");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                log.debug("API Response: {}", jsonResponse);

                List<PatchNote> urls = parsePatchNotesUrls(jsonResponse);

                return urls;
            }
        } catch (IOException e) {
            log.error("Failed to fetch patch notes", e);
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Unexpected error while generating patch notes report", e);
            return new ArrayList<>();
        }
    }


    private static List<PatchNote> parsePatchNotesUrls(String jsonResponse) {
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



            return patchNotes;

        } catch (Exception e) {
            log.error("Error parsing patch notes", e);
            return null;
        }
    }


    public static List<PatchLocalEntity> captureScreenshots(List<PatchNote> patchNotes, String screenshotServiceUrl) {
        List<PatchLocalEntity> imagePaths = new ArrayList<>();
        HttpClient client = HttpClient.newHttpClient();

        Path directory = Paths.get("patch");

        File dic = directory.toFile();

        if (!dic.exists()) {
            dic.mkdir();
        }



        for (PatchNote note : patchNotes) {

            Path notePath = getNotePath(note);

            File file = notePath.toFile();

            if (file.isFile() && file.exists()) {
                PatchLocalEntity entity = new PatchLocalEntity();
                entity.setTitle(note.getTitle());
                entity.setPath(notePath.toAbsolutePath().toString());

                // 6. 添加到返回列表
                imagePaths.add(entity);

                continue;
            }


            try {
                // 1. 构建JSON请求体
                String jsonBody = String.format(
                        "{\"url\": \"%s\", \"full_page\": 1}",
                        note.getUrl() // 假设PatchNote类有getUrl()方法
                );

                // 2. 创建HTTP请求
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(screenshotServiceUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                // 3. 发送请求并获取响应
                HttpResponse<byte[]> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.ofByteArray()
                );

                // 4. 检查响应状态
                if (response.statusCode() == 200) {
                    // 5. 生成唯一文件名并保存到当前目录
                    Files.write(notePath, response.body());

                    PatchLocalEntity entity = new PatchLocalEntity();
                    entity.setTitle(note.getTitle());
                    entity.setPath(notePath.toAbsolutePath().toString());

                    // 6. 添加到返回列表
                    imagePaths.add(entity);

                } else {
                    log.error("截图失败 (URL: " + note.getUrl() +
                            "), 状态码: " + response.statusCode());
                }

            } catch (Exception e) {
                log.error("处理URL出错: " + note.getUrl());
                log.error("", e);
            }



        }
        return imagePaths;
    }


    public static Path getNotePath(PatchNote note) {

        Path directory = Paths.get("patch");

        String fileName = "screenshot-" + sanitizeFilename(note.getTitle()) + ".png";

        return directory.resolve(fileName);
    }


    private static String sanitizeFilename(String input) {
        if (input == null) return "untitled";

        // 移除非法字符
        String sanitized = ILLEGAL_CHARS.matcher(input).replaceAll("");

        // 处理Unicode字符（兼容macOS/Windows）
        sanitized = Normalizer.normalize(sanitized, Normalizer.Form.NFC);

        // 缩短过长文件名
        int maxLength = 50; // 防止路径过长
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength) + "_";
        }

        // 防止文件名全是空白
        if (sanitized.trim().isEmpty()) {
            sanitized = "untitled";
        }

        return sanitized;
    }

    public static void main(String[] args) {
        captureScreenshots(getLatestPatchNotesUrls(), "http://193.200.130.187:14140/screenshot");
        System.out.println("success");
    }


}
