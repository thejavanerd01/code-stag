package com.sanrai.regression.automatedregressiontool.dag;

import java.util.HashMap;
import java.util.Map;

public class UploadStatusWorker implements Worker {
    @Override
    public String getTaskName() {
        return "upload_status";
    }

    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> input = task.getInputData();
        String userId = (String) input.get("userId");
        Map<String, Object> preference = (Map<String, Object>) input.get("preference");

        // Access userInput from ApplicationContext
        Map<String, Object> userInput = (Map<String, Object>) ApplicationContext.getInstance().getValue("userInput");
        String username = userInput != null ? (String) userInput.get("username") : "unknown";

        Map<String, Object> output = new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        result.put("status", "User " + username + " uploaded status: Feeling great!");
        output.put("result", result);

        System.out.println("Uploaded status for user: " + userId);
        return new TaskResult(Task.TaskStatus.COMPLETED, output);
    }
}