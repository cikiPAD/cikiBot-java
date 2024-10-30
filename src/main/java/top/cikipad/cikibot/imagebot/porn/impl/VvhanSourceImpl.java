package top.cikipad.cikibot.imagebot.porn.impl;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import top.cikipad.cikibot.imagebot.porn.constant.SourceTypeConstant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class VvhanSourceImpl extends SimpleSourceImpl {
    @Override
    String getUrl() {
        return "https://api.vvhan.com/api/mobil.girl";
    }

    @Override
    List<String> getImageUrlsFromResp(String resp) {

        if (resp == null) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        Map map = gson.fromJson(resp, Map.class);
        String img = (String) map.get("imgurl");

        List<String> ret = new ArrayList<>();

        ret.add(img);
        return ret;
    }

    @Override
    public String getType() {
        return SourceTypeConstant.VVHAN;
    }

    @Override
    public Map<String, Object> standardParams(Map<String, Object> params) {
        params.put("type", "json");
        return params;
    }
}
