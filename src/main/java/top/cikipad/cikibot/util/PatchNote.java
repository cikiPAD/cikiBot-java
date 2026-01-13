package top.cikipad.cikibot.util;

import lombok.Data;

@Data
public class PatchNote {
    private String title;
    private String content;
    private String createdAt;
    private String url;
}