package com.sanrai.regression.automatedregressiontool.dag;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ProfileInfoWorker implements Worker {

    private static final Logger logger = Logger.getLogger(ProfileInfoWorker.class.getName());
    @Override
    public String getTaskName() {
        return "profile_info";
    }

    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> input = task.getInputData();
        String userId = (String) input.get("userId");
        String username = (String) input.get("username");

        // Access userInput from ApplicationContext
        Map<String, Object> userInput = (Map<String, Object>) ApplicationContext.getInstance().getValue("userInput");
        String email = userInput != null ? (String) userInput.get("email") : "unknown";

        Map<String, Object> output = new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("profile", Map.of("username", username, "email", email, "bio", "Welcome to my profile!"));
        output.put("result", result);

        System.out.println("Collected profile info for user: " + userId);
        logger.info("execution done for ProfileInfoWorker");
        return new TaskResult(Task.TaskStatus.COMPLETED, output);
    }
}