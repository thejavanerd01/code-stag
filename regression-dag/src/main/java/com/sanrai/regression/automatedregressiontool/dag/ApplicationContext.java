package com.sanrai.regression.automatedregressiontool.dag;

import java.util.HashMap;
import java.util.Map;

public class ApplicationContext {
    private static ApplicationContext instance;
    private Map<String, Object> context;

    private ApplicationContext() {
        context = new HashMap<>();
    }

    public static synchronized ApplicationContext getInstance() {
        if (instance == null) {
            instance = new ApplicationContext();
        }
        return instance;
    }

    public void setValue(String key, Object value) {
        context.put(key, value);
    }

    public Object getValue(String key) {
        return context.get(key);
    }
}