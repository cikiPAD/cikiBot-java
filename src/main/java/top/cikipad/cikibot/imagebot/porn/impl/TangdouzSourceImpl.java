package top.cikipad.cikibot.imagebot.porn.impl;


import lombok.extern.slf4j.Slf4j;
import top.cikipad.cikibot.imagebot.porn.constant.ParamsConstant;
import top.cikipad.cikibot.imagebot.porn.constant.SourceTypeConstant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class TangdouzSourceImpl extends SimpleSourceImpl {
    @Override
    String getUrl() {
        return "https://api.tangdouz.com/hlxmt.php";
    }

    @Override
    List<String> getImageUrlsFromResp(String resp) {

        if (resp == null) {
            return new ArrayList<>();
        }
        List<String> ret = new ArrayList<>();
        String[] arr = resp.split("±");
        for (String tmp:arr) {
            if(tmp.contains("img=")) {
                ret.add(tmp.replace("img=", ""));
            }
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error("",e);
        }

        return ret;
    }

    @Override
    public String getType() {
        return SourceTypeConstant.TDZ;
    }

    @Override
    public Map<String, Object> standardParams(Map<String, Object> params) {
        params.remove(ParamsConstant.NUM);
        return params;
    }
}
