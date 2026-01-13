package top.cikipad.cikibot.imagebot.porn;

import top.cikipad.cikibot.imagebot.porn.entity.ImageUrlEntity;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface ImageSourceInterface {
    List<InputStream> getImageStream(Map<String, Object> params);

    String getType();

    List<String> getImageUrl(Map<String, Object> params);

    default List<ImageUrlEntity> getImageUrlEntity(Map<String, Object> params) {
        List urls = getImageUrl(params);
        if (urls == null) {
            return null;
        }

        if (urls.isEmpty()) {
            return new ArrayList<>();
        }

        List<ImageUrlEntity> entities = new ArrayList<>();
        ImageUrlEntity entity = new ImageUrlEntity();
        entity.setUrls(urls);
        entity.setDisplayString(String.format("图片来源: %s", getType()));
        entities.add(entity);
        return entities;
    }

    Map<String, Object> standardParams(Map<String, Object> params);

    default boolean visible() {
        return true;
    }
}