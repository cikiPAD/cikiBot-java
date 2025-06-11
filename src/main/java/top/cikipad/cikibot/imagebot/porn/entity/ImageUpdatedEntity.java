package top.cikipad.cikibot.imagebot.porn.entity;

import java.util.List;

public class ImageUpdatedEntity {
    ImageUrlEntity urls;
    List<Object> images;

    public ImageUrlEntity getUrls() {
        return urls;
    }

    public void setUrls(ImageUrlEntity urls) {
        this.urls = urls;
    }

    public List<Object> getImages() {
        return images;
    }

    public void setImages(List<Object> images) {
        this.images = images;
    }

    @Override
    public String toString() {
        return "ImageUpdatedEntity{" +
                "urls=" + urls +
                ", images=" + images +
                '}';
    }
}
