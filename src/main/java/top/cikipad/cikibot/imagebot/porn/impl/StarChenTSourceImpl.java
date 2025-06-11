package top.cikipad.cikibot.imagebot.porn.impl;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import top.cikipad.cikibot.imagebot.porn.constant.SourceTypeConstant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class StarChenTSourceImpl extends SimpleSourceImpl {
    @Override
    String getUrl() {
        return "http://api.starchent.top/API/meizi.php";
    }

    @Override
    List<String> getImageUrlsFromResp(String resp) {

        if (resp == null) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        Map map = gson.fromJson(resp, Map.class);
        String img = (String) map.get("text");

        List<String> ret = new ArrayList<>();

        ret.add(img);
        return ret;
    }

    @Override
    public String getType() {
        return SourceTypeConstant.STARCHENT;
    }

    @Override
    public Map<String, Object> standardParams(Map<String, Object> params) {
        params.put("type", "json");
        return params;
    }
}
