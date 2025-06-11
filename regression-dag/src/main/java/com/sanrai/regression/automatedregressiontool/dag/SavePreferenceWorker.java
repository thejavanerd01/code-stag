package com.sanrai.regression.automatedregressiontool.dag;

import java.util.HashMap;
import java.util.Map;

public class SavePreferenceWorker implements Worker {
    @Override
    public String getTaskName() {
        return "save_preference";
    }

    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> input = task.getInputData();
        String userId = (String) input.get("userId");
        Map<String, Object> profile = (Map<String, Object>) input.get("profile");

        // Access userInput from ApplicationContext
        Map<String, Object> userInput = (Map<String, Object>) ApplicationContext.getInstance().getValue("userInput");
        String username = userInput != null ? (String) userInput.get("username") : "unknown";

        Map<String, Object> output = new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("preference", Map.of("notifications", "email", "theme", "dark"));
        output.put("result", result);

        System.out.println("Saved preferences for user: " + userId + ", username: " + username);
        return new TaskResult(Task.TaskStatus.COMPLETED, output);
    }
}