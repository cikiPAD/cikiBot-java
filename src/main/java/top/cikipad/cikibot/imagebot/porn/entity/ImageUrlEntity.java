package top.cikipad.cikibot.imagebot.porn.entity;

import java.util.List;

public class ImageUrlEntity {
    private String displayString;
    private List<String> urls;
    private String source;

    public String getDisplayString() {
        return displayString;
    }

    public void setDisplayString(String displayString) {
        this.displayString = displayString;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "ImageUrlEntity{" +
                "displayString='" + displayString + '\'' +
                ", urls=" + urls +
                ", source='" + source + '\'' +
                '}';
    }
}
