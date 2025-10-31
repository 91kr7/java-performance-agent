package com.cmdev.net.util;

import java.util.HashMap;
import java.util.Map;

public class QueryStringUtils {
    public static Map<String, String> parse(String query) {
        Map<String, String> queryParams = new HashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=", 2);
                String key = java.net.URLDecoder.decode(pair[0], java.nio.charset.StandardCharsets.UTF_8);
                String value = pair.length > 1 ? java.net.URLDecoder.decode(pair[1], java.nio.charset.StandardCharsets.UTF_8) : "";
                queryParams.put(key, value);
            }
        }
        return queryParams;
    }
}
