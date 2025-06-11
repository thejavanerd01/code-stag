package com.sanrai.regression.automatedregressiontool.dag;

import java.util.HashMap;
import java.util.Map;

public class RegisterWorker implements Worker {
    @Override
    public String getTaskName() {
        return "register";
    }

    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> input = task.getInputData();
        System.out.println("RegisterWorker input: " + input); // Debug input
        Map<String, Object> userInput = (Map<String, Object>) input.get("userInput");
        String username = (String) userInput.get("username");
        String email = (String) userInput.get("email");

        // Store userInput in ApplicationContext
        ApplicationContext.getInstance().setValue("userInput", userInput);

        Map<String, Object> output = new HashMap<>();
        Map<String, Object> result = new HashMap<>();

        // Simulate registration logic
        if (username != null && !username.isEmpty() && email != null && email.contains("@")) {
            result.put("userId", "USER_" + username.toUpperCase());
            result.put("status", "success");
            result.put("userInput", userInput);
            output.put("result", result);
            System.out.println("Registered user: " + username + ", ID: " + result.get("userId"));
        } else {
            result.put("status", "failure");
            result.put("error", "Invalid username or email");
            output.put("result", result);
            System.out.println("Registration failed for: " + username);
        }
        System.out.println("RegisterWorker output: " + output); // Debug output
        return new TaskResult(Task.TaskStatus.COMPLETED, output);
    }
}