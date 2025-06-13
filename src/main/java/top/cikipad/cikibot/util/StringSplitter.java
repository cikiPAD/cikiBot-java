package top.cikipad.cikibot.util;

import java.util.ArrayList;
import java.util.List;

public class StringSplitter {

    public static List<String> forceSplit(String input, int threshold) {
        String[] context = input.split("\n");

        List<String> ret = new ArrayList<>();

        StringBuilder sb = new StringBuilder();

        for (String one : context) {
            sb.append(one).append("\n");

            if (sb.length() >= threshold) {
                ret.add(sb.toString());
                sb = new StringBuilder();
            }
        }

        return ret;

    }
    
    public static List<String> splitStringByLength(String input, int threshold) {
        List<String> result = new ArrayList<>();
        // 检查阈值有效性
        if (threshold <= 0) {
            return result; // 返回空列表
        }
        
        // 处理空输入
        if (input == null || input.isEmpty()) {
            return result;
        }
        
        int start = 0; // 当前段落的起始索引
        int length = input.length();
        
        for (int i = 0; i < length; i++) {
            char currentChar = input.charAt(i);
            
            // 优先处理换行符
            if (currentChar == '\n') {
                // 切割换行符前的内容
                result.add(input.substring(start, i));
                start = i + 1; // 跳过换行符
                continue;
            }
            
            // 检查长度是否达到阈值（当前段落的长度）
            int currentLength = i - start + 1;
            if (currentLength == threshold) {
                // 在阈值处强制切割
                result.add(input.substring(start, i + 1));
                start = i + 1; // 移动到下一个字符
            }
        }
        
        // 添加剩余内容
        if (start < length) {
            result.add(input.substring(start));
        }
        
        return result;
    }
}